package com.kardexa.sales.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** Venta no encontrada. */
public class SaleNotFoundException extends NotFoundException {

    public SaleNotFoundException(UUID id) {
        super("RF-19", "No existe la venta %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Venta no encontrada";
    }
}
