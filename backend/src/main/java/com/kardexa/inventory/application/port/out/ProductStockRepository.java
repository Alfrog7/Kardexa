package com.kardexa.inventory.application.port.out;

import com.kardexa.inventory.domain.model.ProductStock;
import com.kardexa.shared.domain.Quantity;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Tabla {@code product_stock}: stock actual de cada producto, con bloqueo optimista (ADR 0006). */
public interface ProductStockRepository {

    Optional<Quantity> findQuantity(UUID productId);

    /** Stock de los productos que tienen fila; los que no la tienen no aparecen en el mapa. */
    Map<UUID, Quantity> findQuantities(Collection<UUID> productIds);

    /** Stock con su versión, para modificarlo. Sin fila devuelve {@link ProductStock#empty}. */
    ProductStock load(UUID productId);

    /**
     * Guarda el stock solo si su versión sigue siendo la que se leyó.
     *
     * @throws com.kardexa.inventory.domain.exception.StaleStockException si otro lo cambió antes
     */
    void save(ProductStock stock);
}
