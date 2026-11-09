package com.kardexa.inventory.application.port.in;

import com.kardexa.inventory.domain.model.Kardex;
import java.time.LocalDate;
import java.util.UUID;

/** HU-08 · Consultar el kárdex de un producto (RF-12). */
public interface GetKardexUseCase {

    /**
     * @param from primera fecha incluida, en hora de Lima; nula para empezar desde el primer movimiento
     * @param to   última fecha incluida, en hora de Lima; nula para llegar hasta el último movimiento
     */
    Kardex get(UUID productId, LocalDate from, LocalDate to);
}
