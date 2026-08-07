/**
 * Global Vitest setup.
 *
 * Node >= 22 ships an experimental built-in `localStorage` global. When Vitest installs
 * the jsdom environment, that built-in shadows jsdom's own Storage and exposes an object
 * whose `setItem`/`getItem` are undefined unless node is started with a valid
 * `--localstorage-file`. Anything using `zustand/middleware` `persist` (e.g. authStore)
 * then blows up with "storage.setItem is not a function".
 *
 * Re-bind the global to the jsdom Storage (or an in-memory shim) so persisted stores work.
 */
const hasWorkingStorage = (candidate: unknown): candidate is Storage =>
  typeof (candidate as Storage | undefined)?.setItem === 'function';

function createMemoryStorage(): Storage {
  const map = new Map<string, string>();
  return {
    get length() {
      return map.size;
    },
    clear: () => map.clear(),
    getItem: (key: string) => (map.has(key) ? map.get(key)! : null),
    key: (index: number) => Array.from(map.keys())[index] ?? null,
    removeItem: (key: string) => void map.delete(key),
    setItem: (key: string, value: string) => void map.set(key, String(value)),
  } as Storage;
}

function ensureStorage(name: 'localStorage' | 'sessionStorage') {
  const fromWindow = typeof window !== 'undefined' ? window[name] : undefined;
  if (hasWorkingStorage(globalThis[name])) return;

  const replacement = hasWorkingStorage(fromWindow) ? fromWindow : createMemoryStorage();
  Object.defineProperty(globalThis, name, {
    configurable: true,
    writable: true,
    value: replacement,
  });
  if (typeof window !== 'undefined' && !hasWorkingStorage(window[name])) {
    Object.defineProperty(window, name, {
      configurable: true,
      writable: true,
      value: replacement,
    });
  }
}

ensureStorage('localStorage');
ensureStorage('sessionStorage');
