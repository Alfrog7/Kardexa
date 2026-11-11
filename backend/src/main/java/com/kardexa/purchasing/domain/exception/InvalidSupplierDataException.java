package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** La razón social, el teléfono o el correo del proveedor están fuera de lo permitido. */
public class InvalidSupplierDataException extends DomainException {

    public InvalidSupplierDataException(String message) {
        super("RF-15", message);
    }

    @Override
    public String title() {
        return "Datos de proveedor inválidos";
    }
}
