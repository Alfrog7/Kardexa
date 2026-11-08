package com.kardexa.inventory.application.port.in;

import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.util.UUID;

/** HU-09 · Registrar un ajuste de stock (RF-11). Devuelve el movimiento de kárdex que generó. */
public interface AdjustStockUseCase {

    StockMovement adjust(AdjustStockCommand command);

    record AdjustStockCommand(
            UUID productId, Direction direction, Quantity quantity, AdjustmentReason reason, String note) {
    }
}
