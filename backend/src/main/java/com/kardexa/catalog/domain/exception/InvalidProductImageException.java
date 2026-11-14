package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** La imagen del producto no es PNG, JPG o WebP, o pesa más de 2 MB. */
public class InvalidProductImageException extends DomainException {

    public InvalidProductImageException(String message) {
        super("RF-07", message);
    }

    @Override
    public String title() {
        return "Imagen inválida";
    }
}
