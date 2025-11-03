package com.example.webshop.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        String description,
        @NotBlank String imageUrl
) {
}
