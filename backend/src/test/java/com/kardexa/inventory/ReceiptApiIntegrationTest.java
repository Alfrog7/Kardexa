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

/** HU-21 · Entradas de mercadería de la sede, de punta a punta contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("Entradas de mercadería contra PostgreSQL (HU-21)")
class ReceiptApiIntegrationTest {

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
        String category = idOf(json(post("/api/v1/categories"), "{\"name\": \"Panes\"}", null));
        productId = idOf(json(post("/api/v1/products"), """
                {"code": "PAN-1", "name": "Pan francés", "categoryId": "%s", "unit": "UNIT",
                 "salePrice": "0.50", "minStock": "6"}""".formatted(category), null));
        galeno = idOf(json(post("/api/v1/sites"), "{\"name\": \"Galeno\"}", null));
        monserrate = idOf(json(post("/api/v1/sites"), "{\"name\": \"Monserrate\"}", null));
    }

    @Test
    @DisplayName("criterio 1: la entrada suma stock solo en la sede de la trabajadora")
    void receiptAddsStockOnlyInItsSite() throws Exception {
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE, UUID.fromString(monserrate));

        json(post("/api/v1/receipts"), receipt(null, "40"), employee)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siteId").value(monserrate))
                .andExpect(jsonPath("$.items[0].quantity").value("40.000"));

        assertThat(stockOf(monserrate)).isEqualTo("40.000");
        assertThat(stockOf(galeno)).isEqualTo("0.000");
    }

    @Test
    @DisplayName("criterio 2: aparece en el kárdex como «Entrada», con tipo RECEIPT y quien la registró")
    void receiptShowsInTheKardex() throws Exception {
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE, UUID.fromString(monserrate));
        json(post("/api/v1/receipts"), receipt(null, "12.5"), employee).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/stock-movements").param("type", "RECEIPT").header("Authorization", employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].movement.type").value("RECEIPT"))
                .andExpect(jsonPath("$.content[0].movement.direction").value("IN"))
                .andExpect(jsonPath("$.content[0].movement.sourceLabel").value("Entrada"))
                .andExpect(jsonPath("$.content[0].movement.user").value("Administrador de pruebas"))
                .andExpect(jsonPath("$.content[0].movement.balanceAfter").value("12.500"));
    }

    @Test
    @DisplayName("una trabajadora que pide otra sede recibe 404 y no cambia ningún stock")
    void otherSiteIsNotFound() throws Exception {
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE, UUID.fromString(monserrate));

        json(post("/api/v1/receipts"), receipt(galeno, "5"), employee).andExpect(status().isNotFound());

        assertThat(stockOf(galeno)).isEqualTo("0.000");
        assertThat(stockOf(monserrate)).isEqualTo("0.000");
    }

    @Test
    @DisplayName("el ADMIN registra la entrada en la sede que indica")
    void adminChoosesTheSite() throws Exception {
        json(post("/api/v1/receipts"), receipt(galeno, "7"), null).andExpect(status().isCreated());

        assertThat(stockOf(galeno)).isEqualTo("7.000");
    }

    @Test
    @DisplayName("rechaza una entrada vacía, con cantidad 0 o con un producto repetido")
    void rejectsInvalidReceipts() throws Exception {
        json(post("/api/v1/receipts"), "{\"items\": []}", null).andExpect(status().isBadRequest());
        json(post("/api/v1/receipts"), receipt(galeno, "0"), null).andExpect(status().isUnprocessableContent());
        json(post("/api/v1/receipts"), """
                {"siteId": "%s", "items": [{"productId": "%s", "quantity": "1"}, {"productId": "%s", "quantity": "2"}]}"""
                .formatted(galeno, productId, productId), null).andExpect(status().isUnprocessableContent());
        assertThat(stockOf(galeno)).isEqualTo("0.000");
    }

    @Test
    @DisplayName("sin token responde 401")
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/receipts").header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON).content(receipt(null, "1")))
                .andExpect(status().isUnauthorized());
    }

    private String receipt(String siteId, String quantity) {
        String site = siteId == null ? "" : "\"siteId\": \"%s\", ".formatted(siteId);
        return "{" + site + "\"items\": [{\"productId\": \"%s\", \"quantity\": \"%s\"}]}".formatted(productId, quantity);
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
