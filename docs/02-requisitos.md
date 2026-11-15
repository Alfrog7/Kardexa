# 02 · Requisitos

> Fase 0 · Prioridad según **MoSCoW**: **M** (Must, imprescindible para el MVP), **S** (Should, importante), **C** (Could, deseable), **W** (Won't, no en el MVP).

## 1. Requisitos funcionales

### Autenticación y usuarios (AUT)

| ID | Requisito | Prioridad |
|---|---|---|
| RF-01 | El sistema permite registrar al **primer administrador** cuando aún no existe ningún usuario. Después, el registro público queda deshabilitado. | M |
| RF-02 | Un usuario inicia sesión con correo y contraseña y recibe un token JWT emitido por la propia API. | M |
| RF-03 | El administrador crea, edita, activa y desactiva usuarios, y les asigna el rol `ADMIN` o `EMPLEADO`. | M |
| RF-04 | La sesión expira tras un tiempo configurable; el usuario puede cerrar sesión manualmente. | M |
| RF-05 | Un usuario puede cambiar su propia contraseña indicando la actual. | S |

### Catálogo (CAT)

| ID | Requisito | Prioridad |
|---|---|---|
| RF-06 | El administrador gestiona **categorías** (crear, editar, desactivar). | M |
| RF-07 | El administrador gestiona **productos**: código, nombre, categoría, unidad de medida, precio de venta, costo referencial y stock mínimo. | M |
| RF-08 | Cualquier usuario busca productos por nombre o código y los filtra por categoría. | M |
| RF-09 | Un producto con movimientos no se elimina: se desactiva y deja de aparecer en las ventas. | M |

### Inventario (INV)

| ID | Requisito | Prioridad |
|---|---|---|
| RF-10 | Cada compra, venta, anulación o ajuste genera automáticamente un **movimiento de kárdex** (entrada o salida). | M |
| RF-11 | El administrador registra **ajustes de stock** (merma, conteo físico, corrección) indicando motivo. | M |
| RF-12 | Cualquier usuario consulta el **kárdex de un producto**: fecha, tipo, documento de origen, entrada, salida, saldo y usuario, filtrable por rango de fechas. | M |
| RF-13 | El sistema muestra el **stock actual** de cada producto. | M |
| RF-14 | El sistema marca los productos cuyo stock es igual o menor que su **stock mínimo**. | M |

### Proveedores y compras (COM)

| ID | Requisito | Prioridad |
|---|---|---|
| RF-15 | El administrador gestiona **proveedores**: razón social, RUC, teléfono y correo. | M |
| RF-16 | El administrador registra una **compra** con proveedor, fecha, número de documento del proveedor y detalle (producto, cantidad, costo unitario). La compra suma stock. | M |
| RF-17 | El administrador **anula** una compra; el sistema genera los movimientos inversos. | S |

### Ventas (VEN)

| ID | Requisito | Prioridad |
|---|---|---|
| RF-18 | Un usuario registra una **venta** con detalle (producto, cantidad, precio). La venta descuenta stock. | M |
| RF-19 | Cada venta genera un **comprobante interno** con número correlativo, imprimible. | M |
| RF-20 | El administrador **anula** una venta indicando motivo; el sistema devuelve el stock. | S |
| RF-21 | Los usuarios consultan el **historial de ventas** filtrado por fecha y vendedor. Un empleado solo ve las suyas. | S |

### Panel y reportes (PAN)

| ID | Requisito | Prioridad |
|---|---|---|
| RF-22 | El panel muestra el **total vendido por día** en un rango de fechas. | M |
| RF-23 | El panel muestra los **productos más vendidos** en un rango de fechas. | S |
| RF-24 | El panel muestra la lista de productos con **stock crítico**. | M |
| RF-25 | El administrador exporta el kárdex o el historial de ventas a CSV. | C |

### Sedes y turnos (SED)

> Fase 8 · Extensión posterior al MVP, decidida en el [ADR 0013](adr/0013-sedes-turnos-y-cierre-por-conteo.md).

| ID | Requisito | Prioridad |
|---|---|---|
| RF-26 | El administrador crea, edita e inhabilita **sedes** con nombre y dirección, sin límite de cantidad. | M |
| RF-27 | Cada sede tiene su **propio stock y kárdex**; las ventas, compras, ajustes y entradas ocurren en una sede. El catálogo y los precios son comunes. | M |
| RF-28 | Cada empleado pertenece a una sede y solo ve y opera en ella. El administrador consulta una sede o todas y elige la sede en la que registra. | M |
| RF-29 | El empleado registra las **entradas de mercadería** de su sede, sin costo ni proveedor. | M |
| RF-30 | Quien lleva la caja **abre un turno** en su sede; solo puede haber un turno abierto por sede. | M |
| RF-31 | Al **cerrar el turno** se cuenta lo que sobró de cada producto; el sistema calcula lo vendido y el monto esperado. | M |
| RF-32 | Al cerrar se anota lo recibido en **efectivo, Yape, Plin y transferencia**; el sistema calcula la diferencia contra lo esperado. | M |
| RF-33 | El administrador ve por **sede, turno y responsable** lo vendido, lo recibido por medio de pago y las diferencias. | S |

## 2. Reglas de negocio

| ID | Regla |
|---|---|
| RN-01 | El stock de un producto **nunca puede ser negativo**. Una venta o un ajuste que lo deje en negativo se rechaza. |
| RN-02 | Saldo del movimiento = saldo anterior + entrada − salida. El stock actual es el saldo del último movimiento. |
| RN-03 | Los movimientos de kárdex son **inmutables**: no se editan ni se borran. Un error se corrige con un movimiento inverso. |
| RN-04 | Todo movimiento registra fecha y hora, usuario y documento de origen (compra, venta, anulación o ajuste). |
| RN-05 | Solo el rol `ADMIN` registra compras y anulaciones; los ajustes de stock los registran `ADMIN` y `EMPLOYEE` (ADR 0013). Ajustes y anulaciones exigen un motivo. |
| RN-06 | El precio de venta y el costo se **copian** en el detalle del documento; cambiar el precio del producto no altera documentos pasados. |
| RN-07 | El número de comprobante es **correlativo y único** (`V-000001`, `V-000002`…) y no se reutiliza aunque la venta se anule. |
| RN-08 | Una compra actualiza el **costo referencial** del producto con el último costo unitario comprado. |
| RN-09 | Un producto está en **stock crítico** cuando su stock actual es menor o igual que su stock mínimo. |
| RN-10 | El código de producto es **único**. El RUC del proveedor es único y tiene 11 dígitos. |
| RN-11 | Siempre debe existir **al menos un administrador activo**; el último no puede desactivarse ni cambiar de rol. |
| RN-12 | Montos en soles (PEN) con 2 decimales y redondeo *half-up*; cantidades con hasta 3 decimales para productos a granel. |
| RN-13 | El stock y el saldo del kárdex (RN-02) se llevan **por producto y sede**; un movimiento de una sede nunca cambia el saldo de otra. El comprobante `V-` sigue siendo único para todo el negocio (RN-07). |
| RN-14 | Hay **un solo turno abierto por sede**. Un turno cerrado no se edita ni se reabre; lo que sobra queda como stock inicial del siguiente. |
| RN-15 | Al cerrar, vendido por conteo = stock registrado − sobrante contado. El sobrante no puede superar el stock registrado (lo que llegó se anota antes como entrada). |
| RN-16 | Diferencia del turno = recibido − esperado, donde esperado = ventas registradas del turno + vendido por conteo × precio vigente. Se calcula en el servidor y negativo es faltante. |

## 3. Requisitos no funcionales

| ID | Categoría | Requisito | Criterio de verificación |
|---|---|---|---|
| RNF-01 | Seguridad | Contraseñas almacenadas con **BCrypt**; nunca en texto plano ni en logs. | Revisión de código y test de persistencia. |
| RNF-02 | Seguridad | Autenticación con **JWT propio** firmado; expiración configurable (60 min por defecto). | Test: un token vencido devuelve 401. |
| RNF-03 | Seguridad | Autorización por rol en **cada endpoint**. | Test: un `EMPLEADO` recibe 403 en endpoints de administración. |
| RNF-04 | Consistencia | Registrar una venta y descontar el stock ocurre en **una sola transacción**. | Test: si falla un ítem, no se guarda nada. |
| RNF-05 | Concurrencia | Dos ventas simultáneas del mismo producto no pueden dejar stock negativo (control de concurrencia en el stock). | Test de integración con hilos concurrentes. |
| RNF-06 | Rendimiento | p95 < 300 ms en búsqueda de productos y registro de venta con 10 000 productos y 100 000 movimientos (entorno local). | Prueba de carga básica. |
| RNF-07 | Mantenibilidad | **Arquitectura hexagonal**: el dominio no depende de Spring ni de JPA. | Test de arquitectura (p. ej., ArchUnit). |
| RNF-08 | Calidad | Cobertura ≥ 80 % en dominio y casos de uso. | Reporte de cobertura en CI. |
| RNF-09 | Datos | Esquema versionado con **migraciones Flyway**; ningún cambio manual a la base. | Las migraciones se aplican desde cero sin errores. |
| RNF-10 | Interoperabilidad | API REST documentada con **OpenAPI**; errores en formato uniforme *Problem Details* (RFC 9457). | Swagger UI disponible; tests de contrato de errores. |
| RNF-11 | Usabilidad | Panel adaptable desde 360 px de ancho; la venta se puede registrar solo con teclado. | Revisión manual en móvil y escritorio. |
| RNF-12 | Accesibilidad | Contraste de color WCAG 2.1 AA y formularios con etiquetas. | Auditoría con Lighthouse ≥ 90 en accesibilidad. |
| RNF-13 | Portabilidad | El entorno local se levanta con un solo comando documentado. | Seguir el README en una máquina limpia. |
| RNF-14 | Auditoría | Las entidades registran fecha de creación, de modificación y usuario. | Revisión del modelo. |
| RNF-15 | Localización | Interfaz en español, moneda PEN, zona horaria `America/Lima`. | Revisión manual. |
| RNF-16 | Integración continua | Cada PR ejecuta build y tests en GitHub Actions. | Check verde obligatorio antes del merge. |

## 4. Matriz de trazabilidad

| Requisito | Historias de usuario |
|---|---|
| RF-01 | HU-01 |
| RF-02, RF-04 | HU-02 |
| RF-03 | HU-03 |
| RF-05 | HU-04 |
| RF-06 | HU-05 |
| RF-07, RF-09 | HU-06 |
| RF-08 | HU-07 |
| RF-12, RF-13 | HU-08 |
| RF-11 | HU-09 |
| RF-14, RF-24 | HU-10 |
| RF-15 | HU-11 |
| RF-16, RF-10 | HU-12 |
| RF-17 | HU-13 |
| RF-18, RF-19, RF-10 | HU-14 |
| RF-20 | HU-15 |
| RF-21 | HU-16 |
| RF-22, RF-23 | HU-17 |
| RF-25 | HU-18 |
| RF-26, RF-27 | HU-19 |
| RF-28 | HU-20 |
| RF-29 | HU-21 |
| RF-30, RF-31 | HU-22 |
| RF-32 | HU-23 |
| RF-33 | HU-24 |

Las historias están en [`03-historias-de-usuario.md`](03-historias-de-usuario.md).
