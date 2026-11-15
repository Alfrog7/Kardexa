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

    /** Guarda el cierre de un turno que seguía abierto, con el detalle de cada producto contado. */
    void saveClosed(Shift shift);
}
