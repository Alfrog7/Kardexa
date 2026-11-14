package com.kardexa.reporting.infrastructure.adapter.out.persistence;

import com.kardexa.reporting.application.port.out.SalesFigures;
import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.TopProduct;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Lee {@code sale} y {@code sale_item} (módulo {@code sales}), y {@code product_image} para la miniatura, para sumar lo vendido por día y por producto: lectura permitida por los ADR 0007 y
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

    /**
     * Agrupa por producto las líneas de las ventas registradas del rango. El código y el nombre son los copiados
     * en la venta más reciente, así un producto renombrado o inhabilitado después sigue apareciendo.
     */
    @Override
    public List<TopProduct> topProducts(ReportPeriod period, int limit) {
        return jdbc.sql("""
                SELECT i.product_id,
                       (array_agg(i.product_code ORDER BY s.sold_at DESC))[1] AS code,
                       (array_agg(i.product_name ORDER BY s.sold_at DESC))[1] AS name,
                       sum(i.quantity) AS quantity, sum(i.subtotal) AS amount, max(img.updated_at) AS image_version
                FROM sale_item i
                JOIN sale s ON s.id = i.sale_id
                LEFT JOIN product_image img ON img.product_id = i.product_id
                WHERE s.status = 'REGISTERED'
                  AND s.sold_at >= (CAST(:from AS timestamp) AT TIME ZONE 'America/Lima')
                  AND s.sold_at < ((CAST(:to AS timestamp) + interval '1 day') AT TIME ZONE 'America/Lima')
                GROUP BY i.product_id
                ORDER BY quantity DESC, amount DESC, name, i.product_id
                LIMIT :limit""")
                .param("from", Date.valueOf(period.from()))
                .param("to", Date.valueOf(period.to()))
                .param("limit", limit)
                .query((row, rowNumber) -> {
                    Timestamp image = row.getTimestamp("image_version");
                    return new TopProduct(row.getObject("product_id", UUID.class), row.getString("code"),
                            row.getString("name"), new Quantity(row.getBigDecimal("quantity")),
                            Money.of(row.getBigDecimal("amount")), image == null ? null : image.toInstant());
                })
                .list();
    }
}
