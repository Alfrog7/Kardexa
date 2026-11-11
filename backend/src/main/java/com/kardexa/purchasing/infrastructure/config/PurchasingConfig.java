package com.kardexa.purchasing.infrastructure.config;

import com.kardexa.purchasing.application.port.in.RegisterPurchaseUseCase;
import com.kardexa.purchasing.application.port.out.PurchaseRepository;
import com.kardexa.purchasing.application.port.out.PurchasedProducts;
import com.kardexa.purchasing.application.port.out.StockLedger;
import com.kardexa.purchasing.application.port.out.SupplierRepository;
import com.kardexa.purchasing.application.service.PurchaseQueryService;
import com.kardexa.purchasing.application.service.RegisterPurchaseService;
import com.kardexa.purchasing.application.service.SupplierService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Registra los casos de uso de compras como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class PurchasingConfig {

    @Bean
    SupplierService supplierService(SupplierRepository supplierRepository) {
        return new SupplierService(supplierRepository);
    }

    @Bean
    PurchaseQueryService purchaseQueryService(PurchaseRepository purchaseRepository) {
        return new PurchaseQueryService(purchaseRepository);
    }

    /** Solo el decorador es un bean: nadie puede registrar una compra fuera de su transacción. */
    @Bean
    RegisterPurchaseUseCase registerPurchaseUseCase(
            SupplierRepository suppliers,
            PurchaseRepository purchases,
            StockLedger stockLedger,
            PurchasedProducts products,
            TransactionTemplate transactionTemplate) {
        return new RetryingRegisterPurchase(
                new RegisterPurchaseService(suppliers, purchases, stockLedger, products, Clock.systemUTC()),
                transactionTemplate);
    }
}
