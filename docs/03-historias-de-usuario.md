# 03 · Historias de usuario

> Fase 0 · Formato: *Como… quiero… para…* con criterios **Dado / Cuando / Entonces**.
> **Prioridad:** MoSCoW. **Estimación:** puntos de historia (Fibonacci), relativa entre historias.

## Resumen

| ID | Historia | Módulo | Prioridad | Puntos |
|---|---|---|---|---|
| HU-01 | Registrar al primer administrador | Autenticación | M | 3 |
| HU-02 | Iniciar y cerrar sesión | Autenticación | M | 5 |
| HU-03 | Gestionar usuarios y roles | Autenticación | M | 5 |
| HU-04 | Cambiar mi contraseña | Autenticación | S | 2 |
| HU-05 | Gestionar categorías | Catálogo | M | 2 |
| HU-06 | Gestionar productos | Catálogo | M | 5 |
| HU-07 | Buscar productos | Catálogo | M | 3 |
| HU-08 | Consultar el kárdex de un producto | Inventario | M | 5 |
| HU-09 | Registrar un ajuste de stock | Inventario | M | 3 |
| HU-10 | Ver alertas de stock mínimo | Inventario | M | 3 |
| HU-11 | Gestionar proveedores | Compras | M | 2 |
| HU-12 | Registrar una compra | Compras | M | 8 |
| HU-13 | Anular una compra | Compras | S | 3 |
| HU-14 | Registrar una venta | Ventas | M | 8 |
| HU-15 | Anular una venta | Ventas | S | 3 |
| HU-16 | Consultar el historial de ventas | Ventas | S | 3 |
| HU-17 | Ver indicadores en el panel | Panel | M | 5 |
| HU-18 | Exportar a CSV | Panel | C | 2 |

**Total MVP (M + S):** 68 puntos · **Deseables (C):** 2 puntos.

---

## Autenticación y usuarios

### HU-01 · Registrar al primer administrador

**Como** dueño del negocio **quiero** crear la primera cuenta de administrador **para** empezar a usar el sistema.

- **Dado** que no existe ningún usuario, **cuando** envío nombre, correo y una contraseña válida, **entonces** se crea un usuario con rol `ADMIN` y puedo iniciar sesión.
- **Dado** que ya existe al menos un usuario, **cuando** intento usar el registro inicial, **entonces** el sistema lo rechaza (403).
- **Dado** una contraseña de menos de 8 caracteres, **cuando** envío el formulario, **entonces** se muestra un error de validación.

*Requisitos:* RF-01 · RNF-01

### HU-02 · Iniciar y cerrar sesión

**Como** usuario **quiero** iniciar sesión con mi correo y contraseña **para** acceder a las funciones de mi rol.

- **Dado** credenciales correctas de un usuario activo, **cuando** inicio sesión, **entonces** recibo un token y entro al panel.
- **Dado** credenciales incorrectas, **cuando** inicio sesión, **entonces** veo un mensaje genérico ("correo o contraseña incorrectos") sin indicar cuál falló.
- **Dado** un usuario desactivado, **cuando** intenta iniciar sesión, **entonces** se le niega el acceso.
- **Dado** que mi token venció, **cuando** hago una petición, **entonces** recibo 401 y vuelvo a la pantalla de inicio de sesión.
- **Dado** que tengo sesión activa, **cuando** cierro sesión, **entonces** el token se descarta en el cliente y vuelvo al inicio de sesión.

*Requisitos:* RF-02, RF-04 · RNF-02

### HU-03 · Gestionar usuarios y roles

**Como** administrador **quiero** crear y administrar las cuentas de mis empleados **para** controlar quién usa el sistema y qué puede hacer.

- **Dado** que soy `ADMIN`, **cuando** creo un usuario con correo no registrado y rol `EMPLEADO`, **entonces** queda activo y puede iniciar sesión.
- **Dado** un correo ya registrado, **cuando** intento crear otro usuario con él, **entonces** el sistema lo rechaza.
- **Dado** que soy el único `ADMIN` activo, **cuando** intento desactivarme o cambiar mi rol, **entonces** el sistema lo impide (RN-11).
- **Dado** que soy `EMPLEADO`, **cuando** intento acceder a la gestión de usuarios, **entonces** recibo 403.

*Requisitos:* RF-03 · RN-11 · RNF-03

### HU-04 · Cambiar mi contraseña

