package com.kardexa.sales.application.port.in;

import com.kardexa.sales.domain.model.Sale;
import com.kardexa.shared.domain.Quantity;
import java.util.List;
import java.util.UUID;

/**
 * HU-14 · Registrar una venta: descuenta el stock de cada producto con una salida de kárdex y le da un
 * número correlativo (RF-18, RF-19, RN-07), todo o nada (RNF-04). El precio lo pone el catálogo (RN-06).
 */
public interface RegisterSaleUseCase {

    Sale register(RegisterSaleCommand command);

    record ItemCommand(UUID productId, Quantity quantity) {}

    /** {@code sellerId} es quien registra la venta. */
    record RegisterSaleCommand(List<ItemCommand> items, UUID sellerId) {}
}
