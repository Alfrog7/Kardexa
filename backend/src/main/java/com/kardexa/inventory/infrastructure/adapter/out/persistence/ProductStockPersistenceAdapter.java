package com.kardexa.inventory.infrastructure.adapter.out.persistence;

import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Lee {@code product_stock} con JDBC. Cuando lleguen los movimientos (HU-09) se pasará a una
 * entidad JPA con {@code @Version} para el bloqueo optimista del ADR 0006.
 */
@Component
class ProductStockPersistenceAdapter implements ProductStockRepository {

    private final JdbcClient jdbc;

    ProductStockPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Quantity> findQuantity(UUID productId) {
        return jdbc.sql("SELECT quantity FROM product_stock WHERE product_id = ?")
                .param(productId)
                .query(BigDecimal.class)
                .optional()
                .map(Quantity::new);
    }
}
