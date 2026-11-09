package com.kardexa.inventory.domain.model;

/** Qué operación generó un movimiento del kárdex. */
public enum MovementType {
    PURCHASE,
    SALE,
    ADJUSTMENT,
    PURCHASE_VOID,
    SALE_VOID
}
