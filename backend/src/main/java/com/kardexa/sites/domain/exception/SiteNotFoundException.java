package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.NotFoundException;
import java.util.UUID;

/** No existe una sede con ese identificador. */
public class SiteNotFoundException extends NotFoundException {

    public SiteNotFoundException(UUID id) {
        super("RF-26", "No existe la sede %s.".formatted(id));
    }

    @Override
    public String title() {
        return "Sede no encontrada";
    }
}
