package com.kardexa.reporting.application.service;

import com.kardexa.reporting.application.port.in.ExportSalesUseCase;
import com.kardexa.reporting.application.port.out.SaleRows;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SaleRow;
import com.kardexa.shared.domain.BusinessCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/** Exportación del historial de ventas (ADR 0012): valida el rango y entrega las filas una a una. */
public class SaleExportService implements ExportSalesUseCase {

    private final SaleRows rows;
    private final Clock clock;

    public SaleExportService(SaleRows rows, Clock clock) {
        this.rows = Objects.requireNonNull(rows);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public ReportPeriod period(LocalDate from, LocalDate to) {
        return ReportPeriod.resolve(from, to, LocalDate.now(clock.withZone(BusinessCalendar.ZONE)));
    }

    @Override
    public void sales(ReportPeriod period, UUID sellerId, String status, Consumer<SaleRow> sink) {
        rows.sales(period, sellerId, status, sink);
    }
}
