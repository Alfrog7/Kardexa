package com.kardexa.sites.application.port.in;

import com.kardexa.sites.domain.model.Shift;
import java.util.UUID;

/**
 * HU-22 · Quien lleva la caja abre el turno de su sede. Guarda una foto del stock de cada producto al abrir; como
 * máximo hay un turno abierto por sede (RN-14).
 */
public interface OpenShiftUseCase {

    Shift open(UUID siteId, UUID responsibleId);
}
