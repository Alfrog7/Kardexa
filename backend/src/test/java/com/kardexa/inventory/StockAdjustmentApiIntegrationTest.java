package com.kardexa.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.inventory.application.port.in.AdjustStockUseCase;
import com.kardexa.inventory.application.port.in.AdjustStockUseCase.AdjustStockCommand;
import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-09 de punta a punta: HTTP → caso de uso con transacción → PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API de ajustes de stock contra PostgreSQL (HU-09)")
class StockAdjustmentApiIntegrationTest {

    private static final String URL = "/api/v1/stock-adjustments";
    private static final String PRODUCTS = "/api/v1/products";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AdjustStockUseCase adjustStock;

    private String categoryId;
    private String productId;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE stock_movement, stock_adjustment, product_stock, product, category");
        TestSecurity.insertAdmin(jdbc);
        categoryId = idOf(send(post("/api/v1/categories"), "{\"name\": \"Abarrotes\"}"));
        productId = idOf(send(post(PRODUCTS), productJson()));
    }

    @Test
    @DisplayName("criterio 1: una salida de 3 por merma sobre 20 crea el movimiento y deja 17")
    void withdrawsWithMovement() throws Exception {
        adjust("IN", "20", "PHYSICAL_COUNT").andExpect(status().isCreated());

        adjust("OUT", "3", "SHRINKAGE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.direction").value("OUT"))
                .andExpect(jsonPath("$.quantity").value("3.000"))
                .andExpect(jsonPath("$.balanceAfter").value("17.000"))
                .andExpect(jsonPath("$.sourceLabel").value("Ajuste"))
                .andExpect(jsonPath("$.user").value("Administrador de pruebas"));

        assertThat(currentStock()).isEqualByComparingTo("17");
        assertThat(count("stock_movement")).isEqualTo(2);
        assertThat(count("stock_adjustment")).isEqualTo(2);
        mvc.perform(get(PRODUCTS + "/" + productId)).andExpect(jsonPath("$.stock").value("17.000"));
    }

    @Test
    @DisplayName("criterio 2: una entrada crea la fila de stock si no existía")
    void createsStockRow() throws Exception {
        assertThat(count("product_stock")).isZero();

        adjust("IN", "2.5", "CORRECTION")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value("2.500"));

        assertThat(currentStock()).isEqualByComparingTo("2.5");
    }

    @Test
    @DisplayName("criterio 3 · RN-05: un ajuste sin motivo devuelve 400")
    void requiresReason() throws Exception {
        send(post(URL), """
                {"productId": "%s", "direction": "IN", "quantity": "1"}""".formatted(productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        assertThat(count("stock_movement")).isZero();
    }

    @Test
    @DisplayName("criterio 4 · RN-01: una salida mayor que el stock devuelve 422 y no cambia nada")
    void rejectsInsufficientStock() throws Exception {
        adjust("IN", "2", "CORRECTION").andExpect(status().isCreated());

        adjust("OUT", "2.001", "SHRINKAGE")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("RN-01"))
                .andExpect(jsonPath("$.title").value("Stock insuficiente"));

        assertThat(currentStock()).isEqualByComparingTo("2");
        assertThat(count("stock_movement")).isEqualTo(1);
    }

    @Test
    @DisplayName("criterio 5: producto inexistente 404; producto desactivado 422")
    void validatesProduct() throws Exception {
        String unknown = """
                {"productId": "%s", "direction": "IN", "quantity": "1", "reason": "CORRECTION"}"""
                .formatted(UUID.randomUUID());
        send(post(URL), unknown).andExpect(status().isNotFound());

        send(put(PRODUCTS + "/" + productId), productJson().replace("}", ", \"active\": false}"))
                .andExpect(status().isOk());
        adjust("IN", "1", "CORRECTION")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Producto desactivado"));
    }

    @Test
    @DisplayName("formato inválido devuelve 400 con el campo; cantidad 0 devuelve 422")
    void validatesFormat() throws Exception {
        adjust("SIDEWAYS", "1", "CORRECTION")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("direction"));
        adjust("IN", "1.0001", "CORRECTION")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        adjust("IN", "1", "ROBO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        adjust("IN", "0", "CORRECTION")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Ajuste de stock inválido"));
    }

    @Test
    @DisplayName("un stock que superaría el máximo devuelve 422, no un error interno")
    void rejectsOverflow() throws Exception {
        adjust("IN", "999999999.999", "CORRECTION").andExpect(status().isCreated());

        adjust("IN", "0.001", "CORRECTION")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Stock fuera de rango"));
    }

    @Test
    @DisplayName("criterio 6 · ADR 0006: ajustes simultáneos dejan el stock y los saldos consistentes")
    void concurrentAdjustmentsStayConsistent() throws Exception {
        adjust("IN", "100", "PHYSICAL_COUNT").andExpect(status().isCreated());

        List<Integer> statuses = concurrently(8, () -> adjust("OUT", "1", "SHRINKAGE"));

        long created = statuses.stream().filter(s -> s == 201).count();
        assertThat(statuses).allMatch(s -> s == 201 || s == 409);
        assertThat(created).isPositive();
        assertThat(currentStock()).isEqualByComparingTo(BigDecimal.valueOf(100 - created));
        assertThat(count("stock_movement")).isEqualTo(created + 1);
        assertBalancesAddUp();
    }

    @Test
    @DisplayName("RN-01 · ADR 0006: la última unidad solo se puede sacar una vez")
    void lastUnitOnlyOnce() throws Exception {
        adjust("IN", "1", "CORRECTION").andExpect(status().isCreated());

        List<Integer> statuses = concurrently(4, () -> adjust("OUT", "1", "SHRINKAGE"));

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).allMatch(s -> s == 201 || s == 422 || s == 409);
        assertThat(currentStock()).isEqualByComparingTo("0");
        assertBalancesAddUp();
    }

    @Test
    @DisplayName("criterio 7: si falla la escritura del movimiento, el stock no cambia")
    void rollsBackWhenMovementFails() throws Exception {
        adjust("IN", "5", "CORRECTION").andExpect(status().isCreated());
        jdbc.execute("ALTER TABLE stock_movement ADD CONSTRAINT test_rechaza_todo CHECK (quantity < 0) NOT VALID");
        try {
            AdjustStockCommand command = new AdjustStockCommand(UUID.fromString(productId), Direction.OUT,
                    Quantity.of("2"), AdjustmentReason.SHRINKAGE, null);

            assertThatThrownBy(() -> adjustStock.adjust(command)).isInstanceOf(DataAccessException.class);
        } finally {
            jdbc.execute("ALTER TABLE stock_movement DROP CONSTRAINT test_rechaza_todo");
        }

        assertThat(currentStock()).isEqualByComparingTo("5");
        assertThat(count("stock_adjustment")).isEqualTo(1);
        assertThat(count("stock_movement")).isEqualTo(1);
    }

    @Test
    @DisplayName("RN-03: la base rechaza editar o borrar movimientos y ajustes")
    void recordsAreImmutable() throws Exception {
        adjust("IN", "5", "CORRECTION").andExpect(status().isCreated());

        assertThatThrownBy(() -> jdbc.update("UPDATE stock_movement SET quantity = 1"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("RN-03");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM stock_movement"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM stock_adjustment"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("HU-10: un ajuste de entrada saca al producto de la lista de stock crítico")
    void adjustmentLeavesCriticalList() throws Exception {
        mvc.perform(get(PRODUCTS).param("criticalOnly", "true")).andExpect(jsonPath("$.totalElements").value(1));

        adjust("IN", "5.001", "PHYSICAL_COUNT").andExpect(status().isCreated());

        mvc.perform(get(PRODUCTS).param("criticalOnly", "true")).andExpect(jsonPath("$.totalElements").value(0));
    }

    /** RN-02: en el orden real del kárdex, cada saldo es el anterior más la entrada o menos la salida. */
    private void assertBalancesAddUp() {
        BigDecimal balance = BigDecimal.ZERO;
        for (var row : jdbc.queryForList("SELECT direction, quantity, balance_after FROM stock_movement ORDER BY seq")) {
            BigDecimal quantity = (BigDecimal) row.get("quantity");
            balance = "IN".equals(row.get("direction")) ? balance.add(quantity) : balance.subtract(quantity);
            assertThat((BigDecimal) row.get("balance_after")).isEqualByComparingTo(balance);
        }
        assertThat(currentStock()).isEqualByComparingTo(balance);
    }

    /** Lanza las peticiones a la vez y devuelve sus códigos HTTP. */
    private List<Integer> concurrently(int requests, Callable<ResultActions> request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(requests)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return request.call().andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        }
    }

    private ResultActions adjust(String direction, String quantity, String reason) throws Exception {
        return send(post(URL), """
                {"productId": "%s", "direction": "%s", "quantity": "%s", "reason": "%s", "note": "prueba"}"""
                .formatted(productId, direction, quantity, reason));
    }

    private ResultActions send(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String json)
            throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String productJson() {
        return """
                {"code": "AR-001", "name": "Arroz 5 kg", "categoryId": "%s", "unit": "UNIT",
                 "salePrice": "20.00", "minStock": "5"}""".formatted(categoryId);
    }

    private BigDecimal currentStock() {
        return jdbc.queryForObject("SELECT quantity FROM product_stock WHERE product_id = ?::uuid",
                BigDecimal.class, productId);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
