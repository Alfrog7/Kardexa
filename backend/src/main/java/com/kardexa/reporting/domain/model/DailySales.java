package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Money;
import java.time.LocalDate;
import java.util.Objects;

/** Lo vendido en un día de Lima: total y número de ventas registradas (RF-22). */
public record DailySales(LocalDate date, Money total, long count) {

    public DailySales {
        Objects.requireNonNull(date, "La fecha es obligatoria");
        Objects.requireNonNull(total, "El total es obligatorio");
        if (count < 0) {
            throw new IllegalArgumentException("La cantidad de ventas no puede ser negativa");
        }
    }
}
