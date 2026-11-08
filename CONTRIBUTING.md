# Guía de contribución

Kardexa es un proyecto personal, pero se trabaja con el mismo flujo que un equipo: todo cambio entra por issue → rama → PR.

## Flujo de trabajo

1. **Issue primero.** Cada historia de usuario o tarea tiene su issue, con etiquetas y milestone de su fase.
2. **Rama desde `develop`.** Nunca se hace commit directo a `develop` ni a `main`.
3. **PR hacia `develop` que cierra el issue.** Usa la plantilla, incluye `Closes #N` y copia las etiquetas y el milestone del issue.
   > GitHub solo reconoce palabras clave en inglés (`Closes`, `Fixes`, `Resolves`); con `Cierra #N` el issue no se cierra solo. Es la única excepción a la regla de idioma.
4. **Merge con squash.** El título del PR queda como mensaje del commit en `develop`.
5. **Al cerrar una fase:** PR `develop → main` con **merge commit** (no squash, para que las dos ramas no se separen), tag `vX.Y-faseN` y entrada en el [CHANGELOG](CHANGELOG.md).

## Ramas permanentes

| Rama | Contenido |
|---|---|
| `main` | Rama por defecto del repositorio. Solo versiones cerradas: cada commit corresponde a una fase terminada y etiquetada. |
| `develop` | Rama de integración. Siempre compila y pasa la CI. |

Las ramas de trabajo se borran solas al fusionar el PR.

GitHub solo aplica `Closes #N` al fusionar en la rama por defecto: tras fusionar en `develop`, el issue se cierra a mano.

## Qué hace aceptable a un PR

- **Una historia de usuario o una tarea por PR.** Si el cambio no cabe en una frase, hay que dividirlo.
- **Comportamiento que se pueda probar.** Una funcionalidad entra de punta a punta: migración, dominio, caso de uso, adaptadores, endpoint y tests. Las carpetas o clases vacías "para después" no cuentan como avance.
- **Tests que demuestren los criterios de aceptación** de la historia de usuario, no solo que el código compila.
- **Descripción corta y verificable:** qué hace ahora el sistema, cómo probarlo y la evidencia. Sin secciones de relleno.

## Ramas

| Prefijo | Uso | Ejemplo |
|---|---|---|
| `feat/` | Nueva funcionalidad | `feat/registro-de-ventas` |
| `fix/` | Corrección de errores | `fix/stock-negativo` |
| `docs/` | Documentación | `docs/fase-0-requisitos` |
| `chore/` | Configuración y mantenimiento | `chore/configuracion-repositorio` |
| `refactor/` | Cambio interno sin alterar el comportamiento | `refactor/puerto-inventario` |
| `test/` | Pruebas | `test/casos-de-uso-ventas` |
| `ci/` | Integración continua | `ci/build-backend` |

## Commits

Se usan [Commits Convencionales](https://www.conventionalcommits.org/es/v1.0.0/), en español y en modo imperativo:

```
tipo(alcance opcional): descripción breve en minúsculas

Cuerpo opcional: el porqué del cambio.
```

Tipos: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`, `style`, `perf`.

## Etiquetas

| Grupo | Etiquetas |
|---|---|
| Tipo | `epic`, `enhancement`, `bug`, `documentation`, `chore`, `ci`, `refactor`, `test`, `branding` |
| Área | `backend`, `frontend`, `landing`, `infra` |
| Fase | `phase-0` … `phase-9` |
| Prioridad | `priority: high`, `priority: medium`, `priority: low` |

Las etiquetas van en inglés, como es habitual en GitHub. Todo issue lleva al menos una etiqueta de **tipo** y una de **fase**. El PR lleva las mismas que el issue que cierra.

## Issues

Cada fase tiene una **épica** (plantilla "Épica") que agrupa todo su trabajo como **sub-issues**: backend, frontend, landing, documentación y el cierre de la fase. Así se ve en un solo lugar qué falta y en qué orden.

Cada sub-issue sigue el mismo formato:

- **Parte de la épica #N**
- **Alcance**: qué podrá hacer el usuario y qué HU, RF o RN cubre.
- **Qué hacer**: pasos técnicos concretos.
- **Notas**: dependencias, decisiones abiertas y lo que queda fuera.
- **Criterios de aceptación**: comportamiento verificable, uno por casilla `- [ ]`, cada uno con su test.

Ejemplo: la épica de la Fase 4 (#27).

## Idioma

Documentación, issues, PRs y commits van en español. El criterio de nombres en el código se fija en un ADR durante la fase 1.
