# 0003 · Código en inglés, producto en español

- **Estado:** Aceptado
- **Fecha:** 2026-11-05

## Contexto

La documentación, los issues y los commits van en español (regla del brief). Falta decidir el idioma de los identificadores del código: clases, métodos, tablas, columnas y rutas de la API. La regla es usar **siempre el mismo criterio**.

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| Todo en español | Coherente con el dominio y la documentación | Choca con el framework (`getProductos`, `findByNombre`), tildes y ñ prohibidas en identificadores; menos legible para equipos internacionales |
| **Código en inglés, producto en español** | Estándar de la industria; convive con Spring sin mezclas; mejor para el portafolio | Requiere un glosario para no traducir distinto el mismo término |
| Mezcla libre | Ninguno | Inconsistencia garantizada |

## Decisión

| Ámbito | Idioma | Ejemplo |
|---|---|---|
| Clases, métodos, variables, paquetes | Inglés | `RegisterSaleUseCase`, `stockMovement` |
| Tablas y columnas | Inglés, `snake_case` | `stock_movement.balance_after` |
| Rutas de la API | Inglés, plural, `kebab-case` | `/api/v1/products`, `/api/v1/stock-adjustments` |
| Mensajes de error y textos de la interfaz | Español | "Stock insuficiente para Arroz 5 kg" |
| Documentación, commits, issues, PRs | Español | — |

Los términos sin traducción natural se mantienen: `kardex`, `ruc`.

El [glosario](../arquitectura/glosario.md) fija la traducción de cada término de negocio. Si un término no está, se agrega en el mismo PR que lo usa.

## Consecuencias

- ✅ Un solo idioma dentro del código; ningún `getNombre()` junto a `getId()`.
- ✅ El usuario final nunca ve inglés.
- ⚠️ El brief mencionaba rutas como `/api/productos`; quedan reemplazadas por sus equivalentes en inglés.
