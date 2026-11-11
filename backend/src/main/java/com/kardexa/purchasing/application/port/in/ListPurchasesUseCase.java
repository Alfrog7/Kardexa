package com.kardexa.purchasing.application.port.in;

import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.time.LocalDate;
import java.util.UUID;

/** HU-12 · Consultar las compras, de la más reciente a la más antigua. */
public interface ListPurchasesUseCase {

    /** Fechas de compra inclusivas; las dos son opcionales. */
    ResultPage<PurchaseView> list(LocalDate from, LocalDate to, PageQuery page);

    /** Lanza {@code PurchaseNotFoundException} (404) si no existe. */
    PurchaseView get(UUID id);
}
