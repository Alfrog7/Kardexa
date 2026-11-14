import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError } from '@shared/api/http'
import { useCanManage } from '@shared/auth/session'
import { formatQuantity } from '@shared/lib/format'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import { useViewMode } from '@shared/ui/useViewMode'
import { ViewToggle } from '@shared/ui/ViewToggle'
import { listCategories, type Category } from '../api/categories'
import {
  createProduct,
  deleteProduct,
  registerInitialStock,
  searchProducts,
  updateProduct,
  type Product,
  type ProductPage,
  type ProductRequest,
} from '../api/products'
import { Icon } from '@shared/ui/Icon'
import { ProductDetail } from '../components/ProductDetail'
import { ProductForm } from '../components/ProductForm'
import { ProductView } from '../components/ProductViews'
import './ProductsPage.css'

const SEARCH_DELAY_MS = 300

interface SearchResult {
  /** Consulta que produjo este resultado: si no es la actual, hay otra en curso. */
  query: object
  page?: ProductPage
  error?: string
}

/** Panel lateral cerrado, creando, editando o mostrando la ficha de un producto. */
type FormMode =
  | { kind: 'closed' }
  | { kind: 'create' }
  | { kind: 'edit'; product: Product }
  | { kind: 'view'; product: Product }

/** Producto que se quiere eliminar; `error` explica por qué no se pudo (tiene historial). */
interface Removal {
  product: Product
  error?: string
  busy?: boolean
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Productos (HU-06, HU-07, HU-10): búsqueda, filtros, marca de stock crítico, alta y edición. */
export function ProductsPage() {
  const [text, setText] = useState('')
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [searchParams] = useSearchParams()
  // El inicio enlaza con `?critico=1` para abrir la lista ya filtrada por stock crítico.
  const [criticalOnly, setCriticalOnly] = useState(() => searchParams.get('critico') === '1')
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<SearchResult>()
  const [categories, setCategories] = useState<Category[]>([])
  const [categoriesError, setCategoriesError] = useState<string>()
  const canManage = useCanManage()
  const [form, setForm] = useState<FormMode>({ kind: 'closed' })
  const [notice, setNotice] = useState<string>()
  const [view, setView] = useViewMode('products', 'table')
  const [removal, setRemoval] = useState<Removal>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  const query = useMemo(
    // Un producto inhabilitado no requiere reposición: el filtro de stock crítico solo muestra activos.
    () => ({ q, categoryId, criticalOnly, includeInactive: !criticalOnly, page, attempt }),
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

  async function handleSubmit(request: ProductRequest, initialStock?: string) {
    let message = `Producto ${request.name} guardado.`
    if (form.kind === 'edit') {
      await updateProduct(form.product.id, request)
    } else {
      const created = await createProduct(request)
      message = `Producto ${created.name} creado con el código ${created.code}.`
      if (initialStock) {
        try {
          await registerInitialStock(created.id, initialStock)
          message += ` Stock inicial: ${formatQuantity(initialStock)}.`
        } catch (error) {
          message += ` No se registró el stock inicial: ${messageOf(error)} Agrégalo desde Inventario.`
        }
      }
    }
    setNotice(message)
    setAttempt((value) => value + 1)
    setForm({ kind: 'closed' })
  }

  /** Reemplaza un producto en la lista y en el panel abierto, sin volver a buscar. */
  function replaceProduct(updated: Product) {
    setResult((current) =>
      current?.page
        ? { ...current, page: { ...current.page, content: current.page.content.map((p) => (p.id === updated.id ? updated : p)) } }
        : current,
    )
    setForm((current) => (current.kind === 'view' && current.product.id === updated.id ? { kind: 'view', product: updated } : current))
  }

  async function confirmRemoval() {
    if (!removal) return
    setRemoval({ ...removal, busy: true, error: undefined })
    try {
      await deleteProduct(removal.product.id)
      setRemoval(undefined)
      setNotice(`Producto ${removal.product.name} eliminado.`)
      setAttempt((value) => value + 1)
    } catch (error) {
      setRemoval({ ...removal, busy: false, error: messageOf(error) })
    }
  }

  /** Alternativa cuando el producto tiene historial: inhabilitarlo para que no se venda más. */
  async function deactivateInstead() {
    if (!removal) return
    const { product } = removal
    setRemoval({ ...removal, busy: true })
    try {
      await updateProduct(product.id, {
        code: product.code,
        name: product.name,
        categoryId: product.categoryId,
        unit: product.unit,
        salePrice: product.salePrice,
        minStock: product.minStock,
        active: false,
      })
      setRemoval(undefined)
      setNotice(`Producto ${product.name} inhabilitado: ya no aparece en las ventas.`)
      setAttempt((value) => value + 1)
    } catch (error) {
      setRemoval({ ...removal, busy: false, error: messageOf(error) })
    }
  }

  const handlers = {
    canManage,
    onView: (product: Product) => setForm({ kind: 'view', product }),
    onEdit: (product: Product) => setForm({ kind: 'edit', product }),
    onDelete: (product: Product) => setRemoval({ product }),
  }

  const drawerTitle =
    form.kind === 'edit' ? `Editar ${form.product.name}` : form.kind === 'view' ? form.product.name : 'Nuevo producto'

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <div>
          <h1>Productos</h1>
          <p className="page__subtitle">Tu catálogo: precios, stock y estado de cada producto.</p>
        </div>
        {canManage ? (
          <button className="button" type="button" onClick={() => setForm({ kind: 'create' })}>
            <Icon name="plus" size={18} />
            Nuevo producto
          </button>
        ) : null}
      </header>

      {form.kind !== 'closed' ? (
        <Dialog variant="drawer" title={drawerTitle} onClose={() => setForm({ kind: 'closed' })}>
          {form.kind === 'view' ? (
            <ProductDetail
              product={form.product}
              canManage={canManage}
              onEdit={() => setForm({ kind: 'edit', product: form.product })}
              onChanged={replaceProduct}
            />
          ) : (
            <ProductForm
              product={form.kind === 'edit' ? form.product : undefined}
              categories={categories}
              onSubmit={handleSubmit}
            />
          )}
        </Dialog>
      ) : null}

      {removal ? (
        <Dialog title={`Eliminar ${removal.product.name}`} onClose={() => setRemoval(undefined)}>
          <div className="form">
            {removal.error ? (
              <>
                <p className="alert" role="alert">
                  {removal.error}
                </p>
                <p>Al inhabilitarlo deja de aparecer en las ventas, pero su historial se conserva y puedes reactivarlo cuando quieras.</p>
                <div className="dialog__actions">
                  <button className="button button--secondary" type="button" onClick={() => setRemoval(undefined)}>
                    Cancelar
                  </button>
                  {removal.product.active ? (
                    <button className="button" type="button" disabled={removal.busy} onClick={() => void deactivateInstead()}>
                      Inhabilitar producto
                    </button>
                  ) : null}
                </div>
              </>
            ) : (
              <>
                <p>
                  Se eliminará <strong>{removal.product.name}</strong> ({removal.product.code}) del catálogo. Esta acción no se
                  puede deshacer.
                </p>
                <p className="muted">Solo se puede eliminar un producto que nunca tuvo compras, ventas ni ajustes.</p>
                <div className="dialog__actions">
                  <button className="button button--secondary" type="button" onClick={() => setRemoval(undefined)}>
                    Cancelar
                  </button>
                  <button className="button button--danger" type="button" disabled={removal.busy} onClick={() => void confirmRemoval()}>
                    Eliminar producto
                  </button>
                </div>
              </>
            )}
          </div>
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}

      {categoriesError ? (
        <p className="alert" role="alert">
          {categoriesError}
        </p>
      ) : null}

      <div className="toolbar products__filters" role="search">
        <div className="field toolbar__grow">
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
        <ViewToggle value={view} onChange={setView} />
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
        <div className="empty-state card">
          <p>{q || categoryId || criticalOnly ? 'No hay productos con estos filtros.' : 'Aún no tienes productos.'}</p>
          {canManage && !q && !categoryId && !criticalOnly ? (
            <button className="button" type="button" onClick={() => setForm({ kind: 'create' })}>
              Crear el primer producto
            </button>
          ) : null}
        </div>
      ) : null}

      {found && found.content.length > 0 ? <ProductView mode={view} products={found.content} handlers={handlers} /> : null}

      {found && found.content.length > 0 ? (
        <p className="muted products__total">
          {found.totalElements === 1 ? '1 producto' : `${found.totalElements} productos`}
        </p>
      ) : null}

      {found && (found.totalPages > 1 || found.page > 0) ? (
        <nav className="products__pagination" aria-label="Páginas de productos">
          <button
            className="button button--secondary button--icon"
            type="button"
            aria-label="Anterior"
            title="Anterior"
            disabled={found.page === 0 || loading}
            onClick={() => setPage(found.page - 1)}
          >
            <Icon name="chevronLeft" />
          </button>
          <span>
            Página {found.page + 1} de {Math.max(found.totalPages, found.page + 1)}
          </span>
          <button
            className="button button--secondary button--icon"
            type="button"
            aria-label="Siguiente"
            title="Siguiente"
            disabled={found.page + 1 >= found.totalPages || loading}
            onClick={() => setPage(found.page + 1)}
          >
            <Icon name="chevronRight" />
          </button>
        </nav>
      ) : null}
    </section>
  )
}
