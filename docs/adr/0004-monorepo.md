# 0004 · Monorepo con backend, frontend y landing

- **Estado:** Aceptado
- **Fecha:** 2026-11-05
- **Requisitos relacionados:** RNF-13, RNF-16

## Contexto

Kardexa tiene tres piezas desplegables: la API, el panel y la landing. Hay que decidir si viven en uno o en varios repositorios.

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| **Un repositorio, una carpeta por pieza** | Un solo lugar para el proceso (issues, PRs, docs); un cambio de contrato API + panel cabe en un PR | CI debe filtrar por carpeta |
| Un repositorio por pieza | Historial y CI independientes | Issues y documentación repartidos; el portafolio se fragmenta |
| Monorepo con herramienta (Nx, Turborepo) | Caché y grafo de dependencias | Innecesario con tres piezas de tecnologías distintas |

## Decisión

Monorepo simple, **sin herramienta de monorepo**:

```text
Kardexa/
├── backend/      # Maven, independiente
├── frontend/     # npm, independiente
├── landing/      # npm, independiente (fase 8)
├── docs/
└── docker-compose.yml
```

Cada carpeta se construye por su cuenta. La CI ejecuta solo los jobs de las carpetas que cambiaron (filtros `paths` de GitHub Actions).

## Consecuencias

- ✅ Todo el proceso del proyecto se ve en un solo repositorio.
- ✅ Sin dependencias extra.
- ⚠️ Si el frontend y el backend se despliegan en servicios distintos, cada servicio debe apuntar a su subcarpeta.
