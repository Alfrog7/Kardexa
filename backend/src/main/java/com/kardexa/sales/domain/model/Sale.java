package com.kardexa.sales.domain.model;

import com.kardexa.sales.domain.exception.InvalidSaleException;
import com.kardexa.sales.domain.exception.SaleAlreadyVoidedException;
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

    public static final int VOID_REASON_MIN_LENGTH = 3;
    public static final int VOID_REASON_MAX_LENGTH = 255;

    private final UUID id;
    private final List<SaleItem> items;
    private final UUID sellerId;
    private final Instant soldAt;
    private SaleStatus status;
    private SaleNumber number;
    private Voiding voiding;

    /** Quién anuló la venta, cuándo y por qué (HU-15). */
    public record Voiding(String reason, UUID userId, Instant at) {

        public Voiding {
            reason = normalizeReason(reason);
            Objects.requireNonNull(at, "at");
        }
    }

    private Sale(UUID id, SaleNumber number, List<SaleItem> items, SaleStatus status, UUID sellerId, Instant soldAt,
            Voiding voiding) {
        this.id = Objects.requireNonNull(id, "id");
        this.number = number;
        this.items = List.copyOf(items);
        this.status = Objects.requireNonNull(status, "status");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId");
        this.soldAt = Objects.requireNonNull(soldAt, "soldAt");
        this.voiding = voiding;
        if (this.items.isEmpty()) {
            throw new InvalidSaleException("La venta debe tener al menos un producto.");
        }
        if (this.items.stream().map(SaleItem::productId).distinct().count() != this.items.size()) {
            throw new InvalidSaleException("Un producto no puede repetirse en la misma venta.");
        }
        if ((status == SaleStatus.VOIDED) != (voiding != null)) {
            throw new IllegalArgumentException("Solo una venta anulada tiene datos de anulación.");
        }
    }

    /** Una venta nueva de {@code sellerId}, todavía sin número: lo recibe al final de la transacción (ADR 0010). */
    public static Sale register(List<SaleItem> items, UUID sellerId, Instant soldAt) {
        return new Sale(UUID.randomUUID(), null, items, SaleStatus.REGISTERED, sellerId, soldAt, null);
    }

    /** Reconstruye una venta ya guardada (lo usa el adaptador de persistencia). */
    public static Sale restore(UUID id, SaleNumber number, List<SaleItem> items, SaleStatus status, UUID sellerId,
            Instant soldAt, Voiding voiding) {
        return new Sale(id, Objects.requireNonNull(number, "number"), items, status, sellerId, soldAt, voiding);
    }

    /**
     * HU-15: deja la venta anulada con su motivo, sin perder su número (RN-07). El stock lo devuelve el
     * inventario con entradas; los movimientos originales no se tocan (RN-03).
     */
    public void voidWith(Voiding voidingData) {
        if (status == SaleStatus.VOIDED) {
            throw new SaleAlreadyVoidedException(number);
        }
        this.voiding = Objects.requireNonNull(voidingData, "voiding");
        this.status = SaleStatus.VOIDED;
    }

    private static String normalizeReason(String reason) {
        String normalized = reason == null ? "" : reason.strip().replaceAll("\\s+", " ");
        if (normalized.length() < VOID_REASON_MIN_LENGTH || normalized.length() > VOID_REASON_MAX_LENGTH) {
            throw new InvalidSaleException("El motivo de la anulación debe tener entre %d y %d caracteres."
                    .formatted(VOID_REASON_MIN_LENGTH, VOID_REASON_MAX_LENGTH));
        }
        return normalized;
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

    /** Nulo mientras la venta siga registrada. */
    public Voiding voiding() {
        return voiding;
    }
}
