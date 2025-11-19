package com.example.webshop.security.passkey.auth;

import com.example.webshop.security.passkey.PasskeyException;
import com.example.webshop.security.passkey.PasskeyService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public class PasskeyAuthenticationProvider implements AuthenticationProvider {

    private final PasskeyService passkeyService;
    private final UserDetailsService userDetailsService;

    public PasskeyAuthenticationProvider(PasskeyService passkeyService, UserDetailsService userDetailsService) {
        this.passkeyService = passkeyService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PasskeyAuthenticationToken token = (PasskeyAuthenticationToken) authentication;
        try {
            passkeyService.verifyAssertion(token.getEmail(), token.getCredentialId(), token.getSignature(), token.getAlgorithm());
            UserDetails userDetails = userDetailsService.loadUserByUsername(token.getEmail());
            return new PasskeyAuthenticationToken(userDetails, token.getCredentialId(), userDetails.getAuthorities());
        } catch (PasskeyException ex) {
            throw new BadCredentialsException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasskeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
