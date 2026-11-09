package com.kardexa.inventory.application.port.in;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Resuelve el nombre de quien hizo cada movimiento, para mostrarlo en el kárdex. */
public interface ResolveUserNamesUseCase {

    Map<UUID, String> namesOf(Collection<UUID> userIds);
}
