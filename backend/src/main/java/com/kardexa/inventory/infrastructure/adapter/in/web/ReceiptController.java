package com.kardexa.inventory.infrastructure.adapter.in.web;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RegisterReceiptUseCase;
import com.kardexa.inventory.application.port.in.RegisterReceiptUseCase.Receipt;
import com.kardexa.inventory.application.port.in.RegisterReceiptUseCase.RegisterReceiptCommand;
import com.kardexa.shared.domain.Quantity;
import com.kardexa.shared.infrastructure.web.SiteScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/receipts} según el contrato {@code docs/api/openapi.yaml} (HU-21). Cualquier usuario con sesión. */
@RestController
@RequestMapping("/receipts")
class ReceiptController {

    private final RegisterReceiptUseCase registerReceipt;
    private final SiteScope siteScope;

    ReceiptController(RegisterReceiptUseCase registerReceipt, SiteScope siteScope) {
        this.registerReceipt = registerReceipt;
        this.siteScope = siteScope;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReceiptResponse register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReceiptRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<Line> lines = request.items().stream()
                .map(item -> new Line(item.productId(), Quantity.of(item.quantity())))
                .toList();
        Receipt receipt = registerReceipt.register(
                new RegisterReceiptCommand(siteScope.single(jwt, request.siteId()), userId, lines));
        return ReceiptResponse.from(receipt);
    }

    /** Esquema {@code ReceiptRequest}. La cantidad viaja como texto para no perder precisión. */
    record ReceiptRequest(
            UUID siteId,
            @NotEmpty(message = "Indica al menos un producto.") List<@Valid ItemRequest> items) {}

    record ItemRequest(
            @NotNull(message = "El producto es obligatorio.")
            UUID productId,
            @NotNull(message = "La cantidad es obligatoria.")
            @Pattern(regexp = "\\d{1,9}(\\.\\d{1,3})?", message = "Cantidad con hasta 3 decimales, por ejemplo 2.5.")
            String quantity) {}

    /** Esquema {@code Receipt}. */
    record ReceiptResponse(UUID id, UUID siteId, Instant createdAt, List<ItemResponse> items) {

        static ReceiptResponse from(Receipt receipt) {
            List<ItemResponse> items = receipt.lines().stream()
                    .map(line -> new ItemResponse(line.productId(), line.quantity().toString()))
                    .toList();
            return new ReceiptResponse(receipt.id(), receipt.siteId(), receipt.createdAt(), items);
        }
    }

    record ItemResponse(UUID productId, String quantity) {}
}
