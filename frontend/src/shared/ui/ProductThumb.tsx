import { apiResourceUrl } from '@shared/api/http'
import './ProductThumb.css'

interface ThumbProduct {
  name: string
  /** Ruta versionada que devuelve la API; sin imagen se muestra la inicial del nombre. */
  imageUrl?: string | null
}

/** Foto del producto o, sin imagen, la inicial de su nombre. La usan Productos y el inicio (ADR 0005). */
export function ProductThumb({ product, size = 'md' }: { product: ThumbProduct; size?: 'sm' | 'md' | 'lg' }) {
  return (
    <div className={`product-thumb product-thumb--${size}`}>
      {product.imageUrl ? (
        <img src={apiResourceUrl(product.imageUrl)} alt="" loading="lazy" />
      ) : (
        <span aria-hidden="true">{product.name.trim().charAt(0).toUpperCase()}</span>
      )}
    </div>
  )
}
