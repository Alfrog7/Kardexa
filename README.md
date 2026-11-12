<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/branding/banner-oscuro.svg">
  <img alt="Kardexa — Inventario y ventas para pequeños negocios" src="assets/branding/banner.svg" width="100%">
</picture>

<p align="center">
  <a href="https://github.com/Alfrog7/Kardexa/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/Alfrog7/Kardexa/actions/workflows/ci.yml/badge.svg"></a>
  <a href="LICENSE"><img alt="Licencia MIT" src="https://img.shields.io/badge/licencia-MIT-0F766E"></a>
  <img alt="Estado: fase 6 cerrada" src="https://img.shields.io/badge/estado-fase%206%20%C2%B7%20ventas-0F766E">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-0F172A?logo=openjdk&logoColor=white">
  <img alt="Spring Boot 4" src="https://img.shields.io/badge/Spring%20Boot-4-0F172A?logo=springboot&logoColor=6DB33F">
  <img alt="React + TypeScript" src="https://img.shields.io/badge/React-TypeScript-0F172A?logo=react&logoColor=61DAFB">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-Flyway-0F172A?logo=postgresql&logoColor=4169E1">
</p>

**Kardexa** es un sistema de inventario y ventas para pequeños negocios: bodegas, ferreterías y minimarkets. El nombre viene del *kárdex*, la tarjeta de control de inventario muy usada en Perú.

## El problema

Muchos negocios pequeños controlan su stock en un cuaderno o en Excel. No saben qué productos se van a acabar, cuáles se venden más ni cuánto ganan, y las compras y ventas no quedan registradas de forma confiable.

## La propuesta

- **Kárdex por producto:** cada entrada y salida queda registrada con su saldo.
- **Alertas de stock mínimo** antes de que el producto se agote.
- **Compras que suman stock y ventas que lo descuentan**, con comprobante interno.
- **Panel con indicadores:** ventas por día, productos más vendidos y stock crítico.

Este repositorio también documenta **el proceso**: requisitos, diagramas, decisiones de arquitectura (ADR) y una bitácora semanal.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 · arquitectura hexagonal · Spring Security con JWT propio (fase 4) |
| Base de datos | PostgreSQL 18 · Flyway |
| Frontend (panel) | React 19 · TypeScript 7 · Vite 8 · React Router 8 |
| Documentación de API | OpenAPI / Swagger |
| Landing | Astro |
| Entorno local | Docker Compose (PostgreSQL) |
| CI | GitHub Actions |

> Decisión y alternativas en el [ADR 0001](docs/adr/0001-stack-tecnologico.md). Sin servicios externos de autenticación: todas las APIs son propias.

## Cómo levantarlo en local

Requisitos: **JDK 21**, **Node.js 22.12+** y **Docker Desktop** encendido (solo para PostgreSQL).

```bash
cp .env.example .env                       # 0. Configuración local (solo la primera vez)
docker compose up -d                       # 1. PostgreSQL en localhost:5432 (o el DB_PORT del .env)
cd backend && ./mvnw spring-boot:run       # 2. API en http://localhost:8080
cd frontend && npm install && npm run dev  # 3. Panel en http://localhost:5173
```

En Windows (PowerShell) se usa `Copy-Item .env.example .env` y `.\mvnw.cmd` en lugar de `./mvnw`. La API lee el mismo `.env` que Docker Compose, así que no hace falta repetir la configuración.

| URL | Qué es |
|---|---|
| http://localhost:5173 | Panel web |
| http://localhost:8080/swagger-ui.html | Documentación interactiva de la API |
| http://localhost:8080/actuator/health | Estado de la API |

### Probar la API en dos minutos

En Swagger UI: `POST /api/v1/categories` con `{"name": "Bebidas"}`, copiar el `id` de la respuesta y usarlo en `POST /api/v1/products`. O con `curl`:

```bash
curl -s -X POST localhost:8080/api/v1/categories -H "Content-Type: application/json" -d '{"name": "Bebidas"}'
curl -s -X POST localhost:8080/api/v1/products -H "Content-Type: application/json"   -d '{"code": "AB-001", "name": "Gaseosa 500 ml", "categoryId": "<id>", "unit": "UNIT", "salePrice": "3.50", "minStock": "5"}'
```

### Datos de ejemplo

