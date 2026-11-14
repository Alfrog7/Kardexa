package com.kardexa.catalog.application.port.in;

import java.time.Instant;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.shared.domain.Quantity;

/** Producto tal como lo ve quien lo consulta: con el nombre de su categoría y su stock actual. */
public record ProductView(Product product, String categoryName, Quantity stock, Instant imageVersion) {

    /** Sin imagen. */
    public ProductView(Product product, String categoryName, Quantity stock) {
        this(product, categoryName, stock, null);
    }

    /** RN-09. */
    public boolean critical() {
        return product.isCritical(stock);
    }
}
