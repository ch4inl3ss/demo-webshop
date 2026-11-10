package com.example.webshop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Login, Passwörter und Passkeys macht Keycloak. Der Shop ist nur noch OIDC-Client.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clients) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/products/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(keycloakRoles())))
                .logout(logout -> logout.logoutSuccessHandler(keycloakLogout(clients)))
                .build();
    }

    // Realm-Rollen kommen als Claim "roles" im ID-Token (Mapper im Client, siehe keycloak/nerdshirts-realm.json)
    private static GrantedAuthoritiesMapper keycloakRoles() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            authorities.stream()
                    .filter(OidcUserAuthority.class::isInstance)
                    .map(OidcUserAuthority.class::cast)
                    .map(authority -> authority.getIdToken().getClaimAsStringList("roles"))
                    .filter(roles -> roles != null)
                    .flatMap(List::stream)
                    .forEach(role -> mapped.add(new SimpleGrantedAuthority("ROLE_" + role)));
            return mapped;
        };
    }

    // Logout auch bei Keycloak beenden, sonst ist man beim nächsten Klick auf "Login" sofort wieder drin
    private static OidcClientInitiatedLogoutSuccessHandler keycloakLogout(ClientRegistrationRepository clients) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(clients);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }
}
