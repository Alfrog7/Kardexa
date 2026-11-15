package com.kardexa.reporting.application.port.out;

import com.kardexa.reporting.domain.model.PaymentTotals;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SiteSales;
import java.util.List;
import java.util.UUID;

/** Cifras de turnos y sedes agregadas por la base de datos (HU-24). */
public interface ShiftFigures {

    /** Lo recibido por medio de pago en los turnos cerrados del rango; {@code siteId} nulo: todas las sedes. */
    PaymentTotals payments(ReportPeriod period, UUID siteId);

    /** Una fila por sede, con lo vendido y la diferencia de sus turnos cerrados en el rango. */
    List<SiteSales> bySite(ReportPeriod period);
}
