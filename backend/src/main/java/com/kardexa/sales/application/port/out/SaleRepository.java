package com.kardexa.sales.application.port.out;

import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.domain.model.Sale;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de ventas y sus ítems. Una venta se inserta una vez, ya con su número. */
public interface SaleRepository {

    void add(Sale sale);

    Optional<SaleView> findView(UUID id);
}
