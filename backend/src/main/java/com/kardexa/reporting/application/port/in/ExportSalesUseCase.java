package com.kardexa.reporting.application.port.in;

import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SaleRow;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

/** HU-18 · Exportar a CSV el historial de ventas con sus filtros (RF-25). */
public interface ExportSalesUseCase {

    /** Rango a exportar: sin fechas, los últimos 30 días; como máximo 366 (ADR 0012). */
    ReportPeriod period(LocalDate from, LocalDate to);

    /** {@code sellerId} y {@code status} son opcionales. */
    void sales(ReportPeriod period, UUID sellerId, String status, Consumer<SaleRow> sink);
}
