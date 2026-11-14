package com.kardexa.inventory.application.service;

import com.kardexa.inventory.application.port.in.ListStockMovementsUseCase;
import com.kardexa.inventory.application.port.out.StockMovementQueries;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.BusinessCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** Historial general del inventario: qué entró y salió, de qué producto y quién lo hizo. */
public class StockMovementHistoryService implements ListStockMovementsUseCase {

    private final StockMovementQueries movements;

    public StockMovementHistoryService(StockMovementQueries movements) {
        this.movements = Objects.requireNonNull(movements);
    }

    @Override
    public ResultPage<MovementEntry> list(LocalDate from, LocalDate to, MovementType type, PageQuery page) {
        Instant start = from == null ? null : BusinessCalendar.startOf(from);
        Instant end = to == null ? null : BusinessCalendar.endOf(to);
        return movements.findAll(start, end, type, page);
    }
}
