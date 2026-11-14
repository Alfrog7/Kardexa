package com.kardexa.reporting.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** Se pidió exportar el kárdex de un producto que no existe (404). */
public class ReportProductNotFoundException extends NotFoundException {

    public ReportProductNotFoundException(UUID id) {
        super("RF-25", "No existe el producto %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Producto no encontrado";
    }
}
