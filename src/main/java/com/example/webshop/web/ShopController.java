package com.example.webshop.web;

import com.example.webshop.product.Product;
import com.example.webshop.product.ProductNotFoundException;
import com.example.webshop.product.ProductRepository;
import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Set;

@Controller
public class ShopController {

    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ShopController(ProductRepository productRepository,
                          AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "index";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        model.addAttribute("product", product);
        return "product-detail";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid RegistrationForm registrationForm,
                                 BindingResult bindingResult) {
        if (!registrationForm.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwörter stimmen nicht überein.");
        }
        if (!bindingResult.hasFieldErrors("email") && userRepository.existsByEmail(registrationForm.getEmail())) {
            bindingResult.rejectValue("email", "email.exists", "Diese E-Mail ist bereits registriert.");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        AppUser user = new AppUser(
                registrationForm.getEmail(),
                passwordEncoder.encode(registrationForm.getPassword()),
                Set.of("ROLE_USER")
        );
        userRepository.save(user);
        return "redirect:/login?registered";
    }
}
