package com.kardexa.inventory.infrastructure.config;

import com.kardexa.inventory.application.port.in.RegisterReceiptUseCase;
import com.kardexa.shared.domain.ConcurrentModificationException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Da a la entrada su transacción y aplica el ADR 0006: si otro cambió el stock, repite todo en una
 * transacción nueva, hasta 3 intentos, antes de dejar que la API responda 409.
 */
class RetryingRegisterReceipt implements RegisterReceiptUseCase {

    static final int MAX_ATTEMPTS = 3;

    private final RegisterReceiptUseCase delegate;
    private final TransactionTemplate transaction;

    RetryingRegisterReceipt(RegisterReceiptUseCase delegate, TransactionTemplate transaction) {
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public Receipt register(RegisterReceiptCommand command) {
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return transaction.execute(status -> delegate.register(command));
            } catch (ConcurrentModificationException retryable) {
                // Se vuelve a leer el stock y se intenta de nuevo.
            }
        }
        return transaction.execute(status -> delegate.register(command));
    }
}
