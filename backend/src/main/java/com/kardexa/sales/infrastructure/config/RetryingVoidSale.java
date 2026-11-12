package com.kardexa.sales.infrastructure.config;

import com.kardexa.sales.application.port.in.VoidSaleUseCase;
import com.kardexa.sales.domain.model.Sale;

/** Anula la venta dentro de su transacción, con los reintentos del ADR 0010. */
class RetryingVoidSale implements VoidSaleUseCase {

    private final VoidSaleUseCase delegate;
    private final RetryingTransaction transaction;

    RetryingVoidSale(VoidSaleUseCase delegate, RetryingTransaction transaction) {
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public Sale voidSale(VoidSaleCommand command) {
        return transaction.execute(() -> delegate.voidSale(command));
    }
}
