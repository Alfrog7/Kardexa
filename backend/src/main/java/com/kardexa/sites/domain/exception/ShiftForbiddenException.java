package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.ForbiddenException;

/** Solo el responsable del turno o un administrador pueden cerrarlo. */
public class ShiftForbiddenException extends ForbiddenException {

    public ShiftForbiddenException() {
        super("RN-14", "Solo quien abrió el turno o un administrador puede cerrarlo.");
    }

    @Override
    public String title() {
        return "Turno ajeno";
    }
}
