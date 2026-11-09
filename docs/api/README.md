# Contrato de la API

[`openapi.yaml`](openapi.yaml) es el contrato **de diseño** de la versión 1 (OpenAPI 3.1). Se escribe antes que el código (*contract-first*): el backend lo implementa y el frontend lo consume.

## Convenciones

| Tema | Convención |
|---|---|
| Base | `/api/v1` |
| Rutas | Inglés, plural, `kebab-case` ([ADR 0003](../adr/0003-criterio-de-nombres.md)) |
| Autenticación | `Authorization: Bearer <JWT>` ([ADR 0008](../adr/0008-autenticacion-con-jwt.md)); públicas solo `/auth/setup`, `/auth/login`, Swagger y *health* |
| Errores | `application/problem+json` (RFC 9457) con `title` y `detail` en español y `rule` con la regla violada |
| Montos y cantidades | Strings decimales (`"12.50"`, `"1.250"`) para no perder precisión en JavaScript |
| Fechas | ISO 8601; los filtros por día usan la zona `America/Lima` |
| Paginación | `?page=0&size=20`; respuesta con `content`, `page`, `size`, `totalElements`, `totalPages` |
| Acciones que no son CRUD | Sub-recurso con verbo: `POST /sales/{id}/void` |

## Códigos de estado

| Código | Cuándo |
|---|---|
| 200 / 201 / 204 | Éxito |
| 400 | Validación de formato (campo faltante, patrón inválido) |
| 401 | Sin token o token vencido |
| 403 | El rol no permite la operación |
| 404 | El recurso no existe |
| 409 | Valor único duplicado, o el stock cambió durante la operación ([ADR 0006](../adr/0006-concurrencia-del-stock.md)) |
| 422 | Regla de negocio violada (por ejemplo, stock insuficiente) |

## Cobertura

| Módulo | Endpoints | Requisitos |
|---|---|---|
| auth | 4 | RF-01, RF-02, RF-05 |
| users | 3 | RF-03 |
| catalog | 7 | RF-06 a RF-09, RF-14 |
| inventory | 2 | RF-11, RF-12 |
| purchasing | 6 | RF-15 a RF-17 |
| sales | 4 | RF-18 a RF-21 |
| dashboard | 1 | RF-22 a RF-24 |

RF-10 (movimientos automáticos) y RF-13 (stock actual) no tienen endpoint propio: son efectos de las compras, ventas y ajustes, y el stock viaja en cada `Product`. RF-25 (exportar CSV, prioridad *Could*) se agregará cuando se implemente.

## Cómo verlo

- En GitHub: abrir `openapi.yaml`.
- En local, desde la fase 2: Swagger UI en `http://localhost:8080/swagger-ui.html`, generado a partir del código.
