package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.in.CreateProductUseCase;
import com.kardexa.catalog.application.port.in.GetProductUseCase;
import com.kardexa.catalog.application.port.in.ProductView;
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
import java.util.Objects;
import java.util.UUID;

/** Casos de uso de HU-06. Java puro: se registra como bean en {@code CatalogConfig}. */
public class ProductService implements CreateProductUseCase, UpdateProductUseCase, GetProductUseCase {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final StockLevels stockLevels;

    public ProductService(ProductRepository products, CategoryRepository categories, StockLevels stockLevels) {
        this.products = Objects.requireNonNull(products);
        this.categories = Objects.requireNonNull(categories);
        this.stockLevels = Objects.requireNonNull(stockLevels);
    }

    @Override
    public ProductView create(Product.Details details) {
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
        return new ProductView(product, category.name(), stockLevels.currentStock(product.id()));
    }
}
