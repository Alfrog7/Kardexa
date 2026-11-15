package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.DomainException;
import com.kardexa.shared.domain.Quantity;

/** El sobrante contado supera el stock registrado: lo que llegó y no se anotó debe registrarse antes (RN-15). */
public class LeftoverExceedsStockException extends DomainException {

    public LeftoverExceedsStockException(String productName, Quantity leftover, Quantity stock) {
        super("RN-15", "El sobrante de «%s» (%s) supera el stock registrado (%s). Registra antes como entrada lo que llegó y no se anotó."
                .formatted(productName, leftover, stock));
    }

    @Override
    public String title() {
        return "Sobrante mayor que el stock";
    }
}
