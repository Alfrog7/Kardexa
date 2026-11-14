package com.kardexa.reporting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.Role;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** HU-17 · total vendido por día y resumen del rango contra PostgreSQL real (ADR 0012). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API del inicio: ventas por día (HU-17)")
class DashboardApiIntegrationTest {

    private static final String URL = "/api/v1/dashboard";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    private int number;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE sale_item, sale, purchase_item, purchase, supplier, stock_movement, stock_adjustment,"
                + " product_stock, product_image, product, category");
        TestSecurity.insertAdmin(jdbc);
        number = 0;
    }

    @Test
    @DisplayName("criterio 1: suma por día de Lima, incluye los días sin ventas y no cuenta las anuladas")
    void salesByDay() throws Exception {
        // 2026-11-01 10:00 y 23:30 en Lima (UTC-5): la segunda ya es 2 de noviembre en UTC.
        sale("2026-11-01T15:00:00Z", "10.00", false);
        sale("2026-11-02T04:30:00Z", "5.50", false);
        // 2026-11-02 00:10 en Lima.
        sale("2026-11-02T05:10:00Z", "7.25", false);
        sale("2026-11-02T06:00:00Z", "100.00", true);

        mvc.perform(get(URL).param("from", "2026-11-01").param("to", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-11-01"))
                .andExpect(jsonPath("$.to").value("2026-11-03"))
                .andExpect(jsonPath("$.salesByDay.length()").value(3))
                .andExpect(jsonPath("$.salesByDay[0].date").value("2026-11-01"))
                .andExpect(jsonPath("$.salesByDay[0].total").value("15.50"))
                .andExpect(jsonPath("$.salesByDay[0].count").value(2))
                .andExpect(jsonPath("$.salesByDay[1].total").value("7.25"))
                .andExpect(jsonPath("$.salesByDay[1].count").value(1))
                .andExpect(jsonPath("$.salesByDay[2].date").value("2026-11-03"))
                .andExpect(jsonPath("$.salesByDay[2].total").value("0.00"))
                .andExpect(jsonPath("$.salesByDay[2].count").value(0))
                .andExpect(jsonPath("$.summary.total").value("22.75"))
                .andExpect(jsonPath("$.summary.count").value(3))
                .andExpect(jsonPath("$.summary.averageTicket").value("7.58"));
    }

    @Test
    @DisplayName("sin fechas devuelve los últimos 30 días")
    void defaultPeriod() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesByDay.length()").value(30))
                .andExpect(jsonPath("$.summary.total").value("0.00"))
                .andExpect(jsonPath("$.summary.averageTicket").value("0.00"));
    }

    @Test
    @DisplayName("un rango invertido o de más de 366 días responde 400")
    void rejectsInvalidPeriods() throws Exception {
        mvc.perform(get(URL).param("from", "2026-11-05").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("from", "2025-01-01").param("to", "2026-01-02"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("solo ADMIN: el EMPLEADO recibe 403 y un token inválido 401")
    void onlyAdmin() throws Exception {
        mvc.perform(get(URL).header("Authorization", "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE)))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL).header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    private void sale(String soldAt, String total, boolean voided) {
        number++;
        Timestamp at = Timestamp.from(Instant.parse(soldAt));
        jdbc.update("""
                INSERT INTO sale (id, number, status, total, seller_id, sold_at, void_reason, voided_at, voided_by)
                VALUES (?, ?, ?, ?::numeric, ?, ?, ?, ?, ?)""",
                UUID.randomUUID(), "V-%06d".formatted(number), voided ? "VOIDED" : "REGISTERED", total,
                TestSecurity.ADMIN_ID, at, voided ? "Error de digitación" : null, voided ? at : null,
                voided ? TestSecurity.ADMIN_ID : null);
    }
}
