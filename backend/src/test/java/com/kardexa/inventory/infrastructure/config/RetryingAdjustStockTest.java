package com.kardexa.inventory.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kardexa.inventory.application.port.in.AdjustStockUseCase;
import com.kardexa.inventory.application.port.in.AdjustStockUseCase.AdjustStockCommand;
import com.kardexa.inventory.domain.exception.InsufficientStockException;
import com.kardexa.inventory.domain.exception.StaleStockException;
import com.kardexa.inventory.domain.model.AdjustmentReason;
import com.kardexa.inventory.domain.model.Direction;
import com.kardexa.inventory.domain.model.StockAdjustment;
import com.kardexa.inventory.domain.model.StockMovement;
import com.kardexa.shared.domain.Quantity;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("Transacción y reintentos del ajuste (ADR 0006)")
class RetryingAdjustStockTest {

    private static final UUID PRODUCT = UUID.randomUUID();
    private static final AdjustStockCommand COMMAND = new AdjustStockCommand(
            PRODUCT, Direction.IN, Quantity.of("1"), AdjustmentReason.CORRECTION, null);
    private static final StockMovement MOVEMENT = StockMovement.fromAdjustment(
            StockAdjustment.create(PRODUCT, Direction.IN, Quantity.of("1"), AdjustmentReason.CORRECTION, null, Instant.EPOCH),
            Quantity.of("1"));

    private final AtomicInteger calls = new AtomicInteger();
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

    private RetryingAdjustStock failing(int staleFailures) {
        AdjustStockUseCase delegate = command -> {
            if (calls.incrementAndGet() <= staleFailures) {
                throw new StaleStockException(PRODUCT);
            }
            return MOVEMENT;
        };
        return new RetryingAdjustStock(delegate, new TransactionTemplate(manager));
    }

    @Test
    @DisplayName("sin conflicto se ejecuta una vez")
    void runsOnce() {
        assertThat(failing(0).adjust(COMMAND)).isEqualTo(MOVEMENT);

        assertThat(calls).hasValue(1);
        verify(manager, times(1)).commit(any());
    }

    @Test
    @DisplayName("si otro cambió el stock, repite en una transacción nueva y termina bien")
    void retries() {
        assertThat(failing(2).adjust(COMMAND)).isEqualTo(MOVEMENT);

        assertThat(calls).hasValue(RetryingAdjustStock.MAX_ATTEMPTS);
        verify(manager, times(2)).rollback(any());
        verify(manager, times(1)).commit(any());
    }

    @Test
    @DisplayName("tras 3 intentos deja salir el conflicto, que la API responde con 409")
    void givesUp() {
        RetryingAdjustStock retrying = failing(Integer.MAX_VALUE);

        assertThatThrownBy(() -> retrying.adjust(COMMAND)).isInstanceOf(StaleStockException.class);

        assertThat(calls).hasValue(RetryingAdjustStock.MAX_ATTEMPTS);
        verify(manager, times(3)).rollback(any());
    }

    @Test
    @DisplayName("una regla de negocio incumplida no se reintenta")
    void doesNotRetryBusinessErrors() {
        AdjustStockUseCase delegate = command -> {
            calls.incrementAndGet();
            throw new InsufficientStockException("Arroz", Quantity.ZERO, Quantity.of("1"));
        };
        RetryingAdjustStock retrying = new RetryingAdjustStock(delegate, new TransactionTemplate(manager));

        assertThatThrownBy(() -> retrying.adjust(COMMAND)).isInstanceOf(InsufficientStockException.class);

        assertThat(calls).hasValue(1);
        verify(manager, times(1)).rollback(any());
    }
}
