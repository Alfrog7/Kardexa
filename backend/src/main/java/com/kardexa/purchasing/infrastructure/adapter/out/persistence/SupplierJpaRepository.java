package com.kardexa.purchasing.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, UUID> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, UUID id);

    /** Orden alfabético sin distinguir mayúsculas, independiente de la collation de la base. */
    @Query("select s from SupplierJpaEntity s order by lower(s.businessName)")
    List<SupplierJpaEntity> findAllOrderedByBusinessName();

    /** {@code pattern} ya viene en minúsculas, escapado y con los comodines {@code %}. */
    @Query("""
            select s from SupplierJpaEntity s
            where s.ruc like :pattern escape '\\' or lower(s.businessName) like :pattern escape '\\'
            order by lower(s.businessName)""")
    List<SupplierJpaEntity> search(String pattern);
}
