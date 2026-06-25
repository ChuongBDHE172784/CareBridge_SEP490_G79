---
title: PHASE_B2D_RUNTIME_READINESS_REPORT
project: CareBridge_SEP490_G79
phase: B.2D — Final Runtime-Readiness Fixes
status: COMPLETE
created: 2026-06-25
author: AI Agent
supabase_modified: NO
destructive_sql_run: NO
commits_created: NO
---

# Phase B.2D Report — Final Runtime-Readiness Fixes

> All changes are source-only. No Supabase data or schema was touched.
> No destructive SQL was run against any shared database.
> No git commits were created.

---

## Summary

| Gap | Status | Key Outcome |
|---|---|---|
| 1 — Gemini optional: startup must not fail without key | ✅ COMPLETE | `FallbackRagServiceImpl` + `@Primary` on Gemini/Mock impls |
| 2 — Web blocked-account UI | ✅ COMPLETE | `BlockedAccountPage.tsx` + router + reason param in redirect |
| 3 — Flutter blocked-account UI | ✅ COMPLETE | `BlockedAccountScreen` + `clearWithReason` + `main.dart` routing |

---

## Gap 1 — Gemini Must Not Prevent Backend Startup

### Root Cause

`GeminiRagServiceImpl` is `@Service @Profile({"prod", "dev", "supabase"})`.
`MockRagServiceImpl` is `@Service @Profile("test")`.
When no matching profile is active (default profile, or any non-matching profile),
neither bean registers → `RagController` constructor injection fails →
`APPLICATION FAILED TO START`.

This was confirmed in Phase B.2C (startup failure against rehearsal DB with no profile set).
It did not surface in day-to-day development because `application.yaml` sets
`spring.profiles.active: dev` and the `dev` profile matches `GeminiRagServiceImpl`.

### Fix

Three changes:

**1. New `FallbackRagServiceImpl.java`**

```
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/
  integration/gemini/service/FallbackRagServiceImpl.java
```

- Plain `@Service`, no `@Profile`, no conditions.
- Always registers regardless of active profile.
- Returns `CONSERVATIVE_FALLBACK` answer + `STANDARD_DISCLAIMER` + `fallback=true`.
- Zero external dependencies — no Gemini client, no retriever.

**2. `@Primary` added to `GeminiRagServiceImpl`**

When both `GeminiRagServiceImpl` and `FallbackRagServiceImpl` are active (dev/prod/supabase
profile + key present/absent), `@Primary` makes Spring inject `GeminiRagServiceImpl`
into `RagController`. This avoids `NoUniqueBeanDefinitionException` without any
scan-order dependency.

**3. `@Primary` added to `MockRagServiceImpl`**

When the test profile is active, both `MockRagServiceImpl` and `FallbackRagServiceImpl`
are candidates. `@Primary` on `MockRagServiceImpl` means all existing tests continue to
use the canned mock responses. `FallbackRagServiceImpl` registers but is never injected.

### Startup Verification

```
Profile active: "dev"
GEMINI_API_KEY="" ./mvnw spring-boot:run (port 8090 — port 8080 already in use)
→ Started BackendApplication in 2.751 seconds
→ No APPLICATION FAILED TO START
→ No "bean of type RagService could not be found"
```

The same fix also covers the default profile (no active profile) scenario, since
`FallbackRagServiceImpl` registers with no conditions.

### Backend Test Verification

```
./mvnw test
Tests run: 252, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

247 pre-existing tests continue to pass.
5 new `RagNoGeminiStartupTest` tests added:

| Test ID | Scenario | Expected | Result |
|---|---|---|---|
| RAG-B2D-01 | Context loads; authorized MOTHER request | 200 + fallback=true | ✅ GREEN |
| RAG-B2D-02 | Unauthenticated | 401 | ✅ GREEN |
| RAG-B2D-03 | PARTNER role | 403 | ✅ GREEN |
| RAG-B2D-04 | Disabled account + valid JWT | 403 ACCOUNT_DISABLED | ✅ GREEN |
| RAG-B2D-05 | Locked account + valid JWT | 403 ACCOUNT_LOCKED | ✅ GREEN |

Tests use `@Import({SecurityConfig.class, FallbackRagServiceImpl.class})` with no
`@MockitoBean RagService` — the fallback is the real dependency under test.

---

## Gap 2 — Web Blocked-Account UI

### Problem

`apiClient.ts` already redirected to `/account-blocked` on 403 ACCOUNT_DISABLED/LOCKED
(implemented in Phase B.2C). However, no page existed at that route:

1. No `BlockedAccountPage` component.
2. No `/account-blocked` route in the router.
3. Redirect carried no reason parameter — page could not distinguish disabled vs locked.
4. The `* → /login` catch-all would intercept any attempt to visit the page
   if added in the wrong position.

### Fix

**1. `BlockedAccountPage.tsx`**

```
05_Development/CareBridgeWebApp/src/features/auth/pages/BlockedAccountPage.tsx
```

- Standalone page (no `AuthLayout`, no `ProtectedRoute`).
- Reads `?reason=disabled` or `?reason=locked` from `window.location.search`.
- Shows non-technical Vietnamese message for each reason.
- All inline JSX styles matching project conventions (`#F6F1EC` bg, `#C98C7B` accent,
  `#5A463F` primary text, `#9C857C` secondary text, `borderRadius: 32px` card,
  `borderRadius: 9999px` button).
