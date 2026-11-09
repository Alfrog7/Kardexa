package com.kardexa.catalog.application.port.in;

import java.util.UUID;

/** HU-06 · Consultar un producto con su categoría y su stock actual. */
public interface GetProductUseCase {

    ProductView get(UUID id);
}
