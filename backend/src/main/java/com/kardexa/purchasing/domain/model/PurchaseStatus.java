package com.kardexa.purchasing.domain.model;

/** Estado de una compra: registrada (sumó stock) o anulada (HU-13). */
public enum PurchaseStatus {
    REGISTERED,
    VOIDED
}
