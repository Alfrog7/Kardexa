package com.kardexa.reporting.infrastructure.adapter.out.persistence;

import com.kardexa.reporting.application.port.out.SaleRows;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SaleRow;
import com.kardexa.shared.domain.BusinessCalendar;
import com.kardexa.shared.domain.Money;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * Lee {@code sale} y {@code sale_item} (módulo {@code sales}) con el nombre del vendedor ({@code app_user}):
 * lectura permitida por los ADR 0007 y 0012. Mismo orden que {@code GET /sales}; filas por partes con
 * {@link StreamingQueries}.
 */
@Component
class SaleRowsAdapter implements SaleRows {

    private final StreamingQueries queries;

    SaleRowsAdapter(StreamingQueries queries) {
        this.queries = queries;
    }

    @Override
    public void sales(ReportPeriod period, UUID sellerId, String status, Consumer<SaleRow> sink) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.number, s.sold_at, u.full_name AS seller, s.status, s.total, s.void_reason,
                       (SELECT count(*) FROM sale_item i WHERE i.sale_id = s.id) AS lines
                FROM sale s
                JOIN app_user u ON u.id = s.seller_id
                WHERE s.sold_at >= :from AND s.sold_at < :to""");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Timestamp.from(BusinessCalendar.startOf(period.from())))
                .addValue("to", Timestamp.from(BusinessCalendar.endOf(period.to())));
        if (sellerId != null) {
            sql.append(" AND s.seller_id = :sellerId");
            params.addValue("sellerId", sellerId);
        }
        if (status != null) {
            sql.append(" AND s.status = :status");
            params.addValue("status", status);
        }
        sql.append(" ORDER BY s.sold_at DESC, s.number DESC");
        queries.stream(sql.toString(), params, row -> sink.accept(new SaleRow(row.getString("number"),
                row.getTimestamp("sold_at").toInstant(), row.getString("seller"), row.getString("status"),
                row.getInt("lines"), Money.of(row.getBigDecimal("total")), row.getString("void_reason"))));
    }
}
