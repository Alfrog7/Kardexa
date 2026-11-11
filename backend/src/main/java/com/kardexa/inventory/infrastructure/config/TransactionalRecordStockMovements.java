package com.kardexa.inventory.infrastructure.config;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR 0009: el caso de uso se une a la transacción de quien coordina el documento y falla si no hay
 * ninguna ({@code PROPAGATION_MANDATORY}). No reintenta: dentro de una transacción ajena no sirve.
 */
class TransactionalRecordStockMovements implements RecordStockMovementsUseCase {

    private final RecordStockMovementsUseCase delegate;
    private final TransactionTemplate transaction;

    TransactionalRecordStockMovements(RecordStockMovementsUseCase delegate, PlatformTransactionManager manager) {
        this.delegate = delegate;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    }

    @Override
    public void record(RecordStockMovementsCommand command) {
        transaction.executeWithoutResult(status -> delegate.record(command));
    }
}
