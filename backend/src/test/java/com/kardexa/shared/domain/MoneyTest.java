package com.kardexa.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Money (RN-12: 2 decimales, half-up)")
class MoneyTest {

    @ParameterizedTest(name = "{0} se normaliza a {1}")
    @CsvSource({"10, 10.00", "10.005, 10.01", "10.004, 10.00", "0.125, 0.13", "-2.345, -2.35"})
    void normalizesToTwoDecimalsHalfUp(String input, String expected) {
        assertThat(Money.of(input)).hasToString(expected);
    }

    @Test
    void addsAmounts() {
        assertThat(Money.of("12.50").add(Money.of("0.75"))).isEqualTo(Money.of("13.25"));
    }

    @Test
    @DisplayName("precio × cantidad a granel redondea el subtotal")
    void multipliesByBulkQuantity() {
        // 4.90 soles/kg × 1.255 kg = 6.14950 → 6.15
        assertThat(Money.of("4.90").multiply(Quantity.of("1.255"))).isEqualTo(Money.of("6.15"));
    }

    @Test
    void equalityIgnoresInputScale() {
        assertThat(Money.of("5")).isEqualTo(Money.of("5.00"));
    }

    @Test
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> new Money(null));
    }
}
