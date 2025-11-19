package com.example.webshop.security.passkey;

import com.example.webshop.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {

    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    boolean existsByCredentialId(String credentialId);

    List<PasskeyCredential> findByUser(AppUser user);
}
