package com.kardexa.sales.application.port.in;

import java.util.UUID;

/** Consulta de ventas. El {@code EMPLEADO} solo ve las suyas (RF-21, ADR 0010). */
public interface ListSalesUseCase {

    /** Una venta ajena a un {@code EMPLEADO} responde como inexistente, para no revelar que existe. */
    SaleView get(UUID id, Viewer viewer);

    /** Quién consulta: su id y si es {@code ADMIN}. */
    record Viewer(UUID userId, boolean admin) {}
}
