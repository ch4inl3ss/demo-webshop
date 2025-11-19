package com.example.webshop.passkey;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class PasskeyDtos {

    private PasskeyDtos() {
    }

    public record PasskeyRegisterStartRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record PasskeyLoginStartRequest(@Email @NotBlank String email) {
    }

    public record PasskeyFinishRequest(@NotBlank String requestId, @NotNull Map<String, Object> credential) {
    }

    public record PasskeyStartResponse(String requestId, Map<String, Object> publicKey) {
    }
}
