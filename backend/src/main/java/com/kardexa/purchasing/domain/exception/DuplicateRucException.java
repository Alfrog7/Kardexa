package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** RN-10: ya existe un proveedor con ese RUC. */
public class DuplicateRucException extends ConflictException {

    public DuplicateRucException(String ruc) {
        super("RN-10", "Ya existe un proveedor con el RUC %s.".formatted(ruc));
    }

    @Override
    public String title() {
        return "RUC duplicado";
    }
}
