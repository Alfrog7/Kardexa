package com.kardexa.purchasing.application.service;

import com.kardexa.purchasing.application.port.in.VoidPurchaseUseCase;
import com.kardexa.purchasing.application.port.out.PurchaseRepository;
import com.kardexa.purchasing.application.port.out.StockLedger;
import com.kardexa.purchasing.domain.exception.PurchaseNotFoundException;
import com.kardexa.purchasing.domain.model.Purchase;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * HU-13. Coordina la anulación con el inventario dentro de una sola transacción, que aporta el
 * decorador registrado en {@code PurchasingConfig} junto con los reintentos (ADR 0009).
 *
 * <p>El costo referencial no se revierte: la siguiente compra lo corrige (ADR 0009, punto 5).
 */
public class VoidPurchaseService implements VoidPurchaseUseCase {

    private final PurchaseRepository purchases;
    private final StockLedger stockLedger;
    private final Clock clock;

    public VoidPurchaseService(PurchaseRepository purchases, StockLedger stockLedger, Clock clock) {
        this.purchases = Objects.requireNonNull(purchases);
        this.stockLedger = Objects.requireNonNull(stockLedger);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Purchase voidPurchase(VoidPurchaseCommand command) {
        Purchase purchase = purchases.findById(command.purchaseId())
                .orElseThrow(() -> new PurchaseNotFoundException(command.purchaseId()));
        purchase.voidWith(new Purchase.Voiding(
                command.reason(), command.userId(), clock.instant().truncatedTo(ChronoUnit.MICROS)));
        // RN-01: si alguna salida dejaría stock negativo, el inventario lo rechaza y no se guarda nada.
        stockLedger.reverse(purchase);
        purchases.saveVoiding(purchase);
        return purchase;
    }
}
