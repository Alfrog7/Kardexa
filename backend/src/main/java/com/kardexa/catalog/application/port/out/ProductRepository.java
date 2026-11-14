package com.kardexa.catalog.application.port.out;

import com.kardexa.catalog.domain.model.Product;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Money;
import java.util.Collection;
import java.util.List;
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

    /** Los que existen entre {@code ids}, en una consulta (RN-06, ADR 0010). */
    List<Product> findAllById(Collection<UUID> ids);

    /**
     * RN-08: cambia solo el costo referencial. {@link #save} nunca lo escribe, para que editar un
     * producto mientras se registra una compra no lo pise. Devuelve {@code false} si no existe.
     */
    boolean updateReferenceCost(UUID productId, Money cost);

    boolean existsByCode(String code);

    /**
     * Borra el producto si nada lo referencia (kárdex, compras, ventas). Devuelve {@code false} si tiene
     * historial y no se borró (RN-03).
     */
    boolean deleteIfUnused(UUID id);

    /** Mayor número usado con el formato automático {@code P-0001} (sin distinguir mayúsculas), o 0. */
    long maxGeneratedCodeNumber();

    boolean existsByCodeExcluding(String code, UUID excludedId);

    /**
     * Productos activos cuyo código o nombre contiene {@code text} (sin distinguir mayúsculas),
     * opcionalmente de una categoría, ordenados por nombre. Los filtros nulos no se aplican.
     * Con {@code criticalOnly}, solo los que tienen stock actual ≤ stock mínimo (RN-09); un producto
     * sin movimientos tiene stock 0.
     */
    ResultPage<Product> search(String text, UUID categoryId, boolean criticalOnly, boolean includeInactive, PageQuery page);
}
