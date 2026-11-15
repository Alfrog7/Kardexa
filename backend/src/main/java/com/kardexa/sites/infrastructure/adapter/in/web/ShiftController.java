package com.kardexa.sites.infrastructure.adapter.in.web;

import com.kardexa.shared.domain.Quantity;
import com.kardexa.shared.infrastructure.web.SiteScope;
import com.kardexa.sites.application.port.in.CloseShiftUseCase;
import com.kardexa.sites.application.port.in.CloseShiftUseCase.CloseShiftCommand;
import com.kardexa.sites.application.port.in.CloseShiftUseCase.Count;
import com.kardexa.sites.application.port.in.GetCurrentShiftUseCase;
import com.kardexa.sites.application.port.in.OpenShiftUseCase;
import com.kardexa.sites.domain.model.Payments;
import com.kardexa.sites.domain.model.Shift;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/shifts} según el contrato {@code docs/api/openapi.yaml} (HU-22). Cualquier usuario con sesión: abre
 * en su sede quien lleva la caja y cierra el responsable o el {@code ADMIN}.
 */
@RestController
@RequestMapping("/shifts")
class ShiftController {

    private final OpenShiftUseCase openShift;
    private final GetCurrentShiftUseCase currentShift;
    private final CloseShiftUseCase closeShift;
    private final SiteScope siteScope;

    ShiftController(OpenShiftUseCase openShift, GetCurrentShiftUseCase currentShift, CloseShiftUseCase closeShift,
            SiteScope siteScope) {
        this.openShift = openShift;
        this.currentShift = currentShift;
        this.closeShift = closeShift;
        this.siteScope = siteScope;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShiftResponse open(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) OpenShiftRequest request) {
        UUID siteId = siteScope.single(jwt, request == null ? null : request.siteId());
        return ShiftResponse.from(openShift.open(siteId, UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/current")
    ShiftResponse current(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID siteId) {
        return ShiftResponse.from(currentShift.current(siteScope.single(jwt, siteId)));
    }

    @PostMapping("/{id}/close")
    ShiftResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody CloseRequest request) {
        List<Count> counts = request.counts().stream()
                .map(count -> new Count(count.productId(), Quantity.of(count.leftover())))
                .toList();
        boolean admin = "ADMIN".equals(jwt.getClaimAsString("role"));
        return ShiftResponse.from(closeShift.close(
                new CloseShiftCommand(id, UUID.fromString(jwt.getSubject()), admin, counts, request.note(),
                request.payments() == null ? null : request.payments().toDomain())));
    }

    /** Esquema {@code OpenShiftRequest}: el ADMIN indica la sede; la trabajadora usa la suya. */
    record OpenShiftRequest(UUID siteId) {}

    /** Esquema {@code CloseShiftRequest}. El sobrante viaja como texto para no perder precisión. */
    record CloseRequest(
            @NotNull(message = "Indica el sobrante de cada producto.") List<@Valid CountRequest> counts,
            @Size(max = Shift.NOTE_MAX_LENGTH, message = "Debe tener como máximo {max} caracteres.") String note,
            @Valid PaymentsRequest payments) {}

    /** Esquema {@code PaymentsRequest}: lo recibido por medio de pago, 0 si no hubo. */
    record PaymentsRequest(
            @NotNull(message = "Indica el efectivo (0 si no hubo).") @DecimalMin(value = "0", message = "No puede ser negativo.")
            @Digits(integer = 20, fraction = 2, message = "Hasta 2 decimales.") BigDecimal cash,
            @NotNull(message = "Indica Yape (0 si no hubo).") @DecimalMin(value = "0", message = "No puede ser negativo.")
            @Digits(integer = 20, fraction = 2, message = "Hasta 2 decimales.") BigDecimal yape,
            @NotNull(message = "Indica Plin (0 si no hubo).") @DecimalMin(value = "0", message = "No puede ser negativo.")
            @Digits(integer = 20, fraction = 2, message = "Hasta 2 decimales.") BigDecimal plin,
            @NotNull(message = "Indica la transferencia (0 si no hubo).") @DecimalMin(value = "0", message = "No puede ser negativo.")
            @Digits(integer = 20, fraction = 2, message = "Hasta 2 decimales.") BigDecimal transfer) {

        Payments toDomain() {
            return new Payments(cash, yape, plin, transfer);
        }
    }

    record CountRequest(
            @NotNull(message = "El producto es obligatorio.") UUID productId,
            @NotNull(message = "El sobrante es obligatorio.")
            @Pattern(regexp = "\\d{1,9}(\\.\\d{1,3})?", message = "Cantidad con hasta 3 decimales, por ejemplo 2.5.")
            String leftover) {}

    /** Esquema {@code Shift}. */
    record ShiftResponse(
            UUID id,
            UUID siteId,
            UUID responsibleId,
            String responsible,
            String status,
            Instant openedAt,
            Instant closedAt,
            BigDecimal registeredSales,
            BigDecimal countedSales,
            BigDecimal expectedAmount,
            String note,
            PaymentsResponse payments,
            BigDecimal receivedAmount,
            BigDecimal difference,
            List<ItemResponse> items) {

        static ShiftResponse from(Shift shift) {
            return new ShiftResponse(shift.id(), shift.siteId(), shift.responsibleId(), shift.responsibleName(),
                    shift.status().name(), shift.openedAt(), shift.closedAt(), shift.registeredSales(),
                    shift.countedSales(), shift.expectedAmount(), shift.note(),
                    shift.payments() == null ? null : PaymentsResponse.from(shift.payments()), shift.receivedAmount(),
                    shift.difference(),
                    shift.items().stream().map(ItemResponse::from).toList());
        }
    }

    record PaymentsResponse(BigDecimal cash, BigDecimal yape, BigDecimal plin, BigDecimal transfer) {

        static PaymentsResponse from(Payments payments) {
            return new PaymentsResponse(payments.cash(), payments.yape(), payments.plin(), payments.transfer());
        }
    }

    record ItemResponse(
            UUID productId,
            String productName,
            String openingStock,
            String leftover,
            String soldByCount,
            BigDecimal unitPrice) {

        static ItemResponse from(Shift.Item item) {
            return new ItemResponse(item.productId(), item.productName(), item.openingStock().toString(),
                    item.leftover() == null ? null : item.leftover().toString(),
                    item.soldByCount() == null ? null : item.soldByCount().toString(), item.unitPrice());
        }
    }
}
