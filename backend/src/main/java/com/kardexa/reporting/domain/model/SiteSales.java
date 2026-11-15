package com.kardexa.reporting.domain.model;

import com.kardexa.shared.domain.Money;
import java.util.UUID;

/**
 * Cómo le fue a una sede en el rango: ventas registradas y la suma de las diferencias de sus turnos cerrados
 * (negativa si en total faltó dinero).
 */
public record SiteSales(UUID siteId, String siteName, Money total, long count, Money difference) {}
