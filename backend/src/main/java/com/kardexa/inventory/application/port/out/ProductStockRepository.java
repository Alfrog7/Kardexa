package com.kardexa.inventory.application.port.out;

import com.kardexa.shared.domain.Quantity;
import java.util.Optional;
import java.util.UUID;

/** Lectura de la tabla {@code product_stock}. La escritura llega con los movimientos (HU-09). */
public interface ProductStockRepository {

    Optional<Quantity> findQuantity(UUID productId);
}
