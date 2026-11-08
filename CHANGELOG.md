# Changelog

Todos los cambios relevantes de Kardexa se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones siguen el plan por fases (`vX.Y-faseN`).

## [Sin publicar]

## [v0.4-fase4] - 2026-11-08

### Agregado

- **Panel · Categorías:** `/categorias` para listar, crear, renombrar, desactivar y reactivar (HU-05).
- **Panel · Productos:** `/productos` con búsqueda, filtros, paginación, marca de stock crítico, alta y edición (HU-06, HU-07, HU-10).
- **Panel · Inventario:** `/inventario` con kárdex por producto y rango de fechas, y registro de ajustes de stock (HU-08, HU-09).
- **Componentes compartidos:** `Dialog` (modal y drawer sobre `<dialog>` nativo), `Toast` y tabla de datos adaptable a móvil.
- **Productos inhabilitados:** `GET /api/v1/products` acepta `includeInactive`; el panel los mantiene visibles y permite reactivarlos.
- **Identidad visual v1.0:** logo según el tema, favicons, manifiesto web, recursos de redes y papelería, y guía de marca.
- **HU-05 · Gestionar categorías (API):** `GET`, `POST` y `PUT /api/v1/categories`, con nombre único sin distinguir mayúsculas y desactivación sin borrado. Es la primera funcionalidad de punta a punta del backend.
- **HU-06 · Gestionar productos (API):** `POST`, `GET` y `PUT /api/v1/products/{id}`, con código único (RN-10), stock 0 al crear, stock crítico (RN-09) y desactivación sin borrado (RF-09). Primer cruce entre módulos: el catálogo consulta el stock a `inventory` por su puerto de entrada.
- **HU-07 · Buscar productos (API):** `GET /api/v1/products` con búsqueda por parte del código o del nombre (sin distinguir mayúsculas), filtro por categoría y paginación. Solo productos activos, en orden por nombre. El stock y las categorías de cada página se obtienen con una consulta cada uno, sin N+1.
- **HU-10 · Stock crítico (API):** filtro `criticalOnly=true` en `GET /api/v1/products` para listar los productos con stock actual menor o igual que su stock mínimo (RN-09), combinable con la búsqueda, la categoría y la paginación. ADR 0007 sobre consultas de lectura entre módulos.
- **HU-08 · Kárdex (API):** `GET /api/v1/products/{id}/kardex` muestra el saldo inicial, los movimientos en orden cronológico y el saldo final, con filtro opcional por fechas en hora de Lima (RF-12, RNF-15).
- **HU-09 · Ajustes de stock (API):** `POST /api/v1/stock-adjustments` registra entradas y salidas por merma, conteo físico o corrección. Actualiza el stock y escribe el ajuste y su movimiento de kárdex en una sola transacción, con bloqueo optimista y hasta 3 intentos (ADR 0006). El stock nunca queda en negativo (RN-01) y la base rechaza editar o borrar movimientos (RN-03).
- La API lee el mismo `.env` que Docker Compose: cambiar `DB_PORT` basta para usar otro puerto. README con prueba rápida y problemas frecuentes.
- Tests de integración contra PostgreSQL real con Testcontainers. La CI ya no necesita un servicio de base de datos.
- Respuestas Problem Details 404 (`recurso-no-encontrado`) y 409 (`conflicto`) para las excepciones de dominio.

- Licencia MIT, `.gitignore`, `.gitattributes` y `.editorconfig`.
- Plantilla de pull request y plantillas de issue (historia de usuario, tarea, bug).
- Guía de contribución con convención de ramas, commits y etiquetas.
- Identidad visual: isotipo, logo (claro y oscuro), banner y guía de marca.
- README con banner, badges, stack previsto, plan por fases y estructura.
- Documentos de la fase 0: problemática, requisitos (25 RF, 16 RNF, 12 reglas de negocio) e historias de usuario (18 HU).
- Bitácora semanal con plantilla y primera entrada (semana 45 de 2026).
- Diseño de la fase 1: seis ADRs (stack, hexagonal, nombres, monorepo, frontend por funcionalidades, concurrencia del stock).
- Diagramas C4 de contexto, contenedores y componentes, modelo entidad-relación y flujos de venta, compra y ajuste, en Mermaid.
- Glosario de términos de negocio y su nombre en el código.
- Contrato OpenAPI 3.1 de la API v1 (27 operaciones).
- Esqueleto del backend: Spring Boot 4.1.2 con Maven Wrapper, seis módulos hexagonales, objetos de valor `Money` y `Quantity`, errores Problem Details, prefijo `/api/v1`, Swagger UI, Actuator, Flyway y JaCoCo.
- Siete reglas de arquitectura con ArchUnit que hacen fallar el build si se rompe el ADR 0002.
- Esqueleto del frontend: React 19, TypeScript 7 en modo estricto, Vite 8 y React Router 8, organizado por funcionalidades, con layout del panel adaptable a móvil, pantalla de inicio de sesión, tokens de marca (claro/oscuro), cliente HTTP con Problem Details y 18 tests con Vitest.
- Entorno local con `docker-compose.yml` (PostgreSQL 18.6) y `.env.example`.
- CI con GitHub Actions: build y tests del backend contra PostgreSQL real, typecheck, lint, tests y build del frontend, ejecución por carpeta modificada y un check único "CI completa".
- Prueba de arranque completo del backend (Flyway, Actuator y Swagger) contra PostgreSQL.

### Cambiado

- Paleta de la guía de marca v1.0: acento claro `#D97706`, texto atenuado `#5B6A7F` y fondos oscuros nuevos, para cumplir los contrastes de WCAG.
- La rama por defecto del repositorio pasa a ser `main`.
- El stack pasa de Spring Boot 3 a **Spring Boot 4.1**, porque la rama 3.x ya no tiene soporte gratuito (ADR 0001).
- Las rutas de la API pasan a inglés (`/api/v1/products`) según el ADR 0003.
- Flujo de ramas `main` + `develop`: el trabajo se integra en `develop` y `main` solo recibe fases cerradas. La plantilla de PR se centra en el comportamiento que se puede probar.

### Corregido

- La plantilla de PR usa `Closes #N` para que GitHub cierre el issue al hacer merge.
- El `.gitignore` ya no ignora los paquetes `port/out` y `adapter/out` (la regla `out/` de IntelliJ se limita a la raíz de cada proyecto).
