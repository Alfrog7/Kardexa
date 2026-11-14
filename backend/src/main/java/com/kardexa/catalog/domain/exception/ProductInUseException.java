package com.kardexa.catalog.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** RN-03: un producto con movimientos, compras o ventas no se borra; se inhabilita. */
public class ProductInUseException extends ConflictException {

    public ProductInUseException(String name) {
        super("RN-03", "«%s» ya tiene movimientos de stock, compras o ventas, así que no se puede eliminar. "
                .formatted(name) + "Inhabilítalo para que no se venda más.");
    }

    @Override
    public String title() {
        return "Producto con historial";
    }
}
