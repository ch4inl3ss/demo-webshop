package com.example.webshop.passkey;

import com.example.webshop.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {
    List<PasskeyCredential> findByUser(AppUser user);
    List<PasskeyCredential> findByCredentialId(String credentialId);
    Optional<PasskeyCredential> findByCredentialIdAndUser(String credentialId, AppUser user);
}
