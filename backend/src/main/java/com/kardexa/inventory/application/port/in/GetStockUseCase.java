package com.kardexa.inventory.application.port.in;

import com.kardexa.shared.domain.Quantity;
import java.util.UUID;

/** Stock actual de un producto. Un producto sin movimientos tiene stock 0. */
public interface GetStockUseCase {

    Quantity currentStock(UUID productId);
}
