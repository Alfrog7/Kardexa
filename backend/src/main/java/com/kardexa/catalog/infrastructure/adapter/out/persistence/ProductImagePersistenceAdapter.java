package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import com.kardexa.catalog.application.port.out.ProductImageRepository;
import com.kardexa.catalog.domain.model.ProductImage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Imágenes de producto en la tabla {@code product_image} (migración V13). */
@Component
class ProductImagePersistenceAdapter implements ProductImageRepository {

    private final JdbcClient jdbc;

    ProductImagePersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(UUID productId, ProductImage image) {
        jdbc.sql("""
                INSERT INTO product_image (product_id, content_type, data, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (product_id) DO UPDATE
                SET content_type = EXCLUDED.content_type, data = EXCLUDED.data, updated_at = EXCLUDED.updated_at""")
                .params(productId, image.contentType(), image.data(), Timestamp.from(image.updatedAt()))
                .update();
    }

    @Override
    public void delete(UUID productId) {
        jdbc.sql("DELETE FROM product_image WHERE product_id = ?").param(productId).update();
    }

    @Override
    public Optional<ProductImage> find(UUID productId) {
        return jdbc.sql("SELECT content_type, data, updated_at FROM product_image WHERE product_id = ?")
                .param(productId)
                .query((rs, row) -> new ProductImage(
                        rs.getString("content_type"), rs.getBytes("data"), rs.getTimestamp("updated_at").toInstant()))
                .optional();
    }

    @Override
    public Map<UUID, Instant> versions(Collection<UUID> productIds) {
        Map<UUID, Instant> versions = new HashMap<>();
        if (productIds.isEmpty()) {
            return versions;
        }
        jdbc.sql("SELECT product_id, updated_at FROM product_image WHERE product_id IN (:ids)")
                .param("ids", productIds)
                .query((rs, row) -> versions.put(
                        rs.getObject("product_id", UUID.class), rs.getTimestamp("updated_at").toInstant()))
                .list();
        return versions;
    }
}
