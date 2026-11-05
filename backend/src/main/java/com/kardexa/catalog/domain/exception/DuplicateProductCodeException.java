package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** RN-10: ya existe un producto con ese código. */
public class DuplicateProductCodeException extends ConflictException {

    public DuplicateProductCodeException(String code) {
        super("RN-10", "Ya existe un producto con el código \"%s\".".formatted(code));
    }

    @Override
    public String title() {
        return "Código de producto duplicado";
    }
}
