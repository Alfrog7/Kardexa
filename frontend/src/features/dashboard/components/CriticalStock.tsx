import { routes } from '@app/navigation'
import { useCanManage } from '@shared/auth/session'
import { formatQuantity } from '@shared/lib/format'
import { ActionLink, Actions } from '@shared/ui/ActionButton'
import { ProductThumb } from '@shared/ui/ProductThumb'
import type { CriticalProduct } from '../api/dashboard'

/** Productos activos en o bajo su mínimo (RF-24): ajustar el stock o, el `ADMIN`, registrar la compra. */
export function CriticalStock({ products }: { products: CriticalProduct[] }) {
  const admin = useCanManage()

  return (
    <section className="home__panel" aria-labelledby="home-critical-title">
      <h3 id="home-critical-title">Stock crítico</h3>
      {products.length === 0 ? (
        <p className="home__empty muted">Todo el stock está sobre el mínimo.</p>
      ) : (
        <ul className="home__critical" aria-label="Stock crítico">
          {products.map((product) => {
            const out = Number(product.stock) === 0
            return (
              <li key={product.productId} className="home__critical-item">
                <span className="home__product">
                  <ProductThumb product={{ name: product.productName, imageUrl: product.imageUrl }} size="sm" />
                  <span>
                    <span className="home__product-name">{product.productName}</span>
                    <span className="home__product-meta muted">{product.categoryName}</span>
                  </span>
                </span>
                <span className={out ? 'home__stock home__stock--out' : 'home__stock'}>
                  {out ? 'Agotado' : `Stock ${formatQuantity(product.stock)}`}
                  <span className="muted"> · mín. {formatQuantity(product.minStock)}</span>
                </span>
                <Actions label={`Acciones de ${product.productName}`}>
                  <ActionLink
                    icon="sliders"
                    label={`Ajustar stock de ${product.productName}`}
                    to={`${routes.inventory}?producto=${product.productId}&ajustar=1`}
                  />
                  {admin ? (
                    <ActionLink icon="plus" label={`Registrar compra de ${product.productName}`} to={`${routes.purchases}?producto=${product.productId}`} />
                  ) : null}
                </Actions>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
