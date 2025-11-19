package com.example.webshop.passkey;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.Origin;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(PasskeyProperties.class)
public class PasskeyConfig {

    @Bean
    RelyingParty relyingParty(PasskeyProperties properties, JpaCredentialRepository credentialRepository) {
        RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
                .id(properties.getRpId())
                .name(properties.getRpName())
                .build();
        Set<Origin> origins = properties.getOrigins().stream()
                .map(origin -> {
                    try {
                        return Origin.create(origin);
                    } catch (Exception exception) {
                        throw new IllegalArgumentException("Ungültige Origin: " + origin, exception);
                    }
                })
                .collect(Collectors.toUnmodifiableSet());
        return RelyingParty.builder()
                .identity(identity)
                .credentialRepository(credentialRepository)
                .origins(origins)
                .build();
    }
}
