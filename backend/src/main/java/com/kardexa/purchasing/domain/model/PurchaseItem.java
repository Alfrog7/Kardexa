package com.kardexa.purchasing.domain.model;

import com.kardexa.purchasing.domain.exception.InvalidPurchaseException;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.util.Objects;
import java.util.UUID;

/** Una línea de la compra: cuánto se compró de un producto y a qué costo unitario (RF-16). */
public record PurchaseItem(UUID productId, Quantity quantity, Money unitCost) {

    public PurchaseItem {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unitCost, "unitCost");
        if (!quantity.isPositive()) {
            throw new InvalidPurchaseException("La cantidad de cada producto debe ser mayor que 0.");
        }
        if (unitCost.isNegative()) {
            throw new InvalidPurchaseException("El costo unitario no puede ser negativo.");
        }
    }

    /** Costo unitario × cantidad, redondeado a 2 decimales. */
    public Money subtotal() {
        return unitCost.multiply(quantity);
    }
}
