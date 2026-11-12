package com.kardexa.inventory;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-08 de punta a punta: HTTP → caso de uso → PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API del kárdex contra PostgreSQL (HU-08)")
class KardexApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String productId;
    private String url;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE sale_item, sale, purchase_item, purchase, stock_movement, stock_adjustment, product_stock, product, category");
        TestSecurity.insertAdmin(jdbc);
        String categoryId = idOf(postJson("/api/v1/categories", "{\"name\": \"Abarrotes\"}"));
        productId = idOf(postJson("/api/v1/products", """
                {"code": "AR-001", "name": "Arroz 5 kg", "categoryId": "%s", "unit": "UNIT",
                 "salePrice": "20.00", "minStock": "5"}""".formatted(categoryId)));
        url = "/api/v1/products/" + productId + "/kardex";
    }

    @Test
    @DisplayName("criterio 1: muestra cada movimiento con sus datos, en orden cronológico")
    void showsMovements() throws Exception {
        adjust("IN", "20", "PHYSICAL_COUNT");
        adjust("OUT", "3", "SHRINKAGE");
        adjust("IN", "0.5", "CORRECTION");

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("Arroz 5 kg"))
                .andExpect(jsonPath("$.openingBalance").value("0.000"))
                .andExpect(jsonPath("$.closingBalance").value("17.500"))
                .andExpect(jsonPath("$.movements[*].direction", contains("IN", "OUT", "IN")))
                .andExpect(jsonPath("$.movements[*].quantity", contains("20.000", "3.000", "0.500")))
                .andExpect(jsonPath("$.movements[*].balanceAfter", contains("20.000", "17.000", "17.500")))
                .andExpect(jsonPath("$.movements[1].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.movements[1].sourceLabel").value("Ajuste"))
                .andExpect(jsonPath("$.movements[1].sourceId").isNotEmpty())
                .andExpect(jsonPath("$.movements[1].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.movements[1].user").value("Administrador de pruebas"));
    }

    @Test
    @DisplayName("criterio 2: con rango muestra el saldo inicial y solo los movimientos de esas fechas")
    void filtersByRange() throws Exception {
        movement("IN", "20", "20", "2026-10-30T15:00:00Z");
        movement("OUT", "3", "17", "2026-11-01T15:00:00Z");
        movement("OUT", "2", "15", "2026-11-02T15:00:00Z");
        movement("IN", "5", "20", "2026-11-04T15:00:00Z");

        mvc.perform(get(url).param("from", "2026-11-01").param("to", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value("20.000"))
                .andExpect(jsonPath("$.closingBalance").value("15.000"))
                .andExpect(jsonPath("$.movements[*].balanceAfter", contains("17.000", "15.000")));
        mvc.perform(get(url).param("from", "2026-11-03"))
                .andExpect(jsonPath("$.openingBalance").value("15.000"))
                .andExpect(jsonPath("$.movements[*].balanceAfter", contains("20.000")));
        mvc.perform(get(url).param("to", "2026-10-30"))
                .andExpect(jsonPath("$.openingBalance").value("0.000"))
                .andExpect(jsonPath("$.closingBalance").value("20.000"));
    }

    @Test
    @DisplayName("RNF-15: las fechas del rango son días en Lima, no en UTC")
    void rangeUsesLimaDays() throws Exception {
        movement("IN", "1", "1", "2026-11-08T04:59:59Z"); // aún 7 de noviembre en Lima
        movement("IN", "1", "2", "2026-11-08T05:00:00Z"); // ya 8 de noviembre en Lima

        mvc.perform(get(url).param("from", "2026-11-08").param("to", "2026-11-08"))
                .andExpect(jsonPath("$.openingBalance").value("1.000"))
                .andExpect(jsonPath("$.movements[*].balanceAfter", contains("2.000")));
    }

    @Test
    @DisplayName("criterio 3 · RN-02: cada saldo cuadra y el saldo final coincide con el stock actual")
    void balancesMatchCurrentStock() throws Exception {
        adjust("IN", "10", "PHYSICAL_COUNT");
        adjust("OUT", "2.25", "SHRINKAGE");
        adjust("OUT", "0.75", "CORRECTION");

        mvc.perform(get(url))
                .andExpect(jsonPath("$.movements[*].balanceAfter", contains("10.000", "7.750", "7.000")))
                .andExpect(jsonPath("$.closingBalance").value("7.000"));
        mvc.perform(get("/api/v1/products/" + productId)).andExpect(jsonPath("$.stock").value("7.000"));
    }

    @Test
    @DisplayName("el orden es el de los saldos aunque dos movimientos coincidan en la fecha")
    void ordersBySequenceOnSameInstant() throws Exception {
        String sameInstant = "2026-11-01T15:00:00Z";
        movement("IN", "9", "9", sameInstant);
        movement("OUT", "1", "8", sameInstant);
        movement("OUT", "1", "7", sameInstant);

        mvc.perform(get(url)).andExpect(jsonPath("$.movements[*].balanceAfter", contains("9.000", "8.000", "7.000")));
    }

    @Test
    @DisplayName("criterio 4: un producto sin movimientos devuelve saldo 0 y lista vacía")
    void emptyKardex() throws Exception {
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value("0.000"))
                .andExpect(jsonPath("$.closingBalance").value("0.000"))
                .andExpect(jsonPath("$.movements").isEmpty());
    }

    @Test
    @DisplayName("el kárdex de un producto no incluye movimientos de otros")
    void onlyThisProduct() throws Exception {
        String categoryId = jdbc.queryForObject("SELECT id::text FROM category LIMIT 1", String.class);
        String other = idOf(postJson("/api/v1/products", """
                {"code": "AR-002", "name": "Arroz 1 kg", "categoryId": "%s", "unit": "UNIT",
                 "salePrice": "5.00", "minStock": "5"}""".formatted(categoryId)));
        postJson("/api/v1/stock-adjustments", adjustmentJson(other, "IN", "4", "CORRECTION"));

        mvc.perform(get(url)).andExpect(jsonPath("$.movements").isEmpty());
    }

    @Test
    @DisplayName("criterio 5: producto inexistente 404; fechas inválidas 400")
    void validates() throws Exception {
        mvc.perform(get("/api/v1/products/" + UUID.randomUUID() + "/kardex")).andExpect(status().isNotFound());
        mvc.perform(get(url).param("from", "2026-11-05").param("to", "2026-11-04"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("La fecha inicial no puede ser posterior a la final."));
        mvc.perform(get(url).param("from", "05/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("from"));
        mvc.perform(get(url).param("from", "2026-11-05").param("to", "2026-11-05")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Swagger muestra las dos fechas como parámetros opcionales")
    void documentsParams() throws Exception {
        String parameters = "$.paths['/api/v1/products/{id}/kardex'].get.parameters";

        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(parameters + "[*].name", containsInAnyOrder("id", "from", "to")));
    }

    /** Inserta un movimiento en una fecha concreta, algo que la API no permite: siempre usa la hora actual. */
    private void movement(String direction, String quantity, String balance, String instant) {
        jdbc.update("""
                INSERT INTO stock_movement
                    (id, product_id, type, direction, quantity, balance_after, source_type, source_id, occurred_at)
                VALUES (?::uuid, ?::uuid, 'ADJUSTMENT', ?, ?::numeric, ?::numeric, 'ADJUSTMENT', ?::uuid, ?)""",
                UUID.randomUUID().toString(), productId, direction, quantity, balance, UUID.randomUUID().toString(),
                Timestamp.from(Instant.parse(instant)));
    }

    private void adjust(String direction, String quantity, String reason) throws Exception {
        postJson("/api/v1/stock-adjustments", adjustmentJson(productId, direction, quantity, reason))
                .andExpect(status().isCreated());
    }

    private static String adjustmentJson(String product, String direction, String quantity, String reason) {
        return """
                {"productId": "%s", "direction": "%s", "quantity": "%s", "reason": "%s"}"""
                .formatted(product, direction, quantity, reason);
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
