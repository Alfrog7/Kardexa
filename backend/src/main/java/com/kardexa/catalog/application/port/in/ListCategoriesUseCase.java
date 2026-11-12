package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.Category;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HU-05 · Listar las categorías, activas e inactivas, ordenadas por nombre. */
public interface ListCategoriesUseCase {

    List<Category> listAll();

    /** Cuántos productos (activos e inactivos) tiene cada categoría; las que no tienen no aparecen. */
    Map<UUID, Long> productCounts();
}
