package com.kardexa.inventory.domain.exception;

import com.kardexa.shared.domain.DomainException;
import com.kardexa.shared.domain.Quantity;

/** El stock no puede superar lo que admite su formato de 12 dígitos con 3 decimales. */
public class StockLimitExceededException extends DomainException {

    public StockLimitExceededException(Quantity result) {
        super("RN-12", "El stock resultante (" + result + ") supera el máximo permitido.");
    }

    @Override
    public String title() {
        return "Stock fuera de rango";
    }
}
