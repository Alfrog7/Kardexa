package com.kardexa.sales.application.service;

import com.kardexa.sales.application.port.in.ListSalesUseCase;
import com.kardexa.sales.application.port.in.SaleView;
import com.kardexa.sales.application.port.out.SaleRepository;
import com.kardexa.sales.domain.exception.SaleNotFoundException;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import com.kardexa.shared.domain.BusinessCalendar;
import java.util.Objects;
import java.util.UUID;

/** Consultas de ventas (RF-19, RF-21). */
public class SaleQueryService implements ListSalesUseCase {

    private final SaleRepository sales;

    public SaleQueryService(SaleRepository sales) {
        this.sales = Objects.requireNonNull(sales);
    }

    @Override
    public ResultPage<SaleView> list(SaleFilter filter, Viewer viewer, PageQuery page) {
        UUID sellerId = viewer.admin() ? filter.sellerId() : viewer.userId();
        return sales.findViews(
                filter.from() == null ? null : BusinessCalendar.startOf(filter.from()),
                filter.to() == null ? null : BusinessCalendar.endOf(filter.to()),
                sellerId, page);
    }

    @Override
    public SaleView get(UUID id, Viewer viewer) {
        return sales.findView(id)
                .filter(sale -> viewer.admin() || sale.sellerId().equals(viewer.userId()))
                .orElseThrow(() -> new SaleNotFoundException(id));
    }
}
