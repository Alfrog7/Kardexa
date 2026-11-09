package com.kardexa.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.catalog.application.port.out.ProductRepository;
import com.kardexa.catalog.domain.exception.DuplicateProductCodeException;
import com.kardexa.catalog.domain.model.Product;
import com.kardexa.catalog.domain.model.Unit;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/** HU-06 de punta a punta: HTTP → caso de uso → JPA e inventario → PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API de productos contra PostgreSQL (HU-06)")
class ProductApiIntegrationTest {

    private static final String URL = "/api/v1/products";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProductRepository repository;

    private String drinks;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE stock_movement, stock_adjustment, product_stock, product, category");
        drinks = createCategory("Bebidas");
    }

    @Test
    @DisplayName("criterio 1: crea con stock 0, costo 0 y en stock crítico")
    void createsProduct() throws Exception {
        ResultActions result = create(body("AB-001", drinks, "3.50", "5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AB-001"))
                .andExpect(jsonPath("$.categoryName").value("Bebidas"))
                .andExpect(jsonPath("$.unit").value("UNIT"))
                .andExpect(jsonPath("$.salePrice").value("3.50"))
                .andExpect(jsonPath("$.minStock").value("5.000"))
                .andExpect(jsonPath("$.referenceCost").value("0.00"))
                .andExpect(jsonPath("$.stock").value("0.000"))
                .andExpect(jsonPath("$.critical").value(true))
                .andExpect(jsonPath("$.active").value(true));

        result.andExpect(header().string("Location", endsWith(URL + "/" + idOf(result))));
    }

    @Test
    @DisplayName("criterio 2 · RN-10: código repetido sin distinguir mayúsculas devuelve 409")
    void rejectsDuplicateCode() throws Exception {
        create(body("AB-001", drinks, "3.50", "5")).andExpect(status().isCreated());

        create(body(" ab-001 ", drinks, "4.00", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("RN-10"));
    }

    @Test
    @DisplayName("criterio 3: categoría inexistente 404; categoría desactivada 422")
    void validatesCategory() throws Exception {
        create(body("AB-001", UUID.randomUUID().toString(), "3.50", "5")).andExpect(status().isNotFound());

        mvc.perform(put("/api/v1/categories/" + drinks).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Bebidas\", \"active\": false}")).andExpect(status().isOk());

        create(body("AB-001", drinks, "3.50", "5"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Categoría desactivada"));
    }

    @Test
    @DisplayName("criterio 4: formato inválido 400 con el campo; precio 0 devuelve 422")
    void validatesFormat() throws Exception {
        create(body("AB-001", drinks, "3.505", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("salePrice"));
        create(body("AB-001", drinks, "3.50", "5").replace("\"UNIT\"", "\"LITRO\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("unit"));
        create(body("AB-001", drinks, "0.00", "5"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Producto inválido"));
    }

    @Test
    @DisplayName("criterio 5: consulta con el stock del inventario; id inexistente 404")
    void getsProductWithInventoryStock() throws Exception {
        String id = idOf(create(body("AB-001", drinks, "3.50", "5")));
        jdbc.update("INSERT INTO product_stock (product_id, quantity) VALUES (?::uuid, 12.5)", id);

        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value("12.500"))
                .andExpect(jsonPath("$.critical").value(false));
        mvc.perform(get(URL + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("criterio 6 · RF-09: se edita y se desactiva; no existe DELETE")
    void editsAndDeactivates() throws Exception {
        String id = idOf(create(body("AB-001", drinks, "3.50", "5")));

        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("AB-001", drinks, "3.90", "2").replace("}", ", \"active\": false}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salePrice").value("3.90"))
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(delete(URL + "/" + id)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get(URL + "/" + id)).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("HU-07 criterio 1: busca por parte del nombre o código, sin mayúsculas, con precio y stock")
    void searchesByText() throws Exception {
        String soda = idOf(create(product("AB-001", "Gaseosa 500 ml", drinks)));
        create(product("GAS-02", "Agua mineral", drinks));
        create(product("AR-003", "Arroz 5 kg", drinks));
        jdbc.update("INSERT INTO product_stock (product_id, quantity) VALUES (?::uuid, 7)", soda);

        mvc.perform(get(URL).param("q", "GAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].code").value("GAS-02"))
                .andExpect(jsonPath("$.content[1].code").value("AB-001"))
                .andExpect(jsonPath("$.content[1].salePrice").value("3.50"))
                .andExpect(jsonPath("$.content[1].stock").value("7.000"))
                .andExpect(jsonPath("$.content[1].categoryName").value("Bebidas"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("HU-07: los comodines % y _ escritos por el usuario se buscan tal cual")
    void escapesWildcards() throws Exception {
        create(product("AB-001", "Gaseosa 500 ml", drinks));
        create(product("AB_002", "Detergente 100% limpio", drinks));

        mvc.perform(get(URL).param("q", "100%"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("AB_002"));
        mvc.perform(get(URL).param("q", "_"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("HU-07 criterio 2: filtra por categoría y lo combina con el texto")
    void filtersByCategory() throws Exception {
        String groceries = createCategory("Abarrotes");
        create(product("AB-001", "Gaseosa 500 ml", drinks));
        create(product("AR-003", "Arroz 5 kg", groceries));
        create(product("AR-004", "Arroz 1 kg", groceries));

        mvc.perform(get(URL).param("categoryId", groceries))
                .andExpect(jsonPath("$.content[*].code", contains("AR-004", "AR-003")));
        mvc.perform(get(URL).param("categoryId", groceries).param("q", "5 KG"))
                .andExpect(jsonPath("$.content[*].code", contains("AR-003")));
    }

    @Test
    @DisplayName("HU-07: los productos desactivados no aparecen")
    void excludesInactive() throws Exception {
        String id = idOf(create(product("AB-001", "Gaseosa 500 ml", drinks)));
        create(product("AB-002", "Gaseosa 1 l", drinks));
        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(product("AB-001", "Gaseosa 500 ml", drinks).replace("}", ", \"active\": false}")));

        mvc.perform(get(URL))
                .andExpect(jsonPath("$.content[*].code", contains("AB-002")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("includeInactive: también devuelve los desactivados, marcados como inactivos")
    void includesInactiveOnRequest() throws Exception {
        String id = idOf(create(product("AB-001", "Gaseosa 500 ml", drinks)));
        create(product("AB-002", "Gaseosa 1 l", drinks));
        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(product("AB-001", "Gaseosa 500 ml", drinks).replace("}", ", \"active\": false}")));

        mvc.perform(get(URL).param("includeInactive", "true"))
                .andExpect(jsonPath("$.content[*].code", contains("AB-002", "AB-001")))
                .andExpect(jsonPath("$.content[1].active").value(false))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("HU-07: pagina en orden estable por nombre, con el total")
    void paginates() throws Exception {
        for (int i = 5; i >= 1; i--) {
            create(product("P-00" + i, "Producto " + i, drinks));
        }

        mvc.perform(get(URL).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", contains("P-003", "P-004")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        mvc.perform(get(URL))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    @DisplayName("HU-07: parámetros inválidos devuelven 400 con el campo")
    void rejectsInvalidParams() throws Exception {
        mvc.perform(get(URL).param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("q"));
        mvc.perform(get(URL).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        mvc.perform(get(URL).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
        mvc.perform(get(URL).param("categoryId", "no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("categoryId"))
                .andExpect(jsonPath("$.errors[0].message").value("Formato inválido."));
    }

    @Test
    @DisplayName("HU-10 criterio 1 · RN-09: criticalOnly muestra los productos con stock ≤ mínimo")
    void filtersCriticalStock() throws Exception {
        String atMinimum = idOf(create(product("AB-001", "Arroz", drinks, "10")));
        String below = idOf(create(product("AB-002", "Azúcar", drinks, "10")));
        String above = idOf(create(product("AB-003", "Fideos", drinks, "10")));
        create(product("AB-004", "Sal", drinks, "10"));
        setStock(atMinimum, "10");
        setStock(below, "3.5");
        setStock(above, "11");

        mvc.perform(get(URL).param("criticalOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", contains("AB-001", "AB-002", "AB-004")))
                .andExpect(jsonPath("$.content[*].critical", contains(true, true, true)))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get(URL).param("criticalOnly", "false"))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("HU-10 criterio 2: al subir el stock por encima del mínimo deja de aparecer")
    void leavesCriticalWhenStockRises() throws Exception {
        String id = idOf(create(product("AB-001", "Arroz", drinks, "10")));
        mvc.perform(get(URL).param("criticalOnly", "true")).andExpect(jsonPath("$.totalElements").value(1));

        setStock(id, "10.001");

        mvc.perform(get(URL).param("criticalOnly", "true")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("HU-10: criticalOnly se combina con texto, categoría y paginación")
    void combinesCriticalWithOtherFilters() throws Exception {
        String groceries = createCategory("Abarrotes");
        create(product("AB-001", "Arroz 5 kg", groceries));
        create(product("AB-002", "Arroz 1 kg", groceries));
        create(product("AB-003", "Arroz 750 g", drinks));
        String stocked = idOf(create(product("AB-004", "Arroz 2 kg", groceries)));
        setStock(stocked, "50");

        mvc.perform(get(URL).param("criticalOnly", "true").param("q", "arroz")
                        .param("categoryId", groceries).param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content[*].code", contains("AB-001")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(URL).param("criticalOnly", "quizás"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("criticalOnly"));
    }

    @Test
    @DisplayName("Swagger muestra cada parámetro de la búsqueda por separado, todos opcionales")
    void documentsSearchParams() throws Exception {
        String parameters = "$.paths['/api/v1/products'].get.parameters";

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parameters + "[*].name",
                        containsInAnyOrder("q", "categoryId", "criticalOnly", "includeInactive", "page", "size")))
                .andExpect(jsonPath(parameters + "[?(@.required == true)]").isEmpty());
    }

    @Test
    @DisplayName("el índice único de la base frena códigos repetidos aunque se salte el servicio")
    void databaseEnforcesUniqueCode() {
        UUID category = UUID.fromString(drinks);
        repository.save(Product.create(new Product.Details(
                "AB-001", "Gaseosa", category, Unit.UNIT, Money.of("3.50"), Quantity.of("5"))));

        assertThatThrownBy(() -> repository.save(Product.create(new Product.Details(
                "ab-001", "Otra", category, Unit.UNIT, Money.of("1.00"), Quantity.of("1")))))
                .isInstanceOf(DuplicateProductCodeException.class);
    }

    private static String body(String code, String categoryId, String price, String minStock) {
        return """
                {"code": "%s", "name": "Gaseosa 500 ml", "categoryId": "%s", "unit": "UNIT",
                 "salePrice": "%s", "minStock": "%s"}""".formatted(code, categoryId, price, minStock);
    }

    private static String product(String code, String name, String categoryId) {
        return product(code, name, categoryId, "5");
    }

    private static String product(String code, String name, String categoryId, String minStock) {
        return body(code, categoryId, "3.50", minStock).replace("Gaseosa 500 ml", name);
    }

    /** Simula el stock que dejará un movimiento de inventario (los ajustes llegan con HU-09). */
    private void setStock(String productId, String quantity) {
        jdbc.update("INSERT INTO product_stock (product_id, quantity) VALUES (?::uuid, ?::numeric) "
                + "ON CONFLICT (product_id) DO UPDATE SET quantity = EXCLUDED.quantity", productId, quantity);
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String createCategory(String name) throws Exception {
        return idOf(mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}")));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
