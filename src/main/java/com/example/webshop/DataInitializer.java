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
                                "/images/products/binary-sunset.svg"
                        ),
                        new Product(
                                "404 Not Found Shirt",
                                new BigDecimal("19.99"),
                                "Für Devs, die nicht gefunden werden wollen",
                                "/images/products/404.svg"
                        ),
                        new Product(
                                "Space Invader Hoodie",
                                new BigDecimal("39.95"),
                                "Kuscheliger Hoodie mit Pixel-Invader",
                                "/images/products/space-invader.svg"
                        ),
                        new Product(
                                "Retro Console Tee",
                                new BigDecimal("22.50"),
                                "Nostalgisches Shirt mit Handheld-Konsole",
                                "/images/products/retro-console.svg"
                        ),
                        new Product(
                                "Galactic Explorer Hoodie",
                                new BigDecimal("44.90"),
                                "Extra weicher Hoodie mit Galaxienprint",
                                "/images/products/galactic-explorer.svg"
                        ),
                        new Product(
                                "Ascii Art Shirt",
                                new BigDecimal("18.95"),
                                "Minimalistisches Shirt mit ASCII-Rakete",
                                "/images/products/ascii-art.svg"
                        ),
                        new Product(
                                "Dungeon Master Tee",
                                new BigDecimal("26.00"),
                                "Für Pen-&-Paper-Abende voller Abenteuer",
                                "/images/products/dungeon-master.svg"
                        ),
                        new Product(
                                "Cyberpunk Neon Hoodie",
                                new BigDecimal("49.95"),
                                "Leuchtende Akzente für Nachtläufer:innen",
                                "/images/products/cyberpunk-neon.svg"
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
