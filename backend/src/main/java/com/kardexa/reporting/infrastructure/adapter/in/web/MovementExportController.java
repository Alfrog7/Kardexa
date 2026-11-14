package com.kardexa.reporting.infrastructure.adapter.in.web;

import static com.kardexa.reporting.infrastructure.adapter.in.web.CsvResponses.DATE_TIME;
import static com.kardexa.reporting.infrastructure.adapter.in.web.CsvResponses.csv;

import com.kardexa.reporting.application.port.in.ExportMovementsUseCase;
import com.kardexa.reporting.domain.model.MovementRow;
import com.kardexa.reporting.domain.model.ReportPeriod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * HU-18 · {@code GET /api/v1/products/{id}/kardex/export} y {@code GET /api/v1/stock-movements/export}: CSV con
 * las columnas de la pantalla, escrito por partes (ADR 0012). Solo {@code ADMIN}.
 */
@RestController
class MovementExportController {

    /** Mismos nombres que muestra Inventario. */
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "PURCHASE", "Compra",
            "SALE", "Venta",
            "ADJUSTMENT", "Ajuste",
            "PURCHASE_VOID", "Anulación de compra",
            "SALE_VOID", "Anulación de venta");

    private final ExportMovementsUseCase export;

    MovementExportController(ExportMovementsUseCase export) {
        this.export = export;
    }

    @GetMapping("/products/{id}/kardex/export")
    ResponseEntity<StreamingResponseBody> kardex(@PathVariable UUID id, @Valid @ParameterObject ExportParams params) {
        ReportPeriod period = export.period(params.from(), params.to());
        String code = export.productCode(id);
        StreamingResponseBody body = stream -> {
            CsvWriter csv = new CsvWriter(stream);
            csv.row("Fecha y hora", "Tipo", "Documento", "Entrada", "Salida", "Saldo", "Costo", "Usuario");
            export.kardex(id, period, row -> csv.row(DATE_TIME.format(row.occurredAt()), label(row), row.document(),
                    entry(row), exit(row), row.balanceAfter().toString(), cost(row), row.user()));
            csv.flush();
        };
        return csv("kardex-%s-%s_%s.csv".formatted(safe(code), period.from(), period.to()), body);
    }

    @GetMapping("/stock-movements/export")
    ResponseEntity<StreamingResponseBody> movements(@Valid @ParameterObject HistoryExportParams params) {
        ReportPeriod period = export.period(params.from(), params.to());
        StreamingResponseBody body = stream -> {
            CsvWriter csv = new CsvWriter(stream);
            csv.row("Fecha y hora", "Código", "Producto", "Tipo", "Documento", "Entrada", "Salida", "Saldo", "Costo",
                    "Usuario");
            export.movements(period, params.type(), row -> csv.row(DATE_TIME.format(row.occurredAt()),
                    row.productCode(), row.productName(), label(row), row.document(), entry(row), exit(row),
                    row.balanceAfter().toString(), cost(row), row.user()));
            csv.flush();
        };
        return csv("movimientos-%s_%s.csv".formatted(period.from(), period.to()), body);
    }

    private static String label(MovementRow row) {
        return TYPE_LABELS.getOrDefault(row.type(), row.type());
    }

    private static String entry(MovementRow row) {
        return row.entry() ? row.quantity().toString() : "";
    }

    private static String exit(MovementRow row) {
        return row.entry() ? "" : row.quantity().toString();
    }

    private static String cost(MovementRow row) {
        return row.unitCost() == null ? "" : row.unitCost().toString();
    }

    /** El código va en el nombre del archivo: solo letras, números, punto, guion y guion bajo. */
    private static String safe(String code) {
        return code.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** Fechas inclusivas en hora de Lima; sin fechas, los últimos 30 días. */
    record ExportParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        @AssertTrue(message = "La fecha inicial no puede ser posterior a la final.")
        public boolean isRange() {
            return CsvResponses.validRange(from, to);
        }

        @AssertTrue(message = "El rango puede tener como máximo " + ReportPeriod.MAX_DAYS + " días.")
        public boolean isWithinLimit() {
            return CsvResponses.withinLimit(from, to);
        }
    }

    /** Los filtros del historial: rango y, opcionalmente, el tipo de movimiento. */
    record HistoryExportParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Pattern(regexp = "PURCHASE|SALE|ADJUSTMENT|PURCHASE_VOID|SALE_VOID", message = "Tipo de movimiento desconocido.")
            String type) {

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
