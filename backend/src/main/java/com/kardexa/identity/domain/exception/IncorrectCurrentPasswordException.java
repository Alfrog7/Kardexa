package com.kardexa.identity.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** La contraseña actual indicada no coincide. Responde 422 (no 401, para no cerrar la sesión del panel). */
public class IncorrectCurrentPasswordException extends DomainException {

    public IncorrectCurrentPasswordException() {
        super("RF-05", "La contraseña actual no es correcta.");
    }

    @Override
    public String title() {
        return "Contraseña actual incorrecta";
    }
}
