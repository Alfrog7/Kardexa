package com.kardexa.purchasing.application.port.out;

import com.kardexa.purchasing.application.port.in.PurchaseView;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de compras y sus ítems. Una compra se inserta una vez; solo cambia al anularse. */
public interface PurchaseRepository {

    void add(Purchase purchase);

    /** La compra con sus ítems, para anularla. */
    Optional<Purchase> findById(UUID id);

    /**
     * Guarda la anulación solo si la compra seguía registrada.
     *
     * @throws com.kardexa.purchasing.domain.exception.PurchaseAlreadyVoidedException si otro la anuló antes
     */
    void saveVoiding(Purchase purchase);

    Optional<PurchaseView> findView(UUID id);

    /** Ordenadas por fecha de compra y luego por registro, de la más reciente a la más antigua. */
    ResultPage<PurchaseView> findViews(LocalDate from, LocalDate to, PageQuery page);
}
