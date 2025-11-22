package com.example.webshop.passkey;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasskeyChallengeRepository extends JpaRepository<PasskeyChallenge, String> {
}
