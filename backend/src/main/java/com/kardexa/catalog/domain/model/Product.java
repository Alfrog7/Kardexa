package com.kardexa.catalog.domain.model;

import com.kardexa.catalog.domain.exception.InvalidProductException;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.util.Objects;
import java.util.UUID;

/**
 * Producto que se compra y se vende (HU-06).
 *
 * <p>El stock no vive aquí: lo lleva el módulo {@code inventory} a partir del kárdex. El producto
 * solo define el <em>stock mínimo</em> con el que se decide si está en stock crítico (RN-09).
 */
public final class Product {

    public static final int CODE_MAX_LENGTH = 40;
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 120;

    private final UUID id;
    private String code;
    private String name;
    private UUID categoryId;
    private Unit unit;
    private Money salePrice;
    private final Money referenceCost;
    private Quantity minStock;
    private boolean active;

    private Product(UUID id, String code, String name, UUID categoryId, Unit unit, Money salePrice,
            Money referenceCost, Quantity minStock, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.referenceCost = Objects.requireNonNull(referenceCost, "referenceCost");
        this.active = active;
        applyDetails(new Details(code, name, categoryId, unit, salePrice, minStock));
    }

    /** Datos editables del producto; el costo referencial lo actualizan las compras (RN-08). */
    public record Details(String code, String name, UUID categoryId, Unit unit, Money salePrice, Quantity minStock) {}

    /** Crea un producto activo, con costo referencial 0 hasta su primera compra. */
    public static Product create(Details details) {
        return new Product(UUID.randomUUID(), details.code(), details.name(), details.categoryId(), details.unit(),
                details.salePrice(), Money.ZERO, details.minStock(), true);
    }

    /** Reconstruye un producto ya guardado (lo usa el adaptador de persistencia). */
    public static Product restore(UUID id, Details details, Money referenceCost, boolean active) {
        return new Product(id, details.code(), details.name(), details.categoryId(), details.unit(),
                details.salePrice(), referenceCost, details.minStock(), active);
    }

    public void changeDetails(Details details) {
        applyDetails(details);
    }

    /** RF-09: un producto no se elimina, se desactiva. */
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    /** RN-09: el producto está en stock crítico cuando su stock es menor o igual que el mínimo. */
    public boolean isCritical(Quantity currentStock) {
        return !minStock.isLessThan(currentStock);
    }

    /** RN-10: el código se compara sin espacios en los extremos y sin distinguir mayúsculas. */
    public static String normalizeCode(String code) {
        String normalized = code == null ? "" : code.strip();
        if (normalized.isEmpty() || normalized.length() > CODE_MAX_LENGTH) {
            throw new InvalidProductException("El código debe tener entre 1 y %d caracteres.".formatted(CODE_MAX_LENGTH));
        }
        return normalized;
    }

    private void applyDetails(Details details) {
        String normalizedName = details.name() == null ? "" : details.name().strip().replaceAll("\\s+", " ");
        if (normalizedName.length() < NAME_MIN_LENGTH || normalizedName.length() > NAME_MAX_LENGTH) {
            throw new InvalidProductException("El nombre debe tener entre %d y %d caracteres."
                    .formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        }
        if (details.salePrice() == null || details.salePrice().compareTo(Money.ZERO) <= 0) {
            throw new InvalidProductException("El precio de venta debe ser mayor que 0.");
        }
        this.code = normalizeCode(details.code());
        this.name = normalizedName;
        this.categoryId = Objects.requireNonNull(details.categoryId(), "categoryId");
        this.unit = Objects.requireNonNull(details.unit(), "unit");
        this.salePrice = details.salePrice();
        this.minStock = Objects.requireNonNull(details.minStock(), "minStock");
    }

    public Details details() {
        return new Details(code, name, categoryId, unit, salePrice, minStock);
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public Unit unit() {
        return unit;
    }

    public Money salePrice() {
        return salePrice;
    }

    public Money referenceCost() {
        return referenceCost;
    }

    public Quantity minStock() {
        return minStock;
    }

    public boolean isActive() {
        return active;
    }
}
