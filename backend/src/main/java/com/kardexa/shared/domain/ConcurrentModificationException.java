package com.kardexa.shared.domain;

/**
 * Otro usuario cambió los datos desde que se leyeron. Quien abre la transacción puede repetir la
 * operación completa; si el conflicto persiste, la API responde 409 (ADR 0006 y ADR 0009).
 */
public abstract class ConcurrentModificationException extends ConflictException {

    protected ConcurrentModificationException(String rule, String message) {
        super(rule, message);
    }
}
