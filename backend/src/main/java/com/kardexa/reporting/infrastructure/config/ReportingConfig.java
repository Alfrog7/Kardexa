package com.kardexa.reporting.infrastructure.config;

import com.kardexa.reporting.application.port.out.SalesFigures;
import com.kardexa.reporting.application.service.DashboardService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de reportes como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class ReportingConfig {

    @Bean
    DashboardService dashboardService(SalesFigures sales) {
        return new DashboardService(sales, Clock.systemUTC());
    }
}
