-- V10 · Anulación de compras (HU-13, RF-17, RN-03)
-- Anular no borra nada: la compra queda en VOIDED con su motivo, y el inventario registra las salidas inversas.
ALTER TABLE purchase
    ADD COLUMN void_reason varchar(255),
    ADD COLUMN voided_at   timestamptz,
    ADD COLUMN voided_by   uuid REFERENCES app_user (id),
    ADD CONSTRAINT ck_purchase_void
        CHECK ((status = 'VOIDED') = (void_reason IS NOT NULL AND voided_at IS NOT NULL));
