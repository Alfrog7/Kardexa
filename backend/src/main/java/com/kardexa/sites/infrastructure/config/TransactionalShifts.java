package com.kardexa.sites.infrastructure.config;

import com.kardexa.shared.domain.ConcurrentModificationException;
import com.kardexa.sites.application.port.in.CloseShiftUseCase;
import com.kardexa.sites.application.port.in.GetCurrentShiftUseCase;
import com.kardexa.sites.application.port.in.OpenShiftUseCase;
import com.kardexa.sites.domain.model.Shift;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Da a los casos de uso de turnos su frontera transaccional sin meter Spring en {@code application}. El cierre se
 * repite entero, hasta 3 intentos, si otro cambió el stock (ADR 0006), antes de dejar que la API responda 409.
 */
class TransactionalShifts implements OpenShiftUseCase, GetCurrentShiftUseCase, CloseShiftUseCase {

    static final int MAX_ATTEMPTS = 3;

    private final OpenShiftUseCase open;
    private final GetCurrentShiftUseCase current;
    private final CloseShiftUseCase close;
    private final TransactionTemplate transaction;

    TransactionalShifts(OpenShiftUseCase open, GetCurrentShiftUseCase current, CloseShiftUseCase close,
            TransactionTemplate transaction) {
        this.open = open;
        this.current = current;
        this.close = close;
        this.transaction = transaction;
    }

    @Override
    public Shift open(UUID siteId, UUID responsibleId) {
        return transaction.execute(status -> open.open(siteId, responsibleId));
    }

    @Override
    public Shift current(UUID siteId) {
        return current.current(siteId);
    }

    @Override
    public Shift close(CloseShiftCommand command) {
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return transaction.execute(status -> close.close(command));
            } catch (ConcurrentModificationException retryable) {
                // Se vuelve a leer el stock y se cuenta de nuevo.
            }
        }
        return transaction.execute(status -> close.close(command));
    }
}
