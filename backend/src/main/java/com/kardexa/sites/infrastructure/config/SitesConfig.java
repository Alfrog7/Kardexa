package com.kardexa.sites.infrastructure.config;

import com.kardexa.sites.application.port.out.SiteRepository;
import com.kardexa.sites.application.service.SiteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de sedes como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class SitesConfig {

    @Bean
    SiteService siteService(SiteRepository siteRepository) {
        return new SiteService(siteRepository);
    }
}
