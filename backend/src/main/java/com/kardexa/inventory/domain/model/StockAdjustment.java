package com.kardexa.inventory.domain.model;

import com.kardexa.inventory.domain.exception.InvalidStockAdjustmentException;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Corrección manual del stock por merma, conteo físico o corrección (RF-11). Es inmutable (RN-03).
 *
 * @param userId quién la registró; nulo en los registros anteriores a la autenticación
 */
public record StockAdjustment(
        UUID id,
        UUID productId,
        Direction direction,
        Quantity quantity,
        AdjustmentReason reason,
        String note,
        UUID userId,
        Instant createdAt) {

    public static final int NOTE_MAX_LENGTH = 255;

    public StockAdjustment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(createdAt, "createdAt");
        if (!quantity.isPositive()) {
            throw new InvalidStockAdjustmentException("La cantidad del ajuste debe ser mayor que 0.");
        }
        if (reason == null) {
            throw new InvalidStockAdjustmentException("El ajuste debe indicar un motivo.");
        }
        note = note == null || note.isBlank() ? null : note.strip();
        if (note != null && note.length() > NOTE_MAX_LENGTH) {
            throw new InvalidStockAdjustmentException(
                    "La nota debe tener como máximo " + NOTE_MAX_LENGTH + " caracteres.");
        }
    }

    public static StockAdjustment create(
            UUID productId, Direction direction, Quantity quantity, AdjustmentReason reason, String note, Instant now) {
        return create(productId, direction, quantity, reason, note, null, now);
    }

    public static StockAdjustment create(UUID productId, Direction direction, Quantity quantity,
            AdjustmentReason reason, String note, UUID userId, Instant now) {
        return new StockAdjustment(UUID.randomUUID(), productId, direction, quantity, reason, note, userId, now);
    }
}
