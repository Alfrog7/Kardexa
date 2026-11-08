# 0007 · Consultas de lectura entre módulos

- **Estado:** Aceptado
- **Fecha:** 2026-11-07
- **Requisitos relacionados:** RF-08, RF-14, RN-09, RNF-06

## Contexto

La búsqueda de productos (HU-07) debe poder filtrar los que están en **stock crítico** (HU-10, RN-09): stock actual ≤ stock mínimo. El stock mínimo es del módulo `catalog` (tabla `product`) y el stock actual es de `inventory` (tabla `product_stock`). El resultado va **paginado**, así que el filtro tiene que aplicarse en la base: si se filtra en memoria, el total y las páginas salen mal, o hay que traer todo el catálogo en cada consulta.

El ADR 0002 dice que un módulo solo usa a otro por su `application.port.in`. Esa regla, que ArchUnit hace cumplir, se pensó para el código Java y para las **escrituras**.

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| Traer los productos y pedir el stock a `inventory` para filtrar en memoria | Respeta los puertos al pie de la letra | Pagina mal o carga el catálogo entero; incumple RNF-06 con 10 000 productos |
| `inventory` devuelve los ids en stock crítico | La lógica de stock queda en `inventory` | `inventory` necesitaría el stock mínimo, que no es suyo; la lista de ids puede ser enorme |
| Copiar `min_stock` en `product_stock` | Una sola tabla para el filtro | Dato duplicado que hay que sincronizar en cada edición del producto |
| **Consulta de lectura que une `product` con `product_stock`** en un adaptador del catálogo | Un solo viaje a la base, paginación correcta, aprovecha índices | El adaptador conoce el nombre de una tabla de otro módulo |

## Decisión

Se permiten **consultas de solo lectura entre tablas de distintos módulos**, con estas condiciones:

1. Viven en un **adaptador de persistencia** (`infrastructure.adapter.out.persistence`), nunca en dominio ni en aplicación.
2. Son **solo lectura**. Cada tabla la escribe únicamente su módulo dueño: `product_stock` solo la modifica `inventory`.
3. La consulta documenta en un comentario qué tabla ajena usa y por qué.
4. Hay un test de integración contra PostgreSQL real que cubre el cruce.

El primer caso es el filtro `criticalOnly` de `GET /api/v1/products`. Un producto sin fila en `product_stock` cuenta como stock 0.

## Consecuencias

- ✅ El filtro y la paginación salen bien con una sola consulta.
- ✅ Las reglas del ADR 0002 siguen intactas para el código Java y para las escrituras.
- ⚠️ Si un módulo se separa algún día en otro servicio, estas consultas tendrán que pasar a una vista o a un modelo de lectura propio. Por eso se limitan a adaptadores y se documentan.
- ⚠️ Renombrar una columna de `product_stock` obliga a revisar también el adaptador del catálogo. El test de integración lo detecta.
