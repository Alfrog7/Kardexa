package com.kardexa.inventory.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Un ajuste necesita una cantidad mayor que 0 y un motivo. */
public class InvalidStockAdjustmentException extends DomainException {

    public InvalidStockAdjustmentException(String message) {
        super("RF-11", message);
    }

    @Override
    public String title() {
        return "Ajuste de stock inválido";
    }
}
