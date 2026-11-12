package com.kardexa.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.sales.application.port.in.ListSalesUseCase.Viewer;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase.ItemCommand;
import com.kardexa.sales.application.port.in.RegisterSaleUseCase.RegisterSaleCommand;
import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.application.port.out.SaleNumbers;
import com.kardexa.sales.application.port.out.SaleProducts;
import com.kardexa.sales.application.port.out.SaleRepository;
import com.kardexa.sales.application.port.out.StockLedger;
import com.kardexa.sales.domain.exception.InactiveProductException;
import com.kardexa.sales.domain.exception.SaleNotFoundException;
import com.kardexa.sales.domain.exception.SaleProductNotFoundException;
import com.kardexa.sales.domain.model.Sale;
import com.kardexa.sales.domain.model.SaleNumber;
import com.kardexa.shared.domain.Money;
import com.kardexa.shared.domain.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Registrar una venta (HU-14)")
class RegisterSaleServiceTest {

    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID RICE = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    private final InMemorySales sales = new InMemorySales();
    private final Map<UUID, SaleProducts.ProductForSale> catalog = new LinkedHashMap<>();
    private final List<Sale> withdrawn = new ArrayList<>();
    private long lastNumber;

    private final RegisterSaleService service = new RegisterSaleService(sales, this::nextNumber,
            this::findProducts, withdrawn::add, Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneOffset.UTC));

    RegisterSaleServiceTest() {
        catalog.put(RICE, new SaleProducts.ProductForSale(RICE, "AR-001", "Arroz 5 kg", Money.of("25.00"), true));
        catalog.put(OIL, new SaleProducts.ProductForSale(OIL, "AC-001", "Aceite 1 L", Money.of("9.90"), true));
    }

    @Test
    @DisplayName("copia el precio del catálogo, descuenta el stock y numera la venta (RN-06, RN-07)")
    void registers() {
        Sale first = service.register(command(new ItemCommand(RICE, Quantity.of("2")), new ItemCommand(OIL, Quantity.of("1"))));
        Sale second = service.register(command(new ItemCommand(OIL, Quantity.of("3"))));

        assertThat(first.number()).hasToString("V-000001");
        assertThat(second.number()).hasToString("V-000002");
        assertThat(first.items().get(0).unitPrice()).isEqualTo(Money.of("25.00"));
        assertThat(first.items().get(0).productName()).isEqualTo("Arroz 5 kg");
        assertThat(first.total()).isEqualTo(Money.of("59.90"));
        assertThat(withdrawn).containsExactly(first, second);
        assertThat(sales.saved).containsExactly(first, second);
    }

    @Test
    @DisplayName("un producto inexistente o desactivado rechaza la venta sin tocar el stock")
    void rejectsUnavailableProducts() {
        catalog.put(OIL, new SaleProducts.ProductForSale(OIL, "AC-001", "Aceite 1 L", Money.of("9.90"), false));

        assertThatThrownBy(() -> service.register(command(new ItemCommand(UUID.randomUUID(), Quantity.of("1")))))
                .isInstanceOf(SaleProductNotFoundException.class);
        assertThatThrownBy(() -> service.register(command(new ItemCommand(RICE, Quantity.of("1")),
                new ItemCommand(OIL, Quantity.of("1")))))
                .isInstanceOf(InactiveProductException.class)
                .hasMessageContaining("Aceite 1 L");
        assertThat(withdrawn).isEmpty();
        assertThat(lastNumber).isZero();
    }

    @Test
    @DisplayName("si el inventario rechaza la salida (RN-01), no se reserva número ni se guarda la venta")
    void stockFailureKeepsNumber() {
        StockLedger failing = sale -> {
            throw new IllegalStateException("sin stock");
        };
        RegisterSaleService rejecting = new RegisterSaleService(sales, this::nextNumber, this::findProducts, failing,
                Clock.systemUTC());

        assertThatThrownBy(() -> rejecting.register(command(new ItemCommand(RICE, Quantity.of("1")))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(lastNumber).isZero();
        assertThat(sales.saved).isEmpty();
    }

    @Test
    @DisplayName("RF-21: el EMPLEADO solo ve sus ventas; el ADMIN ve todas")
    void viewerSeesOwnSales() {
        Sale sale = service.register(command(new ItemCommand(RICE, Quantity.of("1"))));
        SaleQueryService query = new SaleQueryService(sales);

        assertThat(query.get(sale.id(), new Viewer(SELLER, false)).number()).isEqualTo("V-000001");
        assertThat(query.get(sale.id(), new Viewer(UUID.randomUUID(), true)).number()).isEqualTo("V-000001");
        assertThatThrownBy(() -> query.get(sale.id(), new Viewer(UUID.randomUUID(), false)))
                .isInstanceOf(SaleNotFoundException.class);
    }

    private SaleNumber nextNumber() {
        return new SaleNumber(++lastNumber);
    }

    private List<SaleProducts.ProductForSale> findProducts(Collection<UUID> ids) {
        return ids.stream().map(catalog::get).filter(java.util.Objects::nonNull).toList();
    }

    private static RegisterSaleCommand command(ItemCommand... items) {
        return new RegisterSaleCommand(List.of(items), SELLER);
    }

    /** Doble en memoria de {@link SaleRepository}. */
    private static final class InMemorySales implements SaleRepository {

        private final List<Sale> saved = new ArrayList<>();

        @Override
        public void add(Sale sale) {
            saved.add(sale);
        }

        @Override
        public Optional<SaleView> findView(UUID id) {
            return saved.stream().filter(s -> s.id().equals(id)).findFirst()
                    .map(s -> new SaleView(s.id(), s.number().toString(), s.soldAt(), s.sellerId(), "Vendedor",
                            s.status(), null, s.total(), List.of()));
        }
    }
}
