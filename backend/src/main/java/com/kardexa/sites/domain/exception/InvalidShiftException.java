package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** El conteo de cierre no es válido: un producto repetido o un dato fuera de rango. */
public class InvalidShiftException extends DomainException {

    public InvalidShiftException(String message) {
        super("RN-15", message);
    }

    @Override
    public String title() {
        return "Cierre de turno inválido";
    }
}
