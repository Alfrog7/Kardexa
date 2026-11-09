-- V4 · Stock actual por producto (módulo inventory, ADR 0006)
-- La fila se crea con el primer movimiento; un producto sin fila tiene stock 0.

CREATE TABLE product_stock (
    product_id uuid          PRIMARY KEY REFERENCES product (id),
    quantity   numeric(12,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0), -- RN-01, defensa en profundidad
    version    bigint        NOT NULL DEFAULT 0,                         -- bloqueo optimista
    updated_at timestamptz   NOT NULL DEFAULT now()
);

COMMENT ON TABLE product_stock IS 'Stock actual: saldo del último movimiento del kárdex (RN-02)';
