import type { RouteObject } from 'react-router'
import { LoginPage, SetupPage } from '@features/auth'
import { CategoriesPage, ProductsPage } from '@features/catalog'
import { DashboardPage } from '@features/dashboard'
import { InventoryPage } from '@features/inventory'
import { PurchasesPage, SuppliersPage } from '@features/purchasing'
import { UsersPage } from '@features/users'
import { SalesPage } from '@features/sales'
import { ReceiptsPage } from '@features/receipts'
import { ShiftPage } from '@features/shifts'
import { SitePickerPage, SitesPage } from '@features/sites'
import { PanelLayout } from './layout/PanelLayout'
import { NotFoundPage } from './NotFoundPage'
import { RequireAdmin } from './RequireAdmin'
import { RequireSession } from './RequireSession'
import { RequireSite } from './RequireSite'
import { routes } from './navigation'

/** Árbol de rutas. `app` compone las funcionalidades; ninguna funcionalidad conoce a otra (ADR 0005). */
export const appRoutes: RouteObject[] = [
  { path: routes.login, element: <LoginPage /> },
  { path: routes.setup, element: <SetupPage /> },
  {
    element: <RequireSession />,
    children: [
      {
        element: <RequireAdmin />,
        children: [{ path: routes.pickSite, element: <SitePickerPage /> }],
      },
      {
        element: <RequireSite />,
        children: [
          {
            path: routes.dashboard,
            element: <PanelLayout />,
            children: [
              { index: true, element: <DashboardPage /> },
              { path: routes.sales, element: <SalesPage /> },
              { path: routes.products, element: <ProductsPage /> },
              { path: routes.categories, element: <CategoriesPage /> },
              { path: routes.inventory, element: <InventoryPage /> },
              { path: routes.shift, element: <ShiftPage /> },
              { path: routes.receipts, element: <ReceiptsPage /> },
              { path: routes.suppliers, element: <SuppliersPage /> },
              {
                element: <RequireAdmin />,
                children: [
                  { path: routes.purchases, element: <PurchasesPage /> },
                  { path: routes.sites, element: <SitesPage /> },
                  { path: routes.users, element: <UsersPage /> },
                ],
              },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
]
