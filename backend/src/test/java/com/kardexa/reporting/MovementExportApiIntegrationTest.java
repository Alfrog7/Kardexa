package com.kardexa.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

/** HU-18 · CSV del kárdex y del historial de movimientos contra PostgreSQL real (ADR 0012). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("Exportar movimientos a CSV (HU-18)")
class MovementExportApiIntegrationTest {

    private static final String BOM = "﻿";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    private UUID rice;
    private UUID sugar;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE sale_item, sale, purchase_item, purchase, supplier, stock_movement, stock_adjustment,"
                + " product_stock, product_image, product, category");
        TestSecurity.insertAdmin(jdbc);
        UUID category = UUID.randomUUID();
        jdbc.update("INSERT INTO category (id, name) VALUES (?, 'Abarrotes')", category);
        rice = product(category, "P-0001", "=Arroz; extra");
        sugar = product(category, "AZU-1", "Azúcar");

        UUID supplier = UUID.randomUUID();
        jdbc.update("INSERT INTO supplier (id, ruc, business_name) VALUES (?, '20123456789', 'Distribuidora Lima')",
                supplier);
        UUID purchase = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO purchase (id, supplier_id, supplier_document, purchase_date, status, total, user_id, created_at)
                VALUES (?, ?, 'F001-25', DATE '2026-11-01', 'REGISTERED', 35, ?, now())""",
                purchase, supplier, TestSecurity.ADMIN_ID);
        jdbc.update("""
                INSERT INTO purchase_item (purchase_id, line_no, product_id, quantity, unit_cost, subtotal)
                VALUES (?, 1, ?, 10, 3.50, 35)""", purchase, rice);
        UUID sale = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sale (id, number, status, total, seller_id, sold_at)
                VALUES (?, 'V-000001', 'REGISTERED', 9, ?, now())""", sale, TestSecurity.ADMIN_ID);

        // 2026-11-01 10:00 y 23:30 en Lima; 2026-11-03 10:00; y uno fuera del rango.
        movement(rice, "PURCHASE", "IN", "10", "10", "PURCHASE", purchase, TestSecurity.ADMIN_ID, "2026-11-01T15:00:00Z");
        movement(rice, "SALE", "OUT", "2", "8", "SALE", sale, TestSecurity.ADMIN_ID, "2026-11-02T04:30:00Z");
        movement(sugar, "ADJUSTMENT", "IN", "5", "5", "ADJUSTMENT", UUID.randomUUID(), null, "2026-11-03T15:00:00Z");
        movement(rice, "ADJUSTMENT", "OUT", "1", "7", "ADJUSTMENT", UUID.randomUUID(), null, "2026-12-01T15:00:00Z");
    }

    @Test
    @DisplayName("kárdex: BOM, columnas de la pantalla, documento, costo y nombre de archivo con el código")
    void kardex() throws Exception {
        MvcResult result = download(get("/api/v1/products/" + rice + "/kardex/export")
                .param("from", "2026-11-01").param("to", "2026-11-30"));

        assertThat(result.getResponse().getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"kardex-P-0001-2026-11-01_2026-11-30.csv\"");
        assertThat(body(result)).isEqualTo(BOM
                + "Fecha y hora;Tipo;Documento;Entrada;Salida;Saldo;Costo;Usuario\r\n"
                + "2026-11-01 10:00;Compra;F001-25;10.000;;10.000;3.50;Administrador de pruebas\r\n"
                + "2026-11-01 23:30;Venta;V-000001;;2.000;8.000;;Administrador de pruebas\r\n");
    }

    @Test
    @DisplayName("historial: todos los productos del más reciente al más antiguo, fórmulas neutralizadas y filtro por tipo")
    void movements() throws Exception {
        String all = body(download(get("/api/v1/stock-movements/export")
                .param("from", "2026-11-01").param("to", "2026-11-30")));

        assertThat(all.split("\r\n")).containsExactly(
                BOM + "Fecha y hora;Código;Producto;Tipo;Documento;Entrada;Salida;Saldo;Costo;Usuario",
                "2026-11-03 10:00;AZU-1;Azúcar;Ajuste;Ajuste;5.000;;5.000;;Sistema",
                "2026-11-01 23:30;P-0001;\"'=Arroz; extra\";Venta;V-000001;;2.000;8.000;;Administrador de pruebas",
                "2026-11-01 10:00;P-0001;\"'=Arroz; extra\";Compra;F001-25;10.000;;10.000;3.50;Administrador de pruebas");

        MvcResult sales = download(get("/api/v1/stock-movements/export")
                .param("from", "2026-11-01").param("to", "2026-11-30").param("type", "SALE"));
        assertThat(body(sales).split("\r\n")).hasSize(2);
        assertThat(sales.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"movimientos-2026-11-01_2026-11-30.csv\"");
    }

    @Test
    @DisplayName("producto inexistente 404; rango de más de 366 días o tipo desconocido 400")
    void errors() throws Exception {
        mvc.perform(get("/api/v1/products/" + UUID.randomUUID() + "/kardex/export")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/stock-movements/export").param("from", "2025-01-01").param("to", "2026-01-02"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/stock-movements/export").param("type", "ROBO")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("solo ADMIN: el EMPLEADO recibe 403")
    void onlyAdmin() throws Exception {
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE);
        mvc.perform(get("/api/v1/stock-movements/export").header("Authorization", employee))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/products/" + rice + "/kardex/export").header("Authorization", employee))
                .andExpect(status().isForbidden());
    }

    private MvcResult download(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult started = mvc.perform(request).andExpect(request().asyncStarted()).andReturn();
        return mvc.perform(asyncDispatch(started)).andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition")).andReturn();
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

    private void movement(UUID product, String type, String direction, String quantity, String balance,
            String sourceType, UUID sourceId, UUID userId, String at) {
        jdbc.update("""
                INSERT INTO stock_movement (id, product_id, type, direction, quantity, balance_after, source_type,
                                            source_id, user_id, occurred_at)
                VALUES (?, ?, ?, ?, ?::numeric, ?::numeric, ?, ?, ?, ?)""",
                UUID.randomUUID(), product, type, direction, quantity, balance, sourceType, sourceId, userId,
                Timestamp.from(Instant.parse(at)));
    }
}
