package com.kardexa.purchasing.infrastructure.adapter.out.catalog;

import com.kardexa.catalog.application.port.in.UpdateReferenceCostUseCase;
import com.kardexa.purchasing.application.port.out.PurchasedProducts;
import com.kardexa.shared.domain.Money;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Traduce el puerto {@link PurchasedProducts} al caso de uso del catálogo, por su puerto de entrada. */
@Component
class CatalogPurchasedProductsAdapter implements PurchasedProducts {

    private final UpdateReferenceCostUseCase updateReferenceCost;

    CatalogPurchasedProductsAdapter(UpdateReferenceCostUseCase updateReferenceCost) {
        this.updateReferenceCost = updateReferenceCost;
    }

    @Override
    public void updateReferenceCosts(Map<UUID, Money> costsByProduct) {
        updateReferenceCost.updateReferenceCosts(costsByProduct);
    }
}
