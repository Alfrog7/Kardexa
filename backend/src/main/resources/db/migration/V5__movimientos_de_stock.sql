-- V5 · Ajustes y movimientos de stock (módulo inventory, HU-09, ADR 0006)
-- user_id queda sin clave foránea ni NOT NULL hasta que exista app_user (Fase 3).

CREATE TABLE stock_adjustment (
    id         uuid          PRIMARY KEY,
    product_id uuid          NOT NULL REFERENCES product (id),
    direction  varchar(3)    NOT NULL CHECK (direction IN ('IN', 'OUT')),
    quantity   numeric(12,3) NOT NULL CHECK (quantity > 0),
    reason     varchar(20)   NOT NULL CHECK (reason IN ('SHRINKAGE', 'PHYSICAL_COUNT', 'CORRECTION')),
    note       varchar(255),
    user_id    uuid,
    created_at timestamptz   NOT NULL
);

CREATE INDEX ix_stock_adjustment_product ON stock_adjustment (product_id);

CREATE TABLE stock_movement (
    id            uuid          PRIMARY KEY,
    -- Orden real del kárdex: se asigna con la fila de stock bloqueada, así que por producto
    -- siempre sigue el orden de los saldos, aunque dos movimientos coincidan en occurred_at.
    seq           bigint        GENERATED ALWAYS AS IDENTITY,
    product_id    uuid          NOT NULL REFERENCES product (id),
    type          varchar(15)   NOT NULL CHECK (type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'PURCHASE_VOID', 'SALE_VOID')),
    direction     varchar(3)    NOT NULL CHECK (direction IN ('IN', 'OUT')),
    quantity      numeric(12,3) NOT NULL CHECK (quantity > 0),
    balance_after numeric(12,3) NOT NULL CHECK (balance_after >= 0),
    source_type   varchar(10)   NOT NULL CHECK (source_type IN ('PURCHASE', 'SALE', 'ADJUSTMENT')),
    source_id     uuid          NOT NULL,
    user_id       uuid,
    occurred_at   timestamptz   NOT NULL
);

CREATE INDEX ix_stock_movement_product_date ON stock_movement (product_id, occurred_at);

-- RN-03: lo registrado no se edita ni se borra. Un error se corrige con un movimiento inverso.
CREATE FUNCTION forbid_stock_record_change() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% es inmutable (RN-03)', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER stock_movement_immutable
    BEFORE UPDATE OR DELETE ON stock_movement
    FOR EACH ROW EXECUTE FUNCTION forbid_stock_record_change();

CREATE TRIGGER stock_adjustment_immutable
    BEFORE UPDATE OR DELETE ON stock_adjustment
    FOR EACH ROW EXECUTE FUNCTION forbid_stock_record_change();

COMMENT ON TABLE stock_movement IS 'Kárdex: movimientos inmutables de entrada y salida (RN-02, RN-03, RN-04)';
COMMENT ON TABLE stock_adjustment IS 'Ajustes manuales de stock con motivo (RF-11, RN-05)';
