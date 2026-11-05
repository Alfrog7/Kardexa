import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

const src = (path: string) => fileURLToPath(new URL(`./src/${path}`, import.meta.url))

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    // Mismos alias que tsconfig.app.json → compilerOptions.paths (ADR 0005)
    alias: {
      '@app': src('app'),
      '@features': src('features'),
      '@shared': src('shared'),
    },
  },
  server: {
    port: 5173,
    // En desarrollo, /api/* se redirige al backend: sin CORS y con la misma URL que en producción.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
