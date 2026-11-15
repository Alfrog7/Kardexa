package com.kardexa.sites.application.port.in;

import java.util.UUID;

/**
 * HU-19 · Eliminar una sede que nunca se usó. Una sede con stock, ventas, compras, movimientos o trabajadores
 * no se borra: se inhabilita para conservar su historial.
 */
public interface DeleteSiteUseCase {

    void delete(UUID id);
}
