package com.kardexa.inventory.application.port.in;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU-21 · Registrar lo que llega a una sede (pan, bocadillos): suma stock sin costo ni proveedor y deja una
 * entrada {@code RECEIPT} por producto en el kárdex de la sede (ADR 0013).
 */
public interface RegisterReceiptUseCase {

    Receipt register(RegisterReceiptCommand command);

    /** Una línea por producto; la cantidad debe ser mayor que 0. */
    record RegisterReceiptCommand(UUID siteId, UUID userId, List<Line> lines) {

        public RegisterReceiptCommand {
            Objects.requireNonNull(siteId, "siteId");
            lines = List.copyOf(lines);
        }
    }

    /** La entrada registrada: su identificador agrupa los movimientos de sus líneas. */
    record Receipt(UUID id, UUID siteId, Instant createdAt, List<Line> lines) {}
}
