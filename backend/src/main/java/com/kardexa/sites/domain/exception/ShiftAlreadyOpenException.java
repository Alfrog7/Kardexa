package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** Ya hay un turno abierto en la sede (RN-14). */
public class ShiftAlreadyOpenException extends ConflictException {

    public ShiftAlreadyOpenException() {
        super("RN-14", "Ya hay un turno abierto en esta sede: ciérralo antes de abrir otro.");
    }

    @Override
    public String title() {
        return "Turno ya abierto";
    }
}
