package com.kardexa.catalog.application.port.in;

import com.kardexa.shared.domain.Money;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * RN-06 · Código, nombre y precio de venta vigente de varios productos, en una consulta, para copiarlos
 * en una venta (ADR 0010). Los ids que no existen no aparecen.
 */
public interface FindProductsForSaleUseCase {

    List<ProductForSale> findForSale(Collection<UUID> ids);

    record ProductForSale(UUID id, String code, String name, Money salePrice, boolean active) {}
}
