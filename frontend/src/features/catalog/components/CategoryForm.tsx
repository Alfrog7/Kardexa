import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import type { Category } from '../api/categories'

/** Límite de `Category.NAME_MAX_LENGTH` en la API. */
const NAME_MAX_LENGTH = 60

interface CategoryFormProps {
  /** Categoría que se edita; sin ella, el formulario crea una nueva. */
  category?: Category
  /** Lleva el foco al campo al montarse, para seguir con el teclado al entrar o salir de la edición. */
  focusOnMount: boolean
  onSubmit: (name: string) => Promise<void>
  onCancel: () => void
}

/**
 * Alta y edición. Ante un error conserva lo escrito y muestra el mensaje de la API.
 * La página le da una `key` por categoría, así que cada modo empieza con el estado limpio.
 */
export function CategoryForm({ category, focusOnMount, onSubmit, onCancel }: CategoryFormProps) {
  const [name, setName] = useState(category?.name ?? '')
  const [nameError, setNameError] = useState<string>()
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)
  const input = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (focusOnMount) {
      input.current?.focus()
    }
  }, [focusOnMount])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setNameError(undefined)
    setFormError(undefined)
    try {
      await onSubmit(name)
      setName('')
    } catch (error) {
      showError(error)
    } finally {
      setSaving(false)
    }
  }

  function showError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setFormError('No pudimos guardar la categoría. Inténtalo otra vez.')
      return
    }
    const fieldError = error.fieldErrors.find((item) => item.field === 'name')
    if (fieldError) {
      setNameError(fieldError.message)
    } else {
      setFormError(error.message)
    }
  }

  return (
    <form className="card category-form" onSubmit={handleSubmit}>
      <h2 className="category-form__title">{category ? `Editar ${category.name}` : 'Nueva categoría'}</h2>
      <div className="field">
        <label htmlFor="category-name">Nombre</label>
        <input
          ref={input}
          id="category-name"
          name="name"
          value={name}
          maxLength={NAME_MAX_LENGTH}
          required
          aria-invalid={nameError ? true : undefined}
          aria-describedby={nameError ? 'category-name-error' : undefined}
          onChange={(event) => setName(event.target.value)}
        />
        {nameError ? (
          <p id="category-name-error" className="field__error">
            {nameError}
          </p>
        ) : null}
      </div>
      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <div className="category-form__actions">
        <button className="button" type="submit" disabled={saving}>
          {category ? 'Guardar cambios' : 'Crear categoría'}
        </button>
        {category ? (
          <button className="button button--secondary" type="button" onClick={onCancel}>
            Cancelar
          </button>
        ) : null}
      </div>
    </form>
  )
}
