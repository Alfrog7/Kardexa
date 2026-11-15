-- V19 · Cuadre de pagos del turno (HU-23)
-- Al cerrar, quien lleva la caja anota lo recibido por medio de pago; la diferencia (recibido − esperado)
-- se guarda con el turno. Los montos son opcionales: los turnos cerrados antes no los tienen.

ALTER TABLE shift
    ADD COLUMN cash_amount     numeric(22,2) CHECK (cash_amount >= 0),
    ADD COLUMN yape_amount     numeric(22,2) CHECK (yape_amount >= 0),
    ADD COLUMN plin_amount     numeric(22,2) CHECK (plin_amount >= 0),
    ADD COLUMN transfer_amount numeric(22,2) CHECK (transfer_amount >= 0),
    ADD COLUMN received_amount numeric(22,2) CHECK (received_amount >= 0),
    ADD COLUMN difference      numeric(22,2),
    ADD CONSTRAINT ck_shift_payments CHECK (
        (cash_amount IS NULL AND yape_amount IS NULL AND plin_amount IS NULL AND transfer_amount IS NULL
            AND received_amount IS NULL AND difference IS NULL)
        OR (cash_amount IS NOT NULL AND yape_amount IS NOT NULL AND plin_amount IS NOT NULL
            AND transfer_amount IS NOT NULL AND received_amount IS NOT NULL AND difference IS NOT NULL
            AND status = 'CLOSED'));

COMMENT ON COLUMN shift.difference IS 'Recibido − esperado: negativo falta, positivo sobra (HU-23)';
