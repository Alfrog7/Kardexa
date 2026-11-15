package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** Ya existe una sede con ese nombre (sin distinguir mayúsculas). */
public class DuplicateSiteNameException extends ConflictException {

    public DuplicateSiteNameException(String name) {
        super("RF-26", "Ya existe una sede llamada \"%s\".".formatted(name));
    }

    @Override
    public String title() {
        return "Nombre de sede duplicado";
    }
}
