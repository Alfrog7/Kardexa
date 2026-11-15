package com.kardexa.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.shared.domain.MainSite;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * HU-19 · Stock y kárdex por sede (RN-13, ADR 0013): dos sedes con el mismo producto, de punta a punta contra
 * PostgreSQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("Stock y kárdex por sede contra PostgreSQL (HU-19)")
class StockPerSiteApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    private String productId;
    private String galeno;
    private String monserrate;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE sale_item, sale, purchase_item, purchase, supplier, stock_movement, stock_adjustment,"
                + " product_stock, product_image, product, category");
        jdbc.update("DELETE FROM site WHERE id <> ?", MainSite.ID);
        TestSecurity.insertAdmin(jdbc);
        String category = idOf(json(post("/api/v1/categories"), "{\"name\": \"Panes\"}"));
        productId = idOf(json(post("/api/v1/products"), """
                {"code": "PAN-1", "name": "Pan francés", "categoryId": "%s", "unit": "UNIT",
                 "salePrice": "0.50", "minStock": "6"}""".formatted(category)));
        galeno = idOf(json(post("/api/v1/sites"), "{\"name\": \"Galeno\"}"));
        monserrate = idOf(json(post("/api/v1/sites"), "{\"name\": \"Monserrate\"}"));
        adjust(galeno, "10");
        adjust(monserrate, "5");
    }

    @Test
    @DisplayName("criterio 1: una venta en una sede no cambia el stock de la otra")
    void saleDoesNotTouchOtherSite() throws Exception {
        sell(galeno, "3").andExpect(status().isCreated()).andExpect(jsonPath("$.siteId").value(galeno));

        assertThat(stockOf(galeno)).isEqualTo("7.000");
        assertThat(stockOf(monserrate)).isEqualTo("5.000");
        // Sin sede, el stock del producto es la suma de todas.
        mvc.perform(get("/api/v1/products/" + productId)).andExpect(jsonPath("$.stock").value("12.000"));
    }

    @Test
    @DisplayName("criterio 1: el stock de una sede no alcanza aunque la otra tenga de sobra")
    void insufficientStockIsPerSite() throws Exception {
        sell(monserrate, "8")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Pan francés")));

        assertThat(stockOf(monserrate)).isEqualTo("5.000");
        assertThat(stockOf(galeno)).isEqualTo("10.000");
    }

    @Test
    @DisplayName("criterio 2: el kárdex de un producto muestra los saldos de una sola sede")
    void kardexIsPerSite() throws Exception {
        sell(galeno, "3").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/products/" + productId + "/kardex").param("siteId", galeno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closingBalance").value("7.000"))
                .andExpect(jsonPath("$.movements.length()").value(2))
                .andExpect(jsonPath("$.movements[0].balanceAfter").value("10.000"))
                .andExpect(jsonPath("$.movements[1].balanceAfter").value("7.000"))
                .andExpect(jsonPath("$.movements[1].siteId").value(galeno));
        mvc.perform(get("/api/v1/products/" + productId + "/kardex").param("siteId", monserrate))
                .andExpect(jsonPath("$.closingBalance").value("5.000"))
                .andExpect(jsonPath("$.movements.length()").value(1));
        // Sin sede, el ADMIN ve el kárdex de la Sede principal, que aquí no tiene movimientos.
        mvc.perform(get("/api/v1/products/" + productId + "/kardex"))
                .andExpect(jsonPath("$.movements.length()").value(0));
    }

    @Test
    @DisplayName("el historial de movimientos filtra por sede y, sin sede, el ADMIN ve todas")
    void movementHistoryFiltersBySite() throws Exception {
        sell(galeno, "3").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/stock-movements").param("siteId", monserrate))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/stock-movements").param("siteId", galeno))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/stock-movements")).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("anular una venta devuelve el stock a la sede donde se vendió")
    void voidingReturnsStockToItsSite() throws Exception {
        String sale = idOf(sell(galeno, "4").andExpect(status().isCreated()));
        assertThat(stockOf(galeno)).isEqualTo("6.000");

        json(post("/api/v1/sales/" + sale + "/void"), "{\"reason\": \"Cliente devolvió el pan\"}")
                .andExpect(status().isOk());

        assertThat(stockOf(galeno)).isEqualTo("10.000");
        assertThat(stockOf(monserrate)).isEqualTo("5.000");
    }

    @Test
    @DisplayName("el historial de ventas y las compras filtran por sede")
    void salesAndPurchasesFilterBySite() throws Exception {
        sell(galeno, "1").andExpect(status().isCreated());
        sell(monserrate, "1").andExpect(status().isCreated());
        String supplier = idOf(json(post("/api/v1/suppliers"), "{\"ruc\": \"20123456789\", \"businessName\": \"Harinera\"}"));
        json(post("/api/v1/purchases"), """
                {"supplierId": "%s", "siteId": "%s", "purchaseDate": "2026-11-10",
                 "items": [{"productId": "%s", "quantity": "20", "unitCost": "0.30"}]}""".formatted(supplier, galeno, productId))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/sales").param("siteId", galeno)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/sales")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/purchases").param("siteId", galeno)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/purchases").param("siteId", monserrate)).andExpect(jsonPath("$.totalElements").value(0));
        assertThat(stockOf(galeno)).isEqualTo("29.000"); // 10 - 1 vendido + 20 comprados
        assertThat(stockOf(monserrate)).isEqualTo("4.000"); // 5 - 1 vendido, sin compras
    }

    @Test
    @DisplayName("el stock crítico se evalúa por sede (RN-09)")
    void criticalStockIsPerSite() throws Exception {
        // Mínimo 6: Monserrate (5) está en crítico; Galeno (10) no.
        mvc.perform(get("/api/v1/products").param("criticalOnly", "true").param("siteId", monserrate))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/products").param("criticalOnly", "true").param("siteId", galeno))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/dashboard").param("siteId", monserrate))
                .andExpect(jsonPath("$.criticalStock.length()").value(1))
                .andExpect(jsonPath("$.summary.criticalCount").value(1));
        mvc.perform(get("/api/v1/dashboard").param("siteId", galeno))
                .andExpect(jsonPath("$.criticalStock.length()").value(0));
    }

    @Test
    @DisplayName("un EMPLOYEE opera en la Sede principal e ignora el siteId que pida (hasta la HU-20)")
    void employeeIgnoresRequestedSite() throws Exception {
        adjustIn(MainSite.ID.toString(), "9");
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE);

        ResultActions sale = mvc.perform(post("/api/v1/sales").header("Authorization", employee)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"siteId\": \"%s\", \"items\": [{\"productId\": \"%s\", \"quantity\": \"2\"}]}"
                        .formatted(galeno, productId)));

        sale.andExpect(status().isCreated()).andExpect(jsonPath("$.siteId").value(MainSite.ID.toString()));
        assertThat(stockOf(MainSite.ID.toString())).isEqualTo("7.000");
        assertThat(stockOf(galeno)).isEqualTo("10.000");
        mvc.perform(get("/api/v1/stock-movements").param("siteId", galeno).header("Authorization", employee))
                .andExpect(jsonPath("$.totalElements").value(2)); // solo ve la suya: la principal
    }

    @Test
    @DisplayName("criterio 4: un EMPLOYEE con sede en su token opera solo en ella")
    void employeeOperatesInItsSite() throws Exception {
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE, java.util.UUID.fromString(monserrate));

        mvc.perform(post("/api/v1/sales").header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"siteId\": \"%s\", \"items\": [{\"productId\": \"%s\", \"quantity\": \"2\"}]}"
                                .formatted(galeno, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siteId").value(monserrate));

        assertThat(stockOf(monserrate)).isEqualTo("3.000");
        assertThat(stockOf(galeno)).isEqualTo("10.000");
    }

    private String stockOf(String siteId) throws Exception {
        String body = mvc.perform(get("/api/v1/products/" + productId).param("siteId", siteId))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.stock");
    }

    private ResultActions sell(String siteId, String quantity) throws Exception {
        return json(post("/api/v1/sales"), """
                {"siteId": "%s", "items": [{"productId": "%s", "quantity": "%s"}]}""".formatted(siteId, productId, quantity));
    }

    private void adjust(String siteId, String quantity) throws Exception {
        adjustIn(siteId, quantity);
    }

    private void adjustIn(String siteId, String quantity) throws Exception {
        json(post("/api/v1/stock-adjustments"), """
                {"productId": "%s", "siteId": "%s", "direction": "IN", "quantity": "%s", "reason": "PHYSICAL_COUNT"}"""
                .formatted(productId, siteId, quantity)).andExpect(status().isCreated());
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
