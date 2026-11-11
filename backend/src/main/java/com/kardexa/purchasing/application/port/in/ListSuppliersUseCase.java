package com.kardexa.purchasing.application.port.in;

import com.kardexa.purchasing.domain.model.Supplier;
import java.util.List;

/** HU-11 · Listar los proveedores, activos e inactivos, ordenados por razón social. */
public interface ListSuppliersUseCase {

    /** {@code query} filtra por RUC o razón social sin distinguir mayúsculas; vacío o nulo trae todos. */
    List<Supplier> list(String query);
}
