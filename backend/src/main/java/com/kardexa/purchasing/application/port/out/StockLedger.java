package com.kardexa.purchasing.application.port.out;

import com.kardexa.purchasing.domain.model.Purchase;

/**
 * Lo que compras necesita del inventario: registrar las entradas de una compra (RF-10). Lo implementa
 * un adaptador que llama al puerto de entrada de {@code inventory} (ADR 0009).
 *
 * <p>Si otro cambió el stock lanza una {@code ConcurrentModificationException}; si un producto no
 * existe o está desactivado, la excepción del inventario (404 o 422).
 */
public interface StockLedger {

    void receive(Purchase purchase);
}
