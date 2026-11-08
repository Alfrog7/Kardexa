package com.kardexa.inventory.application.port.in;

import com.kardexa.shared.domain.Quantity;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Stock actual de los productos. Un producto sin movimientos tiene stock 0. */
public interface GetStockUseCase {

    Quantity currentStock(UUID productId);

    /** Stock de varios productos con una sola consulta: una entrada por cada id pedido. */
    Map<UUID, Quantity> currentStock(Collection<UUID> productIds);
}
