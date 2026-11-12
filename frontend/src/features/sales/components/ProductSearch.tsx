import { useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import { findProducts, type ProductOption } from '../api/sales'

const SEARCH_DELAY_MS = 300

interface SearchResult {
  term: string
  options?: ProductOption[]
  error?: string
}

interface ProductSearchProps {
  /** Productos ya agregados: no se ofrecen otra vez (un producto no se repite en la venta). */
  excludedIds: string[]
  onSelect: (product: ProductOption) => void
}

/** Busca por nombre o código y muestra precio y stock; un producto sin stock no se puede agregar. */
export function ProductSearch({ excludedIds, onSelect }: ProductSearchProps) {
  const [text, setText] = useState('')
  const [result, setResult] = useState<SearchResult>()
  const term = text.trim()

  useEffect(() => {
    if (!term) return
    const timer = setTimeout(() => {
      findProducts(term)
        .then((options) => setResult({ term, options }))
        .catch((error: unknown) => {
          const message = error instanceof ApiError ? error.message : 'No pudimos buscar. Inténtalo otra vez.'
          setResult({ term, error: message })
        })
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [term])

  const shown = term && result?.term === term ? result : undefined
  const options = shown?.options?.filter((option) => !excludedIds.includes(option.id))

  function choose(option: ProductOption) {
    onSelect(option)
    setText('')
  }

  return (
    <div className="sale-form__search">
      <div className="field">
        <label htmlFor="sale-product-search">Agregar producto</label>
        <input
          data-autofocus
          id="sale-product-search"
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
      {options?.length === 0 ? <p className="muted">Ningún producto nuevo coincide con tu búsqueda.</p> : null}
      {options?.length ? (
        <ul className="sale-form__results card" aria-label="Productos encontrados">
          {options.map((option) => {
            const empty = Number(option.stock) <= 0
            return (
              <li key={option.id}>
                <button className="link-button" type="button" disabled={empty} onClick={() => choose(option)}>
                  {option.name}
                </button>
                <span className="sale-form__option-meta">
                  {option.code} · {formatMoney(option.salePrice)} · {empty ? 'sin stock' : `stock ${formatQuantity(option.stock)}`}
                </span>
              </li>
            )
          })}
        </ul>
      ) : null}
    </div>
  )
}
