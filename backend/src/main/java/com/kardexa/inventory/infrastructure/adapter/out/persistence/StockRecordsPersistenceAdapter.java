package com.kardexa.inventory.infrastructure.adapter.out.persistence;

import com.kardexa.inventory.application.port.out.StockRecords;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Inserta ajustes y movimientos. La base rechaza cualquier cambio posterior (RN-03, migración V5). */
@Component
class StockRecordsPersistenceAdapter implements StockRecords {

    private final JdbcClient jdbc;

    StockRecordsPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(StockAdjustment adjustment) {
        jdbc.sql("""
                INSERT INTO stock_adjustment
                    (id, product_id, direction, quantity, reason, note, user_id, created_at)
                VALUES (:id, :productId, :direction, :quantity, :reason, :note, :userId, :createdAt)""")
                .param("id", adjustment.id())
                .param("productId", adjustment.productId())
                .param("direction", adjustment.direction().name())
                .param("quantity", adjustment.quantity().value())
                .param("reason", adjustment.reason().name())
                .param("note", adjustment.note(), java.sql.Types.VARCHAR)
                .param("userId", adjustment.userId(), java.sql.Types.OTHER)
                .param("createdAt", Timestamp.from(adjustment.createdAt()))
                .update();
    }

    @Override
    public void add(StockMovement movement) {
        jdbc.sql("""
                INSERT INTO stock_movement
                    (id, product_id, type, direction, quantity, balance_after, source_type, source_id, user_id, occurred_at)
                VALUES (:id, :productId, :type, :direction, :quantity, :balanceAfter, :sourceType, :sourceId,
                        :userId, :occurredAt)""")
                .param("id", movement.id())
                .param("productId", movement.productId())
                .param("type", movement.type().name())
                .param("direction", movement.direction().name())
                .param("quantity", movement.quantity().value())
                .param("balanceAfter", movement.balanceAfter().value())
                .param("sourceType", movement.sourceType().name())
                .param("sourceId", movement.sourceId())
                .param("userId", movement.userId(), java.sql.Types.OTHER)
                .param("occurredAt", Timestamp.from(movement.occurredAt()))
                .update();
    }
}
