package com.kardexa.purchasing.application.port.in;

import com.kardexa.purchasing.domain.model.Supplier;
import java.util.UUID;

/** HU-11 · Editar, desactivar o reactivar un proveedor. */
public interface UpdateSupplierUseCase {

    Supplier update(UpdateSupplierCommand command);

    record UpdateSupplierCommand(
            UUID id, String ruc, String businessName, String phone, String email, boolean active) {}
}
