package com.kardexa.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.inventory.application.port.in.AdjustStockUseCase.AdjustStockCommand;
import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.application.port.out.StockProducts.StockProduct;
import com.kardexa.inventory.application.port.out.StockRecords;
import com.kardexa.inventory.domain.exception.InactiveProductException;
import com.kardexa.inventory.domain.exception.InsufficientStockException;
import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.ProductStock;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Registrar un ajuste de stock (HU-09)")
class AdjustStockServiceTest {

    private static final UUID PRODUCT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00.123456789Z");

    private final Map<UUID, ProductStock> rows = new HashMap<>();
    private final List<StockAdjustment> adjustments = new ArrayList<>();
    private final List<StockMovement> movements = new ArrayList<>();
    private boolean active = true;

    private final AdjustStockService service = new AdjustStockService(new InMemoryStocks(), new StockRecords() {
        @Override
        public void add(StockAdjustment adjustment) {
            adjustments.add(adjustment);
        }

        @Override
        public void add(StockMovement movement) {
            movements.add(movement);
        }
    }, id -> new StockProduct(id, "Arroz 5 kg", active), Clock.fixed(NOW, ZoneOffset.UTC));

    private StockMovement adjust(Direction direction, String quantity) {
        return service.adjust(new AdjustStockCommand(
                PRODUCT, direction, Quantity.of(quantity), AdjustmentReason.SHRINKAGE, "rotura"));
    }

    @Test
    @DisplayName("criterio 1: una salida de 3 sobre 20 deja 17 y crea su movimiento")
    void withdraws() {
        rows.put(PRODUCT, new ProductStock(PRODUCT, Quantity.of("20"), 2));

        StockMovement movement = adjust(Direction.OUT, "3");

        assertThat(movement.balanceAfter()).isEqualTo(Quantity.of("17"));
        assertThat(rows.get(PRODUCT).quantity()).isEqualTo(Quantity.of("17"));
        assertThat(movements).containsExactly(movement);
        assertThat(adjustments).singleElement().satisfies(adjustment -> {
            assertThat(adjustment.id()).isEqualTo(movement.sourceId());
            assertThat(adjustment.reason()).isEqualTo(AdjustmentReason.SHRINKAGE);
            assertThat(adjustment.note()).isEqualTo("rotura");
        });
    }

    @Test
    @DisplayName("una entrada sobre un producto sin fila parte de stock 0")
    void receivesWithoutRow() {
        assertThat(adjust(Direction.IN, "2.5").balanceAfter()).isEqualTo(Quantity.of("2.5"));
    }

    @Test
    @DisplayName("guarda la fecha con la precisión de la base (microsegundos)")
    void truncatesDate() {
        assertThat(adjust(Direction.IN, "1").occurredAt()).isEqualTo(Instant.parse("2026-11-08T10:00:00.123456Z"));
    }

    @Test
    @DisplayName("RN-01: una salida mayor que el stock no registra nada")
    void rejectsInsufficientStock() {
        assertThatThrownBy(() -> adjust(Direction.OUT, "1")).isInstanceOf(InsufficientStockException.class);

        assertThat(rows).isEmpty();
        assertThat(adjustments).isEmpty();
        assertThat(movements).isEmpty();
    }

    @Test
    @DisplayName("un producto desactivado no admite ajustes")
    void rejectsInactiveProduct() {
        active = false;

        assertThatThrownBy(() -> adjust(Direction.IN, "1")).isInstanceOf(InactiveProductException.class);
        assertThat(movements).isEmpty();
    }

    /** Doble de prueba: en memoria no hay concurrencia, así que solo guarda el nuevo estado. */
    private final class InMemoryStocks implements ProductStockRepository {

        @Override
        public Optional<Quantity> findQuantity(UUID productId) {
            return Optional.ofNullable(rows.get(productId)).map(ProductStock::quantity);
        }

        @Override
        public Map<UUID, Quantity> findQuantities(Collection<UUID> productIds) {
            return Map.of();
        }

        @Override
        public ProductStock load(UUID productId) {
            return rows.getOrDefault(productId, ProductStock.empty(productId));
        }

        @Override
        public void save(ProductStock stock) {
            rows.put(stock.productId(), stock);
        }
    }
}
