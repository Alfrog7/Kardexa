package com.kardexa.sites;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** HU-22 · Abrir y cerrar turno con conteo, de punta a punta contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("Turnos contra PostgreSQL (HU-22)")
class ShiftApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    private String productId;
    private String monserrate;
    private String employee;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE shift_item, shift, sale_item, sale, purchase_item, purchase, supplier, stock_movement,"
                + " stock_adjustment, product_stock, product_image, product, category");
        jdbc.update("UPDATE app_user SET site_id = NULL");
        jdbc.update("DELETE FROM site WHERE id <> ?", MainSite.ID);
        TestSecurity.insertAdmin(jdbc);
        String category = idOf(json(post("/api/v1/categories"), "{\"name\": \"Panes\"}", null));
        productId = idOf(json(post("/api/v1/products"), """
                {"code": "PAN-1", "name": "Pan francés", "categoryId": "%s", "unit": "UNIT",
                 "salePrice": "0.50", "minStock": "6"}""".formatted(category), null));
        monserrate = idOf(json(post("/api/v1/sites"), "{\"name\": \"Monserrate\"}", null));
        employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE, UUID.fromString(monserrate));
        json(post("/api/v1/stock-adjustments"), """
                {"productId": "%s", "siteId": "%s", "direction": "IN", "quantity": "10", "reason": "PHYSICAL_COUNT"}"""
                .formatted(productId, monserrate), null).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("abre el turno con la foto del stock y solo permite uno abierto por sede")
    void opensOnlyOnePerSite() throws Exception {
        json(post("/api/v1/shifts"), "{}", employee)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.siteId").value(monserrate))
                .andExpect(jsonPath("$.items[0].openingStock").value("10.000"));

        json(post("/api/v1/shifts"), "{}", employee).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/shifts/current").header("Authorization", employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("sin turno abierto, /shifts/current responde 404")
    void noCurrentShift() throws Exception {
        mvc.perform(get("/api/v1/shifts/current").header("Authorization", employee)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el cierre calcula lo vendido, descuenta el stock y deja el sobrante como stock del turno siguiente")
    void closesByCount() throws Exception {
        String shift = idOf(json(post("/api/v1/shifts"), "{}", employee));
        json(post("/api/v1/sales"), "{\"items\": [{\"productId\": \"%s\", \"quantity\": \"2\"}]}".formatted(productId), employee)
                .andExpect(status().isCreated());

        json(post("/api/v1/shifts/" + shift + "/close"), close("3"), employee)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.registeredSales").value(1.0))
                // Antes de contar quedaban 8: 8 − 3 = 5 vendidos por conteo × 0.50 = 2.50; esperado = 1.00 + 2.50.
                .andExpect(jsonPath("$.countedSales").value(2.5))
                .andExpect(jsonPath("$.expectedAmount").value(3.5))
                .andExpect(jsonPath("$.items[0].soldByCount").value("5.000"));

        assertThat(stockOf(monserrate)).isEqualTo("3.000");
        mvc.perform(get("/api/v1/stock-movements").param("type", "SHIFT_CLOSE").header("Authorization", employee))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].movement.direction").value("OUT"))
                .andExpect(jsonPath("$.content[0].movement.sourceLabel").value("Cierre de turno"));

        json(post("/api/v1/shifts"), "{}", employee)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].openingStock").value("3.000"));
    }

    @Test
    @DisplayName("un sobrante mayor que el stock responde 422 y el turno sigue abierto")
    void leftoverAboveStock() throws Exception {
        String shift = idOf(json(post("/api/v1/shifts"), "{}", employee));

        json(post("/api/v1/shifts/" + shift + "/close"), close("11"), employee)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Pan francés")));

        assertThat(stockOf(monserrate)).isEqualTo("10.000");
        mvc.perform(get("/api/v1/shifts/current").header("Authorization", employee)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("un turno cerrado no se vuelve a cerrar ni se modifica en la base (RN-03)")
    void closedShiftIsImmutable() throws Exception {
        String shift = idOf(json(post("/api/v1/shifts"), "{}", employee));
        json(post("/api/v1/shifts/" + shift + "/close"), close("10"), employee).andExpect(status().isOk());

        json(post("/api/v1/shifts/" + shift + "/close"), close("10"), employee).andExpect(status().isConflict());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> jdbc.update("UPDATE shift SET note = 'x' WHERE id = ?::uuid", shift))
                .hasMessageContaining("inmutable");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> jdbc.update("DELETE FROM shift_item WHERE shift_id = ?::uuid", shift))
                .hasMessageContaining("inmutable");
    }

    @Test
    @DisplayName("sin token responde 401")
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/shifts/current").header("Authorization", "")).andExpect(status().isUnauthorized());
    }

    private String close(String leftover) {
        return "{\"counts\": [{\"productId\": \"%s\", \"leftover\": \"%s\"}]}".formatted(productId, leftover);
    }

    private String stockOf(String siteId) throws Exception {
        String body = mvc.perform(get("/api/v1/products/" + productId).param("siteId", siteId))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.stock");
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String body, String authorization)
            throws Exception {
        request.contentType(MediaType.APPLICATION_JSON).content(body);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mvc.perform(request);
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
