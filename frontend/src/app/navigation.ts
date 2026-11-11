/** Rutas del panel. Las URL van en español porque las ve el usuario (ADR 0003). */
export const routes = {
  login: '/ingresar',
  setup: '/registro-inicial',
  dashboard: '/',
  sales: '/ventas',
  products: '/productos',
  categories: '/categorias',
  inventory: '/inventario',
  purchases: '/compras',
  suppliers: '/proveedores',
  users: '/usuarios',
} as const

export const navigation = [
  { to: routes.dashboard, label: 'Inicio' },
  { to: routes.sales, label: 'Ventas' },
  { to: routes.products, label: 'Productos' },
  { to: routes.categories, label: 'Categorías' },
  { to: routes.inventory, label: 'Inventario' },
  { to: routes.purchases, label: 'Compras', adminOnly: true },
  { to: routes.suppliers, label: 'Proveedores' },
  { to: routes.users, label: 'Usuarios', adminOnly: true },
] as const
