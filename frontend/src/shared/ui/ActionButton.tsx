import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { Icon, type IconName } from './Icon'

interface ActionProps {
  icon: IconName
  /** Texto del botón para lectores de pantalla y del aviso al pasar el mouse. */
  label: string
  tone?: 'default' | 'danger'
}

function className(tone: ActionProps['tone']) {
  return tone === 'danger' ? 'action-button action-button--danger' : 'action-button'
}

/** Botón cuadrado con icono para las acciones de una fila o tarjeta (ver, editar, eliminar…). */
export function ActionButton({ icon, label, tone, onClick, disabled }: ActionProps & { onClick: () => void; disabled?: boolean }) {
  return (
    <button type="button" className={className(tone)} aria-label={label} title={label} onClick={onClick} disabled={disabled}>
      <Icon name={icon} size={16} />
    </button>
  )
}

/** Igual que {@link ActionButton}, pero navega a otra pantalla del panel. */
export function ActionLink({ icon, label, tone, to }: ActionProps & { to: string }) {
  return (
    <Link className={className(tone)} aria-label={label} title={label} to={to}>
      <Icon name={icon} size={16} />
    </Link>
  )
}

export function Actions({ children, label }: { children: ReactNode; label: string }) {
  return (
    <div className="actions" role="group" aria-label={label}>
      {children}
    </div>
  )
}
