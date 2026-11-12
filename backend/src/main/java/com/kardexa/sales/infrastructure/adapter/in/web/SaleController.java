package com.kardexa.sales.infrastructure.adapter.in.web;

import com.kardexa.sales.application.port.in.ListSalesUseCase;
import com.kardexa.sales.application.port.in.ListSalesUseCase.SaleFilter;
import com.kardexa.sales.application.port.in.ListSalesUseCase.Viewer;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase.ItemCommand;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase.RegisterSaleCommand;
import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.domain.model.Sale;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Quantity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
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

    /** HU-16: el EMPLEADO solo recibe sus ventas, aunque envíe otro {@code sellerId}. */
    @GetMapping
    SalePageResponse list(@AuthenticationPrincipal Jwt jwt, @Valid @ParameterObject SaleParams params) {
        return SalePageResponse.from(listSales.list(new SaleFilter(params.from(), params.to(), params.sellerId()),
                viewer(jwt), PageQuery.of(params.page(), params.size())));
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

    /** Fechas de venta inclusivas en hora de Lima; todo es opcional. */
    record SaleParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            UUID sellerId,
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

    /** Esquema {@code SalePage}: {@code PageMeta} más el contenido. */
    record SalePageResponse(List<SaleResponse> content, int page, int size, long totalElements, int totalPages) {

        static SalePageResponse from(ResultPage<SaleView> page) {
            return new SalePageResponse(page.content().stream().map(SaleResponse::from).toList(),
                    page.page(), page.size(), page.totalElements(), page.totalPages());
        }
    }
}
