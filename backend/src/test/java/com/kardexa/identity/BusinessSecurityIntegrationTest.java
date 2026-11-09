package com.kardexa.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kardexa.TestSecurity;
import com.kardexa.TestcontainersConfiguration;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.Role;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Permisos de los endpoints de negocio (RN-05, RNF-03): sin sesión, empleado y administrador. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("Permisos del catálogo y el inventario (RN-05, RNF-03)")
class BusinessSecurityIntegrationTest {

    private static final String API = "/api/v1";
    private static final String ANY_ID = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenIssuer tokens;

    private String bearer(Role role) {
        return "Bearer " + TestSecurity.token(tokens, role);
    }

    @Test
    @DisplayName("criterio 1: sin token, cualquier endpoint de negocio responde 401")
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get(API + "/categories")).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/products")).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/products/" + ANY_ID + "/kardex")).andExpect(status().isUnauthorized());
        mvc.perform(post(API + "/stock-adjustments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("criterio 2: un EMPLOYEE recibe 403 al crear, editar o ajustar")
    void employeeCannotWrite() throws Exception {
        String employee = bearer(Role.EMPLOYEE);

        mvc.perform(post(API + "/categories").header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Bebidas\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(API + "/categories/" + ANY_ID).header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Bebidas\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(API + "/products").header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(API + "/stock-adjustments").header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("criterio 2: un EMPLOYEE puede consultar el catálogo y el kárdex")
    void employeeCanRead() throws Exception {
        String employee = bearer(Role.EMPLOYEE);

        mvc.perform(get(API + "/categories").header("Authorization", employee)).andExpect(status().isOk());
        mvc.perform(get(API + "/products").header("Authorization", employee)).andExpect(status().isOk());
        mvc.perform(get(API + "/products/" + ANY_ID + "/kardex").header("Authorization", employee))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Swagger y el health siguen públicos")
    void publicEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
