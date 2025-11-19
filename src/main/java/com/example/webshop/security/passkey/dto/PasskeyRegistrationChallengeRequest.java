package com.example.webshop.security.passkey.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasskeyRegistrationChallengeRequest(@NotBlank @Email String email) {
}
