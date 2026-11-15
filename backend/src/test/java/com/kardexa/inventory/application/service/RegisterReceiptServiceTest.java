package com.kardexa.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.DocumentMovement;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.RecordStockMovementsCommand;
import com.kardexa.inventory.application.port.in.RegisterReceiptUseCase.Receipt;
import com.kardexa.inventory.application.port.in.RegisterReceiptUseCase.RegisterReceiptCommand;
import com.kardexa.inventory.domain.exception.InvalidReceiptException;
import com.kardexa.shared.domain.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Entradas de mercadería (HU-21)")
class RegisterReceiptServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");
    private static final UUID SITE = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    private final List<RecordStockMovementsCommand> recorded = new ArrayList<>();
    private final RegisterReceiptService service =
            new RegisterReceiptService(recorded::add, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("registra los movimientos RECEIPT de la sede con un documento que agrupa las líneas")
    void recordsReceiptMovements() {
        Line pan = new Line(UUID.randomUUID(), Quantity.of("40"));
        Line bocadillo = new Line(UUID.randomUUID(), Quantity.of("12.5"));

        Receipt receipt = service.register(new RegisterReceiptCommand(SITE, USER, List.of(pan, bocadillo)));

        assertThat(recorded).hasSize(1);
        RecordStockMovementsCommand command = recorded.getFirst();
        assertThat(command.movement()).isEqualTo(DocumentMovement.RECEIPT);
        assertThat(command.siteId()).isEqualTo(SITE);
        assertThat(command.userId()).isEqualTo(USER);
        assertThat(command.documentId()).isEqualTo(receipt.id());
        assertThat(command.lines()).containsExactly(pan, bocadillo);
        assertThat(receipt.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("rechaza una entrada vacía, con cantidad 0 o con un producto repetido")
    void rejectsInvalidReceipts() {
        UUID product = UUID.randomUUID();

        assertThatThrownBy(() -> service.register(new RegisterReceiptCommand(SITE, USER, List.of())))
                .isInstanceOf(InvalidReceiptException.class);
        assertThatThrownBy(() -> service.register(
                new RegisterReceiptCommand(SITE, USER, List.of(new Line(product, Quantity.ZERO)))))
                .isInstanceOf(InvalidReceiptException.class);
        assertThatThrownBy(() -> service.register(new RegisterReceiptCommand(SITE, USER,
                List.of(new Line(product, Quantity.of("1")), new Line(product, Quantity.of("2"))))))
                .isInstanceOf(InvalidReceiptException.class);
        assertThat(recorded).isEmpty();
    }
}
