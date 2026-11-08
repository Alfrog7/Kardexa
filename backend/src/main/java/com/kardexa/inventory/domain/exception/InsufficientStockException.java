package com.kardexa.inventory.domain.exception;

import com.kardexa.shared.domain.DomainException;
import com.kardexa.shared.domain.Quantity;

/** RN-01: el stock de un producto nunca queda en negativo. */
public class InsufficientStockException extends DomainException {

    public InsufficientStockException(String productName, Quantity available, Quantity requested) {
        super("RN-01", "Stock insuficiente para \"" + productName + "\": disponible " + available + ", solicitado " + requested + ".");
    }

    @Override
    public String title() {
        return "Stock insuficiente";
    }
}
