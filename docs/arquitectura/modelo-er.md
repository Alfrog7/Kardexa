# Modelo entidad-relación

Nombres en inglés y `snake_case` ([ADR 0003](../adr/0003-criterio-de-nombres.md)). Identificadores `uuid`; montos `numeric(12,2)`; cantidades `numeric(12,3)` (RN-12).

```mermaid
erDiagram
    app_user {
        uuid id PK
        varchar full_name
        varchar email UK
        varchar password_hash "BCrypt"
        varchar role "ADMIN | EMPLOYEE"
        boolean active
        timestamptz created_at
        timestamptz updated_at
    }

    category {
        uuid id PK
        varchar name UK
        boolean active
    }

    product {
        uuid id PK
        varchar code UK
        varchar name
        uuid category_id FK
        varchar unit "UNIT | KG | LITER | BOX"
        decimal sale_price "12,2"
        decimal reference_cost "12,2 · último costo de compra"
        decimal min_stock "12,3"
        boolean active
        timestamptz created_at
        timestamptz updated_at
    }

    product_stock {
        uuid product_id PK, FK
        decimal quantity "12,3 · CHECK >= 0"
        bigint version "bloqueo optimista"
        timestamptz updated_at
    }

    stock_movement {
        uuid id PK
        bigint seq "orden real del kárdex"
        uuid product_id FK
        varchar type "PURCHASE | SALE | ADJUSTMENT | PURCHASE_VOID | SALE_VOID"
        varchar direction "IN | OUT"
        decimal quantity "12,3 · > 0"
        decimal balance_after "12,3 · saldo"
        varchar source_type "PURCHASE | SALE | ADJUSTMENT"
        uuid source_id
        uuid user_id FK
        timestamptz occurred_at
    }

    stock_adjustment {
        uuid id PK
        uuid product_id FK
        varchar direction "IN | OUT"
        decimal quantity "12,3"
        varchar reason "SHRINKAGE | PHYSICAL_COUNT | CORRECTION"
        varchar note
        uuid user_id FK
        timestamptz created_at
    }

    supplier {
        uuid id PK
        char ruc UK "11 dígitos"
        varchar business_name
        varchar phone
        varchar email
        boolean active
    }

    purchase {
        uuid id PK
        uuid supplier_id FK
        varchar supplier_document "factura del proveedor"
        date purchase_date
        decimal total "12,2"
        varchar status "REGISTERED | VOIDED"
        varchar void_reason
        uuid created_by FK
        timestamptz created_at
    }

    purchase_item {
        uuid id PK
        uuid purchase_id FK
        uuid product_id FK
        decimal quantity "12,3"
        decimal unit_cost "12,2"
        decimal subtotal "12,2"
    }

    sale {
        uuid id PK
        varchar number UK "V-000001"
        timestamptz sold_at
        decimal total "12,2"
        varchar status "COMPLETED | VOIDED"
        varchar void_reason
        uuid seller_id FK
        uuid voided_by FK
    }

    sale_item {
        uuid id PK
        uuid sale_id FK
        uuid product_id FK
        varchar product_name "copia al vender (RN-06)"
        decimal quantity "12,3"
        decimal unit_price "12,2 · copia (RN-06)"
        decimal subtotal "12,2"
    }

    category ||--o{ product : "agrupa"
    product ||--|| product_stock : "tiene"
    product ||--o{ stock_movement : "kárdex"
    product ||--o{ stock_adjustment : "se ajusta"
    supplier ||--o{ purchase : "provee"
    purchase ||--|{ purchase_item : "contiene"
    product ||--o{ purchase_item : "se compra en"
    sale ||--|{ sale_item : "contiene"
    product ||--o{ sale_item : "se vende en"
    app_user ||--o{ sale : "vende"
    app_user ||--o{ purchase : "registra"
    app_user ||--o{ stock_movement : "genera"
    app_user ||--o{ stock_adjustment : "registra"
```

## Restricciones e índices

| Tabla | Restricción / índice | Regla |
|---|---|---|
| `product_stock` | `CHECK (quantity >= 0)` | RN-01 |
| `stock_movement` | `CHECK (quantity > 0)`; índice `(product_id, occurred_at)` | RN-02, RF-12 |
| `stock_movement`, `stock_adjustment` | Un *trigger* rechaza `UPDATE` y `DELETE` | RN-03 |
| `product` | `UNIQUE (code)`; índice `lower(name)` para búsqueda | RN-10, RF-08 |
| `supplier` | `UNIQUE (ruc)`; `CHECK (ruc ~ '^[0-9]{11}$')` | RN-10 |
| `sale` | `number` tomado de la tabla `sale_number_counter` dentro de la transacción (ADR 0010); `UNIQUE (number)` | RN-07 |
| `app_user` | `UNIQUE (lower(email))` | RF-03 |

## Por qué existe `product_stock` separado de `product`

El stock cambia en cada venta; los datos del producto, casi nunca. Separarlos permite que el **bloqueo optimista** ([ADR 0006](../adr/0006-concurrencia-del-stock.md)) afecte solo al stock: editar el precio de un producto no hace fallar una venta concurrente.

## Por qué `stock_movement` usa `source_type` + `source_id`

Un movimiento puede venir de una compra, una venta o un ajuste. Una referencia polimórfica evita tres columnas FK casi siempre nulas y permite enlazar el kárdex con su documento de origen (RN-04). La integridad la garantiza el módulo `inventory`, que es el único que escribe en esta tabla.
