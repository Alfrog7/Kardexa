package com.kardexa.identity.application.port.out;

/** Puerto de salida: convierte una contraseña en un hash irreversible (RNF-01). */
public interface PasswordHasher {

    String hash(String rawPassword);
}
