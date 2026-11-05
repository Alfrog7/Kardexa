# 0006 · Control de concurrencia del stock

- **Estado:** Aceptado
- **Fecha:** 2026-11-05
- **Requisitos relacionados:** RN-01, RN-02, RNF-04, RNF-05

## Contexto

Dos vendedores pueden vender a la vez la última unidad de un producto. Si ambas transacciones leen `stock = 1` y descuentan 1, el stock queda en −1 y se rompe RN-01. Además, el saldo de cada movimiento del kárdex depende del anterior (RN-02), así que dos movimientos simultáneos del mismo producto no pueden calcular su saldo a partir del mismo valor.

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| Sin control | Simple | Stock negativo y saldos incorrectos bajo concurrencia |
| **Bloqueo optimista** (`@Version` en el stock del producto) | Sin bloqueos en BD; ideal con poca contención | La transacción que pierde debe reintentarse |
| Bloqueo pesimista (`SELECT … FOR UPDATE`) | Nunca hay conflicto | Bloquea filas; riesgo de *deadlocks* si el orden de bloqueo varía |
| Solo restricción `CHECK (quantity >= 0)` | La BD garantiza RN-01 | No protege el cálculo del saldo del kárdex |

## Decisión

1. El stock actual se guarda en una tabla `product_stock` con una columna `version` (**bloqueo optimista**).
2. Cada venta, compra, ajuste o anulación actualiza `product_stock` y escribe sus `stock_movement` **en la misma transacción**. El saldo del movimiento se calcula a partir del stock leído con su versión.
3. Si la versión cambió, la transacción falla con `OptimisticLockException` y el caso de uso **se reintenta hasta 3 veces** antes de responder `409 Conflict`.
4. Como defensa en profundidad, `product_stock.quantity` tiene `CHECK (quantity >= 0)`.
5. Para evitar *deadlocks* en ventas con varios productos, se procesan los ítems **ordenados por `product_id`**.

## Consecuencias

- ✅ RN-01 y RN-02 se cumplen aunque haya ventas simultáneas.
- ✅ En un negocio pequeño la contención es baja: los reintentos serán raros.
- ⚠️ Hay que cubrirlo con un test de integración concurrente contra PostgreSQL real (Testcontainers), no con H2.
