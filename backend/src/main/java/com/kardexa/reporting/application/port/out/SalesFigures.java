package com.kardexa.reporting.application.port.out;

import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.TopProduct;
import java.util.List;

/** Cifras de ventas agregadas por la base de datos. */
public interface SalesFigures {

    /** Un elemento por cada día del rango, en orden, con total 0 los días sin ventas registradas. */
    List<DailySales> dailySales(ReportPeriod period);

    /**
     * Hasta {@code limit} productos con ventas registradas en el rango, por cantidad de mayor a menor; en empate,
     * por monto de mayor a menor y luego por nombre. Lista vacía si no hubo ventas.
     */
    List<TopProduct> topProducts(ReportPeriod period, int limit);
}
