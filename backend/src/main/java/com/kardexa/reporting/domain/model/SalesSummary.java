package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Resumen del rango: total vendido, número de ventas y ticket promedio (ADR 0012). */
public record SalesSummary(Money total, long count, Money averageTicket) {

    /** Suma los días; el ticket promedio es total / ventas con redondeo half-up, y 0 si no hubo ventas. */
    public static SalesSummary of(List<DailySales> days) {
        Money total = days.stream().map(DailySales::total).reduce(Money.ZERO, Money::add);
        long count = days.stream().mapToLong(DailySales::count).sum();
        Money average = count == 0
                ? Money.ZERO
                : Money.of(total.amount().divide(BigDecimal.valueOf(count), Money.SCALE, RoundingMode.HALF_UP));
        return new SalesSummary(total, count, average);
    }
}
