package com.kardexa.shared.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Las fechas del negocio se entienden en hora de Lima (RNF-15); en la base todo se guarda como instante. */
public final class BusinessCalendar {

    public static final ZoneId ZONE = ZoneId.of("America/Lima");

    private BusinessCalendar() {
    }

    /** Primer instante de esa fecha en Lima. */
    public static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    /** Primer instante del día siguiente: sirve de límite exclusivo para incluir toda la fecha. */
    public static Instant endOf(LocalDate date) {
        return startOf(date.plusDays(1));
    }
}
