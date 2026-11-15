-- V16 · Trabajadores asignados a una sede (HU-20, RN-14, ADR 0013)
-- El empleado pertenece a una sola sede; el administrador (site_id nulo) las ve todas.
-- Los empleados que ya existían quedan en la «Sede principal».
ALTER TABLE app_user ADD COLUMN site_id uuid REFERENCES site (id);

UPDATE app_user SET site_id = '00000000-0000-0000-0000-000000000001' WHERE role = 'EMPLOYEE';

COMMENT ON COLUMN app_user.site_id IS 'Sede del empleado; nulo en el administrador, que opera en todas (RN-14)';
