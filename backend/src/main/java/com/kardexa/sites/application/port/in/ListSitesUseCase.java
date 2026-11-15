package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Site;
import java.util.List;

/** HU-19 · Listar las sedes, habilitadas e inhabilitadas, ordenadas por nombre. */
public interface ListSitesUseCase {

    List<Site> list();
}
