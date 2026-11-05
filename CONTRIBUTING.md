# Guía de contribución

Kardexa es un proyecto personal, pero se trabaja con el mismo flujo que un equipo: todo cambio entra por issue → rama → PR.

## Flujo de trabajo

1. **Issue primero.** Cada historia de usuario o tarea tiene su issue, con etiquetas y milestone de su fase.
2. **Rama desde `main`.** Nunca se hace commit directo a `main`.
3. **PR que cierra el issue.** Usa la plantilla, incluye `Closes #N` y copia las etiquetas y el milestone del issue.
   > GitHub solo reconoce palabras clave en inglés (`Closes`, `Fixes`, `Resolves`); con `Cierra #N` el issue no se cierra solo. Es la única excepción a la regla de idioma.
4. **Merge con squash.** El título del PR queda como mensaje del commit en `main`.
5. **Al cerrar una fase:** tag `vX.Y-faseN` y entrada en el [CHANGELOG](CHANGELOG.md).

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
| Tipo | `feature`, `bug`, `docs`, `chore`, `ci`, `refactor`, `test`, `branding` |
| Área | `backend`, `frontend`, `landing`, `infra` |
| Fase | `fase-0` … `fase-9` |
| Prioridad | `prioridad: alta`, `prioridad: media`, `prioridad: baja` |

Todo issue lleva al menos una etiqueta de **tipo** y una de **fase**. El PR lleva las mismas que el issue que cierra.

## Idioma

Documentación, issues, PRs y commits van en español. El criterio de nombres en el código se fija en un ADR durante la fase 1.
