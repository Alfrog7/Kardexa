package com.kardexa.inventory.infrastructure.adapter.in.web;

import com.kardexa.inventory.application.port.in.GetKardexUseCase;
import com.kardexa.inventory.domain.model.Kardex;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/products/{id}/kardex} según el contrato {@code docs/api/openapi.yaml} (HU-08). */
@RestController
class KardexController {

    private final GetKardexUseCase getKardex;

    KardexController(GetKardexUseCase getKardex) {
        this.getKardex = getKardex;
    }

    @GetMapping("/products/{id}/kardex")
    KardexResponse get(@PathVariable UUID id, @Valid @ParameterObject KardexParams params) {
        return KardexResponse.from(getKardex.get(id, params.from(), params.to()));
    }

    /** Fechas inclusivas en hora de Lima; las dos son opcionales. */
    record KardexParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        @AssertTrue(message = "La fecha inicial no puede ser posterior a la final.")
        public boolean isRange() {
            return from == null || to == null || !from.isAfter(to);
        }
    }

    /** Esquema {@code Kardex}. */
    record KardexResponse(
            UUID productId,
            String productName,
            String openingBalance,
            String closingBalance,
            List<StockMovementResponse> movements) {

        static KardexResponse from(Kardex kardex) {
            return new KardexResponse(kardex.productId(), kardex.productName(), kardex.openingBalance().toString(),
                    kardex.closingBalance().toString(),
                    kardex.movements().stream().map(StockMovementResponse::from).toList());
        }
    }
}
