
# CareBridge — Startup Guide

## Prerequisites

| Tool    | Version             | Required for                |
| ------- | ------------------- | --------------------------- |
| Java    | 21+                 | Backend                     |
| Maven   | 3.9+ (or`./mvnw`) | Backend                     |
| Node.js | 20+                 | Web App                     |
| Flutter | 3.22+               | Mobile App                  |
| Docker  | 24+                 | Local PostgreSQL (optional) |

---

## 0. Canonical edge-local stack

The architecture-parity workflow routes both Web and API traffic through a
named Cloudflare Tunnel and edge Nginx. It intentionally publishes no backend
or Vite host ports. Complete the one-time Cloudflare steps in
`05_Development/Deployment/EDGE_TOPOLOGY.md`, then run from the repository root:

```bash
docker compose \
  --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml up --build
```

Use the individual service commands below only when intentionally debugging
without the edge topology.

---

## 1. Backend (Spring Boot)

**Path:** `05_Development/CareBridgeAPI`

### Using local PostgreSQL (Docker Compose)

```bash
cd 05_Development/CareBridgeAPI

# Start PostgreSQL via Docker Compose
docker compose up -d

# Run with local profile
./mvnw spring-boot:run
```

### Using Supabase (remote DB)

```bash
cd 05_Development/CareBridgeAPI

# Set environment variables
export SUPABASE_DB_URL=jdbc:postgresql://<host>:<port>/<db>
export SUPABASE_DB_USERNAME=<user>
export SUPABASE_DB_PASSWORD=<password>
export SUPABASE_URL=https://<project>.supabase.co
export SUPABASE_ANON_KEY=<key>
export JWT_ACTIVE_KEY_ID=<active-kid>
export JWT_PRIVATE_KEY=<base64-der-pkcs8-private-key>
export JWT_PUBLIC_KEYS=<active-kid>:<base64-der-spki-public-key>

# Run with supabase profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase
```

**Backend starts at:** `http://localhost:8080`

### Optional synthetic dev data

Dev seeding is disabled by default. It is available only when the Spring `dev`
profile is active, the `prod` profile is absent, and
`CAREBRIDGE_DEV_SEED_ENABLED=true` is explicitly configured. Before enabling it,
the operator must inject a unique, non-default synthetic password through
`CAREBRIDGE_DEV_SEED_PASSWORD` in an uncommitted local secret source. Do not put
the value in this guide, shell history, logs, screenshots, or Git.

When seeding is enabled, a blank password or the retired historical default makes
backend startup fail closed. Never enable dev seeding on staging or production.

### Run Tests

```bash
./mvnw test
# Expected: 240 pass, 1 error (BackendApplicationTests requires live DB)
```

---

## 2. Web App (Vite React TypeScript)

**Path:** `05_Development/CareBridgeWebApp`

```bash
cd 05_Development/CareBridgeWebApp

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev
```

**Web app starts at:** `http://localhost:5173`

### Environment Variables (optional)

Create `.env.local` in `05_Development/CareBridgeWebApp/`:

```env
VITE_API_URL=http://localhost:8080
```

If not set, defaults to `http://localhost:8080`.

### Available Pages

| Tab              | Route     | Use Case | Role          |
| ---------------- | --------- | -------- | ------------- |
| Moderation Queue | default   | UC99     | MODERATOR     |
| Manage Topics    | click tab | UC109    | MODERATOR     |
| Create Content   | click tab | UC105    | CONTENT_ADMIN |
| Create Partner   | click tab | UC118    | PARTNER       |

> **Note:** All API calls require a JWT token in `localStorage.accessToken`. Log in via the backend `/api/v1/auth/login` endpoint first and store the token.

---

## 3. Mobile App (Flutter)

**Path:** `05_Development/CareBridgeMobileApp`

```bash
cd 05_Development/CareBridgeMobileApp

# Get dependencies
flutter pub get

# Run on connected device or emulator
flutter run
```

### Change Backend URL

Edit `lib/core/network/api_client.dart`:

```dart
const String _baseUrl = 'http://10.0.2.2:8080';  // Android emulator → host
// or
const String _baseUrl = 'http://localhost:8080';  // iOS simulator
// or  
const String _baseUrl = 'http://<your-local-ip>:8080';  // physical device
```

### Available Screens

| Tab       | Screen                              | Use Case    |
| --------- | ----------------------------------- | ----------- |
| Feed      | Community feed with infinite scroll | UC198       |
| Questions | Search community questions          | UC162       |
| Content   | View articles & checklists          | UC82, UC224 |
| Search    | Search verified content             | UC224       |
| AI Chat   | RAG-powered AI health assistant     | UC132       |

> Additional screens for creating questions (UC54) and posting answers (UC56) are launched from within the feed and question detail flows.

---

## 4. CORS Setup

Backend CORS is pre-configured to allow `http://localhost:5173` (Web App) and all origins during development. No extra configuration needed.

---

## 5. Running All Three Together

```bash
# Terminal 1 — Backend
cd 05_Development/CareBridgeAPI && ./mvnw spring-boot:run

# Terminal 2 — Web App
cd 05_Development/CareBridgeWebApp && npm run dev

# Terminal 3 — Mobile
cd 05_Development/CareBridgeMobileApp && flutter run
```
