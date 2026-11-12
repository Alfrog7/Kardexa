package com.kardexa.sales.infrastructure.adapter.out.persistence;

import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.application.port.out.SaleRepository;
import com.kardexa.sales.domain.model.Sale;
import com.kardexa.sales.domain.exception.SaleAlreadyVoidedException;
import com.kardexa.sales.domain.model.SaleItem;
import com.kardexa.sales.domain.model.SaleNumber;
import com.kardexa.sales.domain.model.SaleStatus;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Ventas con {@code JdbcClient}: una venta se inserta con sus ítems y se lee con el nombre del vendedor.
 *
 * <p>El nombre se lee de {@code app_user}, del módulo {@code identity}: es una lectura permitida por el
 * ADR 0007 (solo lectura y solo en este adaptador).
 */
@Component
class SalePersistenceAdapter implements SaleRepository {

    private static final String HEADER_SELECT = """
            SELECT s.id, s.number, s.sold_at, s.seller_id, u.full_name, s.status, s.void_reason, s.total
            FROM sale s
            JOIN app_user u ON u.id = s.seller_id
            """;

    private final JdbcClient jdbc;

    SalePersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(Sale sale) {
        jdbc.sql("""
                INSERT INTO sale (id, number, status, total, seller_id, sold_at)
                VALUES (:id, :number, :status, :total, :sellerId, :soldAt)""")
                .param("id", sale.id())
                .param("number", sale.number().toString())
                .param("status", sale.status().name())
                .param("total", sale.total().amount())
                .param("sellerId", sale.sellerId())
                .param("soldAt", Timestamp.from(sale.soldAt()))
                .update();
        int line = 1;
        for (SaleItem item : sale.items()) {
            jdbc.sql("""
                    INSERT INTO sale_item
                        (sale_id, line_no, product_id, product_code, product_name, quantity, unit_price, subtotal)
                    VALUES (:saleId, :line, :productId, :code, :name, :quantity, :unitPrice, :subtotal)""")
                    .param("saleId", sale.id())
                    .param("line", line++)
                    .param("productId", item.productId())
                    .param("code", item.productCode())
                    .param("name", item.productName())
                    .param("quantity", item.quantity().value())
                    .param("unitPrice", item.unitPrice().amount())
                    .param("subtotal", item.subtotal().amount())
                    .update();
        }
    }

    @Override
    public Optional<Sale> findById(UUID id) {
        List<SaleItem> items = jdbc.sql("""
                SELECT product_id, product_code, product_name, quantity, unit_price FROM sale_item
                WHERE sale_id = :id ORDER BY line_no""")
                .param("id", id)
                .query((rs, n) -> new SaleItem(rs.getObject("product_id", UUID.class), rs.getString("product_code"),
                        rs.getString("product_name"), new Quantity(rs.getBigDecimal("quantity")),
                        Money.of(rs.getBigDecimal("unit_price"))))
                .list();
        return jdbc.sql("""
                SELECT id, number, status, seller_id, sold_at, void_reason, voided_by, voided_at
                FROM sale WHERE id = :id""")
                .param("id", id)
                .query((rs, n) -> Sale.restore(rs.getObject("id", UUID.class), SaleNumber.parse(rs.getString("number")),
                        items, SaleStatus.valueOf(rs.getString("status")), rs.getObject("seller_id", UUID.class),
                        rs.getTimestamp("sold_at").toInstant(),
                        rs.getString("void_reason") == null ? null : new Sale.Voiding(rs.getString("void_reason"),
                                rs.getObject("voided_by", UUID.class), rs.getTimestamp("voided_at").toInstant())))
                .optional();
    }

    /** La condición sobre el estado impide anular dos veces aunque dos anulaciones lleguen juntas. */
    @Override
    public void saveVoiding(Sale sale) {
        Sale.Voiding voiding = sale.voiding();
        int updated = jdbc.sql("""
                UPDATE sale
                SET status = 'VOIDED', void_reason = :reason, voided_by = :userId, voided_at = :at
                WHERE id = :id AND status = 'REGISTERED'""")
                .param("reason", voiding.reason())
                .param("userId", voiding.userId(), Types.OTHER)
                .param("at", Timestamp.from(voiding.at()))
                .param("id", sale.id())
                .update();
        if (updated == 0) {
            throw new SaleAlreadyVoidedException(sale.number());
        }
    }

    @Override
    public Optional<SaleView> findView(UUID id) {
        List<Header> headers = jdbc.sql(HEADER_SELECT + "WHERE s.id = :id")
                .param("id", id)
                .query(SalePersistenceAdapter::header)
                .list();
        return withItems(headers).stream().findFirst();
    }

    @Override
    public ResultPage<SaleView> findViews(Instant from, Instant to, UUID sellerId, PageQuery page) {
        StringBuilder where = new StringBuilder("WHERE true");
        Map<String, Object> params = new LinkedHashMap<>();
        if (from != null) {
            where.append(" AND s.sold_at >= :from");
            params.put("from", Timestamp.from(from));
        }
        if (to != null) {
            where.append(" AND s.sold_at < :to");
            params.put("to", Timestamp.from(to));
        }
        if (sellerId != null) {
            where.append(" AND s.seller_id = :sellerId");
            params.put("sellerId", sellerId);
        }
        long total = jdbc.sql("SELECT count(*) FROM sale s " + where)
                .params(params)
                .query(Long.class)
                .single();
        List<Header> headers = jdbc.sql(HEADER_SELECT + where + """

                ORDER BY s.sold_at DESC, s.number DESC
                LIMIT :limit OFFSET :offset""")
                .params(params)
                .param("limit", page.size())
                .param("offset", page.offset())
                .query(SalePersistenceAdapter::header)
                .list();
        return ResultPage.of(withItems(headers), page, total);
    }

    /** Lee los ítems de todas las ventas de una vez (sin N+1), en el orden en que se registraron. */
    private List<SaleView> withItems(List<Header> headers) {
        if (headers.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<SaleView.Item>> items = new LinkedHashMap<>();
        headers.forEach(h -> items.put(h.id(), new ArrayList<>()));
        jdbc.sql("""
                SELECT sale_id, product_id, product_code, product_name, quantity, unit_price, subtotal
                FROM sale_item
                WHERE sale_id IN (:ids)
                ORDER BY sale_id, line_no""")
                .param("ids", items.keySet())
                .query((rs, n) -> {
                    items.get(rs.getObject("sale_id", UUID.class)).add(new SaleView.Item(
                            rs.getObject("product_id", UUID.class),
                            rs.getString("product_code"),
                            rs.getString("product_name"),
                            new Quantity(rs.getBigDecimal("quantity")),
                            Money.of(rs.getBigDecimal("unit_price")),
                            Money.of(rs.getBigDecimal("subtotal"))));
                    return null;
                })
                .list();
        return headers.stream()
                .map(h -> new SaleView(h.id(), h.number(), h.soldAt(), h.sellerId(), h.sellerName(), h.status(), h.voidReason(),
                        h.total(), items.get(h.id())))
                .toList();
    }

    private static Header header(ResultSet rs, int rowNum) throws SQLException {
        return new Header(rs.getObject("id", UUID.class), rs.getString("number"),
                rs.getTimestamp("sold_at").toInstant(), rs.getObject("seller_id", UUID.class),
                rs.getString("full_name"), SaleStatus.valueOf(rs.getString("status")), rs.getString("void_reason"),
                Money.of(rs.getBigDecimal("total")));
    }

    private record Header(UUID id, String number, Instant soldAt, UUID sellerId, String sellerName, SaleStatus status,
            String voidReason, Money total) {}
}
