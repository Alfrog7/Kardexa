package com.kardexa.reporting.infrastructure.adapter.in.web;

import static com.kardexa.reporting.infrastructure.adapter.in.web.CsvResponses.DATE_TIME;

import com.kardexa.reporting.application.port.in.ExportSalesUseCase;
import com.kardexa.reporting.domain.model.ReportPeriod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * HU-18 · {@code GET /api/v1/sales/export}: el historial de ventas en CSV con los filtros de la pantalla (fechas y
 * vendedor) y, opcionalmente, el estado (ADR 0012). Solo {@code ADMIN}.
 */
@RestController
class SaleExportController {

    private final ExportSalesUseCase export;

    SaleExportController(ExportSalesUseCase export) {
        this.export = export;
    }

    @GetMapping("/sales/export")
    ResponseEntity<StreamingResponseBody> sales(@Valid @ParameterObject SaleExportParams params) {
        ReportPeriod period = export.period(params.from(), params.to());
        StreamingResponseBody body = stream -> {
            CsvWriter csv = new CsvWriter(stream);
            csv.row("Número", "Fecha y hora", "Vendedor", "Estado", "Productos", "Total", "Motivo de anulación");
            export.sales(period, params.sellerId(), params.status(), row -> csv.row(row.number(),
                    DATE_TIME.format(row.soldAt()), row.seller(), "VOIDED".equals(row.status()) ? "Anulada" : "Registrada",
                    String.valueOf(row.lines()), row.total().toString(), row.voidReason()));
            csv.flush();
        };
        return CsvResponses.csv("ventas-%s_%s.csv".formatted(period.from(), period.to()), body);
    }

    /** Fechas inclusivas en hora de Lima (sin fechas, los últimos 30 días), vendedor y estado opcionales. */
    record SaleExportParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            UUID sellerId,
            @Pattern(regexp = "REGISTERED|VOIDED", message = "El estado debe ser REGISTERED o VOIDED.") String status) {

        @AssertTrue(message = "La fecha inicial no puede ser posterior a la final.")
        public boolean isRange() {
            return CsvResponses.validRange(from, to);
        }

        @AssertTrue(message = "El rango puede tener como máximo " + ReportPeriod.MAX_DAYS + " días.")
        public boolean isWithinLimit() {
            return CsvResponses.withinLimit(from, to);
        }
    }
}
