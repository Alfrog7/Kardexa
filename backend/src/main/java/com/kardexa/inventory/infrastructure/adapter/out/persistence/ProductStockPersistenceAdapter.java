package com.kardexa.inventory.infrastructure.adapter.out.persistence;

import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.domain.exception.StaleStockException;
import com.kardexa.inventory.domain.model.ProductStock;
import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@code product_stock} con JDBC. El bloqueo optimista del ADR 0006 se aplica en el propio SQL: la
 * escritura solo afecta a la fila si su versión sigue siendo la que se leyó.
 */
@Component
class ProductStockPersistenceAdapter implements ProductStockRepository {

    private final JdbcClient jdbc;

    ProductStockPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Quantity> findQuantity(UUID productId, UUID siteId) {
        return jdbc.sql("""
                SELECT sum(quantity) FROM product_stock
                WHERE product_id = :productId AND (CAST(:siteId AS uuid) IS NULL OR site_id = :siteId)
                GROUP BY product_id""")
                .param("productId", productId)
                .param("siteId", siteId, java.sql.Types.OTHER)
                .query(BigDecimal.class)
                .optional()
                .map(Quantity::new);
    }

    @Override
    public ProductStock load(UUID productId, UUID siteId) {
        return jdbc.sql("SELECT quantity, version FROM product_stock WHERE product_id = ? AND site_id = ?")
                .params(productId, siteId)
                .query((row, rowNumber) -> new ProductStock(
                        productId, siteId, new Quantity(row.getBigDecimal("quantity")), row.getLong("version")))
                .optional()
                .orElseGet(() -> ProductStock.empty(productId, siteId));
    }

    @Override
    public void save(ProductStock stock) {
        int affected = stock.version() == ProductStock.NEVER_SAVED ? insert(stock) : update(stock);
        if (affected == 0) {
            throw new StaleStockException(stock.productId());
        }
    }

    /** Si otro creó la fila primero no inserta nada, y se trata como un stock que cambió. */
    private int insert(ProductStock stock) {
        return jdbc.sql("""
                INSERT INTO product_stock (product_id, site_id, quantity) VALUES (?, ?, ?)
                ON CONFLICT (product_id, site_id) DO NOTHING""")
                .params(stock.productId(), stock.siteId(), stock.quantity().value())
                .update();
    }

    private int update(ProductStock stock) {
        return jdbc.sql("""
                UPDATE product_stock SET quantity = ?, version = version + 1, updated_at = now()
                WHERE product_id = ? AND site_id = ? AND version = ?""")
                .params(stock.quantity().value(), stock.productId(), stock.siteId(), stock.version())
                .update();
    }

    @Override
    public Map<UUID, Quantity> findQuantities(Collection<UUID> productIds, UUID siteId) {
        Map<UUID, Quantity> result = new HashMap<>();
        if (productIds.isEmpty()) {
            return result;
        }
        jdbc.sql("""
                SELECT product_id, sum(quantity) AS quantity FROM product_stock
                WHERE product_id IN (:ids) AND (CAST(:siteId AS uuid) IS NULL OR site_id = :siteId)
                GROUP BY product_id""")
                .param("ids", List.copyOf(productIds))
                .param("siteId", siteId, java.sql.Types.OTHER)
                .query((RowCallbackHandler) row -> result.put(
                        row.getObject("product_id", UUID.class), new Quantity(row.getBigDecimal("quantity"))));
        return result;
    }
}
