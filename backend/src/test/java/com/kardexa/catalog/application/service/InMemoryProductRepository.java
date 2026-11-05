package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.domain.model.Product;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble de prueba del puerto: guarda copias para que los cambios no se filtren sin {@code save}. */
class InMemoryProductRepository implements ProductRepository {

    private final Map<UUID, Product> rows = new LinkedHashMap<>();

    @Override
    public Product save(Product product) {
        rows.put(product.id(), copy(product));
        return product;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryProductRepository::copy);
    }

    @Override
    public boolean existsByCode(String code) {
        return rows.values().stream().anyMatch(p -> p.code().equalsIgnoreCase(code));
    }

    @Override
    public boolean existsByCodeExcluding(String code, UUID excludedId) {
        return rows.values().stream().anyMatch(p -> p.code().equalsIgnoreCase(code) && !p.id().equals(excludedId));
    }

    private static Product copy(Product p) {
        return Product.restore(p.id(), p.details(), p.referenceCost(), p.isActive());
    }
}
