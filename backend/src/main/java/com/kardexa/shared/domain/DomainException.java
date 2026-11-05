package com.kardexa.shared.domain;

/**
 * Violación de una regla de negocio.
 *
 * <p>Cada subclase identifica la regla que protege (por ejemplo, {@code RN-01}). El adaptador web la
 * traduce a una respuesta {@code 422} en formato Problem Details con el campo {@code rule}.
 */
public abstract class DomainException extends RuntimeException {

    private final String rule;

    protected DomainException(String rule, String message) {
        super(message);
        this.rule = rule;
    }

    /** Identificador de la regla de negocio violada, por ejemplo {@code RN-01}. */
    public String rule() {
        return rule;
    }

    /** Título corto y legible del error, en español. */
    public abstract String title();
}
