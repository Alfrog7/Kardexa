package com.kardexa.sales.domain.model;

import com.kardexa.sales.domain.exception.InvalidSaleException;
import com.kardexa.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Venta de varios productos (HU-14). Descuenta el stock de cada uno y recibe un número correlativo
 * (RN-07). No se edita ni se borra: un error se corrige anulándola (HU-15).
 */
public final class Sale {

    private final UUID id;
    private final List<SaleItem> items;
    private final UUID sellerId;
    private final Instant soldAt;
    private final SaleStatus status;
    private SaleNumber number;

    private Sale(UUID id, SaleNumber number, List<SaleItem> items, SaleStatus status, UUID sellerId, Instant soldAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.number = number;
        this.items = List.copyOf(items);
        this.status = Objects.requireNonNull(status, "status");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId");
        this.soldAt = Objects.requireNonNull(soldAt, "soldAt");
        if (this.items.isEmpty()) {
            throw new InvalidSaleException("La venta debe tener al menos un producto.");
        }
        if (this.items.stream().map(SaleItem::productId).distinct().count() != this.items.size()) {
            throw new InvalidSaleException("Un producto no puede repetirse en la misma venta.");
        }
    }

    /** Una venta nueva de {@code sellerId}, todavía sin número: lo recibe al final de la transacción (ADR 0010). */
    public static Sale register(List<SaleItem> items, UUID sellerId, Instant soldAt) {
        return new Sale(UUID.randomUUID(), null, items, SaleStatus.REGISTERED, sellerId, soldAt);
    }

    /** Reconstruye una venta ya guardada (lo usa el adaptador de persistencia). */
    public static Sale restore(UUID id, SaleNumber number, List<SaleItem> items, SaleStatus status, UUID sellerId,
            Instant soldAt) {
        return new Sale(id, Objects.requireNonNull(number, "number"), items, status, sellerId, soldAt);
    }

    /** RN-07: el número se asigna una sola vez. */
    public void assignNumber(SaleNumber saleNumber) {
        if (number != null) {
            throw new IllegalStateException("La venta ya tiene número.");
        }
        this.number = Objects.requireNonNull(saleNumber, "saleNumber");
    }

    public Money total() {
        return items.stream().map(SaleItem::subtotal).reduce(Money.ZERO, Money::add);
    }

    public UUID id() {
        return id;
    }

    /** Nulo hasta que la venta se guarda. */
    public SaleNumber number() {
        return number;
    }

    public List<SaleItem> items() {
        return items;
    }

    public SaleStatus status() {
        return status;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public Instant soldAt() {
        return soldAt;
    }
}
