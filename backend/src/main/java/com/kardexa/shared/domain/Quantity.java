package com.kardexa.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Cantidad de producto con hasta 3 decimales, para productos a granel (RN-12).
 *
 * <p>Nunca es negativa: una cantidad representa unidades físicas. Que el stock no quede en negativo
 * (RN-01) lo valida el módulo de inventario antes de restar.
 *
 * @param value valor normalizado a escala 3
 */
public record Quantity(BigDecimal value) implements Comparable<Quantity> {

    public static final int SCALE = 3;
    public static final Quantity ZERO = new Quantity(BigDecimal.ZERO);

    public Quantity {
        Objects.requireNonNull(value, "La cantidad es obligatoria");
        if (value.stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException("La cantidad admite como máximo " + SCALE + " decimales: " + value);
        }
        value = value.setScale(SCALE, RoundingMode.UNNECESSARY);
        if (value.signum() < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa: " + value);
        }
    }

    public static Quantity of(String value) {
        return new Quantity(new BigDecimal(value));
    }

    public Quantity add(Quantity other) {
        return new Quantity(value.add(other.value));
    }

    /**
     * Resta otra cantidad.
     *
     * @throws IllegalArgumentException si el resultado sería negativo
     */
    public Quantity subtract(Quantity other) {
        return new Quantity(value.subtract(other.value));
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public boolean isLessThan(Quantity other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(Quantity other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
