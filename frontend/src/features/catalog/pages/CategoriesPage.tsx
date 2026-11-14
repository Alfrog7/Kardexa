import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { useCanManage } from '@shared/auth/session'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import { createCategory, listCategories, updateCategory, type Category } from '../api/categories'
import { ActionButton } from '@shared/ui/ActionButton'
import { Icon } from '@shared/ui/Icon'
import { CategoryForm } from '../components/CategoryForm'
import './CategoriesPage.css'

/** La modal está cerrada, crea una categoría o edita la indicada. */
type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; category: Category }

function sortByName(categories: Category[]): Category[] {
  return [...categories].sort((a, b) => a.name.localeCompare(b.name, 'es'))
}

function productsLabel(count: number): string {
  return count === 1 ? '1 producto' : `${count} productos`
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Gestión de categorías (HU-05): listar, crear, renombrar, desactivar y reactivar. */
export function CategoriesPage() {
  const canManage = useCanManage()
  const [categories, setCategories] = useState<Category[]>()
  const [loadError, setLoadError] = useState<string>()
  const [actionError, setActionError] = useState<string>()
  const [form, setForm] = useState<FormMode>({ kind: 'closed' })
  const [togglingId, setTogglingId] = useState<string>()
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  useEffect(() => {
    let current = true
    listCategories()
      .then((loaded) => {
        if (current) setCategories(sortByName(loaded))
      })
      .catch((error: unknown) => {
        if (current) setLoadError(messageOf(error))
      })
    return () => {
      current = false
    }
  }, [loadAttempt])

  function retryLoad() {
    setLoadError(undefined)
    setLoadAttempt((attempt) => attempt + 1)
  }

  function replace(updated: Category) {
    setCategories((current) => sortByName((current ?? []).map((item) => (item.id === updated.id ? updated : item))))
  }

  async function handleSubmit(name: string) {
    if (form.kind === 'edit') {
      const productCount = form.category.productCount
      replace({ ...(await updateCategory(form.category.id, name, form.category.active)), productCount })
    } else {
      const created = await createCategory(name)
      setCategories((current) => sortByName([...(current ?? []), { ...created, productCount: 0 }]))
    }
    setForm({ kind: 'closed' })
    setNotice(`Categoría ${name.trim()} guardada.`)
  }

  async function handleToggle(category: Category) {
    setActionError(undefined)
    setTogglingId(category.id)
    try {
      replace({ ...(await updateCategory(category.id, category.name, !category.active)), productCount: category.productCount })
    } catch (error) {
      setActionError(messageOf(error))
    } finally {
      setTogglingId(undefined)
    }
  }

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <div>
          <h1>Categorías</h1>
          <p className="page__subtitle">Agrupan tus productos para buscarlos y filtrarlos rápido.</p>
        </div>
        {canManage ? (
          <button className="button" type="button" onClick={() => setForm({ kind: 'create' })}>
            <Icon name="plus" size={18} />
            Nueva categoría
          </button>
        ) : null}
      </header>

      {form.kind !== 'closed' ? (
        <Dialog
          title={form.kind === 'edit' ? `Editar ${form.category.name}` : 'Nueva categoría'}
          onClose={() => setForm({ kind: 'closed' })}
        >
          <CategoryForm category={form.kind === 'edit' ? form.category : undefined} onSubmit={handleSubmit} />
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}

      {actionError ? (
        <p className="alert" role="alert">
          {actionError}
        </p>
      ) : null}

      {loadError ? (
        <div className="alert" role="alert">
          <p>{loadError}</p>
          <button className="button button--secondary" type="button" onClick={retryLoad}>
            Reintentar
          </button>
        </div>
      ) : categories === undefined ? (
        <p className="muted" role="status">
          Cargando categorías…
        </p>
      ) : categories.length === 0 ? (
        <div className="category-empty card">
          <p>Aún no tienes categorías. Agrupan tus productos, por ejemplo «Bebidas» o «Abarrotes».</p>
          {canManage ? (
            <button className="button" type="button" onClick={() => setForm({ kind: 'create' })}>
              Crear la primera categoría
            </button>
          ) : null}
        </div>
      ) : (
        <>
          <dl className="category-summary">
            <div className="card">
              <dt>Categorías</dt>
              <dd>{categories.length}</dd>
            </div>
            <div className="card">
              <dt>Activas</dt>
              <dd>{categories.filter((category) => category.active).length}</dd>
            </div>
            <div className="card">
              <dt>Productos</dt>
              <dd>{categories.reduce((sum, category) => sum + (category.productCount ?? 0), 0)}</dd>
            </div>
          </dl>
          <ul className="category-list card" aria-label="Categorías">
            {categories.map((category) => (
              <li key={category.id} className="category-list__item">
                <span className="category-list__name">{category.name}</span>
                <span className={category.productCount ? 'category-list__count' : 'category-list__count muted'}>
                  {productsLabel(category.productCount ?? 0)}
                </span>
                <label className="switch">
                  <input
                    type="checkbox"
                    role="switch"
                    aria-label={`${category.name} activa`}
                    checked={category.active}
                    disabled={!canManage || togglingId === category.id}
                    onChange={() => void handleToggle(category)}
                  />
                  <span aria-hidden="true">{category.active ? 'Activa' : 'Desactivada'}</span>
                </label>
                {canManage ? (
                  <ActionButton icon="pencil" label={`Editar ${category.name}`} onClick={() => setForm({ kind: 'edit', category })} />
                ) : null}
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
