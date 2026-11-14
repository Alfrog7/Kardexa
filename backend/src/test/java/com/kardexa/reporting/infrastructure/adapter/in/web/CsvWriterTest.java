package com.kardexa.reporting.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CSV para Excel en español (ADR 0012)")
class CsvWriterTest {

    @Test
    @DisplayName("escribe BOM, separa con ; y termina las líneas con CRLF")
    void format() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CsvWriter csv = new CsvWriter(bytes);
        csv.row("Producto", "Saldo");
        csv.row("Azúcar", "1.500");
        csv.flush();

        assertThat(bytes.toString(StandardCharsets.UTF_8)).isEqualTo("﻿Producto;Saldo\r\nAzúcar;1.500\r\n");
    }

    @Test
    @DisplayName("neutraliza las celdas que empiezan como fórmula")
    void formulas() {
        assertThat(CsvWriter.cell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(CsvWriter.cell("+51 999")).isEqualTo("'+51 999");
        assertThat(CsvWriter.cell("-1")).isEqualTo("'-1");
        assertThat(CsvWriter.cell("@SUMA")).isEqualTo("'@SUMA");
        assertThat(CsvWriter.cell("\tx")).isEqualTo("'\tx");
        assertThat(CsvWriter.cell("Arroz=bueno")).isEqualTo("Arroz=bueno");
    }

    @Test
    @DisplayName("pone entre comillas lo que tiene ;, comillas o saltos de línea, y deja vacío el nulo")
    void quoting() {
        assertThat(CsvWriter.cell("a;b")).isEqualTo("\"a;b\"");
        assertThat(CsvWriter.cell("dijo \"hola\"")).isEqualTo("\"dijo \"\"hola\"\"\"");
        assertThat(CsvWriter.cell("línea\nnueva")).isEqualTo("\"línea\nnueva\"");
        assertThat(CsvWriter.cell(null)).isEmpty();
    }
}
