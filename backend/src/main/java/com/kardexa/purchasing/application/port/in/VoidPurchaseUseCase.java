package com.kardexa.purchasing.application.port.in;

import com.kardexa.purchasing.domain.model.Purchase;
import java.util.UUID;

/**
 * HU-13 · Anular una compra registrada por error: el inventario registra una salida inversa por
 * producto y la compra queda anulada con su motivo, todo o nada (RF-17, RN-01, RN-03).
 */
public interface VoidPurchaseUseCase {

    Purchase voidPurchase(VoidPurchaseCommand command);

    /** {@code userId} es quien anula la compra. */
    record VoidPurchaseCommand(UUID purchaseId, String reason, UUID userId) {}
}
