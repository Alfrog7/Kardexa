package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Site;
import java.util.UUID;

/** HU-20 · Leer una sede por su identificador (la trabajadora lee la suya). */
public interface GetSiteUseCase {

    Site get(UUID id);
}
