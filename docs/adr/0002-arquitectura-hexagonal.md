# 0002 · Arquitectura hexagonal modular en el backend

- **Estado:** Aceptado
- **Fecha:** 2026-11-05
- **Requisitos relacionados:** RNF-07, RNF-08

## Contexto

Las reglas de negocio (RN-01 a RN-12) son el corazón del sistema: stock nunca negativo, kárdex inmutable, correlativos únicos. Deben poder probarse sin base de datos ni servidor web, y no deben quedar repartidas entre controladores y repositorios.

Además, el sistema tiene módulos claros (catálogo, inventario, compras, ventas…) que conviene mantener separados para que crezcan sin enredarse.

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| Capas clásicas (`controller` / `service` / `repository`) | Simple, la conoce todo el mundo | El dominio termina dependiendo de JPA; todos los módulos mezclados en cada carpeta |
| **Hexagonal (puertos y adaptadores) por módulo** | Dominio puro y testeable; módulos independientes | Más clases e interfaces; curva de aprendizaje |
| Microservicios | Independencia total de despliegue | Excesivo para un solo desarrollador y un solo negocio |

## Decisión

**Monolito modular con arquitectura hexagonal dentro de cada módulo.**

```text
com.kardexa
├── shared/                   # utilidades transversales (valores, errores)
└── <módulo>/                 # identity, catalog, inventory, purchasing, sales, reporting
    ├── domain/               # modelo y reglas de negocio — Java puro
    │   ├── model/            # entidades, agregados, objetos de valor
    │   └── exception/        # errores de dominio
    ├── application/          # casos de uso — Java puro
    │   ├── port/in/          # interfaces de casos de uso (lo que el módulo ofrece)
    │   ├── port/out/         # interfaces que el módulo necesita (persistencia, reloj…)
    │   └── service/          # implementación de los casos de uso
    └── infrastructure/       # detalles técnicos — Spring, JPA, HTTP
        ├── adapter/in/web/   # controladores REST, DTOs
        ├── adapter/out/persistence/  # entidades JPA, repositorios Spring Data
        └── config/           # beans del módulo
```

### Reglas de dependencia

1. `domain` no depende de nada fuera de `domain` y `shared.domain`.
2. `application` depende solo de `domain`.
3. `infrastructure` puede depender de todo lo anterior y de Spring.
4. Un módulo solo usa otro a través de sus **puertos de entrada** (`application.port.in`), nunca de su `infrastructure` ni de su `domain` interno.
5. Las clases JPA (`@Entity`) viven en `infrastructure` y se mapean al modelo de dominio.

Estas reglas se **verifican con tests de ArchUnit** en cada build: si alguien las rompe, la CI falla.

## Consecuencias

- ✅ Los casos de uso se prueban con tests unitarios rápidos, sin Spring.
- ✅ Cambiar la base de datos o agregar una interfaz (por ejemplo, una CLI) no toca el dominio.
- ✅ La arquitectura es visible en las carpetas y en los diagramas, que es parte del objetivo del portafolio.
- ⚠️ Hay más código de mapeo (dominio ↔ JPA ↔ DTO). Se acepta a cambio del aislamiento.
- ⚠️ Riesgo de sobreingeniería en módulos simples (categorías). Se permite un caso de uso por operación sin capas extra.
