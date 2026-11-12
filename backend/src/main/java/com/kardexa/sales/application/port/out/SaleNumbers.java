package com.kardexa.sales.application.port.out;

import com.kardexa.sales.domain.model.SaleNumber;

/**
 * RN-07: reserva el siguiente número de comprobante dentro de la transacción de la venta. Si la venta
 * se revierte, el número también, así que no quedan huecos (ADR 0010).
 */
public interface SaleNumbers {

    SaleNumber next();
}
