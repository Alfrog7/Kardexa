package com.kardexa.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.purchasing.application.port.in.CreateSupplierUseCase.CreateSupplierCommand;
import com.kardexa.purchasing.application.port.in.UpdateSupplierUseCase.UpdateSupplierCommand;
import com.kardexa.purchasing.domain.exception.DuplicateRucException;
import com.kardexa.purchasing.domain.exception.SupplierNotFoundException;
import com.kardexa.purchasing.domain.model.Supplier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Casos de uso de proveedores (HU-11)")
class SupplierServiceTest {

    private InMemorySupplierRepository repository;
    private SupplierService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySupplierRepository();
        service = new SupplierService(repository);
    }

    private Supplier create(String ruc, String name) {
        return service.create(new CreateSupplierCommand(ruc, name, null, null));
    }

    @Test
    @DisplayName("criterio 1: un RUC válido y nuevo queda registrado y activo")
    void createsSupplier() {
        Supplier created = create("20123456789", "Distribuidora Lima");

        assertThat(repository.findById(created.id())).isPresent();
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("criterio 2: un RUC ya registrado se rechaza (RN-10)")
    void rejectsDuplicateRuc() {
        create("20123456789", "Distribuidora Lima");

        assertThatThrownBy(() -> create(" 20123456789 ", "Otra empresa"))
                .isInstanceOf(DuplicateRucException.class);
    }

    @Test
    @DisplayName("editar cambia los datos y el estado; puede conservar su propio RUC")
    void updatesSupplier() {
        Supplier created = create("20123456789", "Distribuidora Lima");

        Supplier updated = service.update(new UpdateSupplierCommand(
                created.id(), "20123456789", "Distribuidora Lima SAC", "987654321", null, false));

        assertThat(updated.businessName()).isEqualTo("Distribuidora Lima SAC");
        assertThat(updated.details().phone()).isEqualTo("987654321");
        assertThat(repository.findById(created.id()).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("editar con el RUC de otro proveedor se rechaza; un id inexistente da no encontrado")
    void updateErrors() {
        create("20123456789", "Distribuidora Lima");
        Supplier other = create("10456789012", "Bodega Central");

        assertThatThrownBy(() -> service.update(new UpdateSupplierCommand(
                other.id(), "20123456789", "Bodega Central", null, null, true)))
                .isInstanceOf(DuplicateRucException.class);
        assertThatThrownBy(() -> service.update(new UpdateSupplierCommand(
                UUID.randomUUID(), "20999999999", "Nadie", null, null, true)))
                .isInstanceOf(SupplierNotFoundException.class);
    }

    @Test
    @DisplayName("la lista filtra por RUC o razón social y ordena por razón social")
    void listsAndSearches() {
        create("20123456789", "Distribuidora Lima");
        create("10456789012", "bodega Central");

        assertThat(service.list(null)).extracting(Supplier::businessName)
                .containsExactly("bodega Central", "Distribuidora Lima");
        assertThat(service.list("  LIMA ")).extracting(Supplier::ruc).containsExactly("20123456789");
        assertThat(service.list("104567")).extracting(Supplier::businessName).containsExactly("bodega Central");
    }
}
