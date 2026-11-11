package com.kardexa.purchasing.application.port.out;

import com.kardexa.shared.domain.Money;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que compras necesita del catálogo: fijar el costo referencial con el último costo comprado
 * (RN-08). Lo implementa un adaptador que llama al puerto de entrada de {@code catalog} (ADR 0009).
 */
public interface PurchasedProducts {

    void updateReferenceCosts(Map<UUID, Money> costsByProduct);
}
