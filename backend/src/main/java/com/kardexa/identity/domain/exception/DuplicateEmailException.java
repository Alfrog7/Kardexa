package com.kardexa.identity.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** Ya existe un usuario con ese correo (sin distinguir mayúsculas). */
public class DuplicateEmailException extends ConflictException {

    public DuplicateEmailException(String email) {
        super("RF-03", "Ya existe un usuario con el correo %s.".formatted(email));
    }

    @Override
    public String title() {
        return "Correo duplicado";
    }
}
