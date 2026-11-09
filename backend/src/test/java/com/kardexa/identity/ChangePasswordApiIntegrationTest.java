package com.kardexa.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** HU-04 de punta a punta: cambio de la contraseña propia. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("API de cambio de contraseña contra PostgreSQL (HU-04)")
class ChangePasswordApiIntegrationTest {

    private static final String BASE = "/api/v1/auth";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void createAdminAndLogin() throws Exception {
        jdbc.execute("TRUNCATE app_user");
        post("setup", "{\"fullName\": \"Ana Pérez\", \"email\": \"ana@correo.com\", \"password\": \"secreta123\"}", null)
                .andExpect(status().isCreated());
        token = login("secreta123").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = JsonPath.read(token, "$.accessToken");
    }

    @Test
    @DisplayName("criterio 1: con la actual correcta y una nueva válida, la contraseña cambia")
    void changesPassword() throws Exception {
        change("secreta123", "nuevaclave99").andExpect(status().isNoContent());

        login("nuevaclave99").andExpect(status().isOk());
        login("secreta123").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("criterio 2: con la actual incorrecta, el cambio se rechaza")
    void rejectsWrongCurrentPassword() throws Exception {
        change("equivocada1", "nuevaclave99")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("RF-05"));

        login("secreta123").andExpect(status().isOk());
    }

    @Test
    @DisplayName("criterio 3: una nueva contraseña inválida muestra el error junto al campo")
    void rejectsInvalidNewPassword() throws Exception {
        change("secreta123", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
    }

    @Test
    @DisplayName("sin token responde 401")
    void requiresToken() throws Exception {
        mvc.perform(put(BASE + "/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"secreta123\", \"newPassword\": \"nuevaclave99\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions change(String current, String next) throws Exception {
        return mvc.perform(put(BASE + "/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\": \"%s\", \"newPassword\": \"%s\"}".formatted(current, next)));
    }

    private ResultActions login(String password) throws Exception {
        return post("login", "{\"email\": \"ana@correo.com\", \"password\": \"%s\"}".formatted(password), null);
    }

    private ResultActions post(String path, String body, String bearer) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(BASE + "/" + path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mvc.perform(request);
    }
}
