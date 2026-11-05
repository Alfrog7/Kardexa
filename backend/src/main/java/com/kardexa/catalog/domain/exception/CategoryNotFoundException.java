package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** No existe una categoría con ese identificador. */
public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException(UUID id) {
        super("RF-06", "No existe la categoría %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Categoría no encontrada";
    }
}
