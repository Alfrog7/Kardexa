-- V6 · Usuarios (HU-01, RF-01)
-- La contraseña se guarda solo como hash BCrypt (RNF-01).

CREATE TABLE app_user (
    id            uuid         PRIMARY KEY,
    full_name     varchar(120) NOT NULL CHECK (char_length(full_name) BETWEEN 2 AND 120),
    email         varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    role          varchar(10)  NOT NULL CHECK (role IN ('ADMIN', 'EMPLOYEE')),
    active        boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

-- "Ana@x.com" y "ana@x.com" son el mismo correo.
CREATE UNIQUE INDEX uk_app_user_email ON app_user (lower(email));

COMMENT ON TABLE app_user IS 'Personas que usan el sistema, con su rol (HU-01)';
