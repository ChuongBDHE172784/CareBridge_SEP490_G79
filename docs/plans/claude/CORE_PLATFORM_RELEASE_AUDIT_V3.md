---
title: CORE_PLATFORM_RELEASE_AUDIT_V3
project: CareBridge_SEP490_G79
mode: READ_ONLY_AUDIT
supabase_modified: NO
phase: Post-B.2C — Full Schema Baseline + Account-State Enforcement
created: 2026-06-25
author: AI Agent
supersedes: CORE_PLATFORM_RELEASE_AUDIT_V2.md
---

# Core Platform Release Audit V3

> This document supersedes `CORE_PLATFORM_RELEASE_AUDIT_V2.md`.
> It evaluates the same dimensions after Phase B.2B (full schema baseline)
> and Phase B.2C (baseline safety correction + account-state enforcement).
> Supabase has NOT been touched. All changes are source-only.

---

## 1. V1 Baseline Scope

| Dimension | V2 Audit (Post-B.1) | V3 (Post-B.2B/B.2C) |
|---|---|---|
| Total tables in V1 | 25 | **71** ✅ |
| CORE tables | 13 | 13 ✅ |
| CURRENT_IMPLEMENTED_UC tables | 12 | 12 ✅ (same) |
| FUTURE_FEATURE tables | 0 (removed in B.1) | **46** (re-added with correct ERD DDL) ✅ |
| FK constraints | 3 | **98** ✅ |
| Indexes | 22 | **~95** (excl. PKs) ✅ |
| Lines | 952 | 1,995 ✅ |

### Strategy Change from V2

V2 removed the 46 FUTURE_FEATURE tables to reduce V1 scope. Phase B.2B
reverses this: all 71 ERD-approved tables are included in V1 as a "Master
Database Schema Baseline." Rationale:

- DB scope ≠ Java entity scope
- Future feature tables exist in PostgreSQL before `@Entity` is written
- `ddl-auto: validate` ignores unmapped tables — zero startup failures
- Cross-team FK references available from day 1

### V1 Flyway Rehearsal (Flyway-native — not psql)

Phase B.2C conducted an actual Flyway rehearsal (previous rehearsal in B.2B
used `psql` directly, bypassing Flyway machinery):

```
DB: carebridge_full_flyway_test (local PostgreSQL 18.1)
flyway:migrate → SUCCESS — "Successfully applied 1 migration"
flyway:validate → SUCCESS — "Successfully validated 1 migration"
Table count: SELECT COUNT(*) ... → 71 ✅
flyway_schema_history: 1 row — version=1, description='init schema',
  checksum=1601315817, success=t ✅
Hibernate ddl-auto=validate: JPA EntityManagerFactory initialized ✅
DB dropped after rehearsal ✅
```

**Verdict: ✅ PASS** — V1 applies cleanly via Flyway, validates cleanly,
and Hibernate validates all 15 mapped entities against the 71-table schema.

---

## 2. Flyway Safety

| Check | V2 (Post-B.1) | V3 (Post-B.2C) |
|---|---|---|
| `clean-disabled: true` (dev) | ✅ Added | ✅ Unchanged |
| `clean-disabled: true` (supabase) | ✅ Added | ✅ Unchanged |
| `ignore-migration-patterns` removed | ✅ Removed | ✅ Unchanged |
| `flyway:repair` guidance | ✅ Not documented | ⚠️ Was in B.2B docs — **corrected in B.2C** |
| Local dev rebaseline runbook | ❌ Missing | ✅ `LOCAL_DATABASE_REBASELINE_RUNBOOK.md` |
| Supabase rebaseline runbook | ❌ Missing | ✅ `SUPABASE_REBASELINE_RUNBOOK.md` |

### `flyway:repair` Safety Correction (Part A of B.2C)

Phase B.2B documents (REBASELINE_REPORT, V1_SCHEMA_BASELINE, CORRECTION_PLAN,
AUDIT_V2, REMEDIATION_REPORT) incorrectly recommended `flyway:repair` for
developers adopting the new V1.

**Why this was wrong:** `flyway:repair` only updates `flyway_schema_history`
checksums. It does NOT add the 46 new tables. A developer running `flyway:repair`
would have a "healthy" Flyway state with 25 tables (missing 46).

**Corrected rule in all 5 documents:**
> Any existing local or shared development database created from the old V1 +
> V2–V12 migration history must be recreated from a clean schema before using
> the new V1. Do not use `flyway:repair` as a schema upgrade mechanism.

**Verdict: ✅ PASS** — `flyway:repair` guidance removed from all documents.
Runbooks created with Case A (disposable DB) and Case B (data worth keeping).

---

## 3. Auth / Session

### 3a. Web Auth Token Storage

