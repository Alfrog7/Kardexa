package com.kardexa.inventory.application.port.out;

import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;

/** Escritura de ajustes y movimientos del kárdex. Solo se agregan: son inmutables (RN-03). */
public interface StockRecords {

    void add(StockAdjustment adjustment);

    void add(StockMovement movement);
}
