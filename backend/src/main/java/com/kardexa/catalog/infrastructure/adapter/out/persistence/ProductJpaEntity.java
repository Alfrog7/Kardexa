package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import com.kardexa.catalog.domain.model.Product;
import com.kardexa.catalog.domain.model.Unit;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Fila de la tabla {@code product} (migración V3). Solo existe en esta capa. */
@Entity
@Table(name = "product")
class ProductJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = Product.CODE_MAX_LENGTH)
    private String code;

    @Column(nullable = false, length = Product.NAME_MAX_LENGTH)
    private String name;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Unit unit;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    /** Solo lo cambia una compra con {@code updateReferenceCost} (RN-08); editar el producto no lo pisa. */
    @Column(name = "reference_cost", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal referenceCost;

    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStock;

    @Column(nullable = false)
    private boolean active;

    /** Lo pone la base al insertar ({@code DEFAULT now()}). */
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductJpaEntity() {
        // Requerido por JPA.
    }

    static ProductJpaEntity fromDomain(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.id = product.id();
        entity.code = product.code();
        entity.name = product.name();
        entity.categoryId = product.categoryId();
        entity.unit = product.unit();
        entity.salePrice = product.salePrice().amount();
        entity.referenceCost = product.referenceCost().amount();
        entity.minStock = product.minStock().value();
        entity.active = product.isActive();
        entity.updatedAt = Instant.now();
        return entity;
    }

    Product toDomain() {
        Product.Details details = new Product.Details(
                code, name, categoryId, unit, Money.of(salePrice), new Quantity(minStock));
        return Product.restore(id, details, Money.of(referenceCost), active);
    }
}
