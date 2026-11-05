package com.kardexa.catalog.application.port.out;

import com.kardexa.shared.domain.Quantity;
import java.util.UUID;

/**
 * Lo que el catálogo necesita saber del inventario: el stock actual de un producto. Lo implementa
 * un adaptador que llama al puerto de entrada de {@code inventory}, así el catálogo no depende de él.
 */
public interface StockLevels {

    Quantity currentStock(UUID productId);
}
