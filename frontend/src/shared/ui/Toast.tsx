import { useEffect } from 'react'

const TOAST_DURATION_MS = 4000

interface ToastProps {
  message: string
  onDismiss: () => void
}

/** Aviso temporal de que una acción terminó bien. Los errores no van aquí: se quedan en pantalla. */
export function Toast({ message, onDismiss }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onDismiss, TOAST_DURATION_MS)
    return () => clearTimeout(timer)
  }, [message, onDismiss])

  return (
    <p className="toast" role="status">
      {message}
    </p>
  )
}
