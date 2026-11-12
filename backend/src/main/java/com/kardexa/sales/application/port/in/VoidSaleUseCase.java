package com.kardexa.sales.application.port.in;

import com.kardexa.sales.domain.model.Sale;
import java.util.UUID;

/**
 * HU-15 · Anular una venta: el inventario registra una entrada por producto que devuelve el stock y la
 * venta queda anulada con su motivo y su número, todo o nada (RF-20, RN-03, RN-07). Solo {@code ADMIN}.
 */
public interface VoidSaleUseCase {

    Sale voidSale(VoidSaleCommand command);

    record VoidSaleCommand(UUID saleId, String reason, UUID userId) {}
}
