package com.kardexa.catalog.infrastructure.config;

import com.kardexa.catalog.application.port.in.UpdateReferenceCostUseCase;
import com.kardexa.shared.domain.Money;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR 0009: el costo se fija dentro de la transacción de la compra y falla si no hay ninguna
 * ({@code PROPAGATION_MANDATORY}), para que nunca quede un costo de una compra que no se guardó.
 */
class TransactionalUpdateReferenceCost implements UpdateReferenceCostUseCase {

    private final UpdateReferenceCostUseCase delegate;
    private final TransactionTemplate transaction;

    TransactionalUpdateReferenceCost(UpdateReferenceCostUseCase delegate, PlatformTransactionManager manager) {
        this.delegate = delegate;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    }

    @Override
    public void updateReferenceCosts(Map<UUID, Money> costsByProduct) {
        transaction.executeWithoutResult(status -> delegate.updateReferenceCosts(costsByProduct));
    }
}
