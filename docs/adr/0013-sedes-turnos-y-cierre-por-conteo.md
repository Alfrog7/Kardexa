# 0013 · Sedes con stock propio y turnos que se cierran por conteo

- **Estado:** Aceptado
- **Fecha:** 2026-11-14
- **Requisitos relacionados:** RF-26 a RF-33, RN-13 a RN-16, RN-01, RN-02, RN-03, RN-05, RN-06, RN-07, RNF-03

## Contexto

Hasta la Fase 7 Kardexa tiene **un solo inventario** y usuarios `ADMIN` y `EMPLEADO` que operan sobre él. El caso real que motiva la Fase 8 es el de una panadería con varias **sedes** en distintas zonas de la ciudad:

- El dueño quiere ver, sobre todo, **cuánto entró de dinero**: por sede, por turno y por quien atendió.
- En cada sede trabajan una o dos personas en **dos turnos** al día (por ejemplo 7:00–12:00 y 16:00–22:00). Si hay dos, solo una lleva la caja.
- Hoy anotan con lápiz. Registrar cada pan vendido en el momento sería **doble trabajo** frente al mostrador; lo que sí hacen de forma natural es contar lo que sobró al terminar.
- El pan que sobra **pasa al turno siguiente** (de la mañana a la tarde, de la tarde al día siguiente).
- Los precios son los mismos en todas las sedes. Los pedidos o encargos y los costos de insumos no se gestionan en Kardexa.

Esto cambia el modelo de datos (el stock deja de ser único), así que se decide antes de programar. También hay que dejar claro cómo **creceríamos a varios dueños** sin hacerlo todavía.

## Alternativas

**Cómo separar el stock de cada sede**

| Opción | A favor | En contra |
|---|---|---|
| Un producto distinto por sede (`Pan francés – Galeno`) | No cambia el esquema | El catálogo se multiplica, no hay precios comunes ni reportes por producto del negocio |
| Una instalación (base de datos) por sede | Aislamiento total | El dueño no ve todo junto; es lo contrario de lo que necesita |
| **Catálogo único y stock por (producto, sede)** | Un precio y un nombre por producto; el kárdex y el stock se separan por sede; el dueño suma o compara | Hay que migrar `product_stock` y `stock_movement` |

**Cómo saber lo vendido en un turno**

| Opción | A favor | En contra |
|---|---|---|
| Registrar cada venta en el momento | Detalle completo | Es la tarea que hoy hacen con lápiz; frente al mostrador no se sostiene |
| **Conteo al cierre**: vendido = lo que había en el sistema − lo que quedó | Una sola tarea al final, parecida a la actual; el stock siempre cuadra con lo contado | No dice a quién ni a qué hora se vendió; confía en el conteo |
| Voz o escaneo en el mostrador | Muy rápido | Errores de reconocimiento, hardware, y confirmar igual; se evalúa después |

**Cómo llegar a varios negocios**

| Opción | A favor | En contra |
|---|---|---|
| Base de datos o esquema por negocio | Aislamiento fuerte | Operación y migraciones multiplicadas; excesivo para empezar |
| **Un nivel `business` encima de `site`, con filtro por negocio en cada consulta** | Un solo despliegue; la sede ya es el límite natural de los datos | Cada consulta debe filtrar siempre por negocio: se vigila con tests |

## Decisión

### 1. Sede y stock por sede

- Tabla **`site`** (nombre único sin distinguir mayúsculas, dirección, activa). El `ADMIN` crea las que necesite, sin límite.
- El **catálogo es común**: productos, categorías, precios y proveedores no pertenecen a una sede.
- **`product_stock` pasa a tener clave `(product_id, site_id)`** y `stock_movement`, `sale`, `purchase` y `stock_adjustment` guardan `site_id`. El saldo del kárdex (RN-02) se calcula **por producto y sede**; un movimiento de una sede nunca toca el saldo de otra (RN-13).
- La migración crea la **«Sede principal»** y le asigna todo lo existente sin cambiar saldos ni historial.
- El **número de comprobante** sigue siendo **uno solo para todo el negocio** (`V-000001`…, RN-07): único y correlativo aunque haya varias sedes.
- El **stock crítico** (RN-09) se evalúa por sede: un producto puede estar crítico en una y bien en otra.

### 2. Quién ve y opera dónde

- `app_user.site_id`: obligatorio para el `EMPLEADO`, vacío para el `ADMIN`. La sede viaja como claim **`siteId`** en el token; cambiar a una persona de sede rige desde su siguiente inicio de sesión.
- El **`EMPLEADO`** opera solo en su sede: la toma del token e ignora cualquier `siteId` que envíe. Pedir datos de otra sede responde **404**, como ya ocurre con las ventas ajenas (RF-21).
- El **`ADMIN`** puede consultar una sede o **todas juntas**, pero para **registrar** una operación debe indicar una sede concreta (`siteId`).
- **Ajuste de RN-05 (HU-20, tras probar el panel):** la trabajadora trabaja «casi a la par» del administrador: además de vender, abrir y cerrar turno y registrar entradas, **crea y edita productos y categorías y ajusta el stock** (queda con su nombre en el kárdex). Siguen siendo del `ADMIN`: eliminar productos, compras (muestran costos), proveedores, anular ventas, usuarios, sedes, indicadores y exportaciones. No hay rol de encargado de sede.

### 3. Entradas de mercadería

