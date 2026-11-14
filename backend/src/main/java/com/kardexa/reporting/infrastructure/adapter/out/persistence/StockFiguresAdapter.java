package com.kardexa.reporting.infrastructure.adapter.out.persistence;

import com.kardexa.reporting.application.port.out.StockFigures;
import com.kardexa.reporting.domain.model.CriticalProduct;
import com.kardexa.shared.domain.Quantity;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Lee {@code product}, {@code category}, {@code product_stock} y {@code product_image} (módulos {@code catalog} e
 * {@code inventory}): lectura permitida por los ADR 0007 y 0012. Un producto sin fila de stock tiene stock 0.
 */
@Component
class StockFiguresAdapter implements StockFigures {

    private final JdbcClient jdbc;

    StockFiguresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CriticalProduct> criticalStock() {
        return jdbc.sql("""
                SELECT p.id, p.code, p.name, c.name AS category, coalesce(s.quantity, 0) AS stock, p.min_stock,
                       i.updated_at AS image_version
                FROM product p
                JOIN category c ON c.id = p.category_id
                LEFT JOIN product_stock s ON s.product_id = p.id
                LEFT JOIN product_image i ON i.product_id = p.id
                WHERE p.active
                  AND coalesce(s.quantity, 0) <= p.min_stock
                ORDER BY coalesce(s.quantity, 0) > 0, coalesce(s.quantity, 0) - p.min_stock, p.name, p.id""")
                .query((row, rowNumber) -> {
                    Timestamp image = row.getTimestamp("image_version");
                    return new CriticalProduct(row.getObject("id", UUID.class), row.getString("code"),
                            row.getString("name"), row.getString("category"),
                            new Quantity(row.getBigDecimal("stock")), new Quantity(row.getBigDecimal("min_stock")),
                            image == null ? null : image.toInstant());
                })
                .list();
    }
}
