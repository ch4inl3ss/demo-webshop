package com.example.webshop.security;

import com.example.webshop.user.AppUserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authorization.AllRequiredFactorsAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(PasskeyProperties.class)
public class SecurityConfig {

    private final AppUserRepository userRepository;

    public SecurityConfig(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PasskeyProperties passkeys) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/products/**", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").access(AuthorizationManagers.allOf(hasRole("ADMIN"), recentPasskey()))
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").permitAll())
                .webAuthn(webAuthn -> webAuthn
                        .rpName("Nerdshirts")
                        .rpId(passkeys.rpId())
                        .allowedOrigins(passkeys.allowedOrigins()))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .build();
    }

    // Step-up: Admin nur mit Passkey-Anmeldung, die höchstens 5 Minuten alt ist
    private static AuthorizationManager<RequestAuthorizationContext> recentPasskey() {
        return AllRequiredFactorsAuthorizationManager.<RequestAuthorizationContext>builder()
                .requireFactor(factor -> factor.webauthnAuthority().validDuration(Duration.ofMinutes(5)))
                .build();
    }

    @Bean
    PublicKeyCredentialUserEntityRepository userEntities(JdbcOperations jdbc) {
        return new JdbcPublicKeyCredentialUserEntityRepository(jdbc);
    }

    @Bean
    UserCredentialRepository userCredentials(JdbcOperations jdbc) {
        return new JdbcUserCredentialRepository(jdbc);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(user.getRoles().toArray(String[]::new))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
