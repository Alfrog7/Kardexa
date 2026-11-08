# Kardexa · API

API REST de Kardexa: Java 21 · Spring Boot 4.1 · PostgreSQL · arquitectura hexagonal modular ([ADR 0002](../docs/adr/0002-arquitectura-hexagonal.md)).

## Requisitos

- JDK 21 (no hace falta instalar Maven: se usa el wrapper `mvnw`)
- Docker en marcha: los tests de integración levantan PostgreSQL 18 con Testcontainers, y `docker-compose.yml` lo levanta para desarrollo

## Comandos

```bash
./mvnw verify             # compila, ejecuta los tests y genera el reporte de cobertura
./mvnw spring-boot:run    # levanta la API en http://localhost:8080 (requiere PostgreSQL)
```

En Windows (PowerShell o CMD) se usa `mvnw.cmd` en lugar de `./mvnw`.

| URL | Contenido |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Documentación interactiva |
| `http://localhost:8080/actuator/health` | Estado de la aplicación |
| `target/site/jacoco/index.html` | Reporte de cobertura tras `verify` |

## Configuración

En local, la API lee el archivo `.env` de la raíz del repo (el mismo que usa `docker-compose.yml`) si existe. Las variables de entorno del sistema tienen prioridad sobre el archivo. Fuera de local no hay `.env`: todo se define con variables de entorno.

| Variable | Por defecto (solo local) | Descripción |
|---|---|---|
| `DB_PORT` | `5432` | Puerto de PostgreSQL en el equipo (cámbialo si ya tienes otro PostgreSQL instalado) |
| `DB_NAME` | `kardexa` | Nombre de la base |
| `DB_URL` | `jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME}` | URL JDBC completa; si se define, reemplaza a `DB_PORT` y `DB_NAME` |
| `DB_USER` | `kardexa` | Usuario de la base |
| `DB_PASSWORD` | `kardexa` | Contraseña de la base |
| `PORT` | `8080` | Puerto HTTP |

Los tests no usan esta configuración: Testcontainers levanta un PostgreSQL desechable y Spring se conecta a él con `@ServiceConnection`.

## Estructura

```text
src/main/java/com/kardexa/
├── KardexaApplication.java
├── shared/                         # transversal
│   ├── domain/                     # Money, Quantity, DomainException (Java puro)
│   └── infrastructure/             # prefijo /api/v1, OpenAPI, errores Problem Details
└── <módulo>/                       # identity · catalog · inventory · purchasing · sales · reporting
    ├── domain/
    │   ├── model/                  # entidades y objetos de valor
    │   └── exception/              # una excepción por regla de negocio
    ├── application/
    │   ├── port/in/                # casos de uso que el módulo ofrece
    │   ├── port/out/               # lo que el módulo necesita (persistencia…)
    │   └── service/                # implementación de los casos de uso
    └── infrastructure/
        ├── adapter/in/web/         # controladores REST y DTOs
        ├── adapter/out/persistence/# entidades JPA y repositorios
        └── config/                 # beans y transacciones del módulo
```

Cada paquete tiene un `package-info.java` que explica qué va en él.

## Reglas de arquitectura

`HexagonalArchitectureTest` (ArchUnit) hace fallar el build si:

1. El dominio o la aplicación dependen de Spring, JPA, Hibernate o Jackson.
2. El dominio conoce la aplicación o la infraestructura.
3. La aplicación conoce la infraestructura.
4. Una entidad JPA está fuera de `infrastructure.adapter.out.persistence`.
5. Un controlador está fuera de `infrastructure.adapter.in.web`.
6. Un módulo usa otro sin pasar por su `application.port.in`.

## Errores

Todas las respuestas de error usan `application/problem+json` (RFC 9457):

```json
{
  "type": "https://kardexa.dev/problemas/regla-de-negocio",
  "title": "Stock insuficiente",
  "status": 422,
  "detail": "Stock insuficiente para \"Arroz 5 kg\": disponible 2, solicitado 3.",
  "rule": "RN-01"
}
```

Para una regla nueva basta con crear una subclase de `DomainException` en el `domain.exception` del módulo.
