-- V3 · Productos (HU-06, RF-07, RF-09, RN-10)
-- Un producto no se borra: se desactiva (RF-09), porque sus movimientos y ventas lo referencian.

CREATE TABLE product (
    id             uuid          PRIMARY KEY,
    code           varchar(40)   NOT NULL CHECK (char_length(code) BETWEEN 1 AND 40),
    name           varchar(120)  NOT NULL CHECK (char_length(name) BETWEEN 2 AND 120),
    category_id    uuid          NOT NULL REFERENCES category (id),
    unit           varchar(10)   NOT NULL CHECK (unit IN ('UNIT', 'KG', 'LITER', 'BOX')),
    sale_price     numeric(12,2) NOT NULL CHECK (sale_price > 0),
    reference_cost numeric(12,2) NOT NULL DEFAULT 0 CHECK (reference_cost >= 0),
    min_stock      numeric(12,3) NOT NULL CHECK (min_stock >= 0),
    active         boolean       NOT NULL DEFAULT true,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_at     timestamptz   NOT NULL DEFAULT now()
);

-- RN-10: "ab-001" y "AB-001" son el mismo código.
CREATE UNIQUE INDEX uk_product_code ON product (lower(code));
CREATE INDEX ix_product_category ON product (category_id);

COMMENT ON TABLE product IS 'Productos que se compran y venden (HU-06)';
