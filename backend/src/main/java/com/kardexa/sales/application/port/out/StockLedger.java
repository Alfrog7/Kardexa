package com.kardexa.sales.application.port.out;

import com.kardexa.sales.domain.model.Sale;

/**
 * Lo que ventas necesita del inventario: registrar una salida por producto vendido (RF-18). Lo
 * implementa un adaptador que llama al puerto de entrada de {@code inventory} (ADR 0010).
 *
 * <p>Si otro cambió el stock lanza una {@code ConcurrentModificationException}; si un producto se
 * quedaría sin stock (RN-01), la excepción del inventario (422).
 */
public interface StockLedger {

    void withdraw(Sale sale);
}
