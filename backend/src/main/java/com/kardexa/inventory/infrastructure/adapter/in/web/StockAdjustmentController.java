package com.kardexa.inventory.infrastructure.adapter.in.web;

import com.kardexa.inventory.application.port.in.AdjustStockUseCase;
import com.kardexa.inventory.application.port.in.ResolveUserNamesUseCase;
import com.kardexa.inventory.domain.model.StockMovement;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.kardexa.inventory.application.port.in.AdjustStockUseCase.AdjustStockCommand;
import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.shared.domain.Quantity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/stock-adjustments} según el contrato {@code docs/api/openapi.yaml} (HU-09). */
@RestController
@RequestMapping("/stock-adjustments")
class StockAdjustmentController {

    private final AdjustStockUseCase adjustStock;
    private final ResolveUserNamesUseCase userNames;

    StockAdjustmentController(AdjustStockUseCase adjustStock, ResolveUserNamesUseCase userNames) {
        this.adjustStock = adjustStock;
        this.userNames = userNames;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StockMovementResponse adjust(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StockAdjustmentRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        StockMovement movement = adjustStock.adjust(request.toCommand(userId));
        return StockMovementResponse.from(movement, userNames.namesOf(List.of(userId)));
    }

    /** Esquema {@code StockAdjustmentRequest}. La cantidad viaja como texto para no perder precisión. */
    record StockAdjustmentRequest(
            @NotNull(message = "El producto es obligatorio.")
            UUID productId,
            @NotNull(message = "La dirección es obligatoria.")
            @Pattern(regexp = "IN|OUT", message = "Debe ser IN o OUT.")
            String direction,
            @NotNull(message = "La cantidad es obligatoria.")
            @Pattern(regexp = "\\d{1,9}(\\.\\d{1,3})?", message = "Cantidad con hasta 3 decimales, por ejemplo 2.5.")
            String quantity,
            @NotNull(message = "El motivo es obligatorio.")
            @Pattern(regexp = "SHRINKAGE|PHYSICAL_COUNT|CORRECTION",
                    message = "Debe ser SHRINKAGE, PHYSICAL_COUNT o CORRECTION.")
            String reason,
            @Size(max = StockAdjustment.NOTE_MAX_LENGTH, message = "Debe tener como máximo {max} caracteres.")
            String note) {

        AdjustStockCommand toCommand(UUID userId) {
            return new AdjustStockCommand(productId, Direction.valueOf(direction), Quantity.of(quantity),
                    AdjustmentReason.valueOf(reason), note, userId);
        }
    }
}
