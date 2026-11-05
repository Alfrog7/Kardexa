package com.kardexa.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.catalog.domain.exception.InvalidCategoryNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Category")
class CategoryTest {

    @Test
    @DisplayName("una categoría nueva nace activa y con identificador")
    void newCategoryIsActive() {
        Category category = Category.create("Bebidas");

        assertThat(category.id()).isNotNull();
        assertThat(category.name()).isEqualTo("Bebidas");
        assertThat(category.isActive()).isTrue();
    }

    @Test
    @DisplayName("normaliza los espacios del nombre")
    void normalizesWhitespace() {
        assertThat(Category.create("  Bebidas   frías ").name()).isEqualTo("Bebidas frías");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", "  B  "})
    @DisplayName("rechaza nombres vacíos o de menos de 2 caracteres")
    void rejectsShortNames(String name) {
        assertThatThrownBy(() -> Category.create(name)).isInstanceOf(InvalidCategoryNameException.class);
    }

    @Test
    @DisplayName("acepta 60 caracteres y rechaza 61")
    void enforcesMaxLength() {
        assertThat(Category.create("x".repeat(60)).name()).hasSize(60);
        assertThatThrownBy(() -> Category.create("x".repeat(61)))
                .isInstanceOf(InvalidCategoryNameException.class);
    }

    @Test
    @DisplayName("desactivar conserva la categoría y se puede reactivar")
    void deactivateAndReactivate() {
        Category category = Category.create("Lácteos");

        category.deactivate();
        assertThat(category.isActive()).isFalse();

        category.activate();
        assertThat(category.isActive()).isTrue();
    }

    @Test
    @DisplayName("renombrar aplica las mismas reglas que crear")
    void renameValidates() {
        Category category = Category.create("Limpieza");

        category.rename(" Aseo ");
        assertThat(category.name()).isEqualTo("Aseo");
        assertThatThrownBy(() -> category.rename("")).isInstanceOf(InvalidCategoryNameException.class);
    }
}
