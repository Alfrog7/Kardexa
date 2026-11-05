package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.Category;

/** HU-05 · Crear una categoría con un nombre que no exista todavía. */
public interface CreateCategoryUseCase {

    Category create(CreateCategoryCommand command);

    record CreateCategoryCommand(String name) {}
}
