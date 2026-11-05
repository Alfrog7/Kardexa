package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import static com.kardexa.shared.infrastructure.persistence.ConstraintViolations.violates;

import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.domain.exception.DuplicateProductCodeException;
import com.kardexa.catalog.domain.model.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link ProductRepository} con Spring Data JPA sobre PostgreSQL. */
@Component
class ProductPersistenceAdapter implements ProductRepository {

    /** Índice único de la migración V3 sobre {@code lower(code)} (RN-10). */
    static final String UNIQUE_CODE_INDEX = "uk_product_code";

    private final ProductJpaRepository jpa;

    ProductPersistenceAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Product save(Product product) {
        try {
            return jpa.saveAndFlush(ProductJpaEntity.fromDomain(product)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            if (violates(ex, UNIQUE_CODE_INDEX)) {
                throw new DuplicateProductCodeException(product.code());
            }
            throw ex;
        }
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpa.findById(id).map(ProductJpaEntity::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCodeIgnoreCase(code);
    }

    @Override
    public boolean existsByCodeExcluding(String code, UUID excludedId) {
        return jpa.existsByCodeIgnoreCaseAndIdNot(code, excludedId);
    }
}
