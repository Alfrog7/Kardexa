package com.kardexa.sites.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SiteJpaRepository extends JpaRepository<SiteJpaEntity, UUID> {

    @Query("select count(s) > 0 from SiteJpaEntity s where lower(s.name) = lower(:name)")
    boolean existsByNameIgnoreCase(String name);

    @Query("select count(s) > 0 from SiteJpaEntity s where lower(s.name) = lower(:name) and s.id <> :id")
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Orden alfabético sin distinguir mayúsculas, independiente de la collation de la base. */
    @Query("select s from SiteJpaEntity s order by lower(s.name)")
    List<SiteJpaEntity> findAllOrderedByName();
}
