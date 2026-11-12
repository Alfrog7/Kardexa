package com.kardexa.sales.infrastructure.adapter.in.web;

import com.kardexa.sales.application.port.in.ListSalesUseCase;
import com.kardexa.sales.application.port.in.ListSalesUseCase.Viewer;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase.ItemCommand;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase.RegisterSaleCommand;
import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.domain.model.Sale;
import com.kardexa.shared.domain.Quantity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

/** {@code /api/v1/sales} según el contrato {@code docs/api/openapi.yaml} (HU-14). ADMIN y EMPLEADO. */
@RestController
@RequestMapping("/sales")
class SaleController {

    private static final String QUANTITY = "\\d{1,9}(\\.\\d{1,3})?";

    private final RegisterSaleUseCase registerSale;
    private final ListSalesUseCase listSales;

    SaleController(RegisterSaleUseCase registerSale, ListSalesUseCase listSales) {
        this.registerSale = registerSale;
        this.listSales = listSales;
    }

    @PostMapping
    ResponseEntity<SaleResponse> register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SaleRequest request) {
        Sale sale = registerSale.register(request.toCommand(UUID.fromString(jwt.getSubject())));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sale.id())
                .toUri();
        return ResponseEntity.created(location).body(SaleResponse.from(listSales.get(sale.id(), viewer(jwt))));
    }

    /** RF-19: el comprobante. Una venta ajena responde 404 al EMPLEADO. */
    @GetMapping("/{id}")
    SaleResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return SaleResponse.from(listSales.get(id, viewer(jwt)));
    }

    private static Viewer viewer(Jwt jwt) {
        return new Viewer(UUID.fromString(jwt.getSubject()), "ADMIN".equals(jwt.getClaimAsString("role")));
    }

    /** Esquema {@code SaleRequest}. El precio no viaja: lo pone el catálogo (RN-06). */
    record SaleRequest(
            @NotEmpty(message = "La venta debe tener al menos un producto.")
            List<@Valid @NotNull(message = "El ítem no puede estar vacío.") SaleItemRequest> items) {

        /** RNF-04: un producto repetido rechaza la venta entera antes de tocar el stock. */
        @AssertTrue(message = "Un producto no puede repetirse en la misma venta.")
        public boolean isItemsWithoutRepeatedProducts() {
            if (items == null) {
                return true;
            }
            List<UUID> ids = items.stream().filter(Objects::nonNull).map(SaleItemRequest::productId)
                    .filter(Objects::nonNull).toList();
            return ids.stream().distinct().count() == ids.size();
        }

        RegisterSaleCommand toCommand(UUID sellerId) {
            return new RegisterSaleCommand(items.stream().map(SaleItemRequest::toCommand).toList(), sellerId);
        }
    }

    /** Esquema {@code SaleItemRequest}. La cantidad viaja como texto para no perder precisión. */
    record SaleItemRequest(
            @NotNull(message = "El producto es obligatorio.")
            UUID productId,
            @NotNull(message = "La cantidad es obligatoria.")
            @Pattern(regexp = QUANTITY, message = "Cantidad con hasta 3 decimales, por ejemplo 2.5.")
            String quantity) {

        @AssertTrue(message = "La cantidad debe ser mayor que 0.")
        public boolean isQuantityPositive() {
            return quantity == null || !quantity.matches(QUANTITY) || new BigDecimal(quantity).signum() > 0;
        }

        ItemCommand toCommand() {
            return new ItemCommand(productId, Quantity.of(quantity));
        }
    }

    /** Esquema {@code Sale}. */
    record SaleResponse(
            UUID id,
            String number,
            Instant soldAt,
            UUID sellerId,
            String seller,
            String status,
            String voidReason,
            String total,
            List<ItemResponse> items) {

        static SaleResponse from(SaleView view) {
            return new SaleResponse(view.id(), view.number(), view.soldAt(), view.sellerId(), view.sellerName(),
                    view.status().name(), view.voidReason(), view.total().toString(),
                    view.items().stream().map(ItemResponse::from).toList());
        }
    }

    /** Esquema {@code SaleItem}. */
    record ItemResponse(
            UUID productId, String productCode, String productName, String quantity, String unitPrice,
            String subtotal) {

        static ItemResponse from(SaleView.Item item) {
            return new ItemResponse(item.productId(), item.productCode(), item.productName(),
                    item.quantity().toString(), item.unitPrice().toString(), item.subtotal().toString());
        }
    }
}
