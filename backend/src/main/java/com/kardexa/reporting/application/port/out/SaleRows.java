package com.kardexa.reporting.application.port.out;

import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SaleRow;
import java.util.UUID;
import java.util.function.Consumer;

/** Ventas leídas fila a fila para exportarlas, sin cargarlas todas en memoria (ADR 0012). */
public interface SaleRows {

    /** Ventas del rango, de la más reciente a la más antigua; {@code sellerId} y {@code status} son opcionales. */
    void sales(ReportPeriod period, UUID sellerId, String status, Consumer<SaleRow> sink);
}
