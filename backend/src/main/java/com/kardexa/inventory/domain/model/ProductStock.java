package com.kardexa.inventory.domain.model;

import com.kardexa.inventory.domain.exception.InsufficientStockException;
import com.kardexa.inventory.domain.exception.StockLimitExceededException;
import com.kardexa.shared.domain.Quantity;
import java.util.Objects;
import java.util.UUID;

/**
 * Stock actual de un producto. Es inmutable: aplicar un movimiento devuelve el nuevo estado, con la
 * misma {@code version} que se leyó, para que al guardarlo se detecte si otro lo cambió (ADR 0006).
 */
public record ProductStock(UUID productId, Quantity quantity, long version) {

    /** Versión de un stock que todavía no tiene fila: nace con su primer movimiento. */
    public static final long NEVER_SAVED = -1;

    /** Máximo que admite la columna {@code numeric(12,3)}. */
    static final Quantity MAX_QUANTITY = Quantity.of("999999999.999");

    public ProductStock {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(quantity, "quantity");
    }

    public static ProductStock empty(UUID productId) {
        return new ProductStock(productId, Quantity.ZERO, NEVER_SAVED);
    }

    /** RN-01: una salida mayor que el stock se rechaza. */
    public ProductStock apply(Direction direction, Quantity amount, String productName) {
        return switch (direction) {
            case IN -> receive(amount);
            case OUT -> withdraw(amount, productName);
        };
    }

    private ProductStock receive(Quantity amount) {
        Quantity result = quantity.add(amount);
        if (MAX_QUANTITY.isLessThan(result)) {
            throw new StockLimitExceededException(result);
        }
        return new ProductStock(productId, result, version);
    }

    private ProductStock withdraw(Quantity amount, String productName) {
        if (quantity.isLessThan(amount)) {
            throw new InsufficientStockException(productName, quantity, amount);
        }
        return new ProductStock(productId, quantity.subtract(amount), version);
    }
}
