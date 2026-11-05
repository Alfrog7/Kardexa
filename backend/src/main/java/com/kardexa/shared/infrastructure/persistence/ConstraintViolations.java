package com.kardexa.shared.infrastructure.persistence;

import org.springframework.dao.DataIntegrityViolationException;

/** Identifica qué restricción de la base provocó un {@link DataIntegrityViolationException}. */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    /** {@code true} si alguna causa de la excepción menciona la restricción o índice indicado. */
    public static boolean violates(DataIntegrityViolationException ex, String constraintName) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(constraintName)) {
                return true;
            }
        }
        return false;
    }
}
