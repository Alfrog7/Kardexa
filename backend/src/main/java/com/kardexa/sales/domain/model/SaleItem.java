package com.kardexa.sales.domain.model;

import com.kardexa.sales.domain.exception.InvalidSaleException;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.util.Objects;
import java.util.UUID;

/** Una línea de la venta con el código, el nombre y el precio copiados del producto (RN-06). */
public record SaleItem(UUID productId, String productCode, String productName, Quantity quantity, Money unitPrice) {

    public SaleItem {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(productCode, "productCode");
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (!quantity.isPositive()) {
            throw new InvalidSaleException("La cantidad de cada producto debe ser mayor que 0.");
        }
        if (unitPrice.compareTo(Money.ZERO) <= 0) {
            throw new InvalidSaleException("El precio de \"%s\" debe ser mayor que 0.".formatted(productName));
        }
    }

    /** Precio × cantidad, redondeado a 2 decimales half-up (RN-12). */
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
