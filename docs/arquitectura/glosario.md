# Glosario

Traducción oficial de los términos de negocio al código ([ADR 0003](../adr/0003-criterio-de-nombres.md)). Si un término no está aquí, se agrega en el PR que lo introduce.

| Término de negocio | En el código | Módulo | Definición |
|---|---|---|---|
| Usuario | `User` / `app_user` | identity | Persona que inicia sesión. (`user` es palabra reservada en PostgreSQL.) |
| Administrador | `Role.ADMIN` | identity | Dueño o encargado; acceso total. |
| Empleado | `Role.EMPLOYEE` | identity | Vendedor; registra ventas y consulta stock. |
| Categoría | `Category` | catalog | Agrupación de productos. |
| Producto | `Product` | catalog | Artículo que se compra y se vende. |
| Código de producto | `code` | catalog | Identificador visible y único (puede ser el código de barras). |
| Unidad de medida | `Unit` | catalog | `UNIT`, `KG`, `LITER`, `BOX`. |
| Precio de venta | `salePrice` | catalog | Precio vigente al público. |
| Costo referencial | `referenceCost` | catalog | Último costo de compra (RN-08). |
| Stock mínimo | `minStock` | catalog | Umbral de alerta (RN-09). |
| Stock | `ProductStock` | inventory | Cantidad disponible actual. |
| Kárdex | `Kardex` | inventory | Historial de movimientos de un producto con su saldo. |
| Movimiento | `StockMovement` | inventory | Entrada o salida que altera el stock. |
| Entrada / salida | `Direction.IN` / `Direction.OUT` | inventory | Sentido del movimiento. |
| Saldo | `balanceAfter` | inventory | Stock resultante tras el movimiento (RN-02). |
| Ajuste | `StockAdjustment` | inventory | Corrección manual con motivo (merma, conteo…). |
| Merma | `AdjustmentReason.SHRINKAGE` | inventory | Pérdida por daño, vencimiento o robo. |
| Stock crítico | `criticalStock` | inventory | Stock ≤ stock mínimo (RN-09). |
| Proveedor | `Supplier` | purchasing | Empresa que abastece mercadería. |
| RUC | `ruc` | purchasing | Registro Único de Contribuyentes (11 dígitos). |
| Razón social | `businessName` | purchasing | Nombre legal del proveedor. |
| Compra | `Purchase` | purchasing | Ingreso de mercadería de un proveedor. |
| Venta | `Sale` | sales | Salida de mercadería a un cliente. |
| Comprobante interno | `Receipt` | sales | Documento imprimible de la venta (no es tributario). |
| Número correlativo | `number` | sales | `V-000001`, `V-000002`… (RN-07). |
| Anulación | `void` / `VOIDED` | purchasing, sales | Cancelación que revierte el stock sin borrar el documento. |
| Vendedor | `seller` | sales | Usuario que registró la venta. |
| Panel / indicadores | `Dashboard` | reporting | Resumen de ventas y stock. |