- "Quay lại đăng nhập" button: `window.location.href = '/login'` (hard redirect,
  session already cleared before this page is rendered).

**2. Router update (`src/app/router/index.tsx`)**

- Added `{ path: '/account-blocked', element: <BlockedAccountPage /> }` as a
  top-level route, BEFORE the `*` catch-all.
- Route is not wrapped in `ProtectedRoute` — user is unauthenticated when redirected here.

**3. `apiClient.ts` update**

```
window.location.href = `/account-blocked?reason=${reason}`;
```

Where `reason = errorCode === 'ACCOUNT_DISABLED' ? 'disabled' : 'locked'`.

### Web Build Verification

```
npm run build
94 modules transformed (up from 93)
0 TypeScript errors
✓ built in 98ms
```

---

## Gap 3 — Flutter Blocked-Account UI

### Problem

`api_client.dart` already called `AuthState.instance.clear()` on account-blocked 403s
(Phase B.2C). `clear()` sets `isAuthenticated=false` → `ListenableBuilder` in `main.dart`
routes to `LoginScreen`. No indication to the user that their account is blocked.

### Fix

**1. `auth_state.dart` changes**

New field: `String? _blockedReason`
New getter: `String? get blockedReason => _blockedReason`

New method `clearWithReason(String reason)`:
- Sets `_blockedReason = reason` **before** calling `clearState()`.
- `clearState()` nulls tokens and calls `notifyListeners()`.
- On the first `notifyListeners()` call, `blockedReason != null` → `main.dart`
  routes to `BlockedAccountScreen` (not `LoginScreen`).
- `_storage.clear()` is fire-and-forget (`unawaited`).

Updated `clear()` (used for 401 / normal logout):
- Resets `_blockedReason = null` before `clearState()`.
- Ensures a normal logout never strands the user on the blocked screen.

New method `clearBlockedReason()`:
- Sets `_blockedReason = null` and calls `notifyListeners()`.
- `ListenableBuilder` transitions: `blockedReason=null, isAuthenticated=false → LoginScreen`.

**2. `api_client.dart` update**

Both `apiGet` and `apiPost`: account-blocked 403 now calls
`unawaited(AuthState.instance.clearWithReason(code))` instead of
`unawaited(AuthState.instance.clear())`.

**3. `blocked_account_screen.dart`**

```
05_Development/CareBridgeMobileApp/lib/features/auth/screens/blocked_account_screen.dart
```

- Warm Claymorphism Flutter design: `Color(0xFFF6F1EC)` scaffold,
  `Color(0xFFC98C7B)` accent, `BoxDecoration` with `BorderRadius.circular(32)`.
- Reads `AuthState.instance.blockedReason` to display ACCOUNT_DISABLED or
  ACCOUNT_LOCKED message.
- "Quay lại đăng nhập" button calls `AuthState.instance.clearBlockedReason()`.

**4. `main.dart` routing update**

In `ListenableBuilder` builder:
```dart
if (AuthState.instance.isRestoring)         → _SplashScreen
if (AuthState.instance.blockedReason != null) → BlockedAccountScreen  ← NEW
if (!AuthState.instance.isAuthenticated)     → LoginScreen
                                              → MainShell
```

### Flutter Verification

```
flutter test
12/12 tests passed (up from 9/9)
  - 1 widget smoke test (pre-existing)
  - 8 account_block_parser_test.dart (Phase B.2C)
  - 3 blocked_account_screen_test.dart (new — Phase B.2D)

flutter build apk --debug
✓ Built build/app/outputs/flutter-apk/app-debug.apk

flutter analyze
analysis server exited with code 255 (same pre-existing crash as B.2C —
LspByteStreamServerChannel infrastructure bug, not application code)
```

---

## Files Changed

### Backend

| File | Change |
|---|---|
| `integration/gemini/service/FallbackRagServiceImpl.java` | NEW — always-active fallback |
| `integration/gemini/service/GeminiRagServiceImpl.java` | Added `@Primary` |
| `integration/gemini/service/MockRagServiceImpl.java` | Added `@Primary` |
| `integration/gemini/RagNoGeminiStartupTest.java` | NEW — 5 startup/auth tests |

### Web

| File | Change |
|---|---|
| `features/auth/pages/BlockedAccountPage.tsx` | NEW — blocked account page |
| `app/router/index.tsx` | Added `/account-blocked` route + import |
| `shared/api/apiClient.ts` | Added `?reason=` param to redirect URL |

### Flutter

| File | Change |
|---|---|
| `core/auth/auth_state.dart` | Added `_blockedReason`, `clearWithReason`, `clearBlockedReason`, updated `clear` |
| `core/network/api_client.dart` | Calls `clearWithReason(code)` instead of `clear()` |
| `features/auth/screens/blocked_account_screen.dart` | NEW — Warm Claymorphism blocked screen |
| `main.dart` | Added `blockedReason` routing check |
| `test/features/auth/blocked_account_screen_test.dart` | NEW — 3 widget tests |

---

## What Was NOT Done (per authorization constraints)

- Supabase schema not touched
- Phase C reset not executed
- No destructive SQL
- No commits staged or pushed
- No new external dependencies (no Redis, no MongoDB, no Docker additions)
- No new Spring Boot profiles or Flyway migrations
