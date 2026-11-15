package com.kardexa.catalog.application.port.in;

import java.util.UUID;

/** HU-06 · Consultar un producto con su categoría y su stock actual. */
public interface GetProductUseCase {

    /** {@code siteId} nulo: el stock es la suma de todas las sedes. */
    ProductView get(UUID id, UUID siteId);
}
