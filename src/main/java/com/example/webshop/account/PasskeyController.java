package com.example.webshop.account;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/konto/passkeys")
public class PasskeyController {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private final PublicKeyCredentialUserEntityRepository userEntities;
    private final UserCredentialRepository userCredentials;

    public PasskeyController(PublicKeyCredentialUserEntityRepository userEntities,
                             UserCredentialRepository userCredentials) {
        this.userEntities = userEntities;
        this.userCredentials = userCredentials;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        PublicKeyCredentialUserEntity user = userEntities.findByUsername(authentication.getName());
        List<PasskeyView> passkeys = user == null ? List.of() : userCredentials.findByUserId(user.getId()).stream()
                .sorted(Comparator.comparing(CredentialRecord::getCreated))
                .map(PasskeyView::of)
                .toList();
        model.addAttribute("passkeys", passkeys);
        // Für die Signal API: Der Passkey-Manager soll wissen, welche Passkeys der Server noch kennt
        model.addAttribute("userHandle", user == null ? null : user.getId().toBase64UrlString());
        return "passkeys";
    }

    @PostMapping("/{id}/umbenennen")
    public String rename(@PathVariable String id, @RequestParam String label, Authentication authentication) {
        CredentialRecord credential = ownCredential(id, authentication);
        userCredentials.save(ImmutableCredentialRecord.fromCredentialRecord(credential).label(label.strip()).build());
        return "redirect:/konto/passkeys";
    }

    @PostMapping("/{id}/loeschen")
    public String delete(@PathVariable String id, Authentication authentication) {
        CredentialRecord credential = ownCredential(id, authentication);
        userCredentials.delete(credential.getCredentialId());
        return "redirect:/konto/passkeys?geloescht";
    }

    // Nur Passkeys des eingeloggten Kontos – sonst könnte man fremde Passkeys löschen (IDOR)
    private CredentialRecord ownCredential(String id, Authentication authentication) {
        PublicKeyCredentialUserEntity user = userEntities.findByUsername(authentication.getName());
        CredentialRecord credential = userCredentials.findByCredentialId(Bytes.fromBase64(id));
        if (user == null || credential == null || !credential.getUserEntityUserId().equals(user.getId())) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return credential;
    }

    public record PasskeyView(String id, String label, LocalDateTime created, LocalDateTime lastUsed, boolean synced) {

        static PasskeyView of(CredentialRecord credential) {
            return new PasskeyView(
                    credential.getCredentialId().toBase64UrlString(),
                    credential.getLabel(),
                    local(credential.getCreated()),
                    local(credential.getLastUsed()),
                    credential.isBackupEligible());
        }

        private static LocalDateTime local(Instant instant) {
            return instant == null ? null : LocalDateTime.ofInstant(instant, ZONE);
        }
    }
}
