package com.kardexa.inventory.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Respuesta del movimiento: quién lo hizo")
class StockMovementResponseTest {

    private static StockMovement movement(UUID userId) {
        StockAdjustment adjustment = StockAdjustment.create(UUID.randomUUID(), Direction.IN, Quantity.of("1"),
                AdjustmentReason.CORRECTION, null, userId, Instant.EPOCH);
        return StockMovement.fromAdjustment(adjustment, Quantity.of("1"));
    }

    @Test
    @DisplayName("muestra el nombre real del usuario que lo registró")
    void showsRealName() {
        UUID user = UUID.randomUUID();

        assertThat(StockMovementResponse.from(movement(user), Map.of(user, "Ana Pérez")).user())
                .isEqualTo("Ana Pérez");
    }

    @Test
    @DisplayName("un movimiento anterior a la autenticación, sin usuario, se muestra como Sistema")
    void legacyMovementIsSystem() {
        assertThat(StockMovementResponse.from(movement(null), Map.of()).user()).isEqualTo("Sistema");
    }
}
