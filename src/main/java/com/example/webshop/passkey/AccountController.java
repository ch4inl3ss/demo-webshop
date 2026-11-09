package com.example.webshop.passkey;

import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

    private final AppUserRepository userRepository;
    private final PasskeyCredentialRepository credentialRepository;

    public AccountController(AppUserRepository userRepository, PasskeyCredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        model.addAttribute("passkeys", credentialRepository.findByUser(user));
        return "account";
    }
}
