package com.kardexa.reporting.application.port.out;

import com.kardexa.reporting.domain.model.MovementRow;
import com.kardexa.reporting.domain.model.ReportPeriod;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Movimientos de stock leídos fila a fila, sin cargarlos todos en memoria (ADR 0012). */
public interface MovementFigures {

    /** Código del producto, o vacío si no existe. */
    Optional<String> productCode(UUID productId);

    /** Movimientos del producto en el rango, en el orden del kárdex. */
    void kardex(UUID productId, ReportPeriod period, Consumer<MovementRow> sink);

    /** Movimientos de todos los productos en el rango, del más reciente al más antiguo; {@code type} opcional. */
    void movements(ReportPeriod period, String type, Consumer<MovementRow> sink);
}
