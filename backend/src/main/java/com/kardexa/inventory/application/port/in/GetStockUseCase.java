package com.kardexa.inventory.application.port.in;

import com.kardexa.shared.domain.Quantity;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Stock actual de los productos. Un producto sin movimientos tiene stock 0. */
public interface GetStockUseCase {

    /** {@code siteId} nulo: la suma de todas las sedes. */
    Quantity currentStock(UUID productId, UUID siteId);

    /** Stock de varios productos con una sola consulta: una entrada por cada id pedido. */
    Map<UUID, Quantity> currentStock(Collection<UUID> productIds, UUID siteId);
}
