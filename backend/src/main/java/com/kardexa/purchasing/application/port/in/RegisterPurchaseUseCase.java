package com.kardexa.purchasing.application.port.in;

import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * HU-12 · Registrar una compra: suma el stock de cada producto con un movimiento de entrada y fija su
 * costo referencial (RF-10, RF-16, RN-08), todo o nada (RNF-04).
 */
public interface RegisterPurchaseUseCase {

    Purchase register(RegisterPurchaseCommand command);

    record ItemCommand(UUID productId, Quantity quantity, Money unitCost) {}

    /** {@code userId} es quien registra la compra (RN-04). */
    record RegisterPurchaseCommand(
            UUID supplierId, String supplierDocument, LocalDate purchaseDate, List<ItemCommand> items, UUID userId) {}
}
