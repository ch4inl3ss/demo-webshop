package com.example.webshop.passkey;

import com.example.webshop.passkey.PasskeyDtos.PasskeyFinishRequest;
import com.example.webshop.passkey.PasskeyDtos.PasskeyStartResponse;
import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationFailedException;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.data.authenticator.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.authenticator.ResidentKeyRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PasskeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final TypeReference<Map<String, Object>> MAP_REFERENCE = new TypeReference<>() {};

    private final RelyingParty relyingParty;
    private final JpaCredentialRepository credentialRepository;
    private final PasskeyChallengeRepository challengeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final AppUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PasskeyService(RelyingParty relyingParty,
                          JpaCredentialRepository credentialRepository,
                          PasskeyChallengeRepository challengeRepository,
                          PasskeyCredentialRepository passkeyCredentialRepository,
                          AppUserRepository userRepository,
                          ObjectMapper objectMapper) {
        this.relyingParty = relyingParty;
        this.credentialRepository = credentialRepository;
        this.challengeRepository = challengeRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public PasskeyStartResponse startRegistration(AppUser user) {
        ensureUserHandle(user);
        List<PublicKeyCredentialDescriptor> descriptors = passkeyCredentialRepository.findAllByUser(user).stream()
                .map(cred -> PublicKeyCredentialDescriptor.builder()
                        .id(new ByteArray(decode(cred.getCredentialId())))
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build())
                .toList();
        UserIdentity identity = UserIdentity.builder()
                .name(user.getEmail())
                .displayName(user.getEmail())
                .id(new ByteArray(decode(user.getPasskeyHandle())))
                .build();
        AuthenticatorSelectionCriteria selection = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.PREFERRED)
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();
        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(identity)
                .authenticatorSelection(selection)
                .excludeCredentials(descriptors)
                .build();
        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);
        return persistAndConvert(user, creationOptions, creationOptions, PasskeyChallengeType.REGISTRATION);
    }

    public AppUser finishRegistration(PasskeyFinishRequest request) {
        PasskeyChallenge challenge = loadChallenge(request.requestId(), PasskeyChallengeType.REGISTRATION);
        AppUser user = userRepository.findById(challenge.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benutzer nicht gefunden"));
        PublicKeyCredentialCreationOptions options = readOptions(challenge.getOptionsJson(), PublicKeyCredentialCreationOptions.class);
        PublicKeyCredential<AuthenticatorAttestationResponse, ?> credential = parseAttestation(request.credential());
        try {
            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(options)
                    .response(credential)
                    .build());
            if (!result.isSuccess()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passkey konnte nicht gespeichert werden");
            }
            RegisteredCredential registered = RegisteredCredential.builder()
                    .credentialId(result.getKeyId().getId())
                    .userHandle(new ByteArray(decode(user.getPasskeyHandle())))
                    .publicKeyCose(result.getPublicKeyCose())
                    .signatureCount(result.getSignatureCount())
                    .build();
            credentialRepository.saveCredential(registered, user);
            challengeRepository.delete(challenge);
            return user;
        } catch (RegistrationFailedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passkey konnte nicht gespeichert werden", e);
        }
    }

    public PasskeyStartResponse startLogin(AppUser user) {
        if (!passkeyCredentialRepository.existsByUser(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Für diesen Account existiert kein Passkey");
        }
        StartAssertionOptions options = StartAssertionOptions.builder()
                .username(user.getEmail())
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();
        AssertionRequest request = relyingParty.startAssertion(options);
        return persistAndConvert(user, request, request.getPublicKeyCredentialRequestOptions(), PasskeyChallengeType.LOGIN);
    }

    public AppUser finishLogin(PasskeyFinishRequest request) {
        PasskeyChallenge challenge = loadChallenge(request.requestId(), PasskeyChallengeType.LOGIN);
        AssertionRequest assertionRequest = readOptions(challenge.getOptionsJson(), AssertionRequest.class);
        PublicKeyCredential<AuthenticatorAssertionResponse, ?> credential = parseAssertion(request.credential());
        try {
            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(credential)
                    .build());
            if (!result.isSuccess()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey konnte nicht verifiziert werden");
            }
            PasskeyCredential credentialEntity = passkeyCredentialRepository.findByCredentialId(encode(result.getCredentialId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unbekanntes Credential"));
            if (!credentialEntity.getUser().getId().equals(challenge.getUserId())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Challenge gehört zu einem anderen Benutzer");
            }
            credentialRepository.updateSignatureCount(credentialEntity, result.getSignatureCount());
            challengeRepository.delete(challenge);
            return credentialEntity.getUser();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passkey konnte nicht verifiziert werden", e);
        }
    }

    private PasskeyStartResponse persistAndConvert(AppUser user, Object storedObject, Object responseObject, PasskeyChallengeType type) {
        try {
            String requestId = UUID.randomUUID().toString();
            String json = objectMapper.writeValueAsString(storedObject);
            PasskeyChallenge challenge = new PasskeyChallenge(requestId, type, user.getId(), json, Instant.now());
            challengeRepository.save(challenge);
            String responseJson = objectMapper.writeValueAsString(responseObject);
            Map<String, Object> payload = objectMapper.readValue(responseJson, MAP_REFERENCE);
            return new PasskeyStartResponse(requestId, payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Challenge konnte nicht erstellt werden", e);
        }
    }

    private void ensureUserHandle(AppUser user) {
        if (user.getPasskeyHandle() != null) {
            return;
        }
        byte[] handle = new byte[32];
        RANDOM.nextBytes(handle);
        user.setPasskeyHandle(encode(handle));
        userRepository.save(user);
    }

    private PasskeyChallenge loadChallenge(String id, PasskeyChallengeType type) {
        PasskeyChallenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge nicht gefunden"));
        if (challenge.getType() != type) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültige Challenge");
        }
        return challenge;
    }

    private PublicKeyCredential<AuthenticatorAttestationResponse, ?> parseAttestation(Map<String, Object> credential) {
        try {
            String json = objectMapper.writeValueAsString(credential);
            return PublicKeyCredential.parseRegistrationResponseJson(json);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültige Antwort", e);
        }
    }

    private PublicKeyCredential<AuthenticatorAssertionResponse, ?> parseAssertion(Map<String, Object> credential) {
        try {
            String json = objectMapper.writeValueAsString(credential);
            return PublicKeyCredential.parseAssertionResponseJson(json);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültige Antwort", e);
        }
    }

    private <T> T readOptions(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültige Challenge", e);
        }
    }

    private String encode(ByteArray value) {
        return encode(value.getBytes());
    }

    private String encode(byte[] value) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return java.util.Base64.getUrlDecoder().decode(value);
    }
}
