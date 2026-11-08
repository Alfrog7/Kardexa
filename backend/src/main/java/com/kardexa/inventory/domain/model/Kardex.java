package com.kardexa.inventory.domain.model;

import com.kardexa.shared.domain.Quantity;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Historial de un producto en un periodo (RF-12): saldo con el que empieza, movimientos en orden
 * cronológico y saldo con el que termina (RN-02).
 */
public record Kardex(
        UUID productId,
        String productName,
        Quantity openingBalance,
        Quantity closingBalance,
        List<StockMovement> movements) {

    public Kardex {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(openingBalance, "openingBalance");
        Objects.requireNonNull(closingBalance, "closingBalance");
        movements = List.copyOf(movements);
    }

    /** El saldo final es el del último movimiento o, si no hubo ninguno, el saldo inicial. */
    public static Kardex of(UUID productId, String productName, Quantity openingBalance, List<StockMovement> movements) {
        Quantity closing = movements.isEmpty() ? openingBalance : movements.getLast().balanceAfter();
        return new Kardex(productId, productName, openingBalance, closing, movements);
    }
}
