package com.kardexa.purchasing.infrastructure.config;

import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase;
import com.kardexa.purchasing.domain.model.Purchase;

/** Registra la compra dentro de su transacción, con los reintentos del ADR 0009. */
class RetryingRegisterPurchase implements RegisterPurchaseUseCase {

    private final RegisterPurchaseUseCase delegate;
    private final RetryingTransaction transaction;

    RetryingRegisterPurchase(RegisterPurchaseUseCase delegate, RetryingTransaction transaction) {
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public Purchase register(RegisterPurchaseCommand command) {
        return transaction.execute(() -> delegate.register(command));
    }
}
