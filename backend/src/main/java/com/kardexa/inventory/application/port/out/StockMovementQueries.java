package com.kardexa.inventory.application.port.out;

import com.kardexa.inventory.application.port.in.ListStockMovementsUseCase.MovementEntry;
import com.kardexa.inventory.domain.model.MovementType;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lectura del kárdex. El orden es el real de los movimientos, no solo el de sus fechas. */
public interface StockMovementQueries {

    /** Saldo que dejó el último movimiento anterior a {@code instant}; 0 si no hubo ninguno. */
    Quantity balanceBefore(UUID productId, Instant instant);

    /**
     * Movimientos del producto en orden cronológico.
     *
     * @param from primer instante incluido; nulo para no limitar
     * @param to   límite exclusivo; nulo para no limitar
     */
    List<StockMovement> findMovements(UUID productId, Instant from, Instant to);

    /**
     * Movimientos de todos los productos, del más reciente al más antiguo, con el código y el nombre del
     * producto. {@code from} incluido y {@code to} excluido; los filtros nulos no se aplican.
     */
    ResultPage<MovementEntry> findAll(Instant from, Instant to, MovementType type, PageQuery page);
}
