package com.kardexa.inventory.infrastructure.adapter.in.web;

import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.inventory.domain.model.StockMovement;
import java.time.Instant;
import java.util.UUID;

/** Esquema {@code StockMovement}. Las cantidades viajan como texto para no perder precisión. */
record StockMovementResponse(
        UUID id,
        Instant occurredAt,
        MovementType type,
        Direction direction,
        String quantity,
        String balanceAfter,
        UUID sourceId,
        String sourceLabel,
        String user) {

    /** Hasta que exista la autenticación (Fase 3), los movimientos no tienen un usuario real. */
    static final String SYSTEM_USER = "Sistema";

    static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(movement.id(), movement.occurredAt(), movement.type(), movement.direction(),
                movement.quantity().toString(), movement.balanceAfter().toString(), movement.sourceId(),
                label(movement), SYSTEM_USER);
    }

    /** Las compras y ventas mostrarán su número de documento cuando existan (fases 5 y 6). */
    private static String label(StockMovement movement) {
        return switch (movement.sourceType()) {
            case ADJUSTMENT -> "Ajuste";
            case PURCHASE -> "Compra";
            case SALE -> "Venta";
        };
    }
}
