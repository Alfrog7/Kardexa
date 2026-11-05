package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.out.CategoryRepository;
import com.kardexa.catalog.domain.model.Category;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble de prueba del puerto: permite probar los casos de uso sin base de datos. */
class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<UUID, Category> rows = new LinkedHashMap<>();

    @Override
    public Category save(Category category) {
        rows.put(category.id(), Category.restore(category.id(), category.name(), category.isActive()));
        return category;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(rows.get(id))
                .map(c -> Category.restore(c.id(), c.name(), c.isActive()));
    }

    @Override
    public List<Category> findAllOrderedByName() {
        return rows.values().stream()
                .sorted(Comparator.comparing(c -> c.name().toLowerCase()))
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return rows.values().stream().anyMatch(c -> c.name().equalsIgnoreCase(name));
    }

    @Override
    public boolean existsByNameExcluding(String name, UUID excludedId) {
        return rows.values().stream()
                .anyMatch(c -> c.name().equalsIgnoreCase(name) && !c.id().equals(excludedId));
    }
}
