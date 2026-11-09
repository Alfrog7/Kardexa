package com.kardexa.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.catalog.domain.exception.InvalidProductException;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Product")
class ProductTest {

    private static final UUID CATEGORY = UUID.randomUUID();

    private static Product.Details details(String code, String name, String price, String minStock) {
        return new Product.Details(code, name, CATEGORY, Unit.UNIT, Money.of(price), Quantity.of(minStock));
    }

    @Test
    @DisplayName("un producto nuevo nace activo y con costo referencial 0")
    void newProductDefaults() {
        Product product = Product.create(details("AB-001", "Gaseosa 500 ml", "3.50", "5"));

        assertThat(product.id()).isNotNull();
        assertThat(product.isActive()).isTrue();
        assertThat(product.referenceCost()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("normaliza los espacios del código y del nombre")
    void normalizesText() {
        Product product = Product.create(details("  AB-001 ", "  Gaseosa   500 ml ", "3.50", "5"));

        assertThat(product.code()).isEqualTo("AB-001");
        assertThat(product.name()).isEqualTo("Gaseosa 500 ml");
    }

    @Test
    @DisplayName("rechaza un precio de venta igual a 0")
    void rejectsZeroPrice() {
        assertThatThrownBy(() -> Product.create(details("AB-001", "Gaseosa", "0", "5")))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("mayor que 0");
    }

    @Test
    @DisplayName("rechaza códigos vacíos o de más de 40 caracteres")
    void rejectsInvalidCode() {
        assertThatThrownBy(() -> Product.create(details("  ", "Gaseosa", "3.50", "5")))
                .isInstanceOf(InvalidProductException.class);
        assertThatThrownBy(() -> Product.create(details("x".repeat(41), "Gaseosa", "3.50", "5")))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    @DisplayName("rechaza nombres de menos de 2 caracteres")
    void rejectsShortName() {
        assertThatThrownBy(() -> Product.create(details("AB-001", " G ", "3.50", "5")))
                .isInstanceOf(InvalidProductException.class);
    }

    @ParameterizedTest(name = "stock {0} con mínimo 5 → crítico: {1}")
    @CsvSource({"0, true", "4.999, true", "5, true", "5.001, false", "20, false"})
    @DisplayName("RN-09: crítico cuando el stock es menor o igual que el mínimo")
    void criticalStock(String stock, boolean critical) {
        Product product = Product.create(details("AB-001", "Gaseosa", "3.50", "5"));

        assertThat(product.isCritical(Quantity.of(stock))).isEqualTo(critical);
    }

    @Test
    @DisplayName("RF-09: se desactiva y se reactiva, nunca se borra")
    void deactivateAndReactivate() {
        Product product = Product.create(details("AB-001", "Gaseosa", "3.50", "5"));

        product.deactivate();
        assertThat(product.isActive()).isFalse();
        product.activate();
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("editar aplica las mismas reglas y conserva el costo referencial")
    void changeDetailsValidates() {
        Product product = Product.restore(UUID.randomUUID(), details("AB-001", "Gaseosa", "3.50", "5"),
                Money.of("2.10"), true);

        product.changeDetails(details("AB-002", "Gaseosa 1 L", "5.00", "2"));

        assertThat(product.code()).isEqualTo("AB-002");
        assertThat(product.salePrice()).isEqualTo(Money.of("5.00"));
        assertThat(product.referenceCost()).isEqualTo(Money.of("2.10"));
        assertThatThrownBy(() -> product.changeDetails(details("AB-002", "Gaseosa", "0.00", "2")))
                .isInstanceOf(InvalidProductException.class);
    }
}
