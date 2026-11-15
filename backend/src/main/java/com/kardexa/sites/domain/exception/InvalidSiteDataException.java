package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** El nombre o la dirección de la sede están fuera de lo permitido. */
public class InvalidSiteDataException extends DomainException {

    public InvalidSiteDataException(String message) {
        super("RF-26", message);
    }

    @Override
    public String title() {
        return "Datos de sede inválidos";
    }
}
