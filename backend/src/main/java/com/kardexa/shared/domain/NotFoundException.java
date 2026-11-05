package com.kardexa.shared.domain;

/**
 * El recurso pedido no existe. La capa web lo traduce a {@code 404 Not Found}.
 */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String rule, String message) {
        super(rule, message);
    }
}
