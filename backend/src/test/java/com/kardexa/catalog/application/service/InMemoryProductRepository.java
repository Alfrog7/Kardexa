package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    @Override
    public ResultPage<Product> searchActive(String text, UUID categoryId, PageQuery page) {
        String needle = text == null ? null : text.toLowerCase(Locale.ROOT);
        List<Product> matches = rows.values().stream()
                .filter(Product::isActive)
                .filter(p -> categoryId == null || p.categoryId().equals(categoryId))
                .filter(p -> needle == null
                        || p.code().toLowerCase(Locale.ROOT).contains(needle)
                        || p.name().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing((Product p) -> p.name().toLowerCase(Locale.ROOT)).thenComparing(Product::id))
                .map(InMemoryProductRepository::copy)
                .toList();
        List<Product> content = matches.stream().skip(page.offset()).limit(page.size()).toList();
        return ResultPage.of(content, page, matches.size());
    }

    private static Product copy(Product p) {
        return Product.restore(p.id(), p.details(), p.referenceCost(), p.isActive());
    }
}
