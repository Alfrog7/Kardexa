package com.kardexa.purchasing;

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

/** HU-11 de punta a punta: HTTP → caso de uso → JPA → PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API de proveedores contra PostgreSQL (HU-11)")
class SupplierApiIntegrationTest {

    private static final String URL = "/api/v1/suppliers";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE supplier");
    }

    @Test
    @DisplayName("criterio 1: un RUC válido y nuevo devuelve 201, Location y el proveedor activo")
    void createsSupplier() throws Exception {
        ResultActions result = create("20123456789", "Distribuidora Lima")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruc").value("20123456789"))
                .andExpect(jsonPath("$.businessName").value("Distribuidora Lima"))
                .andExpect(jsonPath("$.active").value(true));

        result.andExpect(header().string("Location", endsWith(URL + "/" + idOf(result))));
        mvc.perform(get(URL)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("criterio 2: un RUC ya registrado devuelve 409 (RN-10)")
    void rejectsDuplicateRuc() throws Exception {
        create("20123456789", "Distribuidora Lima").andExpect(status().isCreated());

        create("20123456789", "Otra empresa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("RUC duplicado"))
                .andExpect(jsonPath("$.rule").value("RN-10"));
    }

    @Test
    @DisplayName("criterio 2: un RUC con formato inválido devuelve 400 con el campo")
    void rejectsInvalidRuc() throws Exception {
        create("2012345678", "Distribuidora Lima")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("ruc"));
        create("2012345678X", "Distribuidora Lima").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un correo inválido devuelve 400")
    void rejectsInvalidEmail() throws Exception {
        send(post(URL), """
                {"ruc": "20123456789", "businessName": "Distribuidora Lima", "email": "no-es-correo"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("criterio 3: se edita y se desactiva; sigue en la lista como inactivo")
    void updatesAndDeactivates() throws Exception {
        String id = idOf(create("20123456789", "Distribuidora Lima"));

        send(put(URL + "/" + id), """
                {"ruc": "20123456789", "businessName": "Distribuidora Lima SAC",
                 "phone": "987654321", "email": "ventas@dl.pe", "active": false}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Distribuidora Lima SAC"))
                .andExpect(jsonPath("$.phone").value("987654321"))
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get(URL))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    @DisplayName("editar: id inexistente devuelve 404 y el RUC de otro proveedor devuelve 409")
    void updateErrors() throws Exception {
        create("20123456789", "Distribuidora Lima");
        String other = idOf(create("10456789012", "Bodega Central"));

        send(put(URL + "/" + UUID.randomUUID()), """
                {"ruc": "20999999999", "businessName": "Nadie"}""")
                .andExpect(status().isNotFound());
        send(put(URL + "/" + other), """
                {"ruc": "20123456789", "businessName": "Bodega Central"}""")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("la búsqueda filtra por RUC o razón social sin distinguir mayúsculas")
    void searches() throws Exception {
        create("20123456789", "Distribuidora Lima");
        create("10456789012", "Bodega Central");

        mvc.perform(get(URL).param("q", "lima"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ruc").value("20123456789"));
        mvc.perform(get(URL).param("q", "104567"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].businessName").value("Bodega Central"));
        mvc.perform(get(URL).param("q", "%"))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(URL))
                .andExpect(jsonPath("$[0].businessName").value("Bodega Central"))
                .andExpect(jsonPath("$[1].businessName").value("Distribuidora Lima"));
    }

    private ResultActions create(String ruc, String businessName) throws Exception {
        return send(post(URL), """
                {"ruc": "%s", "businessName": "%s"}""".formatted(ruc, businessName));
    }

    private ResultActions send(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
