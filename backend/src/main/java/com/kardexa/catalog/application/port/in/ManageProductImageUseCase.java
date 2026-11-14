package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.ProductImage;
import java.util.Optional;
import java.util.UUID;

/** Subir, quitar y leer la imagen de un producto. */
public interface ManageProductImageUseCase {

    /** Guarda o reemplaza la imagen y devuelve el producto con su nueva versión de imagen. */
    ProductView upload(UUID productId, byte[] data);

    ProductView remove(UUID productId);

    Optional<ProductImage> find(UUID productId);
}
