# Kardexa · Panel web

Panel de operación de Kardexa: React 19 · TypeScript 7 · Vite 8 · React Router 8, organizado por funcionalidades ([ADR 0005](../docs/adr/0005-frontend-por-funcionalidades.md)).

## Requisitos

- Node.js 22.12 o superior

## Comandos

```bash
npm install
npm run dev         # http://localhost:5173 (redirige /api al backend en :8080)
npm run build       # typecheck + build de producción en dist/
npm run typecheck   # TypeScript en modo estricto
npm run lint        # oxlint; las advertencias también fallan
npm test            # Vitest + Testing Library
```

## Configuración

Copia `.env.example` como `.env.local` si necesitas cambiar algo.

| Variable | Por defecto | Descripción |
|---|---|---|
| `VITE_API_URL` | `/api/v1` | URL base de la API. En desarrollo pasa por el proxy de Vite. |

## Estructura

```text
src/
├── main.tsx                # arranque: router y estilos globales
├── app/                    # compone las funcionalidades
│   ├── router.tsx          # árbol de rutas
│   ├── navigation.ts       # URL (en español) y menú
│   ├── layout/             # PanelLayout: barra lateral + contenido
│   └── NotFoundPage.tsx
├── features/               # una carpeta por módulo del backend
│   ├── auth/               # inicio de sesión            (fase 3)
│   ├── catalog/            # productos y categorías      (fase 4)
│   ├── inventory/          # kárdex, ajustes, alertas    (fase 4)
│   ├── purchasing/         # proveedores y compras       (fase 5)
│   ├── sales/              # ventas y comprobantes       (fase 6)
│   └── dashboard/          # indicadores                 (fase 7)
│       ├── api/            # llamadas HTTP y tipos del contrato
│       ├── components/     # componentes propios
│       ├── pages/          # pantallas enrutables
│       └── index.ts        # API pública de la funcionalidad
└── shared/
    ├── api/http.ts         # cliente HTTP + ApiError (Problem Details)
    ├── lib/format.ts       # soles y cantidades (es-PE)
    ├── styles/             # tokens de marca (claro/oscuro) y estilos globales
    └── ui/                 # Logo, ComingSoon…
```

## Reglas

1. `shared` no importa de `app` ni de `features`.
2. Una funcionalidad no importa archivos internos de otra; solo su `index.ts`.
3. Se importa con alias: `@app/…`, `@features/…`, `@shared/…`.
4. Los colores salen de `shared/styles/tokens.css` (derivados de [docs/marca.md](../docs/marca.md)); el ámbar solo para alertas.

## Errores de la API

`apiRequest` lanza un `ApiError` con `status`, `title`, `message` (el `detail` en español), `rule` (por ejemplo `RN-01`) y `fieldErrors`. `isConflict` indica un 409 de concurrencia que el usuario puede reintentar.
