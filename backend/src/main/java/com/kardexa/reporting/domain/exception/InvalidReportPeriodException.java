package com.kardexa.reporting.domain.exception;

import com.kardexa.shared.domain.DomainException;

/** Rango de fechas inválido para un indicador o reporte (ADR 0012). */
public class InvalidReportPeriodException extends DomainException {

    public InvalidReportPeriodException(String message) {
        super("RF-22", message);
    }

    @Override
    public String title() {
        return "Rango de fechas inválido";
    }
}
