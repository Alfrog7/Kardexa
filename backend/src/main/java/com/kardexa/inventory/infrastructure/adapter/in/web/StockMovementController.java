package com.kardexa.inventory.infrastructure.adapter.in.web;

import com.kardexa.inventory.application.port.in.ListStockMovementsUseCase;
import com.kardexa.inventory.application.port.in.ListStockMovementsUseCase.MovementEntry;
import com.kardexa.inventory.application.port.in.ResolveUserNamesUseCase;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/stock-movements}: historial general del inventario, paginado. */
@RestController
class StockMovementController {

    private final ListStockMovementsUseCase listMovements;
    private final ResolveUserNamesUseCase userNames;

    StockMovementController(ListStockMovementsUseCase listMovements, ResolveUserNamesUseCase userNames) {
        this.listMovements = listMovements;
        this.userNames = userNames;
    }

    @GetMapping("/stock-movements")
    MovementPageResponse list(@Valid @ParameterObject HistoryParams params) {
        ResultPage<MovementEntry> page = listMovements.list(params.from(), params.to(), params.type(),
                PageQuery.of(params.page(), params.size()));
        Map<UUID, String> names = userNames.namesOf(page.content().stream().map(e -> e.movement().userId()).toList());
        return new MovementPageResponse(
                page.content().stream().map(e -> MovementEntryResponse.from(e, names)).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    record HistoryParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            MovementType type,
            @Min(value = 0, message = "Debe ser 0 o mayor.") Integer page,
            @Min(value = 1, message = "Debe ser al menos 1.") @Max(value = PageQuery.MAX_SIZE, message = "Debe ser como máximo {value}.") Integer size) {

        @AssertTrue(message = "La fecha inicial no puede ser posterior a la final.")
        public boolean isRange() {
            return from == null || to == null || !from.isAfter(to);
        }
    }

    /** Movimiento del historial: el de siempre más el producto al que pertenece. */
    record MovementEntryResponse(UUID productId, String productCode, String productName, StockMovementResponse movement) {

        static MovementEntryResponse from(MovementEntry entry, Map<UUID, String> names) {
            return new MovementEntryResponse(entry.movement().productId(), entry.productCode(), entry.productName(),
                    StockMovementResponse.from(entry.movement(), names));
        }
    }

    record MovementPageResponse(List<MovementEntryResponse> content, int page, int size, long totalElements, int totalPages) {}
}
