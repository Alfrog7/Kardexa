# Changelog

Todos los cambios relevantes de Kardexa se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones siguen el plan por fases (`vX.Y-faseN`).

## [Sin publicar]

### Agregado

- **ADR 0009 · Compras entre módulos:** `purchasing` coordina una sola transacción con `inventory` y `catalog`, con reintentos ante conflictos de stock.
- **HU-11 · Gestionar proveedores (API):** `GET /api/v1/suppliers?q=`, `POST` y `PUT /api/v1/suppliers/{id}`. RUC único de 11 dígitos (RN-10); crear y editar son solo de `ADMIN`. Tabla `supplier` (migración V8).
- **HU-12 · Registrar una compra (API):** `POST /api/v1/purchases` suma el stock con una entrada `PURCHASE` por producto y fija el costo referencial con el último costo comprado (RN-08), todo en una transacción con reintentos (RNF-04, ADR 0009). `GET /api/v1/purchases` lista por fecha de compra y pagina. Solo `ADMIN`. Tablas `purchase` y `purchase_item` (migración V9).
- **Panel · Proveedores:** `/proveedores` para buscar por RUC o razón social, registrar, editar, activar y desactivar (HU-11). El RUC inválido o repetido se marca en su campo; el empleado solo consulta.

## [v0.4-fase4] - 2026-11-09

### Agregado

- **ADR 0008 · Autenticación con JWT propio:** HS256 emitido por la API con Spring Security, 60 min configurables, sin *refresh tokens*, token en memoria en el panel y BCrypt.
- **HU-01 · Registrar al primer administrador (API):** `POST /api/v1/auth/setup`, solo mientras no exista ningún usuario (403 después). Tabla `app_user` (migración V6).
- **HU-02 · Iniciar sesión (API):** `POST /api/v1/auth/login` y `GET /api/v1/auth/me`. Credenciales incorrectas o usuario desactivado devuelven el mismo 401 genérico; un token ausente, inválido o vencido devuelve 401 en Problem Details.
- **HU-03 · Gestionar usuarios (API):** `GET`, `POST` y `PATCH /api/v1/users`, solo `ADMIN`. Correo único sin distinguir mayúsculas y RN-11: el último administrador activo no puede desactivarse ni cambiar de rol.
- **HU-04 · Cambiar la contraseña propia (API):** `PUT /api/v1/auth/me/password`, con la contraseña actual.
- **Permisos por rol (RN-05, RNF-03):** toda la API exige sesión. Crear y editar categorías y productos, y registrar ajustes, son solo de `ADMIN`; un `EMPLOYEE` puede consultar.
- **Usuario real en el kárdex:** los ajustes guardan quién los hizo (clave foránea, migración V7) y el kárdex muestra su nombre. Los movimientos anteriores siguen como «Sistema».
- **Panel · Ingreso y sesión:** `/ingresar` conectado a la API, rutas protegidas, aviso de sesión vencida, cierre de sesión y acciones de administración ocultas para el empleado.
- **Panel · Registro inicial:** `/registro-inicial` para crear al primer administrador.
- **Panel · Usuarios:** `/usuarios` (solo `ADMIN`) con alta, edición, activar y desactivar, y cambio de la contraseña propia desde el menú del usuario.

### Cambiado

- `JWT_SECRET` y `JWT_EXPIRATION_MINUTES` son nuevas variables de entorno (ver `.env.example`). Sin `JWT_SECRET` se usa una clave de desarrollo: hay que definir la propia fuera de local.

### Pendiente de decisión

- Si el empleado debe poder corregir el stock (hoy RN-05 lo reserva al administrador). Se revisa con el uso real en la Fase 6.

## [v0.3-fase3] - 2026-11-08

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
