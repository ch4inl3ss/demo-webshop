package com.example.webshop.passkey;

import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping(value = "/passkeys", produces = MediaType.APPLICATION_JSON_VALUE)
public class PasskeyController {

    private static final Logger log = LoggerFactory.getLogger(PasskeyController.class);
    private static final String REG_REQUEST = "passkey.registration.request";
    private static final String AUTH_REQUEST = "passkey.assertion.request";

    private final RelyingParty relyingParty;
    private final AppUserRepository userRepository;
    private final PasskeyCredentialRepository credentialRepository;
    private final UserDetailsService userDetailsService;
    private final SecureRandom random = new SecureRandom();
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public PasskeyController(RelyingParty relyingParty,
                             AppUserRepository userRepository,
                             PasskeyCredentialRepository credentialRepository,
                             UserDetailsService userDetailsService) {
        this.relyingParty = relyingParty;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userDetailsService = userDetailsService;
    }

    // ---------------------------------------------------------------- Registrierung (eingeloggt)

    @PostMapping("/register/options")
    @Transactional
    public String registrationOptions(Authentication authentication, HttpSession session) throws IOException {
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        if (user.getWebauthnUserHandle() == null) {
            byte[] handle = new byte[32];
            random.nextBytes(handle);
            user.setWebauthnUserHandle(new ByteArray(handle).getBase64Url());
            userRepository.save(user);
        }
        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(UserIdentity.builder()
                                .name(user.getEmail())
                                .displayName(user.getEmail())
                                .id(JpaCredentialRepository.b64(user.getWebauthnUserHandle()))
                                .build())
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .residentKey(ResidentKeyRequirement.REQUIRED)
                                .userVerification(UserVerificationRequirement.REQUIRED)
                                .build())
                        .timeout(120_000L)
                        .build());
        session.setAttribute(REG_REQUEST, options.toJson());
        return options.toCredentialsCreateJson();
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@RequestBody String credentialJson,
                                                        @RequestParam(defaultValue = "Passkey") String label,
                                                        Authentication authentication,
                                                        HttpSession session) throws IOException {
        String requestJson = (String) session.getAttribute(REG_REQUEST);
        session.removeAttribute(REG_REQUEST); // Challenge nur einmal verwendbar
        if (requestJson == null) {
            return error(HttpStatus.BAD_REQUEST, "Keine laufende Registrierung");
        }
        PublicKeyCredentialCreationOptions request = PublicKeyCredentialCreationOptions.fromJson(requestJson);
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        if (!request.getUser().getId().getBase64Url().equals(user.getWebauthnUserHandle())) {
            return error(HttpStatus.BAD_REQUEST, "Registrierung gehört zu anderem Benutzer");
        }
        try {
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                    PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());
            PasskeyCredential saved = credentialRepository.save(new PasskeyCredential(
                    user,
                    result.getKeyId().getId().getBase64Url(),
                    result.getPublicKeyCose().getBase64Url(),
                    result.getSignatureCount(),
                    label.length() > 100 ? label.substring(0, 100) : label));
            log.info("Passkey {} für {} registriert", saved.getId(), user.getEmail());
            return ResponseEntity.ok(Map.of("status", "ok", "id", saved.getId()));
        } catch (RegistrationFailedException e) {
            log.warn("Passkey-Registrierung fehlgeschlagen: {}", e.getMessage());
            return error(HttpStatus.BAD_REQUEST, "Registrierung fehlgeschlagen");
        }
    }

    // ---------------------------------------------------------------- Login (anonym)

    @PostMapping("/login/options")
    public String loginOptions(HttpSession session) throws IOException {
        // Ohne Username: discoverable credentials ("usernameless" Login)
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.REQUIRED)
                .timeout(120_000L)
                .build());
        session.setAttribute(AUTH_REQUEST, request.toJson());
        return request.toCredentialsGetJson();
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> login(@RequestBody String credentialJson,
                                                     HttpServletRequest httpRequest,
                                                     HttpServletResponse httpResponse) throws IOException {
        HttpSession session = httpRequest.getSession();
        String requestJson = (String) session.getAttribute(AUTH_REQUEST);
        session.removeAttribute(AUTH_REQUEST);
        if (requestJson == null) {
            return error(HttpStatus.BAD_REQUEST, "Kein laufender Login");
        }
        try {
            AssertionRequest request = AssertionRequest.fromJson(requestJson);
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                    PublicKeyCredential.parseAssertionResponseJson(credentialJson);
            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());
            if (!result.isSuccess()) {
                return error(HttpStatus.UNAUTHORIZED, "Passkey-Login fehlgeschlagen");
            }
            // Signatur-Zähler aktualisieren (Clone-Erkennung macht die Library)
            credentialRepository.findByCredentialId(result.getCredential().getCredentialId().getBase64Url())
                    .forEach(c -> {
                        c.setSignatureCount(result.getSignatureCount());
                        c.setLastUsedAt(Instant.now());
                    });

            UserDetails userDetails = userDetailsService.loadUserByUsername(result.getUsername());
            Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                    userDetails, null, userDetails.getAuthorities());
            httpRequest.changeSessionId(); // Schutz vor Session Fixation
            SecurityContext context = contextHolder.createEmptyContext();
            context.setAuthentication(auth);
            contextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
            log.info("Passkey-Login für {}", result.getUsername());
            return ResponseEntity.ok(Map.of("status", "ok", "redirect", "/"));
        } catch (AssertionFailedException e) {
            log.warn("Passkey-Login fehlgeschlagen: {}", e.getMessage());
            return error(HttpStatus.UNAUTHORIZED, "Passkey-Login fehlgeschlagen");
        }
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", "error", "message", message));
    }
}
