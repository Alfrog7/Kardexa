package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Doble de prueba del puerto: guarda copias para que los cambios no se filtren sin {@code save}. */
class InMemoryProductRepository implements ProductRepository {

    private final Map<UUID, Product> rows = new LinkedHashMap<>();
    private final Function<UUID, Quantity> stock;

    /** @param stock stock actual de cada producto, como lo vería la consulta que cruza con el inventario */
    InMemoryProductRepository(Function<UUID, Quantity> stock) {
        this.stock = stock;
    }

    @Override
    public Product save(Product product) {
        rows.put(product.id(), copy(product));
        return product;
    }

    @Override
    public boolean updateReferenceCost(UUID productId, Money cost) {
        Product current = rows.get(productId);
        if (current == null) {
            return false;
        }
        rows.put(productId, Product.restore(productId, current.details(), cost, current.isActive()));
        return true;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryProductRepository::copy);
    }

    @Override
    public List<Product> findAllById(Collection<UUID> ids) {
        return ids.stream().map(rows::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return rows.values().stream().anyMatch(p -> p.code().equalsIgnoreCase(code));
    }

    @Override
    public long maxGeneratedCodeNumber() {
        return rows.values().stream()
                .map(Product::code)
                .filter(code -> code.matches("(?i)P-\d{1,15}"))
                .mapToLong(code -> Long.parseLong(code.substring(2)))
                .max()
                .orElse(0);
    }

    @Override
    public boolean existsByCodeExcluding(String code, UUID excludedId) {
        return rows.values().stream().anyMatch(p -> p.code().equalsIgnoreCase(code) && !p.id().equals(excludedId));
    }

    @Override
    public ResultPage<Product> search(String text, UUID categoryId, boolean criticalOnly, boolean includeInactive, PageQuery page) {
        String needle = text == null ? null : text.toLowerCase(Locale.ROOT);
        List<Product> matches = rows.values().stream()
                .filter(p -> includeInactive || p.isActive())
                .filter(p -> categoryId == null || p.categoryId().equals(categoryId))
                .filter(p -> !criticalOnly || p.isCritical(stock.apply(p.id())))
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
