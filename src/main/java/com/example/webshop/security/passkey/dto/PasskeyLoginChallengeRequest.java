package com.example.webshop.security.passkey.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasskeyLoginChallengeRequest(@NotBlank @Email String email, @NotBlank String credentialId) {
}
