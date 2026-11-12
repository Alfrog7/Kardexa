package com.kardexa.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.catalog.application.port.out.CategoryRepository;
import com.kardexa.catalog.domain.exception.DuplicateCategoryNameException;
import com.kardexa.catalog.domain.model.Category;
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

/** HU-05 de punta a punta: HTTP → caso de uso → JPA → PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API de categorías contra PostgreSQL (HU-05)")
class CategoryApiIntegrationTest {

    private static final String URL = "/api/v1/categories";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CategoryRepository repository;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE sale_item, sale, purchase_item, purchase, stock_movement, stock_adjustment, product_stock, product, category");
    }

    @Test
    @DisplayName("criterio 1: crear devuelve 201, Location y la categoría activa")
    void createsCategory() throws Exception {
        ResultActions result = create("Bebidas")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bebidas"))
                .andExpect(jsonPath("$.active").value(true));

        result.andExpect(header().string("Location", endsWith(URL + "/" + idOf(result))));
    }

    @Test
    @DisplayName("el listado indica cuántos productos tiene cada categoría")
    void listsProductCounts() throws Exception {
        String drinks = idOf(create("Bebidas"));
        create("Limpieza");
        for (String code : new String[] {"B-1", "B-2"}) {
            mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                    {"code": "%s", "name": "Gaseosa %s", "categoryId": "%s", "unit": "UNIT", "salePrice": "3.50", "minStock": "1"}
                    """.formatted(code, code, drinks))).andExpect(status().isCreated());
        }

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bebidas"))
                .andExpect(jsonPath("$[0].productCount").value(2))
                .andExpect(jsonPath("$[1].productCount").value(0));
    }

    @Test
    @DisplayName("criterio 2: nombre repetido sin distinguir mayúsculas devuelve 409")
    void rejectsDuplicateName() throws Exception {
        create("Bebidas").andExpect(status().isCreated());

        create("  BEBIDAS ")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Categoría duplicada"))
                .andExpect(jsonPath("$.rule").value("RF-06"));
    }

    @Test
    @DisplayName("criterio 3: un nombre de 1 o de 61 caracteres devuelve 400 con el campo")
    void validatesNameLength() throws Exception {
        create("A")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        create("x".repeat(61)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("criterio 4: lista todas ordenadas por nombre")
    void listsOrderedByName() throws Exception {
        create("snacks");
        create("Bebidas");
        create("lácteos");

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Bebidas"))
                .andExpect(jsonPath("$[1].name").value("lácteos"))
                .andExpect(jsonPath("$[2].name").value("snacks"));
    }

    @Test
    @DisplayName("criterios 5 y 6: renombrar y desactivar; sigue en el listado como inactiva")
    void renamesAndDeactivates() throws Exception {
        String id = idOf(create("Limpieza"));

        update(id, """
                {"name": "Aseo", "active": false}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aseo"))
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get(URL))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    @DisplayName("criterio 5: id inexistente devuelve 404 y nombre ocupado devuelve 409")
    void updateErrors() throws Exception {
        create("Bebidas");
        String snacks = idOf(create("Snacks"));

        update(UUID.randomUUID().toString(), """
                {"name": "Otra"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Categoría no encontrada"));
        update(snacks, """
                {"name": "bebidas"}""")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("el índice único de la base frena duplicados aunque se salte el servicio")
    void databaseEnforcesUniqueness() {
        repository.save(Category.create("Bebidas"));

        assertThatThrownBy(() -> repository.save(Category.create("BEBIDAS")))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    private ResultActions create(String name) throws Exception {
        return mvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\"}"));
    }

    private ResultActions update(String id, String body) throws Exception {
        return mvc.perform(put(URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
