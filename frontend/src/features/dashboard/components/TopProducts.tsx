import { routes } from '@app/navigation'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import { ActionLink, Actions } from '@shared/ui/ActionButton'
import { ProductThumb } from '@shared/ui/ProductThumb'
import type { TopProduct } from '../api/dashboard'

/** Los productos que más salen en el rango (RF-23), con acceso a su kárdex. */
export function TopProducts({ products }: { products: TopProduct[] }) {
  return (
    <section className="home__panel" aria-labelledby="home-top-title">
      <h3 id="home-top-title">Más vendidos</h3>
      {products.length === 0 ? (
        <p className="home__empty muted">Sin ventas en este rango.</p>
      ) : (
        <table className="data-table home__table" aria-label="Más vendidos">
          <thead>
            <tr>
              <th scope="col">#</th>
              <th scope="col">Producto</th>
              <th scope="col" className="data-table__number">
                Cantidad
              </th>
              <th scope="col" className="data-table__number">
                Monto
              </th>
              <th scope="col">
                <span className="visually-hidden">Acciones</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {products.map((product, index) => (
              <tr key={product.productId}>
                <td data-label="Puesto" className="home__rank">
                  {index + 1}
                </td>
                <td data-label="Producto">
                  <span className="home__product">
                    <ProductThumb product={{ name: product.productName, imageUrl: product.imageUrl }} size="sm" />
                    <span>
                      <span className="home__product-name">{product.productName}</span>
                      <span className="home__product-meta muted">{product.productCode}</span>
                    </span>
                  </span>
                </td>
                <td data-label="Cantidad" className="data-table__number">
                  {formatQuantity(product.quantity)}
                </td>
                <td data-label="Monto" className="data-table__number">
                  {formatMoney(product.amount)}
                </td>
                <td data-label="Acciones">
                  <Actions label={`Acciones de ${product.productName}`}>
                    <ActionLink icon="history" label={`Ver kárdex de ${product.productName}`} to={`${routes.inventory}?producto=${product.productId}`} />
                  </Actions>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
