package com.kardexa.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.inventory.domain.exception.InsufficientStockException;
import com.kardexa.inventory.domain.exception.InvalidStockAdjustmentException;
import com.kardexa.inventory.domain.exception.StockLimitExceededException;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dominio del inventario")
class StockDomainTest {

    private static final UUID PRODUCT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    private static ProductStock stock(String quantity) {
        return new ProductStock(PRODUCT, Quantity.of(quantity), 4);
    }

    private static StockAdjustment adjustment(String quantity, AdjustmentReason reason, String note) {
        return StockAdjustment.create(PRODUCT, Direction.OUT, Quantity.of(quantity), reason, note, NOW);
    }

    @Test
    @DisplayName("una entrada suma y una salida resta, conservando la versión leída")
    void appliesMovements() {
        assertThat(stock("20").apply(Direction.OUT, Quantity.of("3"), "Arroz")).isEqualTo(stock("17"));
        assertThat(stock("20").apply(Direction.IN, Quantity.of("0.5"), "Arroz")).isEqualTo(stock("20.5"));
    }

    @Test
    @DisplayName("RN-01: permite dejar el stock en 0, pero no en negativo")
    void neverNegative() {
        assertThat(stock("3").apply(Direction.OUT, Quantity.of("3"), "Arroz")).isEqualTo(stock("0"));

        assertThatThrownBy(() -> stock("3").apply(Direction.OUT, Quantity.of("3.001"), "Arroz"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Stock insuficiente para \"Arroz\": disponible 3.000, solicitado 3.001.");
    }

    @Test
    @DisplayName("un producto sin fila tiene stock 0 y aún no tiene versión")
    void emptyStock() {
        ProductStock empty = ProductStock.empty(PRODUCT);

        assertThat(empty.quantity()).isEqualTo(Quantity.ZERO);
        assertThat(empty.version()).isEqualTo(ProductStock.NEVER_SAVED);
    }

    @Test
    @DisplayName("rechaza un stock mayor que el máximo de la columna")
    void rejectsOverflow() {
        assertThat(stock("999999999").apply(Direction.IN, Quantity.of("0.999"), "Arroz").quantity())
                .isEqualTo(ProductStock.MAX_QUANTITY);

        assertThatThrownBy(() -> stock("999999999.999").apply(Direction.IN, Quantity.of("0.001"), "Arroz"))
                .isInstanceOf(StockLimitExceededException.class);
    }

    @Test
    @DisplayName("el ajuste exige una cantidad mayor que 0")
    void rejectsZeroQuantity() {
        assertThatThrownBy(() -> adjustment("0", AdjustmentReason.SHRINKAGE, null))
                .isInstanceOf(InvalidStockAdjustmentException.class);
    }

    @Test
    @DisplayName("RN-05: el ajuste exige un motivo")
    void requiresReason() {
        assertThatThrownBy(() -> adjustment("1", null, null))
                .isInstanceOf(InvalidStockAdjustmentException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    @DisplayName("normaliza la nota y limita su longitud")
    void normalizesNote() {
        assertThat(adjustment("1", AdjustmentReason.SHRINKAGE, "  rotura  ").note()).isEqualTo("rotura");
        assertThat(adjustment("1", AdjustmentReason.SHRINKAGE, "   ").note()).isNull();
        assertThatThrownBy(() -> adjustment("1", AdjustmentReason.SHRINKAGE, "x".repeat(256)))
                .isInstanceOf(InvalidStockAdjustmentException.class);
    }

    @Test
    @DisplayName("RN-04: el movimiento apunta al ajuste que lo originó, con su saldo")
    void movementFromAdjustment() {
        StockAdjustment adjustment = adjustment("3", AdjustmentReason.SHRINKAGE, null);

        StockMovement movement = StockMovement.fromAdjustment(adjustment, Quantity.of("17"));

        assertThat(movement.type()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(movement.sourceType()).isEqualTo(SourceType.ADJUSTMENT);
        assertThat(movement.sourceId()).isEqualTo(adjustment.id());
        assertThat(movement.direction()).isEqualTo(Direction.OUT);
        assertThat(movement.quantity()).isEqualTo(Quantity.of("3"));
        assertThat(movement.balanceAfter()).isEqualTo(Quantity.of("17"));
        assertThat(movement.occurredAt()).isEqualTo(NOW);
    }
}
