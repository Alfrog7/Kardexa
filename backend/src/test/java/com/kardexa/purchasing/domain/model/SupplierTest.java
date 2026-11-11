package com.kardexa.purchasing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.purchasing.domain.exception.InvalidRucException;
import com.kardexa.purchasing.domain.exception.InvalidSupplierDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Proveedor (HU-11)")
class SupplierTest {

    private static Supplier.Details details(String ruc, String name) {
        return new Supplier.Details(ruc, name, null, null);
    }

    @Test
    @DisplayName("se crea activo, con el RUC y la razón social normalizados")
    void createsActiveAndNormalized() {
        Supplier supplier = Supplier.create(
                new Supplier.Details(" 20123456789 ", "  Distribuidora   Lima  SAC ", "  ", " ventas@dl.pe "));

        assertThat(supplier.isActive()).isTrue();
        assertThat(supplier.ruc()).isEqualTo("20123456789");
        assertThat(supplier.businessName()).isEqualTo("Distribuidora Lima SAC");
        assertThat(supplier.details().phone()).isNull();
        assertThat(supplier.details().email()).isEqualTo("ventas@dl.pe");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2012345678", "201234567890", "2012345678A", "20 12345678"})
    @DisplayName("RN-10: un RUC que no son 11 dígitos se rechaza")
    void rejectsInvalidRuc(String ruc) {
        assertThatThrownBy(() -> details(ruc, "Distribuidora Lima"))
                .isInstanceOf(InvalidRucException.class)
                .hasFieldOrPropertyWithValue("rule", "RN-10");
    }

    @Test
    @DisplayName("la razón social debe tener entre 2 y 150 caracteres")
    void validatesBusinessName() {
        assertThatThrownBy(() -> details("20123456789", " A ")).isInstanceOf(InvalidSupplierDataException.class);
        assertThatThrownBy(() -> details("20123456789", "x".repeat(151)))
                .isInstanceOf(InvalidSupplierDataException.class);
    }

    @Test
    @DisplayName("el teléfono admite hasta 20 caracteres")
    void validatesPhone() {
        assertThatThrownBy(() -> new Supplier.Details("20123456789", "Distribuidora", "9".repeat(21), null))
                .isInstanceOf(InvalidSupplierDataException.class);
    }

    @Test
    @DisplayName("se edita, se desactiva y se reactiva sin cambiar de id")
    void updatesAndToggles() {
        Supplier supplier = Supplier.create(details("20123456789", "Distribuidora"));

        supplier.update(details("10456789012", "Bodega Central"));
        supplier.deactivate();

        assertThat(supplier.ruc()).isEqualTo("10456789012");
        assertThat(supplier.businessName()).isEqualTo("Bodega Central");
        assertThat(supplier.isActive()).isFalse();

        supplier.activate();
        assertThat(supplier.isActive()).isTrue();
    }
}
