package com.kardexa.sales.infrastructure.config;

import com.kardexa.sales.application.port.in.RegisterSaleUseCase;
import com.kardexa.sales.application.port.out.SaleNumbers;
import com.kardexa.sales.application.port.out.SaleProducts;
import com.kardexa.sales.application.port.out.SaleRepository;
import com.kardexa.sales.application.port.out.StockLedger;
import com.kardexa.sales.application.service.RegisterSaleService;
import com.kardexa.sales.application.service.SaleQueryService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Registra los casos de uso de ventas como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class SalesConfig {

    @Bean
    SaleQueryService saleQueryService(SaleRepository saleRepository) {
        return new SaleQueryService(saleRepository);
    }

    /** Solo el decorador es un bean: nadie puede registrar una venta fuera de su transacción. */
    @Bean
    RegisterSaleUseCase registerSaleUseCase(
            SaleRepository sales,
            SaleNumbers numbers,
            SaleProducts products,
            StockLedger stockLedger,
            TransactionTemplate transactionTemplate) {
        return new RetryingRegisterSale(
                new RegisterSaleService(sales, numbers, products, stockLedger, Clock.systemUTC()),
                new RetryingTransaction(transactionTemplate));
    }
}
