# Flujos principales

## Registrar una venta (HU-14)

```mermaid
sequenceDiagram
    autonumber
    actor E as Empleado
    participant SPA as Panel web
    participant SC as SaleController
    participant RS as RegisterSaleService
    participant CAT as catalog<br/>(FindProduct)
    participant INV as inventory<br/>(RecordStockExit)
    participant DB as PostgreSQL

    E->>SPA: Agrega productos y confirma
    SPA->>SC: POST /api/v1/sales {items}
    SC->>RS: register(command, sellerId)
    RS->>CAT: findActiveByIds(productIds)
    CAT-->>RS: nombres y precios vigentes
    Note over RS: Copia nombre y precio en cada ítem (RN-06)<br/>Ordena ítems por productId (ADR 0006)

    rect rgb(240, 253, 250)
    Note over RS,DB: Una sola transacción (RNF-04)
    RS->>DB: nextval('sale_number_seq') → V-000123 (RN-07)
    loop por cada ítem
        RS->>INV: recordExit(productId, qty, SALE, saleId)
        INV->>DB: SELECT product_stock (version)
        alt stock suficiente
            INV->>DB: UPDATE product_stock SET quantity, version+1<br/>WHERE version = :v
            INV->>DB: INSERT stock_movement (balance_after)
        else stock insuficiente (RN-01)
            INV-->>RS: InsufficientStockException
            RS-->>SC: rollback
            SC-->>SPA: 422 Problem Details "Stock insuficiente para …"
        end
    end
    RS->>DB: INSERT sale + sale_items
    end

    alt otra transacción cambió la versión
        Note over RS: OptimisticLockException → reintento (máx. 3)
        RS-->>SC: tras 3 intentos
        SC-->>SPA: 409 Conflict
    end

    RS-->>SC: venta registrada
    SC-->>SPA: 201 Created + comprobante
    SPA-->>E: Muestra comprobante V-000123
```

## Registrar una compra (HU-12)

```mermaid
sequenceDiagram
    autonumber
    actor A as Administrador
    participant SPA as Panel web
    participant PC as PurchaseController
    participant RP as RegisterPurchaseService
    participant CAT as catalog
    participant INV as inventory<br/>(RecordStockEntry)
    participant DB as PostgreSQL

    A->>SPA: Elige proveedor, agrega ítems con costo
    SPA->>PC: POST /api/v1/purchases
    PC->>PC: Valida rol ADMIN (RN-05)
    PC->>RP: register(command, userId)

    rect rgb(240, 253, 250)
    Note over RP,DB: Una sola transacción
    RP->>DB: INSERT purchase + purchase_items
    loop por cada ítem
        RP->>INV: recordEntry(productId, qty, PURCHASE, purchaseId)
        INV->>DB: UPDATE product_stock (+qty, version+1)
        INV->>DB: INSERT stock_movement IN
        RP->>CAT: updateReferenceCost(productId, unitCost) (RN-08)
    end
    end

    RP-->>PC: compra registrada
    PC-->>SPA: 201 Created
    SPA-->>A: Stock actualizado y alerta de mínimo recalculada
```

## Registrar un ajuste de stock (HU-09)

```mermaid
sequenceDiagram
    autonumber
    actor A as Administrador
    participant SPA as Panel web
    participant AC as StockAdjustmentController
    participant AS as AdjustStockService
    participant DB as PostgreSQL

    A->>SPA: Producto, entrada/salida, cantidad, motivo
    SPA->>AC: POST /api/v1/stock-adjustments
    AC->>AC: Valida rol ADMIN y motivo obligatorio (RN-05)
    AC->>AS: adjust(command, userId)

    rect rgb(240, 253, 250)
    AS->>DB: SELECT product_stock (version)
    alt salida mayor que el stock (RN-01)
        AS-->>AC: InsufficientStockException
        AC-->>SPA: 422 Problem Details
    else válido
        AS->>DB: INSERT stock_adjustment
        AS->>DB: UPDATE product_stock (version+1)
        AS->>DB: INSERT stock_movement ADJUSTMENT
        AS-->>AC: ajuste registrado
        AC-->>SPA: 201 Created
    end
    end
```

## Anular una venta (HU-15)

La anulación **no borra nada** (RN-03): marca la venta como `VOIDED` y genera, por cada ítem, un movimiento `SALE_VOID` de entrada que devuelve el stock. El número de comprobante se conserva (RN-07).
