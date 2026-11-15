package com.kardexa.sites.application.port.out;

import com.kardexa.sites.domain.model.Site;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de sedes.
 *
 * <p>{@link #save} lanza {@code DuplicateSiteNameException} si otra sede ya tiene el nombre: es la garantía
 * final frente a dos altas simultáneas, aunque el servicio lo compruebe antes.
 */
public interface SiteRepository {

    Site save(Site site);

    Optional<Site> findById(UUID id);

    /** Todas las sedes ordenadas por nombre sin distinguir mayúsculas. */
    List<Site> findAllOrderedByName();

    boolean existsByName(String name);

    boolean existsByNameExcluding(String name, UUID excludedId);
}
