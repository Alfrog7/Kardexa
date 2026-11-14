package com.kardexa.reporting.application.port.out;

import com.kardexa.reporting.domain.model.DailySales;
import com.kardexa.reporting.domain.model.ReportPeriod;
import java.util.List;

/** Cifras de ventas agregadas por la base de datos. */
public interface SalesFigures {

    /** Un elemento por cada día del rango, en orden, con total 0 los días sin ventas registradas. */
    List<DailySales> dailySales(ReportPeriod period);
}
