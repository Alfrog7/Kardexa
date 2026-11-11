package com.kardexa.purchasing.infrastructure.config;

import com.kardexa.purchasing.application.port.out.SupplierRepository;
import com.kardexa.purchasing.application.service.SupplierService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de compras como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class PurchasingConfig {

    @Bean
    SupplierService supplierService(SupplierRepository supplierRepository) {
        return new SupplierService(supplierRepository);
    }
}
