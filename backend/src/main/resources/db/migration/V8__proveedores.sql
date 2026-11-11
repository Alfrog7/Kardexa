-- V8 · Proveedores (HU-11, RF-15, RN-10)
-- Un proveedor no se borra: se desactiva para conservar las compras que lo usan.
CREATE TABLE supplier (
    id            uuid         PRIMARY KEY,
    ruc           varchar(11)  NOT NULL CHECK (ruc ~ '^[0-9]{11}$'),
    business_name varchar(150) NOT NULL CHECK (char_length(business_name) BETWEEN 2 AND 150),
    phone         varchar(20),
    email         varchar(254),
    active        boolean      NOT NULL DEFAULT true,
    CONSTRAINT uk_supplier_ruc UNIQUE (ruc)
);

COMMENT ON TABLE supplier IS 'Proveedores a los que se registran compras (HU-11)';
