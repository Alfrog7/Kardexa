package com.kardexa.sales.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** Producto no encontrado. */
public class SaleProductNotFoundException extends NotFoundException {

    public SaleProductNotFoundException(UUID id) {
        super("RF-18", "No existe el producto %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Producto no encontrado";
    }
}
