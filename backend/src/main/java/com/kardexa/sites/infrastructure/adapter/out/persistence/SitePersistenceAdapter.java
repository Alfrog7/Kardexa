package com.kardexa.sites.infrastructure.adapter.out.persistence;

import static com.kardexa.shared.infrastructure.persistence.ConstraintViolations.violates;

import com.kardexa.sites.application.port.out.SiteRepository;
import com.kardexa.sites.domain.exception.DuplicateSiteNameException;
import com.kardexa.sites.domain.model.Site;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link SiteRepository} con Spring Data JPA sobre PostgreSQL. */
@Component
class SitePersistenceAdapter implements SiteRepository {

    /** Índice único de la migración V14 sobre {@code lower(name)}. */
    static final String UNIQUE_NAME_CONSTRAINT = "uk_site_name";

    private final SiteJpaRepository jpa;

    SitePersistenceAdapter(SiteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Site save(Site site) {
        try {
            return jpa.saveAndFlush(SiteJpaEntity.fromDomain(site)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            if (violates(ex, UNIQUE_NAME_CONSTRAINT)) {
                throw new DuplicateSiteNameException(site.name());
            }
            throw ex;
        }
    }

    @Override
    public Optional<Site> findById(UUID id) {
        return jpa.findById(id).map(SiteJpaEntity::toDomain);
    }

    @Override
    public List<Site> findAllOrderedByName() {
        return jpa.findAllOrderedByName().stream().map(SiteJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameExcluding(String name, UUID excludedId) {
        return jpa.existsByNameIgnoreCaseAndIdNot(name, excludedId);
    }
}
