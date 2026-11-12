package com.kardexa.sales.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.sales.domain.exception.InvalidSaleException;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Venta (HU-14)")
class SaleTest {

    private static final UUID SELLER = UUID.randomUUID();

    @Test
    @DisplayName("el total suma los subtotales redondeados half-up (RN-12)")
    void total() {
        Sale sale = Sale.register(List.of(item(UUID.randomUUID(), "2.5", "3.99"), item(UUID.randomUUID(), "1", "10.00")),
                SELLER, Instant.now());

        assertThat(sale.items().get(0).subtotal()).isEqualTo(Money.of("9.98"));
        assertThat(sale.total()).isEqualTo(Money.of("19.98"));
        assertThat(sale.status()).isEqualTo(SaleStatus.REGISTERED);
        assertThat(sale.number()).isNull();
    }

    @Test
    @DisplayName("sin productos, con un producto repetido o con cantidad 0 se rechaza")
    void rejectsInvalidItems() {
        UUID product = UUID.randomUUID();
        assertThatThrownBy(() -> Sale.register(List.of(), SELLER, Instant.now()))
                .isInstanceOf(InvalidSaleException.class);
        assertThatThrownBy(() -> Sale.register(List.of(item(product, "1", "2.00"), item(product, "2", "2.00")), SELLER,
                Instant.now()))
                .isInstanceOf(InvalidSaleException.class)
                .hasMessageContaining("repetirse");
        assertThatThrownBy(() -> item(product, "0", "2.00")).isInstanceOf(InvalidSaleException.class);
    }

    @Test
    @DisplayName("RN-07: el número se asigna una sola vez y se formatea con 6 dígitos")
    void number() {
        Sale sale = Sale.register(List.of(item(UUID.randomUUID(), "1", "2.00")), SELLER, Instant.now());
        sale.assignNumber(new SaleNumber(123));

        assertThat(sale.number()).hasToString("V-000123");
        assertThat(SaleNumber.parse("V-1234567").value()).isEqualTo(1_234_567);
        assertThatThrownBy(() -> sale.assignNumber(new SaleNumber(124))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SaleNumber.parse("V-12")).isInstanceOf(IllegalArgumentException.class);
    }

    private static SaleItem item(UUID productId, String quantity, String price) {
        return new SaleItem(productId, "P-1", "Producto", Quantity.of(quantity), Money.of(price));
    }
}
