package com.kardexa.sales.infrastructure.config;

import com.kardexa.shared.domain.ConcurrentModificationException;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR 0010: abre la única transacción de una venta o de su anulación, a la que se unen el inventario y
 * el catálogo, y la repite completa hasta 3 veces si otro cambió el stock (ADR 0006); luego la API
 * responde 409. Cualquier otro error revierte todo y no se reintenta (RNF-04).
 */
final class RetryingTransaction {

    static final int MAX_ATTEMPTS = 3;

    private final TransactionTemplate transaction;

    RetryingTransaction(TransactionTemplate transaction) {
        this.transaction = transaction;
    }

    <T> T execute(Supplier<T> operation) {
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return transaction.execute(status -> operation.get());
            } catch (ConcurrentModificationException retryable) {
                // Se repite desde cero: se vuelven a leer el precio y el stock de cada producto.
            }
        }
        return transaction.execute(status -> operation.get());
    }
}
