package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** Ya existe una categoría con ese nombre (sin distinguir mayúsculas). */
public class DuplicateCategoryNameException extends ConflictException {

    public DuplicateCategoryNameException(String name) {
        super("RF-06", "Ya existe una categoría llamada \"%s\".".formatted(name));
    }

    @Override
    public String title() {
        return "Categoría duplicada";
    }
}
