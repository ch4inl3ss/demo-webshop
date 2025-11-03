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
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80"
                        ),
                        new Product(
                                "404 Not Found Shirt",
                                new BigDecimal("19.99"),
                                "Für Devs, die nicht gefunden werden wollen",
                                "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=800&q=80"
                        ),
                        new Product(
                                "Space Invader Hoodie",
                                new BigDecimal("39.95"),
                                "Kuscheliger Hoodie mit Pixel-Invader",
                                "https://images.unsplash.com/photo-1618354691417-d270c803d5b7?auto=format&fit=crop&w=800&q=80"
                        ),
                        new Product(
                                "Retro Console Tee",
                                new BigDecimal("22.50"),
                                "Nostalgisches Shirt mit Handheld-Konsole",
                                "https://images.unsplash.com/photo-1585386959984-a4155224a1ad?auto=format&fit=crop&w=800&q=80"
                        ),
                        new Product(
                                "Galactic Explorer Hoodie",
                                new BigDecimal("44.90"),
                                "Extra weicher Hoodie mit Galaxienprint",
                                "https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=800&q=80"
                        ),
                        new Product(
                                "Ascii Art Shirt",
                                new BigDecimal("18.95"),
                                "Minimalistisches Shirt mit ASCII-Rakete",
                                "https://images.unsplash.com/photo-1525171254930-643fc658b64e?auto=format&fit=crop&w=800&q=80"
                        ),
                        new Product(
                                "Dungeon Master Tee",
                                new BigDecimal("26.00"),
                                "Für Pen-&-Paper-Abende voller Abenteuer",
                                "https://images.unsplash.com/photo-1618354691373-d851c5c3a990?auto=format&fit=crop&w=800&q=80"
                        ),
                        new Product(
                                "Cyberpunk Neon Hoodie",
                                new BigDecimal("49.95"),
                                "Leuchtende Akzente für Nachtläufer:innen",
                                "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80"
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
