---
title: CORE_PLATFORM_RELEASE_AUDIT_V4
project: CareBridge_SEP490_G79
mode: READ_ONLY_AUDIT
supabase_modified: NO
phase: Post-B.2D — Final Runtime-Readiness
created: 2026-06-25
author: AI Agent
supersedes: CORE_PLATFORM_RELEASE_AUDIT_V3.md
---

# Core Platform Release Audit V4

> This document supersedes `CORE_PLATFORM_RELEASE_AUDIT_V3.md`.
> It evaluates the same dimensions after Phase B.2D
> (Gemini optional startup + blocked-account UI for web and Flutter).
> Supabase has NOT been touched. All changes are source-only.

---

## 10-Item Release Checklist

| # | Item | V3 Status | V4 Status | Verdict |
|---|---|---|---|---|
| 1 | V1 migration applies cleanly via Flyway with all 71 ERD tables | ✅ PASS | ✅ PASS (unchanged) | ✅ PASS |
| 2 | Flyway safety guards: `clean-disabled=true` in dev and supabase profiles | ✅ PASS | ✅ PASS (unchanged) | ✅ PASS |
| 3 | Backend starts without Gemini API key — no `APPLICATION FAILED TO START` | ⚠️ KNOWN GAP | ✅ PASS — `FallbackRagServiceImpl` registered | ✅ PASS |
| 4 | RAG endpoint returns conservative fallback (not 500) when Gemini is unconfigured | ✅ PASS (GeminiRagServiceImpl catches GeminiUnavailableException) | ✅ PASS — also verified via FallbackRagServiceImpl (RAG-B2D-01) | ✅ PASS |
| 5 | RAG endpoint enforces RBAC: unauthenticated→401, PARTNER→403 | ✅ PASS | ✅ PASS — re-verified RAG-B2D-02, RAG-B2D-03 | ✅ PASS |
| 6 | Per-request account-state enforcement: ACCOUNT_DISABLED and ACCOUNT_LOCKED return 403 with correct error code on every authenticated request | ✅ PASS (JwtAuthenticationFilter) | ✅ PASS — also verified RAG-B2D-04, RAG-B2D-05 against RAG endpoint | ✅ PASS |
| 7 | Web session is cleared on 403 ACCOUNT_DISABLED / ACCOUNT_LOCKED — no stale auth state left in localStorage | ✅ PASS (apiClient.ts logout + redirect) | ✅ PASS — redirect now includes `?reason=` param | ✅ PASS |
| 8 | Web `/account-blocked` page exists, is publicly routable, shows non-technical reason-specific message, provides "Return to Login" | ❌ MISSING | ✅ PASS — `BlockedAccountPage.tsx` with claymorphism design | ✅ PASS |
| 9 | Flutter routes to `BlockedAccountScreen` (not `LoginScreen`) on ACCOUNT_DISABLED / ACCOUNT_LOCKED — shows non-technical message, "Return to Login" clears blocked state | ❌ MISSING | ✅ PASS — `clearWithReason` + `BlockedAccountScreen` + `main.dart` routing | ✅ PASS |
| 10 | All test suites pass: backend 252/252, web build 0 errors, Flutter 12/12 | ⚠️ 247 backend, 9 Flutter | ✅ PASS — 252 backend, 94 web modules, 12 Flutter | ✅ PASS |

All 10 items PASS.

---

## 1. V1 Migration / Schema

Unchanged from V3. V1 applies cleanly (71 tables, 98 FK constraints, ~95 indexes).
Flyway-native rehearsal completed in Phase B.2C.

**Verdict: ✅ PASS**

---

## 2. Flyway Safety

Unchanged from V3. `clean-disabled: true` in dev and supabase profiles.
`flyway:repair` guidance removed from all documents. Local and Supabase rebaseline
runbooks exist.

**Verdict: ✅ PASS**

---

## 3. Backend Startup Without Gemini Key

### V3 Status
Open item: "`RagService` requires Gemini API key for full startup — set key in `.env`
before Supabase reset."

### V4 Fix
`FallbackRagServiceImpl` (`@Service`, no profile restriction) always registers.
`@Primary` on `GeminiRagServiceImpl` and `MockRagServiceImpl` ensures only the
correct implementation is injected when multiple candidates are present.

Startup with `GEMINI_API_KEY=""` and `dev` profile:
```
Started BackendApplication in 2.751 seconds
No APPLICATION FAILED TO START
No "bean of type RagService could not be found"
```

**Verdict: ✅ PASS** — Gemini API key is now a runtime-optional enhancement, not a
startup requirement.

---

## 4. RAG Fallback Behaviour

`GeminiRagServiceImpl` (when active in dev/prod/supabase) already caught
`GeminiUnavailableException` and returned `CONSERVATIVE_FALLBACK`. The fallback
string and `STANDARD_DISCLAIMER` are used unchanged.

`FallbackRagServiceImpl` returns the same content when neither Gemini nor Mock is active.

RAG-B2D-01 verified: authorized MOTHER request against `FallbackRagServiceImpl` → 200 +
`fallback=true` + non-empty `disclaimer`.

