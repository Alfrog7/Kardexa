package com.kardexa.sales.application.port.in;

import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.time.LocalDate;
import java.util.UUID;

/** Consulta de ventas. El {@code EMPLEADO} solo ve las suyas (RF-21, ADR 0010). */
public interface ListSalesUseCase {

    /** Una venta ajena a un {@code EMPLEADO} responde como inexistente, para no revelar que existe. */
    SaleView get(UUID id, Viewer viewer);

    /**
     * HU-16 · Ventas de la más reciente a la más antigua. El {@code ADMIN} filtra por vendedor; al
     * {@code EMPLEADO} se le aplica siempre su propio id, aunque pida otro (RNF-03).
     */
    ResultPage<SaleView> list(SaleFilter filter, Viewer viewer, PageQuery page);

    /** Fechas inclusivas en hora de Lima; todo es opcional. */
    record SaleFilter(LocalDate from, LocalDate to, UUID sellerId) {}

    /** Quién consulta: su id y si es {@code ADMIN}. */
    record Viewer(UUID userId, boolean admin) {}
}
