package com.example.webshop.admin;

import com.example.webshop.product.ProductRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
public class AdminController {

    private final ProductRepository productRepository;

    public AdminController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/admin")
    public String admin(Authentication authentication, Model model) {
        model.addAttribute("products", productRepository.findAll());
        // Zum Zeigen in der Demo: Mit welchen Faktoren ist die Sitzung angemeldet, und wann?
        List<Factor> factors = authentication.getAuthorities().stream()
                .filter(FactorGrantedAuthority.class::isInstance)
                .map(FactorGrantedAuthority.class::cast)
                .map(f -> new Factor(f.getAuthority(), LocalDateTime.ofInstant(f.getIssuedAt(), ZoneId.of("Europe/Berlin"))))
                .toList();
        model.addAttribute("factors", factors);
        return "admin";
    }

    public record Factor(String authority, LocalDateTime issuedAt) {
    }
}
