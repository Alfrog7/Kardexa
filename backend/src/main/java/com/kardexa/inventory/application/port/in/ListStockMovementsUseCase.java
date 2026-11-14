package com.kardexa.inventory.application.port.in;

import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.time.LocalDate;

/** Historial de todos los movimientos de stock, del más reciente al más antiguo. */
public interface ListStockMovementsUseCase {

    /**
     * @param from primer día incluido, en hora de Lima; nulo para no limitar
     * @param to   último día incluido; nulo para no limitar
     * @param type solo ese tipo; nulo para todos
     */
    ResultPage<MovementEntry> list(LocalDate from, LocalDate to, MovementType type, PageQuery page);

    /** Movimiento con el código y el nombre de su producto. */
    record MovementEntry(StockMovement movement, String productCode, String productName) {}
}
