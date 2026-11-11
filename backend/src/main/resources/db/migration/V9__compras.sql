-- V9 · Compras (HU-12, RF-16, RN-04, RN-08)
-- Una compra no se borra ni se edita: un error se corrige anulándola (HU-13), con movimientos inversos.
CREATE TABLE purchase (
    id                uuid          PRIMARY KEY,
    supplier_id       uuid          NOT NULL REFERENCES supplier (id),
    supplier_document varchar(40),
    purchase_date     date          NOT NULL,
    status            varchar(10)   NOT NULL CHECK (status IN ('REGISTERED', 'VOIDED')),
    total             numeric(22,2) NOT NULL CHECK (total >= 0),
    user_id           uuid          REFERENCES app_user (id),
    created_at        timestamptz   NOT NULL
);

CREATE INDEX ix_purchase_date ON purchase (purchase_date, created_at);

-- Un producto aparece una sola vez en cada compra.
CREATE TABLE purchase_item (
    purchase_id uuid          NOT NULL REFERENCES purchase (id),
    line_no     smallint      NOT NULL CHECK (line_no > 0),
    product_id  uuid          NOT NULL REFERENCES product (id),
    quantity    numeric(12,3) NOT NULL CHECK (quantity > 0),
    unit_cost   numeric(12,2) NOT NULL CHECK (unit_cost >= 0),
    subtotal    numeric(22,2) NOT NULL CHECK (subtotal >= 0),
    PRIMARY KEY (purchase_id, product_id),
    CONSTRAINT uk_purchase_item_line UNIQUE (purchase_id, line_no)
);

COMMENT ON TABLE purchase IS 'Compras a proveedores: cada una suma stock (HU-12)';
COMMENT ON TABLE purchase_item IS 'Detalle de una compra: producto, cantidad y costo unitario (RF-16)';
