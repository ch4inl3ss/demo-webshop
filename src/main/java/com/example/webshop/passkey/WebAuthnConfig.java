package com.example.webshop.passkey;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingParty relyingParty(JpaCredentialRepository credentialRepository,
                                     @Value("${webauthn.rp-id:localhost}") String rpId,
                                     @Value("${webauthn.rp-name:Nerdshirts}") String rpName,
                                     @Value("${webauthn.origins:http://localhost:8080,http://localhost:8081}") List<String> origins) {
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder().id(rpId).name(rpName).build())
                .credentialRepository(credentialRepository)
                .origins(new HashSet<>(origins))
                .build();
    }
}
