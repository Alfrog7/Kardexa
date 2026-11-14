package com.kardexa.reporting.infrastructure.adapter.in.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * CSV que abre bien en Excel en español (ADR 0012): UTF-8 con BOM, separador {@code ;}, fin de línea
 * {@code \r\n}, comillas dobles cuando hace falta y celdas que parecen fórmulas neutralizadas con {@code '}.
 */
final class CsvWriter {

    private static final char BOM = '﻿';
    private static final char SEPARATOR = ';';
    private static final String LINE_END = "\r\n";

    private final Writer out;

    CsvWriter(OutputStream stream) {
        this.out = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
        write(String.valueOf(BOM));
    }

    void row(String... cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                line.append(SEPARATOR);
            }
            line.append(cell(cells[i]));
        }
        write(line.append(LINE_END).toString());
    }

    void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Una celda vacía para {@code null}; con {@code '} delante si empieza como fórmula; entre comillas si hace falta. */
    static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = startsLikeFormula(value) ? "'" + value : value;
        boolean quote = safe.indexOf(SEPARATOR) >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0
                || safe.indexOf('\r') >= 0;
        return quote ? '"' + safe.replace("\"", "\"\"") + '"' : safe;
    }

    private static boolean startsLikeFormula(String value) {
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r';
    }

    private void write(String text) {
        try {
            out.write(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
