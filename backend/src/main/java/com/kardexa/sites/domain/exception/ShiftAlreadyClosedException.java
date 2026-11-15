package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** Un turno cerrado no se modifica ni se reabre (RN-03, RN-14). */
public class ShiftAlreadyClosedException extends ConflictException {

    public ShiftAlreadyClosedException() {
        super("RN-14", "El turno ya está cerrado y no se puede modificar.");
    }

    @Override
    public String title() {
        return "Turno ya cerrado";
    }
}
