package com.example.webshop.security.passkey;

import com.example.webshop.security.passkey.dto.PasskeyChallengeResponse;
import com.example.webshop.security.passkey.dto.PasskeyLoginChallengeRequest;
import com.example.webshop.security.passkey.dto.PasskeyRegistrationChallengeRequest;
import com.example.webshop.security.passkey.dto.PasskeyRegistrationCompleteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passkeys")
public class PasskeyController {

    private final PasskeyService passkeyService;

    public PasskeyController(PasskeyService passkeyService) {
        this.passkeyService = passkeyService;
    }

    @PostMapping("/register/options")
    public PasskeyChallengeResponse requestRegistrationChallenge(@Valid @RequestBody PasskeyRegistrationChallengeRequest request) {
        return passkeyService.createRegistrationChallenge(request);
    }

    @PostMapping("/register/complete")
    public ResponseEntity<Void> completeRegistration(@Valid @RequestBody PasskeyRegistrationCompleteRequest request) {
        passkeyService.completeRegistration(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login/options")
    public PasskeyChallengeResponse requestLoginChallenge(@Valid @RequestBody PasskeyLoginChallengeRequest request) {
        return passkeyService.createLoginChallenge(request);
    }
}
