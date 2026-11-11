# 0009 · Compras que cambian stock y costo en una sola transacción

- **Estado:** Aceptado
- **Fecha:** 2026-11-11
- **Requisitos relacionados:** RF-10, RF-16, RF-17, RN-01, RN-03, RN-04, RN-05, RN-08, RNF-04

## Contexto

Una compra (HU-12) es el primer caso de uso que **escribe en tres módulos a la vez**:

- `purchasing` guarda la compra y sus ítems.
- `inventory` suma el stock y registra un movimiento de kárdex por producto (RF-10, RN-04).
- `catalog` actualiza el costo referencial con el último costo comprado (RN-08).

Todo debe quedar guardado o nada (RNF-04): una compra sin sus movimientos, o con el stock sumado a medias, rompe el kárdex. La anulación (HU-13) hace lo contrario: salidas inversas, que no pueden dejar stock negativo (RN-01), sin tocar los movimientos originales (RN-03).

Hasta ahora solo había ajustes, que viven en `inventory` y tienen su propia transacción y sus reintentos en el decorador `RetryingAdjustStock` ([ADR 0006](0006-concurrencia-del-stock.md)). El [ADR 0002](0002-arquitectura-hexagonal.md) exige que un módulo use a otro solo por su `application.port.in`, y el [ADR 0007](0007-consultas-de-lectura-entre-modulos.md) dice que cada tabla la escribe únicamente su módulo dueño. Las ventas (Fase 6) tendrán el mismo problema, así que la solución debe servir también para ellas.

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| `purchasing` escribe directo en `product_stock`, `stock_movement` y `product` | Simple, una sola transacción | Rompe el ADR 0002 y el 0007; duplica RN-01 y el bloqueo optimista fuera de `inventory` |
| Eventos asíncronos (la compra se guarda y luego `inventory` reacciona) | Módulos muy desacoplados | El stock queda desfasado y una falla deja la compra sin movimientos: incumple RNF-04 |
| Eventos síncronos de Spring (`@TransactionalEventListener`, `BEFORE_COMMIT`) | Una transacción, sin llamadas directas | Flujo oculto, difícil de seguir y de probar; mete Spring en `application` |
| **Llamadas síncronas por `port.in`, con una transacción que abre el módulo que coordina** | Respeta los puertos, todo o nada, el flujo se lee en el servicio | El coordinador maneja la transacción y los reintentos; `inventory` necesita un caso de uso nuevo |

## Decisión

`purchasing` **coordina**: llama a `inventory` y a `catalog` por sus puertos de entrada, y es dueño de la única transacción y de los reintentos.

1. **Puerto nuevo en `inventory`:** `RecordStockMovementsUseCase` registra varias líneas (producto, dirección, cantidad) con un documento de origen (`PURCHASE` o `PURCHASE_VOID` y el id de la compra) y el usuario. Valida que cada producto exista y esté activo, aplica RN-01 y procesa las líneas **ordenadas por `product_id`** (ADR 0006, punto 5). Se registra con un `TransactionTemplate` en `PROPAGATION_MANDATORY`: **falla si se llama fuera de una transacción** y nunca reintenta por su cuenta, porque un reintento dentro de una transacción ajena no sirve (queda marcada para revertirse).
2. **Puerto nuevo en `catalog`:** `UpdateReferenceCostUseCase` fija el costo referencial de varios productos. También se registra en `PROPAGATION_MANDATORY`.
3. **Puertos de salida en `purchasing`** (`StockLedger`, `PurchasedProducts`), implementados por adaptadores que llaman a esos `port.in`, igual que `StockLevels` en el catálogo. El servicio de compras no conoce Spring ni los otros módulos.
4. **Transacción y reintentos en el coordinador:** `RetryingRegisterPurchase` y `RetryingVoidPurchase`, en `purchasing/infrastructure/config`, abren la transacción y repiten todo hasta 3 veces ante un conflicto de concurrencia; luego la API responde 409. Para que `purchasing` no dependa del dominio de `inventory`, `StaleStockException` pasa a heredar de una excepción nueva de `shared.domain`, `ConcurrentModificationException`, que es la que reintentan los decoradores.
5. **RN-08:** cada compra registrada fija el costo referencial de sus productos con su costo unitario, aunque la fecha de la compra sea anterior. **Anular no revierte el costo:** es un valor referencial y revertirlo exigiría guardar su historial; la siguiente compra lo corrige.
6. **Anulación:** genera una salida `PURCHASE_VOID` por ítem con el mismo id de compra como origen. Si alguna dejaría stock negativo, `inventory` rechaza la operación (422, RN-01) y se revierte todo. Una compra ya anulada se rechaza con 422.
7. **Permisos:** cualquier usuario con sesión **consulta proveedores**; crearlos y editarlos es solo de `ADMIN`. **Las compras** (listar, registrar y anular) son **solo de `ADMIN`**, porque muestran costos (RN-05). El kárdex sigue visible para todos y muestra el origen del movimiento, no su costo.

## Consecuencias

- ✅ La compra, sus ítems, los movimientos y el costo se guardan en una sola transacción (RNF-04).
- ✅ RN-01, el bloqueo optimista y el orden de los ítems siguen viviendo solo en `inventory`. Las ventas de la Fase 6 reutilizan el mismo puerto con `SALE` y `SALE_VOID`.
- ✅ `PROPAGATION_MANDATORY` hace imposible llamar a los puertos nuevos sin la transacción del coordinador; un test lo comprueba.
- ⚠️ El coordinador repite toda la operación al reintentar: el caso de uso no debe tener efectos fuera de la base (correos, archivos) dentro de la transacción.
- ⚠️ Una compra con fecha antigua registrada después de otra más nueva deja su costo como referencial. Se acepta por ahora; si molesta, se usará la fecha de la compra para decidir.
- ⚠️ Si `inventory` o `catalog` se separan algún día en otro servicio, esta transacción única tendrá que pasar a un patrón de compensación (saga).
