package com.kardexa.inventory.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Una entrada de mercadería necesita al menos un producto, sin repetir, con cantidad mayor que 0. */
public class InvalidReceiptException extends DomainException {

    public InvalidReceiptException(String message) {
        super("HU-21", message);
    }

    @Override
    public String title() {
        return "Entrada de mercadería inválida";
    }
}
