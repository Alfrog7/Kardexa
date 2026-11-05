# Changelog

Todos los cambios relevantes de Kardexa se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones siguen el plan por fases (`vX.Y-faseN`).

## [Sin publicar]

### Agregado

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

### Cambiado

- El stack pasa de Spring Boot 3 a **Spring Boot 4.1**, porque la rama 3.x ya no tiene soporte gratuito (ADR 0001).
- Las rutas de la API pasan a inglés (`/api/v1/products`) según el ADR 0003.

### Corregido

- La plantilla de PR usa `Closes #N` para que GitHub cierre el issue al hacer merge.
