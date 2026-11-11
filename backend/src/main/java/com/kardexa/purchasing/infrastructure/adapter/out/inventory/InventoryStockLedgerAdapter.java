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
        record(DocumentMovement.PURCHASE, purchase, purchase.userId());
    }

    /** Las salidas inversas llevan el mismo id de compra como origen y el usuario que anula. */
    @Override
    public void reverse(Purchase purchase) {
        record(DocumentMovement.PURCHASE_VOID, purchase, purchase.voiding().userId());
    }

    private void record(DocumentMovement movement, Purchase purchase, java.util.UUID userId) {
        recordStockMovements.record(new RecordStockMovementsCommand(movement, purchase.id(), userId,
                purchase.items().stream().map(i -> new Line(i.productId(), i.quantity())).toList()));
    }
}
