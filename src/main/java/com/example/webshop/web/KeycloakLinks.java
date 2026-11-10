package com.example.webshop.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Passkeys verwaltet jetzt Keycloak: Der Shop verlinkt nur noch auf dessen Account-Konsole
@ControllerAdvice
public class KeycloakLinks {

    private final String accountUrl;

    public KeycloakLinks(@Value("${nerdshirts.keycloak.account-url}") String accountUrl) {
        this.accountUrl = accountUrl;
    }

    @ModelAttribute("keycloakAccountUrl")
    public String keycloakAccountUrl() {
        return accountUrl;
    }
}
