package com.kardexa.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase.ItemCommand;
import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase.RegisterPurchaseCommand;
import com.kardexa.purchasing.domain.exception.InactiveSupplierException;
import com.kardexa.purchasing.domain.exception.SupplierNotFoundException;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.purchasing.domain.model.Supplier;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Registrar una compra (HU-12)")
class RegisterPurchaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T15:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private static final UUID RICE = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    private InMemorySupplierRepository suppliers;
    private final InMemoryPurchaseRepository purchases = new InMemoryPurchaseRepository();
    private final FakeStockLedger ledger = new FakeStockLedger();
    private final Map<UUID, Money> costs = new HashMap<>();
    private RegisterPurchaseService service;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        suppliers = new InMemorySupplierRepository();
        supplier = suppliers.save(Supplier.create(new Supplier.Details("20123456789", "Distribuidora", null, null)));
        service = new RegisterPurchaseService(suppliers, purchases, ledger, costs::putAll,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private RegisterPurchaseCommand command(UUID supplierId) {
        return new RegisterPurchaseCommand(supplierId, "F001-77", LocalDate.of(2026, 11, 10), List.of(
                new ItemCommand(RICE, Quantity.of("10"), Money.of("18.50")),
                new ItemCommand(OIL, Quantity.of("4"), Money.of("9.90"))), USER);
    }

    @Test
    @DisplayName("criterios 1 y 2: suma el stock de cada producto, fija su costo y guarda la compra")
    void registersPurchase() {
        Purchase purchase = service.register(command(supplier.id()));

        assertThat(ledger.received).containsExactly(purchase);
        assertThat(costs).containsEntry(RICE, Money.of("18.50")).containsEntry(OIL, Money.of("9.90"));
        assertThat(purchases.rows).containsOnlyKeys(purchase.id());
        assertThat(purchase.total()).isEqualTo(Money.of("224.60"));
        assertThat(purchase.userId()).isEqualTo(USER);
        assertThat(purchase.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("criterio 3: si el inventario rechaza un producto, no fija costos ni guarda la compra")
    void stopsWhenInventoryFails() {
        ledger.failure = new IllegalStateException("producto desactivado");

        assertThatThrownBy(() -> service.register(command(supplier.id()))).isSameAs(ledger.failure);
        assertThat(costs).isEmpty();
        assertThat(purchases.rows).isEmpty();
    }

    @Test
    @DisplayName("un proveedor inexistente o desactivado no admite compras")
    void rejectsMissingOrInactiveSupplier() {
        assertThatThrownBy(() -> service.register(command(UUID.randomUUID())))
                .isInstanceOf(SupplierNotFoundException.class);

        supplier.deactivate();
        suppliers.save(supplier);
        assertThatThrownBy(() -> service.register(command(supplier.id())))
                .isInstanceOf(InactiveSupplierException.class);
        assertThat(ledger.received).isEmpty();
    }
}
