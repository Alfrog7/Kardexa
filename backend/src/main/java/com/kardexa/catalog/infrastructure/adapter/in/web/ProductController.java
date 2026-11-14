package com.kardexa.catalog.infrastructure.adapter.in.web;

import com.kardexa.catalog.application.port.in.DeleteProductUseCase;
import com.kardexa.catalog.application.port.in.ManageProductImageUseCase;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.kardexa.catalog.application.port.in.CreateProductUseCase;
import com.kardexa.catalog.application.port.in.GetProductUseCase;
import com.kardexa.catalog.application.port.in.ProductView;
import com.kardexa.catalog.application.port.in.SearchProductsUseCase.ProductSearch;
import com.kardexa.catalog.application.port.in.SearchProductsUseCase;
import com.kardexa.catalog.application.port.in.UpdateProductUseCase;
import com.kardexa.catalog.application.port.in.UpdateProductUseCase.UpdateProductCommand;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.catalog.domain.model.Unit;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
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
 * {@code DELETE}: un producto solo se desactiva (RF-09). La búsqueda es de HU-07.
 */
@RestController
@RequestMapping("/products")
class ProductController {

    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final GetProductUseCase getProduct;
    private final SearchProductsUseCase searchProducts;
    private final DeleteProductUseCase deleteProduct;
    private final ManageProductImageUseCase productImage;

    ProductController(
            CreateProductUseCase createProduct,
            UpdateProductUseCase updateProduct,
            GetProductUseCase getProduct,
            SearchProductsUseCase searchProducts,
            DeleteProductUseCase deleteProduct,
            ManageProductImageUseCase productImage) {
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.getProduct = getProduct;
        this.searchProducts = searchProducts;
        this.deleteProduct = deleteProduct;
        this.productImage = productImage;
    }

    /** Elimina un producto sin historial; con movimientos, compras o ventas responde 409 (RN-03). */
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteProduct.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Sube o reemplaza la imagen (PNG, JPG o WebP de hasta 2 MB). */
    @PutMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ProductResponse uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws IOException {
        return ProductResponse.from(productImage.upload(id, file.getBytes()));
    }

    @DeleteMapping("/{id}/image")
    ProductResponse removeImage(@PathVariable UUID id) {
        return ProductResponse.from(productImage.remove(id));
    }

    /**
     * Imagen pública para que una etiqueta img la cargue sin token. La URL lleva la versión en
     * {@code ?v=}, así que se puede guardar en caché mucho tiempo.
     */
    @GetMapping("/{id}/image")
    ResponseEntity<byte[]> image(@PathVariable UUID id) {
        return productImage.find(id)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(image.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** HU-07 y HU-10 · Búsqueda paginada de productos (activos, salvo que se pidan todos; {@code searchProducts} en el contrato). */
    @GetMapping
    ProductPageResponse search(@Valid @ParameterObject SearchParams params) {
        ResultPage<ProductView> page = searchProducts.search(new ProductSearch(
                params.q(), params.categoryId(), params.criticalOnlyOrDefault(), params.includeInactiveOrDefault(),
                PageQuery.of(params.page(), params.size())));
        return ProductPageResponse.from(page);
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
            // Opcional al crear: vacío, la API asigna el siguiente de la secuencia P-0001.
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

    /** Parámetros de consulta de {@code GET /products}; los que no llegan toman su valor por defecto. */
    record SearchParams(
            @Size(max = ProductSearch.TEXT_MAX_LENGTH, message = "Debe tener como máximo {max} caracteres.")
            String q,
            UUID categoryId,
            Boolean criticalOnly,
            Boolean includeInactive,
            @Min(value = 0, message = "Debe ser 0 o mayor.")
            Integer page,
            @Min(value = 1, message = "Debe estar entre 1 y " + PageQuery.MAX_SIZE + ".")
            @Max(value = PageQuery.MAX_SIZE, message = "Debe estar entre 1 y " + PageQuery.MAX_SIZE + ".")
            Integer size) {

        boolean criticalOnlyOrDefault() {
            return Boolean.TRUE.equals(criticalOnly);
        }

        boolean includeInactiveOrDefault() {
            return Boolean.TRUE.equals(includeInactive);
        }
    }

    /** Esquema {@code ProductPage}: {@code PageMeta} más el contenido. */
    record ProductPageResponse(List<ProductResponse> content, int page, int size, long totalElements, int totalPages) {

        static ProductPageResponse from(ResultPage<ProductView> page) {
            return new ProductPageResponse(page.content().stream().map(ProductResponse::from).toList(),
                    page.page(), page.size(), page.totalElements(), page.totalPages());
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
            boolean critical,
            String imageUrl) {

        static ProductResponse from(ProductView view) {
            Product p = view.product();
            return new ProductResponse(p.id(), p.code(), p.name(), p.categoryId(), view.categoryName(), p.unit(),
                    p.salePrice().toString(), p.minStock().toString(), p.isActive(),
                    p.referenceCost().toString(), view.stock().toString(), view.critical(),
                    view.imageVersion() == null ? null
                            : "/api/v1/products/%s/image?v=%d".formatted(p.id(), view.imageVersion().toEpochMilli()));
        }
    }
}
