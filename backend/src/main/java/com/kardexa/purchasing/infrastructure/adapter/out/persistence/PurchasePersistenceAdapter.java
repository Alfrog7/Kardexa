package com.kardexa.purchasing.infrastructure.adapter.out.persistence;

import com.kardexa.purchasing.application.port.in.PurchaseView;
import com.kardexa.purchasing.application.port.out.PurchaseRepository;
import com.kardexa.purchasing.domain.exception.PurchaseAlreadyVoidedException;
import com.kardexa.purchasing.domain.model.Purchase;
import com.kardexa.purchasing.domain.model.PurchaseItem;
import com.kardexa.purchasing.domain.model.PurchaseStatus;
import com.kardexa.purchasing.domain.model.Supplier;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Compras con {@code JdbcClient}: una compra se inserta con sus ítems y se lee junto a su proveedor.
 *
 * <p>Los nombres de producto se leen de la tabla {@code product}, del módulo {@code catalog}: es una
 * lectura permitida por el ADR 0007 (solo lectura y solo en este adaptador).
 */
@Component
class PurchasePersistenceAdapter implements PurchaseRepository {

    /** Sin filtro de fechas se usan los extremos, para que PostgreSQL conozca siempre el tipo. */
    private static final LocalDate MIN_DATE = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private static final String HEADER_SELECT = """
            SELECT p.id, p.supplier_document, p.purchase_date, p.status, p.void_reason, p.total,
                   s.id AS supplier_id, s.ruc, s.business_name, s.phone, s.email, s.active
            FROM purchase p
            JOIN supplier s ON s.id = p.supplier_id
            """;

    private final JdbcClient jdbc;

    PurchasePersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(Purchase purchase) {
        jdbc.sql("""
                INSERT INTO purchase
                    (id, supplier_id, supplier_document, purchase_date, status, total, user_id, created_at)
                VALUES (:id, :supplierId, :document, :purchaseDate, :status, :total, :userId, :createdAt)""")
                .param("id", purchase.id())
                .param("supplierId", purchase.supplierId())
                .param("document", purchase.supplierDocument(), Types.VARCHAR)
                .param("purchaseDate", purchase.purchaseDate())
                .param("status", purchase.status().name())
                .param("total", purchase.total().amount())
                .param("userId", purchase.userId(), Types.OTHER)
                .param("createdAt", Timestamp.from(purchase.createdAt()))
                .update();
        int line = 1;
        for (PurchaseItem item : purchase.items()) {
            jdbc.sql("""
                    INSERT INTO purchase_item (purchase_id, line_no, product_id, quantity, unit_cost, subtotal)
                    VALUES (:purchaseId, :line, :productId, :quantity, :unitCost, :subtotal)""")
                    .param("purchaseId", purchase.id())
                    .param("line", line++)
                    .param("productId", item.productId())
                    .param("quantity", item.quantity().value())
                    .param("unitCost", item.unitCost().amount())
                    .param("subtotal", item.subtotal().amount())
                    .update();
        }
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        List<PurchaseItem> items = jdbc.sql("""
                SELECT product_id, quantity, unit_cost FROM purchase_item
                WHERE purchase_id = :id ORDER BY line_no""")
                .param("id", id)
                .query((rs, n) -> new PurchaseItem(rs.getObject("product_id", UUID.class),
                        new Quantity(rs.getBigDecimal("quantity")), Money.of(rs.getBigDecimal("unit_cost"))))
                .list();
        return jdbc.sql("""
                SELECT id, supplier_id, supplier_document, purchase_date, status, user_id, created_at,
                       void_reason, voided_by, voided_at
                FROM purchase WHERE id = :id""")
                .param("id", id)
                .query((rs, n) -> Purchase.restore(rs.getObject("id", UUID.class),
                        rs.getObject("supplier_id", UUID.class), rs.getString("supplier_document"),
                        rs.getObject("purchase_date", LocalDate.class), items,
                        PurchaseStatus.valueOf(rs.getString("status")), rs.getObject("user_id", UUID.class),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("void_reason") == null ? null : new Purchase.Voiding(rs.getString("void_reason"),
                                rs.getObject("voided_by", UUID.class), rs.getTimestamp("voided_at").toInstant())))
                .optional();
    }

