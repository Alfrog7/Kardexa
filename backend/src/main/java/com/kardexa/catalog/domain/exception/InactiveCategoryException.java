package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** HU-05: una categoría desactivada ya no se ofrece para productos nuevos. */
public class InactiveCategoryException extends DomainException {

    public InactiveCategoryException(String categoryName) {
        super("RF-06", "La categoría \"%s\" está desactivada.".formatted(categoryName));
    }

    @Override
    public String title() {
        return "Categoría desactivada";
    }
}
