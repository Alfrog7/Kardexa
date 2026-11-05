package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.Product;
import java.util.UUID;

/** HU-06 · Editar los datos de un producto o desactivarlo (RF-09: no se elimina). */
public interface UpdateProductUseCase {

    ProductView update(UpdateProductCommand command);

    record UpdateProductCommand(UUID id, Product.Details details, boolean active) {}
}
