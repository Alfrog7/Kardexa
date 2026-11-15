package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.NotFoundException;

/** La sede no tiene un turno abierto. */
public class NoOpenShiftException extends NotFoundException {

    public NoOpenShiftException() {
        super("RN-14", "No hay un turno abierto en esta sede.");
    }

    @Override
    public String title() {
        return "Sin turno abierto";
    }
}
