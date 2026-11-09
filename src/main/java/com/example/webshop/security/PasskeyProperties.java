package com.example.webshop.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Domain, an die die Passkeys gebunden sind, und erlaubte Origins.
 * Achtung: Eine nachträgliche Änderung der RP-ID macht alle registrierten Passkeys unbrauchbar.
 */
@ConfigurationProperties("nerdshirts.passkeys")
public record PasskeyProperties(String rpId, Set<String> allowedOrigins) {
}
