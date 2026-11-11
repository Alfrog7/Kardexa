package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** RN-10: el RUC del proveedor debe tener exactamente 11 dígitos. */
public class InvalidRucException extends DomainException {

    public InvalidRucException() {
        super("RN-10", "El RUC debe tener exactamente 11 dígitos.");
    }

    @Override
    public String title() {
        return "RUC inválido";
    }
}
