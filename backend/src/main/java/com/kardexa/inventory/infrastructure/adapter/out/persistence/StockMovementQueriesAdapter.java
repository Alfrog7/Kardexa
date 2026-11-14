package com.kardexa.inventory.infrastructure.adapter.out.persistence;

import com.kardexa.inventory.application.port.in.ListStockMovementsUseCase.MovementEntry;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.HashMap;
import java.util.Map;
import com.kardexa.inventory.application.port.out.StockMovementQueries;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.inventory.domain.model.SourceType;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Lee {@code stock_movement}, ordenado por {@code seq}: el orden en que se calcularon los saldos. */
@Component
class StockMovementQueriesAdapter implements StockMovementQueries {

    private final JdbcClient jdbc;

    StockMovementQueriesAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Quantity balanceBefore(UUID productId, Instant instant) {
        return jdbc.sql("""
                SELECT balance_after FROM stock_movement
                WHERE product_id = :productId AND occurred_at < :instant
                ORDER BY seq DESC LIMIT 1""")
                .param("productId", productId)
                .param("instant", Timestamp.from(instant))
                .query(BigDecimal.class)
                .optional()
                .map(Quantity::new)
                .orElse(Quantity.ZERO);
    }

    @Override
    public List<StockMovement> findMovements(UUID productId, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder("SELECT * FROM stock_movement WHERE product_id = :productId");
        if (from != null) {
            sql.append(" AND occurred_at >= :from");
        }
        if (to != null) {
            sql.append(" AND occurred_at < :to");
        }
        JdbcClient.StatementSpec query = jdbc.sql(sql.append(" ORDER BY seq").toString()).param("productId", productId);
        if (from != null) {
            query = query.param("from", Timestamp.from(from));
        }
        if (to != null) {
            query = query.param("to", Timestamp.from(to));
        }
        return query.query((row, rowNumber) -> toMovement(row)).list();
    }

    /**
     * Historial general. Lee {@code product} (módulo {@code catalog}) solo para mostrar el código y el
     * nombre: lectura permitida por el ADR 0007.
     */
    @Override
    public ResultPage<MovementEntry> findAll(Instant from, Instant to, MovementType type, PageQuery page) {
        StringBuilder where = new StringBuilder(" WHERE true");
        Map<String, Object> params = new HashMap<>();
        if (from != null) {
            where.append(" AND m.occurred_at >= :from");
            params.put("from", Timestamp.from(from));
        }
        if (to != null) {
            where.append(" AND m.occurred_at < :to");
            params.put("to", Timestamp.from(to));
        }
        if (type != null) {
            where.append(" AND m.type = :type");
            params.put("type", type.name());
        }
        long total = jdbc.sql("SELECT count(*) FROM stock_movement m" + where).params(params)
                .query(Long.class).single();
        List<MovementEntry> content = jdbc.sql("SELECT m.*, p.code AS product_code, p.name AS product_name"
                        + " FROM stock_movement m JOIN product p ON p.id = m.product_id" + where
                        + " ORDER BY m.occurred_at DESC, m.seq DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .param("limit", page.size())
                .param("offset", (long) page.page() * page.size())
                .query((row, rowNumber) -> new MovementEntry(
                        toMovement(row), row.getString("product_code"), row.getString("product_name")))
                .list();
        return ResultPage.of(content, page, total);
    }

    private static StockMovement toMovement(ResultSet row) throws SQLException {
        return new StockMovement(
                row.getObject("id", UUID.class),
                row.getObject("product_id", UUID.class),
                MovementType.valueOf(row.getString("type")),
                Direction.valueOf(row.getString("direction")),
                new Quantity(row.getBigDecimal("quantity")),
                new Quantity(row.getBigDecimal("balance_after")),
                SourceType.valueOf(row.getString("source_type")),
                row.getObject("source_id", UUID.class),
                row.getObject("user_id", UUID.class),
                row.getTimestamp("occurred_at").toInstant());
    }
}
