package com.kardexa.purchasing.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Un proveedor desactivado no admite compras nuevas. */
public class InactiveSupplierException extends DomainException {

    public InactiveSupplierException(String businessName) {
        super("RF-15", "El proveedor \"" + businessName + "\" está desactivado.");
    }

    @Override
    public String title() {
        return "Proveedor desactivado";
    }
}
