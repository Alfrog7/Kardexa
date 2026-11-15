package com.kardexa.sites.infrastructure.config;

import com.kardexa.sites.application.port.out.ShiftLedger;
import com.kardexa.sites.application.port.out.ShiftRepository;
import com.kardexa.sites.application.port.out.ShiftSales;
import com.kardexa.sites.application.port.out.ShiftStock;
import com.kardexa.sites.application.port.out.SiteRepository;
import com.kardexa.sites.application.service.ShiftService;
import com.kardexa.sites.application.service.SiteService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Registra los casos de uso de sedes y turnos como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class SitesConfig {

    @Bean
    SiteService siteService(SiteRepository siteRepository) {
        return new SiteService(siteRepository);
    }

    /** Solo el decorador es un bean: nadie puede abrir ni cerrar un turno fuera de su transacción. */
    @Bean
    TransactionalShifts shiftUseCases(ShiftRepository shifts, ShiftStock stock, ShiftSales sales, ShiftLedger ledger,
            TransactionTemplate transactionTemplate) {
        ShiftService service = new ShiftService(shifts, stock, sales, ledger, Clock.systemUTC());
        return new TransactionalShifts(service, service, service, transactionTemplate);
    }
}
