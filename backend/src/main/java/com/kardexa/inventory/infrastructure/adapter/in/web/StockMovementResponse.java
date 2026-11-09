package com.kardexa.inventory.infrastructure.adapter.in.web;

import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.inventory.domain.model.StockMovement;
import java.time.Instant;
import java.util.Map;
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

    /** Los movimientos anteriores a la autenticación no tienen usuario. */
    static final String SYSTEM_USER = "Sistema";

    static StockMovementResponse from(StockMovement movement, Map<UUID, String> userNames) {
        return new StockMovementResponse(movement.id(), movement.occurredAt(), movement.type(), movement.direction(),
                movement.quantity().toString(), movement.balanceAfter().toString(), movement.sourceId(),
                label(movement), userName(movement, userNames));
    }

    private static String userName(StockMovement movement, Map<UUID, String> userNames) {
        return movement.userId() == null ? SYSTEM_USER : userNames.getOrDefault(movement.userId(), SYSTEM_USER);
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
