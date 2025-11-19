package com.example.webshop.passkey;

import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.RegisteredCredential;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class JpaCredentialRepository implements CredentialRepository {

    private final PasskeyCredentialRepository credentialRepository;
    private final AppUserRepository userRepository;

    JpaCredentialRepository(PasskeyCredentialRepository credentialRepository,
                            AppUserRepository userRepository) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findByEmail(username)
                .map(AppUser::getPasskeyHandle)
                .filter(java.util.Objects::nonNull)
                .map(handle -> new ByteArray(decode(handle)));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        String handle = encode(userHandle);
        return userRepository.findByPasskeyHandle(handle)
                .map(AppUser::getEmail);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credentialRepository.findByCredentialId(encode(credentialId))
                .filter(credential -> {
                    AppUser user = credential.getUser();
                    return user.getPasskeyHandle() != null && user.getPasskeyHandle().equals(encode(userHandle));
                })
                .map(credential -> RegisteredCredential.builder()
                        .credentialId(credentialId)
                        .userHandle(userHandle)
                        .publicKeyCose(new ByteArray(credential.getPublicKeyCose()))
                        .signatureCount(credential.getSignatureCount())
                        .build());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentialRepository.findByCredentialId(encode(credentialId))
                .map(this::toRegisteredCredential)
                .map(Set::of)
                .orElseGet(Set::of);
    }

    @Override
    public Set<RegisteredCredential> lookupAllByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(credentialRepository::findAllByUser)
                .stream()
                .flatMap(List::stream)
                .map(this::toRegisteredCredential)
                .collect(Collectors.toUnmodifiableSet());
    }

    void updateSignatureCount(PasskeyCredential credential, long signatureCount) {
        credential.setSignatureCount(signatureCount);
        credentialRepository.save(credential);
    }

    void saveCredential(RegisteredCredential registeredCredential, AppUser user) {
        PasskeyCredential credential = new PasskeyCredential(
                encode(registeredCredential.getCredentialId()),
                user,
                registeredCredential.getPublicKeyCose().getBytes(),
                registeredCredential.getSignatureCount()
        );
        credentialRepository.save(credential);
    }

    private RegisteredCredential toRegisteredCredential(PasskeyCredential credential) {
        AppUser user = credential.getUser();
        return RegisteredCredential.builder()
                .credentialId(new ByteArray(decode(credential.getCredentialId())))
                .userHandle(new ByteArray(decode(user.getPasskeyHandle())))
                .publicKeyCose(new ByteArray(credential.getPublicKeyCose()))
                .signatureCount(credential.getSignatureCount())
                .build();
    }

    private String encode(ByteArray byteArray) {
        return encode(byteArray.getBytes());
    }

    private String encode(byte[] data) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] decode(String value) {
        return java.util.Base64.getUrlDecoder().decode(value);
    }
}
