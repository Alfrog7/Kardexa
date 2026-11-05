# 01 · Problemática

> Fase 0 · Documento vivo: se actualiza si cambia el entendimiento del problema.

## 1. Contexto

En el Perú, gran parte del comercio minorista lo sostienen negocios pequeños: bodegas de barrio, ferreterías y minimarkets. Suelen tener entre 1 y 5 personas atendiendo, cientos de productos distintos y proveedores que reponen mercadería una o varias veces por semana.

El **kárdex** es la tarjeta de control de inventario que registra, por producto, cada **entrada** (compra, devolución), cada **salida** (venta, merma) y el **saldo** resultante. Es la base de cualquier control de stock confiable, pero llevarlo a mano en un negocio con mucho movimiento diario es poco práctico, así que en la práctica casi nunca se mantiene al día.

## 2. Problema

> Los pequeños negocios no tienen un registro confiable y actualizado de su inventario ni de sus ventas, por lo que toman decisiones de compra y precio a ciegas.

### Síntomas observables

- El stock se controla en un cuaderno, en Excel o "de memoria".
- Un producto se agota y el dueño se entera cuando un cliente lo pide.
- Se compra de más lo que no rota y de menos lo que sí se vende.
- No se sabe cuánto se vendió en el día ni cuánto se ganó.
- Las diferencias entre lo registrado y lo que hay en el anaquel no tienen explicación (mermas, errores, robos).

### Causas

| Causa | Detalle |
|---|---|
| Registro manual | Anotar cada movimiento consume tiempo en horas punta, así que se posterga o se omite. |
| Herramientas inadecuadas | Excel no impide errores (fórmulas rotas, filas borradas) ni guarda un historial de quién cambió qué. |
| Software costoso o complejo | Los ERP y POS comerciales son caros, piden capacitación o incluyen funciones que el negocio no usa. |
| Compras y ventas desconectadas | Una compra no actualiza el stock automáticamente, ni una venta lo descuenta. |

### Consecuencias

- **Ventas perdidas** por quiebres de stock.
- **Capital inmovilizado** en productos de baja rotación.
- **Pérdidas no detectadas** (mermas, faltantes).
- **Decisiones sin datos** sobre qué comprar, a quién y a qué precio vender.

## 3. Actores y partes interesadas

| Actor | Descripción | Necesidad principal |
|---|---|---|
| **Administrador** (dueño) | Gestiona el negocio, compra a proveedores y define precios. | Saber qué tiene, qué se vende y cuánto gana. |
| **Empleado** (vendedor) | Atiende al público y registra ventas. | Registrar una venta rápido, sin pasos innecesarios. |
| **Proveedor** | Abastece mercadería. *No usa el sistema*: es un dato. | — |
| **Cliente final** | Compra en el negocio. *No usa el sistema* en el MVP. | — |

## 4. Objetivo general

Construir un sistema web que permita a un pequeño negocio **registrar compras y ventas, mantener el kárdex de cada producto actualizado automáticamente y anticiparse a los quiebres de stock**, con una interfaz simple para el día a día.

### Objetivos específicos

1. Llevar el kárdex por producto a partir de los movimientos, sin cálculos manuales.
2. Alertar cuando un producto llega a su stock mínimo.
3. Registrar una venta en pocos pasos y descontar el stock en la misma operación.
4. Registrar compras a proveedores que sumen stock.
5. Mostrar indicadores básicos: ventas por día, productos más vendidos y stock crítico.
6. Controlar el acceso por roles (administrador y empleado).

## 5. Alcance del MVP

### Incluido

- Autenticación propia con roles `ADMIN` y `EMPLEADO`.
- Catálogo de productos y categorías, con precio de venta y stock mínimo.
- Kárdex por producto: entradas, salidas, ajustes y saldo.
- Proveedores y compras (suman stock).
- Ventas con comprobante interno (descuentan stock).
- Panel con indicadores básicos.
- Un solo negocio y un solo almacén por instalación.

### No incluido (por ahora)

- Facturación electrónica ante SUNAT (boletas y facturas oficiales).
- Varios locales o almacenes.
- Integración con lectores de código de barras o balanzas (más allá de escribir el código a mano).
- Pasarelas de pago, delivery o tienda en línea.
- Contabilidad, planillas o cuentas por pagar.
- Aplicación móvil nativa (el panel será web y adaptable).
- Valorización del inventario por métodos contables (PEPS, promedio ponderado). Se evaluará en una fase posterior.

## 6. Supuestos y restricciones

### Supuestos

- El negocio tiene al menos una computadora o tablet con navegador e internet.
- Hay una persona (el administrador) dispuesta a cargar el catálogo inicial.
- Las ventas son al contado; no se manejan créditos a clientes en el MVP.

### Restricciones

- **Sin servicios externos de autenticación** ni BaaS: todas las APIs son propias.
- **Costo cero de infraestructura**: desarrollo en local y despliegue en planes gratuitos.
- Proyecto de una sola persona, sin fecha límite, construido por fases.

## 7. Métricas de éxito

| Métrica | Meta del MVP |
|---|---|
| Tiempo para registrar una venta de 3 productos | Menos de 30 segundos |
| Coherencia del stock | El saldo del kárdex siempre es igual a entradas − salidas |
| Alerta de stock mínimo | Visible en el panel en cuanto el saldo llega al mínimo |
| Trazabilidad | Todo movimiento indica fecha, usuario y documento de origen |
