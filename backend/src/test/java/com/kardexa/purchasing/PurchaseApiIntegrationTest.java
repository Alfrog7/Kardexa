package com.kardexa.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.DocumentMovement;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.RecordStockMovementsCommand;
import com.kardexa.shared.domain.Quantity;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.transaction.IllegalTransactionStateException;

/** HU-12 de punta a punta: compras, inventario y catálogo contra PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API de compras contra PostgreSQL (HU-12)")
class PurchaseApiIntegrationTest {

    private static final String URL = "/api/v1/purchases";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    @Autowired
    private RecordStockMovementsUseCase recordStockMovements;

    private UUID supplierId;
    private UUID rice;
    private UUID oil;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE purchase_item, purchase, supplier, stock_movement, stock_adjustment, product_stock,"
                + " product, category");
        TestSecurity.insertAdmin(jdbc);
        UUID category = UUID.randomUUID();
        jdbc.update("INSERT INTO category (id, name) VALUES (?, 'Abarrotes')", category);
        rice = product(category, "AR-001", "Arroz 5 kg");
        oil = product(category, "AC-001", "Aceite 1 L");
        supplierId = UUID.randomUUID();
        jdbc.update("INSERT INTO supplier (id, ruc, business_name) VALUES (?, '20123456789', 'Distribuidora Lima')",
                supplierId);
    }

    @Test
    @DisplayName("criterio 1: una compra de dos productos crea dos entradas y suma su stock")
    void registersPurchase() throws Exception {
        ResultActions result = register(supplierId, item(rice, "10", "18.50"), item(oil, "4", "9.90"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.supplier.businessName").value("Distribuidora Lima"))
                .andExpect(jsonPath("$.supplierDocument").value("F001-77"))
                .andExpect(jsonPath("$.purchaseDate").value("2026-11-10"))
                .andExpect(jsonPath("$.total").value("224.60"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Arroz 5 kg"))
                .andExpect(jsonPath("$.items[0].quantity").value("10.000"))
                .andExpect(jsonPath("$.items[0].subtotal").value("185.00"));
        String id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        result.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(URL + "/" + id)));

        assertThat(stock(rice)).isEqualByComparingTo("10");
        assertThat(stock(oil)).isEqualByComparingTo("4");
        mvc.perform(get("/api/v1/products/" + rice + "/kardex"))
                .andExpect(jsonPath("$.movements.length()").value(1))
                .andExpect(jsonPath("$.movements[0].type").value("PURCHASE"))
                .andExpect(jsonPath("$.movements[0].direction").value("IN"))
                .andExpect(jsonPath("$.movements[0].sourceId").value(id))
                .andExpect(jsonPath("$.movements[0].sourceLabel").value("Compra"))
                .andExpect(jsonPath("$.movements[0].user").value("Administrador de pruebas"));
    }

    @Test
    @DisplayName("criterio 2: el costo referencial queda en el último costo comprado (RN-08)")
    void updatesReferenceCost() throws Exception {
        register(supplierId, item(rice, "10", "18.50")).andExpect(status().isCreated());
        register(supplierId, item(rice, "5", "19.20")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/products/" + rice))
                .andExpect(jsonPath("$.referenceCost").value("19.20"))
                .andExpect(jsonPath("$.stock").value("15.000"));
    }

    @Test
    @DisplayName("criterio 3: un ítem con cantidad 0, sin productos o repetido devuelve 400 y no guarda nada")
    void rejectsInvalidItems() throws Exception {
        register(supplierId, item(rice, "10", "18.50"), item(oil, "0", "9.90"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items[1].quantityPositive"));
        register(supplierId).andExpect(status().isBadRequest());
        register(supplierId, item(rice, "1", "18.50"), item(rice, "2", "18.50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("itemsWithoutRepeatedProducts"));
        register(supplierId, item(rice, "1", "-1")).andExpect(status().isBadRequest());

        assertNothingSaved();
    }

    @Test
    @DisplayName("criterio 3 (RNF-04): un producto desactivado revierte también el stock del otro")
    void rollsBackEverything() throws Exception {
        jdbc.update("UPDATE product SET active = false WHERE id = ?", oil);

        register(supplierId, item(rice, "10", "18.50"), item(oil, "4", "9.90"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Producto desactivado"));

        assertNothingSaved();
        assertThat(jdbc.queryForObject("SELECT reference_cost FROM product WHERE id = ?", BigDecimal.class, rice))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("un proveedor desactivado devuelve 422 y uno inexistente, 404")
    void rejectsSupplier() throws Exception {
        register(UUID.randomUUID(), item(rice, "1", "1.00")).andExpect(status().isNotFound());
        jdbc.update("UPDATE supplier SET active = false WHERE id = ?", supplierId);
        register(supplierId, item(rice, "1", "1.00"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Proveedor desactivado"));

        assertNothingSaved();
    }

    @Test
    @DisplayName("la lista filtra por fecha de compra, pagina y va de la más reciente a la más antigua")
    void listsPurchases() throws Exception {
        registerOn(supplierId, "2026-11-01", item(rice, "1", "10.00"));
        registerOn(supplierId, "2026-11-05", item(oil, "2", "5.00"));
        registerOn(supplierId, "2026-11-09", item(rice, "3", "11.00"));

        mvc.perform(get(URL).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].purchaseDate").value("2026-11-09"))
                .andExpect(jsonPath("$.content[1].items[0].productName").value("Aceite 1 L"));
        mvc.perform(get(URL).param("from", "2026-11-02").param("to", "2026-11-05"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].total").value("10.00"));
        mvc.perform(get(URL).param("from", "2026-11-05").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RN-05: un EMPLOYEE no puede consultar ni registrar compras")
    void employeeIsForbidden() throws Exception {
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE);

        mvc.perform(get(URL).header("Authorization", employee)).andExpect(status().isForbidden());
        mvc.perform(post(URL).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADR 0009: el inventario no registra movimientos fuera de una transacción")
    void inventoryRequiresTransaction() {
        assertThatThrownBy(() -> recordStockMovements.record(new RecordStockMovementsCommand(
                DocumentMovement.PURCHASE, UUID.randomUUID(), null, List.of(new Line(rice, Quantity.of("1"))))))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(count("stock_movement")).isZero();
    }

    @Test
    @DisplayName("HU-13 criterio 1: anular crea las salidas inversas y la compra queda anulada")
    void voidsPurchase() throws Exception {
        String id = idOf(register(supplierId, item(rice, "10", "18.50"), item(oil, "4", "9.90")));

        voidPurchase(id, "{\"reason\": \"Factura duplicada\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"))
                .andExpect(jsonPath("$.voidReason").value("Factura duplicada"))
                .andExpect(jsonPath("$.items.length()").value(2));

        assertThat(stock(rice)).isEqualByComparingTo("0");
        assertThat(stock(oil)).isEqualByComparingTo("0");
        mvc.perform(get("/api/v1/products/" + rice + "/kardex"))
                .andExpect(jsonPath("$.movements.length()").value(2))
                .andExpect(jsonPath("$.movements[0].type").value("PURCHASE"))
                .andExpect(jsonPath("$.movements[1].type").value("PURCHASE_VOID"))
                .andExpect(jsonPath("$.movements[1].direction").value("OUT"))
                .andExpect(jsonPath("$.movements[1].sourceId").value(id))
                .andExpect(jsonPath("$.movements[1].balanceAfter").value("0.000"));
        mvc.perform(get(URL)).andExpect(jsonPath("$.content[0].status").value("VOIDED"));
        // El costo referencial no se revierte (ADR 0009).
        mvc.perform(get("/api/v1/products/" + rice)).andExpect(jsonPath("$.referenceCost").value("18.50"));
    }

    @Test
    @DisplayName("HU-13 criterio 2: si anular dejaría stock negativo, 422 y no cambia nada (RN-01)")
    void rejectsVoidWithoutStock() throws Exception {
        String id = idOf(register(supplierId, item(rice, "10", "18.50"), item(oil, "4", "9.90")));
        mvc.perform(post("/api/v1/stock-adjustments").contentType(MediaType.APPLICATION_JSON).content("""
                {"productId": "%s", "direction": "OUT", "quantity": "1", "reason": "SHRINKAGE"}""".formatted(oil)))
                .andExpect(status().isCreated());

        voidPurchase(id, "{\"reason\": \"Factura duplicada\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("RN-01"));

        assertThat(stock(rice)).isEqualByComparingTo("10");
        assertThat(stock(oil)).isEqualByComparingTo("3");
        assertThat(count("stock_movement")).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT status FROM purchase WHERE id = ?::uuid", String.class, id))
                .isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("HU-13: no se anula dos veces, sin motivo, una compra inexistente ni como EMPLOYEE")
    void voidErrors() throws Exception {
        String id = idOf(register(supplierId, item(rice, "10", "18.50")));

        voidPurchase(id, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        voidPurchase(id, "{\"reason\": \"ab\"}").andExpect(status().isBadRequest());
        voidPurchase(UUID.randomUUID().toString(), "{\"reason\": \"Motivo\"}").andExpect(status().isNotFound());
        mvc.perform(post(URL + "/" + id + "/void")
                        .header("Authorization", "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Motivo\"}"))
                .andExpect(status().isForbidden());

        voidPurchase(id, "{\"reason\": \"Factura duplicada\"}").andExpect(status().isOk());
        voidPurchase(id, "{\"reason\": \"Otra vez\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Compra ya anulada"));
        assertThat(count("stock_movement")).isEqualTo(2);
    }

    private ResultActions voidPurchase(String id, String body) throws Exception {
        return mvc.perform(post(URL + "/" + id + "/void").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private UUID product(UUID category, String code, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (id, code, name, category_id, unit, sale_price, min_stock)
                VALUES (?, ?, ?, ?, 'UNIT', 25.00, 5)""", id, code, name, category);
        return id;
    }

    private static String item(UUID productId, String quantity, String unitCost) {
        return """
                {"productId": "%s", "quantity": "%s", "unitCost": "%s"}""".formatted(productId, quantity, unitCost);
    }

    private ResultActions register(UUID supplier, String... items) throws Exception {
        return registerOn(supplier, "2026-11-10", items);
    }

    private ResultActions registerOn(UUID supplier, String date, String... items) throws Exception {
        String body = """
                {"supplierId": "%s", "supplierDocument": "F001-77", "purchaseDate": "%s", "items": [%s]}"""
                .formatted(supplier, date, String.join(",", items));
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private BigDecimal stock(UUID productId) {
        return jdbc.queryForObject("SELECT quantity FROM product_stock WHERE product_id = ?", BigDecimal.class,
                productId);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private void assertNothingSaved() {
        assertThat(count("purchase")).isZero();
        assertThat(count("purchase_item")).isZero();
        assertThat(count("stock_movement")).isZero();
        assertThat(count("product_stock")).isZero();
    }
}
