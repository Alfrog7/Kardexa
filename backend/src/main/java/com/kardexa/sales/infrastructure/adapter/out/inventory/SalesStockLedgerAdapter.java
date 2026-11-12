package com.kardexa.sales.infrastructure.adapter.out.inventory;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.DocumentMovement;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.RecordStockMovementsCommand;
import com.kardexa.sales.application.port.out.StockLedger;
import com.kardexa.sales.domain.model.Sale;
import org.springframework.stereotype.Component;

/** Traduce el puerto {@link StockLedger} de ventas al caso de uso del inventario, por su puerto de entrada. */
@Component
class SalesStockLedgerAdapter implements StockLedger {

    private final RecordStockMovementsUseCase recordStockMovements;

    SalesStockLedgerAdapter(RecordStockMovementsUseCase recordStockMovements) {
        this.recordStockMovements = recordStockMovements;
    }

    @Override
    public void withdraw(Sale sale) {
        record(DocumentMovement.SALE, sale, sale.sellerId());
    }

    @Override
    public void giveBack(Sale sale) {
        record(DocumentMovement.SALE_VOID, sale, sale.voiding().userId());
    }

    private void record(DocumentMovement movement, Sale sale, java.util.UUID userId) {
        recordStockMovements.record(new RecordStockMovementsCommand(movement, sale.id(), userId,
                sale.items().stream().map(i -> new Line(i.productId(), i.quantity())).toList()));
    }
}
