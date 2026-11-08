import { useEffect, useMemo, useRef, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import { listCategories, type Category } from '../api/categories'
import {
  createProduct,
  searchProducts,
  updateProduct,
  type Product,
  type ProductPage,
  type ProductRequest,
} from '../api/products'
import { ProductForm } from '../components/ProductForm'
import './ProductsPage.css'

const SEARCH_DELAY_MS = 300

interface SearchResult {
  /** Consulta que produjo este resultado: si no es la actual, hay otra en curso. */
  query: object
  page?: ProductPage
  error?: string
}

/** El formulario está cerrado, crea un producto o edita el indicado. */
type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; product: Product }

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Productos (HU-06, HU-07, HU-10): búsqueda, filtros, marca de stock crítico, alta y edición. */
export function ProductsPage() {
  const [text, setText] = useState('')
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [criticalOnly, setCriticalOnly] = useState(false)
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<SearchResult>()
  const [categories, setCategories] = useState<Category[]>([])
  const [categoriesError, setCategoriesError] = useState<string>()
  const [form, setForm] = useState<FormMode>({ kind: 'closed' })
  const [saved, setSaved] = useState<string>()
  const newButton = useRef<HTMLButtonElement>(null)

  const query = useMemo(
    () => ({ q, categoryId, criticalOnly, page, attempt }),
    [q, categoryId, criticalOnly, page, attempt],
  )

  useEffect(() => {
    let current = true
    listCategories()
      .then((loaded) => {
        if (current) setCategories([...loaded].sort((a, b) => a.name.localeCompare(b.name, 'es')))
      })
      .catch((error: unknown) => {
        if (current) setCategoriesError(messageOf(error))
      })
    return () => {
      current = false
    }
  }, [])

  // Espera a que el usuario deje de escribir antes de consultar.
  useEffect(() => {
    const timer = setTimeout(() => {
      setQ(text.trim())
      setPage(0)
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [text])

  // Solo la última consulta actualiza la tabla: una respuesta atrasada se descarta.
  useEffect(() => {
    let current = true
    searchProducts(query)
      .then((found) => {
        if (current) setResult({ query, page: found })
      })
      .catch((error: unknown) => {
        if (current) setResult({ query, error: messageOf(error) })
      })
    return () => {
      current = false
    }
  }, [query])

  const loading = result?.query !== query
  const found = result?.page

  function closeForm() {
    setForm({ kind: 'closed' })
    newButton.current?.focus()
  }

  async function handleSubmit(request: ProductRequest) {
    if (form.kind === 'edit') {
      await updateProduct(form.product.id, request)
    } else {
      await createProduct(request)
    }
    setSaved(`Producto ${request.name} guardado.`)
    setAttempt((value) => value + 1)
    closeForm()
  }

  return (
    <section className="page">
      <header className="page__header products__header">
        <h1>Productos</h1>
        <button
          ref={newButton}
          className="button"
          type="button"
          onClick={() => {
            setSaved(undefined)
            setForm({ kind: 'create' })
          }}
        >
          Nuevo producto
        </button>
      </header>

      {form.kind !== 'closed' ? (
        <ProductForm
          key={form.kind === 'edit' ? form.product.id : 'new'}
          product={form.kind === 'edit' ? form.product : undefined}
          categories={categories}
          onSubmit={handleSubmit}
          onCancel={closeForm}
        />
      ) : null}

      {saved ? <p role="status">{saved}</p> : null}

      {categoriesError ? (
        <p className="alert" role="alert">
          {categoriesError}
        </p>
      ) : null}

      <div className="products__filters" role="search">
        <div className="field">
          <label htmlFor="products-search">Buscar por nombre o código</label>
          <input
            id="products-search"
            type="search"
            value={text}
            maxLength={100}
            onChange={(event) => setText(event.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="products-category">Categoría</label>
          <select
            id="products-category"
            value={categoryId}
            onChange={(event) => {
              setCategoryId(event.target.value)
              setPage(0)
            }}
          >
            <option value="">Todas</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.active ? category.name : `${category.name} (desactivada)`}
              </option>
            ))}
          </select>
        </div>
        <label className="check">
          <input
            type="checkbox"
            checked={criticalOnly}
            onChange={(event) => {
              setCriticalOnly(event.target.checked)
              setPage(0)
            }}
          />
          Solo stock crítico
        </label>
      </div>

      {loading ? (
        <p className="muted" role="status">
          Cargando productos…
        </p>
      ) : null}

      {result?.error && !loading ? (
        <div className="alert" role="alert">
          <p>{result.error}</p>
          <button className="button button--secondary" type="button" onClick={() => setAttempt((value) => value + 1)}>
            Reintentar
          </button>
        </div>
      ) : null}

      {found && found.content.length === 0 && !loading ? (
        <p className="muted">No hay productos con estos filtros.</p>
      ) : null}

      {found && found.content.length > 0 ? (
        <div className="card products__table-card">
          <table className="products__table">
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Nombre</th>
                <th scope="col">Categoría</th>
                <th scope="col" className="products__number">
                  Precio
                </th>
                <th scope="col" className="products__number">
                  Stock
                </th>
                <th scope="col">Estado</th>
                <th scope="col">
                  <span className="visually-hidden">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {found.content.map((product) => (
                <tr key={product.id} className={product.critical ? 'products__row--critical' : undefined}>
                  <td data-label="Código">{product.code}</td>
                  <td data-label="Nombre">{product.name}</td>
                  <td data-label="Categoría">{product.categoryName}</td>
                  <td data-label="Precio" className="products__number">
                    {formatMoney(product.salePrice)}
                  </td>
                  <td data-label="Stock" className="products__number">
                    {formatQuantity(product.stock)}
                  </td>
                  <td data-label="Estado">
                    {product.critical ? (
                      <span className="badge">
                        <span aria-hidden="true">⚠ </span>
                        Stock crítico
                      </span>
                    ) : (
                      <span className="status">Stock normal</span>
                    )}
                  </td>
                  <td className="products__actions">
                    <button
                      className="button button--secondary"
                      type="button"
                      aria-label={`Editar ${product.name}`}
                      onClick={() => {
                        setSaved(undefined)
                        setForm({ kind: 'edit', product })
                      }}
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {found && (found.totalPages > 1 || found.page > 0) ? (
        <nav className="products__pagination" aria-label="Páginas de productos">
          <button
            className="button button--secondary"
            type="button"
            disabled={found.page === 0 || loading}
            onClick={() => setPage(found.page - 1)}
          >
            Anterior
          </button>
          <span>
            Página {found.page + 1} de {Math.max(found.totalPages, found.page + 1)}
          </span>
          <button
            className="button button--secondary"
            type="button"
            disabled={found.page + 1 >= found.totalPages || loading}
            onClick={() => setPage(found.page + 1)}
          >
            Siguiente
          </button>
        </nav>
      ) : null}
    </section>
  )
}
