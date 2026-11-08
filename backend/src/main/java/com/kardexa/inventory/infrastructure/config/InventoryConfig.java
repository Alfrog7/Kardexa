package com.kardexa.inventory.infrastructure.config;

import com.kardexa.inventory.application.port.in.AdjustStockUseCase;
import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.application.port.out.StockProducts;
import com.kardexa.inventory.application.port.out.StockRecords;
import com.kardexa.inventory.application.service.AdjustStockService;
import com.kardexa.inventory.application.service.StockQueryService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Registra los casos de uso de inventario como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class InventoryConfig {

    @Bean
    StockQueryService stockQueryService(ProductStockRepository productStockRepository) {
        return new StockQueryService(productStockRepository);
    }

    /** Solo el decorador es un bean: nadie puede llamar al servicio fuera de su transacción. */
    @Bean
    AdjustStockUseCase adjustStockUseCase(
            ProductStockRepository stocks,
            StockRecords records,
            StockProducts products,
            TransactionTemplate transactionTemplate) {
        return new RetryingAdjustStock(
                new AdjustStockService(stocks, records, products, Clock.systemUTC()), transactionTemplate);
    }
}
