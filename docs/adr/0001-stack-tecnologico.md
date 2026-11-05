# 0001 · Stack tecnológico

- **Estado:** Aceptado
- **Fecha:** 2026-11-05
- **Requisitos relacionados:** RNF-01, RNF-02, RNF-07, RNF-09, RNF-10, RNF-13, RNF-16

## Contexto

El brief propone Java + Spring Boot 3, PostgreSQL, React + TypeScript y Astro. Al confirmar versiones en noviembre de 2026:

- **Spring Boot 3.5**, la última de la rama 3.x, terminó su soporte gratuito (OSS) a mediados de 2026. Empezar un proyecto nuevo sobre una versión sin parches de seguridad no tiene sentido.
- **Spring Boot 4.1** es la versión estable vigente, sobre Spring Framework 7, Jakarta EE 11 y Jackson 3.
- El proyecto no puede usar servicios externos de autenticación ni de backend (restricción del brief).

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| **Spring Boot 4.1 + Java 21** | Versión con soporte, mismo ecosistema que se usa en el trabajo, Java 21 es LTS | Algunas librerías aún se adaptan a Jackson 3 |
| Spring Boot 3.5 + Java 21 | Más ejemplos y tutoriales | Sin parches gratuitos; migración obligatoria a corto plazo |
| Node (NestJS) en el backend | Un solo lenguaje en todo el repo | No refuerza el perfil Java/Spring, que es el objetivo |
| MySQL en lugar de PostgreSQL | Popular en hosting barato | Peor soporte de tipos (`numeric`, `uuid`) y de restricciones `CHECK` |

## Decisión

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje backend | Java (Temurin) | 21 LTS |
| Framework backend | Spring Boot | 4.1.x |
| Build backend | Maven (vía Maven Wrapper) | 3.9.x |
| Seguridad | Spring Security + JWT propio | fase 3 |
| Persistencia | Spring Data JPA + Hibernate | la de Spring Boot |
| Base de datos | PostgreSQL | 18 |
| Migraciones | Flyway | la de Spring Boot |
| Documentación de API | springdoc-openapi (Swagger UI) | 3.x |
| Tests backend | JUnit 5, AssertJ, ArchUnit, Testcontainers (fase 3) | — |
| Frontend | React + TypeScript + Vite | las estables vigentes |
| Tests frontend | Vitest + Testing Library | — |
| Landing | Astro | fase 8 |
| Entorno local | Docker Compose (solo PostgreSQL) | — |
| CI | GitHub Actions | — |

Las versiones exactas quedan fijadas en `pom.xml` y `package.json`. Las actualizaciones menores se hacen por PR con su entrada en el CHANGELOG.

## Consecuencias

- ✅ Se parte de versiones con soporte y no habrá que migrar de Spring Boot 3 a 4 a mitad del proyecto.
- ✅ Todo el stack es gratuito y puede desplegarse en planes gratuitos (fase 9).
- ⚠️ Spring Boot 4 usa **Jackson 3** (paquete `tools.jackson`): algunos ejemplos en internet todavía usan `com.fasterxml.jackson`.
- ⚠️ El README del brief menciona "Spring Boot 3"; se actualiza a 4.
