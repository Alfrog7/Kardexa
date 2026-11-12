-- HU-15 · Anular una venta: motivo, fecha y usuario de la anulación (RF-20, RN-03, RN-05).
-- La venta conserva su número (RN-07): solo cambia el estado.
ALTER TABLE sale
    ADD COLUMN void_reason varchar(255),
    ADD COLUMN voided_at   timestamptz,
    ADD COLUMN voided_by   uuid REFERENCES app_user (id),
    ADD CONSTRAINT ck_sale_void
        CHECK ((status = 'VOIDED') = (void_reason IS NOT NULL AND voided_at IS NOT NULL));
