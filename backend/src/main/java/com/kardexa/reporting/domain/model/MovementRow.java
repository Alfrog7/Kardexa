package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.Objects;

/**
 * Una fila del kárdex o del historial de movimientos para exportar (RF-25). {@code type} es el tipo del
 * movimiento ({@code PURCHASE}, {@code SALE_VOID}…) y {@code entry} indica si fue entrada. {@code unitCost} solo
 * existe en movimientos de compra.
 */
public record MovementRow(Instant occurredAt, String productCode, String productName, String type, boolean entry,
        Quantity quantity, Quantity balanceAfter, String document, Money unitCost, String user) {

    public MovementRow {
        Objects.requireNonNull(occurredAt, "La fecha es obligatoria");
        Objects.requireNonNull(type, "El tipo es obligatorio");
        Objects.requireNonNull(quantity, "La cantidad es obligatoria");
        Objects.requireNonNull(balanceAfter, "El saldo es obligatorio");
    }
}
