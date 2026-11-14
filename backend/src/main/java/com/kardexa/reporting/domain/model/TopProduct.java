package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un producto entre los más vendidos del rango: cantidad y monto de sus ventas registradas (RF-23).
 * {@code imageVersion} es nulo si el producto no tiene imagen.
 */
public record TopProduct(UUID productId, String productCode, String productName, Quantity quantity, Money amount,
        Instant imageVersion) {

    /** Máximo de productos que muestra el inicio del panel. */
    public static final int LIMIT = 10;

    public TopProduct {
        Objects.requireNonNull(productId, "El producto es obligatorio");
        Objects.requireNonNull(productCode, "El código es obligatorio");
        Objects.requireNonNull(productName, "El nombre es obligatorio");
        Objects.requireNonNull(quantity, "La cantidad es obligatoria");
        Objects.requireNonNull(amount, "El monto es obligatorio");
    }
}
