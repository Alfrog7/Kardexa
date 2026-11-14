package com.kardexa.reporting.infrastructure.adapter.in.web;

import com.kardexa.reporting.application.port.in.GetDashboardUseCase;
import com.kardexa.reporting.application.port.in.GetDashboardUseCase.Dashboard;
import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SalesSummary;
import com.kardexa.reporting.domain.model.TopProduct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/dashboard}: indicadores del inicio del panel, solo para ADMIN (HU-17, ADR 0012). */
@RestController
class DashboardController {

    private final GetDashboardUseCase dashboard;

    DashboardController(GetDashboardUseCase dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    DashboardResponse get(@Valid @ParameterObject PeriodParams params) {
        return DashboardResponse.from(dashboard.get(params.from(), params.to()));
    }

    record PeriodParams(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        @AssertTrue(message = "La fecha inicial no puede ser posterior a la final.")
        public boolean isRange() {
            return from == null || to == null || !from.isAfter(to);
        }

        @AssertTrue(message = "El rango puede tener como máximo " + ReportPeriod.MAX_DAYS + " días.")
        public boolean isWithinLimit() {
            return from == null || to == null || from.isAfter(to)
                    || ChronoUnit.DAYS.between(from, to) + 1 <= ReportPeriod.MAX_DAYS;
        }
    }

    record DashboardResponse(LocalDate from, LocalDate to, SummaryResponse summary, List<DayResponse> salesByDay,
            List<TopProductResponse> topProducts) {

        static DashboardResponse from(Dashboard dashboard) {
            return new DashboardResponse(dashboard.period().from(), dashboard.period().to(),
                    SummaryResponse.from(dashboard.summary()),
                    dashboard.salesByDay().stream().map(DayResponse::from).toList(),
                    dashboard.topProducts().stream().map(TopProductResponse::from).toList());
        }
    }

    record SummaryResponse(String total, long count, String averageTicket) {

        static SummaryResponse from(SalesSummary summary) {
            return new SummaryResponse(summary.total().toString(), summary.count(), summary.averageTicket().toString());
        }
    }

    record DayResponse(LocalDate date, String total, long count) {

        static DayResponse from(DailySales day) {
            return new DayResponse(day.date(), day.total().toString(), day.count());
        }
    }

    record TopProductResponse(UUID productId, String productCode, String productName, String quantity,
            String amount) {

        static TopProductResponse from(TopProduct product) {
            return new TopProductResponse(product.productId(), product.productCode(), product.productName(),
                    product.quantity().toString(), product.amount().toString());
        }
    }
}
