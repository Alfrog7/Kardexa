package com.kardexa.inventory.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Nombres de los usuarios que generaron movimientos. Lo implementa un adaptador de lectura (ADR 0007). */
public interface UserNames {

    /** Nombre completo por identificador; los que no existan no aparecen en el mapa. */
    Map<UUID, String> namesOf(Collection<UUID> userIds);
}
