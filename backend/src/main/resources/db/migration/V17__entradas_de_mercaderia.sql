-- V17 · Entradas de mercadería de la sede (HU-21, ADR 0013)
-- Una entrada RECEIPT suma stock en la sede sin costo ni proveedor. Solo se amplían los valores admitidos del
-- kárdex: cada entrada es un documento (source_id) que agrupa sus líneas, sin tabla propia.

ALTER TABLE stock_movement DROP CONSTRAINT stock_movement_type_check;
ALTER TABLE stock_movement ADD CONSTRAINT stock_movement_type_check
    CHECK (type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'PURCHASE_VOID', 'SALE_VOID', 'RECEIPT'));

ALTER TABLE stock_movement DROP CONSTRAINT stock_movement_source_type_check;
ALTER TABLE stock_movement ADD CONSTRAINT stock_movement_source_type_check
    CHECK (source_type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'RECEIPT'));
