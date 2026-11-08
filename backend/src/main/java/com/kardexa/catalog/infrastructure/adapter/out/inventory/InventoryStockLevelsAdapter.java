package com.kardexa.catalog.infrastructure.adapter.out.inventory;

import com.kardexa.catalog.application.port.out.StockLevels;
import com.kardexa.inventory.application.port.in.GetStockUseCase;
import com.kardexa.shared.domain.Quantity;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Traduce el puerto {@link StockLevels} del catálogo al caso de uso del módulo de inventario.
 * Es el único punto en que el catálogo conoce a {@code inventory}, y lo hace por su puerto de entrada.
 */
@Component
class InventoryStockLevelsAdapter implements StockLevels {

    private final GetStockUseCase getStock;

    InventoryStockLevelsAdapter(GetStockUseCase getStock) {
        this.getStock = getStock;
    }

    @Override
    public Map<UUID, Quantity> currentStock(Collection<UUID> productIds) {
        return getStock.currentStock(productIds);
    }

    @Override
    public Quantity currentStock(UUID productId) {
        return getStock.currentStock(productId);
    }
}
