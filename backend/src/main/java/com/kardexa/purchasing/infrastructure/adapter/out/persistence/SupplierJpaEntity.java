package com.kardexa.purchasing.infrastructure.adapter.out.persistence;

import com.kardexa.purchasing.domain.model.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Fila de la tabla {@code supplier} (migración V8). Solo existe en esta capa. */
@Entity
@Table(name = "supplier")
class SupplierJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = Supplier.RUC_LENGTH)
    private String ruc;

    @Column(name = "business_name", nullable = false, length = Supplier.BUSINESS_NAME_MAX_LENGTH)
    private String businessName;

    @Column(length = Supplier.PHONE_MAX_LENGTH)
    private String phone;

    @Column(length = Supplier.EMAIL_MAX_LENGTH)
    private String email;

    @Column(nullable = false)
    private boolean active;

    protected SupplierJpaEntity() {
        // Requerido por JPA.
    }

    static SupplierJpaEntity fromDomain(Supplier supplier) {
        SupplierJpaEntity entity = new SupplierJpaEntity();
        entity.id = supplier.id();
        entity.ruc = supplier.ruc();
        entity.businessName = supplier.businessName();
        entity.phone = supplier.details().phone();
        entity.email = supplier.details().email();
        entity.active = supplier.isActive();
        return entity;
    }

    Supplier toDomain() {
        return Supplier.restore(id, new Supplier.Details(ruc, businessName, phone, email), active);
    }
}
