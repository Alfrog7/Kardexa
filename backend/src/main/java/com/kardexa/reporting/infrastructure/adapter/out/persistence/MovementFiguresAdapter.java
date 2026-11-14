package com.kardexa.reporting.infrastructure.adapter.out.persistence;

import com.kardexa.reporting.application.port.out.MovementFigures;
import com.kardexa.reporting.domain.model.MovementRow;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.shared.domain.BusinessCalendar;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lee {@code stock_movement} (módulo {@code inventory}) con el producto, el documento de origen ({@code sale},
 * {@code purchase}), el costo de compra ({@code purchase_item}) y el usuario ({@code app_user}): lectura
 * permitida por los ADR 0007 y 0012.
 *
 * <p>PostgreSQL solo entrega las filas por partes dentro de una transacción y con un {@code fetchSize}; así el
 * CSV se escribe mientras se lee, sin cargar todo el resultado en memoria.
 */
@Component
class MovementFiguresAdapter implements MovementFigures {

    private static final int FETCH_SIZE = 500;

    private static final String SELECT = """
            SELECT m.occurred_at, p.code, p.name, m.type, m.direction, m.quantity, m.balance_after,
                   CASE m.source_type
                       WHEN 'SALE' THEN s.number
                       WHEN 'PURCHASE' THEN coalesce(pu.supplier_document, 'Compra')
                       ELSE 'Ajuste'
                   END AS document,
                   pi.unit_cost,
                   coalesce(u.full_name, 'Sistema') AS user_name
            FROM stock_movement m
            JOIN product p ON p.id = m.product_id
            LEFT JOIN sale s ON m.source_type = 'SALE' AND s.id = m.source_id
            LEFT JOIN purchase pu ON m.source_type = 'PURCHASE' AND pu.id = m.source_id
            LEFT JOIN purchase_item pi
                   ON m.source_type = 'PURCHASE' AND pi.purchase_id = m.source_id AND pi.product_id = m.product_id
            LEFT JOIN app_user u ON u.id = m.user_id
            WHERE m.occurred_at >= :from AND m.occurred_at < :to
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate readOnly;

    MovementFiguresAdapter(DataSource dataSource, PlatformTransactionManager transactions) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setFetchSize(FETCH_SIZE);
        this.jdbc = new NamedParameterJdbcTemplate(template);
        this.readOnly = new TransactionTemplate(transactions);
        this.readOnly.setReadOnly(true);
    }

    @Override
    public Optional<String> productCode(UUID productId) {
        return jdbc.queryForList("SELECT code FROM product WHERE id = :id", new MapSqlParameterSource("id", productId),
                String.class).stream().findFirst();
    }

    @Override
    public void kardex(UUID productId, ReportPeriod period, Consumer<MovementRow> sink) {
        stream(SELECT + " AND m.product_id = :productId ORDER BY m.seq",
                range(period).addValue("productId", productId), sink);
    }

    @Override
    public void movements(ReportPeriod period, String type, Consumer<MovementRow> sink) {
        MapSqlParameterSource params = range(period);
        String sql = SELECT;
        if (type != null) {
            sql += " AND m.type = :type";
            params.addValue("type", type);
        }
        stream(sql + " ORDER BY m.occurred_at DESC, m.seq DESC", params, sink);
    }

    private void stream(String sql, MapSqlParameterSource params, Consumer<MovementRow> sink) {
        readOnly.executeWithoutResult(status -> jdbc.query(sql, params, row -> {
            sink.accept(toRow(row));
        }));
    }

    private static MapSqlParameterSource range(ReportPeriod period) {
        return new MapSqlParameterSource()
                .addValue("from", Timestamp.from(BusinessCalendar.startOf(period.from())))
                .addValue("to", Timestamp.from(BusinessCalendar.endOf(period.to())));
    }

    private static MovementRow toRow(ResultSet row) throws SQLException {
        BigDecimal cost = row.getBigDecimal("unit_cost");
        return new MovementRow(row.getTimestamp("occurred_at").toInstant(), row.getString("code"),
                row.getString("name"), row.getString("type"), "IN".equals(row.getString("direction")),
                new Quantity(row.getBigDecimal("quantity")), new Quantity(row.getBigDecimal("balance_after")),
                row.getString("document"), cost == null ? null : Money.of(cost), row.getString("user_name"));
    }
}
