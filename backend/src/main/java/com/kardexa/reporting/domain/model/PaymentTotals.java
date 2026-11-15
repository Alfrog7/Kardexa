package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Money;

/** Lo recibido por cada medio de pago en los turnos cerrados del rango (HU-24). */
public record PaymentTotals(Money cash, Money yape, Money plin, Money transfer) {

    public Money total() {
        return cash.add(yape).add(plin).add(transfer);
    }
}
