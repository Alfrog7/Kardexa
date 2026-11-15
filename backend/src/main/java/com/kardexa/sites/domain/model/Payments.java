package com.kardexa.sites.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Lo recibido al cerrar el turno por cada medio de pago (HU-23). Montos no negativos con hasta 2 decimales; 0 si
 * no hubo.
 */
public record Payments(BigDecimal cash, BigDecimal yape, BigDecimal plin, BigDecimal transfer) {

    public Payments {
        cash = normalize(cash, "efectivo");
        yape = normalize(yape, "Yape");
        plin = normalize(plin, "Plin");
        transfer = normalize(transfer, "transferencia");
    }

    private static BigDecimal normalize(BigDecimal amount, String medium) {
        Objects.requireNonNull(amount, medium);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("El monto de " + medium + " no puede ser negativo.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El monto de " + medium + " admite hasta 2 decimales.");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public BigDecimal total() {
        return cash.add(yape).add(plin).add(transfer);
    }
}
