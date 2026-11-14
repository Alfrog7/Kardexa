package com.kardexa.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.Role;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** HU-18 · CSV del historial de ventas contra PostgreSQL real (ADR 0012). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("Exportar ventas a CSV (HU-18)")
class SaleExportApiIntegrationTest {

    private static final String URL = "/api/v1/sales/export";
    private static final String HEADER = "﻿Número;Fecha y hora;Vendedor;Estado;Productos;Total;Motivo de anulación";
    private static final UUID SELLER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e5");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE sale_item, sale, purchase_item, purchase, supplier, stock_movement, stock_adjustment,"
                + " product_stock, product_image, product, category");
        TestSecurity.insertAdmin(jdbc);
        jdbc.update("""
                INSERT INTO app_user (id, full_name, email, password_hash, role)
                VALUES (?, 'Luis Rojas', 'luis@pruebas.test', 'x', 'EMPLOYEE') ON CONFLICT DO NOTHING""", SELLER_ID);
        UUID category = UUID.randomUUID();
        jdbc.update("INSERT INTO category (id, name) VALUES (?, 'Abarrotes')", category);
        UUID rice = product(category, "ARR-1", "Arroz");
        UUID oil = product(category, "ACE-1", "Aceite");

        // 2026-11-01 10:00 y 2026-11-02 18:00 en Lima; la tercera, fuera del rango.
        UUID first = sale("V-000001", "2026-11-01T15:00:00Z", TestSecurity.ADMIN_ID, "59.90", null);
        item(first, 1, rice, "2", "25.00");
        item(first, 2, oil, "1", "9.90");
        UUID second = sale("V-000002", "2026-11-02T23:00:00Z", SELLER_ID, "9.90", "=Error; cliente");
        item(second, 1, oil, "1", "9.90");
        item(sale("V-000003", "2026-12-01T15:00:00Z", TestSecurity.ADMIN_ID, "25.00", null), 1, rice, "1", "25.00");
    }

    @Test
    @DisplayName("ventas del rango, de la más reciente a la más antigua, con estado, productos, total y motivo")
    void sales() throws Exception {
        MvcResult result = download(get(URL).param("from", "2026-11-01").param("to", "2026-11-30"));

        assertThat(result.getResponse().getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"ventas-2026-11-01_2026-11-30.csv\"");
        assertThat(body(result).split("\r\n")).containsExactly(HEADER,
                "V-000002;2026-11-02 18:00;Luis Rojas;Anulada;1;9.90;\"'=Error; cliente\"",
                "V-000001;2026-11-01 10:00;Administrador de pruebas;Registrada;2;59.90;");
    }

    @Test
    @DisplayName("respeta el vendedor y el estado")
    void filters() throws Exception {
        String[] bySeller = body(download(get(URL).param("from", "2026-11-01").param("to", "2026-11-30")
                .param("sellerId", SELLER_ID.toString()))).split("\r\n");
        assertThat(bySeller).hasSize(2);
        assertThat(bySeller[1]).startsWith("V-000002;");
        String[] registered = body(download(get(URL).param("from", "2026-11-01").param("to", "2026-11-30")
                .param("status", "REGISTERED"))).split("\r\n");
        assertThat(registered).hasSize(2);
        assertThat(registered[1]).startsWith("V-000001;");
    }

    @Test
    @DisplayName("rango de más de 366 días o estado desconocido 400; el EMPLEADO 403")
    void errors() throws Exception {
        mvc.perform(get(URL).param("from", "2025-01-01").param("to", "2026-01-02")).andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("status", "BORRADA")).andExpect(status().isBadRequest());
        mvc.perform(get(URL).header("Authorization", "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    private MvcResult download(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult started = mvc.perform(request).andExpect(request().asyncStarted()).andReturn();
        return mvc.perform(asyncDispatch(started)).andExpect(status().isOk()).andReturn();
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private UUID product(UUID category, String code, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (id, code, name, category_id, unit, sale_price, min_stock)
                VALUES (?, ?, ?, ?, 'UNIT', 1, 1)""", id, code, name, category);
        return id;
    }

    private UUID sale(String number, String soldAt, UUID seller, String total, String voidReason) {
        UUID id = UUID.randomUUID();
        Timestamp at = Timestamp.from(Instant.parse(soldAt));
        boolean voided = voidReason != null;
        jdbc.update("""
                INSERT INTO sale (id, number, status, total, seller_id, sold_at, void_reason, voided_at, voided_by)
                VALUES (?, ?, ?, ?::numeric, ?, ?, ?, ?, ?)""",
                id, number, voided ? "VOIDED" : "REGISTERED", total, seller, at, voidReason, voided ? at : null,
                voided ? TestSecurity.ADMIN_ID : null);
        return id;
    }

    private void item(UUID sale, int line, UUID product, String quantity, String price) {
        jdbc.update("""
                INSERT INTO sale_item (sale_id, line_no, product_id, product_code, product_name, quantity, unit_price,
                                       subtotal)
                SELECT ?, ?, p.id, p.code, p.name, ?::numeric, ?::numeric, ?::numeric * ?::numeric
                FROM product p WHERE p.id = ?""", sale, line, quantity, price, quantity, price, product);
    }
}
