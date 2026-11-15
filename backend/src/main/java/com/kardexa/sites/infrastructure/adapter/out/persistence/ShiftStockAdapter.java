package com.kardexa.sites.infrastructure.adapter.out.persistence;

import com.kardexa.shared.domain.Quantity;
import com.kardexa.sites.application.port.out.ShiftStock;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Lee el stock por sede y el precio del catálogo con SQL de solo lectura (como {@code reporting}): el turno solo
 * necesita una foto, y las escrituras del kárdex pasan por el puerto del inventario.
 */
@Component
class ShiftStockAdapter implements ShiftStock {

    private final JdbcClient jdbc;

    ShiftStockAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<StockLine> withStock(UUID siteId) {
        return jdbc.sql("""
                SELECT p.id, p.name, p.sale_price, ps.quantity
                FROM product_stock ps JOIN product p ON p.id = ps.product_id
                WHERE ps.site_id = :siteId AND ps.quantity > 0 AND p.active
                ORDER BY p.name""")
                .param("siteId", siteId)
                .query((row, n) -> new StockLine(row.getObject("id", UUID.class), row.getString("name"),
                        row.getBigDecimal("sale_price"), new Quantity(row.getBigDecimal("quantity"))))
                .list();
    }

    @Override
    public Map<UUID, StockLine> lock(UUID siteId, Collection<UUID> productIds) {
        Map<UUID, StockLine> lines = new HashMap<>();
        for (UUID productId : productIds.stream().sorted().toList()) {
            // FOR UPDATE bloquea la fila de stock hasta el final de la transacción: nadie vende entre esta lectura y
            // el cierre. Un producto sin fila de stock figura con 0.
            Quantity quantity = jdbc.sql("""
                    SELECT quantity FROM product_stock WHERE product_id = :productId AND site_id = :siteId FOR UPDATE""")
                    .param("siteId", siteId)
                    .param("productId", productId)
                    .query(java.math.BigDecimal.class)
                    .optional()
                    .map(Quantity::new)
                    .orElse(Quantity.ZERO);
            jdbc.sql("SELECT id, name, sale_price FROM product WHERE id = :productId")
                    .param("productId", productId)
                    .query((row, n) -> new StockLine(row.getObject("id", UUID.class), row.getString("name"),
                            row.getBigDecimal("sale_price"), quantity))
                    .optional()
                    .ifPresent(line -> lines.put(productId, line));
        }
        return lines;
    }
}
