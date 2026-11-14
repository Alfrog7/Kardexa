import { routes } from '@app/navigation'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import { ActionButton, ActionLink, Actions } from '@shared/ui/ActionButton'
import { ProductThumb } from '@shared/ui/ProductThumb'
import type { ViewMode } from '@shared/ui/useViewMode'
import type { Product } from '../api/products'
import { unitLabel } from './productFormat'

export interface ProductHandlers {
  canManage: boolean
  onView: (product: Product) => void
  onEdit: (product: Product) => void
  onDelete: (product: Product) => void
}

function rowClass(product: Product): string | undefined {
  if (!product.active) return 'products__row--inactive'
  return product.critical ? 'products__row--critical' : undefined
}

/** Estado del producto con texto (no solo color). */
export function ProductStatus({ product }: { product: Product }) {
  if (!product.active) return <span className="status status--disabled">Inhabilitado</span>
  if (product.critical) {
    return (
      <span className="badge">
        <span aria-hidden="true">⚠ </span>
        Stock crítico
      </span>
    )
  }
  return <span className="status">Stock normal</span>
}

/** Ver, editar, ajustar stock y eliminar: las mismas acciones en las tres vistas. */
function ProductActions({ product, handlers }: { product: Product; handlers: ProductHandlers }) {
  return (
    <Actions label={`Acciones de ${product.name}`}>
      <ActionButton icon="eye" label={`Ver ${product.name}`} onClick={() => handlers.onView(product)} />
      {handlers.canManage ? (
        <>
          <ActionButton icon="pencil" label={`Editar ${product.name}`} onClick={() => handlers.onEdit(product)} />
          {product.active ? (
            <ActionLink
              icon="sliders"
              label={`Ajustar stock de ${product.name}`}
              to={`${routes.inventory}?producto=${product.id}&ajustar=1`}
            />
          ) : null}
          <ActionButton
            icon="trash"
            tone="danger"
            label={`Eliminar ${product.name}`}
            onClick={() => handlers.onDelete(product)}
          />
        </>
      ) : null}
    </Actions>
  )
}

interface ProductViewProps {
  mode: ViewMode
  products: Product[]
  handlers: ProductHandlers
}

export function ProductView({ mode, products, handlers }: ProductViewProps) {
  if (mode === 'cards') {
    return (
      <ul className="product-cards" aria-label="Productos">
        {products.map((product) => (
          <li key={product.id} className={`product-card card ${rowClass(product) ?? ''}`}>
            <div className="product-card__media">
              <ProductThumb product={product} size="lg" />
            </div>
            <div className="product-card__body">
              <span className="product-card__category">{product.categoryName}</span>
              <h2 className="product-card__name">{product.name}</h2>
              <p className="product-card__code muted">{product.code}</p>
              <dl className="product-card__figures">
                <div>
                  <dt>Precio</dt>
                  <dd>{formatMoney(product.salePrice)}</dd>
                </div>
                <div>
                  <dt>Stock</dt>
                  <dd>
                    {formatQuantity(product.stock)} {unitLabel(product)}
                  </dd>
                </div>
              </dl>
              <ProductStatus product={product} />
            </div>
            <div className="product-card__actions">
              <ProductActions product={product} handlers={handlers} />
            </div>
          </li>
        ))}
      </ul>
    )
  }

  if (mode === 'list') {
    return (
      <ul className="product-list card" aria-label="Productos">
        {products.map((product) => (
          <li key={product.id} className={`product-list__item ${rowClass(product) ?? ''}`}>
            <ProductThumb product={product} size="sm" />
            <div className="product-list__main">
              <strong>{product.name}</strong>
              <span className="muted">
                {product.code} · {product.categoryName}
              </span>
            </div>
            <div className="product-list__figures">
              <span>{formatMoney(product.salePrice)}</span>
              <span className="muted">
                Stock {formatQuantity(product.stock)} {unitLabel(product)}
              </span>
            </div>
            <ProductStatus product={product} />
            <ProductActions product={product} handlers={handlers} />
          </li>
        ))}
      </ul>
    )
  }

  return (
    <div className="card data-table-card">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Código</th>
            <th scope="col">Nombre</th>
            <th scope="col">Categoría</th>
            <th scope="col" className="data-table__number">
              Precio
            </th>
            <th scope="col" className="data-table__number">
              Stock
            </th>
            <th scope="col">Estado</th>
            <th scope="col">Acciones</th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id} className={rowClass(product)}>
              <td data-label="Código">{product.code}</td>
              <td data-label="Nombre">
                <span className="products__name">
                  <ProductThumb product={product} size="sm" />
                  {product.name}
                </span>
              </td>
              <td data-label="Categoría">{product.categoryName}</td>
              <td data-label="Precio" className="data-table__number">
                {formatMoney(product.salePrice)}
              </td>
              <td data-label="Stock" className="data-table__number">
                {formatQuantity(product.stock)}
              </td>
              <td data-label="Estado">
                <ProductStatus product={product} />
              </td>
              <td data-label="Acciones">
                <ProductActions product={product} handlers={handlers} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
