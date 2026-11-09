package com.kardexa.shared.domain;

/** La operación no está permitida en el estado actual. La capa web la traduce a {@code 403}. */
public abstract class ForbiddenException extends DomainException {

    protected ForbiddenException(String rule, String message) {
        super(rule, message);
    }
}
