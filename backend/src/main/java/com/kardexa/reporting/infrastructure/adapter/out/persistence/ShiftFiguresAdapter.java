package com.kardexa.reporting.infrastructure.adapter.out.persistence;

import com.kardexa.reporting.application.port.out.ShiftFigures;
import com.kardexa.reporting.domain.model.PaymentTotals;
import com.kardexa.reporting.domain.model.ReportPeriod;
import com.kardexa.reporting.domain.model.SiteSales;
import com.kardexa.shared.domain.BusinessCalendar;
import com.kardexa.shared.domain.Money;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Lee {@code shift}, {@code sale} y {@code site} (módulos {@code sites} y {@code sales}) para sumar lo recibido por
 * medio y lo vendido por sede: lectura permitida por el ADR 0012. Un turno cuenta en el día en que se cerró.
 */
@Component
class ShiftFiguresAdapter implements ShiftFigures {

    private final JdbcClient jdbc;

    ShiftFiguresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PaymentTotals payments(ReportPeriod period, UUID siteId) {
        return jdbc.sql("""
                SELECT coalesce(sum(cash_amount), 0) AS cash, coalesce(sum(yape_amount), 0) AS yape,
                       coalesce(sum(plin_amount), 0) AS plin, coalesce(sum(transfer_amount), 0) AS transfer
                FROM shift
                WHERE status = 'CLOSED' AND closed_at >= :start AND closed_at < :end
                  AND (CAST(:siteId AS uuid) IS NULL OR site_id = :siteId)""")
                .param("start", Timestamp.from(BusinessCalendar.startOf(period.from())))
                .param("end", Timestamp.from(BusinessCalendar.endOf(period.to())))
                .param("siteId", siteId, Types.OTHER)
                .query((row, rowNumber) -> new PaymentTotals(Money.of(row.getBigDecimal("cash")),
                        Money.of(row.getBigDecimal("yape")), Money.of(row.getBigDecimal("plin")),
                        Money.of(row.getBigDecimal("transfer"))))
                .single();
    }

    @Override
    public List<SiteSales> bySite(ReportPeriod period) {
        return jdbc.sql("""
                SELECT st.id, st.name,
                       coalesce((SELECT sum(s.total) FROM sale s
                                 WHERE s.site_id = st.id AND s.status = 'REGISTERED'
                                   AND s.sold_at >= :start AND s.sold_at < :end), 0) AS total,
                       (SELECT count(*) FROM sale s
                        WHERE s.site_id = st.id AND s.status = 'REGISTERED'
                          AND s.sold_at >= :start AND s.sold_at < :end) AS sales,
                       coalesce((SELECT sum(h.difference) FROM shift h
                                 WHERE h.site_id = st.id AND h.status = 'CLOSED'
                                   AND h.closed_at >= :start AND h.closed_at < :end), 0) AS difference
                FROM site st
                ORDER BY st.name""")
                .param("start", Timestamp.from(BusinessCalendar.startOf(period.from())))
                .param("end", Timestamp.from(BusinessCalendar.endOf(period.to())))
                .query((row, rowNumber) -> new SiteSales(row.getObject("id", UUID.class), row.getString("name"),
                        Money.of(row.getBigDecimal("total")), row.getLong("sales"),
                        Money.of(row.getBigDecimal("difference"))))
                .list();
    }
}
