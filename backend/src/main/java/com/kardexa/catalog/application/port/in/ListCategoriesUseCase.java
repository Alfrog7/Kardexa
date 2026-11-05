package com.kardexa.catalog.application.port.in;

import com.kardexa.catalog.domain.model.Category;
import java.util.List;

/** HU-05 · Listar las categorías, activas e inactivas, ordenadas por nombre. */
public interface ListCategoriesUseCase {

    List<Category> listAll();
}
