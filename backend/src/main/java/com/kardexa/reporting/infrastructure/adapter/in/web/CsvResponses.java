package com.kardexa.reporting.infrastructure.adapter.in.web;

import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.shared.domain.BusinessCalendar;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Piezas comunes de las descargas CSV (ADR 0012): cabeceras, formato de fecha y validación del rango. */
final class CsvResponses {

    static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    /** {@code AAAA-MM-DD HH:mm} en hora de Lima. */
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(BusinessCalendar.ZONE);

    private CsvResponses() {
    }

    static ResponseEntity<StreamingResponseBody> csv(String filename, StreamingResponseBody body) {
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build()
                        .toString())
                .body(body);
    }

    static boolean validRange(LocalDate from, LocalDate to) {
        return from == null || to == null || !from.isAfter(to);
    }

    static boolean withinLimit(LocalDate from, LocalDate to) {
        return from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) + 1 <= ReportPeriod.MAX_DAYS;
    }
}
