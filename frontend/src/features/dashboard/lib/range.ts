/** Rango de fechas del inicio: atajos y lectura desde la URL. Las fechas son `AAAA-MM-DD` del negocio. */

export interface DateRange {
  from: string
  to: string
}

export type Preset = 'today' | 'last7' | 'last30' | 'month'

export const PRESETS: { id: Preset; label: string }[] = [
  { id: 'today', label: 'Hoy' },
  { id: 'last7', label: 'Últimos 7 días' },
  { id: 'last30', label: 'Últimos 30 días' },
  { id: 'month', label: 'Este mes' },
]

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

/** Parámetros de la URL; en español porque los ve el usuario (ADR 0003). */
export const FROM_PARAM = 'desde'
export const TO_PARAM = 'hasta'

function addDays(date: string, days: number): string {
  const result = new Date(`${date}T00:00:00Z`)
  result.setUTCDate(result.getUTCDate() + days)
  return result.toISOString().slice(0, 10)
}

export function presetRange(preset: Preset, today: string): DateRange {
  switch (preset) {
    case 'today':
      return { from: today, to: today }
    case 'last7':
      return { from: addDays(today, -6), to: today }
    case 'last30':
      return { from: addDays(today, -29), to: today }
    case 'month':
      return { from: `${today.slice(0, 8)}01`, to: today }
  }
}

export function matchingPreset(range: DateRange, today: string): Preset | undefined {
  return PRESETS.find(({ id }) => {
    const candidate = presetRange(id, today)
    return candidate.from === range.from && candidate.to === range.to
  })?.id
}

function validDate(value: string | null): value is string {
  return value !== null && ISO_DATE.test(value) && !Number.isNaN(Date.parse(`${value}T00:00:00Z`))
}

/** Rango guardado en la URL; si falta o no es una fecha válida, los últimos 30 días. */
export function rangeFromParams(params: URLSearchParams, today: string): DateRange {
  const from = params.get(FROM_PARAM)
  const to = params.get(TO_PARAM)
  return validDate(from) && validDate(to) ? { from, to } : presetRange('last30', today)
}

const longDate = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })

/** `"2026-11-01"` → `"1 de noviembre de 2026"`. La fecha ya es del negocio: se formatea sin cambiar de zona. */
export function formatBusinessDate(date: string): string {
  return longDate.format(new Date(`${date}T00:00:00Z`))
}
