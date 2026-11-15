package com.kardexa.sites.infrastructure.adapter.out.persistence;

import com.kardexa.shared.domain.Quantity;
import com.kardexa.sites.application.port.out.ShiftRepository;
import com.kardexa.sites.domain.exception.ShiftAlreadyClosedException;
import com.kardexa.sites.domain.exception.ShiftAlreadyOpenException;
import com.kardexa.sites.domain.model.Payments;
import com.kardexa.sites.domain.model.Shift;
import com.kardexa.sites.domain.model.Shift.Item;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Lee y escribe {@code shift} y {@code shift_item}; la base garantiza un turno abierto por sede (V18). */
@Component
class ShiftPersistenceAdapter implements ShiftRepository {

    private static final String SELECT = """
            SELECT s.*, u.full_name AS responsible_name
            FROM shift s JOIN app_user u ON u.id = s.responsible_id""";

    private final JdbcClient jdbc;

    ShiftPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(Shift shift) {
        try {
            jdbc.sql("""
                    INSERT INTO shift (id, site_id, responsible_id, status, opened_at)
                    VALUES (:id, :siteId, :responsibleId, 'OPEN', :openedAt)""")
                    .param("id", shift.id())
                    .param("siteId", shift.siteId())
                    .param("responsibleId", shift.responsibleId())
                    .param("openedAt", Timestamp.from(shift.openedAt()))
                    .update();
        } catch (DuplicateKeyException alreadyOpen) {
            throw new ShiftAlreadyOpenException();
        }
        for (Item item : shift.items()) {
            jdbc.sql("""
                    INSERT INTO shift_item (shift_id, product_id, product_name, opening_stock)
                    VALUES (:shiftId, :productId, :name, :opening)""")
                    .param("shiftId", shift.id())
                    .param("productId", item.productId())
                    .param("name", item.productName())
                    .param("opening", item.openingStock().value())
                    .update();
        }
    }

    @Override
    public Optional<Shift> findById(UUID id) {
        return jdbc.sql(SELECT + " WHERE s.id = :id").param("id", id).query(this::toShift).optional()
                .map(this::withItems);
    }

    @Override
    public Optional<Shift> findOpenBySite(UUID siteId) {
        return jdbc.sql(SELECT + " WHERE s.site_id = :siteId AND s.status = 'OPEN'").param("siteId", siteId)
                .query(this::toShift).optional().map(this::withItems);
    }

    @Override
    public List<Shift> search(java.time.Instant from, java.time.Instant to, UUID siteId, int limit) {
        return jdbc.sql(SELECT + " " + """
                 WHERE s.opened_at >= :from AND s.opened_at < :to
                   AND (CAST(:siteId AS uuid) IS NULL OR s.site_id = :siteId)
                 ORDER BY s.opened_at DESC
                 LIMIT :limit""")
                .param("from", Timestamp.from(from))
                .param("to", Timestamp.from(to))
                .param("siteId", siteId, java.sql.Types.OTHER)
                .param("limit", limit)
                .query(this::toShift)
                .list();
    }

    @Override
    public void saveClosed(Shift shift) {
        // Primero el detalle, mientras el turno sigue abierto: la base no deja tocar lo de un turno cerrado.
        for (Item item : shift.items()) {
            jdbc.sql("""
                    INSERT INTO shift_item (shift_id, product_id, product_name, opening_stock, leftover, sold_by_count,
                                            unit_price)
                    VALUES (:shiftId, :productId, :name, :opening, :leftover, :sold, :price)
                    ON CONFLICT (shift_id, product_id)
                    DO UPDATE SET leftover = :leftover, sold_by_count = :sold, unit_price = :price""")
                    .param("shiftId", shift.id())
                    .param("productId", item.productId())
                    .param("name", item.productName())
                    .param("opening", item.openingStock().value())
                    .param("leftover", item.leftover().value())
                    .param("sold", item.soldByCount().value())
                    .param("price", item.unitPrice())
                    .update();
        }
        Payments payments = shift.payments();
        int updated = jdbc.sql("""
                UPDATE shift SET status = 'CLOSED', closed_at = :closedAt, closed_by = :closedBy,
                       registered_sales = :registered, counted_sales = :counted, expected_amount = :expected,
                       note = :note, cash_amount = :cash, yape_amount = :yape, plin_amount = :plin,
                       transfer_amount = :transfer, received_amount = :received, difference = :difference
                WHERE id = :id AND status = 'OPEN'""")
                .param("id", shift.id())
                .param("closedAt", Timestamp.from(shift.closedAt()))
                .param("closedBy", shift.closedBy())
                .param("registered", shift.registeredSales())
                .param("counted", shift.countedSales())
                .param("expected", shift.expectedAmount())
                .param("note", shift.note(), java.sql.Types.VARCHAR)
                .param("cash", payments == null ? null : payments.cash(), java.sql.Types.NUMERIC)
                .param("yape", payments == null ? null : payments.yape(), java.sql.Types.NUMERIC)
                .param("plin", payments == null ? null : payments.plin(), java.sql.Types.NUMERIC)
                .param("transfer", payments == null ? null : payments.transfer(), java.sql.Types.NUMERIC)
                .param("received", shift.receivedAmount(), java.sql.Types.NUMERIC)
                .param("difference", shift.difference(), java.sql.Types.NUMERIC)
                .update();
        if (updated == 0) {
            throw new ShiftAlreadyClosedException();
        }
    }

    private Shift withItems(Shift shift) {
        List<Item> items = jdbc.sql("SELECT * FROM shift_item WHERE shift_id = :id ORDER BY product_name")
                .param("id", shift.id())
                .query(this::toItem)
                .list();
        return new Shift(shift.id(), shift.siteId(), shift.responsibleId(), shift.responsibleName(), shift.status(),
                shift.openedAt(), shift.closedAt(), shift.closedBy(), shift.registeredSales(), shift.countedSales(),
                shift.expectedAmount(), shift.note(), shift.payments(), shift.difference(), items);
    }

    private Shift toShift(ResultSet row, int rowNumber) throws SQLException {
        Timestamp closedAt = row.getTimestamp("closed_at");
        return new Shift(
                row.getObject("id", UUID.class),
                row.getObject("site_id", UUID.class),
                row.getObject("responsible_id", UUID.class),
                row.getString("responsible_name"),
                Shift.Status.valueOf(row.getString("status")),
                row.getTimestamp("opened_at").toInstant(),
                closedAt == null ? null : closedAt.toInstant(),
                row.getObject("closed_by", UUID.class),
                row.getBigDecimal("registered_sales"),
                row.getBigDecimal("counted_sales"),
                row.getBigDecimal("expected_amount"),
                row.getString("note"),
                paymentsOf(row),
                row.getBigDecimal("difference"),
                List.of());
    }

    private Payments paymentsOf(ResultSet row) throws SQLException {
        BigDecimal cash = row.getBigDecimal("cash_amount");
        return cash == null
                ? null
                : new Payments(cash, row.getBigDecimal("yape_amount"), row.getBigDecimal("plin_amount"),
                        row.getBigDecimal("transfer_amount"));
    }

    private Item toItem(ResultSet row, int rowNumber) throws SQLException {
        BigDecimal leftover = row.getBigDecimal("leftover");
        BigDecimal sold = row.getBigDecimal("sold_by_count");
        return new Item(
                row.getObject("product_id", UUID.class),
                row.getString("product_name"),
                new Quantity(row.getBigDecimal("opening_stock")),
                leftover == null ? null : new Quantity(leftover),
                sold == null ? null : new Quantity(sold),
                row.getBigDecimal("unit_price"));
    }
}