| Dimension | V2 (Post-B.1) | V3 (Post-B.2C) |
|---|---|---|
| Bearer token injection | ✅ `useAuthStore.getState().accessToken` | ✅ Unchanged |
| 401 → logout + redirect | ✅ Added | ✅ Unchanged |
| 403 ACCOUNT_DISABLED → logout | ❌ Missing | ✅ Added — `/account-blocked` redirect |
| 403 ACCOUNT_LOCKED → logout | ❌ Missing | ✅ Added — `/account-blocked` redirect |
| 403 role mismatch → NO logout | ✅ (intentional) | ✅ Preserved — only account-blocked 403s trigger logout |

**Verdict: ✅ PASS** — 403 account-blocked codes now correctly clear web session.

### 3b. Flutter Session Persistence

| Dimension | V2 (Post-B.1) | V3 (Post-B.2C) |
|---|---|---|
| Secure storage | ✅ Added | ✅ Unchanged |
| Token hydration on startup | ✅ Added | ✅ Unchanged |
| 401 → clear session | ✅ Added | ✅ Unchanged |
| 403 ACCOUNT_DISABLED → clear session | ❌ Missing | ✅ Added (`_accountBlockedCode` helper) |
| 403 ACCOUNT_LOCKED → clear session | ❌ Missing | ✅ Added |

**Verdict: ✅ PASS** — 403 account-blocked codes now correctly clear Flutter session.

---

## 4. Authorization / Guest Access

| Dimension | V2 (Post-B.1) | V3 (Post-B.2C) |
|---|---|---|
| `RagController @PreAuthorize` | ✅ Added | ✅ Unchanged |
| PARTNER blocked from RAG | ✅ | ✅ Unchanged |
| Disabled account per-request check | ⚠️ DEFERRED (Token TTL = 15 min accepted as residual risk) | ✅ **IMPLEMENTED** |
| Locked account per-request check | ⚠️ DEFERRED | ✅ **IMPLEMENTED** |
| User deleted after JWT issued | ⚠️ DEFERRED | ✅ **IMPLEMENTED** (401 AUTHENTICATION_FAILED) |
| Enforcement location | N/A | `JwtAuthenticationFilter` — centralized, no Redis, no controller changes |
| Auth endpoints excluded | N/A | ✅ `shouldNotFilter()` excludes register/login/verify-otp/refresh |

### Per-Request Account-State Check (Part C of B.2C)

`JwtAuthenticationFilter` now performs a `userRepository.findById(UUID)` lookup
on every authenticated request after JWT signature/expiry validation:

```
Token valid + user not found    → 401 AUTHENTICATION_FAILED
Token valid + enabled=false     → 403 ACCOUNT_DISABLED
Token valid + locked=true       → 403 ACCOUNT_LOCKED
Token valid + active user       → set SecurityContext (existing behavior)
```

The `ACCOUNT_DISABLED` and `ACCOUNT_LOCKED` codes are stable string identifiers
distinguishable from `ACCESS_DENIED` (role mismatch). Both web `apiClient.ts`
and Flutter `api_client.dart` inspect the `error` field to distinguish them.

**DB cost:** 1 `SELECT users WHERE user_id=?` per authenticated request.
For MVP scale this is acceptable. Redis caching can be added later if needed.

**Verdict: ✅ PASS** — account-state enforcement is now real-time and centralized.

---

## 5. Supabase Reset Process

| Dimension | V2 (Post-B.1) | V3 (Post-B.2C) |
|---|---|---|
| Guarded reset script | ✅ Added | ✅ Unchanged |
| GUC guard 1 | ✅ | ✅ Unchanged |
| GUC guard 2 | ✅ | ✅ Unchanged |
| Supabase rebaseline runbook | ❌ Missing | ✅ `SUPABASE_REBASELINE_RUNBOOK.md` |
| Supabase `flyway:repair` prohibited | ❌ Not documented | ✅ Documented as absolute prohibition |
| Supabase current status | AWAITING_PHASE_C_APPROVAL | AWAITING_PHASE_C_APPROVAL (unchanged) |

**Verdict: ✅ PASS** — reset process unchanged from V2 (still valid). Supabase
rebaseline runbook adds the Phase C checklist and explicit prohibitions.

---

## 6. Test Quality

| Suite | V2 Count | V3 Count | Status |
|---|---|---|---|
| Backend `./mvnw clean test` | 242 | **247** | ✅ All pass |
| Account-state filter tests (new) | 0 | **5** | ✅ F-AS-01 through F-AS-05 |
| `RAG-TC-AUTH-001` (PARTNER→403) | 1 | 1 | ✅ Unchanged |
| Web `npm run build` | 93 modules, 0 errors | 93 modules, 0 errors | ✅ |
| Flutter `flutter test` | 1/1 | **9/9** (8 new account-block parser tests) | ✅ |
| Flutter `flutter build apk --debug` | ✅ Built | ✅ Built | ✅ |
| Flyway rehearsal (psql-based) | 25 tables ✅ | 71 tables ✅ | ✅ |
| Flyway rehearsal (Flyway-native, NEW) | ❌ Not done in B.2B | ✅ Done in B.2C | ✅ |

