package com.kardexa.purchasing.domain.model;

import com.kardexa.purchasing.domain.exception.InvalidRucException;
import com.kardexa.purchasing.domain.exception.InvalidSupplierDataException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Empresa a la que se le compra mercadería (HU-11).
 *
 * <p>Un proveedor nunca se borra: se desactiva, para que sus compras lo conserven y deje de ofrecerse
 * al registrar compras nuevas.
 */
public final class Supplier {

    public static final int RUC_LENGTH = 11;
    public static final int BUSINESS_NAME_MIN_LENGTH = 2;
    public static final int BUSINESS_NAME_MAX_LENGTH = 150;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int EMAIL_MAX_LENGTH = 254;

    private static final Pattern RUC = Pattern.compile("\\d{" + RUC_LENGTH + "}");

    private final UUID id;
    private Details details;
    private boolean active;

    /** Datos editables del proveedor; {@code phone} y {@code email} son opcionales. */
    public record Details(String ruc, String businessName, String phone, String email) {

        public Details {
            ruc = normalizeRuc(ruc);
            businessName = normalizeBusinessName(businessName);
            phone = optional(phone, PHONE_MAX_LENGTH, "El teléfono admite hasta %d caracteres.");
            email = optional(email, EMAIL_MAX_LENGTH, "El correo admite hasta %d caracteres.");
        }
    }

    private Supplier(UUID id, Details details, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.details = Objects.requireNonNull(details, "details");
        this.active = active;
    }

    /** Crea un proveedor nuevo, activo y con identificador propio. */
    public static Supplier create(Details details) {
        return new Supplier(UUID.randomUUID(), details, true);
    }

    /** Reconstruye un proveedor ya guardado (lo usa el adaptador de persistencia). */
    public static Supplier restore(UUID id, Details details, boolean active) {
        return new Supplier(id, details, active);
    }

    /** RN-10: el RUC son exactamente 11 dígitos; se ignoran los espacios de los extremos. */
    public static String normalizeRuc(String ruc) {
        String normalized = ruc == null ? "" : ruc.strip();
        if (!RUC.matcher(normalized).matches()) {
            throw new InvalidRucException();
        }
        return normalized;
    }

    /** Quita los espacios de los extremos y junta los repetidos. */
    public static String normalizeBusinessName(String businessName) {
        String normalized = businessName == null ? "" : businessName.strip().replaceAll("\\s+", " ");
        if (normalized.length() < BUSINESS_NAME_MIN_LENGTH || normalized.length() > BUSINESS_NAME_MAX_LENGTH) {
            throw new InvalidSupplierDataException("La razón social debe tener entre %d y %d caracteres."
                    .formatted(BUSINESS_NAME_MIN_LENGTH, BUSINESS_NAME_MAX_LENGTH));
        }
        return normalized;
    }

    /** Un texto opcional en blanco se guarda como ausente. */
    private static String optional(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new InvalidSupplierDataException(message.formatted(maxLength));
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

    public String ruc() {
        return details.ruc();
    }

    public String businessName() {
        return details.businessName();
    }

    public boolean isActive() {
        return active;
    }
}
