package com.kardexa.purchasing.infrastructure.adapter.in.web;

import com.kardexa.purchasing.application.port.in.CreateSupplierUseCase;
import com.kardexa.purchasing.application.port.in.CreateSupplierUseCase.CreateSupplierCommand;
import com.kardexa.purchasing.application.port.in.ListSuppliersUseCase;
import com.kardexa.purchasing.application.port.in.UpdateSupplierUseCase;
import com.kardexa.purchasing.application.port.in.UpdateSupplierUseCase.UpdateSupplierCommand;
import com.kardexa.purchasing.domain.model.Supplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** {@code /api/v1/suppliers} según el contrato {@code docs/api/openapi.yaml} (HU-11). */
@RestController
@RequestMapping("/suppliers")
class SupplierController {

    private final CreateSupplierUseCase createSupplier;
    private final UpdateSupplierUseCase updateSupplier;
    private final ListSuppliersUseCase listSuppliers;

    SupplierController(
            CreateSupplierUseCase createSupplier,
            UpdateSupplierUseCase updateSupplier,
            ListSuppliersUseCase listSuppliers) {
        this.createSupplier = createSupplier;
        this.updateSupplier = updateSupplier;
        this.listSuppliers = listSuppliers;
    }

    @GetMapping
    List<SupplierResponse> list(@RequestParam(required = false) String q) {
        return listSuppliers.list(q).stream().map(SupplierResponse::from).toList();
    }

    @PostMapping
    ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        Supplier created = createSupplier.create(new CreateSupplierCommand(
                request.ruc(), request.businessName(), request.phone(), request.email()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(SupplierResponse.from(created));
    }

    @PutMapping("/{id}")
    SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return SupplierResponse.from(updateSupplier.update(new UpdateSupplierCommand(
                id, request.ruc(), request.businessName(), request.phone(), request.email(),
                request.activeOrDefault())));
    }

    /** Esquema {@code SupplierRequest}: {@code active} es opcional y vale {@code true} por defecto. */
    record SupplierRequest(
            @NotBlank(message = "El RUC es obligatorio.")
            @Pattern(regexp = "\\s*\\d{11}\\s*", message = "El RUC debe tener exactamente 11 dígitos.")
            String ruc,
            @NotBlank(message = "La razón social es obligatoria.")
            @Size(min = Supplier.BUSINESS_NAME_MIN_LENGTH, max = Supplier.BUSINESS_NAME_MAX_LENGTH,
                    message = "Debe tener entre {min} y {max} caracteres.")
            String businessName,
            @Size(max = Supplier.PHONE_MAX_LENGTH, message = "Admite hasta {max} caracteres.")
            String phone,
            @Email(message = "El correo no es válido.")
            @Size(max = Supplier.EMAIL_MAX_LENGTH, message = "Admite hasta {max} caracteres.")
            String email,
            Boolean active) {

        boolean activeOrDefault() {
            return active == null || active;
        }
    }

    /** Esquema {@code Supplier}. */
    record SupplierResponse(UUID id, String ruc, String businessName, String phone, String email, boolean active) {

        static SupplierResponse from(Supplier supplier) {
            Supplier.Details d = supplier.details();
            return new SupplierResponse(
                    supplier.id(), d.ruc(), d.businessName(), d.phone(), d.email(), supplier.isActive());
        }
    }
}
