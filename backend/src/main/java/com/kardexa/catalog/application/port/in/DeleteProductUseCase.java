package com.kardexa.catalog.application.port.in;

import java.util.UUID;

/** Eliminar un producto que nunca se usó. Con historial se inhabilita en su lugar (RN-03). */
public interface DeleteProductUseCase {

    void delete(UUID id);
}
