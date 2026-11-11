package com.kardexa.purchasing.infrastructure.adapter.out.persistence;

import static com.kardexa.shared.infrastructure.persistence.ConstraintViolations.violates;

import com.kardexa.purchasing.application.port.out.SupplierRepository;
import com.kardexa.purchasing.domain.exception.DuplicateRucException;
import com.kardexa.purchasing.domain.model.Supplier;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link SupplierRepository} con Spring Data JPA sobre PostgreSQL. */
@Component
class SupplierPersistenceAdapter implements SupplierRepository {

    /** Restricción única de la migración V8 sobre {@code ruc}. */
    static final String UNIQUE_RUC_CONSTRAINT = "uk_supplier_ruc";

    private final SupplierJpaRepository jpa;

    SupplierPersistenceAdapter(SupplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Supplier save(Supplier supplier) {
        try {
            return jpa.saveAndFlush(SupplierJpaEntity.fromDomain(supplier)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            if (violates(ex, UNIQUE_RUC_CONSTRAINT)) {
                throw new DuplicateRucException(supplier.ruc());
            }
            throw ex;
        }
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return jpa.findById(id).map(SupplierJpaEntity::toDomain);
    }

    @Override
    public List<Supplier> search(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return jpa.search("%" + escaped + "%").stream().map(SupplierJpaEntity::toDomain).toList();
    }

    @Override
    public List<Supplier> findAllOrderedByBusinessName() {
        return jpa.findAllOrderedByBusinessName().stream().map(SupplierJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsByRuc(String ruc) {
        return jpa.existsByRuc(ruc);
    }

    @Override
    public boolean existsByRucExcluding(String ruc, UUID excludedId) {
        return jpa.existsByRucAndIdNot(ruc, excludedId);
    }
}
