package com.kardexa.reporting.infrastructure.adapter.out.persistence;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consultas que se recorren fila a fila para los CSV (ADR 0012). PostgreSQL solo entrega las filas por partes
 * dentro de una transacción y con un {@code fetchSize}; así el archivo se escribe mientras se lee, sin cargar
 * todo el resultado en memoria.
 */
@Component
class StreamingQueries {

    private static final int FETCH_SIZE = 500;

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate readOnly;

    StreamingQueries(DataSource dataSource, PlatformTransactionManager transactions) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setFetchSize(FETCH_SIZE);
        this.jdbc = new NamedParameterJdbcTemplate(template);
        this.readOnly = new TransactionTemplate(transactions);
        this.readOnly.setReadOnly(true);
    }

    void stream(String sql, MapSqlParameterSource params, RowCallbackHandler handler) {
        readOnly.executeWithoutResult(status -> jdbc.query(sql, params, handler));
    }

    NamedParameterJdbcTemplate jdbc() {
        return jdbc;
    }
}
