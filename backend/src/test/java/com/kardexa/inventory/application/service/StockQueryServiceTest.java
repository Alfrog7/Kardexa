package com.kardexa.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kardexa.inventory.application.port.out.ProductStockRepository;
import com.kardexa.shared.domain.Quantity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Consulta de stock")
class StockQueryServiceTest {

    private final Map<UUID, Quantity> rows = new HashMap<>();
    private final List<Collection<UUID>> batchQueries = new ArrayList<>();

    private final StockQueryService service = new StockQueryService(new ProductStockRepository() {
        @Override
        public Optional<Quantity> findQuantity(UUID productId) {
            return Optional.ofNullable(rows.get(productId));
        }

        @Override
        public Map<UUID, Quantity> findQuantities(Collection<UUID> productIds) {
            batchQueries.add(List.copyOf(productIds));
            Map<UUID, Quantity> found = new HashMap<>();
            productIds.stream().filter(rows::containsKey).forEach(id -> found.put(id, rows.get(id)));
            return found;
        }
    });

    @Test
    @DisplayName("un producto sin movimientos tiene stock 0")
    void zeroWithoutRow() {
        assertThat(service.currentStock(UUID.randomUUID())).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("por lote devuelve una entrada por id, con 0 si no hay fila, en una sola consulta")
    void batch() {
        UUID withStock = UUID.randomUUID();
        UUID withoutStock = UUID.randomUUID();
        rows.put(withStock, Quantity.of("4.5"));

        Map<UUID, Quantity> stock = service.currentStock(List.of(withStock, withoutStock));

        assertThat(stock).containsExactly(
                Map.entry(withStock, Quantity.of("4.5")), Map.entry(withoutStock, Quantity.ZERO));
        assertThat(batchQueries).hasSize(1);
    }

    @Test
    @DisplayName("una lista vacía no consulta la base")
    void emptyBatch() {
        assertThat(service.currentStock(List.of())).isEmpty();
        assertThat(batchQueries).isEmpty();
    }
}
