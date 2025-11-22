package com.example.webshop.passkey;

import com.example.webshop.passkey.PasskeyDtos.PasskeyFinishRequest;
import com.example.webshop.passkey.PasskeyDtos.PasskeyLoginStartRequest;
import com.example.webshop.passkey.PasskeyDtos.PasskeyRegisterStartRequest;
import com.example.webshop.passkey.PasskeyDtos.PasskeyStartResponse;
import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/passkeys")
public class PasskeyController {

    private final PasskeyService passkeyService;
    private final AppUserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public PasskeyController(PasskeyService passkeyService,
                             AppUserRepository userRepository,
                             AuthenticationManager authenticationManager,
                             UserDetailsService userDetailsService) {
        this.passkeyService = passkeyService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register/start")
    public PasskeyStartResponse startRegistration(@Valid @RequestBody PasskeyRegisterStartRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AppUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benutzer nicht gefunden"));
        return passkeyService.startRegistration(user);
    }

    @PostMapping("/register/finish")
    public ResponseEntity<Map<String, String>> finishRegistration(@Valid @RequestBody PasskeyFinishRequest request) {
        AppUser user = passkeyService.finishRegistration(request);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Passkey gespeichert",
                "email", user.getEmail()
        ));
    }

    @PostMapping("/login/start")
    public PasskeyStartResponse startLogin(@Valid @RequestBody PasskeyLoginStartRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benutzer nicht gefunden"));
        return passkeyService.startLogin(user);
    }

    @PostMapping("/login/finish")
    public ResponseEntity<Map<String, String>> finishLogin(@Valid @RequestBody PasskeyFinishRequest request, HttpServletRequest httpServletRequest) {
        AppUser user = passkeyService.finishLogin(request);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(token);
        httpServletRequest.getSession(true);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Login erfolgreich",
                "email", user.getEmail()
        ));
    }
}
