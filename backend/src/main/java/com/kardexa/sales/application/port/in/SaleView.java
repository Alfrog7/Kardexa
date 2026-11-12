package com.kardexa.sales.application.port.in;

import com.kardexa.sales.domain.model.SaleStatus;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Una venta lista para mostrar en su comprobante (RF-19): con el nombre del vendedor. */
public record SaleView(
        UUID id,
        String number,
        Instant soldAt,
        UUID sellerId,
        String sellerName,
        SaleStatus status,
        String voidReason,
        Money total,
        List<Item> items) {

    public SaleView {
        items = List.copyOf(items);
    }

    public record Item(
            UUID productId, String productCode, String productName, Quantity quantity, Money unitPrice,
            Money subtotal) {}
}
