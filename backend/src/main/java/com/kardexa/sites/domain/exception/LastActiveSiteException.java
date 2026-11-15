package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** El negocio debe conservar al menos una sede habilitada. */
public class LastActiveSiteException extends ConflictException {

    public LastActiveSiteException() {
        super("RF-26", "El negocio necesita al menos una sede habilitada: habilita otra antes de inhabilitar esta.");
    }

    @Override
    public String title() {
        return "Última sede habilitada";
    }
}
