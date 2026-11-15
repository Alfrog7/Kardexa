package com.kardexa.sites.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Las ventas registradas una a una en una sede durante un turno. */
public interface ShiftSales {

    /** Suma de las ventas {@code REGISTERED} de la sede vendidas en {@code [from, to)}. */
    BigDecimal registeredTotal(UUID siteId, Instant from, Instant to);
}
