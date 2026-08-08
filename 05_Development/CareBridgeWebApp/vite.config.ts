import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Absolute, not './'. This is a single-page app served from the domain root,
  // and GitLab Pages answers every unknown path with the same index.html copied
  // to 404.html. With a relative base the browser resolves ./assets/... against
  // the current directory, so /login happens to work while /expert/register asks
  // for /expert/assets/... and gets 404 — every route two segments deep or more
  // fails to boot, and the page just hangs with no application error to see.
  base: '/',
  test: {
    environment: 'jsdom',
    // Playwright owns e2e/*.spec.ts; Vitest owns colocated unit/component tests.
    exclude: ['**/node_modules/**', '**/dist/**', '**/e2e/**'],
  },
})
