package com.kardexa.reporting.application.port.in;

import com.kardexa.reporting.domain.model.MovementRow;
import com.kardexa.reporting.domain.model.ReportPeriod;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

/** HU-18 · Exportar a CSV el kárdex de un producto o el historial de movimientos (RF-25). */
public interface ExportMovementsUseCase {

    /** Rango a exportar: sin fechas, los últimos 30 días; como máximo 366 (ADR 0012). */
    ReportPeriod period(LocalDate from, LocalDate to);

    /** Código del producto para el nombre del archivo; si no existe lanza {@code ReportProductNotFoundException}. */
    String productCode(UUID productId);

    void kardex(UUID productId, ReportPeriod period, Consumer<MovementRow> sink);

    /** {@code type} es opcional: sin él, todos los tipos. */
    void movements(ReportPeriod period, String type, Consumer<MovementRow> sink);
}