**Verdict: ✅ PASS**

---

## 5. RAG RBAC Enforcement

Unchanged from V3. `@PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR',
'CONTENT_ADMIN', 'SYSTEM_ADMIN')")` on `RagController`.

Re-verified in B.2D context:
- RAG-B2D-02: no auth → 401 ✅
- RAG-B2D-03: PARTNER role → 403 ✅

**Verdict: ✅ PASS**

---

## 6. Per-Request Account-State Enforcement

Unchanged from V3. `JwtAuthenticationFilter` performs `userRepository.findById(UUID)` on
every authenticated request.

Re-verified against RAG endpoint in B.2D:
- RAG-B2D-04: disabled account + valid JWT → 403 `ACCOUNT_DISABLED` ✅
- RAG-B2D-05: locked account + valid JWT → 403 `ACCOUNT_LOCKED` ✅

**Verdict: ✅ PASS**

---

## 7. Web Session Cleared on Account-Blocked

Unchanged from V3. `apiClient.ts` calls `useAuthStore.getState().logout()` then redirects.

Updated in B.2D: redirect now includes `?reason=disabled` or `?reason=locked` so the
blocked-account page can show the correct message.

**Verdict: ✅ PASS**

---

## 8. Web Blocked-Account Page

### V3 Status
Open item: "/account-blocked route needs a UI page — MEDIUM risk."

### V4 Fix

`BlockedAccountPage.tsx`:
- Reads `?reason` from `window.location.search`
- Shows "Tài khoản bị vô hiệu hoá" (disabled) or "Tài khoản bị khoá tạm thời" (locked)
- Non-technical message with support contact guidance
- "Quay lại đăng nhập" button → `window.location.href = '/login'`
- Inline styles: `#F6F1EC` bg, `#C98C7B` accent, `#5A463F` text, `borderRadius:32px` card,
  `borderRadius:9999px` button — matches project design conventions

Router: `/account-blocked` added as top-level route BEFORE `* → /login` catch-all.
Not wrapped in `ProtectedRoute`.

**Verdict: ✅ PASS**

---

## 9. Flutter Blocked-Account Screen

### V3 Status
Not mentioned as a gap in V3 — but was a known gap: blocked users returned to
`LoginScreen` with no message.

### V4 Fix

`AuthState` changes:
- `String? blockedReason` — set before `clearState()` in `clearWithReason()`
- `clearWithReason(String reason)` — sets reason, clears tokens (sync), fire-and-forgets storage clear
- `clearBlockedReason()` — clears reason, triggers `ListenableBuilder` → `LoginScreen`
- `clear()` updated — resets `blockedReason=null` so normal 401 logout is clean

`api_client.dart` change: calls `clearWithReason(code)` on account-blocked 403s.

`BlockedAccountScreen`:
- `Color(0xFFF6F1EC)` scaffold, `Color(0xFFC98C7B)` accent, rounded card
- Shows correct message for DISABLED vs LOCKED
- "Quay lại đăng nhập" → `clearBlockedReason()` → `main.dart` routes to `LoginScreen`

`main.dart` routing: checks `blockedReason != null` before `!isAuthenticated`.

**Verdict: ✅ PASS**

---

## 10. Test Suite Status

| Suite | V3 Count | V4 Count | Status |
|---|---|---|---|
| Backend `./mvnw test` | 247 | **252** | ✅ All pass |
| New `RagNoGeminiStartupTest` (B.2D) | 0 | **5** | ✅ RAG-B2D-01..05 |
| Web `npm run build` | 93 modules, 0 errors | **94 modules**, 0 errors | ✅ |
| Flutter `flutter test` | 9/9 | **12/12** | ✅ |
| New `blocked_account_screen_test` (B.2D) | 0 | **3** | ✅ |
| Flutter `flutter build apk --debug` | ✅ | ✅ | ✅ |
| `flutter analyze` | ⚠️ server crash (code 255) | ⚠️ server crash (code 255) | Same pre-existing tooling bug |

`flutter analyze` crash is in `LspByteStreamServerChannel` — the analysis server
infrastructure, not application code. APK builds cleanly. This is the same
pre-existing issue observed in B.1 and B.2C.

**Verdict: ✅ PASS**

---

## Open Items (Post-B.2D)

| Item | Category | Risk | Resolution Path |
|---|---|---|---|
| Supabase not yet migrated to V1 (71 tables) | Schema | DEFERRED | Phase C explicit approval + guarded reset |
| `flutter analyze` server crash | Tooling | LOW — build and tests pass | Flutter SDK bug; monitor Flutter release notes |
| 46 future tables have no `@Entity` yet | Implementation | EXPECTED | Each domain team creates `@Entity` when implementing the feature |

---

## Final Status

```
READY_FOR_SHARED_DB_RESET
```

All 10 checklist items PASS. The two runtime-readiness gaps from V3 (backend startup
without Gemini key; blocked-account UI) are resolved. The only remaining pre-Phase-C
blocker is the Supabase reset itself, which requires explicit Phase C approval and
execution of the guarded reset script per `SUPABASE_REBASELINE_RUNBOOK.md`.
