# 0012 · Indicadores y reportes de solo lectura en el módulo `reporting`

- **Estado:** Aceptado
- **Fecha:** 2026-11-14
- **Requisitos relacionados:** RF-22, RF-23, RF-24, RF-25, RN-05, RN-09, RNF-03, RNF-06

## Contexto

La Fase 7 agrega el inicio con indicadores (HU-17) y la exportación a CSV (HU-18). Son las primeras funciones que **solo leen** datos de varios módulos a la vez: ventas e ítems (`sales`), stock y kárdex (`inventory`) y productos (`catalog`). Antes de programarlas hay que fijar:

- **Dónde viven** las consultas sin romper la arquitectura hexagonal ni la regla de que cada tabla la escribe su módulo.
- **Cómo se cuenta:** qué es un día, qué ventas suman y qué rango se permite.
- **Cómo se exporta:** un CSV que abra bien en Excel en español, que no cargue todo en memoria y que no sea una puerta para fórmulas maliciosas.
- **Quién ve qué:** los indicadores muestran montos de todo el negocio.

## Alternativas

**Dónde calcular los indicadores**

| Opción | A favor | En contra |
|---|---|---|
| Cada módulo expone un caso de uso de lectura y `reporting` los combina en memoria | Ningún SQL cruza módulos | Para «más vendidos» habría que traer todas las líneas de venta del rango y agrupar en Java: lento con 100 000 movimientos (RNF-06) |
| Tablas de resumen que se actualizan con cada venta | Lectura instantánea | Escrituras extra en la transacción de la venta, datos duplicados que pueden desincronizarse |
| **Consultas agregadas en un adaptador de persistencia de `reporting` (ADR 0007)** | La base agrupa y suma; una consulta por indicador; nada nuevo que mantener sincronizado | `reporting` conoce tablas ajenas: se limita a lectura y se cubre con tests de integración |

**Formato del CSV**

| Opción | A favor | En contra |
|---|---|---|
| Coma como separador y punto decimal (RFC 4180 puro) | Estándar | Excel con configuración regional de Perú lo abre todo en una sola columna |
| **Punto y coma, UTF-8 con BOM, punto decimal sin separador de miles** | Excel en español lo separa en columnas y muestra las tildes; los números siguen siendo legibles por otras herramientas | No es el CSV «por defecto» de algunas herramientas: se documenta |

## Decisión

Los indicadores y los reportes viven en el módulo **`reporting`** (ya previsto en el diagrama C4) y son de solo lectura.

1. **Arquitectura:** `reporting` tiene sus puertos de entrada (`GetDashboardUseCase`, `ExportKardexUseCase`, `ExportSalesUseCase`), servicios y controladores propios. Las consultas que cruzan `sale`, `sale_item`, `product`, `product_stock` y `stock_movement` van en `reporting/infrastructure/adapter/out/persistence` con `JdbcClient`, siguiendo el ADR 0007: solo `SELECT`, con un comentario que nombra la tabla ajena y un test de integración por consulta. `reporting` nunca escribe.
2. **Endpoint del panel:** `GET /api/v1/dashboard?from=&to=` devuelve todo el inicio en una sola respuesta (`summary`, `salesByDay`, `topProducts`, `criticalStock`) para que el panel haga una sola llamada. Cada historia de la fase agrega su parte al mismo contrato.
3. **Día y rango:**
   - Los días se cuentan en hora de Lima (`America/Lima`): `date_trunc('day', sold_at AT TIME ZONE 'America/Lima')`. Una venta a las 23:30 de Lima pertenece a ese día aunque en UTC ya sea el siguiente.
   - `from` y `to` son fechas inclusivas. Sin fechas, los últimos 30 días incluido hoy. `from > to` o más de **366 días** responden 400.
   - `salesByDay` trae **todos** los días del rango, los que no tuvieron ventas con total 0 y cantidad 0 (`generate_series` en la consulta), para que el gráfico no salte días.
4. **Qué suma:**
   - Solo ventas `REGISTERED`; las `VOIDED` no cuentan en totales, cantidades ni más vendidos (RF-22).
   - Los montos se suman en la base con 2 decimales. El ticket promedio es `total / número de ventas`, redondeado *half-up* a 2 decimales, y 0 si no hubo ventas.
   - «Más vendidos»: los 10 primeros por cantidad, desempatando por monto y luego por nombre, con el código y el nombre copiados en la venta (RN-06): un producto inhabilitado después de vender sigue apareciendo.
5. **Stock crítico (RN-09):** productos activos con `coalesce(stock, 0) <= min_stock`, la misma condición que el filtro `criticalOnly` del catálogo. No depende del rango: es el estado actual. Se ordenan primero los de stock 0 y luego por `stock - min_stock`.
6. **Índice:** las consultas por rango filtran por `status` y `sold_at`. La migración **`V14`** agrega `ix_sale_status_sold_at ON sale (status, sold_at)` si las pruebas con los datos de ejemplo muestran que `ix_sale_sold_at` no basta; se decide en la HU-17 (API) con `EXPLAIN`.
7. **CSV (RF-25):**
   - `text/csv; charset=utf-8`, con BOM, separador `;`, fin de línea `\r\n`, comillas dobles cuando el valor tiene `;`, comillas o saltos de línea.
   - Fechas `AAAA-MM-DD HH:mm` en hora de Lima; cantidades y montos con punto decimal y sin separador de miles.
   - Las columnas y los filtros son los mismos que muestra la pantalla.
   - **Inyección de fórmulas:** una celda de texto que empieza con `=`, `+`, `-`, `@`, tabulador o retorno de carro se antepone con `'` para que la hoja de cálculo no la ejecute.
   - Se escribe por partes con un `StreamingResponseBody` y una consulta que se recorre fila a fila, sin paginar ni cargar todo en memoria; el rango máximo es el mismo de 366 días.
   - `Content-Disposition: attachment` con un nombre que dice qué contiene, por ejemplo `kardex-P-0001-2026-11-01_2026-11-30.csv` o `ventas-2026-11-01_2026-11-30.csv`.
8. **Permisos:** `/dashboard` y todos los `/export` son solo `ADMIN` (RN-05, RNF-03); el `EMPLEADO` recibe 403 y el panel no le muestra indicadores, sino accesos rápidos. El panel descarga el CSV con `fetch` y el token, no con un enlace directo, para no exponer el archivo sin sesión.

## Consecuencias

- ✅ Cada indicador es una consulta agregada: el inicio responde rápido sin tablas de resumen que mantener.
- ✅ `sales`, `inventory` y `catalog` no cambian; `reporting` solo lee.
- ✅ El CSV abre en Excel en español con columnas y tildes correctas, y no ejecuta fórmulas.
- ⚠️ `reporting` depende de la forma de tablas ajenas: si `sale` o `stock_movement` cambian, sus tests de integración lo avisan.
- ⚠️ El punto y coma no es lo que esperan todas las herramientas; quien use otra herramienta debe elegir el separador al importar.
- ⚠️ Las issues de la fase que mencionan un módulo `dashboard` en el backend se implementan en `reporting`; `dashboard` es el nombre de la funcionalidad en el panel (`features/dashboard`).
