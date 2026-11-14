package com.kardexa.reporting.application.port.out;

import com.kardexa.reporting.domain.model.CriticalProduct;
import java.util.List;

/** Estado actual del stock leído de la base de datos. */
public interface StockFigures {

    /**
     * Productos activos con stock menor o igual que su mínimo, la misma regla del filtro {@code criticalOnly} del
     * catálogo (RN-09). Primero los de stock 0; luego por {@code stock - mínimo} y por nombre.
     */
    List<CriticalProduct> criticalStock();
}
