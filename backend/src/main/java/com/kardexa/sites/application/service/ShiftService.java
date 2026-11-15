package com.kardexa.sites.application.service;

import com.kardexa.shared.domain.Quantity;
import com.kardexa.sites.application.port.in.CloseShiftUseCase;
import com.kardexa.sites.application.port.in.GetCurrentShiftUseCase;
import com.kardexa.sites.application.port.in.OpenShiftUseCase;
import com.kardexa.sites.application.port.in.ReviewShiftsUseCase;
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
import com.kardexa.sites.domain.exception.ShiftNotFoundException;
import com.kardexa.sites.domain.model.Shift;
import com.kardexa.sites.domain.model.Shift.Item;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Casos de uso de HU-22 sobre los turnos. Java puro: se registra como bean en {@code SitesConfig}. El cierre
 * debe ejecutarse en una transacción, que aporta el decorador de la configuración.
 */
public class ShiftService implements OpenShiftUseCase, GetCurrentShiftUseCase, CloseShiftUseCase, ReviewShiftsUseCase {

    private final ShiftRepository shifts;
    private final ShiftStock stock;
    private final ShiftSales sales;
    private final ShiftLedger ledger;
    private final Clock clock;

    public ShiftService(
            ShiftRepository shifts, ShiftStock stock, ShiftSales sales, ShiftLedger ledger, Clock clock) {
        this.shifts = Objects.requireNonNull(shifts);
        this.stock = Objects.requireNonNull(stock);
        this.sales = Objects.requireNonNull(sales);
        this.ledger = Objects.requireNonNull(ledger);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Shift open(UUID siteId, UUID responsibleId) {
        if (shifts.findOpenBySite(siteId).isPresent()) {
            throw new ShiftAlreadyOpenException();
        }
        List<Item> snapshot = stock.withStock(siteId).stream()
                .map(line -> Item.opening(line.productId(), line.productName(), line.quantity()))
                .toList();
        Shift shift = Shift.open(siteId, responsibleId, snapshot, clock.instant().truncatedTo(ChronoUnit.MICROS));
        shifts.add(shift);
        return shifts.findById(shift.id()).orElse(shift);
    }

    @Override
    public Shift current(UUID siteId) {
        return shifts.findOpenBySite(siteId).orElseThrow(NoOpenShiftException::new);
    }

    @Override
    public List<Shift> list(java.time.LocalDate from, java.time.LocalDate to, UUID siteId) {
        var today = java.time.LocalDate.now(clock.withZone(com.kardexa.shared.domain.BusinessCalendar.ZONE));
        var end = to == null ? today : to;
        var start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new InvalidShiftException("La fecha inicial no puede ser posterior a la final.");
        }
        return shifts.search(com.kardexa.shared.domain.BusinessCalendar.startOf(start),
                com.kardexa.shared.domain.BusinessCalendar.endOf(end), siteId, LIMIT);
    }

    @Override
    public Shift get(UUID id) {
        return shifts.findById(id).orElseThrow(() -> new ShiftNotFoundException(id));
    }

    @Override
    public Shift close(CloseShiftCommand command) {
        Shift shift = shifts.findById(command.shiftId()).orElseThrow(() -> new ShiftNotFoundException(command.shiftId()));
        if (!shift.isOpen()) {
            throw new ShiftAlreadyClosedException();
        }
        if (!shift.canBeClosedBy(command.userId(), command.admin())) {
            throw new ShiftForbiddenException();
        }
        if (command.counts().stream().map(c -> c.productId()).distinct().count() != command.counts().size()) {
            throw new InvalidShiftException("Un producto se repite en el conteo.");
        }
        if (command.note() != null && command.note().strip().length() > Shift.NOTE_MAX_LENGTH) {
            throw new InvalidShiftException("La observación debe tener como máximo " + Shift.NOTE_MAX_LENGTH + " caracteres.");
        }

        // Siempre en el mismo orden (ADR 0006, punto 5), con las filas de stock bloqueadas.
        var counts = command.counts().stream().sorted(Comparator.comparing(c -> c.productId())).toList();
        Map<UUID, StockLine> locked = stock.lock(shift.siteId(), counts.stream().map(c -> c.productId()).toList());
        Map<UUID, Item> opening = new HashMap<>();
        shift.items().forEach(item -> opening.put(item.productId(), item));

        List<Item> closedItems = new ArrayList<>();
        List<Sold> sold = new ArrayList<>();
        for (var count : counts) {
            StockLine line = locked.get(count.productId());
            if (line == null) {
                throw new InvalidShiftException("El producto " + count.productId() + " no existe.");
            }
            if (count.leftover().compareTo(line.quantity()) > 0) {
                throw new LeftoverExceedsStockException(line.productName(), count.leftover(), line.quantity());
            }
            Quantity soldByCount = line.quantity().subtract(count.leftover());
            if (soldByCount.isPositive()) {
                sold.add(new Sold(line.productId(), soldByCount));
            }
            Item before = opening.get(line.productId());
            closedItems.add(new Item(line.productId(), line.productName(),
                    before == null ? Quantity.ZERO : before.openingStock(), count.leftover(), soldByCount, line.price()));
        }

        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var registered = sales.registeredTotal(shift.siteId(), shift.openedAt(), now);
        if (!sold.isEmpty()) {
            ledger.recordSold(shift.id(), shift.siteId(), command.userId(), sold);
        }
        Shift closed = shift.closed(command.userId(), now, registered, command.note(), command.payments(), closedItems);
        shifts.saveClosed(closed);
        return shifts.findById(closed.id()).orElse(closed);
    }
}
