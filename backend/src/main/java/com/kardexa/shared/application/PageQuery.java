package com.kardexa.shared.application;

/**
 * Página pedida por un caso de uso de consulta: número de página (desde 0) y tamaño. Es Java puro
 * para que los puertos no dependan de los tipos de paginación de Spring.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa: " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + MAX_SIZE + ": " + size);
        }
    }

    /** Usa la primera página y el tamaño por defecto cuando el valor no llega. */
    public static PageQuery of(Integer page, Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public long offset() {
        return (long) page * size;
    }
}
