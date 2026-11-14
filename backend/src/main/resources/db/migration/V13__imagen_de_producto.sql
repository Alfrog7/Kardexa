-- Imagen opcional de cada producto. Se borra con el producto.
CREATE TABLE product_image (
    product_id   uuid         PRIMARY KEY REFERENCES product (id) ON DELETE CASCADE,
    content_type varchar(20)  NOT NULL,
    data         bytea        NOT NULL,
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_product_image_type CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp')),
    CONSTRAINT ck_product_image_size CHECK (octet_length(data) BETWEEN 1 AND 2097152)
);
