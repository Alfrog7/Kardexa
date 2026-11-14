import { describe, expect, it } from 'vitest'
import { formatBusinessDate, matchingPreset, presetRange, rangeFromParams } from './range'

describe('rango del inicio', () => {
  const today = '2026-03-05'

  it('calcula los atajos con días completos e incluidos, cruzando meses', () => {
    expect(presetRange('today', today)).toEqual({ from: today, to: today })
    expect(presetRange('last7', today)).toEqual({ from: '2026-02-27', to: today })
    expect(presetRange('last30', today)).toEqual({ from: '2026-02-04', to: today })
    expect(presetRange('month', today)).toEqual({ from: '2026-03-01', to: today })
  })

  it('reconoce el atajo de un rango y no inventa uno para fechas personalizadas', () => {
    expect(matchingPreset({ from: '2026-02-27', to: today }, today)).toBe('last7')
    expect(matchingPreset({ from: '2026-01-01', to: '2026-01-31' }, today)).toBeUndefined()
  })

  it('lee el rango de la URL y usa los últimos 30 días si falta o no es válido', () => {
    expect(rangeFromParams(new URLSearchParams('desde=2026-01-01&hasta=2026-01-31'), today)).toEqual({ from: '2026-01-01', to: '2026-01-31' })
    expect(rangeFromParams(new URLSearchParams('desde=ayer&hasta=2026-01-31'), today)).toEqual(presetRange('last30', today))
    expect(rangeFromParams(new URLSearchParams(''), today)).toEqual(presetRange('last30', today))
  })

  it('formatea la fecha del negocio sin correrla de día', () => {
    expect(formatBusinessDate('2026-11-01')).toBe('1 de noviembre de 2026')
  })
})
