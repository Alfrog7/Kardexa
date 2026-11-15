package com.kardexa.sites.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.sites.domain.exception.InvalidSiteDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Sede (HU-19)")
class SiteTest {

    @Test
    @DisplayName("se crea habilitada, con el nombre y la dirección normalizados")
    void createsActiveAndNormalized() {
        Site site = Site.create(new Site.Details("  Panadería   Galeno ", "  Av.  Larco 123  "));

        assertThat(site.name()).isEqualTo("Panadería Galeno");
        assertThat(site.details().address()).isEqualTo("Av. Larco 123");
        assertThat(site.isActive()).isTrue();
        assertThat(site.id()).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "A"})
    @DisplayName("un nombre vacío o de un solo carácter se rechaza")
    void rejectsShortName(String name) {
        assertThatThrownBy(() -> new Site.Details(name, null)).isInstanceOf(InvalidSiteDataException.class);
    }

    @Test
    @DisplayName("un nombre o una dirección demasiado largos se rechazan")
    void rejectsLongValues() {
        assertThatThrownBy(() -> new Site.Details("x".repeat(Site.NAME_MAX_LENGTH + 1), null))
                .isInstanceOf(InvalidSiteDataException.class);
        assertThatThrownBy(() -> new Site.Details("Galeno", "x".repeat(Site.ADDRESS_MAX_LENGTH + 1)))
                .isInstanceOf(InvalidSiteDataException.class);
    }

    @Test
    @DisplayName("una dirección en blanco se guarda como ausente")
    void blankAddressIsAbsent() {
        assertThat(new Site.Details("Galeno", "   ").address()).isNull();
    }

    @Test
    @DisplayName("se inhabilita y se vuelve a habilitar sin perder sus datos")
    void togglesActive() {
        Site site = Site.create(new Site.Details("Galeno", null));

        site.deactivate();
        assertThat(site.isActive()).isFalse();
        site.activate();
        assertThat(site.isActive()).isTrue();
        assertThat(site.name()).isEqualTo("Galeno");
    }
}
