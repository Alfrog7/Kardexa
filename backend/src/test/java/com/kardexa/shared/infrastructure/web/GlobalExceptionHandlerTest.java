package com.kardexa.shared.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kardexa.shared.domain.DomainException;
import com.kardexa.shared.infrastructure.config.WebConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.FakeController.class)
@Import({GlobalExceptionHandler.class, WebConfig.class, GlobalExceptionHandlerTest.FakeController.class})
@DisplayName("Errores en formato Problem Details (RNF-10)")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("una regla de negocio violada responde 422 con la regla")
    void domainExceptionIsUnprocessable() throws Exception {
        mvc.perform(get("/api/v1/fake/stock"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Stock insuficiente"))
                .andExpect(jsonPath("$.detail").value("Stock insuficiente para \"Arroz 5 kg\": disponible 2, solicitado 3."))
                .andExpect(jsonPath("$.rule").value("RN-01"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("un campo inválido responde 400 con la lista de errores")
    void validationErrorIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/fake/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("El nombre es obligatorio"));
    }

    @Test
    @DisplayName("los controladores quedan bajo /api/v1")
    void controllersAreVersioned() throws Exception {
        mvc.perform(get("/fake/stock")).andExpect(status().isNotFound());
    }

    static final class InsufficientStockException extends DomainException {
        InsufficientStockException() {
            super("RN-01", "Stock insuficiente para \"Arroz 5 kg\": disponible 2, solicitado 3.");
        }

        @Override
        public String title() {
            return "Stock insuficiente";
        }
    }

    record FakeProductRequest(@NotBlank(message = "El nombre es obligatorio") String name) {}

    @RestController
    static class FakeController {

        @GetMapping("/fake/stock")
        void stock() {
            throw new InsufficientStockException();
        }

        @PostMapping("/fake/products")
        void create(@Valid @RequestBody FakeProductRequest request) {}
    }
}
