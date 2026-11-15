package com.kardexa.inventory.application.service;

import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.DocumentMovement;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.Line;
import com.kardexa.inventory.application.port.in.RecordStockMovementsUseCase.RecordStockMovementsCommand;
import com.kardexa.inventory.application.port.in.RegisterReceiptUseCase;
import com.kardexa.inventory.domain.exception.InvalidReceiptException;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * HU-21. Valida la entrada y delega los movimientos en {@link RecordStockMovementsUseCase}, que exige la
 * transacción que aporta el decorador registrado en {@code InventoryConfig}.
 */
public class RegisterReceiptService implements RegisterReceiptUseCase {

    private final RecordStockMovementsUseCase movements;
    private final Clock clock;

    public RegisterReceiptService(RecordStockMovementsUseCase movements, Clock clock) {
        this.movements = Objects.requireNonNull(movements);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Receipt register(RegisterReceiptCommand command) {
        if (command.lines().isEmpty()) {
            throw new InvalidReceiptException("La entrada debe tener al menos un producto.");
        }
        if (command.lines().stream().map(Line::productId).distinct().count() != command.lines().size()) {
            throw new InvalidReceiptException("Un producto se repite en la entrada.");
        }
        if (command.lines().stream().anyMatch(line -> !line.quantity().isPositive())) {
            throw new InvalidReceiptException("La cantidad de cada producto debe ser mayor que 0.");
        }
        UUID id = UUID.randomUUID();
        movements.record(new RecordStockMovementsCommand(
                DocumentMovement.RECEIPT, id, command.siteId(), command.userId(), command.lines()));
        return new Receipt(id, command.siteId(), clock.instant(), command.lines());
    }
}
