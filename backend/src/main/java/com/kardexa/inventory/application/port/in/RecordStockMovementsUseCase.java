package com.kardexa.inventory.application.port.in;

import com.kardexa.shared.domain.Quantity;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registra los movimientos de kárdex de un documento de otro módulo: una compra, una venta o su
 * anulación (RF-10, RN-04). Valida que cada producto exista y esté activo y aplica RN-01.
 *
 * <p>ADR 0009: se ejecuta dentro de la transacción de quien coordina el documento y nunca reintenta
 * por su cuenta. Si otro cambió el stock lanza una {@code ConcurrentModificationException}, para que el
 * coordinador repita toda la operación.
 */
public interface RecordStockMovementsUseCase {

    void record(RecordStockMovementsCommand command);

    /** El tipo de documento decide la dirección: compras y anulaciones de venta suman, el resto resta. */
    enum DocumentMovement {
        PURCHASE,
        PURCHASE_VOID,
        SALE,
        SALE_VOID
    }

    /** Una línea por producto; un producto no puede repetirse en el mismo documento. */
    record Line(UUID productId, Quantity quantity) {

        public Line {
            Objects.requireNonNull(productId, "productId");
            Objects.requireNonNull(quantity, "quantity");
        }
    }

    /** {@code documentId} es el id de la compra o la venta; {@code userId}, quién la registra. */
    record RecordStockMovementsCommand(DocumentMovement movement, UUID documentId, UUID userId, List<Line> lines) {

        public RecordStockMovementsCommand {
            Objects.requireNonNull(movement, "movement");
            Objects.requireNonNull(documentId, "documentId");
            lines = List.copyOf(lines);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("El documento no tiene líneas.");
            }
            if (lines.stream().map(Line::productId).distinct().count() != lines.size()) {
                throw new IllegalArgumentException("Un producto se repite en el documento.");
            }
        }
    }
}
