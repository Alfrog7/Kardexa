package com.kardexa.reporting.application.service;

import com.kardexa.reporting.application.port.in.ExportMovementsUseCase;
import com.kardexa.reporting.application.port.out.MovementFigures;
import com.kardexa.reporting.domain.exception.ReportProductNotFoundException;
import com.kardexa.reporting.domain.model.MovementRow;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.shared.domain.BusinessCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/** Exportación de movimientos de stock (ADR 0012): valida el rango y entrega las filas una a una. */
public class MovementExportService implements ExportMovementsUseCase {

    private final MovementFigures movements;
    private final Clock clock;

    public MovementExportService(MovementFigures movements, Clock clock) {
        this.movements = Objects.requireNonNull(movements);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public ReportPeriod period(LocalDate from, LocalDate to) {
        return ReportPeriod.resolve(from, to, LocalDate.now(clock.withZone(BusinessCalendar.ZONE)));
    }

    @Override
    public String productCode(UUID productId) {
        return movements.productCode(productId).orElseThrow(() -> new ReportProductNotFoundException(productId));
    }

    @Override
    public void kardex(UUID productId, ReportPeriod period, Consumer<MovementRow> sink) {
        movements.kardex(productId, period, sink);
    }

    @Override
    public void movements(ReportPeriod period, String type, Consumer<MovementRow> sink) {
        movements.movements(period, type, sink);
    }
}
