package com.kardexa.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.purchasing.application.port.in.VoidPurchaseUseCase.VoidPurchaseCommand;
import com.kardexa.purchasing.domain.exception.InvalidPurchaseException;
import com.kardexa.purchasing.domain.exception.PurchaseAlreadyVoidedException;
import com.kardexa.purchasing.domain.exception.PurchaseNotFoundException;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.purchasing.domain.model.PurchaseItem;
import com.kardexa.purchasing.domain.model.PurchaseStatus;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Anular una compra (HU-13)")
class VoidPurchaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T18:00:00Z");
    private static final UUID ADMIN = UUID.randomUUID();

    private final InMemoryPurchaseRepository purchases = new InMemoryPurchaseRepository();
    private final FakeStockLedger ledger = new FakeStockLedger();
    private final VoidPurchaseService service =
            new VoidPurchaseService(purchases, ledger, Clock.fixed(NOW, ZoneOffset.UTC));
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        purchase = Purchase.register(UUID.randomUUID(), "F001-9", LocalDate.of(2026, 11, 10),
                List.of(new PurchaseItem(UUID.randomUUID(), Quantity.of("4"), Money.of("2.50"))),
                UUID.randomUUID(), NOW.minusSeconds(3600));
        purchases.add(purchase);
    }

    private Purchase voidIt(UUID id, String reason) {
        return service.voidPurchase(new VoidPurchaseCommand(id, reason, ADMIN));
    }

    @Test
    @DisplayName("criterio 1: pide las salidas inversas y deja la compra anulada con su motivo")
    void voidsPurchase() {
        Purchase voided = voidIt(purchase.id(), "  Factura   duplicada ");

        assertThat(ledger.reversed).containsExactly(voided);
        Purchase saved = purchases.findById(purchase.id()).orElseThrow();
        assertThat(saved.status()).isEqualTo(PurchaseStatus.VOIDED);
        assertThat(saved.voiding().reason()).isEqualTo("Factura duplicada");
        assertThat(saved.voiding().userId()).isEqualTo(ADMIN);
        assertThat(saved.voiding().at()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("criterio 2: si el inventario la rechaza (RN-01), la compra sigue registrada")
    void keepsPurchaseWhenInventoryRejects() {
        ledger.failure = new IllegalStateException("stock insuficiente");

        assertThatThrownBy(() -> voidIt(purchase.id(), "Error de digitación")).isSameAs(ledger.failure);
        assertThat(purchases.findById(purchase.id()).orElseThrow().status()).isEqualTo(PurchaseStatus.REGISTERED);
    }

    @Test
    @DisplayName("no se anula dos veces, sin motivo ni una compra inexistente")
    void rejectsInvalidVoids() {
        voidIt(purchase.id(), "Factura duplicada");

        assertThatThrownBy(() -> voidIt(purchase.id(), "Otra vez")).isInstanceOf(PurchaseAlreadyVoidedException.class);
        assertThatThrownBy(() -> voidIt(UUID.randomUUID(), "Motivo")).isInstanceOf(PurchaseNotFoundException.class);
        assertThat(ledger.reversed).hasSize(1);
    }

    @Test
    @DisplayName("un motivo de menos de 3 caracteres se rechaza antes de tocar el inventario")
    void rejectsShortReason() {
        assertThatThrownBy(() -> voidIt(purchase.id(), " x ")).isInstanceOf(InvalidPurchaseException.class);
        assertThat(ledger.reversed).isEmpty();
    }
}
