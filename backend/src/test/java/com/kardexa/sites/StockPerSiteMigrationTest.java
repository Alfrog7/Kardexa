package com.kardexa.sites;

import static org.assertj.core.api.Assertions.assertThat;

import com.kardexa.shared.domain.MainSite;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Migración V15 (HU-19): una base con datos de la Fase 7 (V14) pasa al stock por sede sin perder saldos ni
 * historial, y todo queda en la «Sede principal». Usa su propio PostgreSQL para poder migrar en dos tiempos.
 */
@DisplayName("Migración V15: datos anteriores a la Sede principal")
class StockPerSiteMigrationTest {

    private static final String PRINCIPAL = MainSite.ID.toString();

    @Test
    @DisplayName("criterio 3: los datos actuales quedan en la Sede principal con los mismos saldos")
    void legacyDataMovesToMainSite() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            migrate(postgres, "14");
            insertLegacyData(postgres);

            migrate(postgres, null);

            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
            assertThat(jdbc.queryForObject("SELECT quantity::text FROM product_stock", String.class)).isEqualTo("12.500");
            assertThat(jdbc.queryForObject("SELECT site_id::text FROM product_stock", String.class)).isEqualTo(PRINCIPAL);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_movement WHERE site_id = ?::uuid", Integer.class,
                    PRINCIPAL)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT balance_after::text FROM stock_movement ORDER BY seq DESC LIMIT 1",
                    String.class)).isEqualTo("12.500");
            assertThat(jdbc.queryForObject("SELECT site_id::text FROM sale", String.class)).isEqualTo(PRINCIPAL);
            assertThat(jdbc.queryForObject("SELECT site_id::text FROM purchase", String.class)).isEqualTo(PRINCIPAL);
            assertThat(jdbc.queryForObject("SELECT site_id::text FROM stock_adjustment", String.class)).isEqualTo(PRINCIPAL);

            // El stock ya admite otra fila del mismo producto en otra sede, y las columnas nuevas no tienen valor por omisión.
            jdbc.update("INSERT INTO site (id, name) VALUES ('00000000-0000-0000-0000-0000000000a1', 'Galeno')");
            jdbc.update("INSERT INTO product_stock (product_id, site_id, quantity) SELECT product_id,"
                    + " '00000000-0000-0000-0000-0000000000a1', 3 FROM product_stock");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM product_stock", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM information_schema.columns
                    WHERE column_name = 'site_id' AND column_default IS NOT NULL""", Integer.class)).isZero();
        }
    }

    private static void migrate(PostgreSQLContainer postgres, String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    /** Un producto con stock, una compra, una venta, un ajuste y sus movimientos, como los dejaba la Fase 7. */
    private static void insertLegacyData(PostgreSQLContainer postgres) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                Statement sql = connection.createStatement()) {
            sql.execute("""
                    INSERT INTO app_user (id, full_name, email, password_hash, role)
                    VALUES ('00000000-0000-0000-0000-0000000000b1', 'Admin', 'admin@legacy.test', 'x', 'ADMIN')""");
            sql.execute("INSERT INTO category (id, name) VALUES ('00000000-0000-0000-0000-0000000000c1', 'Panes')");
            sql.execute("""
                    INSERT INTO product (id, code, name, category_id, unit, sale_price, min_stock)
                    VALUES ('00000000-0000-0000-0000-0000000000d1', 'PAN-1', 'Pan', '00000000-0000-0000-0000-0000000000c1',
                            'UNIT', 1, 1)""");
            sql.execute("INSERT INTO product_stock (product_id, quantity, version) VALUES"
                    + " ('00000000-0000-0000-0000-0000000000d1', 12.5, 1)");
            sql.execute("INSERT INTO supplier (id, ruc, business_name) VALUES"
                    + " ('00000000-0000-0000-0000-0000000000e1', '20123456789', 'Harinera')");
            sql.execute("""
                    INSERT INTO purchase (id, supplier_id, supplier_document, purchase_date, status, total, user_id, created_at)
                    VALUES ('00000000-0000-0000-0000-0000000000f1', '00000000-0000-0000-0000-0000000000e1', 'F001-1',
                            DATE '2026-11-01', 'REGISTERED', 10, '00000000-0000-0000-0000-0000000000b1', now())""");
            sql.execute("""
                    INSERT INTO sale (id, number, status, total, seller_id, sold_at)
                    VALUES ('00000000-0000-0000-0000-0000000000a9', 'V-000001', 'REGISTERED', 2,
                            '00000000-0000-0000-0000-0000000000b1', now())""");
            sql.execute("""
                    INSERT INTO stock_adjustment (id, product_id, direction, quantity, reason, user_id, created_at)
                    VALUES ('00000000-0000-0000-0000-0000000000a8', '00000000-0000-0000-0000-0000000000d1', 'IN', 2.5,
                            'PHYSICAL_COUNT', '00000000-0000-0000-0000-0000000000b1', now())""");
            sql.execute("""
                    INSERT INTO stock_movement (id, product_id, type, direction, quantity, balance_after, source_type,
                                                source_id, user_id, occurred_at)
                    VALUES ('00000000-0000-0000-0000-0000000000a7', '00000000-0000-0000-0000-0000000000d1', 'PURCHASE',
                            'IN', 10, 10, 'PURCHASE', '00000000-0000-0000-0000-0000000000f1',
                            '00000000-0000-0000-0000-0000000000b1', now()),
                           ('00000000-0000-0000-0000-0000000000a6', '00000000-0000-0000-0000-0000000000d1', 'ADJUSTMENT',
                            'IN', 2.5, 12.5, 'ADJUSTMENT', '00000000-0000-0000-0000-0000000000a8',
                            '00000000-0000-0000-0000-0000000000b1', now())""");
        }
    }
}
