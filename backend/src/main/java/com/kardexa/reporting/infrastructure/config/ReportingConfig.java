package com.kardexa.reporting.infrastructure.config;

import com.kardexa.reporting.application.port.out.MovementFigures;
import com.kardexa.reporting.application.port.out.SaleRows;
import com.kardexa.reporting.application.port.out.SalesFigures;
import com.kardexa.reporting.application.port.out.StockFigures;
import com.kardexa.reporting.application.service.DashboardService;
import com.kardexa.reporting.application.service.MovementExportService;
import com.kardexa.reporting.application.service.SaleExportService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de reportes como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class ReportingConfig {

    @Bean
    DashboardService dashboardService(SalesFigures sales, StockFigures stock) {
        return new DashboardService(sales, stock, Clock.systemUTC());
    }

    @Bean
    MovementExportService movementExportService(MovementFigures movements) {
        return new MovementExportService(movements, Clock.systemUTC());
    }

    @Bean
    SaleExportService saleExportService(SaleRows rows) {
        return new SaleExportService(rows, Clock.systemUTC());
    }
}
