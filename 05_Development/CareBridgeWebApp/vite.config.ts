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
  build: {
    rollupOptions: {
      output: {
        // Everything used to land in one 2.1 MB chunk, so signing in meant
        // downloading the Zego video SDK and the TipTap editor before the login
        // form could render. On 11/08/2026 GitLab Pages dropped to ~20 KB/s and
        // that single chunk stopped arriving at all - the browser gave up
        // mid-download and left a white screen. Splitting it means the entry
        // chunk is small, the heavy libraries fetch in parallel on their own
        // connections, and a stall in one of them no longer blocks boot.
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('@zegocloud')) return 'zego'
          if (id.includes('@tiptap') || id.includes('prosemirror')) return 'editor'
          if (id.includes('firebase') || id.includes('@firebase')) return 'firebase'
          if (id.includes('react-hook-form') || id.includes('@hookform') || id.includes('zod')) return 'forms'
          if (id.includes('@tanstack') || id.includes('axios') || id.includes('zustand') || id.includes('dayjs')) return 'data'
          if (id.includes('react-router')) return 'router'
          if (id.includes('/react-dom/') || id.includes('/react/') || id.includes('scheduler')) return 'react'
          return 'vendor'
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    // Playwright owns e2e/*.spec.ts; Vitest owns colocated unit/component tests.
    exclude: ['**/node_modules/**', '**/dist/**', '**/e2e/**'],
  },
})
