package com.kardexa.catalog.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    /** RN-08: solo el costo, sin tocar los datos que edita el administrador. */
    @Modifying(flushAutomatically = true)
    @Query("update ProductJpaEntity p set p.referenceCost = :cost where p.id = :id")
    int updateReferenceCost(@Param("id") UUID id, @Param("cost") BigDecimal cost);

    /**
     * Búsqueda de HU-07 y HU-10. SQL nativo porque cruza con {@code product_stock}, tabla del módulo
     * {@code inventory}: es una lectura permitida por el ADR 0007 (solo lectura, solo en este adaptador).
     *
     * <p>Ningún parámetro es nulo, para que PostgreSQL conozca siempre su tipo: sin texto, el patrón es
     * {@code %}; sin categoría, {@code anyCategory} es verdadero. {@code pattern} llega en minúsculas,
     * con los comodines escapados con {@code !}. Sin fila de stock, el stock es 0.
     */
    @Query(nativeQuery = true, value = "SELECT p.* " + SEARCH_FROM + " ORDER BY lower(p.name), p.id",
            countQuery = "SELECT count(*) " + SEARCH_FROM)
    Page<ProductJpaEntity> search(
            @Param("pattern") String pattern,
            @Param("anyCategory") boolean anyCategory,
            @Param("categoryId") UUID categoryId,
            @Param("criticalOnly") boolean criticalOnly,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable);

    String SEARCH_FROM = """
            FROM product p
            LEFT JOIN product_stock s ON s.product_id = p.id
            WHERE (:includeInactive OR p.active)
              AND (:anyCategory OR p.category_id = :categoryId)
              AND (lower(p.code) LIKE :pattern ESCAPE '!' OR lower(p.name) LIKE :pattern ESCAPE '!')
              AND (NOT :criticalOnly OR coalesce(s.quantity, 0) <= p.min_stock)
            """;
}
