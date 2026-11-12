package com.kardexa.sales.infrastructure.config;

import com.kardexa.sales.application.port.in.RegisterSaleUseCase;
import com.kardexa.sales.domain.model.Sale;

/** Registra la venta dentro de su transacción, con los reintentos del ADR 0010. */
class RetryingRegisterSale implements RegisterSaleUseCase {

    private final RegisterSaleUseCase delegate;
    private final RetryingTransaction transaction;

    RetryingRegisterSale(RegisterSaleUseCase delegate, RetryingTransaction transaction) {
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public Sale register(RegisterSaleCommand command) {
        return transaction.execute(() -> delegate.register(command));
    }
}
