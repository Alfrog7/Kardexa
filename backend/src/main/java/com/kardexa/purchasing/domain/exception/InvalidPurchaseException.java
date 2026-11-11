package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** La compra o alguno de sus ítems no cumple lo que pide RF-16. */
public class InvalidPurchaseException extends DomainException {

    public InvalidPurchaseException(String message) {
        super("RF-16", message);
    }

    @Override
    public String title() {
        return "Compra inválida";
    }
}
