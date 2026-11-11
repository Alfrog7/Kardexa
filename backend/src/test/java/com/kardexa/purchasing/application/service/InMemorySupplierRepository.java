package com.kardexa.purchasing.application.service;

import com.kardexa.purchasing.application.port.out.SupplierRepository;
import com.kardexa.purchasing.domain.model.Supplier;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble de prueba del puerto: permite probar los casos de uso sin base de datos. */
class InMemorySupplierRepository implements SupplierRepository {

    private final Map<UUID, Supplier> rows = new LinkedHashMap<>();

    private static Supplier copy(Supplier s) {
        return Supplier.restore(s.id(), s.details(), s.isActive());
    }

    @Override
    public Supplier save(Supplier supplier) {
        rows.put(supplier.id(), copy(supplier));
        return supplier;
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(InMemorySupplierRepository::copy);
    }

    @Override
    public List<Supplier> search(String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        return findAllOrderedByBusinessName().stream()
                .filter(s -> s.ruc().contains(needle) || s.businessName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    @Override
    public List<Supplier> findAllOrderedByBusinessName() {
        return rows.values().stream()
                .sorted(Comparator.comparing(s -> s.businessName().toLowerCase(Locale.ROOT)))
                .map(InMemorySupplierRepository::copy)
                .toList();
    }

    @Override
    public boolean existsByRuc(String ruc) {
        return rows.values().stream().anyMatch(s -> s.ruc().equals(ruc));
    }

    @Override
    public boolean existsByRucExcluding(String ruc, UUID excludedId) {
        return rows.values().stream().anyMatch(s -> s.ruc().equals(ruc) && !s.id().equals(excludedId));
    }
}
