package com.kardexa.reporting.infrastructure.adapter.out.persistence;

import com.kardexa.reporting.application.port.out.SalesFigures;
import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.shared.domain.Money;
import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Lee {@code sale} (módulo {@code sales}) para sumar lo vendido por día: lectura permitida por los ADR 0007 y
 * 0012. Solo cuenta ventas {@code REGISTERED}.
 */
@Component
class SalesFiguresAdapter implements SalesFigures {

    private final JdbcClient jdbc;

    SalesFiguresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * {@code generate_series} da una fila por día de Lima, también los días sin ventas. Cada día se convierte a
     * su rango de instantes para que el filtro por {@code sold_at} use el índice {@code ix_sale_sold_at}.
     *
     * <p>Los días son {@code timestamp} sin zona: con {@code date}, PostgreSQL elegiría la versión con zona de
     * la sesión y {@code AT TIME ZONE} convertiría al revés, dependiendo de la zona del servidor.
     */
    @Override
    public List<DailySales> dailySales(ReportPeriod period) {
        return jdbc.sql("""
                SELECT d.day::date AS day, coalesce(sum(s.total), 0) AS total, count(s.id) AS sales
                FROM generate_series(CAST(:from AS timestamp), CAST(:to AS timestamp), interval '1 day') AS d(day)
                LEFT JOIN sale s
                       ON s.status = 'REGISTERED'
                      AND s.sold_at >= (d.day AT TIME ZONE 'America/Lima')
                      AND s.sold_at < ((d.day + interval '1 day') AT TIME ZONE 'America/Lima')
                GROUP BY d.day
                ORDER BY d.day""")
                .param("from", Date.valueOf(period.from()))
                .param("to", Date.valueOf(period.to()))
                .query((row, rowNumber) -> new DailySales(row.getDate("day").toLocalDate(),
                        Money.of(row.getBigDecimal("total")), row.getLong("sales")))
                .list();
    }
}
