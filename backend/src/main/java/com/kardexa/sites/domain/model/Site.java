package com.kardexa.sites.domain.model;

import com.kardexa.sites.domain.exception.InvalidSiteDataException;
import java.util.Objects;
import java.util.UUID;

/**
 * Un local del negocio con su propio stock y kárdex (HU-19, RN-13).
 *
 * <p>Una sede nunca se borra: se inhabilita, para conservar su historial y dejar de ofrecerla al operar.
 */
public final class Site {

    /** Sede que crea la migración V14 con todo lo que existía antes de la Fase 8. */
    public static final UUID MAIN_SITE_ID = com.kardexa.shared.domain.MainSite.ID;

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 80;
    public static final int ADDRESS_MAX_LENGTH = 200;

    private final UUID id;
    private Details details;
    private boolean active;

    /** Datos editables de la sede; la dirección es opcional. */
    public record Details(String name, String address) {

        public Details {
            name = normalizeName(name);
            address = normalizeAddress(address);
        }
    }

    private Site(UUID id, Details details, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.details = Objects.requireNonNull(details, "details");
        this.active = active;
    }

    /** Crea una sede nueva, activa y con identificador propio. */
    public static Site create(Details details) {
        return new Site(UUID.randomUUID(), details, true);
    }

    /** Reconstruye una sede ya guardada (lo usa el adaptador de persistencia). */
    public static Site restore(UUID id, Details details, boolean active) {
        return new Site(id, details, active);
    }

    /** Quita los espacios de los extremos y junta los repetidos. */
    public static String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < NAME_MIN_LENGTH || normalized.length() > NAME_MAX_LENGTH) {
            throw new InvalidSiteDataException("El nombre debe tener entre %d y %d caracteres."
                    .formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        }
        return normalized;
    }

    /** Una dirección en blanco se guarda como ausente. */
    public static String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String normalized = address.strip().replaceAll("\\s+", " ");
        if (normalized.length() > ADDRESS_MAX_LENGTH) {
            throw new InvalidSiteDataException("La dirección admite hasta %d caracteres.".formatted(ADDRESS_MAX_LENGTH));
        }
        return normalized;
    }

    public void update(Details newDetails) {
        this.details = Objects.requireNonNull(newDetails, "details");
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

    public Details details() {
        return details;
    }

    public String name() {
        return details.name();
    }

    public boolean isActive() {
        return active;
    }
}
