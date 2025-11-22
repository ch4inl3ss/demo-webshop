package com.example.webshop.security.passkey.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class PasskeyAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final String credentialId;
    private final String signature;
    private final String algorithm;

    public PasskeyAuthenticationToken(String email, String credentialId, String signature, String algorithm) {
        super(null);
        this.principal = email;
        this.credentialId = credentialId;
        this.signature = signature;
        this.algorithm = algorithm;
        setAuthenticated(false);
    }

    public PasskeyAuthenticationToken(UserDetails userDetails, String credentialId,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = userDetails;
        this.credentialId = credentialId;
        this.signature = null;
        this.algorithm = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.signature;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getEmail() {
        return principal instanceof UserDetails userDetails ? userDetails.getUsername() : principal.toString();
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getSignature() {
        return signature;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
