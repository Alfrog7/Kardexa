package com.kardexa.inventory.infrastructure.config;

import com.kardexa.inventory.application.port.in.AdjustStockUseCase;
import com.kardexa.inventory.domain.exception.StaleStockException;
import com.kardexa.inventory.domain.model.StockMovement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Da al caso de uso su frontera transaccional sin meter Spring en {@code application}, y aplica el
 * ADR 0006: si otro cambió el stock, repite todo en una transacción nueva, hasta 3 intentos, antes de
 * dejar que la API responda 409. Cualquier otro error revierte la transacción y no se reintenta.
 */
class RetryingAdjustStock implements AdjustStockUseCase {

    static final int MAX_ATTEMPTS = 3;

    private final AdjustStockUseCase delegate;
    private final TransactionTemplate transaction;

    RetryingAdjustStock(AdjustStockUseCase delegate, TransactionTemplate transaction) {
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public StockMovement adjust(AdjustStockCommand command) {
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return transaction.execute(status -> delegate.adjust(command));
            } catch (StaleStockException retryable) {
                // Se vuelve a leer el stock: una salida que ya no alcance fallará por RN-01.
            }
        }
        return transaction.execute(status -> delegate.adjust(command));
    }
}
