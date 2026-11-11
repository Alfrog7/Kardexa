package com.kardexa.purchasing.application.service;

import com.kardexa.purchasing.application.port.in.PurchaseView;
import com.kardexa.purchasing.application.port.out.PurchaseRepository;
import com.kardexa.purchasing.domain.exception.PurchaseAlreadyVoidedException;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.purchasing.domain.model.PurchaseStatus;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble de prueba del puerto: guarda copias, como la base, para que los tests vean lo persistido. */
class InMemoryPurchaseRepository implements PurchaseRepository {

    final Map<UUID, Purchase> rows = new LinkedHashMap<>();

    private static Purchase copy(Purchase p) {
        return Purchase.restore(p.id(), p.supplierId(), p.supplierDocument(), p.purchaseDate(), p.items(),
                p.status(), p.userId(), p.createdAt(), p.voiding());
    }

    @Override
    public void add(Purchase purchase) {
        rows.put(purchase.id(), copy(purchase));
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryPurchaseRepository::copy);
    }

    @Override
    public void saveVoiding(Purchase purchase) {
        Purchase current = rows.get(purchase.id());
        if (current == null || current.status() != PurchaseStatus.REGISTERED) {
            throw new PurchaseAlreadyVoidedException(purchase.id());
        }
        rows.put(purchase.id(), copy(purchase));
    }

    @Override
    public Optional<PurchaseView> findView(UUID id) {
        return Optional.empty();
    }

    @Override
    public ResultPage<PurchaseView> findViews(LocalDate from, LocalDate to, PageQuery page) {
        return ResultPage.of(List.of(), page, 0);
    }
}
