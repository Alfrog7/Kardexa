package com.kardexa.purchasing.application.service;

import com.kardexa.purchasing.application.port.out.StockLedger;
import com.kardexa.purchasing.domain.model.Purchase;
import java.util.ArrayList;
import java.util.List;

/** Registra lo que se le pidió al inventario; {@link #failure} simula un rechazo del inventario. */
class FakeStockLedger implements StockLedger {

    final List<Purchase> received = new ArrayList<>();
    final List<Purchase> reversed = new ArrayList<>();
    RuntimeException failure;

    @Override
    public void receive(Purchase purchase) {
        failIfAsked();
        received.add(purchase);
    }

    @Override
    public void reverse(Purchase purchase) {
        failIfAsked();
        reversed.add(purchase);
    }

    private void failIfAsked() {
        if (failure != null) {
            throw failure;
        }
    }
}
