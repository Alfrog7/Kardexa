package com.kardexa.purchasing.infrastructure.config;

import com.kardexa.purchasing.application.port.in.VoidPurchaseUseCase;
import com.kardexa.purchasing.domain.model.Purchase;

/** Anula la compra dentro de su transacción, con los reintentos del ADR 0009. */
class RetryingVoidPurchase implements VoidPurchaseUseCase {

    private final VoidPurchaseUseCase delegate;
    private final RetryingTransaction transaction;

    RetryingVoidPurchase(VoidPurchaseUseCase delegate, RetryingTransaction transaction) {
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public Purchase voidPurchase(VoidPurchaseCommand command) {
        return transaction.execute(() -> delegate.voidPurchase(command));
    }
}
