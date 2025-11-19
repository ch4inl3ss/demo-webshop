package com.example.webshop.security.passkey.dto;

import java.time.Instant;

public record PasskeyChallengeResponse(String email, String challenge, Instant expiresAt, String algorithm) {
}