Con el backend corriendo y un `ADMIN` creado, `scripts/datos-demo.py` carga productos `DEMO-*`, un proveedor, una empleada de prueba y una compra, registra ventas, anula una y comprueba las reglas principales (stock insuficiente, permisos, correlativo). Solo usa Python estándar; pide las credenciales por consola si no están en `KARDEXA_ADMIN_EMAIL` y `KARDEXA_ADMIN_PASSWORD`. Úsalo solo en una base local: cada ejecución agrega ventas.

```bash
python scripts/datos-demo.py
```

### Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `docker compose up` falla con `bind: ... 5432` | Ya hay un PostgreSQL instalado en el equipo usando ese puerto | En `.env` poner `DB_PORT=5433`, `docker compose down` y `docker compose up -d`. La API toma el puerto del mismo `.env`. |
| `./mvnw verify` falla con `Could not find a valid Docker environment` | Docker Desktop está apagado | Encender Docker Desktop: los tests levantan su propio PostgreSQL con Testcontainers. |
| La API no arranca: `Connection refused` | El contenedor de PostgreSQL no está en marcha | `docker compose up -d` y revisar con `docker compose ps`. |

Más detalle en [backend/README.md](backend/README.md) y [frontend/README.md](frontend/README.md).

## Plan por fases

| Fase | Contenido | Estado |
|---|---|---|
| 0 · Problemática | Problema, actores, alcance, requisitos e historias de usuario | ✅ Completada |
| 1 · Diseño | C4, modelo ER, flujos, contrato OpenAPI, ADRs | ✅ Completada |
| 2 · Base técnica | Monorepo, Docker Compose, esqueletos, CI | ✅ Completada |
| 3 · Catálogo e inventario | Productos, categorías, movimientos, stock mínimo | ✅ Completada |
| 4 · Autenticación | Registro, login, roles, JWT | ✅ Completada |
| 5 · Proveedores y compras | Una compra suma stock | ✅ Completada |
| 6 · Ventas | Una venta descuenta stock | ✅ Completada |
| 7 · Panel y reportes | Dashboard con indicadores | ⚪ Pendiente |
| 8 · Landing | Página pública del producto | ⚪ Pendiente |
| 9 · Despliegue | Publicación gratuita | ⚪ Pendiente |

El avance se sigue en los [milestones](https://github.com/Alfrog7/Kardexa/milestones) y en los [issues](https://github.com/Alfrog7/Kardexa/issues).

## Estructura del repositorio

```text
Kardexa/
├── assets/branding/   # logo, isotipo, banner, redes, papelería y guía
├── docs/              # problemática, requisitos, arquitectura, ADRs y bitácora
├── backend/           # API Spring Boot · hexagonal por módulo (ADR 0002)
├── frontend/          # panel React · por funcionalidades (ADR 0005)
├── landing/           # página pública (fase 8)
├── .github/workflows/ # CI
├── docker-compose.yml # PostgreSQL local
├── CHANGELOG.md
└── CONTRIBUTING.md
```

## Documentación

**Fase 0 · Problemática**

- [01 · Problemática](docs/01-problematica.md): contexto, problema, actores, objetivos y alcance del MVP.
- [02 · Requisitos](docs/02-requisitos.md): funcionales, no funcionales, reglas de negocio y trazabilidad.
- [03 · Historias de usuario](docs/03-historias-de-usuario.md): 18 historias con criterios de aceptación.
**Fase 1 · Diseño**

- [Arquitectura](docs/arquitectura/README.md): C4 (contexto, contenedores, componentes), modelo ER, flujos y glosario.
- [Decisiones (ADR)](docs/adr/README.md): stack, hexagonal, nombres, monorepo, frontend y concurrencia.
- [Contrato de la API](docs/api/README.md): OpenAPI 3.1 con 27 operaciones.

**Proceso**

- [Bitácora](docs/bitacora/README.md): qué hice, qué aprendí y qué decidí cada semana.

**Proyecto**

- [Guía de contribución](CONTRIBUTING.md): flujo de trabajo, ramas, commits y etiquetas.
- [Identidad visual](docs/marca.md): logo, paleta y tipografía.
- [Changelog](CHANGELOG.md)

## Autor

**Alfredo Guzmán** · [@Alfrog7](https://github.com/Alfrog7)

## Licencia

[MIT](LICENSE) © 2026 Alfredo Guzmán
