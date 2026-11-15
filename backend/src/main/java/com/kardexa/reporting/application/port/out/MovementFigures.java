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
    /** El kárdex es de una sola sede: el saldo de cada movimiento es el de esa sede (RN-13). */
    void kardex(UUID productId, UUID siteId, ReportPeriod period, Consumer<MovementRow> sink);

    /** Movimientos de todos los productos en el rango, del más reciente al más antiguo; {@code type} opcional. */
    /** {@code siteId} nulo: los movimientos de todas las sedes. */
    void movements(UUID siteId, ReportPeriod period, String type, Consumer<MovementRow> sink);
}
