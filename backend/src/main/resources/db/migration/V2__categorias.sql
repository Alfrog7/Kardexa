-- V2 · Categorías (HU-05, RF-06)
-- Una categoría no se borra: se desactiva para conservar los productos que la usan.

CREATE TABLE category (
    id     uuid        PRIMARY KEY,
    name   varchar(60) NOT NULL CHECK (char_length(name) BETWEEN 2 AND 60),
    active boolean     NOT NULL DEFAULT true
);

-- "Bebidas" y "bebidas" son la misma categoría: la unicidad no distingue mayúsculas.
CREATE UNIQUE INDEX uk_category_name ON category (lower(name));

COMMENT ON TABLE category IS 'Agrupa productos para buscarlos y analizarlos (HU-05)';
