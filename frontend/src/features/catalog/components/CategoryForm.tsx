import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import type { Category } from '../api/categories'

/** Límite de `Category.NAME_MAX_LENGTH` en la API. */
const NAME_MAX_LENGTH = 60

interface CategoryFormProps {
  /** Categoría que se edita; sin ella, el formulario crea una nueva. */
  category?: Category
  onSubmit: (name: string) => Promise<void>
}

/** Alta y edición. Ante un error conserva lo escrito y muestra el mensaje de la API. */
export function CategoryForm({ category, onSubmit }: CategoryFormProps) {
  const [name, setName] = useState(category?.name ?? '')
  const [nameError, setNameError] = useState<string>()
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setNameError(undefined)
    setFormError(undefined)
    try {
      await onSubmit(name)
    } catch (error) {
      showError(error)
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
    <form className="form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="category-name">Nombre</label>
        <input
          data-autofocus
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
      <button className="button" type="submit" disabled={saving}>
        {category ? 'Guardar cambios' : 'Crear categoría'}
      </button>
    </form>
  )
}
