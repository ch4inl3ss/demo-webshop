package com.example.webshop.security.passkey.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasskeyRegistrationCompleteRequest(@NotBlank @Email String email,
                                                 @NotBlank String credentialId,
                                                 @NotBlank String publicKeyPem,
                                                 @NotBlank String signature,
                                                 String algorithm) {
}
