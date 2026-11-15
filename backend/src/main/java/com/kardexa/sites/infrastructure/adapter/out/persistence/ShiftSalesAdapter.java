package com.kardexa.sites.infrastructure.adapter.out.persistence;

import com.kardexa.sites.application.port.out.ShiftSales;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Suma las ventas registradas de la sede con SQL de solo lectura. */
@Component
class ShiftSalesAdapter implements ShiftSales {

    private final JdbcClient jdbc;

    ShiftSalesAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BigDecimal registeredTotal(UUID siteId, Instant from, Instant to) {
        return jdbc.sql("""
                SELECT coalesce(sum(total), 0) FROM sale
                WHERE site_id = :siteId AND status = 'REGISTERED' AND sold_at >= :from AND sold_at < :to""")
                .param("siteId", siteId)
                .param("from", Timestamp.from(from))
                .param("to", Timestamp.from(to))
                .query(BigDecimal.class)
                .single()
                .setScale(2);
    }
}
