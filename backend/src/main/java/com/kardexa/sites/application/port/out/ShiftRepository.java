package com.kardexa.sites.application.port.out;

import com.kardexa.sites.domain.model.Shift;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de turnos y su detalle por producto. */
public interface ShiftRepository {

    /**
     * Inserta un turno abierto.
     *
     * @throws com.kardexa.sites.domain.exception.ShiftAlreadyOpenException si la sede ya tiene uno abierto
     */
    void add(Shift shift);

    Optional<Shift> findById(UUID id);

    Optional<Shift> findOpenBySite(UUID siteId);

    /**
     * Turnos abiertos en el rango de instantes {@code [from, to)}, del más reciente al más antiguo, sin el detalle
     * por producto; {@code siteId} nulo: todas las sedes. A lo sumo {@code limit}.
     */
    java.util.List<Shift> search(java.time.Instant from, java.time.Instant to, UUID siteId, int limit);

    /** Guarda el cierre de un turno que seguía abierto, con el detalle de cada producto contado. */
    void saveClosed(Shift shift);
}
