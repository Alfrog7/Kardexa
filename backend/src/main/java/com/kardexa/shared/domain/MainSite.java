package com.kardexa.shared.domain;

import java.util.UUID;

/**
 * La «Sede principal» que crea la migración V14 y que recibe todo lo anterior a la Fase 8 (ADR 0013).
 * Mientras un usuario no tenga sede asignada (HU-20) y el panel no la elija (selector de sede), las
 * operaciones caen en ella.
 */
public final class MainSite {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private MainSite() {
    }
}
