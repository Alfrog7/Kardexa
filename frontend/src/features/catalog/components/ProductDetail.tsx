import { useRef, useState, type ChangeEvent } from 'react'
import { Link } from 'react-router'
import { routes } from '@app/navigation'
import { ApiError } from '@shared/api/http'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import { Icon } from '@shared/ui/Icon'
import { removeProductImage, uploadProductImage, type Product } from '../api/products'
import { unitLabel } from './productFormat'
import { ProductStatus, ProductThumb } from './ProductViews'

const MAX_IMAGE_BYTES = 2 * 1024 * 1024
const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']

interface ProductDetailProps {
  product: Product
  canManage: boolean
  onEdit: () => void
  /** La imagen cambió: la página actualiza su lista con el producto devuelto. */
  onChanged: (product: Product) => void
}

/** Ficha del producto: imagen, datos, estado y accesos a editar y al historial. */
export function ProductDetail({ product, canManage, onEdit, onChanged }: ProductDetailProps) {
  const input = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function run(action: () => Promise<Product>) {
    setError(undefined)
    setBusy(true)
    try {
      onChanged(await action())
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : 'No pudimos guardar la imagen. Inténtalo otra vez.')
    } finally {
      setBusy(false)
    }
  }

  function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!IMAGE_TYPES.includes(file.type)) {
      setError('La imagen debe ser PNG, JPG o WebP.')
      return
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setError('La imagen debe pesar como máximo 2 MB.')
      return
    }
    void run(() => uploadProductImage(product.id, file))
  }

  return (
    <div className="product-detail">
      <div className="product-detail__media">
        <ProductThumb product={product} size="lg" />
        {canManage ? (
          <div className="product-detail__image-actions">
            <input
              ref={input}
              className="visually-hidden"
              type="file"
              accept={IMAGE_TYPES.join(',')}
              aria-label="Elegir imagen del producto"
              onChange={handleFile}
            />
            <button className="button button--secondary" type="button" disabled={busy} onClick={() => input.current?.click()}>
              <Icon name="image" size={18} />
              {product.imageUrl ? 'Cambiar imagen' : 'Agregar imagen'}
            </button>
            {product.imageUrl ? (
              <button
                className="link-button"
                type="button"
                disabled={busy}
                onClick={() => void run(() => removeProductImage(product.id))}
              >
                Quitar imagen
              </button>
            ) : null}
            <p className="muted">PNG, JPG o WebP de hasta 2 MB.</p>
          </div>
        ) : null}
        {error ? (
          <p className="alert" role="alert">
            {error}
          </p>
        ) : null}
      </div>

      <dl className="product-detail__data">
        <div>
          <dt>Código</dt>
          <dd>{product.code}</dd>
        </div>
        <div>
          <dt>Categoría</dt>
          <dd>{product.categoryName}</dd>
        </div>
        <div>
          <dt>Precio de venta</dt>
          <dd>{formatMoney(product.salePrice)}</dd>
        </div>
        <div>
          <dt>Stock actual</dt>
          <dd>
            {formatQuantity(product.stock)} {unitLabel(product)}
          </dd>
        </div>
        <div>
          <dt>Stock mínimo</dt>
          <dd>{formatQuantity(product.minStock)}</dd>
        </div>
        {canManage ? (
          <div>
            <dt>Último costo</dt>
            <dd>{formatMoney(product.referenceCost)}</dd>
          </div>
        ) : null}
        <div>
          <dt>Estado</dt>
          <dd>
            <ProductStatus product={product} />
          </dd>
        </div>
      </dl>

      <div className="product-detail__actions">
        <Link className="button button--secondary" to={`${routes.inventory}?producto=${product.id}`}>
          <Icon name="history" size={18} />
          Ver historial de stock
        </Link>
        {canManage ? (
          <button className="button" type="button" onClick={onEdit}>
            <Icon name="pencil" size={18} />
            Editar
          </button>
        ) : null}
      </div>
    </div>
  )
}
