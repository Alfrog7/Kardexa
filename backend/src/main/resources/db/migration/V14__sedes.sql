-- V14 · Sedes (HU-19, RF-26, ADR 0013)
-- Una sede nunca se borra: se inhabilita para conservar su historial.
-- «Sede principal» recibe todo lo que existía antes de la Fase 8 (la migración del stock por sede la usa);
-- su identificador es fijo para que las migraciones y el código puedan referirla.
CREATE TABLE site (
    id      uuid         PRIMARY KEY,
    name    varchar(80)  NOT NULL CHECK (char_length(name) BETWEEN 2 AND 80),
    address varchar(200),
    active  boolean      NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX uk_site_name ON site (lower(name));

INSERT INTO site (id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'Sede principal');

COMMENT ON TABLE site IS 'Locales del negocio, cada uno con su stock y kárdex (HU-19, RN-13)';
