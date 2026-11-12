package com.kardexa.sales.application.port.out;

import com.kardexa.shared.domain.Money;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Lo que ventas necesita del catálogo: el código, el nombre y el precio vigente de los productos que se
 * venden (RN-06). Lo implementa un adaptador que llama al puerto de entrada de {@code catalog}.
 */
public interface SaleProducts {

    /** Solo los que existen, activos o no. */
    List<ProductForSale> findByIds(Collection<UUID> ids);

    record ProductForSale(UUID id, String code, String name, Money salePrice, boolean active) {}
}
