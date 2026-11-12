package com.kardexa.catalog.application.port.out;

import com.kardexa.catalog.domain.model.Category;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de categorías. Las comparaciones de nombre no distinguen mayúsculas.
 *
 * <p>{@link #save} lanza {@code DuplicateCategoryNameException} si otra categoría ya tiene el
 * nombre: es la garantía final frente a dos altas simultáneas, aunque el servicio lo compruebe antes.
 */
public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    List<Category> findAllOrderedByName();

    /** Productos por categoría, en una consulta. */
    Map<UUID, Long> countProductsByCategory();

    /** Las categorías con esos ids, en cualquier orden; los ids inexistentes se ignoran. */
    List<Category> findAllById(Collection<UUID> ids);

    boolean existsByName(String name);

    boolean existsByNameExcluding(String name, UUID excludedId);
}