### New Account-State Tests (F-AS-01 through F-AS-05)

| ID | Scenario | Assert | Result |
|---|---|---|---|
| F-AS-01 | Active user + valid token | 200 pass-through | ✅ GREEN |
| F-AS-02 | Disabled user + valid token | 403 + `error=ACCOUNT_DISABLED` | ✅ GREEN |
| F-AS-03 | Locked user + valid token | 403 + `error=ACCOUNT_LOCKED` | ✅ GREEN |
| F-AS-04 | Deleted user + valid token | 401 AUTHENTICATION_FAILED | ✅ GREEN |
| F-AS-05 | No token | 401 (entry point, filter no-ops) | ✅ GREEN |

### New Flutter Account-Block Parser Tests (8 tests)

| Test | Scenario | Expected | Result |
|---|---|---|---|
| 1 | 403 + `error=ACCOUNT_DISABLED` | returns `"ACCOUNT_DISABLED"` | ✅ GREEN |
| 2 | 403 + `error=ACCOUNT_LOCKED` | returns `"ACCOUNT_LOCKED"` | ✅ GREEN |
| 3 | 403 + `error=ACCESS_DENIED` (role mismatch) | returns `null` — must NOT logout | ✅ GREEN |
| 4 | 401 | returns `null` (handled by 401 branch separately) | ✅ GREEN |
| 5 | 200 | returns `null` | ✅ GREEN |
| 6 | 403 + empty body | returns `null` (no crash) | ✅ GREEN |
| 7 | 403 + non-JSON body | returns `null` (no crash) | ✅ GREEN |
| 8 | 403 + missing `error` field | returns `null` | ✅ GREEN |

Tests live in `test/core/network/account_block_parser_test.dart` and target
`lib/core/network/account_block_parser.dart` — the extracted public function
that `api_client.dart` delegates to.

### `flutter analyze` Status

Analysis server crashed with code 255 (LspByteStreamServerChannel crash). Same
pre-existing tooling bug as Phase B.1. Debug APK builds cleanly; Dart code is
syntactically valid. This is a Flutter SDK/tooling issue on this machine, not
an application code issue.

---

## 7. Schema Divergences (B.2B Decisions)

Five deliberate divergences between the ERD and `V1__init_schema.sql` are
documented in `FULL_SCHEMA_V1_RECONCILIATION.md`:

| Table | Decision | ERD Update Needed |
|---|---|---|
| `users` | `enabled/locked/role` instead of `account_status/email_verified` | Yes |
| `partner_organizations` | UC-56 shape (city/phone/email) | Yes |
| `community_topics/questions/answers` | PK = `id` not `topic_id/question_id/answer_id` | Yes |
| `community_answers` | `is_expert_labeled` boolean not `expert_profile_id FK` | Yes |
| Existing 25-table FK gaps | Not back-filled (test safety) | Deferred to V2+ migrations |

**Verdict: ✅ DOCUMENTED** — all divergences have explicit rationale.
ERD update is deferred to a future design review sprint.

---

## 8. Remaining Open Items

| Item | Category | Risk | Resolution Path |
|---|---|---|---|
| Supabase not yet migrated to V1 (71 tables) | Schema | DEFERRED | Phase C explicit approval + guarded reset |
| `flutter analyze` server crash | Tooling | LOW — build succeeds | Flutter SDK bug; monitor Flutter release notes |
| `RagService` requires Gemini API key for full startup | Config | LOW — tests mock it | Set key in `.env` before Supabase reset |
| 46 future tables have no `@Entity` yet | Implementation | EXPECTED | Each domain team creates `@Entity` when implementing the feature |
| `/account-blocked` route needs a UI page | Frontend | MEDIUM | Web team must create the blocked-account screen |

---

## 9. Final Verdict

| Blocker | V2 Status | V3 Status |
|---|---|---|
| Web auth token storage dual-gap | ✅ RESOLVED | ✅ RESOLVED |
| Reset script — no executable guards | ✅ RESOLVED | ✅ RESOLVED |
| Flutter session in-memory only | ✅ RESOLVED (code-verified) | ✅ RESOLVED |
| `ignore-migration-patterns` silently hiding problems | ✅ RESOLVED | ✅ RESOLVED |
| RAG endpoint missing `@PreAuthorize` | ✅ RESOLVED | ✅ RESOLVED |
| V1 not containing all 71 ERD tables | ✅ RESOLVED in B.2B | ✅ RESOLVED |
| `flyway:repair` guidance in docs | ❌ NEW — introduced in B.2B | ✅ RESOLVED in B.2C |
| Account-state not checked per-request | ⚠️ DEFERRED (V2) | ✅ RESOLVED in B.2C |
| Flyway rehearsal (Flyway-native, not psql) | ❌ Not done | ✅ DONE in B.2C |

---

## Final Status

```
AWAITING_CORE_RELEASE_AUDIT_V3_REVIEW
```
