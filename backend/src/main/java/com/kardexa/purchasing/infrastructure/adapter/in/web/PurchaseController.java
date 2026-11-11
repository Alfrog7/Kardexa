package com.kardexa.purchasing.infrastructure.adapter.in.web;

import com.kardexa.purchasing.application.port.in.ListPurchasesUseCase;
import com.kardexa.purchasing.application.port.in.PurchaseView;
import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase;
import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase.ItemCommand;
import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase.RegisterPurchaseCommand;
import com.kardexa.purchasing.application.port.in.VoidPurchaseUseCase;
import com.kardexa.purchasing.application.port.in.VoidPurchaseUseCase.VoidPurchaseCommand;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.purchasing.infrastructure.adapter.in.web.SupplierController.SupplierResponse;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** {@code /api/v1/purchases} según el contrato {@code docs/api/openapi.yaml} (HU-12). Solo ADMIN. */
@RestController
@RequestMapping("/purchases")
class PurchaseController {

    private static final String QUANTITY = "\\d{1,9}(\\.\\d{1,3})?";
    private static final String AMOUNT = "\\d{1,10}(\\.\\d{1,2})?";

    private final RegisterPurchaseUseCase registerPurchase;
    private final VoidPurchaseUseCase voidPurchase;
    private final ListPurchasesUseCase listPurchases;

    PurchaseController(
            RegisterPurchaseUseCase registerPurchase,
            VoidPurchaseUseCase voidPurchase,
            ListPurchasesUseCase listPurchases) {
        this.registerPurchase = registerPurchase;
        this.voidPurchase = voidPurchase;
        this.listPurchases = listPurchases;
    }

    @GetMapping
    PurchasePageResponse list(@Valid @ParameterObject PurchaseParams params) {
        return PurchasePageResponse.from(
                listPurchases.list(params.from(), params.to(), PageQuery.of(params.page(), params.size())));
    }

    @PostMapping
    ResponseEntity<PurchaseResponse> register(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PurchaseRequest request) {
        Purchase purchase = registerPurchase.register(request.toCommand(UUID.fromString(jwt.getSubject())));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(purchase.id())
                .toUri();
        return ResponseEntity.created(location).body(PurchaseResponse.from(listPurchases.get(purchase.id())));
    }

    /** HU-13: devuelve la compra ya anulada. */
    @PostMapping("/{id}/void")
    PurchaseResponse voidPurchase(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody VoidRequest request) {
        voidPurchase.voidPurchase(new VoidPurchaseCommand(id, request.reason(), UUID.fromString(jwt.getSubject())));
        return PurchaseResponse.from(listPurchases.get(id));
    }

    /** Esquema {@code VoidRequest}. */
    record VoidRequest(
            @NotBlank(message = "El motivo es obligatorio.")
            @Size(min = Purchase.VOID_REASON_MIN_LENGTH, max = Purchase.VOID_REASON_MAX_LENGTH,
                    message = "Debe tener entre {min} y {max} caracteres.")
            String reason) {}

    /** Fechas de compra inclusivas; las dos son opcionales. */
    record PurchaseParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Min(value = 0, message = "Debe ser 0 o mayor.")
            Integer page,
            @Min(value = 1, message = "Debe estar entre 1 y " + PageQuery.MAX_SIZE + ".")
            @Max(value = PageQuery.MAX_SIZE, message = "Debe estar entre 1 y " + PageQuery.MAX_SIZE + ".")
            Integer size) {

        @AssertTrue(message = "La fecha inicial no puede ser posterior a la final.")
        public boolean isRange() {
            return from == null || to == null || !from.isAfter(to);
        }
    }

    /** Esquema {@code PurchaseRequest}. Cantidades y costos viajan como texto para no perder precisión. */
    record PurchaseRequest(
            @NotNull(message = "El proveedor es obligatorio.")
            UUID supplierId,
            @Size(max = Purchase.SUPPLIER_DOCUMENT_MAX_LENGTH, message = "Debe tener como máximo {max} caracteres.")
            String supplierDocument,
            @NotNull(message = "La fecha de compra es obligatoria.")
            LocalDate purchaseDate,
            @NotEmpty(message = "La compra debe tener al menos un producto.")
            List<@Valid @NotNull(message = "El ítem no puede estar vacío.") PurchaseItemRequest> items) {

        /** RNF-04: un producto repetido rechaza la compra entera antes de tocar el stock. */
        @AssertTrue(message = "Un producto no puede repetirse en la misma compra.")
        public boolean isItemsWithoutRepeatedProducts() {
            if (items == null) {
                return true;
            }
            List<UUID> ids = items.stream().filter(Objects::nonNull).map(PurchaseItemRequest::productId)
                    .filter(Objects::nonNull).toList();
            return ids.stream().distinct().count() == ids.size();
        }

        RegisterPurchaseCommand toCommand(UUID userId) {
            return new RegisterPurchaseCommand(supplierId, supplierDocument, purchaseDate,
                    items.stream().map(PurchaseItemRequest::toCommand).toList(), userId);
        }
    }

    /** Esquema {@code PurchaseItemRequest}. */
    record PurchaseItemRequest(
            @NotNull(message = "El producto es obligatorio.")
            UUID productId,
            @NotNull(message = "La cantidad es obligatoria.")
            @Pattern(regexp = QUANTITY, message = "Cantidad con hasta 3 decimales, por ejemplo 2.5.")
            String quantity,
            @NotNull(message = "El costo unitario es obligatorio.")
            @Pattern(regexp = AMOUNT, message = "Monto con hasta 2 decimales, por ejemplo 3.50.")
            String unitCost) {

        @AssertTrue(message = "La cantidad debe ser mayor que 0.")
        public boolean isQuantityPositive() {
            return quantity == null || !quantity.matches(QUANTITY) || new BigDecimal(quantity).signum() > 0;
        }

        ItemCommand toCommand() {
            return new ItemCommand(productId, Quantity.of(quantity), Money.of(unitCost));
        }
    }

    /** Esquema {@code Purchase}. */
    record PurchaseResponse(
            UUID id,
            SupplierResponse supplier,
            String supplierDocument,
            LocalDate purchaseDate,
            String status,
            String voidReason,
            String total,
            List<ItemResponse> items) {

        static PurchaseResponse from(PurchaseView view) {
            return new PurchaseResponse(view.id(), SupplierResponse.from(view.supplier()), view.supplierDocument(),
                    view.purchaseDate(), view.status().name(), view.voidReason(), view.total().toString(),
                    view.items().stream().map(ItemResponse::from).toList());
        }
    }

    record ItemResponse(UUID productId, String productName, String quantity, String unitCost, String subtotal) {

        static ItemResponse from(PurchaseView.Item item) {
            return new ItemResponse(item.productId(), item.productName(), item.quantity().toString(),
                    item.unitCost().toString(), item.subtotal().toString());
        }
    }

    /** Esquema {@code PurchasePage}: {@code PageMeta} más el contenido. */
    record PurchasePageResponse(List<PurchaseResponse> content, int page, int size, long totalElements, int totalPages) {

        static PurchasePageResponse from(ResultPage<PurchaseView> page) {
            return new PurchasePageResponse(page.content().stream().map(PurchaseResponse::from).toList(),
                    page.page(), page.size(), page.totalElements(), page.totalPages());
        }
    }
}
