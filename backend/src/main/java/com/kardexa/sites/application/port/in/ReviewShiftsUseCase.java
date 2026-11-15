package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Shift;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** HU-24 · El dueño revisa los turnos de sus sedes: la lista del rango y el detalle de uno. */
public interface ReviewShiftsUseCase {

    /** Cantidad máxima de turnos que devuelve la lista. */
    int LIMIT = 200;

    /**
     * Turnos abiertos dentro del rango de fechas (hora de Lima, ambos días incluidos), del más reciente al más
     * antiguo y sin el detalle por producto. {@code siteId} nulo: todas las sedes.
     */
    List<Shift> list(LocalDate from, LocalDate to, UUID siteId);

    /** Un turno con lo vendido por producto. */
    Shift get(UUID id);
}
