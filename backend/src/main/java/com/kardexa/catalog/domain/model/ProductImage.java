package com.kardexa.catalog.domain.model;

import com.kardexa.catalog.domain.exception.InvalidProductImageException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Imagen de un producto: PNG, JPEG o WebP de hasta 2 MB. El tipo se comprueba con los primeros bytes
 * del archivo, no con lo que declara el cliente.
 */
public record ProductImage(String contentType, byte[] data, Instant updatedAt) {

    public static final int MAX_BYTES = 2 * 1024 * 1024;

    public ProductImage {
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(data, "data");
    }

    /** Valida el archivo y detecta su tipo real. */
    public static ProductImage of(byte[] data, Instant updatedAt) {
        if (data == null || data.length == 0) {
            throw new InvalidProductImageException("Elige una imagen.");
        }
        if (data.length > MAX_BYTES) {
            throw new InvalidProductImageException("La imagen debe pesar como máximo 2 MB.");
        }
        String type = detectType(data);
        if (type == null) {
            throw new InvalidProductImageException("La imagen debe ser PNG, JPG o WebP.");
        }
        return new ProductImage(type, data, updatedAt);
    }

    private static String detectType(byte[] d) {
        if (startsWith(d, 0x89, 0x50, 0x4E, 0x47)) {
            return "image/png";
        }
        if (startsWith(d, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        // RIFF....WEBP
        if (d.length >= 12 && startsWith(d, 0x52, 0x49, 0x46, 0x46)
                && d[8] == 0x57 && d[9] == 0x45 && d[10] == 0x42 && d[11] == 0x50) {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProductImage image && contentType.equals(image.contentType)
                && Arrays.equals(data, image.data) && Objects.equals(updatedAt, image.updatedAt);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(contentType, updatedAt) + Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "ProductImage[%s, %d bytes]".formatted(contentType, data.length);
    }
}
