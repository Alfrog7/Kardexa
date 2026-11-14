package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * Una venta del historial para exportar (RF-25): {@code status} es {@code REGISTERED} o {@code VOIDED},
 * {@code lines} la cantidad de productos distintos y {@code voidReason} solo existe en las anuladas.
 */
public record SaleRow(String number, Instant soldAt, String seller, String status, int lines, Money total,
        String voidReason) {

    public SaleRow {
        Objects.requireNonNull(number, "El número es obligatorio");
        Objects.requireNonNull(soldAt, "La fecha es obligatoria");
        Objects.requireNonNull(status, "El estado es obligatorio");
        Objects.requireNonNull(total, "El total es obligatorio");
    }
}
