package com.kardexa.inventory.application.service;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.application.port.out.StockProducts;
import com.kardexa.inventory.application.port.out.StockProducts.StockProduct;
import com.kardexa.inventory.application.port.out.StockRecords;
import com.kardexa.inventory.domain.exception.InactiveProductException;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.inventory.domain.model.ProductStock;
import com.kardexa.inventory.domain.model.SourceType;
import com.kardexa.inventory.domain.model.StockMovement;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * Movimientos de compras y ventas. Necesita la transacción del coordinador, que exige el decorador
 * registrado en {@code InventoryConfig} (aquí no puede haber Spring).
 */
public class RecordStockMovementsService implements RecordStockMovementsUseCase {

    private final ProductStockRepository stocks;
    private final StockRecords records;
    private final StockProducts products;
    private final Clock clock;

    public RecordStockMovementsService(
            ProductStockRepository stocks, StockRecords records, StockProducts products, Clock clock) {
        this.stocks = Objects.requireNonNull(stocks);
        this.records = Objects.requireNonNull(records);
        this.products = Objects.requireNonNull(products);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void record(RecordStockMovementsCommand command) {
        MovementType type = MovementType.valueOf(command.movement().name());
        Direction direction = directionOf(command.movement());
        SourceType source = switch (command.movement()) {
            case PURCHASE, PURCHASE_VOID -> SourceType.PURCHASE;
            case SALE, SALE_VOID -> SourceType.SALE;
        };
        // ADR 0006, punto 5: siempre en el mismo orden, para que dos documentos no se bloqueen entre sí.
        var lines = command.lines().stream().sorted(Comparator.comparing(Line::productId)).toList();
        for (Line line : lines) {
            StockProduct product = products.require(line.productId());
            if (!product.active()) {
                throw new InactiveProductException(product.name());
            }
            ProductStock updated = stocks.load(line.productId()).apply(direction, line.quantity(), product.name());
            // Igual que en los ajustes: primero el stock, que bloquea la fila, y luego la hora.
            stocks.save(updated);
            records.add(new StockMovement(UUID.randomUUID(), line.productId(), type, direction, line.quantity(),
                    updated.quantity(), source, command.documentId(), command.userId(),
                    clock.instant().truncatedTo(ChronoUnit.MICROS)));
        }
    }

    private static Direction directionOf(DocumentMovement movement) {
        return switch (movement) {
            case PURCHASE, SALE_VOID -> Direction.IN;
            case PURCHASE_VOID, SALE -> Direction.OUT;
        };
    }
}
