-- V7 · Usuario real en los movimientos de stock (Fase 4)
-- V5 dejó user_id sin clave foránea porque app_user aún no existía. Los movimientos anteriores
-- conservan user_id nulo y se siguen mostrando como "Sistema". No se modifica V5: Flyway valida su checksum.

ALTER TABLE stock_adjustment
    ADD CONSTRAINT fk_stock_adjustment_user FOREIGN KEY (user_id) REFERENCES app_user (id);

ALTER TABLE stock_movement
    ADD CONSTRAINT fk_stock_movement_user FOREIGN KEY (user_id) REFERENCES app_user (id);
