package com.kardexa.catalog.domain.model;

import com.kardexa.catalog.domain.exception.InvalidCategoryNameException;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrupa productos para encontrarlos y analizarlos (HU-05).
 *
 * <p>Una categoría nunca se borra: se desactiva, para que los productos que ya la usan la conserven
 * y deje de ofrecerse al registrar productos nuevos.
 */
public final class Category {

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 60;

    private final UUID id;
    private String name;
    private boolean active;

    private Category(UUID id, String name, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = normalizeName(name);
        this.active = active;
    }

    /** Crea una categoría nueva, activa y con identificador propio. */
    public static Category create(String name) {
        return new Category(UUID.randomUUID(), name, true);
    }

    /** Reconstruye una categoría ya guardada (lo usa el adaptador de persistencia). */
    public static Category restore(UUID id, String name, boolean active) {
        return new Category(id, name, active);
    }

    /**
     * Quita los espacios de los extremos y junta los espacios repetidos, de modo que
     * {@code "  Bebidas   frías "} y {@code "Bebidas frías"} sean el mismo nombre.
     */
    public static String normalizeName(String name) {
        if (name == null) {
            throw new InvalidCategoryNameException();
        }
        String normalized = name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < NAME_MIN_LENGTH || normalized.length() > NAME_MAX_LENGTH) {
            throw new InvalidCategoryNameException();
        }
        return normalized;
    }

    public void rename(String newName) {
        this.name = normalizeName(newName);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean isActive() {
        return active;
    }
}
