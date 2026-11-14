package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.out.ProductImageRepository;
import com.kardexa.catalog.domain.model.ProductImage;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Imágenes en memoria para los tests del servicio. */
class InMemoryProductImageRepository implements ProductImageRepository {

    private final Map<UUID, ProductImage> rows = new HashMap<>();

    @Override
    public void save(UUID productId, ProductImage image) {
        rows.put(productId, image);
    }

    @Override
    public void delete(UUID productId) {
        rows.remove(productId);
    }

    @Override
    public Optional<ProductImage> find(UUID productId) {
        return Optional.ofNullable(rows.get(productId));
    }

    @Override
    public Map<UUID, Instant> versions(Collection<UUID> productIds) {
        Map<UUID, Instant> versions = new HashMap<>();
        productIds.forEach(id -> {
            if (rows.containsKey(id)) {
                versions.put(id, rows.get(id).updatedAt());
            }
        });
        return versions;
    }
}
