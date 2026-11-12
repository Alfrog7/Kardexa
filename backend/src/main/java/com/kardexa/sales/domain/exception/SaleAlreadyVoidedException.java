package com.kardexa.sales.domain.exception;

import com.kardexa.sales.domain.model.SaleNumber;
import com.kardexa.shared.domain.DomainException;

/** Una venta anulada no se anula otra vez (HU-15). */
public class SaleAlreadyVoidedException extends DomainException {

    public SaleAlreadyVoidedException(SaleNumber number) {
        super("RF-20", "La venta %s ya está anulada.".formatted(number));
    }

    @Override
    public String title() {
        return "Venta ya anulada";
    }
}
