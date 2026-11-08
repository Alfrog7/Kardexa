package com.kardexa.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.catalog.application.port.in.ProductView;
import com.kardexa.catalog.application.port.in.SearchProductsUseCase.ProductSearch;
import com.kardexa.catalog.application.port.in.UpdateProductUseCase.UpdateProductCommand;
import com.kardexa.catalog.domain.exception.CategoryNotFoundException;
import com.kardexa.catalog.domain.exception.DuplicateProductCodeException;
import com.kardexa.catalog.domain.exception.InactiveCategoryException;
import com.kardexa.catalog.domain.exception.ProductNotFoundException;
import com.kardexa.catalog.domain.model.Category;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.catalog.domain.model.Unit;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Casos de uso de productos (HU-06)")
class ProductServiceTest {

    private final Map<UUID, Quantity> stock = new HashMap<>();
    private final List<Collection<UUID>> stockQueries = new ArrayList<>();
    private InMemoryCategoryRepository categories;
    private ProductService service;
    private Category drinks;

    @BeforeEach
    void setUp() {
        categories = new InMemoryCategoryRepository();
        service = new ProductService(new InMemoryProductRepository(), categories, ids -> {
            stockQueries.add(List.copyOf(ids));
            Map<UUID, Quantity> result = new HashMap<>();
            ids.forEach(id -> result.put(id, stock.getOrDefault(id, Quantity.ZERO)));
            return result;
        });
        drinks = categories.save(Category.create("Bebidas"));
    }

    private static Product.Details details(String code, UUID categoryId) {
        return details(code, "Gaseosa 500 ml", categoryId);
    }

    private static Product.Details details(String code, String name, UUID categoryId) {
        return new Product.Details(code, name, categoryId, Unit.UNIT, Money.of("3.50"), Quantity.of("5"));
    }

    private ResultPage<ProductView> search(String text, UUID categoryId, int page, int size) {
        return service.search(new ProductSearch(text, categoryId, new PageQuery(page, size)));
    }

    private static List<String> codes(ResultPage<ProductView> page) {
        return page.content().stream().map(v -> v.product().code()).toList();
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
    @DisplayName("HU-07: busca por parte del nombre o del código sin distinguir mayúsculas")
    void searchesByNameOrCode() {
        service.create(details("AB-001", "Gaseosa 500 ml", drinks.id()));
        service.create(details("GAS-02", "Agua mineral", drinks.id()));
        service.create(details("AR-003", "Arroz 5 kg", drinks.id()));

        assertThat(codes(search("  gAs ", null, 0, 20))).containsExactly("GAS-02", "AB-001");
    }

    @Test
    @DisplayName("HU-07: filtra por categoría y lo combina con el texto")
    void filtersByCategory() {
        Category groceries = categories.save(Category.create("Abarrotes"));
        service.create(details("AB-001", "Gaseosa 500 ml", drinks.id()));
        service.create(details("AR-003", "Arroz 5 kg", groceries.id()));
        service.create(details("AR-004", "Arroz 1 kg", groceries.id()));

        assertThat(codes(search(null, groceries.id(), 0, 20))).containsExactly("AR-004", "AR-003");
        assertThat(codes(search("5 kg", groceries.id(), 0, 20))).containsExactly("AR-003");
        assertThat(search("gaseosa", groceries.id(), 0, 20).content()).isEmpty();
    }

    @Test
    @DisplayName("HU-07: no muestra productos desactivados")
    void excludesInactive() {
        UUID id = service.create(details("AB-001", drinks.id())).product().id();
        service.create(details("AB-002", drinks.id()));
        service.update(new UpdateProductCommand(id, details("AB-001", drinks.id()), false));

        assertThat(codes(search(null, null, 0, 20))).containsExactly("AB-002");
    }

    @Test
    @DisplayName("HU-07: pagina con el total y devuelve categoría y stock de cada producto")
    void paginatesWithCategoryAndStock() {
        for (int i = 1; i <= 5; i++) {
            service.create(details("P-00" + i, "Producto " + i, drinks.id()));
        }
        UUID fourth = search("P-004", null, 0, 1).content().getFirst().product().id();
        stock.put(fourth, Quantity.of("9"));

        ResultPage<ProductView> second = search(null, null, 1, 3);

        assertThat(codes(second)).containsExactly("P-004", "P-005");
        assertThat(second.totalElements()).isEqualTo(5);
        assertThat(second.totalPages()).isEqualTo(2);
        assertThat(second.content()).allSatisfy(v -> assertThat(v.categoryName()).isEqualTo("Bebidas"));
        assertThat(second.content().getFirst().stock()).isEqualTo(Quantity.of("9"));
    }

    @Test
    @DisplayName("HU-07: pide el stock de toda la página en una sola consulta (sin N+1)")
    void loadsStockInOneQuery() {
        for (int i = 1; i <= 4; i++) {
            service.create(details("P-00" + i, "Producto " + i, drinks.id()));
        }
        stockQueries.clear();

        search(null, null, 0, 20);

        assertThat(stockQueries).hasSize(1);
        assertThat(stockQueries.getFirst()).hasSize(4);
    }

    @Test
    @DisplayName("HU-07: sin coincidencias devuelve una página vacía sin consultar el stock")
    void emptySearch() {
        service.create(details("AB-001", drinks.id()));
        stockQueries.clear();

        ResultPage<ProductView> page = search("no-existe", null, 0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(stockQueries).isEmpty();
    }

    @Test
    @DisplayName("falla si el producto no existe")
    void failsWhenNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ProductNotFoundException.class);
    }
}
