import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    // Playwright owns e2e/*.spec.ts; Vitest owns colocated unit/component tests.
    exclude: ['**/node_modules/**', '**/dist/**', '**/e2e/**'],
  },
})
