/**
 * Formato de montos y cantidades.
 *
 * La API envía decimales como string (`"12.50"`) para no perder precisión (docs/api/README.md).
 * Aquí solo se formatean para mostrarlos; los cálculos se hacen en el backend.
 */

const money = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const quantity = new Intl.NumberFormat('es-PE', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 3,
})

/** `"1234.5"` → `"S/ 1,234.50"` */
export function formatMoney(amount: string): string {
  return money.format(Number(amount))
}

/** `"1.250"` → `"1.25"`, `"3.000"` → `"3"` */
export function formatQuantity(value: string): string {
  return quantity.format(Number(value))
}
