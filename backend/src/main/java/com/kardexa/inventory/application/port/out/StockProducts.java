package com.kardexa.inventory.application.port.out;

import java.util.UUID;

/**
 * Lo que el inventario necesita saber de un producto. Lo implementa un adaptador que llama al puerto
 * de entrada de {@code catalog}, así el inventario no depende de él.
 */
public interface StockProducts {

    /**
     * Datos del producto. Si no existe, el catálogo lanza su excepción de "no encontrado", que la API
     * traduce a 404.
     */
    StockProduct require(UUID productId);

    record StockProduct(UUID id, String name, boolean active) {
    }
}
