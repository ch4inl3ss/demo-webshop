package com.example.webshop;

import com.example.webshop.product.Product;
import com.example.webshop.product.ProductRepository;
import com.example.webshop.user.AppUser;
import com.example.webshop.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner loadData(ProductRepository productRepository,
                               AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (productRepository.count() == 0) {
                List<Product> products = List.of(
                        new Product(
                                "Binary Sunset Tee",
                                new BigDecimal("24.95"),
                                "Shirt mit Twin-Suns-Motiv im Retro-Stil",
                                "https://example.com/images/binary-sunset.png"
                        ),
                        new Product(
                                "404 Not Found Shirt",
                                new BigDecimal("19.99"),
                                "Für Devs, die nicht gefunden werden wollen",
                                "https://example.com/images/404-shirt.png"
                        ),
                        new Product(
                                "Space Invader Hoodie",
                                new BigDecimal("39.95"),
                                "Kuscheliger Hoodie mit Pixel-Invader",
                                "https://example.com/images/space-invader.png"
                        )
                );
                productRepository.saveAll(products);
                log.info("Beispielprodukte angelegt: {}", products.size());
            }

            if (userRepository.count() == 0) {
                AppUser admin = new AppUser(
                        "admin@nerdshirts.de",
                        passwordEncoder.encode("changeme"),
                        Set.of("ROLE_ADMIN", "ROLE_USER")
                );
                userRepository.save(admin);
                log.info("Standard-Admin erstellt: {}", admin.getEmail());
            }
        };
    }
}
