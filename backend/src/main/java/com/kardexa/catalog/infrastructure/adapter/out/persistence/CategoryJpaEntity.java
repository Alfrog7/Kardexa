package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import com.kardexa.catalog.domain.model.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Fila de la tabla {@code category} (migración V2). Solo existe en esta capa. */
@Entity
@Table(name = "category")
class CategoryJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = Category.NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false)
    private boolean active;

    protected CategoryJpaEntity() {
        // Requerido por JPA.
    }

    static CategoryJpaEntity fromDomain(Category category) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.id = category.id();
        entity.name = category.name();
        entity.active = category.isActive();
        return entity;
    }

    Category toDomain() {
        return Category.restore(id, name, active);
    }
}
