package com.kardexa.sales.domain.model;

/** Una venta registrada descuenta stock; una anulada lo devolvió (HU-15). */
public enum SaleStatus {
    REGISTERED,
    VOIDED
}
