package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Site;
import java.util.UUID;

/** HU-19 · Editar el nombre y la dirección de una sede (RF-26). */
public interface UpdateSiteUseCase {

    Site update(UpdateSiteCommand command);

    record UpdateSiteCommand(UUID id, String name, String address) {}
}
