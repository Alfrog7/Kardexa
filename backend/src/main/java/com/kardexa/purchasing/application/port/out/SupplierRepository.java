package com.kardexa.purchasing.application.port.out;

import com.kardexa.purchasing.domain.model.Supplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de proveedores.
 *
 * <p>{@link #save} lanza {@code DuplicateRucException} si otro proveedor ya tiene el RUC: es la
 * garantía final frente a dos altas simultáneas, aunque el servicio lo compruebe antes.
 */
public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(UUID id);

    /** Proveedores cuyo RUC o razón social contienen el texto (sin distinguir mayúsculas), por razón social. */
    List<Supplier> search(String text);

    List<Supplier> findAllOrderedByBusinessName();

    boolean existsByRuc(String ruc);

    boolean existsByRucExcluding(String ruc, UUID excludedId);
}
