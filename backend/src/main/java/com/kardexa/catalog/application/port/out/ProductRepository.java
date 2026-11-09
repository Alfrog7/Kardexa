package com.kardexa.catalog.application.port.out;

import com.kardexa.catalog.domain.model.Product;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
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

    /**
     * Productos activos cuyo código o nombre contiene {@code text} (sin distinguir mayúsculas),
     * opcionalmente de una categoría, ordenados por nombre. Los filtros nulos no se aplican.
     * Con {@code criticalOnly}, solo los que tienen stock actual ≤ stock mínimo (RN-09); un producto
     * sin movimientos tiene stock 0.
     */
    ResultPage<Product> search(String text, UUID categoryId, boolean criticalOnly, boolean includeInactive, PageQuery page);
}
