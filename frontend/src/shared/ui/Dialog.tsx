import { useEffect, useId, useRef, type ReactNode } from 'react'

interface DialogProps {
  title: string
  /** `modal` para formularios cortos; `drawer` entra por la derecha para los largos. */
  variant?: 'modal' | 'drawer'
  onClose: () => void
  children: ReactNode
}

/**
 * Ventana sobre `<dialog>` nativo: el navegador retiene el foco dentro y lo devuelve al cerrar.
 * Se abre al montarse, así que quien la usa la renderiza solo cuando corresponde.
 * El campo con `data-autofocus` recibe el foco inicial.
 */
export function Dialog({ title, variant = 'modal', onClose, children }: DialogProps) {
  const dialog = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    element?.querySelector<HTMLElement>('[data-autofocus]')?.focus()
    return () => element?.close()
  }, [])

  return (
    <dialog
      ref={dialog}
      className={`dialog dialog--${variant}`}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
    >
      <header className="dialog__header">
        <h2 id={titleId} className="dialog__title">
          {title}
        </h2>
        <button className="icon-button" type="button" aria-label="Cerrar" onClick={onClose}>
          <span aria-hidden="true">✕</span>
        </button>
      </header>
      <div className="dialog__body">{children}</div>
    </dialog>
  )
}
