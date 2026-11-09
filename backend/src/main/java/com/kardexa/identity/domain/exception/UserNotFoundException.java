package com.kardexa.identity.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** No existe un usuario con ese identificador. */
public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(UUID id) {
        super("RF-03", "No existe el usuario %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Usuario no encontrado";
    }
}
