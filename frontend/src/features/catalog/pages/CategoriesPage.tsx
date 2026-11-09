import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { useCanManage } from '@shared/auth/session'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import { createCategory, listCategories, updateCategory, type Category } from '../api/categories'
import { CategoryForm } from '../components/CategoryForm'
import './CategoriesPage.css'

/** La modal está cerrada, crea una categoría o edita la indicada. */
type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; category: Category }

function sortByName(categories: Category[]): Category[] {
  return [...categories].sort((a, b) => a.name.localeCompare(b.name, 'es'))
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
      replace(await updateCategory(form.category.id, name, form.category.active))
    } else {
      const created = await createCategory(name)
      setCategories((current) => sortByName([...(current ?? []), created]))
    }
    setForm({ kind: 'closed' })
    setNotice(`Categoría ${name.trim()} guardada.`)
  }

  async function handleToggle(category: Category) {
    setActionError(undefined)
    setTogglingId(category.id)
    try {
      replace(await updateCategory(category.id, category.name, !category.active))
    } catch (error) {
      setActionError(messageOf(error))
    } finally {
      setTogglingId(undefined)
    }
  }

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <h1>Categorías</h1>
        {canManage ? (
          <button className="button" type="button" onClick={() => setForm({ kind: 'create' })}>
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
        <p className="muted">Aún no tienes categorías. Crea la primera con «Nueva categoría».</p>
      ) : (
        <ul className="category-list card" aria-label="Categorías">
          {categories.map((category) => (
            <li key={category.id} className="category-list__item">
              {canManage ? (
                <button
                  className="link-button category-list__name"
                  type="button"
                  onClick={() => setForm({ kind: 'edit', category })}
                >
                  {category.name}
                </button>
              ) : (
                <span className="category-list__name">{category.name}</span>
              )}
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
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
