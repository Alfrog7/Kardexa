import { useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { createCategory, listCategories, updateCategory, type Category } from '../api/categories'
import { CategoryForm } from '../components/CategoryForm'
import './CategoriesPage.css'

function sortByName(categories: Category[]): Category[] {
  return [...categories].sort((a, b) => a.name.localeCompare(b.name, 'es'))
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Gestión de categorías (HU-05): listar, crear, renombrar, desactivar y reactivar. */
export function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>()
  const [loadError, setLoadError] = useState<string>()
  const [actionError, setActionError] = useState<string>()
  const [editingId, setEditingId] = useState<string>()
  const [togglingId, setTogglingId] = useState<string>()
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [focusForm, setFocusForm] = useState(false)

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

  function startEditing(id: string) {
    setActionError(undefined)
    setFocusForm(true)
    setEditingId(id)
  }

  const editing = categories?.find((category) => category.id === editingId)

  function replace(updated: Category) {
    setCategories((current) => sortByName((current ?? []).map((item) => (item.id === updated.id ? updated : item))))
  }

  async function handleSubmit(name: string) {
    setActionError(undefined)
    if (editing) {
      replace(await updateCategory(editing.id, name, editing.active))
      setEditingId(undefined)
    } else {
      const created = await createCategory(name)
      setCategories((current) => sortByName([...(current ?? []), created]))
    }
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
      <header className="page__header">
        <h1>Categorías</h1>
      </header>

      <CategoryForm
        key={editingId ?? 'new'}
        category={editing}
        focusOnMount={focusForm}
        onSubmit={handleSubmit}
        onCancel={() => setEditingId(undefined)}
      />

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
        <p className="muted">Aún no tienes categorías. Crea la primera con el formulario.</p>
      ) : (
        <ul className="category-list card" aria-label="Categorías">
          {categories.map((category) => (
            <li key={category.id} className="category-list__item">
              <span className="category-list__name">{category.name}</span>
              <span className={category.active ? 'status status--active' : 'status'}>
                {category.active ? 'Activa' : 'Desactivada'}
              </span>
              <span className="category-list__actions">
                <button
                  className="button button--secondary"
                  type="button"
                  aria-label={`Editar ${category.name}`}
                  onClick={() => startEditing(category.id)}
                >
                  Editar
                </button>
                <button
                  className="button button--secondary"
                  type="button"
                  aria-label={`${category.active ? 'Desactivar' : 'Reactivar'} ${category.name}`}
                  disabled={togglingId === category.id}
                  onClick={() => void handleToggle(category)}
                >
                  {category.active ? 'Desactivar' : 'Reactivar'}
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
