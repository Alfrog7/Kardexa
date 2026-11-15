-- V18 · Turnos con cierre por conteo (HU-22, RN-14, RN-15, ADR 0013)
-- Un turno lo abre quien lleva la caja y lo cierra contando lo que sobró de cada producto. Solo puede haber un
-- turno abierto por sede. Un turno cerrado no se edita ni se borra (RN-03).

CREATE TABLE shift (
    id               uuid          PRIMARY KEY,
    site_id          uuid          NOT NULL REFERENCES site (id),
    responsible_id   uuid          NOT NULL REFERENCES app_user (id),
    status           varchar(6)    NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opened_at        timestamptz   NOT NULL,
    closed_at        timestamptz,
    closed_by        uuid          REFERENCES app_user (id),
    registered_sales numeric(22,2) CHECK (registered_sales >= 0),
    counted_sales    numeric(22,2) CHECK (counted_sales >= 0),
    expected_amount  numeric(22,2) CHECK (expected_amount >= 0),
    note             varchar(255),
    CONSTRAINT ck_shift_closed CHECK (
        (status = 'OPEN' AND closed_at IS NULL)
        OR (status = 'CLOSED' AND closed_at IS NOT NULL AND closed_by IS NOT NULL AND expected_amount IS NOT NULL))
);

-- RN-14: un solo turno abierto por sede.
CREATE UNIQUE INDEX uk_shift_open_per_site ON shift (site_id) WHERE status = 'OPEN';
CREATE INDEX ix_shift_site_opened ON shift (site_id, opened_at);

CREATE TABLE shift_item (
    shift_id      uuid          NOT NULL REFERENCES shift (id),
    product_id    uuid          NOT NULL REFERENCES product (id),
    product_name  varchar(120)  NOT NULL,
    opening_stock numeric(12,3) NOT NULL CHECK (opening_stock >= 0),
    leftover      numeric(12,3) CHECK (leftover >= 0),
    sold_by_count numeric(12,3) CHECK (sold_by_count >= 0),
    unit_price    numeric(12,2) CHECK (unit_price >= 0),
    PRIMARY KEY (shift_id, product_id)
);

CREATE FUNCTION forbid_closed_shift_change() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'CLOSED' THEN
        RAISE EXCEPTION 'Un turno cerrado es inmutable (RN-03, RN-14)';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE FUNCTION forbid_closed_shift_item_change() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM shift WHERE id = OLD.shift_id AND status = 'CLOSED') THEN
        RAISE EXCEPTION 'Un turno cerrado es inmutable (RN-03, RN-14)';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER shift_closed_immutable BEFORE UPDATE OR DELETE ON shift
    FOR EACH ROW EXECUTE FUNCTION forbid_closed_shift_change();
CREATE TRIGGER shift_item_closed_immutable BEFORE UPDATE OR DELETE ON shift_item
    FOR EACH ROW EXECUTE FUNCTION forbid_closed_shift_item_change();

ALTER TABLE stock_movement DROP CONSTRAINT stock_movement_type_check;
ALTER TABLE stock_movement ADD CONSTRAINT stock_movement_type_check
    CHECK (type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'PURCHASE_VOID', 'SALE_VOID', 'RECEIPT', 'SHIFT_CLOSE'));
ALTER TABLE stock_movement DROP CONSTRAINT stock_movement_source_type_check;
ALTER TABLE stock_movement ADD CONSTRAINT stock_movement_source_type_check
    CHECK (source_type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'RECEIPT', 'SHIFT'));

COMMENT ON TABLE shift IS 'Turno de una sede, cerrado por conteo de lo que sobró (RN-14, RN-15)';
