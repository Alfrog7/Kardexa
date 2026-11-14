package com.kardexa.reporting.application.port.in;

import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SalesSummary;
import java.time.LocalDate;
import java.util.List;

/** Indicadores del inicio del panel (HU-17). */
public interface GetDashboardUseCase {

    /** Las fechas nulas toman el rango por defecto de {@link ReportPeriod#resolve}. */
    Dashboard get(LocalDate from, LocalDate to);

    /** Rango aplicado, resumen y ventas de cada día del rango, incluidos los días sin ventas. */
    record Dashboard(ReportPeriod period, SalesSummary summary, List<DailySales> salesByDay) {

        public Dashboard {
            salesByDay = List.copyOf(salesByDay);
        }
    }
}
