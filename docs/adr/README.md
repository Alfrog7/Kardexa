# Registro de decisiones de arquitectura (ADR)

Cada decisión técnica relevante se documenta en un ADR: contexto, alternativas, decisión y consecuencias. Un ADR aceptado no se edita; si la decisión cambia, se escribe uno nuevo que lo **reemplaza**.

| N.º | Decisión | Estado | Fecha |
|---|---|---|---|
| [0001](0001-stack-tecnologico.md) | Stack tecnológico | Aceptado | 2026-11-05 |
| [0002](0002-arquitectura-hexagonal.md) | Arquitectura hexagonal modular en el backend | Aceptado | 2026-11-05 |
| [0003](0003-criterio-de-nombres.md) | Código en inglés, producto en español | Aceptado | 2026-11-05 |
| [0004](0004-monorepo.md) | Monorepo con backend, frontend y landing | Aceptado | 2026-11-05 |
| [0005](0005-frontend-por-funcionalidades.md) | Frontend organizado por funcionalidades | Aceptado | 2026-11-05 |
| [0006](0006-concurrencia-del-stock.md) | Control de concurrencia del stock | Aceptado | 2026-11-05 |
| [0007](0007-consultas-de-lectura-entre-modulos.md) | Consultas de lectura entre módulos | Aceptado | 2026-11-07 |
| [0008](0008-autenticacion-con-jwt.md) | Autenticación con JWT propio | Aceptado | 2026-11-09 |
| [0009](0009-compras-entre-modulos.md) | Compras que cambian stock y costo en una sola transacción | Aceptado | 2026-11-11 |
| [0010](0010-ventas-entre-modulos.md) | Ventas que descuentan stock con comprobante correlativo | Aceptado | 2026-11-12 |
| [0011](0011-sesion-en-sessionstorage.md) | La sesión del panel sobrevive a una recarga | Aceptado | 2026-11-12 |

## Plantilla

```markdown
# NNNN · Título en forma de decisión

- **Estado:** Propuesto | Aceptado | Reemplazado por NNNN
- **Fecha:** AAAA-MM-DD
- **Requisitos relacionados:** RF-XX, RNF-XX

## Contexto
¿Qué problema o fuerza obliga a decidir?

## Alternativas
| Opción | A favor | En contra |
|---|---|---|

## Decisión
Qué se decide, en una frase, y los detalles necesarios.

## Consecuencias
Positivas, negativas y lo que habrá que vigilar.
```
