package com.kardexa.inventory.domain.model;

import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una línea del kárdex: entrada o salida con el saldo que dejó (RN-02) y su documento de origen
 * (RN-04). Es inmutable (RN-03): un error se corrige con un movimiento inverso.
 *
 * @param userId quién lo generó; nulo en los registros anteriores a la autenticación
 */
public record StockMovement(
        UUID id,
        UUID productId,
        MovementType type,
        Direction direction,
        Quantity quantity,
        Quantity balanceAfter,
        SourceType sourceType,
        UUID sourceId,
        UUID userId,
        Instant occurredAt) {

    public StockMovement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(balanceAfter, "balanceAfter");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static StockMovement fromAdjustment(StockAdjustment adjustment, Quantity balanceAfter) {
        return new StockMovement(UUID.randomUUID(), adjustment.productId(), MovementType.ADJUSTMENT,
                adjustment.direction(), adjustment.quantity(), balanceAfter, SourceType.ADJUSTMENT, adjustment.id(),
                adjustment.userId(), adjustment.createdAt());
    }
}
