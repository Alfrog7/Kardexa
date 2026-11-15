package com.kardexa.sites.application.port.out;

import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lo que el turno necesita saber del stock y del precio de los productos de una sede. */
public interface ShiftStock {

    /** Los productos activos con stock en la sede, para la foto de apertura. */
    List<StockLine> withStock(UUID siteId);

    /**
     * El stock vigente y el precio de los productos pedidos, con la fila de stock bloqueada hasta el final de la
     * transacción para que nadie venda entre la lectura y el cierre. Un producto sin stock figura con 0; uno que
     * no existe, no figura.
     */
    Map<UUID, StockLine> lock(UUID siteId, Collection<UUID> productIds);

    record StockLine(UUID productId, String productName, BigDecimal price, Quantity quantity) {}
}
