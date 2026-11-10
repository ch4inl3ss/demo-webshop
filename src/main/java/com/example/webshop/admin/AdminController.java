package com.example.webshop.admin;

import com.example.webshop.product.ProductRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
public class AdminController {

    private final ProductRepository productRepository;

    public AdminController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/admin")
    public String admin(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("products", productRepository.findAll());
        // Zum Zeigen in der Demo: Was sagt das ID-Token von Keycloak über die Anmeldung?
        model.addAttribute("user", user);
        model.addAttribute("authTime", LocalDateTime.ofInstant(user.getAuthenticatedAt(), ZoneId.of("Europe/Berlin")));
        return "admin";
    }
}
