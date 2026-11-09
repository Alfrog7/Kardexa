package com.kardexa.shared.domain;

/** La persona no está autenticada o sus credenciales no valen. La capa web la traduce a {@code 401}. */
public abstract class UnauthorizedException extends DomainException {

    protected UnauthorizedException(String rule, String message) {
        super(rule, message);
    }
}
