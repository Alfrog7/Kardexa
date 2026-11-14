package com.kardexa.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.inventory.application.port.out.StockMovementQueries;
import com.kardexa.inventory.application.port.out.StockProducts.StockProduct;
import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.Kardex;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Consultar el kárdex (HU-08)")
class KardexServiceTest {

    private static final UUID PRODUCT = UUID.randomUUID();

    private final List<StockMovement> rows = new ArrayList<>();
    private boolean exists = true;

    private final KardexService service = new KardexService(new StockMovementQueries() {
        @Override
        public Quantity balanceBefore(UUID productId, Instant instant) {
            return rows.stream().filter(m -> m.occurredAt().isBefore(instant))
                    .reduce((first, second) -> second)
                    .map(StockMovement::balanceAfter)
                    .orElse(Quantity.ZERO);
        }

        @Override
        public List<StockMovement> findMovements(UUID productId, Instant from, Instant to) {
            return rows.stream()
                    .filter(m -> from == null || !m.occurredAt().isBefore(from))
                    .filter(m -> to == null || m.occurredAt().isBefore(to))
                    .toList();
        }

        @Override
        public com.kardexa.shared.application.ResultPage<
                com.kardexa.inventory.application.port.in.ListStockMovementsUseCase.MovementEntry> findAll(
                Instant from, Instant to, com.kardexa.inventory.domain.model.MovementType type,
                com.kardexa.shared.application.PageQuery page) {
            throw new UnsupportedOperationException();
        }
    }, id -> {
        if (!exists) {
            throw new IllegalStateException("no encontrado");
        }
        return new StockProduct(id, "Arroz 5 kg", true);
    });

    private void movement(Direction direction, String quantity, String balance, String instant) {
        rows.add(StockMovement.fromAdjustment(StockAdjustment.create(PRODUCT, direction, Quantity.of(quantity),
                AdjustmentReason.CORRECTION, null, Instant.parse(instant)), Quantity.of(balance)));
    }

    @Test
    @DisplayName("sin movimientos: saldo 0 y lista vacía")
    void emptyKardex() {
        Kardex kardex = service.get(PRODUCT, null, null);

        assertThat(kardex.productName()).isEqualTo("Arroz 5 kg");
        assertThat(kardex.openingBalance()).isEqualTo(Quantity.ZERO);
        assertThat(kardex.closingBalance()).isEqualTo(Quantity.ZERO);
        assertThat(kardex.movements()).isEmpty();
    }

    @Test
    @DisplayName("sin fechas: todo el historial, y el saldo final es el del último movimiento")
    void fullHistory() {
        movement(Direction.IN, "20", "20", "2026-11-01T15:00:00Z");
        movement(Direction.OUT, "3", "17", "2026-11-02T15:00:00Z");

        Kardex kardex = service.get(PRODUCT, null, null);

        assertThat(kardex.openingBalance()).isEqualTo(Quantity.ZERO);
        assertThat(kardex.closingBalance()).isEqualTo(Quantity.of("17"));
        assertThat(kardex.movements()).hasSize(2);
    }

    @Test
    @DisplayName("con rango: saldo inicial del rango y solo sus movimientos")
    void range() {
        movement(Direction.IN, "20", "20", "2026-11-01T15:00:00Z");
        movement(Direction.OUT, "3", "17", "2026-11-02T15:00:00Z");
        movement(Direction.OUT, "2", "15", "2026-11-03T15:00:00Z");

        Kardex kardex = service.get(PRODUCT, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-02"));

        assertThat(kardex.openingBalance()).isEqualTo(Quantity.of("20"));
        assertThat(kardex.closingBalance()).isEqualTo(Quantity.of("17"));
        assertThat(kardex.movements()).singleElement()
                .satisfies(m -> assertThat(m.balanceAfter()).isEqualTo(Quantity.of("17")));
    }

    @Test
    @DisplayName("un rango sin movimientos conserva el saldo que traía")
    void rangeWithoutMovements() {
        movement(Direction.IN, "20", "20", "2026-11-01T15:00:00Z");

        Kardex kardex = service.get(PRODUCT, LocalDate.parse("2026-11-05"), null);

        assertThat(kardex.openingBalance()).isEqualTo(Quantity.of("20"));
        assertThat(kardex.closingBalance()).isEqualTo(Quantity.of("20"));
        assertThat(kardex.movements()).isEmpty();
    }

    @Test
    @DisplayName("RNF-15: los días del rango son días en Lima, no en UTC")
    void usesLimaDays() {
        movement(Direction.IN, "1", "1", "2026-11-08T04:59:59Z"); // aún 7 de noviembre en Lima
        movement(Direction.IN, "1", "2", "2026-11-08T05:00:00Z"); // ya 8 de noviembre en Lima
        movement(Direction.IN, "1", "3", "2026-11-09T04:59:59Z"); // aún 8 de noviembre en Lima
        movement(Direction.IN, "1", "4", "2026-11-09T05:00:00Z"); // ya 9 de noviembre en Lima

        Kardex kardex = service.get(PRODUCT, LocalDate.parse("2026-11-08"), LocalDate.parse("2026-11-08"));

        assertThat(kardex.openingBalance()).isEqualTo(Quantity.of("1"));
        assertThat(kardex.movements()).extracting(StockMovement::balanceAfter)
                .containsExactly(Quantity.of("2"), Quantity.of("3"));
    }

    @Test
    @DisplayName("si el catálogo no encuentra el producto, deja pasar su error")
    void propagatesNotFound() {
        exists = false;

        assertThatThrownBy(() -> service.get(PRODUCT, null, null)).isInstanceOf(IllegalStateException.class);
    }
}
