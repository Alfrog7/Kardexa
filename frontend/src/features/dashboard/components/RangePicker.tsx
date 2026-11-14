import { PRESETS, matchingPreset, presetRange, type DateRange } from '../lib/range'

interface RangePickerProps {
  range: DateRange
  /** Fecha del negocio de hoy, `AAAA-MM-DD`. */
  today: string
  onChange: (range: DateRange) => void
}

/** Atajos de rango y fechas personalizadas para los indicadores del inicio. */
export function RangePicker({ range, today, onChange }: RangePickerProps) {
  const active = matchingPreset(range, today)

  return (
    <div className="range-picker" role="group" aria-label="Rango de fechas">
      <div className="range-picker__presets">
        {PRESETS.map((preset) => (
          <button
            key={preset.id}
            type="button"
            className="button button--secondary range-picker__preset"
            aria-pressed={active === preset.id}
            onClick={() => onChange(presetRange(preset.id, today))}
          >
            {preset.label}
          </button>
        ))}
      </div>
      <div className="range-picker__dates">
        <div className="field">
          <label htmlFor="dashboard-from">Desde</label>
          <input
            id="dashboard-from"
            type="date"
            value={range.from}
            max={range.to}
            onChange={(event) => event.target.value && onChange({ ...range, from: event.target.value })}
          />
        </div>
        <div className="field">
          <label htmlFor="dashboard-to">Hasta</label>
          <input
            id="dashboard-to"
            type="date"
            value={range.to}
            min={range.from}
            onChange={(event) => event.target.value && onChange({ ...range, to: event.target.value })}
          />
        </div>
      </div>
    </div>
  )
}
