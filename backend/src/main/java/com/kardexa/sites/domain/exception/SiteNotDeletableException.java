package com.kardexa.sites.domain.exception;

import com.kardexa.shared.domain.ConflictException;

/** La sede no se puede eliminar: tiene historial o trabajadores, o es la «Sede principal». */
public class SiteNotDeletableException extends ConflictException {

    private SiteNotDeletableException(String message) {
        super("RF-26", message);
    }

    public static SiteNotDeletableException inUse() {
        return new SiteNotDeletableException(
                "La sede tiene stock, ventas, compras o trabajadores: inhabilítala para conservar su historial.");
    }

    public static SiteNotDeletableException main() {
        return new SiteNotDeletableException(
                "La Sede principal no se elimina: puedes cambiarle el nombre o inhabilitarla.");
    }

    @Override
    public String title() {
        return "La sede no se puede eliminar";
    }
}
