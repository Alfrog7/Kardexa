package com.kardexa.catalog.application.service;

import com.kardexa.catalog.application.port.in.FindProductsForSaleUseCase;
import com.kardexa.catalog.application.port.out.ProductRepository;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** RN-06. Java puro: se registra en {@code CatalogConfig}. */
public class ProductsForSaleService implements FindProductsForSaleUseCase {

    private final ProductRepository products;

    public ProductsForSaleService(ProductRepository products) {
        this.products = Objects.requireNonNull(products);
    }

    @Override
    public List<ProductForSale> findForSale(Collection<UUID> ids) {
        return products.findAllById(ids).stream()
                .map(p -> new ProductForSale(p.id(), p.code(), p.name(), p.salePrice(), p.isActive()))
                .toList();
    }
}
