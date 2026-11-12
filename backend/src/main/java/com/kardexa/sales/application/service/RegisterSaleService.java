package com.kardexa.sales.application.service;

import com.kardexa.sales.application.port.in.RegisterSaleUseCase;
import com.kardexa.sales.application.port.out.SaleNumbers;
import com.kardexa.sales.application.port.out.SaleProducts;
import com.kardexa.sales.application.port.out.SaleProducts.ProductForSale;
import com.kardexa.sales.application.port.out.SaleRepository;
import com.kardexa.sales.application.port.out.StockLedger;
import com.kardexa.sales.domain.exception.InactiveProductException;
import com.kardexa.sales.domain.exception.SaleProductNotFoundException;
import com.kardexa.sales.domain.model.Sale;
import com.kardexa.sales.domain.model.SaleItem;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HU-14. Java puro: la transacción y los reintentos los pone {@code RetryingRegisterSale} (ADR 0010).
 * Orden: precios del catálogo, salidas del inventario, número y, por último, la venta.
 */
public class RegisterSaleService implements RegisterSaleUseCase {

    private final SaleRepository sales;
    private final SaleNumbers numbers;
    private final SaleProducts products;
    private final StockLedger stockLedger;
    private final Clock clock;

    public RegisterSaleService(
            SaleRepository sales, SaleNumbers numbers, SaleProducts products, StockLedger stockLedger, Clock clock) {
        this.sales = Objects.requireNonNull(sales);
        this.numbers = Objects.requireNonNull(numbers);
        this.products = Objects.requireNonNull(products);
        this.stockLedger = Objects.requireNonNull(stockLedger);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Sale register(RegisterSaleCommand command) {
        Map<UUID, ProductForSale> found = new HashMap<>();
        products.findByIds(command.items().stream().map(ItemCommand::productId).toList())
                .forEach(product -> found.put(product.id(), product));
        List<SaleItem> items = command.items().stream().map(item -> {
            ProductForSale product = found.get(item.productId());
            if (product == null) {
                throw new SaleProductNotFoundException(item.productId());
            }
            if (!product.active()) {
                throw new InactiveProductException(product.name());
            }
            return new SaleItem(product.id(), product.code(), product.name(), item.quantity(), product.salePrice());
        }).toList();
        Sale sale = Sale.register(items, command.sellerId(), clock.instant().truncatedTo(ChronoUnit.MICROS));

        stockLedger.withdraw(sale);
        sale.assignNumber(numbers.next());
        sales.add(sale);
        return sale;
    }
}
