package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.in.UpdateReferenceCostUseCase;
import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.domain.exception.ProductNotFoundException;
import com.kardexa.shared.domain.Money;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** RN-08. Java puro: se registra en {@code CatalogConfig} dentro de la transacción del coordinador. */
public class ReferenceCostService implements UpdateReferenceCostUseCase {

    private final ProductRepository products;

    public ReferenceCostService(ProductRepository products) {
        this.products = Objects.requireNonNull(products);
    }

    @Override
    public void updateReferenceCosts(Map<UUID, Money> costsByProduct) {
        costsByProduct.forEach((productId, cost) -> {
            if (cost.isNegative()) {
                throw new IllegalArgumentException("El costo referencial no puede ser negativo.");
            }
            if (!products.updateReferenceCost(productId, cost)) {
                throw new ProductNotFoundException(productId);
            }
        });
    }
}
