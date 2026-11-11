package com.kardexa.purchasing.infrastructure.config;

import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.shared.domain.ConcurrentModificationException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR 0009: abre la única transacción de la compra, a la que se unen el inventario y el catálogo, y
 * la repite completa hasta 3 veces si otro cambió el stock (ADR 0006); luego la API responde 409.
 * Cualquier otro error revierte todo y no se reintenta (RNF-04).
 */
class RetryingRegisterPurchase implements RegisterPurchaseUseCase {

    static final int MAX_ATTEMPTS = 3;

    private final RegisterPurchaseUseCase delegate;
    private final TransactionTemplate transaction;

    RetryingRegisterPurchase(RegisterPurchaseUseCase delegate, TransactionTemplate transaction) {
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public Purchase register(RegisterPurchaseCommand command) {
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return transaction.execute(status -> delegate.register(command));
            } catch (ConcurrentModificationException retryable) {
                // Se repite desde cero: se vuelve a leer el stock de cada producto.
            }
        }
        return transaction.execute(status -> delegate.register(command));
    }
}
