package com.kardexa.reporting.application.service;

import com.kardexa.reporting.application.port.in.GetDashboardUseCase;
import com.kardexa.reporting.application.port.out.SalesFigures;
import com.kardexa.reporting.application.port.out.StockFigures;
import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SalesSummary;
import com.kardexa.reporting.domain.model.TopProduct;
import com.kardexa.shared.domain.BusinessCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Arma el inicio del panel con las cifras del rango pedido (ADR 0012). */
public class DashboardService implements GetDashboardUseCase {

    private final SalesFigures sales;
    private final StockFigures stock;
    private final Clock clock;

    public DashboardService(SalesFigures sales, StockFigures stock, Clock clock) {
        this.sales = Objects.requireNonNull(sales);
        this.stock = Objects.requireNonNull(stock);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Dashboard get(LocalDate from, LocalDate to) {
        ReportPeriod period = ReportPeriod.resolve(from, to, LocalDate.now(clock.withZone(BusinessCalendar.ZONE)));
        List<DailySales> days = sales.dailySales(period);
        return new Dashboard(period, SalesSummary.of(days), days, sales.topProducts(period, TopProduct.LIMIT),
                stock.criticalStock());
    }
}
