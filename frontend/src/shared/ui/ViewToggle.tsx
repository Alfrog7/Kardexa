import { Icon, type IconName } from './Icon'

import type { ViewMode } from './useViewMode'

export type { ViewMode }

const OPTIONS: { mode: ViewMode; label: string; icon: IconName }[] = [
  { mode: 'cards', label: 'Vista de tarjetas', icon: 'grid' },
  { mode: 'list', label: 'Vista de lista', icon: 'list' },
  { mode: 'table', label: 'Vista de tabla', icon: 'table' },
]

interface ViewToggleProps {
  value: ViewMode
  onChange: (mode: ViewMode) => void
}

/** Botones para ver una lista como tarjetas, filas o tabla. */
export function ViewToggle({ value, onChange }: ViewToggleProps) {
  return (
    <div className="view-toggle" role="group" aria-label="Cambiar vista">
      {OPTIONS.map((option) => (
        <button
          key={option.mode}
          type="button"
          className={value === option.mode ? 'view-toggle__button view-toggle__button--active' : 'view-toggle__button'}
          aria-label={option.label}
          aria-pressed={value === option.mode}
          title={option.label}
          onClick={() => onChange(option.mode)}
        >
          <Icon name={option.icon} size={18} />
        </button>
      ))}
    </div>
  )
}
