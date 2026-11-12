package com.kardexa.sales.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Venta inválida. */
public class InvalidSaleException extends DomainException {

    public InvalidSaleException(String message) {
        super("RF-18", message);
    }

    @Override
    public String title() {
        return "Venta inválida";
    }
}
