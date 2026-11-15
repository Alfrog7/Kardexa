package com.kardexa.sites.domain.model;

import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Un turno de una sede (RN-14): lo abre quien lleva la caja y lo cierra contando lo que sobró de cada
 * producto (RN-15). Un turno cerrado no se edita ni se reabre (RN-03).
 *
 * @param responsibleName nombre de quien lo abrió; solo informativo, lo completa la lectura
 * @param registeredSales suma de las ventas registradas una a una durante el turno; nulo mientras está abierto
 * @param countedSales    lo vendido por conteo × precio; nulo mientras está abierto
 * @param expectedAmount  monto que debería haberse recibido: ventas registradas + vendido por conteo
 * @param payments        lo recibido por medio de pago al cerrar; nulo si no se anotó (HU-23)
 * @param difference      recibido − esperado, calculado en el servidor: negativo falta, positivo sobra
 */
public record Shift(
        UUID id,
        UUID siteId,
        UUID responsibleId,
        String responsibleName,
        Status status,
        Instant openedAt,
        Instant closedAt,
        UUID closedBy,
        BigDecimal registeredSales,
        BigDecimal countedSales,
        BigDecimal expectedAmount,
        String note,
        Payments payments,
        BigDecimal difference,
        List<Item> items) {

    public static final int NOTE_MAX_LENGTH = 255;

    public enum Status {
        OPEN,
        CLOSED
    }

    /**
     * Una línea del turno por producto.
     *
     * @param openingStock stock registrado al abrir (lo que sobró del turno anterior más lo recibido)
     * @param leftover     lo que sobró al contar; nulo mientras el turno está abierto
     * @param soldByCount  stock registrado al cerrar − sobrante contado
     * @param unitPrice    precio vigente al cerrar (RN-06)
     */
    public record Item(
            UUID productId,
            String productName,
            Quantity openingStock,
            Quantity leftover,
            Quantity soldByCount,
            BigDecimal unitPrice) {

        public Item {
            Objects.requireNonNull(productId, "productId");
            Objects.requireNonNull(productName, "productName");
            Objects.requireNonNull(openingStock, "openingStock");
        }

        public static Item opening(UUID productId, String productName, Quantity openingStock) {
            return new Item(productId, productName, openingStock, null, null, null);
        }

        /** Importe de lo vendido por conteo; cero si el turno sigue abierto. */
        public BigDecimal countedAmount() {
            return soldByCount == null || unitPrice == null
                    ? BigDecimal.ZERO
                    : soldByCount.value().multiply(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public Shift {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(responsibleId, "responsibleId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(openedAt, "openedAt");
        items = List.copyOf(items);
    }

    public static Shift open(UUID siteId, UUID responsibleId, List<Item> items, Instant now) {
        return new Shift(UUID.randomUUID(), siteId, responsibleId, null, Status.OPEN, now, null, null, null, null, null,
                null, null, null, items);
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** Solo el responsable o el administrador cierran el turno (RN-14). */
    public boolean canBeClosedBy(UUID userId, boolean admin) {
        return admin || responsibleId.equals(userId);
    }

    /** Total recibido por todos los medios; nulo si no se anotaron los pagos. */
    public BigDecimal receivedAmount() {
        return payments == null ? null : payments.total();
    }

    /** El turno cerrado: otra instancia, porque lo cerrado no se modifica. */
    public Shift closed(UUID userId, Instant now, BigDecimal registeredSales, String observation, Payments payments,
            List<Item> countedItems) {
        BigDecimal counted = countedItems.stream().map(Item::countedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String trimmed = observation == null || observation.isBlank() ? null : observation.strip();
        return new Shift(id, siteId, responsibleId, responsibleName, Status.CLOSED, openedAt, now, userId,
                registeredSales, counted, registeredSales.add(counted), trimmed, payments,
                payments == null ? null : payments.total().subtract(registeredSales.add(counted)), countedItems);
    }
}
