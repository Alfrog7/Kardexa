package com.kardexa.purchasing.domain.model;

import com.kardexa.purchasing.domain.exception.InvalidPurchaseException;
import com.kardexa.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Mercadería comprada a un proveedor (HU-12). Al registrarse suma el stock de cada producto y fija su
 * costo referencial (RN-08). No se edita ni se borra: un error se corrige anulándola (HU-13).
 */
public final class Purchase {

    public static final int SUPPLIER_DOCUMENT_MAX_LENGTH = 40;

    private final UUID id;
    private final UUID supplierId;
    private final String supplierDocument;
    private final LocalDate purchaseDate;
    private final List<PurchaseItem> items;
    private final PurchaseStatus status;
    private final UUID userId;
    private final Instant createdAt;

    private Purchase(UUID id, UUID supplierId, String supplierDocument, LocalDate purchaseDate,
            List<PurchaseItem> items, PurchaseStatus status, UUID userId, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.supplierId = Objects.requireNonNull(supplierId, "supplierId");
        this.supplierDocument = normalizeDocument(supplierDocument);
        this.purchaseDate = Objects.requireNonNull(purchaseDate, "purchaseDate");
        this.items = List.copyOf(items);
        this.status = Objects.requireNonNull(status, "status");
        this.userId = userId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (this.items.isEmpty()) {
            throw new InvalidPurchaseException("La compra debe tener al menos un producto.");
        }
        if (this.items.stream().map(PurchaseItem::productId).distinct().count() != this.items.size()) {
            throw new InvalidPurchaseException("Un producto no puede repetirse en la misma compra.");
        }
    }

    /** Una compra nueva, registrada por {@code userId}. */
    public static Purchase register(UUID supplierId, String supplierDocument, LocalDate purchaseDate,
            List<PurchaseItem> items, UUID userId, Instant createdAt) {
        return new Purchase(UUID.randomUUID(), supplierId, supplierDocument, purchaseDate, items,
                PurchaseStatus.REGISTERED, userId, createdAt);
    }

    /** Reconstruye una compra ya guardada (lo usa el adaptador de persistencia). */
    public static Purchase restore(UUID id, UUID supplierId, String supplierDocument, LocalDate purchaseDate,
            List<PurchaseItem> items, PurchaseStatus status, UUID userId, Instant createdAt) {
        return new Purchase(id, supplierId, supplierDocument, purchaseDate, items, status, userId, createdAt);
    }

    /** El número de factura o guía es opcional; en blanco se guarda como ausente. */
    private static String normalizeDocument(String document) {
        if (document == null || document.isBlank()) {
            return null;
        }
        String normalized = document.strip();
        if (normalized.length() > SUPPLIER_DOCUMENT_MAX_LENGTH) {
            throw new InvalidPurchaseException(
                    "El documento del proveedor admite hasta %d caracteres.".formatted(SUPPLIER_DOCUMENT_MAX_LENGTH));
        }
        return normalized;
    }

    public Money total() {
        return items.stream().map(PurchaseItem::subtotal).reduce(Money.ZERO, Money::add);
    }

    public UUID id() {
        return id;
    }

    public UUID supplierId() {
        return supplierId;
    }

    public String supplierDocument() {
        return supplierDocument;
    }

    public LocalDate purchaseDate() {
        return purchaseDate;
    }

    public List<PurchaseItem> items() {
        return items;
    }

    public PurchaseStatus status() {
        return status;
    }

    public UUID userId() {
        return userId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
