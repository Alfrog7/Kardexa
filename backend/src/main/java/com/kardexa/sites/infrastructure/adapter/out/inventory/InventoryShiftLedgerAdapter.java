package com.kardexa.sites.infrastructure.adapter.out.inventory;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.DocumentMovement;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.RecordStockMovementsCommand;
import com.kardexa.sites.application.port.out.ShiftLedger;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Traduce el puerto {@link ShiftLedger} al caso de uso del inventario, por su puerto de entrada. */
@Component
class InventoryShiftLedgerAdapter implements ShiftLedger {

    private final RecordStockMovementsUseCase recordStockMovements;

    InventoryShiftLedgerAdapter(RecordStockMovementsUseCase recordStockMovements) {
        this.recordStockMovements = recordStockMovements;
    }

    @Override
    public void recordSold(UUID shiftId, UUID siteId, UUID userId, List<Sold> lines) {
        recordStockMovements.record(new RecordStockMovementsCommand(DocumentMovement.SHIFT_CLOSE, shiftId, siteId,
                userId, lines.stream().map(sold -> new Line(sold.productId(), sold.quantity())).toList()));
    }
}
