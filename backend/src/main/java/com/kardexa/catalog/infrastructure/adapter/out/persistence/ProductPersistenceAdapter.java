package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public ResultPage<Product> searchActive(String text, UUID categoryId, PageQuery page) {
        String pattern = text == null ? null : "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        Page<ProductJpaEntity> result =
                jpa.searchActive(pattern, categoryId, PageRequest.of(page.page(), page.size()));
        return ResultPage.of(
                result.getContent().stream().map(ProductJpaEntity::toDomain).toList(), page, result.getTotalElements());
    }

    /** Que {@code %} y {@code _} escritos por el usuario se busquen tal cual y no como comodines. */
    static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
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