**Como** usuario **quiero** cambiar mi contraseña **para** mantener mi cuenta segura.

- **Dado** que ingreso mi contraseña actual correcta y una nueva válida, **cuando** confirmo, **entonces** la contraseña se actualiza.
- **Dado** que la contraseña actual es incorrecta, **cuando** confirmo, **entonces** el cambio se rechaza.

*Requisitos:* RF-05 · RNF-01

---

## Catálogo

### HU-05 · Gestionar categorías

**Como** administrador **quiero** organizar los productos en categorías **para** encontrarlos y analizarlos más fácilmente.

- **Dado** un nombre de categoría que no existe, **cuando** la creo, **entonces** queda disponible al registrar productos.
- **Dado** una categoría con productos, **cuando** la desactivo, **entonces** sus productos se conservan y la categoría ya no aparece para productos nuevos.

*Requisitos:* RF-06

### HU-06 · Gestionar productos

**Como** administrador **quiero** registrar y mantener mis productos con su precio y stock mínimo **para** venderlos y controlar su inventario.

- **Dado** un código no registrado, **cuando** creo un producto con nombre, categoría, unidad, precio de venta y stock mínimo, **entonces** se crea con stock 0.
- **Dado** un código ya registrado, **cuando** intento crear otro producto con él, **entonces** el sistema lo rechaza (RN-10).
- **Dado** un producto con movimientos, **cuando** intento eliminarlo, **entonces** el sistema solo permite desactivarlo (RF-09).
- **Dado** que cambio el precio de un producto, **cuando** consulto ventas anteriores, **entonces** conservan el precio original (RN-06).

*Requisitos:* RF-07, RF-09 · RN-06, RN-10

### HU-07 · Buscar productos

**Como** usuario **quiero** buscar productos por nombre o código **para** encontrarlos rápido mientras atiendo.

- **Dado** que escribo parte del nombre o el código, **cuando** busco, **entonces** veo los productos activos que coinciden, con su precio y stock.
- **Dado** que filtro por una categoría, **cuando** busco, **entonces** solo veo productos de esa categoría.

*Requisitos:* RF-08 · RNF-06

---

## Inventario

### HU-08 · Consultar el kárdex de un producto

**Como** usuario **quiero** ver el historial de movimientos de un producto **para** entender cómo llegó a su stock actual.

- **Dado** un producto con movimientos, **cuando** abro su kárdex, **entonces** veo por cada movimiento: fecha, tipo, documento de origen, entrada, salida, saldo y usuario, en orden cronológico.
- **Dado** un rango de fechas, **cuando** filtro, **entonces** veo el saldo inicial del rango y solo los movimientos dentro de él.
- **Dado** cualquier movimiento, **cuando** reviso su saldo, **entonces** es igual al saldo anterior + entrada − salida (RN-02).

*Requisitos:* RF-12, RF-13 · RN-02, RN-04

### HU-09 · Registrar un ajuste de stock

**Como** administrador **quiero** registrar ajustes por merma o conteo físico **para** que el stock del sistema coincida con el real.

- **Dado** un producto con stock 20, **cuando** registro un ajuste de salida de 3 con motivo "merma", **entonces** se crea un movimiento de salida y el stock queda en 17.
- **Dado** un ajuste sin motivo, **cuando** lo envío, **entonces** se rechaza (RN-05).
- **Dado** un ajuste de salida mayor que el stock actual, **cuando** lo envío, **entonces** se rechaza (RN-01).
- **Dado** que soy `EMPLEADO`, **cuando** intento registrar un ajuste, **entonces** recibo 403.

*Requisitos:* RF-11 · RN-01, RN-03, RN-05

### HU-10 · Ver alertas de stock mínimo

**Como** administrador **quiero** ver qué productos están en su stock mínimo o por debajo **para** reponerlos a tiempo.

- **Dado** un producto con stock mínimo 10, **cuando** su stock llega a 10 o menos, **entonces** aparece en la lista de stock crítico del panel.
- **Dado** que registro una compra que lo deja por encima de 10, **cuando** reviso el panel, **entonces** ya no aparece en la lista.

*Requisitos:* RF-14, RF-24 · RN-09

---

## Proveedores y compras

### HU-11 · Gestionar proveedores

**Como** administrador **quiero** registrar a mis proveedores **para** asociarlos a las compras.

