package com.kardexa.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.reporting.application.port.in.GetDashboardUseCase.Dashboard;
import com.kardexa.reporting.application.port.out.SalesFigures;
import com.kardexa.reporting.domain.exception.InvalidReportPeriodException;
import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.TopProduct;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inicio del panel: resumen y ventas por día (HU-17)")
class DashboardServiceTest {

    /** 2026-11-15 02:00 UTC es todavía el 14 de noviembre en Lima (UTC-5). */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T02:00:00Z"), ZoneOffset.UTC);

    private final List<ReportPeriod> asked = new ArrayList<>();
    private List<DailySales> days = List.of();
    private List<TopProduct> top = List.of();
    private final SalesFigures figures = new SalesFigures() {
        @Override
        public List<DailySales> dailySales(ReportPeriod period) {
            asked.add(period);
            return days;
        }

        @Override
        public List<TopProduct> topProducts(ReportPeriod period, int limit) {
            assertThat(limit).isEqualTo(TopProduct.LIMIT);
            return top;
        }
    };
    private final DashboardService service = new DashboardService(figures, CLOCK);

    @Test
    @DisplayName("sin fechas toma los últimos 30 días hasta hoy en hora de Lima")
    void defaultPeriod() {
        Dashboard dashboard = service.get(null, null);

        assertThat(dashboard.period()).isEqualTo(new ReportPeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 11, 14)));
        assertThat(asked).containsExactly(dashboard.period());
    }

    @Test
    @DisplayName("con solo la fecha final, el rango son los 30 días que terminan ese día")
    void onlyTo() {
        assertThat(service.get(null, LocalDate.of(2026, 3, 31)).period())
                .isEqualTo(new ReportPeriod(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 31)));
    }

    @Test
    @DisplayName("el resumen suma los días y calcula el ticket promedio con redondeo half-up")
    void summary() {
        days = List.of(day(1, "10.00", 2), day(2, "0", 0), day(3, "10.01", 1));

        Dashboard dashboard = service.get(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));

        assertThat(dashboard.summary().total()).isEqualTo(Money.of("20.01"));
        assertThat(dashboard.summary().count()).isEqualTo(3);
        assertThat(dashboard.summary().averageTicket()).isEqualTo(Money.of("6.67"));
        assertThat(dashboard.salesByDay()).hasSize(3);
    }

    @Test
    @DisplayName("incluye los más vendidos del rango, pidiendo como máximo 10")
    void topProducts() {
        top = List.of(new TopProduct(UUID.randomUUID(), "A01", "Arroz", Quantity.of("5"), Money.of("20.00")));

        assertThat(service.get(null, null).topProducts()).isEqualTo(top);
    }

    @Test
    @DisplayName("sin ventas, el total y el ticket promedio son 0")
    void noSales() {
        days = List.of(day(1, "0", 0));

        Dashboard dashboard = service.get(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1));

        assertThat(dashboard.summary().count()).isZero();
        assertThat(dashboard.summary().averageTicket()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("rechaza un rango invertido o de más de 366 días")
    void invalidPeriods() {
        assertThatThrownBy(() -> service.get(LocalDate.of(2026, 11, 20), null))
                .isInstanceOf(InvalidReportPeriodException.class);
        assertThatThrownBy(() -> service.get(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)))
                .isInstanceOf(InvalidReportPeriodException.class)
                .hasMessageContaining("366");
        assertThat(service.get(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)).period().to())
                .isEqualTo(LocalDate.of(2025, 12, 31));
    }

    private static DailySales day(int dayOfMonth, String total, long count) {
        return new DailySales(LocalDate.of(2026, 11, dayOfMonth), Money.of(total), count);
    }
}
