# 0005 · Frontend organizado por funcionalidades

- **Estado:** Aceptado
- **Fecha:** 2026-11-05
- **Requisitos relacionados:** RNF-11, RNF-12

## Contexto

El panel tendrá pantallas de autenticación, catálogo, inventario, compras, ventas y reportes. Si se organiza por tipo de archivo (`components/`, `hooks/`, `pages/`), cada módulo queda repartido en todas las carpetas y es difícil saber qué toca a qué.

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| Por tipo de archivo | Simple al inicio | No escala; acoplamiento oculto entre módulos |
| **Por funcionalidad (`features/`)** | Cada módulo es autocontenido y refleja los módulos del backend | Hay que decidir qué es compartido |
| Feature-Sliced Design completo | Reglas muy precisas | Demasiadas capas para el tamaño del panel |

## Decisión

```text
frontend/src/
├── app/                  # arranque: router, providers, layout global
├── features/
│   └── <funcionalidad>/  # auth, catalog, inventory, purchasing, sales, dashboard
│       ├── api/          # llamadas HTTP y tipos del contrato
│       ├── components/   # componentes propios de la funcionalidad
│       ├── pages/        # pantallas enrutables
│       └── index.ts      # API pública de la funcionalidad
└── shared/
    ├── api/              # cliente HTTP base, manejo de Problem Details
    ├── ui/               # componentes genéricos (Button, Card…)
    ├── styles/           # tokens de diseño de la guía de marca
    └── lib/              # utilidades (formato de moneda, fechas)
```

### Reglas

1. `shared` no importa nada de `features` ni de `app`.
2. Una funcionalidad no importa archivos internos de otra; solo su `index.ts`.
3. `app` compone las funcionalidades (rutas y layout).
4. Los alias `@app`, `@features` y `@shared` evitan rutas relativas largas.

## Consecuencias

- ✅ Los módulos del panel se corresponden 1 a 1 con los del backend.
- ✅ Borrar o mover una funcionalidad es mover una carpeta.
- ⚠️ Las reglas 1 y 2 se revisan en los PRs; más adelante se pueden reforzar con una regla de ESLint.
