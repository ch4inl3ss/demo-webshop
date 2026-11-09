package com.example.webshop.passkey;

import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Brücke zwischen der Yubico-Library und unserer JPA-Persistenz.
 * Die Library fragt hierüber Credentials und User-Handles ab.
 */
@Component
public class JpaCredentialRepository implements CredentialRepository {

    private final AppUserRepository userRepository;
    private final PasskeyCredentialRepository credentialRepository;

    public JpaCredentialRepository(AppUserRepository userRepository,
                                   PasskeyCredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userRepository.findByEmail(username)
                .map(credentialRepository::findByUser)
                .orElse(List.of())
                .stream()
                .map(c -> PublicKeyCredentialDescriptor.builder().id(b64(c.getCredentialId())).build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findByEmail(username)
                .map(AppUser::getWebauthnUserHandle)
                .map(JpaCredentialRepository::b64);
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return userRepository.findByWebauthnUserHandle(userHandle.getBase64Url())
                .map(AppUser::getEmail);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return userRepository.findByWebauthnUserHandle(userHandle.getBase64Url())
                .flatMap(user -> credentialRepository.findByCredentialIdAndUser(credentialId.getBase64Url(), user))
                .map(JpaCredentialRepository::toRegistered);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentialRepository.findByCredentialId(credentialId.getBase64Url()).stream()
                .map(JpaCredentialRepository::toRegistered)
                .collect(Collectors.toSet());
    }

    private static RegisteredCredential toRegistered(PasskeyCredential c) {
        return RegisteredCredential.builder()
                .credentialId(b64(c.getCredentialId()))
                .userHandle(b64(c.getUser().getWebauthnUserHandle()))
                .publicKeyCose(b64(c.getPublicKeyCose()))
                .signatureCount(c.getSignatureCount())
                .build();
    }

    static ByteArray b64(String base64Url) {
        try {
            return ByteArray.fromBase64Url(base64Url);
        } catch (Base64UrlException e) {
            throw new IllegalStateException("Ungültiges base64url in der Datenbank", e);
        }
    }
}
