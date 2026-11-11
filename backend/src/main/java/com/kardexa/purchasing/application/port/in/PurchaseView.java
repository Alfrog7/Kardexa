package com.kardexa.purchasing.application.port.in;

import com.kardexa.purchasing.domain.model.PurchaseStatus;
import com.kardexa.purchasing.domain.model.Supplier;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Una compra lista para mostrar: con su proveedor y el nombre de cada producto. */
public record PurchaseView(
        UUID id,
        Supplier supplier,
        String supplierDocument,
        LocalDate purchaseDate,
        PurchaseStatus status,
        Money total,
        List<Item> items) {

    public PurchaseView {
        items = List.copyOf(items);
    }

    public record Item(UUID productId, String productName, Quantity quantity, Money unitCost, Money subtotal) {}
}
