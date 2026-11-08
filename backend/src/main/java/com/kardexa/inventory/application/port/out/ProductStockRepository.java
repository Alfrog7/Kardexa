package com.kardexa.inventory.application.port.out;

import com.kardexa.shared.domain.Quantity;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Lectura de la tabla {@code product_stock}. La escritura llega con los movimientos (HU-09). */
public interface ProductStockRepository {

    Optional<Quantity> findQuantity(UUID productId);

    /** Stock de los productos que tienen fila; los que no la tienen no aparecen en el mapa. */
    Map<UUID, Quantity> findQuantities(Collection<UUID> productIds);
}
