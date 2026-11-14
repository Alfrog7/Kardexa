package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Producto activo en stock crítico: stock actual menor o igual que su mínimo (RF-24, RN-09). {@code imageVersion}
 * es nulo si no tiene imagen.
 */
public record CriticalProduct(UUID productId, String productCode, String productName, String categoryName,
        Quantity stock, Quantity minStock, Instant imageVersion) {

    public CriticalProduct {
        Objects.requireNonNull(productId, "El producto es obligatorio");
        Objects.requireNonNull(productCode, "El código es obligatorio");
        Objects.requireNonNull(productName, "El nombre es obligatorio");
        Objects.requireNonNull(categoryName, "La categoría es obligatoria");
        Objects.requireNonNull(stock, "El stock es obligatorio");
        Objects.requireNonNull(minStock, "El stock mínimo es obligatorio");
    }
}
