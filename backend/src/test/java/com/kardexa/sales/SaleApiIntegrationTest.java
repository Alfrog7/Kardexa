package com.kardexa.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

/** HU-14 de punta a punta: ventas, catálogo e inventario contra PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API de ventas contra PostgreSQL (HU-14)")
class SaleApiIntegrationTest {

    private static final String URL = "/api/v1/sales";
    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    private UUID rice;
    private UUID oil;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE sale_item, sale, purchase_item, purchase, supplier, stock_movement, stock_adjustment,"
                + " product_stock, product, category");
        jdbc.update("UPDATE sale_number_counter SET last_value = 0");
        TestSecurity.insertAdmin(jdbc);
        jdbc.update("""
                INSERT INTO app_user (id, full_name, email, password_hash, role)
                VALUES (?, 'Empleada de pruebas', 'empleada@pruebas.test', 'x', 'EMPLOYEE')
                ON CONFLICT DO NOTHING""", EMPLOYEE_ID);
        UUID category = UUID.randomUUID();
        jdbc.update("INSERT INTO category (id, name) VALUES (?, 'Abarrotes')", category);
        rice = product(category, "AR-001", "Arroz 5 kg", "25.00", "10");
        oil = product(category, "AC-001", "Aceite 1 L", "9.90", "4");
    }

    @Test
    @DisplayName("criterio 1: una venta descuenta el stock, deja su salida en el kárdex y recibe el siguiente número")
    void registersSale() throws Exception {
        ResultActions result = sell(null, item(rice, "2"), item(oil, "1.5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("V-000001"))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.seller").value("Administrador de pruebas"))
                .andExpect(jsonPath("$.total").value("64.85"))
                .andExpect(jsonPath("$.items[0].productCode").value("AR-001"))
                .andExpect(jsonPath("$.items[0].unitPrice").value("25.00"))
                .andExpect(jsonPath("$.items[1].quantity").value("1.500"))
                .andExpect(jsonPath("$.items[1].subtotal").value("14.85"));
        String id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        result.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(URL + "/" + id)));

        assertThat(stock(rice)).isEqualByComparingTo("8");
        assertThat(stock(oil)).isEqualByComparingTo("2.5");
        mvc.perform(get("/api/v1/products/" + rice + "/kardex"))
                .andExpect(jsonPath("$.movements[0].type").value("SALE"))
                .andExpect(jsonPath("$.movements[0].direction").value("OUT"))
                .andExpect(jsonPath("$.movements[0].sourceId").value(id));
        sell(null, item(rice, "1")).andExpect(jsonPath("$.number").value("V-000002"));
    }

    @Test
    @DisplayName("RN-06: cambiar el precio del producto no altera una venta ya registrada")
    void copiesPrice() throws Exception {
        String id = idOf(sell(null, item(rice, "1")).andExpect(status().isCreated()));
        jdbc.update("UPDATE product SET sale_price = 30.00 WHERE id = ?", rice);

        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value("25.00"))
                .andExpect(jsonPath("$.total").value("25.00"));
        sell(null, item(rice, "1")).andExpect(jsonPath("$.total").value("30.00"));
    }

    @Test
    @DisplayName("criterio 2 (RN-01): stock insuficiente rechaza toda la venta, nombra el producto y no gasta número")
    void rejectsInsufficientStock() throws Exception {
        sell(null, item(rice, "2"), item(oil, "5"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Aceite 1 L")));

        assertThat(stock(rice)).isEqualByComparingTo("10");
        assertThat(count("sale")).isZero();
        assertThat(count("stock_movement")).isZero();
        sell(null, item(rice, "1")).andExpect(jsonPath("$.number").value("V-000001"));
    }

    @Test
    @DisplayName("ítems inválidos devuelven 400; un producto desactivado 422 y uno inexistente 404")
    void rejectsInvalidItems() throws Exception {
        sell(null, item(rice, "0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items[0].quantityPositive"));
        sell(null).andExpect(status().isBadRequest());
        sell(null, item(rice, "1"), item(rice, "2")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("itemsWithoutRepeatedProducts"));
        jdbc.update("UPDATE product SET active = false WHERE id = ?", oil);
        sell(null, item(rice, "1"), item(oil, "1")).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Producto desactivado"));
        sell(null, item(UUID.randomUUID(), "1")).andExpect(status().isNotFound());

        assertThat(count("sale")).isZero();
        assertThat(stock(rice)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("criterio 3 (RNF-05): de dos ventas simultáneas del último ítem solo una se registra")
    void lastItemIsSoldOnce() throws Exception {
        jdbc.update("UPDATE product_stock SET quantity = 1 WHERE product_id = ?", oil);
        String employee = employeeToken();
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String token : List.of(adminToken(), employee)) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return sell(token, item(oil, "1")).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(201, 422);
        assertThat(stock(oil)).isEqualByComparingTo("0");
        assertThat(count("sale")).isEqualTo(1);
    }

    @Test
    @DisplayName("RF-21: el EMPLEADO registra ventas a su nombre y la de otro le responde 404")
    void employeeSeesOnlyOwnSales() throws Exception {
        String employee = employeeToken();
        String own = idOf(sell(employee, item(rice, "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seller").value("Empleada de pruebas")));
        String other = idOf(sell(null, item(rice, "1")));

        mvc.perform(get(URL + "/" + own).header("Authorization", employee)).andExpect(status().isOk());
        mvc.perform(get(URL + "/" + other).header("Authorization", employee)).andExpect(status().isNotFound());
        mvc.perform(get(URL + "/" + own)).andExpect(status().isOk());
    }

    private String employeeToken() {
        User user = User.restore(EMPLOYEE_ID, "Empleada de pruebas", "empleada@pruebas.test", "x", Role.EMPLOYEE, true);
        return "Bearer " + tokens.issue(user).value();
    }

    private String adminToken() {
        return "Bearer " + TestSecurity.token(tokens, Role.ADMIN);
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private UUID product(UUID category, String code, String name, String price, String stock) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (id, code, name, category_id, unit, sale_price, min_stock)
                VALUES (?, ?, ?, ?, 'UNIT', ?::numeric, 1)""", id, code, name, category, price);
        jdbc.update("INSERT INTO product_stock (product_id, quantity, version) VALUES (?, ?::numeric, 0)", id, stock);
        return id;
    }

    private static String item(UUID productId, String quantity) {
        return """
                {"productId": "%s", "quantity": "%s"}""".formatted(productId, quantity);
    }

    /** Sin token usa el del ADMIN por defecto. */
    private ResultActions sell(String token, String... items) throws Exception {
        var request = post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\": [%s]}".formatted(String.join(",", items)));
        return mvc.perform(token == null ? request : request.header("Authorization", token));
    }

    private BigDecimal stock(UUID productId) {
        return jdbc.queryForObject("SELECT quantity FROM product_stock WHERE product_id = ?", BigDecimal.class,
                productId);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
