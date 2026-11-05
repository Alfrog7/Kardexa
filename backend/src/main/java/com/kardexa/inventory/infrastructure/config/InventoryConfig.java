package com.kardexa.inventory.infrastructure.config;

import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.application.service.StockQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de inventario como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class InventoryConfig {

    @Bean
    StockQueryService stockQueryService(ProductStockRepository productStockRepository) {
        return new StockQueryService(productStockRepository);
    }
}
