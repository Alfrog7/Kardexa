import { ComingSoon } from '@shared/ui/ComingSoon'

export function PurchasesPage() {
  return (
    <ComingSoon
      title="Compras"
      description="Registra proveedores y compras: cada compra suma stock y actualiza el costo referencial."
      phase={5}
      stories="HU-11, HU-12, HU-13"
    />
  )
}
