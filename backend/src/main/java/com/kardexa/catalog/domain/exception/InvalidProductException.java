package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Los datos del producto incumplen una regla del catálogo (nombre, código o precio). */
public class InvalidProductException extends DomainException {

    public InvalidProductException(String message) {
        super("RF-07", message);
    }

    @Override
    public String title() {
        return "Producto inválido";
    }
}
