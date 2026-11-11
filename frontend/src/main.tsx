import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { appRoutes } from '@app/router'
import { SessionProvider } from '@shared/auth/SessionProvider'
import '@fontsource-variable/inter'
import '@fontsource/young-serif'
import '@shared/styles/global.css'

const router = createBrowserRouter(appRoutes)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <RouterProvider router={router} />
    </SessionProvider>
  </StrictMode>,
)
