package com.kardexa.catalog.infrastructure.config;

import com.kardexa.catalog.application.port.out.CategoryRepository;
import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.application.port.out.StockLevels;
import com.kardexa.catalog.application.service.CategoryService;
import com.kardexa.catalog.application.service.ProductService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            ProductRepository productRepository, CategoryRepository categoryRepository, StockLevels stockLevels) {
        return new ProductService(productRepository, categoryRepository, stockLevels);
    }
}
