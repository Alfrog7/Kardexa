package com.kardexa.sales.application.port.out;

import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.domain.model.Sale;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de ventas y sus ítems. Una venta se inserta una vez, ya con su número. */
public interface SaleRepository {

    void add(Sale sale);

    Optional<SaleView> findView(UUID id);

    /** Vendidas en {@code [from, to)}; los filtros nulos no se aplican. De la más reciente a la más antigua. */
    ResultPage<SaleView> findViews(Instant from, Instant to, UUID sellerId, PageQuery page);
}
