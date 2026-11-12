package com.kardexa.sales.domain.model;

/** RN-07: número correlativo del comprobante ({@code V-000001}). No se reutiliza aunque la venta se anule. */
public record SaleNumber(long value) {

    public SaleNumber {
        if (value <= 0) {
            throw new IllegalArgumentException("El correlativo empieza en 1.");
        }
    }

    public static SaleNumber parse(String text) {
        if (text == null || !text.matches("V-\\d{6,}")) {
            throw new IllegalArgumentException("Número de venta inválido: " + text);
        }
        return new SaleNumber(Long.parseLong(text.substring(2)));
    }

    /** {@code V-} y al menos 6 dígitos. */
    @Override
    public String toString() {
        return "V-%06d".formatted(value);
    }
}
