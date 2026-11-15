package com.kardexa.sites.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.shared.domain.Quantity;
import com.kardexa.sites.application.port.in.CloseShiftUseCase.CloseShiftCommand;
import com.kardexa.sites.application.port.in.CloseShiftUseCase.Count;
import com.kardexa.sites.application.port.out.ShiftLedger;
import com.kardexa.sites.application.port.out.ShiftLedger.Sold;
import com.kardexa.sites.application.port.out.ShiftRepository;
import com.kardexa.sites.application.port.out.ShiftSales;
import com.kardexa.sites.application.port.out.ShiftStock;
import com.kardexa.sites.application.port.out.ShiftStock.StockLine;
import com.kardexa.sites.domain.exception.InvalidShiftException;
import com.kardexa.sites.domain.exception.LeftoverExceedsStockException;
import com.kardexa.sites.domain.exception.NoOpenShiftException;
import com.kardexa.sites.domain.exception.ShiftAlreadyClosedException;
import com.kardexa.sites.domain.exception.ShiftAlreadyOpenException;
import com.kardexa.sites.domain.exception.ShiftForbiddenException;
import com.kardexa.sites.domain.model.Shift;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Turnos con cierre por conteo (HU-22)")
class ShiftServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T20:00:00Z");
    private static final UUID SITE = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID PAN = UUID.randomUUID();
    private static final UUID BOCADILLO = UUID.randomUUID();

    private final Map<UUID, Shift> saved = new HashMap<>();
    private final Map<UUID, StockLine> stock = new HashMap<>();
    private final List<List<Sold>> ledgerCalls = new ArrayList<>();
    private BigDecimal registeredSales = new BigDecimal("12.00");

    private final ShiftService service = new ShiftService(new ShiftRepository() {
        @Override
        public void add(Shift shift) {
            saved.put(shift.id(), shift);
        }

        @Override
        public Optional<Shift> findById(UUID id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public Optional<Shift> findOpenBySite(UUID siteId) {
            return saved.values().stream().filter(s -> s.siteId().equals(siteId) && s.isOpen()).findFirst();
        }

        @Override
        public void saveClosed(Shift shift) {
            saved.put(shift.id(), shift);
        }
    }, new ShiftStock() {
        @Override
        public List<StockLine> withStock(UUID siteId) {
            return stock.values().stream().filter(l -> l.quantity().isPositive()).toList();
        }

        @Override
        public Map<UUID, StockLine> lock(UUID siteId, Collection<UUID> productIds) {
            Map<UUID, StockLine> found = new HashMap<>();
            productIds.forEach(id -> {
                if (stock.containsKey(id)) {
                    found.put(id, stock.get(id));
                }
            });
            return found;
        }
    }, (siteId, from, to) -> registeredSales, new ShiftLedger() {
        @Override
        public void recordSold(UUID shiftId, UUID siteId, UUID userId, List<Sold> lines) {
            ledgerCalls.add(lines);
        }
    }, Clock.fixed(NOW, ZoneOffset.UTC));

    private void stockOf(UUID id, String name, String price, String quantity) {
        stock.put(id, new StockLine(id, name, new BigDecimal(price), Quantity.of(quantity)));
    }

    private Shift openShift() {
        stockOf(PAN, "Pan francés", "0.50", "40");
        stockOf(BOCADILLO, "Bocadillo", "2.00", "10");
        return service.open(SITE, LUIS);
    }

    private CloseShiftCommand closing(UUID user, boolean admin, Shift shift, Count... counts) {
        return new CloseShiftCommand(shift.id(), user, admin, List.of(counts), null);
    }

    @Test
    @DisplayName("abre el turno con la foto del stock de cada producto y solo uno por sede")
    void opensWithSnapshotAndOnlyOnePerSite() {
        Shift shift = openShift();

        assertThat(shift.isOpen()).isTrue();
        assertThat(shift.items()).extracting(Shift.Item::productName).containsExactlyInAnyOrder("Pan francés", "Bocadillo");
        assertThat(service.current(SITE).id()).isEqualTo(shift.id());
        assertThatThrownBy(() -> service.open(SITE, ANA)).isInstanceOf(ShiftAlreadyOpenException.class);
        assertThatThrownBy(() -> service.current(UUID.randomUUID())).isInstanceOf(NoOpenShiftException.class);
    }

    @Test
    @DisplayName("al cerrar calcula lo vendido por conteo, descuenta el stock y fija el monto esperado")
    void closesByCount() {
        Shift shift = openShift();

        Shift closed = service.close(closing(LUIS, false, shift,
                new Count(PAN, Quantity.of("5")), new Count(BOCADILLO, Quantity.of("10"))));

        assertThat(closed.isOpen()).isFalse();
        // Pan: 40 − 5 = 35 × 0.50 = 17.50. Bocadillo: no se vendió nada por conteo.
        assertThat(closed.countedSales()).isEqualByComparingTo("17.50");
        assertThat(closed.registeredSales()).isEqualByComparingTo("12.00");
        assertThat(closed.expectedAmount()).isEqualByComparingTo("29.50");
        assertThat(ledgerCalls).hasSize(1);
        assertThat(ledgerCalls.getFirst()).containsExactly(new Sold(PAN, Quantity.of("35")));
        assertThat(closed.items()).filteredOn(i -> i.productId().equals(PAN)).singleElement()
                .satisfies(item -> {
                    assertThat(item.openingStock()).isEqualTo(Quantity.of("40"));
                    assertThat(item.leftover()).isEqualTo(Quantity.of("5"));
                });
    }

    @Test
    @DisplayName("un sobrante mayor que el stock registrado se rechaza nombrando el producto y no cambia nada (RN-15)")
    void rejectsLeftoverAboveStock() {
        Shift shift = openShift();

        assertThatThrownBy(() -> service.close(closing(LUIS, false, shift, new Count(PAN, Quantity.of("41")))))
                .isInstanceOf(LeftoverExceedsStockException.class)
                .hasMessageContaining("Pan francés");

        assertThat(ledgerCalls).isEmpty();
        assertThat(service.current(SITE).isOpen()).isTrue();
    }

    @Test
    @DisplayName("solo el responsable o un administrador cierran el turno")
    void onlyResponsibleOrAdminClose() {
        Shift shift = openShift();

        assertThatThrownBy(() -> service.close(closing(ANA, false, shift, new Count(PAN, Quantity.ZERO))))
                .isInstanceOf(ShiftForbiddenException.class);
        assertThat(service.close(closing(ANA, true, shift, new Count(PAN, Quantity.ZERO))).isOpen()).isFalse();
    }

    @Test
    @DisplayName("un turno cerrado no se cierra de nuevo")
    void closedShiftIsNotModified() {
        Shift shift = openShift();
        service.close(closing(LUIS, false, shift, new Count(PAN, Quantity.of("40"))));

        assertThatThrownBy(() -> service.close(closing(LUIS, false, shift, new Count(PAN, Quantity.of("40")))))
                .isInstanceOf(ShiftAlreadyClosedException.class);
        assertThat(ledgerCalls).isEmpty();
    }

    @Test
    @DisplayName("rechaza un producto repetido o inexistente en el conteo")
    void rejectsInvalidCounts() {
        Shift shift = openShift();

        assertThatThrownBy(() -> service.close(closing(LUIS, false, shift,
                new Count(PAN, Quantity.ZERO), new Count(PAN, Quantity.ZERO))))
                .isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> service.close(closing(LUIS, false, shift, new Count(UUID.randomUUID(), Quantity.ZERO))))
                .isInstanceOf(InvalidShiftException.class);
    }
}
