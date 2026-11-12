-- HU-14 · Ventas con comprobante correlativo (RF-18, RF-19, ADR 0010).

-- RN-07: una sola fila que reserva el siguiente número dentro de la transacción de la venta.
-- Si la venta se revierte, el número también: no quedan huecos ni repetidos.
CREATE TABLE sale_number_counter (
    id         smallint PRIMARY KEY CHECK (id = 1),
    last_value bigint   NOT NULL CHECK (last_value >= 0)
);

INSERT INTO sale_number_counter (id, last_value) VALUES (1, 0);

CREATE TABLE sale (
    id        uuid          PRIMARY KEY,
    number    varchar(20)   NOT NULL CHECK (number ~ '^V-[0-9]{6,}$'),
    status    varchar(10)   NOT NULL CHECK (status IN ('REGISTERED', 'VOIDED')),
    total     numeric(22,2) NOT NULL CHECK (total > 0),
    seller_id uuid          NOT NULL REFERENCES app_user (id),
    sold_at   timestamptz   NOT NULL,
    CONSTRAINT uk_sale_number UNIQUE (number)
);

CREATE INDEX ix_sale_sold_at ON sale (sold_at);
CREATE INDEX ix_sale_seller ON sale (seller_id, sold_at);

-- RN-06: el código, el nombre y el precio se copian; cambiar el producto no altera la venta.
CREATE TABLE sale_item (
    sale_id      uuid          NOT NULL REFERENCES sale (id),
    line_no      smallint      NOT NULL CHECK (line_no > 0),
    product_id   uuid          NOT NULL REFERENCES product (id),
    product_code varchar(40)   NOT NULL,
    product_name varchar(120)  NOT NULL,
    quantity     numeric(12,3) NOT NULL CHECK (quantity > 0),
    unit_price   numeric(12,2) NOT NULL CHECK (unit_price > 0),
    subtotal     numeric(22,2) NOT NULL CHECK (subtotal >= 0),
    PRIMARY KEY (sale_id, product_id),
    CONSTRAINT uk_sale_item_line UNIQUE (sale_id, line_no)
);

COMMENT ON TABLE sale_number_counter IS 'Último número de comprobante de venta usado (RN-07)';
COMMENT ON TABLE sale IS 'Ventas: cada una descuenta stock y tiene un comprobante correlativo (HU-14)';
COMMENT ON TABLE sale_item IS 'Detalle de una venta con el precio copiado del producto (RN-06)';
