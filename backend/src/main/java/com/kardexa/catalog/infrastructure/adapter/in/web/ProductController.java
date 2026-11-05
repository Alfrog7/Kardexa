package com.kardexa.catalog.infrastructure.adapter.in.web;

import com.kardexa.catalog.application.port.in.CreateProductUseCase;
import com.kardexa.catalog.application.port.in.GetProductUseCase;
import com.kardexa.catalog.application.port.in.ProductView;
import com.kardexa.catalog.application.port.in.UpdateProductUseCase;
import com.kardexa.catalog.application.port.in.UpdateProductUseCase.UpdateProductCommand;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.catalog.domain.model.Unit;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * {@code /api/v1/products} según el contrato {@code docs/api/openapi.yaml} (HU-06). No hay
 * {@code DELETE}: un producto solo se desactiva (RF-09). La búsqueda llega con HU-07.
 */
@RestController
@RequestMapping("/products")
class ProductController {

    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final GetProductUseCase getProduct;

    ProductController(
            CreateProductUseCase createProduct,
            UpdateProductUseCase updateProduct,
            GetProductUseCase getProduct) {
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.getProduct = getProduct;
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductView created = createProduct.create(request.toDetails());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.product().id())
                .toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(created));
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(getProduct.get(id));
    }

    @PutMapping("/{id}")
    ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(updateProduct.update(
                new UpdateProductCommand(id, request.toDetails(), request.activeOrDefault())));
    }

    /** Esquema {@code ProductRequest}. Montos y cantidades viajan como texto para no perder precisión. */
    record ProductRequest(
            @NotBlank(message = "El código es obligatorio.")
            @Size(max = Product.CODE_MAX_LENGTH, message = "Debe tener como máximo {max} caracteres.")
            String code,
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(min = Product.NAME_MIN_LENGTH, max = Product.NAME_MAX_LENGTH,
                    message = "Debe tener entre {min} y {max} caracteres.")
            String name,
            @NotNull(message = "La categoría es obligatoria.")
            UUID categoryId,
            @NotNull(message = "La unidad es obligatoria.")
            @Pattern(regexp = "UNIT|KG|LITER|BOX", message = "Debe ser UNIT, KG, LITER o BOX.")
            String unit,
            @NotNull(message = "El precio de venta es obligatorio.")
            @Pattern(regexp = "\\d{1,10}(\\.\\d{1,2})?", message = "Monto con hasta 2 decimales, por ejemplo 3.50.")
            String salePrice,
            @NotNull(message = "El stock mínimo es obligatorio.")
            @Pattern(regexp = "\\d{1,9}(\\.\\d{1,3})?", message = "Cantidad con hasta 3 decimales, por ejemplo 2.5.")
            String minStock,
            Boolean active) {

        Product.Details toDetails() {
            return new Product.Details(code, name, categoryId, Unit.valueOf(unit),
                    Money.of(salePrice), Quantity.of(minStock));
        }

        boolean activeOrDefault() {
            return active == null || active;
        }
    }

    /** Esquema {@code Product}. */
    record ProductResponse(
            UUID id,
            String code,
            String name,
            UUID categoryId,
            String categoryName,
            Unit unit,
            String salePrice,
            String minStock,
            boolean active,
            String referenceCost,
            String stock,
            boolean critical) {

        static ProductResponse from(ProductView view) {
            Product p = view.product();
            return new ProductResponse(p.id(), p.code(), p.name(), p.categoryId(), view.categoryName(), p.unit(),
                    p.salePrice().toString(), p.minStock().toString(), p.isActive(),
                    p.referenceCost().toString(), view.stock().toString(), view.critical());
        }
    }
}
