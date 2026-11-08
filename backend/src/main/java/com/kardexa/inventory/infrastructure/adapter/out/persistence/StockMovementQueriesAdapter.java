package com.kardexa.inventory.infrastructure.adapter.out.persistence;

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
