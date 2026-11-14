package com.kardexa.catalog.application.port.out;

import com.kardexa.catalog.domain.model.ProductImage;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Imágenes de productos. */
public interface ProductImageRepository {

    /** Guarda o reemplaza la imagen. */
    void save(UUID productId, ProductImage image);

    void delete(UUID productId);

    Optional<ProductImage> find(UUID productId);

    /** Fecha de la imagen de cada producto que tiene una, en una consulta: sirve de versión para la caché. */
    Map<UUID, Instant> versions(Collection<UUID> productIds);
}
