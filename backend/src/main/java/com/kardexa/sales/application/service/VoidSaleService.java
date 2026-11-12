package com.kardexa.sales.application.service;

import com.kardexa.sales.application.port.in.VoidSaleUseCase;
import com.kardexa.sales.application.port.out.SaleRepository;
import com.kardexa.sales.application.port.out.StockLedger;
import com.kardexa.sales.domain.exception.SaleNotFoundException;
import com.kardexa.sales.domain.model.Sale;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * HU-15. Coordina la anulación con el inventario dentro de una sola transacción, que aporta el
 * decorador registrado en {@code SalesConfig} junto con los reintentos (ADR 0010).
 */
public class VoidSaleService implements VoidSaleUseCase {

    private final SaleRepository sales;
    private final StockLedger stockLedger;
    private final Clock clock;

    public VoidSaleService(SaleRepository sales, StockLedger stockLedger, Clock clock) {
        this.sales = Objects.requireNonNull(sales);
        this.stockLedger = Objects.requireNonNull(stockLedger);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Sale voidSale(VoidSaleCommand command) {
        Sale sale = sales.findById(command.saleId()).orElseThrow(() -> new SaleNotFoundException(command.saleId()));
        sale.voidWith(new Sale.Voiding(command.reason(), command.userId(),
                clock.instant().truncatedTo(ChronoUnit.MICROS)));
        stockLedger.giveBack(sale);
        sales.saveVoiding(sale);
        return sale;
    }
}
