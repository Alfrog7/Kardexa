package com.kardexa.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestcontainersConfiguration;
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

/** HU-03 de punta a punta: gestión de usuarios con autorización por rol. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("API de usuarios contra PostgreSQL (HU-03)")
class UserApiIntegrationTest {

    private static final String BASE = "/api/v1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String adminToken;
    private String adminId;

    @BeforeEach
    void createAdmin() throws Exception {
        jdbc.execute("TRUNCATE app_user CASCADE");
        String setup = """
                {"fullName": "Ana Pérez", "email": "ana@correo.com", "password": "secreta123"}""";
        ResultActions created = json(post(BASE + "/auth/setup"), setup, null).andExpect(status().isCreated());
        adminId = JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id");
        adminToken = loginToken("ana@correo.com", "secreta123");
    }

    @Test
    @DisplayName("criterio 1: un ADMIN crea un EMPLOYEE y este puede iniciar sesión")
    void adminCreatesEmployeeWhoCanLogIn() throws Exception {
        createEmployee("luis@correo.com").andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        String token = loginToken("luis@correo.com", "clave12345");
        mvc.perform(get(BASE + "/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("criterio 2: un correo ya registrado devuelve 409")
    void rejectsDuplicateEmail() throws Exception {
        createEmployee("luis@correo.com").andExpect(status().isCreated());

        createEmployee("LUIS@correo.com").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("criterio 3: el último ADMIN activo no puede desactivarse ni cambiar de rol (RN-11)")
    void lastAdminIsProtected() throws Exception {
        json(patch(BASE + "/users/" + adminId), "{\"active\": false}", adminToken)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("RN-11"));
        json(patch(BASE + "/users/" + adminId), "{\"role\": \"EMPLOYEE\"}", adminToken)
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("criterio 4: un EMPLOYEE recibe 403 en los endpoints de usuarios")
    void employeeIsForbidden() throws Exception {
        createEmployee("luis@correo.com").andExpect(status().isCreated());
        String token = loginToken("luis@correo.com", "clave12345");

        mvc.perform(get(BASE + "/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        json(post(BASE + "/users"), employeeBody("otro@correo.com"), token).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sin token los endpoints de usuarios devuelven 401")
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get(BASE + "/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("el ADMIN lista usuarios con paginación y desactiva a un empleado")
    void listsAndDeactivates() throws Exception {
        ResultActions created = createEmployee("luis@correo.com").andExpect(status().isCreated());
        String employeeId = JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get(BASE + "/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));

        json(patch(BASE + "/users/" + employeeId), "{\"active\": false}", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        json(post(BASE + "/auth/login"), "{\"email\": \"luis@correo.com\", \"password\": \"clave12345\"}", null)
                .andExpect(status().isUnauthorized());
    }

    private ResultActions createEmployee(String email) throws Exception {
        return json(post(BASE + "/users"), employeeBody(email), adminToken);
    }

    private static String employeeBody(String email) {
        return """
                {"fullName": "Luis Gómez", "email": "%s", "password": "clave12345", "role": "EMPLOYEE"}"""
                .formatted(email);
    }

    private String loginToken(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
        ResultActions login = json(post(BASE + "/auth/login"), body, null).andExpect(status().isOk());
        return JsonPath.read(login.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private ResultActions json(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body, String token)
            throws Exception {
        request.contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }
}
