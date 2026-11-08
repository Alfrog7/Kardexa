package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    /** {@code pattern} ya viene en minúsculas, con comodines escapados con {@code !}. */
    @Query(value = """
            select p from ProductJpaEntity p
            where p.active = true
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:pattern is null
                   or lower(p.code) like :pattern escape '!'
                   or lower(p.name) like :pattern escape '!')
            order by lower(p.name), p.id""",
            countQuery = """
            select count(p) from ProductJpaEntity p
            where p.active = true
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:pattern is null
                   or lower(p.code) like :pattern escape '!'
                   or lower(p.name) like :pattern escape '!')""")
    Page<ProductJpaEntity> searchActive(
            @Param("pattern") String pattern, @Param("categoryId") UUID categoryId, Pageable pageable);
}