    /** La condición sobre el estado impide anular dos veces aunque dos anulaciones lleguen juntas. */
    @Override
    public void saveVoiding(Purchase purchase) {
        Purchase.Voiding voiding = purchase.voiding();
        int updated = jdbc.sql("""
                UPDATE purchase
                SET status = 'VOIDED', void_reason = :reason, voided_by = :userId, voided_at = :at
                WHERE id = :id AND status = 'REGISTERED'""")
                .param("reason", voiding.reason())
                .param("userId", voiding.userId(), Types.OTHER)
                .param("at", Timestamp.from(voiding.at()))
                .param("id", purchase.id())
                .update();
        if (updated == 0) {
            throw new PurchaseAlreadyVoidedException(purchase.id());
        }
    }

    @Override
    public Optional<PurchaseView> findView(UUID id) {
        List<Header> headers = jdbc.sql(HEADER_SELECT + "WHERE p.id = :id")
                .param("id", id)
                .query(PurchasePersistenceAdapter::header)
                .list();
        return withItems(headers).stream().findFirst();
    }

    @Override
    public ResultPage<PurchaseView> findViews(LocalDate from, LocalDate to, PageQuery page) {
        LocalDate start = from == null ? MIN_DATE : from;
        LocalDate end = to == null ? MAX_DATE : to;
        long total = jdbc.sql("SELECT count(*) FROM purchase WHERE purchase_date BETWEEN :from AND :to")
                .param("from", start)
                .param("to", end)
                .query(Long.class)
                .single();
        List<Header> headers = jdbc.sql(HEADER_SELECT + """
                WHERE p.purchase_date BETWEEN :from AND :to
                ORDER BY p.purchase_date DESC, p.created_at DESC, p.id
                LIMIT :limit OFFSET :offset""")
                .param("from", start)
                .param("to", end)
                .param("limit", page.size())
                .param("offset", page.offset())
                .query(PurchasePersistenceAdapter::header)
                .list();
        return ResultPage.of(withItems(headers), page, total);
    }

    /** Lee los ítems de todas las compras de una vez (sin N+1), en el orden en que se registraron. */
    private List<PurchaseView> withItems(List<Header> headers) {
        if (headers.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<PurchaseView.Item>> items = new LinkedHashMap<>();
        headers.forEach(h -> items.put(h.id(), new ArrayList<>()));
        jdbc.sql("""
                SELECT i.purchase_id, i.product_id, pr.name, i.quantity, i.unit_cost, i.subtotal
                FROM purchase_item i
                JOIN product pr ON pr.id = i.product_id
                WHERE i.purchase_id IN (:ids)
                ORDER BY i.purchase_id, i.line_no""")
                .param("ids", items.keySet())
                .query((rs, n) -> {
                    items.get(rs.getObject("purchase_id", UUID.class)).add(new PurchaseView.Item(
                            rs.getObject("product_id", UUID.class),
                            rs.getString("name"),
                            new Quantity(rs.getBigDecimal("quantity")),
                            Money.of(rs.getBigDecimal("unit_cost")),
                            Money.of(rs.getBigDecimal("subtotal"))));
                    return null;
                })
                .list();
        return headers.stream()
                .map(h -> new PurchaseView(h.id(), h.supplier(), h.supplierDocument(), h.purchaseDate(), h.status(),
                        h.voidReason(), h.total(), items.get(h.id())))
                .toList();
    }

    private static Header header(ResultSet rs, int rowNum) throws SQLException {
        Supplier supplier = Supplier.restore(rs.getObject("supplier_id", UUID.class),
                new Supplier.Details(rs.getString("ruc"), rs.getString("business_name"), rs.getString("phone"),
                        rs.getString("email")),
                rs.getBoolean("active"));
        return new Header(rs.getObject("id", UUID.class), supplier, rs.getString("supplier_document"),
                rs.getObject("purchase_date", LocalDate.class), PurchaseStatus.valueOf(rs.getString("status")),
                rs.getString("void_reason"), Money.of(rs.getBigDecimal("total")));
    }

    private record Header(UUID id, Supplier supplier, String supplierDocument, LocalDate purchaseDate,
            PurchaseStatus status, String voidReason, Money total) {}
}
