package com.kardexa.sites;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.shared.domain.MainSite;
import com.kardexa.sites.domain.model.Site;
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

/** HU-19 de punta a punta: HTTP → caso de uso → JPA → PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurity.class})
@DisplayName("API de sedes contra PostgreSQL (HU-19)")
class SiteApiIntegrationTest {

    private static final String URL = "/api/v1/sites";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenIssuer tokens;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM site WHERE id <> ?", Site.MAIN_SITE_ID);
    }

    @Test
    @DisplayName("la migración crea la Sede principal habilitada")
    void mainSiteExists() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(Site.MAIN_SITE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Sede principal"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("crear devuelve 201, Location y la sede habilitada")
    void createsSite() throws Exception {
        ResultActions result = create("Galeno", "Av. Larco 123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Galeno"))
                .andExpect(jsonPath("$.address").value("Av. Larco 123"))
                .andExpect(jsonPath("$.active").value(true));

        result.andExpect(header().string("Location", endsWith(URL + "/" + idOf(result))));
        mvc.perform(get(URL)).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("criterio 1: un nombre repetido, aunque cambie la mayúscula, devuelve 409")
    void rejectsDuplicateName() throws Exception {
        create("Galeno", null).andExpect(status().isCreated());

        create("GALENO", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Nombre de sede duplicado"));
        create("sede PRINCIPAL", null).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un nombre vacío o demasiado corto devuelve 400 con el campo")
    void rejectsInvalidName() throws Exception {
        send(post(URL), "{\"name\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        create("A", null).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("se edita el nombre y la dirección; el de otra sede devuelve 409 y un id inexistente 404")
    void updates() throws Exception {
        String galeno = idOf(create("Galeno", null));
        create("Monserrate", null);

        send(put(URL + "/" + galeno), "{\"name\": \"Galeno Norte\", \"address\": \"Av. España 100\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Galeno Norte"))
                .andExpect(jsonPath("$.address").value("Av. España 100"));
        send(put(URL + "/" + galeno), "{\"name\": \"monserrate\"}").andExpect(status().isConflict());
        send(put(URL + "/" + UUID.randomUUID()), "{\"name\": \"Nadie\"}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("criterio 2: inhabilitar y volver a habilitar conserva la sede en la lista")
    void changesStatus() throws Exception {
        String galeno = idOf(create("Galeno", null));

        send(patch(URL + "/" + galeno), "{\"active\": false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get(URL))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Galeno"))
                .andExpect(jsonPath("$[0].active").value(false));
        send(patch(URL + "/" + galeno), "{\"active\": true}").andExpect(jsonPath("$.active").value(true));
        send(patch(URL + "/" + galeno), "{}").andExpect(status().isBadRequest());
        send(patch(URL + "/" + UUID.randomUUID()), "{\"active\": false}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el negocio conserva al menos una sede habilitada")
    void keepsOneActiveSite() throws Exception {
        String galeno = idOf(create("Galeno", null));

        send(patch(URL + "/" + MainSite.ID), "{\"active\": false}").andExpect(status().isOk());
        send(patch(URL + "/" + galeno), "{\"active\": false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("al menos una sede")));
        send(patch(URL + "/" + MainSite.ID), "{\"active\": true}").andExpect(jsonPath("$.active").value(true));
        send(patch(URL + "/" + galeno), "{\"active\": false}").andExpect(status().isOk());
    }

    @Test
    @DisplayName("criterio 3: un EMPLOYEE recibe 403 al consultar, crear o editar, y sin token 401")
    void employeeIsForbidden() throws Exception {
        String employee = "Bearer " + TestSecurity.token(tokens, Role.EMPLOYEE);
        String galeno = idOf(create("Galeno", null));

        mvc.perform(get(URL).header("Authorization", employee)).andExpect(status().isForbidden());
        mvc.perform(post(URL).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Otra\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(URL + "/" + galeno).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Otra\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(URL + "/" + galeno).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\": false}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL).header("Authorization", "")).andExpect(status().isUnauthorized());
    }

    private ResultActions create(String name, String address) throws Exception {
        String body = address == null
                ? "{\"name\": \"%s\"}".formatted(name)
                : "{\"name\": \"%s\", \"address\": \"%s\"}".formatted(name, address);
        return send(post(URL), body);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
