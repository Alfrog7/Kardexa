package com.kardexa.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Quantity (RN-12: hasta 3 decimales, nunca negativa)")
class QuantityTest {

    @Test
    void acceptsUpToThreeDecimals() {
        assertThat(Quantity.of("1.250")).hasToString("1.250");
        assertThat(Quantity.of("2")).hasToString("2.000");
    }

    @Test
    void rejectsMoreThanThreeDecimals() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Quantity.of("1.2345"))
                .withMessageContaining("3 decimales");
    }

    @Test
    void rejectsNegativeValues() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Quantity.of("-1"))
                .withMessageContaining("negativa");
    }

    @Test
    void addsAndSubtracts() {
        Quantity stock = Quantity.of("10").add(Quantity.of("2.5"));
        assertThat(stock.subtract(Quantity.of("12.5"))).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("restar más de lo disponible falla en vez de dar negativo")
    void subtractingMoreThanAvailableFails() {
        assertThatIllegalArgumentException().isThrownBy(() -> Quantity.of("1").subtract(Quantity.of("2")));
    }

    @Test
    void comparesValues() {
        assertThat(Quantity.of("0.999").isLessThan(Quantity.of("1"))).isTrue();
        assertThat(Quantity.ZERO.isPositive()).isFalse();
    }
}
