# Changelog

Todos los cambios relevantes de Kardexa se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones siguen el plan por fases (`vX.Y-faseN`).

## [Sin publicar]

## [v0.7-fase7] - 2026-11-14

### Agregado

- **Datos de ejemplo para el inicio:** `scripts/datos-demo.py --repartir` registra ventas del último mes con algunas anuladas (la fecha se ajusta con `psql` en el contenedor local, porque la API solo acepta la hora actual), deja productos en stock crítico y comprueba que el total de `/dashboard` cuadra con las ventas registradas y que el gráfico, los más vendidos y el stock crítico tienen elementos (#146).
- **Exportar ventas a CSV:** `GET /api/v1/sales/export` (solo `ADMIN`) con los filtros del historial (fechas, vendedor) y estado opcional; columnas número, fecha y hora, vendedor, estado (Registrada o Anulada), cantidad de productos, total y motivo de anulación. Ventas tiene «Exportar CSV» en el historial. El detalle por producto queda fuera de alcance (#145).
- **Exportar a CSV el kárdex y los movimientos:** `GET /api/v1/products/{id}/kardex/export` y `GET /api/v1/stock-movements/export` (solo `ADMIN`) con los mismos filtros de la pantalla; columnas fecha y hora, tipo, documento (número de venta o comprobante de compra), entrada, salida, saldo, costo de compra y usuario. CSV para Excel en español (BOM, `;`, CRLF, fórmulas neutralizadas) escrito por partes. Inventario tiene «Exportar CSV» en el kárdex y en el historial, que descarga con el token (#144).
- **Más vendidos y stock crítico en el inicio:** tabla «Más vendidos» con puesto, miniatura, cantidad, monto y botón «Ver kárdex»; lista «Stock crítico» con stock y mínimo, botón «Ajustar stock» y, para el `ADMIN`, «Registrar compra», que abre Compras con el producto ya agregado (`?producto=`). Estados vacíos propios. La API agrega `imageUrl` a `topProducts`, y la miniatura del producto pasa a `shared/ui` (#143).
- **Gráfico de ventas por día en el inicio:** barras en SVG propio (sin librería), una por día del rango incluidos los días en 0, con eje de montos y fechas espaciadas en rangos largos. Al pasar el puntero o enfocar una barra muestra fecha, total y ventas; se recorre con las flechas, Inicio y Fin, y destaca el mejor día; los lectores de pantalla tienen una tabla equivalente oculta a la vista (#142).
- **Inicio con indicadores:** el `ADMIN` ve total vendido, número de ventas, ticket promedio y productos en stock crítico, con un selector de rango (Hoy, Últimos 7 días, Últimos 30 días, Este mes o fechas propias) que se guarda en la URL; cada tarjeta lleva a Ventas o a Productos ya filtrado por stock crítico (`?critico=1`). El `EMPLEADO` ve accesos a Registrar venta, Productos e Inventario y no pide indicadores (#141).
- **Stock crítico en el inicio (API):** `GET /api/v1/dashboard` incluye `criticalStock`, los productos activos con stock menor o igual que su mínimo (código, nombre, categoría, stock, mínimo e imagen), primero los agotados, y `summary.criticalCount` para la tarjeta. No depende del rango; los inhabilitados no aparecen (#140).
- **Más vendidos en el inicio (API):** `GET /api/v1/dashboard` incluye `topProducts`, los 10 productos con más cantidad vendida en el rango, con código, nombre, cantidad y monto; en empate manda el monto y luego el nombre. Las ventas anuladas no cuentan y un producto inhabilitado después sigue apareciendo (#139).
- **Ventas por día en el inicio (API):** `GET /api/v1/dashboard` (solo `ADMIN`) devuelve el total y el número de ventas de cada día de Lima del rango, con los días sin ventas en 0, y un resumen con total, número de ventas y ticket promedio; las anuladas no cuentan. Sin fechas, los últimos 30 días; como máximo 366 (#138).
- **ADR 0012 · Indicadores y reportes:** el módulo `reporting` lee ventas, stock y catálogo con consultas agregadas de solo lectura; días en hora de Lima, rango máximo de 366 días, ventas anuladas excluidas y CSV con `;`, UTF-8 con BOM y fórmulas neutralizadas, solo para `ADMIN` (#137).
- **Productos en tarjetas, lista o tabla:** botones para cambiar de vista (se recuerda la elegida) y, en cada producto, botones Ver, Editar, Ajustar stock y Eliminar. «Ver» abre la ficha con los datos, el estado y el acceso al historial.
- **Imagen de producto:** `PUT`, `GET` y `DELETE /api/v1/products/{id}/image` (PNG, JPG o WebP de hasta 2 MB, tipo comprobado por contenido; tabla `product_image`, migración V13). Se sube desde la ficha y se ve en las tres vistas.
- **Eliminar un producto:** `DELETE /api/v1/products/{id}` solo si nunca tuvo movimientos, compras ni ventas; si tiene historial responde 409 (RN-03) y el panel ofrece inhabilitarlo.
- **Historial general del inventario:** `GET /api/v1/stock-movements` con filtros de fecha y tipo; Inventario lo muestra con producto, tipo, cantidad (+/−), saldo y usuario, y un botón para abrir el kárdex del producto.
- **La sesión sobrevive a una recarga (ADR 0011):** el token se guarda en `sessionStorage`; F5 o Ctrl+R ya no piden ingresar de nuevo, y cerrar la pestaña sí cierra la sesión.

### Cambiado

- **Agregar stock más claro:** al crear un producto se puede indicar el stock inicial (entrada por conteo físico); cada producto tiene en la lista el enlace «Ajustar», que abre Inventario con ese producto y el ajuste listo; Inventario muestra el stock actual, explica cómo entra y sale el stock y el botón «Registrar ajuste» ya tiene texto (#123).
- **Pantalla de categorías más informativa:** `GET /api/v1/categories` incluye `productCount`; el panel muestra un resumen (categorías, activas y productos), cuántos productos tiene cada categoría y un estado vacío con botón para crear la primera (#125).
- **Código de producto automático:** al crear un producto el código es opcional; vacío, la API asigna el siguiente libre de la secuencia `P-0001` y se salta los que se escribieron a mano (#122).
- **Pantalla de ventas compacta:** los productos de la venta van en una tabla corta (nombre, cantidad, subtotal) y el total con el botón «Registrar venta» quedan en un resumen al costado; los resultados de búsqueda se muestran como lista con precio y stock (#124).
- **Comprobante con forma de ticket:** logo, número, datos alineados, tabla Descripción / Cant. / Importe, total destacado y pie de documento interno; al imprimir sale en 80 mm de ancho (#124).

## [v0.6-fase6] - 2026-11-12

### Agregado

- **ADR 0010 · Ventas entre módulos:** `sales` coordina la transacción con `inventory`, copia el precio vigente del catálogo (RN-06) y numera los comprobantes sin huecos con una tabla contador (RN-07).
- **HU-14 · Registrar una venta (API):** `POST /api/v1/sales` descuenta el stock con una salida `SALE` por producto, copia código, nombre y precio vigente del catálogo (RN-06) y asigna el número `V-000001` sin huecos (RN-07), todo en una transacción con reintentos (RNF-04, RNF-05). `GET /api/v1/sales/{id}` devuelve el comprobante; al `EMPLEADO` solo las suyas. Tablas `sale`, `sale_item` y `sale_number_counter` (migración V11).
- **HU-16 · Historial de ventas (API):** `GET /api/v1/sales` pagina de la más reciente a la más antigua y filtra por fechas en hora de Lima y por vendedor. El `EMPLEADO` solo recibe sus ventas aunque pida otro `sellerId` (RF-21, RNF-03).
- **HU-15 · Anular una venta (API):** `POST /api/v1/sales/{id}/void` con motivo obligatorio devuelve el stock con una entrada `SALE_VOID` por producto y deja la venta `VOIDED` sin perder su número (RN-03, RN-07). Una venta ya anulada responde 422 y un `UPDATE` condicional evita la doble anulación. Solo `ADMIN` (RN-05). Migración V12.
- **Panel · Ventas:** pantalla `/ventas` para registrar una venta en pocos pasos: buscador con precio y stock, cantidades editables, subtotales y total en vivo, aviso si una cantidad supera el stock, y el número del comprobante al confirmar. Muestra qué producto no alcanza (RN-01) y el conflicto de concurrencia. Para `ADMIN` y `EMPLEADO`.
- **Panel · Comprobante de venta:** comprobante interno con número, fecha y hora de Lima, vendedor, ítems con código y precio copiado, subtotales, total y estado (anulada con su motivo). El botón «Imprimir» saca solo el comprobante, sin menú ni botones (RF-19). No es un comprobante electrónico de SUNAT.
- **Panel · Historial de ventas:** en `/ventas`, lista paginada del mes actual con número, fecha, vendedor, total y estado (con el motivo si está anulada). Filtro por fechas y, solo para el `ADMIN`, por vendedor; el `EMPLEADO` ve únicamente sus ventas. Desde la lista se abre el comprobante (HU-16).
- **Panel · Anular una venta:** el `ADMIN` anula una venta vigente desde su comprobante con un motivo obligatorio; queda «Anulada» con su motivo en el comprobante y en el historial, sin perder su número, y la acción desaparece. El `EMPLEADO` no ve la acción (HU-15).
- **Datos de ejemplo:** `scripts/datos-demo.py` carga un catálogo `DEMO-*`, un proveedor, una empleada de prueba y una compra, registra ventas como `ADMIN` y `EMPLEADO`, anula una y comprueba RN-01, RN-05, RN-06, RN-07 y RF-21 contra la API local.

## [v0.5-fase5] - 2026-11-11

### Agregado

- **ADR 0009 · Compras entre módulos:** `purchasing` coordina una sola transacción con `inventory` y `catalog`, con reintentos ante conflictos de stock.
- **HU-11 · Gestionar proveedores (API):** `GET /api/v1/suppliers?q=`, `POST` y `PUT /api/v1/suppliers/{id}`. RUC único de 11 dígitos (RN-10); crear y editar son solo de `ADMIN`. Tabla `supplier` (migración V8).
- **HU-12 · Registrar una compra (API):** `POST /api/v1/purchases` suma el stock con una entrada `PURCHASE` por producto y fija el costo referencial con el último costo comprado (RN-08), todo en una transacción con reintentos (RNF-04, ADR 0009). `GET /api/v1/purchases` lista por fecha de compra y pagina. Solo `ADMIN`. Tablas `purchase` y `purchase_item` (migración V9).
- **Panel · Proveedores:** `/proveedores` para buscar por RUC o razón social, registrar, editar, activar y desactivar (HU-11). El RUC inválido o repetido se marca en su campo; el empleado solo consulta.
- **HU-13 · Anular una compra (API):** `POST /api/v1/purchases/{id}/void` con motivo: una salida `PURCHASE_VOID` por producto y la compra queda `VOIDED`, sin tocar los movimientos originales (RN-03). Si dejaría stock negativo responde 422 (RN-01); una compra ya anulada, también. El costo referencial no se revierte. Columnas de anulación en `purchase` (migración V10).
- **Panel · Compras:** `/compras` (solo `ADMIN`) lista las compras del mes con filtro de fechas y paginación, muestra el detalle con sus productos y registra una compra de varios productos en un panel lateral, con subtotales y total en vivo (HU-12).
- **Panel · Anular una compra:** desde el detalle de una compra registrada, con motivo obligatorio (HU-13). La lista y el detalle muestran el estado «Anulada» y el motivo; si anular dejaría stock negativo (RN-01), se ve el mensaje de la API y nada cambia.

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
