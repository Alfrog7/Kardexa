package com.kardexa.shared.domain;

/** Una trabajadora pidió una sede que no es la suya: se responde como si no existiera (RF-21, ADR 0013). */
public class OtherSiteException extends NotFoundException {

    public OtherSiteException() {
        super("RF-21", "No existe esa sede.");
    }

    @Override
    public String title() {
        return "Sede no encontrada";
    }
}
