package com.kardexa.purchasing.application.port.in;

import com.kardexa.purchasing.domain.model.Supplier;

/** HU-11 · Registrar un proveedor con un RUC que no exista todavía (RF-15, RN-10). */
public interface CreateSupplierUseCase {

    Supplier create(CreateSupplierCommand command);

    record CreateSupplierCommand(String ruc, String businessName, String phone, String email) {}
}
