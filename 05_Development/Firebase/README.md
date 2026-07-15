# Firebase setup — UC-144D Direct Consult Chat & Call

Firebase Realtime Database carries only the minimal realtime signal for direct chat/call
(ADR-DCC-002/DCC-004). PostgreSQL remains the sole durable system of record — nothing here
is optional storage. All three codebases (backend, mobile, web) already have working
adapters that compile and pass their test suites against stubs/mocks; the steps below are
what's needed to point them at a real Firebase project.

## 1. Create the Firebase project (if you don't have one)

1. https://console.firebase.google.com → Add project.
2. **Realtime Database** → Create Database (NOT Firestore — a different product with a
   different SDK). Start in **locked mode**; the rules below replace the default.
3. Note the database URL shown (e.g. `https://<project-id>-default-rtdb.<region>.firebasedatabase.app`).

## 2. Deploy the security rules (mandatory before enabling realtime in any environment)

```bash
cd 05_Development/Firebase
cp .firebaserc.example .firebaserc   # then edit: set "default" to your real project id
firebase login
firebase deploy --only database
```

`database.rules.json` enforces (ADR-DCC-004 §4):
- A user may only **read** `/user-conversation-events/{their own uid}` — no cross-user reads.
- **No client writes anywhere** — only the backend's Admin SDK (which bypasses Rules via
  service-account credentials) ever writes.

Verify locally before deploying to a shared project:

```bash
firebase emulators:start --only auth,database
```

## 3. Backend — reuses the existing FCM service account

No new credential needed if `FIREBASE_CREDENTIALS_BASE64` is already set (see
`05_Development/CareBridgeAPI/.env.example`). Add:

```
CAREBRIDGE_FIREBASE_REALTIME_ENABLED=true
FIREBASE_DATABASE_URL=https://<project-id>-default-rtdb.<region>.firebasedatabase.app
```

If you don't yet have a service account: Firebase Console → Project Settings → Service
Accounts → Generate New Private Key → base64-encode it (`base64 -i service-account.json |
tr -d '\n'`) → `FIREBASE_CREDENTIALS_BASE64`.

Leaving `CAREBRIDGE_FIREBASE_REALTIME_ENABLED=false` (the default) keeps the publisher as a
no-op/logging stub — the REST chat/call API works fully either way; only the realtime
"someone sent a message" nudge is skipped.

## 4. Mobile — needs `google-services.json` regenerated if Realtime Database was added later

`android/app/google-services.json` already exists (from prior FCM setup). If Realtime
Database was enabled on the project *after* that file was downloaded, re-download it from
Firebase Console → Project Settings → Your apps → Android app, and replace the file. No
`.env`/`--dart-define` changes needed — `firebase_core` reads this file directly.

## 5. Web — Firebase Web SDK config (public config, not a secret, but still per-project)

Firebase Console → Project Settings → General → Your apps → Web app → SDK setup and
configuration → Config. Fill in `05_Development/CareBridgeWebApp/.env.example` →
copy to `.env.local`:

```
VITE_FIREBASE_API_KEY=
VITE_FIREBASE_AUTH_DOMAIN=
VITE_FIREBASE_DATABASE_URL=
VITE_FIREBASE_PROJECT_ID=
VITE_FIREBASE_STORAGE_BUCKET=
VITE_FIREBASE_MESSAGING_SENDER_ID=
VITE_FIREBASE_APP_ID=
```

If a web app isn't registered on the Firebase project yet, register one first (Firebase
Console → Add app → Web).

## 6. What's still required from you before this runs against a real project

1. A Firebase project (existing or new) with **Realtime Database enabled** (not just Auth/FCM).
2. `FIREBASE_DATABASE_URL` (backend).
3. The 6 `VITE_FIREBASE_*` values (web).
4. Confirmation of whether `google-services.json` needs re-downloading (mobile) — only if
   Realtime Database was enabled after the file currently in the repo was generated.

Until these are supplied, everything builds and tests green against stubs/mocks
(`carebridge.firebase.realtime.enabled=false` backend default, try/catch-degrade on mobile,
`isFirebaseConfigured()` guard on web) — this is by design, not a placeholder left unfinished.
