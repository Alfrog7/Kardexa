package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** No existe un turno con ese identificador. */
public class ShiftNotFoundException extends NotFoundException {

    public ShiftNotFoundException(UUID id) {
        super("RN-14", "No existe el turno %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Turno no encontrado";
    }
}
