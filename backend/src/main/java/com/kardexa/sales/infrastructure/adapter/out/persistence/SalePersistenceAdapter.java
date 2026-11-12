package com.kardexa.sales.infrastructure.adapter.out.persistence;

import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.application.port.out.SaleRepository;
import com.kardexa.sales.domain.model.Sale;
import com.kardexa.sales.domain.model.SaleItem;
import com.kardexa.sales.domain.model.SaleStatus;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
            SELECT s.id, s.number, s.sold_at, s.seller_id, u.full_name, s.status, s.total
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
    public Optional<SaleView> findView(UUID id) {
        List<Header> headers = jdbc.sql(HEADER_SELECT + "WHERE s.id = :id")
                .param("id", id)
                .query(SalePersistenceAdapter::header)
                .list();
        return withItems(headers).stream().findFirst();
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
                .map(h -> new SaleView(h.id(), h.number(), h.soldAt(), h.sellerId(), h.sellerName(), h.status(), null,
                        h.total(), items.get(h.id())))
                .toList();
    }

    private static Header header(ResultSet rs, int rowNum) throws SQLException {
        return new Header(rs.getObject("id", UUID.class), rs.getString("number"),
                rs.getTimestamp("sold_at").toInstant(), rs.getObject("seller_id", UUID.class),
                rs.getString("full_name"), SaleStatus.valueOf(rs.getString("status")),
                Money.of(rs.getBigDecimal("total")));
    }

    private record Header(UUID id, String number, Instant soldAt, UUID sellerId, String sellerName, SaleStatus status,
            Money total) {}
}
