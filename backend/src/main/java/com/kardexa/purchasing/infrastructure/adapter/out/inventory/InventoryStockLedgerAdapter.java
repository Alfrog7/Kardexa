package com.kardexa.purchasing.infrastructure.adapter.out.inventory;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.DocumentMovement;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.RecordStockMovementsCommand;
import com.kardexa.purchasing.application.port.out.StockLedger;
import com.kardexa.purchasing.domain.model.Purchase;
import org.springframework.stereotype.Component;

/** Traduce el puerto {@link StockLedger} al caso de uso del inventario, por su puerto de entrada. */
@Component
class InventoryStockLedgerAdapter implements StockLedger {

    private final RecordStockMovementsUseCase recordStockMovements;

    InventoryStockLedgerAdapter(RecordStockMovementsUseCase recordStockMovements) {
        this.recordStockMovements = recordStockMovements;
    }

    @Override
    public void receive(Purchase purchase) {
        recordStockMovements.record(new RecordStockMovementsCommand(DocumentMovement.PURCHASE, purchase.id(),
                purchase.userId(),
                purchase.items().stream().map(i -> new Line(i.productId(), i.quantity())).toList()));
    }
}
