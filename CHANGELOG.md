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

- Esqueleto del backend: Spring Boot 4.1.2 con Maven Wrapper, seis módulos hexagonales, objetos de valor `Money` y `Quantity`, errores Problem Details, prefijo `/api/v1`, Swagger UI, Actuator, Flyway y JaCoCo.
- Siete reglas de arquitectura con ArchUnit que hacen fallar el build si se rompe el ADR 0002.
- Esqueleto del frontend: React 19, TypeScript 7 en modo estricto, Vite 8 y React Router 8, organizado por funcionalidades, con layout del panel adaptable a móvil, pantalla de inicio de sesión, tokens de marca (claro/oscuro), cliente HTTP con Problem Details y 18 tests con Vitest.

### Cambiado

- El stack pasa de Spring Boot 3 a **Spring Boot 4.1**, porque la rama 3.x ya no tiene soporte gratuito (ADR 0001).
- Las rutas de la API pasan a inglés (`/api/v1/products`) según el ADR 0003.

### Corregido

- La plantilla de PR usa `Closes #N` para que GitHub cierre el issue al hacer merge.
- El `.gitignore` ya no ignora los paquetes `port/out` y `adapter/out` (la regla `out/` de IntelliJ se limita a la raíz de cada proyecto).