- Nuevo movimiento **`RECEIPT`**: una entrada **sin costo ni proveedor** que registra la trabajadora de la sede con lo que llega (pan, bocadillos). Es distinta de la **compra** (HU-12), que sigue siendo del `ADMIN` y fija el costo referencial.
- Queda en el kárdex de la sede con usuario y hora (RN-04).

### 4. Turno

- Tabla **`shift`**: sede, responsable, hora de apertura y de cierre, estado `OPEN` o `CLOSED`, montos del cierre y observación. Un **índice único parcial** garantiza **un solo turno `OPEN` por sede** (RN-14); intentar abrir otro responde 409.
- **Quien lleva la caja abre el turno.** Si hay dos personas en el local, solo una abre y cierra; la otra puede vender y registrar entradas igual, pero no abre otro turno.
- El stock con el que abre el turno **no se escribe**: es el saldo vigente de la sede, que ya es lo que sobró del turno anterior. Se guarda una **foto por producto** (`shift_item`) para el resumen.
- Las ventas y entradas de la sede entre la apertura y el cierre **pertenecen a ese turno** (por sede y ventana de tiempo, sin `shift_id` en cada venta). No se exige turno abierto para vender: el cierre se apoya en el stock, no en vínculos entre documentos.
- Cierra el **responsable** o el `ADMIN`; el `ADMIN` también puede cerrar un turno que se quedó abierto para no bloquear al siguiente.
- Un turno `CLOSED` **no se edita ni se reabre** (RN-03, RN-14): un error se explica en la observación o se corrige con un ajuste del `ADMIN`.

### 5. Cierre por conteo

Al cerrar, la trabajadora cuenta lo que **sobró** de cada producto con stock en la sede (0 es válido) y lo envía junto con los pagos. En una sola transacción:

1. Para cada producto: `vendidoPorConteo = stockRegistrado − sobranteContado`.
2. Si el sobrante supera el stock registrado, se rechaza con 422 nombrando el producto (RN-15): lo que llegó y no se anotó debe registrarse antes como entrada.
3. Cada `vendidoPorConteo > 0` genera una salida **`SHIFT_CLOSE`** en el kárdex y deja el stock de la sede igual al sobrante contado, que es el **stock inicial del turno siguiente**.
4. Se copia el precio vigente del catálogo en `shift_item` (RN-06).
5. **Monto esperado** = suma de las ventas `REGISTERED` de la sede dentro del turno + `Σ vendidoPorConteo × precio`.

Equivale a: *vendido = stock al abrir + entradas − ventas registradas una a una − sobrante*, pero se calcula contra el saldo real, así que **cuadra aunque** alguien haya vendido sin abrir turno o la sede haya recibido pan sin anotarlo antes.

Las ventas una por una (HU-14) **siguen disponibles** y se descuentan solas; quien las use no cuenta dos veces.

### 6. Cuadre de pagos

- Al cerrar se anotan **efectivo, Yape, Plin y transferencia** (≥ 0, 2 decimales, 0 si no hubo) y una observación opcional.
- `recibido = efectivo + Yape + Plin + transferencia` y **`diferencia = recibido − esperado`**, ambos calculados y guardados **en el servidor** (RN-16). Negativo es faltante; positivo, sobrante.
- El medio de pago se registra **por turno**, no por venta: no se pide nada extra en el mostrador.

### 7. Indicadores

- `GET /dashboard` acepta `siteId`; sin él, suma todas las sedes. Se agrega el ingreso **por medio de pago** de los turnos cerrados del rango.
- `GET /shifts` (solo `ADMIN`) lista turnos por fecha y sede: responsable, horario, vendido, recibido por medio, diferencia y detalle por producto.
- Los días siguen contándose en hora de Lima (ADR 0012).

### 8. Camino a varios negocios (documentado, no implementado)

- Se agregaría la tabla **`business`** y `site.business_id`; los usuarios y el catálogo pasarían a pertenecer a un negocio.
- Toda consulta filtraría por negocio, sacado del token. Hoy no se agrega nada de esto: el sistema es **100 % local, un negocio con varias sedes**.
- Para no cerrarnos la puerta: no se usan datos globales sin sede o negocio nuevos, y el límite de aislamiento es siempre la sede.

### Fuera de alcance

Pedidos y encargos, costos de insumos, precios por sede, transferencias entre sedes, rol de encargado de sede y registro por voz.

## Consecuencias

- ✅ El dueño ve el dinero por sede, turno y responsable con una tarea final de la trabajadora, parecida a lo que ya hace con lápiz.
- ✅ El stock de cada sede siempre coincide con lo contado al cierre.
- ✅ Cada sede es un límite de datos claro: es la base para añadir `business` después.
- ⚠️ La migración toca `product_stock`, `stock_movement` y los documentos: se prueba con los datos de ejemplo de la Fase 7 y debe conservar saldos e historial.
- ⚠️ El conteo se confía a quien cierra: no hay detalle de quién vendió qué ni a qué hora, salvo las ventas registradas una por una. La diferencia de dinero es la señal de control.
- ⚠️ Un turno que nadie cierra bloquea abrir el siguiente: el `ADMIN` puede cerrarlo.
- ⚠️ Dos personas que venden a la vez en la misma sede no se distinguen en el cierre; responde la persona de la caja.
- ⚠️ Cuando se agregue `business`, el catálogo hoy sin dueño necesitará su propia migración.
