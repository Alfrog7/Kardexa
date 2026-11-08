package com.kardexa.inventory.application.service;

import com.kardexa.inventory.application.port.in.AdjustStockUseCase;
import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.application.port.out.StockProducts;
import com.kardexa.inventory.application.port.out.StockProducts.StockProduct;
import com.kardexa.inventory.application.port.out.StockRecords;
import com.kardexa.inventory.domain.exception.InactiveProductException;
import com.kardexa.inventory.domain.model.ProductStock;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * HU-09. Actualiza el stock y escribe el ajuste y su movimiento: debe ejecutarse en una transacción,
 * que aporta el decorador registrado en {@code InventoryConfig} (aquí no puede haber Spring).
 */
public class AdjustStockService implements AdjustStockUseCase {

    private final ProductStockRepository stocks;
    private final StockRecords records;
    private final StockProducts products;
    private final Clock clock;

    public AdjustStockService(
            ProductStockRepository stocks, StockRecords records, StockProducts products, Clock clock) {
        this.stocks = Objects.requireNonNull(stocks);
        this.records = Objects.requireNonNull(records);
        this.products = Objects.requireNonNull(products);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public StockMovement adjust(AdjustStockCommand command) {
        StockProduct product = products.require(command.productId());
        if (!product.active()) {
            throw new InactiveProductException(product.name());
        }
        ProductStock updated = stocks.load(command.productId())
                .apply(command.direction(), command.quantity(), product.name());
        // Primero el stock: si otro lo cambió falla aquí, y a partir de este punto la fila queda
        // bloqueada hasta el final de la transacción, lo que ordena los movimientos del producto.
        stocks.save(updated);

        // La hora se toma con la fila bloqueada, para que las fechas sigan el orden de los saldos.
        // Microsegundos: la precisión de timestamptz, para que lo devuelto sea igual a lo guardado.
        StockAdjustment adjustment = StockAdjustment.create(command.productId(), command.direction(),
                command.quantity(), command.reason(), command.note(), clock.instant().truncatedTo(ChronoUnit.MICROS));
        StockMovement movement = StockMovement.fromAdjustment(adjustment, updated.quantity());
        records.add(adjustment);
        records.add(movement);
        return movement;
    }
}
