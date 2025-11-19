package com.example.webshop.security.passkey.auth;

import com.example.webshop.security.passkey.dto.PasskeyLoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

public class PasskeyAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PasskeyAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(new AntPathRequestMatcher("/api/passkeys/login", "POST"));
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(new PasskeyAuthenticationSuccessHandler());
        setAuthenticationFailureHandler(new PasskeyAuthenticationFailureHandler());
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        PasskeyLoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), PasskeyLoginRequest.class);
        PasskeyAuthenticationToken token = new PasskeyAuthenticationToken(
                loginRequest.email(),
                loginRequest.credentialId(),
                loginRequest.signature(),
                loginRequest.algorithm()
        );
        return this.getAuthenticationManager().authenticate(token);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
