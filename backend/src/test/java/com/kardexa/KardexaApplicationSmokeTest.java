package com.kardexa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Arranca la aplicación completa contra un PostgreSQL real.
 *
 * <p>Solo corre si existe la variable {@code DB_URL}: en la CI la define el workflow (servicio de
 * PostgreSQL) y en local basta con {@code docker compose up -d} y exportar {@code DB_URL}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@DisplayName("Arranque completo contra PostgreSQL")
class KardexaApplicationSmokeTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Flyway flyway;

    @Test
    @DisplayName("Flyway aplica las migraciones")
    void flywayAppliesMigrations() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    @DisplayName("Actuator informa que la aplicación está arriba")
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Swagger publica el contrato de la API")
    void openApiIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Kardexa API"));
    }
}
