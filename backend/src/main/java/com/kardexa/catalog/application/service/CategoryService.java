package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.in.CreateCategoryUseCase;
import com.kardexa.catalog.application.port.in.ListCategoriesUseCase;
import com.kardexa.catalog.application.port.in.UpdateCategoryUseCase;
import com.kardexa.catalog.application.port.out.CategoryRepository;
import com.kardexa.catalog.domain.exception.CategoryNotFoundException;
import com.kardexa.catalog.domain.exception.DuplicateCategoryNameException;
import com.kardexa.catalog.domain.model.Category;
import java.util.List;
import java.util.Objects;

/** Casos de uso de HU-05. Java puro: se registra como bean en {@code CatalogConfig}. */
public class CategoryService implements CreateCategoryUseCase, UpdateCategoryUseCase, ListCategoriesUseCase {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = Objects.requireNonNull(categories);
    }

    @Override
    public Category create(CreateCategoryCommand command) {
        Category category = Category.create(command.name());
        if (categories.existsByName(category.name())) {
            throw new DuplicateCategoryNameException(category.name());
        }
        return categories.save(category);
    }

    @Override
    public Category update(UpdateCategoryCommand command) {
        Category category = categories.findById(command.id())
                .orElseThrow(() -> new CategoryNotFoundException(command.id()));

        category.rename(command.name());
        if (categories.existsByNameExcluding(category.name(), category.id())) {
            throw new DuplicateCategoryNameException(category.name());
        }
        if (command.active()) {
            category.activate();
        } else {
            category.deactivate();
        }
        return categories.save(category);
    }

    @Override
    public List<Category> listAll() {
        return categories.findAllOrderedByName();
    }
}
