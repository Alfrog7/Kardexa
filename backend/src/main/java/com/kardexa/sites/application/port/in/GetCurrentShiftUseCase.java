package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Shift;
import java.util.UUID;

/** HU-22 · El turno abierto de una sede. Si no hay, lanza {@code NoOpenShiftException} (404). */
public interface GetCurrentShiftUseCase {

    Shift current(UUID siteId);
}
