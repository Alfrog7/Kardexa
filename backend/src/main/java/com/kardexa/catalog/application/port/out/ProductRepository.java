package com.kardexa.catalog.application.port.out;

import com.kardexa.catalog.domain.model.Product;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de productos. Los códigos se comparan sin distinguir mayúsculas (RN-10).
 *
 * <p>{@link #save} lanza {@code DuplicateProductCodeException} si el índice único detecta el
 * código repetido, aunque el servicio ya lo haya comprobado antes.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    boolean existsByCode(String code);

    boolean existsByCodeExcluding(String code, UUID excludedId);
}
