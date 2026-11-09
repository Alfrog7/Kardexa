import { useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { findProducts, type ProductOption } from '../api/inventory'

const SEARCH_DELAY_MS = 300

interface PickerResult {
  term: string
  options?: ProductOption[]
  error?: string
}

interface ProductPickerProps {
  onSelect: (product: ProductOption) => void
}

/** Busca por nombre o código y ofrece los productos activos que coinciden. */
export function ProductPicker({ onSelect }: ProductPickerProps) {
  const [text, setText] = useState('')
  const [result, setResult] = useState<PickerResult>()
  const term = text.trim()

  useEffect(() => {
    if (!term) return
    // Una respuesta atrasada no se muestra: su término ya no coincide con el escrito.
    const timer = setTimeout(() => {
      findProducts(term)
        .then((options) => {
          setResult({ term, options })
        })
        .catch((error: unknown) => {
          const message = error instanceof ApiError ? error.message : 'No pudimos buscar. Inténtalo otra vez.'
          setResult({ term, error: message })
        })
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [term])

  const shown = term && result?.term === term ? result : undefined

  return (
    <div className="product-picker">
      <div className="field">
        <label htmlFor="inventory-product">Producto</label>
        <input
          id="inventory-product"
          type="search"
          placeholder="Escribe el nombre o el código"
          value={text}
          maxLength={100}
          onChange={(event) => setText(event.target.value)}
        />
      </div>
      {shown?.error ? (
        <p className="alert" role="alert">
          {shown.error}
        </p>
      ) : null}
      {shown?.options?.length === 0 ? <p className="muted">Ningún producto coincide con tu búsqueda.</p> : null}
      {shown?.options?.length ? (
        <ul className="product-picker__results card" aria-label="Resultados">
          {shown.options.map((option) => (
            <li key={option.id}>
              <button className="link-button" type="button" onClick={() => onSelect(option)}>
                {option.name}
              </button>
              <span className="muted"> · {option.code}</span>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
