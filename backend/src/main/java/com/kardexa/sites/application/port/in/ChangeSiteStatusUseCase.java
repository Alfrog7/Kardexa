package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Site;
import java.util.UUID;

/** HU-19 · Inhabilitar o volver a habilitar una sede; conserva su historial (RF-26). */
public interface ChangeSiteStatusUseCase {

    Site changeStatus(UUID id, boolean active);
}
