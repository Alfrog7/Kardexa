package com.kardexa.catalog.infrastructure.config;

import com.kardexa.catalog.application.port.out.ProductImageRepository;
import java.time.Clock;
import com.kardexa.catalog.application.port.in.FindProductsForSaleUseCase;
import com.kardexa.catalog.application.port.in.UpdateReferenceCostUseCase;
import com.kardexa.catalog.application.port.out.CategoryRepository;
import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.application.port.out.StockLevels;
import com.kardexa.catalog.application.service.CategoryService;
import com.kardexa.catalog.application.service.ProductService;
import com.kardexa.catalog.application.service.ProductsForSaleService;
import com.kardexa.catalog.application.service.ReferenceCostService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Registra los casos de uso del catálogo como beans. Los servicios de {@code application} no
 * llevan anotaciones de Spring (lo exige ArchUnit), así que se conectan aquí.
 */
@Configuration(proxyBeanMethods = false)
class CatalogConfig {

    @Bean
    CategoryService categoryService(CategoryRepository categoryRepository) {
        return new CategoryService(categoryRepository);
    }

    @Bean
    ProductService productService(
            ProductRepository productRepository, CategoryRepository categoryRepository, StockLevels stockLevels,
            ProductImageRepository productImageRepository) {
        return new ProductService(productRepository, categoryRepository, stockLevels, productImageRepository,
                Clock.systemUTC());
    }

    /** Solo se llama dentro de la transacción de una compra (ADR 0009). */
    @Bean
    UpdateReferenceCostUseCase updateReferenceCostUseCase(
            ProductRepository productRepository, PlatformTransactionManager transactionManager) {
        return new TransactionalUpdateReferenceCost(new ReferenceCostService(productRepository), transactionManager);
    }

    /** RN-06: lectura de precios vigentes para una venta (ADR 0010). */
    @Bean
    FindProductsForSaleUseCase findProductsForSaleUseCase(ProductRepository productRepository) {
        return new ProductsForSaleService(productRepository);
    }
}
