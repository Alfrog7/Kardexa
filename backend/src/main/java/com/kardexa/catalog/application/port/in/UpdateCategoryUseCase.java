package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.Category;
import java.util.UUID;

/** HU-05 · Renombrar, desactivar o reactivar una categoría. */
public interface UpdateCategoryUseCase {

    Category update(UpdateCategoryCommand command);

    record UpdateCategoryCommand(UUID id, String name, boolean active) {}
}
