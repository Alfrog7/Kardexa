package com.kardexa.inventory.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Un producto desactivado no admite movimientos nuevos. */
public class InactiveProductException extends DomainException {

    public InactiveProductException(String productName) {
        super("RF-09", "El producto \"" + productName + "\" está desactivado.");
    }

    @Override
    public String title() {
        return "Producto desactivado";
    }
}
