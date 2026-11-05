package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** No existe un producto con ese identificador. */
public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(UUID id) {
        super("RF-07", "No existe el producto %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Producto no encontrado";
    }
}
