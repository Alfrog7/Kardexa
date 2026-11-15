package com.kardexa.sites.application.port.in;

import com.kardexa.shared.domain.Quantity;
import com.kardexa.sites.domain.model.Payments;
import com.kardexa.sites.domain.model.Shift;
import java.util.List;
import java.util.UUID;

/**
 * HU-22 · Cierra el turno con el sobrante contado de cada producto (RN-15). En una sola transacción calcula lo
 * vendido por conteo, lo descuenta del stock con una salida {@code SHIFT_CLOSE} y fija el monto esperado.
 */
public interface CloseShiftUseCase {

    Shift close(CloseShiftCommand command);

    /**
     * @param userId quien cierra; solo el responsable o un administrador ({@code admin})
     * @param counts el sobrante contado por producto (0 es válido); un producto solo una vez
     * @param payments lo recibido por medio de pago; opcional (HU-23)
     */
    record CloseShiftCommand(UUID shiftId, UUID userId, boolean admin, List<Count> counts, String note,
            Payments payments) {

        public CloseShiftCommand {
            counts = List.copyOf(counts);
        }
    }

    record Count(UUID productId, Quantity leftover) {}
}
