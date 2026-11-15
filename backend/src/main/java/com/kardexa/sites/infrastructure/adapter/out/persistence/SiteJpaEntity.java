package com.kardexa.sites.infrastructure.adapter.out.persistence;

import com.kardexa.sites.domain.model.Site;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Fila de la tabla {@code site} (migración V14). Solo existe en esta capa. */
@Entity
@Table(name = "site")
class SiteJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = Site.NAME_MAX_LENGTH)
    private String name;

    @Column(length = Site.ADDRESS_MAX_LENGTH)
    private String address;

    @Column(nullable = false)
    private boolean active;

    protected SiteJpaEntity() {
        // Requerido por JPA.
    }

    static SiteJpaEntity fromDomain(Site site) {
        SiteJpaEntity entity = new SiteJpaEntity();
        entity.id = site.id();
        entity.name = site.name();
        entity.address = site.details().address();
        entity.active = site.isActive();
        return entity;
    }

    Site toDomain() {
        return Site.restore(id, new Site.Details(name, address), active);
    }
}
