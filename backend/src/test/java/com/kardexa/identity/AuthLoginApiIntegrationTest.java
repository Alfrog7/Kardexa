package com.kardexa.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kardexa.TestcontainersConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-02 de punta a punta: login, token y {@code /auth/me} contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("API de inicio de sesión contra PostgreSQL (HU-02)")
class AuthLoginApiIntegrationTest {

    private static final String BASE = "/api/v1/auth";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void createAdmin() throws Exception {
        jdbc.execute("TRUNCATE app_user CASCADE");
        String body = """
                {"fullName": "Ana Pérez", "email": "ana@correo.com", "password": "secreta123"}""";
        mvc.perform(post(BASE + "/setup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("criterio 1: credenciales correctas devuelven un token que abre /auth/me")
    void logsInAndReadsMe() throws Exception {
        ResultActions login = login("ANA@correo.com", "secreta123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
        String token = JsonPath.read(login.andReturn().getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get(BASE + "/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@correo.com"));
    }

    @Test
    @DisplayName("criterio 2: credenciales incorrectas devuelven 401 con un mensaje genérico")
    void rejectsBadCredentials() throws Exception {
        login("ana@correo.com", "equivocada1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
        login("nadie@correo.com", "secreta123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    @DisplayName("criterio 3: un usuario desactivado no puede iniciar sesión")
    void rejectsInactiveUser() throws Exception {
        jdbc.update("UPDATE app_user SET active = false");

        login("ana@correo.com", "secreta123").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("criterio 4: un token vencido recibe 401 (RNF-02)")
    void rejectsExpiredToken() throws Exception {
        String userId = jdbc.queryForObject("SELECT id::text FROM app_user", String.class);
        Instant past = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("role", "ADMIN")
                .issuedAt(past.minusSeconds(3600))
                .expiresAt(past)
                .build();
        String expired = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get(BASE + "/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("sin token o con un token inválido, /auth/me devuelve 401")
    void rejectsMissingOrInvalidToken() throws Exception {
        mvc.perform(get(BASE + "/me")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
        return mvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
