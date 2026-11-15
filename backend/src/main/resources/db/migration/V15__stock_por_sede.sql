-- V15 · Stock y documentos por sede (HU-19, RN-13, ADR 0013)
-- El stock pasa a ser por (producto, sede) y cada venta, compra, ajuste y movimiento del kárdex dice en qué
-- sede ocurrió. Todo lo existente queda en la «Sede principal» (V14), con los mismos saldos e historial.
-- Las columnas se agregan con ese valor por defecto solo para rellenar lo anterior; luego se quita para que
-- ninguna escritura nueva caiga en una sede por omisión. Agregar una columna con valor constante no dispara
-- el trigger de inmutabilidad de stock_movement (no es un UPDATE de filas).

ALTER TABLE product_stock
    ADD COLUMN site_id uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES site (id);
ALTER TABLE product_stock DROP CONSTRAINT product_stock_pkey;
ALTER TABLE product_stock ADD PRIMARY KEY (product_id, site_id);
ALTER TABLE product_stock ALTER COLUMN site_id DROP DEFAULT;

ALTER TABLE stock_movement
    ADD COLUMN site_id uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES site (id);
ALTER TABLE stock_movement ALTER COLUMN site_id DROP DEFAULT;
CREATE INDEX ix_stock_movement_site_product_date ON stock_movement (site_id, product_id, occurred_at);

ALTER TABLE stock_adjustment
    ADD COLUMN site_id uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES site (id);
ALTER TABLE stock_adjustment ALTER COLUMN site_id DROP DEFAULT;

ALTER TABLE sale
    ADD COLUMN site_id uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES site (id);
ALTER TABLE sale ALTER COLUMN site_id DROP DEFAULT;
CREATE INDEX ix_sale_site_sold_at ON sale (site_id, sold_at);

ALTER TABLE purchase
    ADD COLUMN site_id uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES site (id);
ALTER TABLE purchase ALTER COLUMN site_id DROP DEFAULT;
CREATE INDEX ix_purchase_site_date ON purchase (site_id, purchase_date);

COMMENT ON COLUMN product_stock.site_id IS 'Sede a la que pertenece este stock (RN-13)';
