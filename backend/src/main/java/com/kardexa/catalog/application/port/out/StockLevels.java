package com.kardexa.catalog.application.port.out;

import com.kardexa.shared.domain.Quantity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que el catálogo necesita saber del inventario: el stock actual de sus productos. Lo implementa
 * un adaptador que llama al puerto de entrada de {@code inventory}, así el catálogo no depende de él.
 */
@FunctionalInterface
public interface StockLevels {

    /**
     * Stock de varios productos en una sola consulta (evita N+1 al listar). El mapa trae una entrada
     * por cada id pedido; un producto sin movimientos tiene stock 0.
     */
    Map<UUID, Quantity> currentStock(Collection<UUID> productIds);

    default Quantity currentStock(UUID productId) {
        return currentStock(List.of(productId)).getOrDefault(productId, Quantity.ZERO);
    }
}
