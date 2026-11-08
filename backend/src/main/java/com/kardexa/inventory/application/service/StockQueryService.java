package com.kardexa.inventory.application.service;

import com.kardexa.inventory.application.port.in.GetStockUseCase;
import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.shared.domain.Quantity;
import java.util.Objects;
import java.util.UUID;

/** La fila de stock nace con el primer movimiento: mientras no exista, el stock es 0. */
public class StockQueryService implements GetStockUseCase {

    private final ProductStockRepository stocks;

    public StockQueryService(ProductStockRepository stocks) {
        this.stocks = Objects.requireNonNull(stocks);
    }

    @Override
    public Quantity currentStock(UUID productId) {
        return stocks.findQuantity(productId).orElse(Quantity.ZERO);
    }
}
