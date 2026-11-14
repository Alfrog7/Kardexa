package com.kardexa.reporting.domain.model;

import com.kardexa.reporting.domain.exception.InvalidReportPeriodException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Rango de fechas de Lima, inclusivo, de un indicador o reporte (ADR 0012). */
public record ReportPeriod(LocalDate from, LocalDate to) {

    /** Rango por defecto: los últimos 30 días, incluido hoy. */
    public static final int DEFAULT_DAYS = 30;
    /** Rango máximo permitido, en días. */
    public static final int MAX_DAYS = 366;

    public ReportPeriod {
        Objects.requireNonNull(from, "La fecha inicial es obligatoria");
        Objects.requireNonNull(to, "La fecha final es obligatoria");
        if (from.isAfter(to)) {
            throw new InvalidReportPeriodException("La fecha inicial no puede ser posterior a la final.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidReportPeriodException("El rango puede tener como máximo " + MAX_DAYS + " días.");
        }
    }

    /** Completa las fechas que faltan: sin {@code to}, hoy; sin {@code from}, 30 días hasta {@code to}. */
    public static ReportPeriod resolve(LocalDate from, LocalDate to, LocalDate today) {
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
        return new ReportPeriod(start, end);
    }
}
