package com.example.webshop.security.passkey;

import com.example.webshop.security.passkey.dto.PasskeyChallengeResponse;
import com.example.webshop.security.passkey.dto.PasskeyLoginChallengeRequest;
import com.example.webshop.security.passkey.dto.PasskeyRegistrationChallengeRequest;
import com.example.webshop.security.passkey.dto.PasskeyRegistrationCompleteRequest;
import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class PasskeyService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final String DEFAULT_ALGORITHM = "SHA256withECDSA";

    private final AppUserRepository userRepository;
    private final PasskeyCredentialRepository credentialRepository;
    private final PasskeyChallengeRepository challengeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasskeyService(AppUserRepository userRepository,
                          PasskeyCredentialRepository credentialRepository,
                          PasskeyChallengeRepository challengeRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.challengeRepository = challengeRepository;
    }

    public PasskeyChallengeResponse createRegistrationChallenge(PasskeyRegistrationChallengeRequest request) {
        AppUser user = findUser(request.email());
        PasskeyChallenge challenge = storeChallenge(user, PasskeyChallengeType.REGISTRATION, null);
        return new PasskeyChallengeResponse(user.getEmail(), challenge.getChallenge(), challenge.getExpiresAt(), DEFAULT_ALGORITHM);
    }

    public void completeRegistration(PasskeyRegistrationCompleteRequest request) {
        AppUser user = findUser(request.email());
        PasskeyChallenge challenge = requireChallenge(user, PasskeyChallengeType.REGISTRATION, null);
        PublicKey publicKey = parsePublicKey(request.publicKeyPem());
        verifySignature(publicKey, challenge.getChallenge(), request.signature(), request.algorithm());
        if (credentialRepository.existsByCredentialId(request.credentialId())) {
            throw new PasskeyException("Credential-ID wird bereits verwendet");
        }
        PasskeyCredential credential = new PasskeyCredential(user, request.credentialId(), request.publicKeyPem());
        credentialRepository.save(credential);
        challenge.markConsumed();
    }

    public PasskeyChallengeResponse createLoginChallenge(PasskeyLoginChallengeRequest request) {
        AppUser user = findUser(request.email());
        PasskeyCredential credential = credentialRepository.findByCredentialId(request.credentialId())
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new PasskeyException("Passkey wurde für dieses Konto nicht registriert"));
        PasskeyChallenge challenge = storeChallenge(user, PasskeyChallengeType.LOGIN, credential.getCredentialId());
        return new PasskeyChallengeResponse(user.getEmail(), challenge.getChallenge(), challenge.getExpiresAt(), DEFAULT_ALGORITHM);
    }

    public AppUser verifyAssertion(String email, String credentialId, String signature, String algorithm) {
        AppUser user = findUser(email);
        PasskeyCredential credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new PasskeyException("Unbekannter Passkey"));
        if (!credential.getUser().getId().equals(user.getId())) {
            throw new PasskeyException("Passkey gehört zu einem anderen Konto");
        }
        PasskeyChallenge challenge = requireChallenge(user, PasskeyChallengeType.LOGIN, credentialId);
        PublicKey publicKey = parsePublicKey(credential.getPublicKeyPem());
        verifySignature(publicKey, challenge.getChallenge(), signature, algorithm);
        challenge.markConsumed();
        credential.bumpCounter();
        return user;
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new PasskeyException("Unbekannter Benutzer"));
    }

    private PasskeyChallenge storeChallenge(AppUser user, PasskeyChallengeType type, String credentialId) {
        challengeRepository.deleteByExpiresAtBefore(Instant.now());
        String challengeValue = generateChallenge();
        PasskeyChallenge challenge = new PasskeyChallenge(user, type, challengeValue, credentialId,
                Instant.now().plus(CHALLENGE_TTL));
        return challengeRepository.save(challenge);
    }

    private PasskeyChallenge requireChallenge(AppUser user, PasskeyChallengeType type, String credentialId) {
        Optional<PasskeyChallenge> latest = credentialId == null
                ? challengeRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, type)
                : challengeRepository.findTopByUserAndTypeAndCredentialIdOrderByCreatedAtDesc(user, type, credentialId);
        PasskeyChallenge challenge = latest.orElseThrow(() -> new PasskeyException("Keine Challenge vorhanden"));
        if (challenge.isConsumed()) {
            throw new PasskeyException("Challenge wurde bereits verwendet");
        }
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            throw new PasskeyException("Challenge ist abgelaufen");
        }
        return challenge;
    }

    private String generateChallenge() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void verifySignature(PublicKey publicKey, String challenge, String signatureBase64, String algorithm) {
        try {
            String resolvedAlgorithm = algorithm != null && !algorithm.isBlank() ? algorithm : defaultAlgorithm(publicKey);
            Signature verifier = Signature.getInstance(resolvedAlgorithm);
            verifier.initVerify(publicKey);
            verifier.update(Base64.getUrlDecoder().decode(challenge));
            boolean valid = verifier.verify(Base64.getUrlDecoder().decode(signatureBase64));
            if (!valid) {
                throw new PasskeyException("Signatur konnte nicht verifiziert werden");
            }
        } catch (Exception ex) {
            if (ex instanceof PasskeyException) {
                throw (PasskeyException) ex;
            }
            throw new PasskeyException("Signaturprüfung fehlgeschlagen", ex);
        }
    }

    private PublicKey parsePublicKey(String pem) {
        String sanitized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(sanitized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        for (String algorithm : new String[]{"EC", "RSA"}) {
            try {
                KeyFactory factory = KeyFactory.getInstance(algorithm);
                return factory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ignored) {
            }
        }
        throw new PasskeyException("Öffentlicher Schlüssel konnte nicht gelesen werden");
    }

    private String defaultAlgorithm(PublicKey publicKey) {
        return "RSA".equalsIgnoreCase(publicKey.getAlgorithm()) ? "SHA256withRSA" : DEFAULT_ALGORITHM;
    }
}
