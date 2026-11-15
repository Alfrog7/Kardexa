package com.kardexa.sites.application.service;

import com.kardexa.sites.application.port.out.SiteRepository;
import com.kardexa.sites.domain.model.Site;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble de prueba del puerto: permite probar los casos de uso sin base de datos. */
class InMemorySiteRepository implements SiteRepository {

    private final Map<UUID, Site> rows = new LinkedHashMap<>();

    private static Site copy(Site s) {
        return Site.restore(s.id(), s.details(), s.isActive());
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public Site save(Site site) {
        rows.put(site.id(), copy(site));
        return site;
    }

    @Override
    public void delete(UUID id) {
        rows.remove(id);
    }

    @Override
    public Optional<Site> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(InMemorySiteRepository::copy);
    }

    @Override
    public List<Site> findAllOrderedByName() {
        return rows.values().stream()
                .sorted(Comparator.comparing(s -> key(s.name())))
                .map(InMemorySiteRepository::copy)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return rows.values().stream().anyMatch(s -> key(s.name()).equals(key(name)));
    }

    @Override
    public boolean existsByNameExcluding(String name, UUID excludedId) {
        return rows.values().stream().anyMatch(s -> key(s.name()).equals(key(name)) && !s.id().equals(excludedId));
    }
}
