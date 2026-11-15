package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Site;

/** HU-19 · Registrar una sede con un nombre que no exista todavía (RF-26). */
public interface CreateSiteUseCase {

    Site create(CreateSiteCommand command);

    record CreateSiteCommand(String name, String address) {}
}
