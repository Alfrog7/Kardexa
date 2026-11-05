package com.kardexa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API de Kardexa.
 *
 * <p>Monolito modular con arquitectura hexagonal por módulo (ADR 0002).
 */
@SpringBootApplication
public class KardexaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KardexaApplication.class, args);
    }
}
