# 0010 · Ventas que descuentan stock con comprobante correlativo

- **Estado:** Aceptado
- **Fecha:** 2026-11-12
- **Requisitos relacionados:** RF-18, RF-19, RF-20, RF-21, RN-01, RN-03, RN-05, RN-06, RN-07, RN-12, RNF-03, RNF-04, RNF-05

## Contexto

Una venta (HU-14) descuenta stock de varios productos y genera un comprobante interno con número correlativo (`V-000001`). Como la compra, escribe en dos módulos a la vez: `sales` guarda la venta y `inventory` registra una salida por producto. Todo o nada (RNF-04), sin stock negativo (RN-01), y dos ventas simultáneas del último ítem no pueden registrarse las dos (RNF-05).

El [ADR 0009](0009-compras-entre-modulos.md) ya resolvió la transacción entre módulos para las compras. Quedan tres preguntas propias de las ventas:

- **Precio (RN-06):** de dónde sale el precio que se copia en el ítem.
- **Correlativo (RN-07):** cómo se genera un número único, sin repetirse bajo concurrencia y sin reutilizarse al anular.
- **Permisos (RN-05, RF-21):** quién registra, quién anula y qué ve cada rol en el historial.

## Alternativas

**Correlativo**

| Opción | A favor | En contra |
|---|---|---|
| `MAX(number) + 1` al insertar | Sin objetos extra | Dos ventas simultáneas leen el mismo máximo: choca con el índice único y obliga a reintentar |
| Secuencia de PostgreSQL (`nextval`) | Rápida, nunca se repite, no bloquea | No es transaccional: cada venta rechazada o reintentada deja un hueco en la numeración |
| **Tabla contador con `UPDATE … RETURNING` dentro de la transacción** | Sin huecos: si la venta se revierte, el número también; el bloqueo dura solo hasta el commit | Las ventas se serializan en el último paso; aceptable para el volumen de una bodega |

**Precio**

| Opción | A favor | En contra |
|---|---|---|
| El panel envía el precio de cada ítem | El usuario ve exactamente lo que se cobra | Cualquiera podría vender a otro precio con una llamada a la API |
| **El servidor toma el precio de venta vigente del catálogo** | Una sola fuente de verdad; el panel solo muestra | Si el precio cambia entre que se arma la venta y se confirma, se cobra el nuevo |

## Decisión

`sales` **coordina** igual que `purchasing` en el ADR 0009, con estos detalles:

1. **Transacción y reintentos:** `RetryingRegisterSale` y `RetryingVoidSale`, en `sales/infrastructure/config`, abren la transacción y repiten todo hasta 3 veces ante `ConcurrentModificationException`; luego la API responde 409. El stock se mueve con `RecordStockMovementsUseCase` de `inventory` (`PROPAGATION_MANDATORY`) con origen `SALE` o `SALE_VOID` y el id de la venta, a través de un puerto de salida `StockLedger` en `sales`.
2. **Precio copiado (RN-06):** el panel envía solo producto y cantidad. `sales` lee el nombre, el código y el precio de venta vigente de los productos con un puerto de salida `SaleProducts`, implementado con un caso de uso de lectura nuevo en `catalog` (`FindProductsForSaleUseCase`, una consulta para todos los ids). Copia nombre, código y precio en cada ítem y calcula subtotales y total en soles con 2 decimales y redondeo *half-up* (RN-12). Un producto inexistente, desactivado o repetido rechaza la venta.
3. **Correlativo (RN-07):** una tabla `sale_number_counter` de una fila. Al final de la transacción, justo antes de guardar la venta, `UPDATE sale_number_counter SET last_value = last_value + 1 RETURNING last_value` reserva el número y se formatea como `V-` seguido de 6 dígitos (más dígitos si hiciera falta). El número tiene un índice único. Pedirlo al final mantiene el bloqueo de la fila el menor tiempo posible; si la venta falla o se reintenta, el número se libera con el *rollback* y no quedan huecos. Anular no lo libera: la venta conserva su número.
4. **Orden dentro de la transacción:** primero `inventory` (valida productos activos y RN-01, ordena las líneas por `product_id`), después el número y por último la venta con sus ítems.
5. **Anulación (HU-15):** mismo enfoque que la HU-13: una entrada `SALE_VOID` por ítem, la venta pasa a `VOIDED` con motivo, fecha y usuario, y un `UPDATE … WHERE status = 'REGISTERED'` evita la doble anulación concurrente (0 filas → 422). Devolver stock nunca lo deja negativo, así que la anulación no falla por RN-01.
6. **Permisos:**
   - Registrar ventas: `ADMIN` y `EMPLEADO`.
   - Anular: solo `ADMIN`, con motivo obligatorio (RN-05).
   - Historial y detalle: el `ADMIN` ve todas y filtra por vendedor; el `EMPLEADO` solo ve las suyas aunque envíe otro `sellerId`, y el detalle de una venta ajena le responde 404 para no revelar que existe (RF-21, RNF-03).
   - Las ventas no muestran costos ni márgenes, por eso el `EMPLEADO` puede verlas.

## Consecuencias

- ✅ La venta, sus ítems, sus salidas y su número se guardan o se revierten juntos (RNF-04), y RN-01 y el bloqueo optimista siguen solo en `inventory`.
- ✅ La numeración no tiene huecos ni repetidos, y el precio no se puede alterar desde el panel.
- ⚠️ La fila del contador serializa el último paso de cada venta. Con el volumen de una bodega no se nota; si algún día molesta, se cambia por una secuencia aceptando huecos.
- ⚠️ Si el precio cambia mientras alguien arma una venta, se cobra el precio nuevo. El comprobante muestra el precio copiado, que es el que vale.
- ⚠️ Igual que en el ADR 0009, el caso de uso no debe tener efectos fuera de la base dentro de la transacción, porque se repite al reintentar.
