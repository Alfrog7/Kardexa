import { useState } from 'react'
import { ApiError } from '@shared/api/http'

interface ExportButtonProps {
  /** Descarga el archivo; si falla, el botón muestra el motivo. */
  onExport: () => Promise<void>
  disabled?: boolean
}

/** «Exportar CSV» (HU-18): queda deshabilitado mientras descarga y avisa si algo falla. */
export function ExportButton({ onExport, disabled }: ExportButtonProps) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()

  async function handleClick() {
    setBusy(true)
    setError(undefined)
    try {
      await onExport()
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : 'No se pudo exportar. Inténtalo otra vez.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="export-button">
      <button className="button button--secondary" type="button" onClick={handleClick} disabled={disabled || busy}>
        {busy ? 'Exportando…' : 'Exportar CSV'}
      </button>
      {error ? (
        <p className="field__error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  )
}
