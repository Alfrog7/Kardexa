package com.kardexa.reporting.application.port.out;

import com.kardexa.reporting.domain.model.CriticalProduct;
import java.util.List;
import java.util.UUID;

/** Estado actual del stock leído de la base de datos. */
public interface StockFigures {

    /**
     * Productos activos con stock menor o igual que su mínimo, la misma regla del filtro {@code criticalOnly} del
     * catálogo (RN-09). Primero los de stock 0; luego por {@code stock - mínimo} y por nombre.
     */
    /** Stock de la sede indicada (RN-09 por sede) o, con {@code siteId} nulo, la suma de todas. */
    List<CriticalProduct> criticalStock(UUID siteId);
}
