package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.Product;

/** HU-06 · Registrar un producto con un código que no exista, en una categoría activa. Nace con stock 0. */
public interface CreateProductUseCase {

    ProductView create(Product.Details details);
}
