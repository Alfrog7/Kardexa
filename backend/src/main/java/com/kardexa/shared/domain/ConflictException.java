package com.kardexa.shared.domain;

/**
 * La operación choca con el estado actual (por ejemplo, un nombre que ya existe).
 * La capa web lo traduce a {@code 409 Conflict}.
 */
public abstract class ConflictException extends DomainException {

    protected ConflictException(String rule, String message) {
        super(rule, message);
    }
}
