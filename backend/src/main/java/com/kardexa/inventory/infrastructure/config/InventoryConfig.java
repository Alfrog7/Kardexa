package com.kardexa.inventory.infrastructure.config;

import com.kardexa.inventory.application.port.in.AdjustStockUseCase;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.inventory.application.port.out.StockMovementQueries;
import com.kardexa.inventory.application.port.out.StockProducts;
import com.kardexa.inventory.application.port.out.StockRecords;
import com.kardexa.inventory.application.port.out.UserNames;
import com.kardexa.inventory.application.service.AdjustStockService;
import com.kardexa.inventory.application.service.KardexService;
import com.kardexa.inventory.application.service.RecordStockMovementsService;
import com.kardexa.inventory.application.service.StockQueryService;
import com.kardexa.inventory.application.service.UserNamesService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Registra los casos de uso de inventario como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class InventoryConfig {

    @Bean
    StockQueryService stockQueryService(ProductStockRepository productStockRepository) {
        return new StockQueryService(productStockRepository);
    }

    @Bean
    UserNamesService userNamesService(UserNames userNames) {
        return new UserNamesService(userNames);
    }

    @Bean
    KardexService kardexService(StockMovementQueries movements, StockProducts products) {
        return new KardexService(movements, products);
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

    /** Solo se llama dentro de la transacción de compras o ventas (ADR 0009). */
    @Bean
    RecordStockMovementsUseCase recordStockMovementsUseCase(
            ProductStockRepository stocks,
            StockRecords records,
            StockProducts products,
            PlatformTransactionManager transactionManager) {
        return new TransactionalRecordStockMovements(
                new RecordStockMovementsService(stocks, records, products, Clock.systemUTC()), transactionManager);
    }
}
