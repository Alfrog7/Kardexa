package com.kardexa.purchasing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.purchasing.domain.exception.InvalidPurchaseException;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Compra (HU-12)")
class PurchaseTest {

    private static final UUID SUPPLIER = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    private static PurchaseItem item(UUID productId, String quantity, String cost) {
        return new PurchaseItem(productId, Quantity.of(quantity), Money.of(cost));
    }

    private static Purchase purchase(String document, List<PurchaseItem> items) {
        return Purchase.register(SUPPLIER, document, DATE, items, UUID.randomUUID(), Instant.now());
    }

    @Test
    @DisplayName("nace registrada y su total es la suma de los subtotales redondeados")
    void computesTotal() {
        Purchase purchase = purchase(" F001-123 ", List.of(
                item(UUID.randomUUID(), "2.5", "3.33"),
                item(UUID.randomUUID(), "10", "1.20")));

        assertThat(purchase.status()).isEqualTo(PurchaseStatus.REGISTERED);
        assertThat(purchase.supplierDocument()).isEqualTo("F001-123");
        assertThat(purchase.items().get(0).subtotal()).isEqualTo(Money.of("8.33"));
        assertThat(purchase.total()).isEqualTo(Money.of("20.33"));
    }

    @Test
    @DisplayName("un documento en blanco se guarda como ausente; más de 40 caracteres se rechaza")
    void normalizesDocument() {
        assertThat(purchase("  ", List.of(item(UUID.randomUUID(), "1", "1"))).supplierDocument()).isNull();
        assertThatThrownBy(() -> purchase("x".repeat(41), List.of(item(UUID.randomUUID(), "1", "1"))))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    @DisplayName("criterio 3: una cantidad de 0 o un costo negativo se rechazan")
    void rejectsInvalidItems() {
        assertThatThrownBy(() -> item(UUID.randomUUID(), "0", "1.00"))
                .isInstanceOf(InvalidPurchaseException.class)
                .hasFieldOrPropertyWithValue("rule", "RF-16");
        assertThatThrownBy(() -> item(UUID.randomUUID(), "1", "-0.01")).isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    @DisplayName("se rechaza sin productos o con un producto repetido")
    void rejectsEmptyOrRepeated() {
        UUID rice = UUID.randomUUID();

        assertThatThrownBy(() -> purchase(null, List.of())).isInstanceOf(InvalidPurchaseException.class);
        assertThatThrownBy(() -> purchase(null, List.of(item(rice, "1", "2"), item(rice, "3", "2"))))
                .isInstanceOf(InvalidPurchaseException.class)
                .hasMessageContaining("repetirse");
    }

    @Test
    @DisplayName("HU-13: se anula una sola vez y guarda quién, cuándo y por qué")
    void voidsOnce() {
        Purchase purchase = purchase(null, List.of(item(UUID.randomUUID(), "1", "1")));
        UUID admin = UUID.randomUUID();
        Instant at = Instant.parse("2026-11-11T18:00:00Z");

        purchase.voidWith(new Purchase.Voiding(" Factura  duplicada ", admin, at));

        assertThat(purchase.status()).isEqualTo(PurchaseStatus.VOIDED);
        assertThat(purchase.voiding()).isEqualTo(new Purchase.Voiding("Factura duplicada", admin, at));
        assertThatThrownBy(() -> purchase.voidWith(new Purchase.Voiding("Otra vez", admin, at)))
                .isInstanceOf(com.kardexa.purchasing.domain.exception.PurchaseAlreadyVoidedException.class)
                .hasFieldOrPropertyWithValue("rule", "RF-17");
        assertThatThrownBy(() -> new Purchase.Voiding("no", admin, at)).isInstanceOf(InvalidPurchaseException.class);
    }
}
