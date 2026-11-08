package com.kardexa.inventory.application.service;

import com.kardexa.inventory.application.port.in.GetKardexUseCase;
import com.kardexa.inventory.application.port.out.StockMovementQueries;
import com.kardexa.inventory.application.port.out.StockProducts;
import com.kardexa.inventory.application.port.out.StockProducts.StockProduct;
import com.kardexa.inventory.domain.model.Kardex;
import com.kardexa.shared.domain.BusinessCalendar;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** HU-08. Java puro: se registra como bean en {@code InventoryConfig}. */
public class KardexService implements GetKardexUseCase {

    private final StockMovementQueries movements;
    private final StockProducts products;

    public KardexService(StockMovementQueries movements, StockProducts products) {
        this.movements = Objects.requireNonNull(movements);
        this.products = Objects.requireNonNull(products);
    }

    @Override
    public Kardex get(UUID productId, LocalDate from, LocalDate to) {
        StockProduct product = products.require(productId);
        Instant start = from == null ? null : BusinessCalendar.startOf(from);
        Instant end = to == null ? null : BusinessCalendar.endOf(to);
        Quantity opening = start == null ? Quantity.ZERO : movements.balanceBefore(productId, start);
        return Kardex.of(productId, product.name(), opening, movements.findMovements(productId, start, end));
    }
}
