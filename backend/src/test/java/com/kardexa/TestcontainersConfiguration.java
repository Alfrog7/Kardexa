package com.kardexa;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL real y desechable para los tests de integración, la misma imagen que
 * {@code docker-compose.yml}. {@link ServiceConnection} configura el datasource solo.
 *
 * <p>Spring reutiliza el contexto entre clases de test, así que el contenedor arranca una vez por
 * ejecución. Requiere Docker en marcha.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18.6-alpine");
    }
}
