package com.kardexa.catalog.domain.exception;

import com.kardexa.catalog.domain.model.Category;
import com.kardexa.shared.domain.DomainException;

/** El nombre de la categoría está vacío o fuera del rango de longitud permitido. */
public class InvalidCategoryNameException extends DomainException {

    public InvalidCategoryNameException() {
        super("RF-06", "El nombre de la categoría debe tener entre %d y %d caracteres."
                .formatted(Category.NAME_MIN_LENGTH, Category.NAME_MAX_LENGTH));
    }

    @Override
    public String title() {
        return "Nombre de categoría inválido";
    }
}
