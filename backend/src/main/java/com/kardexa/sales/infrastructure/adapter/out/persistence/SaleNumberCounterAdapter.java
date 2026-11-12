package com.kardexa.sales.infrastructure.adapter.out.persistence;

import com.kardexa.sales.application.port.out.SaleNumbers;
import com.kardexa.sales.domain.model.SaleNumber;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * ADR 0010: el {@code UPDATE} bloquea la fila del contador hasta el commit, así dos ventas nunca
 * reciben el mismo número, y un rollback devuelve el número sin dejar hueco.
 */
@Component
class SaleNumberCounterAdapter implements SaleNumbers {

    private final JdbcClient jdbc;

    SaleNumberCounterAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SaleNumber next() {
        long value = jdbc.sql("UPDATE sale_number_counter SET last_value = last_value + 1 WHERE id = 1 RETURNING last_value")
                .query(Long.class)
                .single();
        return new SaleNumber(value);
    }
}
