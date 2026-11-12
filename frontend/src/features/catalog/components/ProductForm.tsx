import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import type { Category } from '../api/categories'
import type { Product, ProductRequest, Unit } from '../api/products'

const UNITS: { value: Unit; label: string }[] = [
  { value: 'UNIT', label: 'Unidad' },
  { value: 'KG', label: 'Kilogramo' },
  { value: 'LITER', label: 'Litro' },
  { value: 'BOX', label: 'Caja' },
]

/** Mismas reglas que `ProductRequest` en la API. */
const MONEY_PATTERN = /^\d{1,10}(\.\d{1,2})?$/
const QUANTITY_PATTERN = /^\d{1,9}(\.\d{1,3})?$/

type FieldName = 'code' | 'name' | 'categoryId' | 'unit' | 'salePrice' | 'minStock'
type FieldErrors = Partial<Record<FieldName, string>>

/** Reglas de negocio que la API rechaza sin indicar campo: RN-10 (código repetido) y RF-06 (categoría). */
const FIELD_BY_RULE: Record<string, FieldName> = { 'RN-10': 'code', 'RF-06': 'categoryId' }

function validate(values: ProductRequest, editing: boolean): FieldErrors {
  const errors: FieldErrors = {}
  // Al crear, un código vacío lo asigna la API (P-0001, P-0002…).
  if (editing && !values.code) errors.code = 'El código es obligatorio.'
  if (values.name.length < 2) errors.name = 'Debe tener entre 2 y 120 caracteres.'
  if (!values.categoryId) errors.categoryId = 'La categoría es obligatoria.'
  if (!MONEY_PATTERN.test(values.salePrice)) errors.salePrice = 'Monto con hasta 2 decimales, por ejemplo 3.50.'
  if (!QUANTITY_PATTERN.test(values.minStock)) errors.minStock = 'Cantidad con hasta 3 decimales, por ejemplo 2.5.'
  return errors
}

interface ProductFormProps {
  /** Producto que se edita; sin él, el formulario crea uno nuevo. */
  product?: Product
  categories: Category[]
  onSubmit: (request: ProductRequest) => Promise<void>
}

/** Alta y edición. Se monta al abrir el drawer, así que cada apertura empieza limpia. */
export function ProductForm({ product, categories, onSubmit }: ProductFormProps) {
  const [code, setCode] = useState(product?.code ?? '')
  const [name, setName] = useState(product?.name ?? '')
  const [categoryId, setCategoryId] = useState(product?.categoryId ?? '')
  const [unit, setUnit] = useState<Unit>(product?.unit ?? 'UNIT')
  const [salePrice, setSalePrice] = useState(product?.salePrice ?? '')
  const [minStock, setMinStock] = useState(product?.minStock ?? '')
  const [active, setActive] = useState(product?.active ?? true)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  // Solo se ofrecen categorías activas; al editar se conserva la actual aunque esté desactivada.
  const options = categories.filter((category) => category.active || category.id === product?.categoryId)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const request: ProductRequest = {
      code: code.trim(),
      name: name.trim(),
      categoryId,
      unit,
      salePrice: salePrice.trim(),
      minStock: minStock.trim(),
      ...(product ? { active } : {}),
    }
    const invalid = validate(request, Boolean(product))
    setErrors(invalid)
    setFormError(undefined)
    if (Object.keys(invalid).length > 0) return

    setSaving(true)
    try {
      await onSubmit(request)
    } catch (error) {
      showError(error)
      setSaving(false)
    }
  }

  function showError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setFormError('No pudimos guardar el producto. Inténtalo otra vez.')
      return
    }
    const fromApi: FieldErrors = {}
    for (const { field, message } of error.fieldErrors) {
      fromApi[field as FieldName] ??= message
    }
    const ruleField = error.rule ? FIELD_BY_RULE[error.rule] : undefined
    if (ruleField) fromApi[ruleField] = error.message

    setErrors(fromApi)
    if (Object.keys(fromApi).length === 0) setFormError(error.message)
  }

  function describe(field: FieldName) {
    return errors[field]
      ? { 'aria-invalid': true, 'aria-describedby': `product-${field}-error` }
      : {}
  }

  function errorOf(field: FieldName) {
    return errors[field] ? (
      <p id={`product-${field}-error`} className="field__error">
        {errors[field]}
      </p>
    ) : null
  }

  return (
    <form className="form" noValidate onSubmit={handleSubmit}>
      <div className="product-form__grid">
        <div className="field">
          <label htmlFor="product-code">Código</label>
          <input
            data-autofocus={product ? true : undefined}
            id="product-code"
            value={code}
            maxLength={40}
            placeholder={product ? undefined : 'Automático'}
            aria-describedby={errors.code ? 'product-code-error' : product ? undefined : 'product-code-hint'}
            aria-invalid={errors.code ? true : undefined}
            onChange={(event) => setCode(event.target.value)}
          />
          {product ? null : (
            <p id="product-code-hint" className="muted">
              Opcional: déjalo vacío y se asigna el siguiente (P-0001, P-0002…).
            </p>
          )}
          {errorOf('code')}
        </div>
        <div className="field">
          <label htmlFor="product-name">Nombre</label>
          <input
            data-autofocus={product ? undefined : true}
            id="product-name"
            value={name}
            maxLength={120}
            onChange={(event) => setName(event.target.value)}
            {...describe('name')}
          />
          {errorOf('name')}
        </div>
        <div className="field">
          <label htmlFor="product-category">Categoría</label>
          <select
            id="product-category"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            {...describe('categoryId')}
          >
            <option value="">Elige una categoría</option>
            {options.map((category) => (
              <option key={category.id} value={category.id}>
                {category.active ? category.name : `${category.name} (desactivada)`}
              </option>
            ))}
          </select>
          {options.length === 0 ? <p className="muted">Aún no hay categorías activas. Créalas en Categorías.</p> : null}
          {errorOf('categoryId')}
        </div>
        <div className="field">
          <label htmlFor="product-unit">Unidad</label>
          <select
            id="product-unit"
            value={unit}
            onChange={(event) => setUnit(event.target.value as Unit)}
            {...describe('unit')}
          >
            {UNITS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
          {errorOf('unit')}
        </div>
        <div className="field">
          <label htmlFor="product-price">Precio de venta (S/)</label>
          <input
            id="product-price"
            inputMode="decimal"
            value={salePrice}
            onChange={(event) => setSalePrice(event.target.value)}
            {...describe('salePrice')}
          />
          {errorOf('salePrice')}
        </div>
        <div className="field">
          <label htmlFor="product-min-stock">Stock mínimo</label>
          <input
            id="product-min-stock"
            inputMode="decimal"
            value={minStock}
            onChange={(event) => setMinStock(event.target.value)}
            {...describe('minStock')}
          />
          {errorOf('minStock')}
        </div>
      </div>

      {product ? (
        <label className="switch">
          <input type="checkbox" role="switch" checked={active} onChange={(event) => setActive(event.target.checked)} />
          Producto activo
          {active ? null : <span className="muted">Seguirá en la lista como inhabilitado.</span>}
        </label>
      ) : null}

      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}

      <button className="button" type="submit" disabled={saving}>
        {product ? 'Guardar cambios' : 'Crear producto'}
      </button>
    </form>
  )
}
