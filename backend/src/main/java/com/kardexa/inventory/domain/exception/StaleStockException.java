package com.kardexa.inventory.domain.exception;

import com.kardexa.shared.domain.ConcurrentModificationException;
import java.util.UUID;

/** El stock cambió desde que se leyó (ADR 0006): la operación se reintenta y, si persiste, responde 409. */
public class StaleStockException extends ConcurrentModificationException {

    public StaleStockException(UUID productId) {
        super("RN-02", "Otro usuario modificó el stock del producto " + productId + ". Vuelve a intentarlo.");
    }

    @Override
    public String title() {
        return "Modificación concurrente";
    }
}
