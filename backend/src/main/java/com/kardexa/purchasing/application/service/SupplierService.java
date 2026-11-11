package com.kardexa.purchasing.application.service;

import com.kardexa.purchasing.application.port.in.CreateSupplierUseCase;
import com.kardexa.purchasing.application.port.in.ListSuppliersUseCase;
import com.kardexa.purchasing.application.port.in.UpdateSupplierUseCase;
import com.kardexa.purchasing.application.port.out.SupplierRepository;
import com.kardexa.purchasing.domain.exception.DuplicateRucException;
import com.kardexa.purchasing.domain.exception.SupplierNotFoundException;
import com.kardexa.purchasing.domain.model.Supplier;
import java.util.List;
import java.util.Objects;

/** Casos de uso de HU-11. Java puro: se registra como bean en {@code PurchasingConfig}. */
public class SupplierService implements CreateSupplierUseCase, UpdateSupplierUseCase, ListSuppliersUseCase {

    private final SupplierRepository suppliers;

    public SupplierService(SupplierRepository suppliers) {
        this.suppliers = Objects.requireNonNull(suppliers);
    }

    @Override
    public Supplier create(CreateSupplierCommand command) {
        Supplier supplier = Supplier.create(
                new Supplier.Details(command.ruc(), command.businessName(), command.phone(), command.email()));
        if (suppliers.existsByRuc(supplier.ruc())) {
            throw new DuplicateRucException(supplier.ruc());
        }
        return suppliers.save(supplier);
    }

    @Override
    public Supplier update(UpdateSupplierCommand command) {
        Supplier supplier = suppliers.findById(command.id())
                .orElseThrow(() -> new SupplierNotFoundException(command.id()));
        supplier.update(
                new Supplier.Details(command.ruc(), command.businessName(), command.phone(), command.email()));
        if (suppliers.existsByRucExcluding(supplier.ruc(), supplier.id())) {
            throw new DuplicateRucException(supplier.ruc());
        }
        if (command.active()) {
            supplier.activate();
        } else {
            supplier.deactivate();
        }
        return suppliers.save(supplier);
    }

    @Override
    public List<Supplier> list(String query) {
        if (query == null || query.isBlank()) {
            return suppliers.findAllOrderedByBusinessName();
        }
        return suppliers.search(query.strip());
    }
}
