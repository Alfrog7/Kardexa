package com.kardexa.identity.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Los datos de un usuario no cumplen las reglas del dominio. */
public class InvalidUserException extends DomainException {

    public InvalidUserException(String message) {
        super("RF-03", message);
    }

    @Override
    public String title() {
        return "Usuario inválido";
    }
}
