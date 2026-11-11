package com.kardexa.purchasing.application.service;

import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase;
import com.kardexa.purchasing.application.port.out.PurchaseRepository;
import com.kardexa.purchasing.application.port.out.PurchasedProducts;
import com.kardexa.purchasing.application.port.out.StockLedger;
import com.kardexa.purchasing.application.port.out.SupplierRepository;
import com.kardexa.purchasing.domain.exception.InactiveSupplierException;
import com.kardexa.purchasing.domain.exception.SupplierNotFoundException;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.purchasing.domain.model.PurchaseItem;
import com.kardexa.purchasing.domain.model.Supplier;
import com.kardexa.shared.domain.Money;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HU-12. Coordina el inventario y el catálogo dentro de una sola transacción, que aporta el decorador
 * registrado en {@code PurchasingConfig} junto con los reintentos (ADR 0009).
 */
public class RegisterPurchaseService implements RegisterPurchaseUseCase {

    private final SupplierRepository suppliers;
    private final PurchaseRepository purchases;
    private final StockLedger stockLedger;
    private final PurchasedProducts products;
    private final Clock clock;

    public RegisterPurchaseService(SupplierRepository suppliers, PurchaseRepository purchases, StockLedger stockLedger,
            PurchasedProducts products, Clock clock) {
        this.suppliers = Objects.requireNonNull(suppliers);
        this.purchases = Objects.requireNonNull(purchases);
        this.stockLedger = Objects.requireNonNull(stockLedger);
        this.products = Objects.requireNonNull(products);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Purchase register(RegisterPurchaseCommand command) {
        Supplier supplier = suppliers.findById(command.supplierId())
                .orElseThrow(() -> new SupplierNotFoundException(command.supplierId()));
        if (!supplier.isActive()) {
            throw new InactiveSupplierException(supplier.businessName());
        }
        Purchase purchase = Purchase.register(supplier.id(), command.supplierDocument(), command.purchaseDate(),
                command.items().stream()
                        .map(i -> new PurchaseItem(i.productId(), i.quantity(), i.unitCost()))
                        .toList(),
                command.userId(), clock.instant().truncatedTo(ChronoUnit.MICROS));

        // Primero el inventario: comprueba que cada producto exista y esté activo antes de guardar nada.
        stockLedger.receive(purchase);
        Map<UUID, Money> costs = new LinkedHashMap<>();
        purchase.items().forEach(item -> costs.put(item.productId(), item.unitCost()));
        products.updateReferenceCosts(costs);
        purchases.add(purchase);
        return purchase;
    }
}
