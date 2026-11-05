package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import com.kardexa.catalog.application.port.out.CategoryRepository;
import com.kardexa.catalog.domain.exception.DuplicateCategoryNameException;
import com.kardexa.catalog.domain.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link CategoryRepository} con Spring Data JPA sobre PostgreSQL. */
@Component
class CategoryPersistenceAdapter implements CategoryRepository {

    /** Índice único de la migración V2 sobre {@code lower(name)}. */
    static final String UNIQUE_NAME_INDEX = "uk_category_name";

    private final CategoryJpaRepository jpa;

    CategoryPersistenceAdapter(CategoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Category save(Category category) {
        try {
            return jpa.saveAndFlush(CategoryJpaEntity.fromDomain(category)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            if (violates(ex, UNIQUE_NAME_INDEX)) {
                throw new DuplicateCategoryNameException(category.name());
            }
            throw ex;
        }
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpa.findById(id).map(CategoryJpaEntity::toDomain);
    }

    @Override
    public List<Category> findAllOrderedByName() {
        return jpa.findAllOrderedByName().stream().map(CategoryJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameExcluding(String name, UUID excludedId) {
        return jpa.existsByNameIgnoreCaseAndIdNot(name, excludedId);
    }

    private static boolean violates(Throwable ex, String constraint) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
