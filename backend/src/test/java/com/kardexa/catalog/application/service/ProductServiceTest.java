package com.kardexa.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.catalog.application.port.in.ProductView;
import com.kardexa.catalog.application.port.in.UpdateProductUseCase.UpdateProductCommand;
import com.kardexa.catalog.domain.exception.CategoryNotFoundException;
import com.kardexa.catalog.domain.exception.DuplicateProductCodeException;
import com.kardexa.catalog.domain.exception.InactiveCategoryException;
import com.kardexa.catalog.domain.exception.ProductNotFoundException;
import com.kardexa.catalog.domain.model.Category;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.catalog.domain.model.Unit;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Casos de uso de productos (HU-06)")
class ProductServiceTest {

    private final Map<UUID, Quantity> stock = new HashMap<>();
    private InMemoryCategoryRepository categories;
    private ProductService service;
    private Category drinks;

    @BeforeEach
    void setUp() {
        categories = new InMemoryCategoryRepository();
        service = new ProductService(new InMemoryProductRepository(), categories,
                productId -> stock.getOrDefault(productId, Quantity.ZERO));
        drinks = categories.save(Category.create("Bebidas"));
    }

    private static Product.Details details(String code, UUID categoryId) {
        return new Product.Details(code, "Gaseosa 500 ml", categoryId, Unit.UNIT, Money.of("3.50"), Quantity.of("5"));
    }

    private Category inactiveCategory(String name) {
        Category category = Category.create(name);
        category.deactivate();
        return categories.save(category);
    }

    @Test
    @DisplayName("crea un producto con stock 0, en stock crítico y con el nombre de su categoría")
    void createsWithZeroStock() {
        ProductView view = service.create(details("AB-001", drinks.id()));

        assertThat(view.stock()).isEqualTo(Quantity.ZERO);
        assertThat(view.critical()).isTrue();
        assertThat(view.categoryName()).isEqualTo("Bebidas");
    }

    @Test
    @DisplayName("RN-10: rechaza un código ya registrado, sin importar mayúsculas")
    void rejectsDuplicateCode() {
        service.create(details("AB-001", drinks.id()));

        assertThatThrownBy(() -> service.create(details(" ab-001 ", drinks.id())))
                .isInstanceOf(DuplicateProductCodeException.class);
    }

    @Test
    @DisplayName("rechaza una categoría inexistente")
    void rejectsUnknownCategory() {
        assertThatThrownBy(() -> service.create(details("AB-001", UUID.randomUUID())))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("HU-05: una categoría desactivada no se ofrece para productos nuevos")
    void rejectsInactiveCategoryOnCreate() {
        Category old = inactiveCategory("Temporada");

        assertThatThrownBy(() -> service.create(details("AB-001", old.id())))
                .isInstanceOf(InactiveCategoryException.class);
    }

    @Test
    @DisplayName("no permite mover un producto a una categoría desactivada")
    void rejectsMoveToInactiveCategory() {
        ProductView created = service.create(details("AB-001", drinks.id()));
        Category old = inactiveCategory("Temporada");

        assertThatThrownBy(() -> service.update(
                new UpdateProductCommand(created.product().id(), details("AB-001", old.id()), true)))
                .isInstanceOf(InactiveCategoryException.class);
    }

    @Test
    @DisplayName("sí permite editar un producto cuya categoría se desactivó después")
    void editsProductInDeactivatedCategory() {
        ProductView created = service.create(details("AB-001", drinks.id()));
        drinks.deactivate();
        categories.save(drinks);

        ProductView updated = service.update(
                new UpdateProductCommand(created.product().id(), details("AB-001", drinks.id()), true));

        assertThat(updated.categoryName()).isEqualTo("Bebidas");
    }

    @Test
    @DisplayName("RF-09: desactiva el producto en lugar de borrarlo")
    void deactivates() {
        UUID id = service.create(details("AB-001", drinks.id())).product().id();

        service.update(new UpdateProductCommand(id, details("AB-001", drinks.id()), false));

        assertThat(service.get(id).product().isActive()).isFalse();
    }

    @Test
    @DisplayName("no permite cambiar el código a uno que usa otro producto")
    void rejectsCodeTakenByAnother() {
        service.create(details("AB-001", drinks.id()));
        UUID other = service.create(details("AB-002", drinks.id())).product().id();

        assertThatThrownBy(() -> service.update(new UpdateProductCommand(other, details("AB-001", drinks.id()), true)))
                .isInstanceOf(DuplicateProductCodeException.class);
    }

    @Test
    @DisplayName("muestra el stock que informa el inventario y deja de ser crítico sobre el mínimo")
    void reflectsInventoryStock() {
        UUID id = service.create(details("AB-001", drinks.id())).product().id();
        stock.put(id, Quantity.of("12.5"));

        ProductView view = service.get(id);

        assertThat(view.stock()).isEqualTo(Quantity.of("12.5"));
        assertThat(view.critical()).isFalse();
    }

    @Test
    @DisplayName("falla si el producto no existe")
    void failsWhenNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ProductNotFoundException.class);
    }
}
