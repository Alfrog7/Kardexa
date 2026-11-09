package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.Product;
import com.kardexa.shared.domain.Quantity;

/** Producto tal como lo ve quien lo consulta: con el nombre de su categoría y su stock actual. */
public record ProductView(Product product, String categoryName, Quantity stock) {

    /** RN-09. */
    public boolean critical() {
        return product.isCritical(stock);
    }
}
