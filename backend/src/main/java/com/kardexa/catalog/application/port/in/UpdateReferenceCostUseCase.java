package com.kardexa.catalog.application.port.in;

import com.kardexa.shared.domain.Money;
import java.util.Map;
import java.util.UUID;

/**
 * RN-08 · Fija el costo referencial de varios productos con el último costo comprado.
 *
 * <p>ADR 0009: se ejecuta dentro de la transacción de quien registra la compra. Si un producto no
 * existe lanza {@code ProductNotFoundException} (404).
 */
public interface UpdateReferenceCostUseCase {

    void updateReferenceCosts(Map<UUID, Money> costsByProduct);
}
