package com.kardexa.sales.infrastructure.adapter.out.catalog;

import com.kardexa.catalog.application.port.in.FindProductsForSaleUseCase;
import com.kardexa.sales.application.port.out.SaleProducts;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Traduce el puerto {@link SaleProducts} al caso de uso del catálogo, por su puerto de entrada. */
@Component
class CatalogSaleProductsAdapter implements SaleProducts {

    private final FindProductsForSaleUseCase findProductsForSale;

    CatalogSaleProductsAdapter(FindProductsForSaleUseCase findProductsForSale) {
        this.findProductsForSale = findProductsForSale;
    }

    @Override
    public List<ProductForSale> findByIds(Collection<UUID> ids) {
        return findProductsForSale.findForSale(ids).stream()
                .map(p -> new ProductForSale(p.id(), p.code(), p.name(), p.salePrice(), p.active()))
                .toList();
    }
}
