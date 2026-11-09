package com.kardexa.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-01 de punta a punta: HTTP → caso de uso → JPA → PostgreSQL real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("API del registro inicial contra PostgreSQL (HU-01)")
class AuthSetupApiIntegrationTest {

    private static final String URL = "/api/v1/auth/setup";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE app_user");
    }

    @Test
    @DisplayName("criterio 1: sin usuarios crea un ADMIN y no devuelve la contraseña")
    void createsAdmin() throws Exception {
        setup("Ana Pérez", "Ana@Correo.com", "secreta123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("ana@correo.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("criterio 2: con al menos un usuario responde 403")
    void closedAfterFirstUser() throws Exception {
        setup("Ana Pérez", "ana@correo.com", "secreta123").andExpect(status().isCreated());

        setup("Luis Gómez", "luis@correo.com", "otraclave123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("RF-01"));
    }

    @Test
    @DisplayName("criterio 3: una contraseña de menos de 8 caracteres responde 400 junto al campo")
    void rejectsShortPassword() throws Exception {
        setup("Ana Pérez", "ana@correo.com", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("criterio 4: la contraseña se guarda con BCrypt, nunca en texto plano")
    void storesBcryptHash() throws Exception {
        setup("Ana Pérez", "ana@correo.com", "secreta123").andExpect(status().isCreated());

        String hash = jdbc.queryForObject("SELECT password_hash FROM app_user", String.class);

        assertThat(hash).startsWith("$2").isNotEqualTo("secreta123");
        assertThat(encoder.matches("secreta123", hash)).isTrue();
    }

    private ResultActions setup(String fullName, String email, String password) throws Exception {
        String body = """
                {"fullName": "%s", "email": "%s", "password": "%s"}""".formatted(fullName, email, password);
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
