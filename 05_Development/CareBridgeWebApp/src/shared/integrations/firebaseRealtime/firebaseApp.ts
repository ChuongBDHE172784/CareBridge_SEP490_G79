import { getApps, initializeApp, type FirebaseApp } from 'firebase/app';

// Web app config — see 05_Development/CareBridgeWebApp/.env.example for the exact
// VITE_FIREBASE_* variables required. Intentionally NOT the same value as the backend's
// service-account credentials (ADR-DCC-004) — this is the public web SDK config.
function readConfig() {
  const env = import.meta.env;
  return {
    apiKey: env.VITE_FIREBASE_API_KEY as string | undefined,
    authDomain: env.VITE_FIREBASE_AUTH_DOMAIN as string | undefined,
    databaseURL: env.VITE_FIREBASE_DATABASE_URL as string | undefined,
    projectId: env.VITE_FIREBASE_PROJECT_ID as string | undefined,
    storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET as string | undefined,
    messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID as string | undefined,
    appId: env.VITE_FIREBASE_APP_ID as string | undefined,
  };
}

export function isFirebaseConfigured(): boolean {
  const config = readConfig();
  return Boolean(config.apiKey && config.databaseURL && config.projectId && config.appId);
}

let cachedApp: FirebaseApp | null = null;

/** Throws if config is missing — callers must check {@link isFirebaseConfigured} first. */
export function getFirebaseWebApp(): FirebaseApp {
  if (cachedApp) return cachedApp;
  const existing = getApps();
  if (existing.length > 0) {
    cachedApp = existing[0];
    return cachedApp;
  }
  if (!isFirebaseConfigured()) {
    throw new Error('Firebase Web config is missing — set VITE_FIREBASE_* env vars');
  }
  cachedApp = initializeApp(readConfig() as Record<string, string>);
  return cachedApp;
}
