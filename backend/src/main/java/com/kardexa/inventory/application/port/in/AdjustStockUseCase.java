package com.kardexa.inventory.application.port.in;

import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.util.UUID;

/** HU-09 · Registrar un ajuste de stock (RF-11). Devuelve el movimiento de kárdex que generó. */
public interface AdjustStockUseCase {

    StockMovement adjust(AdjustStockCommand command);

    /** {@code userId} es quien registra el ajuste; nulo solo en procesos internos sin usuario. */
    record AdjustStockCommand(UUID productId, Direction direction, Quantity quantity, AdjustmentReason reason,
            String note, UUID userId) {

        public AdjustStockCommand(
                UUID productId, Direction direction, Quantity quantity, AdjustmentReason reason, String note) {
            this(productId, direction, quantity, reason, note, null);
        }
    }
}
