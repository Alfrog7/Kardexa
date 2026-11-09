package com.kardexa.catalog.application.port.in;

import java.util.UUID;

/**
 * Datos básicos de un producto para otros módulos, sin exponerles el modelo del catálogo. Si el
 * producto no existe lanza {@code ProductNotFoundException} (404).
 */
public interface GetProductSummaryUseCase {

    ProductSummary getSummary(UUID id);

    record ProductSummary(UUID id, String name, boolean active) {
    }
}
