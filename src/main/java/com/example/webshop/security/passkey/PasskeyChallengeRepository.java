package com.example.webshop.security.passkey;

import com.example.webshop.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasskeyChallengeRepository extends JpaRepository<PasskeyChallenge, Long> {

    Optional<PasskeyChallenge> findTopByUserAndTypeOrderByCreatedAtDesc(AppUser user, PasskeyChallengeType type);

    Optional<PasskeyChallenge> findTopByUserAndTypeAndCredentialIdOrderByCreatedAtDesc(AppUser user,
                                                                                      PasskeyChallengeType type,
                                                                                      String credentialId);

    void deleteByExpiresAtBefore(Instant instant);
}
