package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** No existe una compra con ese identificador. */
public class PurchaseNotFoundException extends NotFoundException {

    public PurchaseNotFoundException(UUID id) {
        super("RF-16", "No existe la compra %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Compra no encontrada";
    }
}
