package com.kardexa.catalog.application.port.in;

import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.Objects;
import java.util.UUID;

/** HU-07 · Buscar productos activos por nombre o código y filtrarlos por categoría (RF-08). */
public interface SearchProductsUseCase {

    ResultPage<ProductView> search(ProductSearch search);

    /**
     * Criterios de búsqueda. Un texto vacío o solo con espacios equivale a no filtrar por texto.
     *
     * @param text       texto contenido en el código o el nombre, sin distinguir mayúsculas; opcional
     * @param categoryId categoría de los productos; opcional
     * @param page       página pedida
     */
    record ProductSearch(String text, UUID categoryId, PageQuery page) {

        public static final int TEXT_MAX_LENGTH = 100;

        public ProductSearch {
            Objects.requireNonNull(page, "page");
            text = text == null || text.isBlank() ? null : text.strip();
            if (text != null && text.length() > TEXT_MAX_LENGTH) {
                throw new IllegalArgumentException("El texto de búsqueda supera " + TEXT_MAX_LENGTH + " caracteres.");
            }
        }
    }
}
