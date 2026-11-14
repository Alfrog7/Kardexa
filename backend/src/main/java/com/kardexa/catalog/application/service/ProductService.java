package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.in.DeleteProductUseCase;
import com.kardexa.catalog.application.port.in.ManageProductImageUseCase;
import com.kardexa.catalog.application.port.out.ProductImageRepository;
import com.kardexa.catalog.domain.exception.ProductInUseException;
import com.kardexa.catalog.domain.model.ProductImage;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import com.kardexa.catalog.application.port.in.CreateProductUseCase;
import com.kardexa.catalog.application.port.in.GetProductSummaryUseCase;
import com.kardexa.catalog.application.port.in.GetProductUseCase;
import com.kardexa.catalog.application.port.in.ProductView;
import com.kardexa.catalog.application.port.in.SearchProductsUseCase;
import com.kardexa.catalog.application.port.in.UpdateProductUseCase;
import com.kardexa.catalog.application.port.out.CategoryRepository;
import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.application.port.out.StockLevels;
import com.kardexa.catalog.domain.exception.CategoryNotFoundException;
import com.kardexa.catalog.domain.exception.DuplicateProductCodeException;
import com.kardexa.catalog.domain.exception.InactiveCategoryException;
import com.kardexa.catalog.domain.exception.ProductNotFoundException;
import com.kardexa.catalog.domain.model.Category;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Quantity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Casos de uso de HU-06, HU-07 y HU-10. Java puro: se registra como bean en {@code CatalogConfig}. */
public class ProductService implements CreateProductUseCase, UpdateProductUseCase, GetProductUseCase,
        SearchProductsUseCase, GetProductSummaryUseCase, DeleteProductUseCase, ManageProductImageUseCase {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final StockLevels stockLevels;
    private final ProductImageRepository images;
    private final Clock clock;

    public ProductService(ProductRepository products, CategoryRepository categories, StockLevels stockLevels,
            ProductImageRepository images, Clock clock) {
        this.products = Objects.requireNonNull(products);
        this.categories = Objects.requireNonNull(categories);
        this.stockLevels = Objects.requireNonNull(stockLevels);
        this.images = Objects.requireNonNull(images);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void delete(UUID id) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (!products.deleteIfUnused(id)) {
            throw new ProductInUseException(product.name());
        }
    }

    @Override
    public ProductView upload(UUID productId, byte[] data) {
        Product product = products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        images.save(productId, ProductImage.of(data, Instant.now(clock)));
        return view(product, existingCategory(product.categoryId()));
    }

    @Override
    public ProductView remove(UUID productId) {
        Product product = products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        images.delete(productId);
        return view(product, existingCategory(product.categoryId()));
    }

    @Override
    public Optional<ProductImage> find(UUID productId) {
        return images.find(productId);
    }

    /** Formato del código que se asigna cuando el administrador lo deja vacío. */
    static final String GENERATED_CODE_FORMAT = "P-%04d";

    @Override
    public ProductView create(Product.Details details) {
        if (details.code() == null || details.code().isBlank()) {
            details = withCode(details, nextGeneratedCode());
        }
        Product product = Product.create(details);
        Category category = activeCategory(product.categoryId());
        if (products.existsByCode(product.code())) {
            throw new DuplicateProductCodeException(product.code());
        }
        return view(products.save(product), category);
    }

    @Override
    public ProductView update(UpdateProductCommand command) {
        Product product = products.findById(command.id())
                .orElseThrow(() -> new ProductNotFoundException(command.id()));
        UUID previousCategory = product.categoryId();

        product.changeDetails(command.details());
        // Si la categoría actual se desactivó, el producto puede seguir editándose; solo se
        // impide moverlo a una categoría desactivada.
        Category category = product.categoryId().equals(previousCategory)
                ? existingCategory(product.categoryId())
                : activeCategory(product.categoryId());
        if (products.existsByCodeExcluding(product.code(), product.id())) {
            throw new DuplicateProductCodeException(product.code());
        }
        if (command.active()) {
            product.activate();
        } else {
            product.deactivate();
        }
        return view(products.save(product), category);
    }

    @Override
    public ProductView get(UUID id) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return view(product, existingCategory(product.categoryId()));
    }

    @Override
    public ProductSummary getSummary(UUID id) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return new ProductSummary(product.id(), product.name(), product.isActive());
    }

    @Override
    public ResultPage<ProductView> search(ProductSearch search) {
        ResultPage<Product> page = products.search(
                search.text(), search.categoryId(), search.criticalOnly(), search.includeInactive(), search.page());
        if (page.content().isEmpty()) {
            return new ResultPage<>(List.of(), page.page(), page.size(), page.totalElements());
        }
        // Nombres de categoría y stock de toda la página con una consulta cada uno, no una por producto.
        Map<UUID, String> categoryNames = categories.findAllById(
                        page.content().stream().map(Product::categoryId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Category::id, Category::name));
        List<UUID> ids = page.content().stream().map(Product::id).toList();
        Map<UUID, Quantity> stock = stockLevels.currentStock(ids);
        Map<UUID, Instant> imageVersions = images.versions(ids);
        return page.map(product -> new ProductView(product, categoryNames.get(product.categoryId()),
                stock.getOrDefault(product.id(), Quantity.ZERO), imageVersions.get(product.id())));
    }

    /** El siguiente número libre: se salta los códigos {@code P-NNNN} que alguien escribió a mano. */
    private String nextGeneratedCode() {
        long next = products.maxGeneratedCodeNumber() + 1;
        String code = GENERATED_CODE_FORMAT.formatted(next);
        while (products.existsByCode(code)) {
            code = GENERATED_CODE_FORMAT.formatted(++next);
        }
        return code;
    }

    private static Product.Details withCode(Product.Details details, String code) {
        return new Product.Details(code, details.name(), details.categoryId(), details.unit(), details.salePrice(),
                details.minStock());
    }

    private Category existingCategory(UUID id) {
        return categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private Category activeCategory(UUID id) {
        Category category = existingCategory(id);
        if (!category.isActive()) {
            throw new InactiveCategoryException(category.name());
        }
        return category;
    }

    private ProductView view(Product product, Category category) {
        return new ProductView(product, category.name(), stockLevels.currentStock(product.id()),
                images.versions(List.of(product.id())).get(product.id()));
    }
}
