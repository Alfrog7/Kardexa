package com.kardexa.sites.application.port.out;

import com.kardexa.shared.domain.Quantity;
import java.util.List;
import java.util.UUID;

/**
 * Lo que el turno necesita del inventario: las salidas {@code SHIFT_CLOSE} de lo vendido por conteo. Lo implementa
 * un adaptador que llama al puerto de entrada de {@code inventory}; si otro cambió el stock lanza una
 * {@code ConcurrentModificationException}.
 */
public interface ShiftLedger {

    void recordSold(UUID shiftId, UUID siteId, UUID userId, List<Sold> lines);

    record Sold(UUID productId, Quantity quantity) {}
}
