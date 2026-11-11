package com.kardexa.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.DocumentMovement;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.RecordStockMovementsCommand;
import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.application.port.out.StockProducts.StockProduct;
import com.kardexa.inventory.application.port.out.StockRecords;
import com.kardexa.inventory.domain.exception.InactiveProductException;
import com.kardexa.inventory.domain.exception.InsufficientStockException;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.inventory.domain.model.ProductStock;
import com.kardexa.inventory.domain.model.SourceType;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Movimientos de compras y ventas (ADR 0009)")
class RecordStockMovementsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
    private static final UUID LOW = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HIGH = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER = UUID.randomUUID();

    private final Map<UUID, ProductStock> rows = new HashMap<>();
    private final List<UUID> saveOrder = new ArrayList<>();
    private final List<StockMovement> movements = new ArrayList<>();
    private final Set<UUID> inactive = new HashSet<>();

    private final RecordStockMovementsService service = new RecordStockMovementsService(new ProductStockRepository() {
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
            saveOrder.add(stock.productId());
            rows.put(stock.productId(), stock);
        }
    }, new StockRecords() {
        @Override
        public void add(StockAdjustment adjustment) {
            throw new AssertionError("No es un ajuste");
        }

        @Override
        public void add(StockMovement movement) {
            movements.add(movement);
        }
    }, id -> new StockProduct(id, "Producto " + id, !inactive.contains(id)), Clock.fixed(NOW, ZoneOffset.UTC));

    private void record(DocumentMovement movement, UUID documentId, Line... lines) {
        service.record(new RecordStockMovementsCommand(movement, documentId, USER, List.of(lines)));
    }

    private static Line line(UUID productId, String quantity) {
        return new Line(productId, Quantity.of(quantity));
    }

    @Test
    @DisplayName("una compra crea una entrada por producto con su documento y usuario, en orden de product_id")
    void purchaseAddsStock() {
        UUID purchase = UUID.randomUUID();
        rows.put(LOW, new ProductStock(LOW, Quantity.of("5"), 1));

        record(DocumentMovement.PURCHASE, purchase, line(HIGH, "2"), line(LOW, "3"));

        assertThat(saveOrder).containsExactly(LOW, HIGH);
        assertThat(rows.get(LOW).quantity()).isEqualTo(Quantity.of("8"));
        assertThat(rows.get(HIGH).quantity()).isEqualTo(Quantity.of("2"));
        assertThat(movements).allSatisfy(m -> {
            assertThat(m.type()).isEqualTo(MovementType.PURCHASE);
            assertThat(m.direction()).isEqualTo(Direction.IN);
            assertThat(m.sourceType()).isEqualTo(SourceType.PURCHASE);
            assertThat(m.sourceId()).isEqualTo(purchase);
            assertThat(m.userId()).isEqualTo(USER);
            assertThat(m.occurredAt()).isEqualTo(NOW);
        });
        assertThat(movements.get(0).balanceAfter()).isEqualTo(Quantity.of("8"));
    }

    @Test
    @DisplayName("anular una compra resta y una salida mayor que el stock se rechaza (RN-01)")
    void voidSubtractsAndChecksStock() {
        rows.put(LOW, new ProductStock(LOW, Quantity.of("5"), 1));

        record(DocumentMovement.PURCHASE_VOID, UUID.randomUUID(), line(LOW, "5"));
        assertThat(rows.get(LOW).quantity()).isEqualTo(Quantity.ZERO);
        assertThat(movements).singleElement().satisfies(m -> {
            assertThat(m.type()).isEqualTo(MovementType.PURCHASE_VOID);
            assertThat(m.direction()).isEqualTo(Direction.OUT);
        });

        assertThatThrownBy(() -> record(DocumentMovement.PURCHASE_VOID, UUID.randomUUID(), line(LOW, "1")))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("las ventas restan y sus anulaciones suman, con origen SALE")
    void salesDirections() {
        rows.put(LOW, new ProductStock(LOW, Quantity.of("5"), 1));

        record(DocumentMovement.SALE, UUID.randomUUID(), line(LOW, "2"));
        record(DocumentMovement.SALE_VOID, UUID.randomUUID(), line(LOW, "2"));

        assertThat(movements).extracting(StockMovement::direction).containsExactly(Direction.OUT, Direction.IN);
        assertThat(movements).extracting(StockMovement::sourceType).containsOnly(SourceType.SALE);
        assertThat(rows.get(LOW).quantity()).isEqualTo(Quantity.of("5"));
    }

    @Test
    @DisplayName("un producto desactivado rechaza el documento")
    void rejectsInactiveProduct() {
        inactive.add(HIGH);

        assertThatThrownBy(() -> record(DocumentMovement.PURCHASE, UUID.randomUUID(), line(HIGH, "1")))
                .isInstanceOf(InactiveProductException.class);
    }

    @Test
    @DisplayName("un documento sin líneas o con un producto repetido es un error de programación")
    void rejectsMalformedCommand() {
        assertThatThrownBy(() -> record(DocumentMovement.PURCHASE, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> record(DocumentMovement.PURCHASE, UUID.randomUUID(), line(LOW, "1"), line(LOW, "2")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
