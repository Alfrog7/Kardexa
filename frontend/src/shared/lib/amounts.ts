/**
 * Subtotales y total en vivo de los formularios de compra y de venta. Solo sirven para mostrarlos mientras se escribe:
 * el total que vale es el que calcula la API. Se usan enteros (milésimas y céntimos) para no perder precisión.
 */

export const QUANTITY_PATTERN = /^\d{1,9}(\.\d{1,3})?$/
export const AMOUNT_PATTERN = /^\d{1,10}(\.\d{1,2})?$/

function scaled(value: string, decimals: number): bigint {
  const [whole, fraction = ''] = value.split('.')
  return BigInt(whole + fraction.padEnd(decimals, '0'))
}

/** Céntimos de cantidad × costo, redondeados a 2 decimales hacia arriba en el medio; `null` si algo no es válido. */
export function subtotalCents(quantity: string, unitCost: string): bigint | null {
  if (!QUANTITY_PATTERN.test(quantity) || !AMOUNT_PATTERN.test(unitCost)) return null
  const thousandthsOfCents = scaled(quantity, 3) * scaled(unitCost, 2)
  return (thousandthsOfCents + 500n) / 1000n
}

/** `1234n` → `"12.34"`, el formato decimal que espera `formatMoney`. */
export function centsToAmount(cents: bigint): string {
  const text = cents.toString().padStart(3, '0')
  return `${text.slice(0, -2)}.${text.slice(-2)}`
}

/** `true` si `quantity` supera `available` (las dos con hasta 3 decimales); `false` si alguna no es válida. */
export function exceeds(quantity: string, available: string): boolean {
  if (!QUANTITY_PATTERN.test(quantity) || !QUANTITY_PATTERN.test(available)) return false
  return scaled(quantity, 3) > scaled(available, 3)
}
