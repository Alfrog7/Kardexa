package com.kardexa.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.catalog.application.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.kardexa.catalog.application.port.in.UpdateCategoryUseCase.UpdateCategoryCommand;
import com.kardexa.catalog.domain.exception.CategoryNotFoundException;
import com.kardexa.catalog.domain.exception.DuplicateCategoryNameException;
import com.kardexa.catalog.domain.model.Category;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Casos de uso de categorías (HU-05)")
class CategoryServiceTest {

    private InMemoryCategoryRepository repository;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCategoryRepository();
        service = new CategoryService(repository);
    }

    @Test
    @DisplayName("crea una categoría con un nombre nuevo")
    void createsCategory() {
        Category created = service.create(new CreateCategoryCommand("Bebidas"));

        assertThat(repository.findById(created.id())).isPresent();
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("no permite dos categorías con el mismo nombre, sin importar mayúsculas")
    void rejectsDuplicateName() {
        service.create(new CreateCategoryCommand("Bebidas"));

        assertThatThrownBy(() -> service.create(new CreateCategoryCommand("  BEBIDAS ")))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    @DisplayName("desactiva una categoría sin borrarla")
    void deactivatesCategory() {
        Category created = service.create(new CreateCategoryCommand("Lácteos"));

        service.update(new UpdateCategoryCommand(created.id(), "Lácteos", false));

        assertThat(service.listAll()).singleElement()
                .satisfies(c -> assertThat(c.isActive()).isFalse());
    }

    @Test
    @DisplayName("permite cambiar solo las mayúsculas de su propio nombre")
    void renamesKeepingSameName() {
        Category created = service.create(new CreateCategoryCommand("lacteos"));

        Category updated = service.update(new UpdateCategoryCommand(created.id(), "Lacteos", true));

        assertThat(updated.name()).isEqualTo("Lacteos");
    }

    @Test
    @DisplayName("no permite renombrar a un nombre que ya usa otra categoría")
    void rejectsRenameToTakenName() {
        service.create(new CreateCategoryCommand("Bebidas"));
        Category snacks = service.create(new CreateCategoryCommand("Snacks"));

        assertThatThrownBy(() -> service.update(new UpdateCategoryCommand(snacks.id(), "bebidas", true)))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    @DisplayName("falla si la categoría no existe")
    void failsWhenNotFound() {
        assertThatThrownBy(() -> service.update(new UpdateCategoryCommand(UUID.randomUUID(), "Bebidas", true)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("lista ordenado por nombre sin distinguir mayúsculas")
    void listsOrderedByName() {
        service.create(new CreateCategoryCommand("snacks"));
        service.create(new CreateCategoryCommand("Bebidas"));
        service.create(new CreateCategoryCommand("lácteos"));

        assertThat(service.listAll()).extracting(Category::name)
                .containsExactly("Bebidas", "lácteos", "snacks");
    }
}
