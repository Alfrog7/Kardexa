package com.kardexa.purchasing.application.service;

import com.kardexa.purchasing.application.port.in.ListPurchasesUseCase;
import com.kardexa.purchasing.application.port.in.PurchaseView;
import com.kardexa.purchasing.application.port.out.PurchaseRepository;
import com.kardexa.purchasing.domain.exception.PurchaseNotFoundException;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Consultas de compras (HU-12). */
public class PurchaseQueryService implements ListPurchasesUseCase {

    private final PurchaseRepository purchases;

    public PurchaseQueryService(PurchaseRepository purchases) {
        this.purchases = Objects.requireNonNull(purchases);
    }

    @Override
    public ResultPage<PurchaseView> list(LocalDate from, LocalDate to, PageQuery page) {
        return purchases.findViews(from, to, page);
    }

    @Override
    public PurchaseView get(UUID id) {
        return purchases.findView(id).orElseThrow(() -> new PurchaseNotFoundException(id));
    }
}
