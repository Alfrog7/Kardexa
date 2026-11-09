package com.kardexa.identity.domain.model;

import com.kardexa.identity.domain.exception.InvalidUserException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Persona que usa el sistema. Guarda solo el hash de la contraseña (RNF-01), nunca el texto. */
public final class User {

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 120;
    public static final int EMAIL_MAX_LENGTH = 254;

    private final UUID id;
    private String fullName;
    private final String email;
    private String passwordHash;
    private Role role;
    private boolean active;

    private User(UUID id, String fullName, String email, String passwordHash, Role role, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.fullName = normalizeName(fullName);
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.active = active;
    }

    /** Crea un usuario nuevo y activo. */
    public static User create(String fullName, String email, String passwordHash, Role role) {
        return new User(UUID.randomUUID(), fullName, email, passwordHash, role, true);
    }

    /** Reconstruye un usuario ya guardado (lo usa el adaptador de persistencia). */
    public static User restore(
            UUID id, String fullName, String email, String passwordHash, Role role, boolean active) {
        return new User(id, fullName, email, passwordHash, role, active);
    }

    /** Normaliza un correo (sin espacios en los extremos y en minúsculas) para compararlo o guardarlo. */
    public static String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > EMAIL_MAX_LENGTH || !normalized.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new InvalidUserException("El correo no es válido.");
        }
        return normalized;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < NAME_MIN_LENGTH || normalized.length() > NAME_MAX_LENGTH) {
            throw new InvalidUserException(
                    "El nombre debe tener entre %d y %d caracteres.".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        }
        return normalized;
    }

    public void rename(String newName) {
        this.fullName = normalizeName(newName);
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "role");
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "passwordHash");
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /** {@code true} si hoy cuenta como administrador activo (RN-11). */
    public boolean isActiveAdmin() {
        return active && role == Role.ADMIN;
    }

    public UUID id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
