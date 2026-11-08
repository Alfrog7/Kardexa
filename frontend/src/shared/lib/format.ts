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

/** Zona del negocio: la misma que usa la API para los rangos de fechas. */
const BUSINESS_TIME_ZONE = 'America/Lima'

const dateTime = new Intl.DateTimeFormat('es-PE', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: BUSINESS_TIME_ZONE,
})

const isoDate = new Intl.DateTimeFormat('en-CA', { timeZone: BUSINESS_TIME_ZONE })

/** `"2026-11-01T15:00:00Z"` → `"1/11/26, 10:00 a. m."` */
export function formatDateTime(instant: string): string {
  return dateTime.format(new Date(instant))
}

/** Fecha del negocio en formato `AAAA-MM-DD`, la que esperan la API y los campos de fecha. */
export function businessDate(date: Date): string {
  return isoDate.format(date)
}

/** `"1234.5"` → `"S/ 1,234.50"` */
export function formatMoney(amount: string): string {
  return money.format(Number(amount))
}

/** `"1.250"` → `"1.25"`, `"3.000"` → `"3"` */
export function formatQuantity(value: string): string {
  return quantity.format(Number(value))
}
