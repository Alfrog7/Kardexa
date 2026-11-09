package com.kardexa.identity.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** RN-11: siempre debe quedar al menos un administrador activo. */
public class LastAdminException extends DomainException {

    public LastAdminException() {
        super("RN-11", "Debe quedar al menos un administrador activo: el último no puede desactivarse ni cambiar de rol.");
    }

    @Override
    public String title() {
        return "Último administrador";
    }
}