- **Dado** un RUC válido de 11 dígitos no registrado, **cuando** creo el proveedor, **entonces** queda disponible al registrar compras.
- **Dado** un RUC ya registrado o con formato inválido, **cuando** creo el proveedor, **entonces** el sistema lo rechaza (RN-10).

*Requisitos:* RF-15 · RN-10

### HU-12 · Registrar una compra

**Como** administrador **quiero** registrar la mercadería que compro **para** que el stock aumente automáticamente.

- **Dado** un proveedor y dos productos, **cuando** registro una compra con sus cantidades y costos, **entonces** se crea un movimiento de entrada por cada producto y su stock aumenta.
- **Dado** una compra registrada, **cuando** reviso los productos, **entonces** su costo referencial es el último costo comprado (RN-08).
- **Dado** una compra con un ítem inválido (cantidad ≤ 0), **cuando** la envío, **entonces** no se guarda nada (RNF-04).

*Requisitos:* RF-10, RF-16 · RN-04, RN-08 · RNF-04

### HU-13 · Anular una compra

**Como** administrador **quiero** anular una compra registrada por error **para** corregir el stock sin borrar el historial.

- **Dado** una compra cuyos productos aún tienen stock suficiente, **cuando** la anulo con un motivo, **entonces** se generan movimientos de salida inversos y la compra queda en estado "anulada".
- **Dado** que anular dejaría algún producto en negativo, **cuando** intento anular, **entonces** se rechaza (RN-01).

*Requisitos:* RF-17 · RN-01, RN-03, RN-05

---

## Ventas

### HU-14 · Registrar una venta

**Como** vendedor **quiero** registrar una venta en pocos pasos **para** atender rápido y que el stock se descuente solo.

- **Dado** productos con stock suficiente, **cuando** agrego ítems y confirmo, **entonces** se registra la venta, se generan movimientos de salida y obtengo un comprobante con número correlativo (RN-07).
- **Dado** un ítem cuya cantidad supera el stock, **cuando** confirmo, **entonces** la venta completa se rechaza y se indica qué producto no alcanza (RN-01, RNF-04).
- **Dado** dos ventas simultáneas del último ítem de un producto, **cuando** ambas se confirman, **entonces** solo una se registra (RNF-05).
- **Dado** una venta registrada, **cuando** abro su comprobante, **entonces** veo número, fecha, vendedor, ítems, precios y total, listo para imprimir.

*Requisitos:* RF-10, RF-18, RF-19 · RN-01, RN-06, RN-07 · RNF-04, RNF-05

### HU-15 · Anular una venta

**Como** administrador **quiero** anular una venta con un motivo **para** devolver el stock cuando hubo un error o una devolución.

- **Dado** una venta vigente, **cuando** la anulo con motivo, **entonces** se generan movimientos de entrada inversos y la venta queda "anulada" sin perder su número.
- **Dado** una venta ya anulada, **cuando** intento anularla otra vez, **entonces** se rechaza.

*Requisitos:* RF-20 · RN-03, RN-05, RN-07

### HU-16 · Consultar el historial de ventas

**Como** usuario **quiero** ver las ventas registradas **para** revisar lo vendido en un periodo.

- **Dado** que soy `ADMIN`, **cuando** filtro por fecha y vendedor, **entonces** veo todas las ventas que coinciden con su total.
- **Dado** que soy `EMPLEADO`, **cuando** abro el historial, **entonces** solo veo mis ventas.

*Requisitos:* RF-21 · RNF-03

---

## Panel y reportes

### HU-17 · Ver indicadores en el panel

**Como** administrador **quiero** ver un resumen al entrar **para** saber cómo va el negocio sin revisar documento por documento.

- **Dado** un rango de fechas, **cuando** abro el panel, **entonces** veo el total vendido por día (sin contar ventas anuladas).
- **Dado** el mismo rango, **cuando** abro el panel, **entonces** veo los 10 productos más vendidos por cantidad.
- **Dado** productos en stock crítico, **cuando** abro el panel, **entonces** veo su lista con stock actual y mínimo.

*Requisitos:* RF-22, RF-23, RF-24

### HU-18 · Exportar a CSV

**Como** administrador **quiero** exportar el kárdex o las ventas a CSV **para** analizarlos en una hoja de cálculo.

- **Dado** un kárdex o historial filtrado, **cuando** exporto, **entonces** descargo un CSV en UTF-8 con las mismas columnas que veo en pantalla.

*Requisitos:* RF-25
