package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.DomainException;
import java.util.UUID;

/** Una compra anulada no se puede volver a anular (RF-17). */
public class PurchaseAlreadyVoidedException extends DomainException {

    public PurchaseAlreadyVoidedException(UUID id) {
        super("RF-17", "La compra %s ya está anulada.".formatted(id));
    }

    @Override
    public String title() {
        return "Compra ya anulada";
    }
}
