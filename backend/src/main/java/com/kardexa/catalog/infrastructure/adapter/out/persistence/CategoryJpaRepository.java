package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Orden alfabético sin distinguir mayúsculas, independiente de la collation de la base. */
    @Query("select c from CategoryJpaEntity c order by lower(c.name)")
    List<CategoryJpaEntity> findAllOrderedByName();

    @Query(nativeQuery = true, value = "SELECT category_id, count(*) FROM product GROUP BY category_id")
    List<Object[]> countProductsByCategory();
}
