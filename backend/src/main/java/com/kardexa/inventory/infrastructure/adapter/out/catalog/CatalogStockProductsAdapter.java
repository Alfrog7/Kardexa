package com.kardexa.inventory.infrastructure.adapter.out.catalog;

import com.kardexa.catalog.application.port.in.GetProductSummaryUseCase;
import com.kardexa.catalog.application.port.in.GetProductSummaryUseCase.ProductSummary;
import com.kardexa.inventory.application.port.out.StockProducts;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Traduce el puerto {@link StockProducts} del inventario al caso de uso del catálogo. Es el único
 * punto en que el inventario conoce a {@code catalog}, y lo hace por su puerto de entrada.
 */
@Component
class CatalogStockProductsAdapter implements StockProducts {

    private final GetProductSummaryUseCase getProductSummary;

    CatalogStockProductsAdapter(GetProductSummaryUseCase getProductSummary) {
        this.getProductSummary = getProductSummary;
    }

    @Override
    public StockProduct require(UUID productId) {
        ProductSummary product = getProductSummary.getSummary(productId);
        return new StockProduct(product.id(), product.name(), product.active());
    }
}
