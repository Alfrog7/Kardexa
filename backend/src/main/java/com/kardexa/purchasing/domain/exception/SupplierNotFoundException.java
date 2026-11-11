package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** No existe un proveedor con ese identificador. */
public class SupplierNotFoundException extends NotFoundException {

    public SupplierNotFoundException(UUID id) {
        super("RF-15", "No existe el proveedor %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Proveedor no encontrado";
    }
}
