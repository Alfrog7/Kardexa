package com.kardexa.sales.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Producto desactivado. */
public class InactiveProductException extends DomainException {

    public InactiveProductException(String productName) {
        super("RF-18", "El producto \"%s\" está desactivado y no se puede vender.".formatted(productName));
    }

    @Override
    public String title() {
        return "Producto desactivado";
    }
}
