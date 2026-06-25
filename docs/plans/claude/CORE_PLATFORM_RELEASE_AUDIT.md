# CareBridge Core Platform Release Audit

```
Status:            DRAFT
Mode:              READ_ONLY_AUDIT
Supabase Modified: NO
Date:              2026-06-25
Auditor:           Claude Code (claude-sonnet-4-6)
```

---

## 1. V1 Baseline Scope Audit

`V1__baseline.sql` contains exactly **71 tables**. Classification below.

**Legend**
- `CORE` — needed at startup for auth, RBAC, consent, audit
- `CURRENT_IMPLEMENTED_UC` — a controller/service/test is already live for this table
- `FUTURE_FEATURE` — no controller, service, or test exists yet

| # | Table | Owning Module | Implemented Use Case / Requirement | Classification | Recommend | Reason |
|---|-------|--------------|-------------------------------------|----------------|-----------|--------|
| 1 | `users` | `security` | Auth registration, login, profile | CORE | KEEP | Auth cannot function without it |
| 2 | `refresh_tokens` | `security` | JWT refresh rotation | CORE | KEEP | Session management |
| 3 | `otp_verifications` | `security` | OTP login flow | CORE | KEEP | Phone login requires it |
| 4 | `roles` | `identity` | RBAC lookup table | CORE | KEEP | Role enum references it |
| 5 | `user_roles` | `identity` | Role assignment | CORE | KEEP | `@PreAuthorize` enforcement |
| 6 | `user_sessions` | `identity` | Session tracking / logout | CORE | KEEP | Logout invalidation |
| 7 | `community_profiles` | `identity` | Anonymous display name | CORE | KEEP | Referenced by community answer mapper |
| 8 | `notification_preferences` | `identity` | Push notification opt-in | CORE | KEEP | Schema FK target; entity mapped |
| 9 | `notifications` | `identity` | Notification inbox | CORE | KEEP | Entity mapped |
| 10 | `data_permissions` | `identity` | Consent data-scope enforcement | CORE | KEEP | Consent architecture requires it |
| 11 | `consent_grants` | `consent` | Consent management | CORE | KEEP | Phase B consent foundation |
| 12 | `audit_logs` | `audit` | Append-only audit trail | CORE | KEEP | `ddl-auto: validate` verifies it |
| 13 | `security_events` | `audit` | Security incident log | CORE | KEEP | Security architecture |
| 14 | `community_topics` | `community` | UC-109 Manage Community Topics | CURRENT_IMPLEMENTED_UC | KEEP | CommunityTopicController live |
| 15 | `community_questions` | `community` | UC-54, UC-162, UC-198 | CURRENT_IMPLEMENTED_UC | KEEP | CommunityQuestionController live |
| 16 | `community_answers` | `community` | UC-56, UC-198 | CURRENT_IMPLEMENTED_UC | KEEP | CommunityAnswerController live |
| 17 | `contribution_points` | `community` | UC-56 (answer reward) | CURRENT_IMPLEMENTED_UC | KEEP | ContributionPoint entity mapped |
| 18 | `content_items` | `content` | UC-82, UC-198, UC-224 | CURRENT_IMPLEMENTED_UC | KEEP | ContentController + AdminContentController live |
| 19 | `content_reports` | `content` | UC-99 Moderation Queue input | CURRENT_IMPLEMENTED_UC | KEEP | ModerationController live |
| 20 | `moderation_actions` | `content` | UC-99 Moderation Queue output | CURRENT_IMPLEMENTED_UC | KEEP | ModerationController live |
| 21 | `checklist_templates` | `content` | UC-82 Checklist view | CURRENT_IMPLEMENTED_UC | KEEP | ContentController /checklists endpoint live |
| 22 | `checklist_items` | `content` | UC-82 Checklist items | CURRENT_IMPLEMENTED_UC | KEEP | ChecklistItem entity mapped |
| 23 | `partner_organizations` | `partner` | UC-118 Create Partner Profile | CURRENT_IMPLEMENTED_UC | KEEP | PartnerProfileController live |
| 24 | `triage_assessments` | `triage` | UC-132 RAG intake | CURRENT_IMPLEMENTED_UC | KEEP | TriageAssessment entity mapped; RagService references |
| 25 | `triage_answers` | `triage` | UC-132 RAG answers | CURRENT_IMPLEMENTED_UC | KEEP | TriageAnswer entity mapped |
| 26 | `baby_daily_logs` | `babycare` | — | FUTURE_FEATURE | FLAG | No controller or test; baby module not started |
| 27 | `baby_profiles` | `babycare` | — | FUTURE_FEATURE | FLAG | Baby module not started |
| 28 | `care_facilities` | `partner/emergency` | — | FUTURE_FEATURE | FLAG | TrackAsia emergency module future |
| 29 | `care_group_members` | `carecoordination` | — | FUTURE_FEATURE | FLAG | Family sync module future |
| 30 | `care_groups` | `carecoordination` | — | FUTURE_FEATURE | FLAG | Family sync module future |
| 31 | `care_tasks` | `carecoordination` | — | FUTURE_FEATURE | FLAG | Family sync module future |
| 32 | `commission_records` | `payment` | — | FUTURE_FEATURE | FLAG | Payment module future |
| 33 | `consultation_bookings` | `consultation` | — | FUTURE_FEATURE | FLAG | Expert consultation lifecycle future |
| 34 | `consultation_disputes` | `payment` | — | FUTURE_FEATURE | FLAG | Dispute resolution future |
| 35 | `consultation_messages` | `consultation` | — | FUTURE_FEATURE | FLAG | Real-time consultation future |
| 36 | `consultation_price_bands` | `payment` | — | FUTURE_FEATURE | FLAG | Pricing tiers future |
| 37 | `consultation_sessions` | `consultation` | — | FUTURE_FEATURE | FLAG | ZegoCloud integration future |
| 38 | `development_milestones` | `babycare` | — | FUTURE_FEATURE | FLAG | Baby module future |
| 39 | `device_measurements` | `device` | — | FUTURE_FEATURE | FLAG | Wearable/IMU integration future |
| 40 | `emergency_events` | `emergency` | — | FUTURE_FEATURE | FLAG | TrackAsia emergency future |
| 41 | `exercise_safety_checks` | `exercise` | — | FUTURE_FEATURE | FLAG | Pregnancy exercise future |
| 42 | `exercise_sessions` | `exercise` | — | FUTURE_FEATURE | FLAG | Pregnancy exercise future |
| 43 | `expenses` | `carecoordination` | — | FUTURE_FEATURE | FLAG | Family expense tracking future |
| 44 | `expert_availability` | `expert` | — | FUTURE_FEATURE | FLAG | Expert scheduling future |
| 45 | `expert_consultation_prices` | `payment` | — | FUTURE_FEATURE | FLAG | Expert pricing future |
| 46 | `expert_credentials` | `expert` | — | FUTURE_FEATURE | FLAG | Expert verification future |
| 47 | `expert_location_shares` | `expert` | — | FUTURE_FEATURE | FLAG | Expert location future |
| 48 | `expert_profiles` | `expert` | — | FUTURE_FEATURE | FLAG | Expert profile future |
| 49 | `expert_reviews` | `expert` | — | FUTURE_FEATURE | FLAG | Review system future |
| 50 | `growth_measurements` | `babycare` | — | FUTURE_FEATURE | FLAG | Growth tracking future |
| 51 | `health_device_connections` | `device` | — | FUTURE_FEATURE | FLAG | Wearable pairing future |
| 52 | `health_records` | `healthrecord` | — | FUTURE_FEATURE | FLAG | Health record module future |
| 53 | `health_summaries` | `healthrecord` | — | FUTURE_FEATURE | FLAG | Health summaries future |
| 54 | `location_snapshots` | `emergency` | — | FUTURE_FEATURE | FLAG | Emergency location future |
| 55 | `maternal_health_metrics` | `carejourney` | — | FUTURE_FEATURE | FLAG | Mother journey future |
| 56 | `mother_journeys` | `carejourney` | — | FUTURE_FEATURE | FLAG | Mother journey future |
| 57 | `partner_expert_links` | `partner` | — | FUTURE_FEATURE | FLAG | Partner-expert association future |
| 58 | `partner_services` | `partner` | — | FUTURE_FEATURE | FLAG | Partner service listing future |
| 59 | `payment_transactions` | `payment` | — | FUTURE_FEATURE | FLAG | VNPay integration future |
| 60 | `postpartum_logs` | `carejourney` | — | FUTURE_FEATURE | FLAG | Postpartum tracking future |
| 61 | `posture_analysis_configs` | `exercise` | — | FUTURE_FEATURE | FLAG | MediaPipe posture future |
| 62 | `posture_feedback_events` | `exercise` | — | FUTURE_FEATURE | FLAG | MediaPipe posture future |
| 63 | `pregnancy_exercises` | `exercise` | — | FUTURE_FEATURE | FLAG | Exercise library future |
| 64 | `refund_records` | `payment` | — | FUTURE_FEATURE | FLAG | Payment refund future |
| 65 | `reminders` | `reminder` | — | FUTURE_FEATURE | FLAG | Reminder/FCM future |
| 66 | `safety_alerts` | `safety` | — | FUTURE_FEATURE | FLAG | Safety monitoring future |
| 67 | `safety_events` | `safety` | — | FUTURE_FEATURE | FLAG | Safety monitoring future |
| 68 | `safety_monitoring_settings` | `safety` | — | FUTURE_FEATURE | FLAG | Safety monitoring future |
| 69 | `settlement_records` | `payment` | — | FUTURE_FEATURE | FLAG | Payment settlement future |
| 70 | `sponsored_campaigns` | `partner` | — | FUTURE_FEATURE | FLAG | Partner campaign future |
| 71 | `vaccination_records` | `babycare` | — | FUTURE_FEATURE | FLAG | Vaccination tracking future |

**Summary**

| Classification | Count |
|---|---|
| CORE | 13 |
| CURRENT_IMPLEMENTED_UC | 12 |
| FUTURE_FEATURE (FLAG) | 46 |
| **Total** | **71** |

**Schema scope assessment**: 46 of 71 tables (64.8%) are for future modules with no current code. The schema is the full projected ERD, not a lean core platform baseline. Hibernate `ddl-auto: validate` accepts this because it only validates tables that JPA `@Entity` classes map — it ignores unmapped tables. There is **no runtime harm** from the extra tables. **Recommendation**: do not remove them at this stage. The Supabase schema will be reset once via Phase C, after which all 71 tables will be applied clean from V1__baseline.sql. Removing future tables now would require a V2 migration when those modules are built.

---

## 2. Flyway Safety Audit

### 2.1 Migration files present

```
src/main/resources/db/migration/
└── V1__baseline.sql       ✅  only file; 63,443 bytes; 71 tables
```

V2 through V12: **absent from source tree** ✅

### 2.2 Flyway naming validity

`V1__baseline.sql` — prefix `V`, version `1`, two underscores, description `baseline`, `.sql` extension. Valid per Flyway convention. ✅

### 2.3 Configuration audit (application.yaml)

| Setting | Value | Assessment |
|---|---|---|
| `spring.flyway.enabled` | `true` | ✅ Flyway active |
| `spring.flyway.locations` | `classpath:db/migration` | ✅ Points to exactly one source directory |
| `spring.flyway.baseline-on-migrate` | `false` | ✅ Will not silently baseline a dirty schema |
| `spring.flyway.validate-on-migrate` | `true` | ✅ Checksums verified on each run |
| `spring.flyway.out-of-order` | `false` | ✅ No silent out-of-order execution |
| `spring.jpa.hibernate.ddl-auto` | `validate` | ✅ No schema mutation by Hibernate |
| `spring.flyway.clean-disabled` | *(not set)* | ⚠️ See note below |

**`flyway.clean-disabled` not explicitly configured.** In Flyway 9+ (and Flyway 12 used here via `spring-boot-starter-flyway`), `cleanDisabled` defaults to `true`, so `flyway:clean` cannot accidentally run. However, without an explicit `clean-disabled: true` in `application.yaml`, a future property change or version downgrade could silently re-enable it. **Recommendation: add `spring.flyway.clean-disabled: true` explicitly before any production use.** Not a hard blocker for Phase C (current default is safe), but should be added in Phase B.1.

### 2.4 Why `ignore-migration-patterns` exists (supabase profile)

The Supabase `flyway_schema_history` table (created before Phase B) contains entries for V1 through V12, all with `success = true`. V2 through V12 source files were consolidated into the single new `V1__baseline.sql` and deleted. Without `ignore-migration-patterns: "*:missing"`, Flyway's `validate-on-migrate: true` would fail at startup with `Detected resolved migration not applied to database: V1`. The pattern suppresses the "missing" classification for those history entries so the supabase-profile application can start while the old Supabase history table still exists.

**Should `ignore-migration-patterns` remain after Phase C reset?** **NO.** After the reset, the history table will be empty and Flyway will apply V1__baseline.sql cleanly. The `"*:missing"` pattern becomes unnecessary and masks future real migration drift. It must be removed from `application.yaml` immediately after Phase C executes successfully.

### 2.5 Stale comment in application.yaml

Line 19 reads:
```yaml
# Disable checksum validation for baseline entry (V1 was a Flyway baseline, not a run)
out-of-order: false
```

This comment is **incorrect and misleading**. No checksum validation is being disabled. `validate-on-migrate: true` is active. The comment belongs to a removed setting and should be deleted. Not a blocker, but creates confusion.

### 2.6 Reset script safety audit (`supabase-reset.sql`)

| Requirement | Present | Assessment |
|---|---|---|
| Comment warning header | ✅ | Present, clear |
| Checklist of safety conditions | ✅ | 8-item checklist |
| `flyway_schema_history` drop | ✅ | `DROP TABLE IF EXISTS public.flyway_schema_history` |
| Schema drop + recreate | ✅ | `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` |
| Host/database redaction | ✅ | Script contains no credentials or DSN |
| **Confirmation environment variable guard** | ❌ | **ABSENT — BLOCKER** |
| Backup verification step | ❌ | Not in script (no data to back up currently, but guard is still required) |
| Fail-fast behavior | ❌ | No `DO $$ ... END $$` block; SQL runs unconditionally |

**BLOCKER — Reset script missing confirmation guard.** The requirement states the script must not execute unless `CAREBRIDGE_DB_RESET_CONFIRMATION=RESET_SHARED_DEV_DATABASE` is set. The current script has no such guard — any user who copies and pastes it into the Supabase SQL editor will drop the entire schema. A PostgreSQL `DO` block can check a GUC parameter set by the caller:

```sql
DO $$
BEGIN
  IF current_setting('app.reset_confirmation', true) <> 'RESET_SHARED_DEV_DATABASE' THEN
    RAISE EXCEPTION 'Safety guard: set app.reset_confirmation=''RESET_SHARED_DEV_DATABASE'' before running this script';
  END IF;
END $$;
```

This guard must be added before Phase C is approved.

---

## 3. Authentication and Session Audit

### 3.1 Web token persistence

**`authStore.ts`** uses `zustand/persist` with `{ name: 'carebridge-auth' }`. Zustand persist writes to `localStorage.setItem('carebridge-auth', JSON.stringify({state:{accessToken,refreshToken,user}, version:0}))`.

**`apiClient.ts`** (shared API client used by all feature pages) reads:
```ts
const token = localStorage.getItem('accessToken');
```

This reads from a **different key** (`accessToken`) than the one zustand writes to (`carebridge-auth`). After a successful OTP login via `OtpPage.tsx`, the tokens are stored in `localStorage['carebridge-auth']` only. `localStorage['accessToken']` remains `null`.

**Result**: every API call from `ModerationQueuePage`, `ManageTopicsPage`, `CreateContentPage`, `CreatePartnerProfilePage`, and `AccountProfilePage` will receive `Authorization: null` headers — the backend will respond `401 Unauthorized`. The web portal is non-functional after login.

Additionally, `src/features/auth/services/authApi.ts` (used by `AccountProfilePage`) contains a second axios instance also reading `localStorage.getItem('accessToken')` — same broken key.

**Classification: BLOCKER**

**Fix required (Phase B.1)**: either
- (a) Update `apiClient.ts` and `authApi.ts` interceptors to read from the zustand store: `useAuthStore.getState().accessToken`, or
- (b) Add `localStorage.setItem('accessToken', accessToken)` in `OtpPage.tsx` after `setTokens()`

Option (a) is architecturally clean. Option (b) maintains two sources of truth (risk of drift) but is simpler.

### 3.2 Web logout

`AdminLayout.tsx` calls `useAuth().logout()` → `useAuthStore.logout()` → sets `{accessToken: null, ...}` → zustand persist writes the cleared state back to `localStorage['carebridge-auth']`. The zustand `isAuthenticated()` check then returns `false` and `ProtectedRoute` redirects to `/login`. **Logout is correct on the auth state side**, but because `apiClient.ts` never reads from `carebridge-auth`, it is irrelevant until the token storage gap (3.1) is fixed.

### 3.3 Flutter token persistence after app restart

`AuthState` is an in-memory singleton with `ChangeNotifier`. No `SharedPreferences`, `flutter_secure_storage`, or file-based persistence is used. After any app restart or process kill, `AuthState.instance.isAuthenticated` returns `false` and `main.dart` routes to `LoginScreen`.

**Classification: PARTIAL BLOCKER** (production severity). For the academic demo context, the user must re-authenticate after each restart. For a formal release, `shared_preferences: ^2.x` must be added to `pubspec.yaml` and tokens must be written and read from secure storage on init. This should be addressed in Phase B.1 alongside the web fix, as both affect the same auth foundation.

### 3.4 Flutter logout

No logout action is currently wired in the mobile `MainShell`. `AuthState.instance.clear()` must be called from a settings screen to log out. The `ChangeNotifier` will fire, `ListenableBuilder` in `main.dart` will rebuild, and `LoginScreen` will be shown. The mechanism is correct once a logout button is added.

### 3.5 Disabled and locked user enforcement

`AuthenticationPolicy.ensureCanAuthenticate(user)`:
- `!user.isEnabled()` → throws `AuthenticationException("Account is disabled")` → HTTP 401
- `user.isLocked()` → throws `AuthenticationException("Account is locked")` → HTTP 401

This is called in `AuthServiceImpl.login()` (line 88), `verifyOtp()` (line 107), and `refresh()` (line 132). **All entry points are guarded.** ✅

Frontend handling: both web `ProtectedRoute` and mobile `ListenableBuilder` check token presence but do not explicitly handle 401 responses from refresh calls. A disabled user whose token expires will be redirected to login on next route access. No special "account disabled" screen exists — acceptable for MVP.

### 3.6 Secrets and sensitive data in logs / UI

- No `console.log` calls in web feature pages or shared auth files. ✅
- Mobile screens show generic error messages ("Invalid or expired OTP", "Network error") without echoing raw API error body. ✅
- `authApi.ts` does not log tokens. ✅
- `api_client.dart` does not log tokens. ✅
- Default JWT secret in `application.yaml` is the insecure dev fallback `carebridge-local-dev-secret-key-must-be-at-least-32-characters`. This is only used when `JWT_SECRET` env var is not set. For Supabase (production profile), `JWT_SECRET` must be set via environment. Not a blocker if the Supabase deployment uses `.env`. **Must be verified before shared deploy.**

---

## 4. Authorization and Guest-Access Audit

### 4.1 Public GET endpoints (no JWT required)

| Path | Method | Reason |
|---|---|---|
| `/api/v1/auth/register` | POST | Self-registration |
| `/api/v1/auth/login` | POST | Login entry point |
| `/api/v1/auth/verify-otp` | POST | OTP step |
| `/api/v1/auth/refresh` | POST | Token refresh |
| All `OPTIONS /**` | OPTIONS | CORS preflight |

All other `/api/v1/**` paths require a valid JWT.

### 4.2 Role-gated endpoints

| Endpoint | Method | Required Role | Source |
|---|---|---|---|
| `/api/v1/admin/audit-logs` | GET | `SYSTEM_ADMIN` | SecurityConfig |
| `/api/v1/admin/moderation/queue` | GET | `MODERATOR` | SecurityConfig + `@PreAuthorize("hasRole('MODERATOR')")` on controller |
| `/api/v1/admin/content` | POST | `CONTENT_ADMIN` | SecurityConfig + `@PreAuthorize("hasRole('CONTENT_ADMIN')")` on class |
| `/api/v1/community/topics` | POST | `MODERATOR` | SecurityConfig + `@PreAuthorize("hasRole('MODERATOR')")` |
| `/api/v1/community/topics/{id}` | PATCH | `MODERATOR` | SecurityConfig + `@PreAuthorize("hasRole('MODERATOR')")` |
| `/api/v1/partner/profile` | POST | `PARTNER` | SecurityConfig + `@PreAuthorize("hasRole('PARTNER')")` on class |
| `/api/v1/community/questions` | POST | `MOTHER` | `@PreAuthorize("hasRole('MOTHER')")` only (not in SecurityConfig) |
| `/api/v1/community/questions/{id}/answers` | POST | any authenticated | `@PreAuthorize("isAuthenticated()")` |

### 4.3 Endpoints requiring JWT but no specific role

All `/api/v1/**` paths not listed above require authentication but no specific role. This includes:
- `GET /api/v1/content/**` — any authenticated user can read content
- `GET /api/v1/community/feed` — any authenticated user
- `GET /api/v1/community/topics` — any authenticated user
- `GET /api/v1/community/questions` — any authenticated user (UC-162 search)
- `POST /api/v1/rag/answer` — any authenticated user (no `@PreAuthorize` on `RagController`)
- `GET /api/v1/auth/profile` — any authenticated user

### 4.4 Role access matrix

| Role | Community Read | Community Write | Content Read | Moderation | Admin Content | Partner | RAG |
|---|---|---|---|---|---|---|---|
| `MOTHER` | ✅ | ✅ (POST question) | ✅ | ❌ | ❌ | ❌ | ✅ |
| `FAMILY` | ✅ | ✅ (POST answer only) | ✅ | ❌ | ❌ | ❌ | ✅ |
| `EXPERT` | ✅ | ✅ (POST answer only) | ✅ | ❌ | ❌ | ❌ | ✅ |
| `MODERATOR` | ✅ | ✅ (POST topic, PATCH topic) | ✅ | ✅ | ❌ | ❌ | ✅ |
| `CONTENT_ADMIN` | ✅ | ❌ | ✅ | ❌ | ✅ | ❌ | ✅ |
| `SYSTEM_ADMIN` | ✅ | ❌ | ✅ | ✅ | ✅ | ❌ | ✅ |
| `PARTNER` | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ |

### 4.5 Frontend/mobile guard vs backend alignment

| Access Rule | Backend | Web Guard | Mobile Guard | Match? |
|---|---|---|---|---|
| Unauthenticated → redirect to login | `HttpStatusEntryPoint(401)` | `ProtectedRoute` checks `isAuthenticated` | `ListenableBuilder` on `AuthState.isAuthenticated` | ✅ Structural match |
| MODERATOR path | `hasRole('MODERATOR')` in SecurityConfig | `ProtectedRoute requiredRoles=['MODERATOR','SYSTEM_ADMIN']` | No mobile moderator screen | ✅ (no mobile screen) |
| CONTENT_ADMIN path | `hasRole('CONTENT_ADMIN')` | `ProtectedRoute requiredRoles=['CONTENT_ADMIN','SYSTEM_ADMIN']` | No mobile admin screen | ✅ (no mobile screen) |
| PARTNER path | `hasRole('PARTNER')` | `ProtectedRoute requiredRoles=['PARTNER','SYSTEM_ADMIN']` | No mobile partner screen | ✅ (no mobile screen) |

### 4.6 Conflict: public content requirement vs JWT-only GET

The previous SecurityConfig permitted `/api/v1/content/**` GET without JWT (public read). Phase B changed this to require JWT, so that backend tests pass. The mobile `view_content_screen.dart` and `search_content_screen.dart` call these GET endpoints via `apiGet()` which now auto-injects the token from `AuthState`. After login, this works. Without login (app restart, no persistence), the call will receive 401 and the screen will display an error.

**Potential conflict**: If the product requirement is that content articles should be publicly browsable (before login), the current backend now blocks that. If the requirement is login-gated content, the current implementation is correct. This should be confirmed by the product owner before Phase C.

---

## 5. API/UI Wiring Audit

### UC-54 — Create Community Question

| Item | Detail |
|---|---|
| Backend endpoint | `POST /api/v1/community/questions` |
| Required role | `MOTHER` (via `@PreAuthorize("hasRole('MOTHER')")`) |
| Database tables | `community_questions`, `community_topics` |
| Web screen | ❌ No web page exists for this UC |
| Mobile screen | ✅ `create_question_screen.dart` calls `apiPost('/api/v1/community/questions', {...})` without explicit token (auto-injected since Phase B) |
| Real API or mocked | Real API wired |
| Test coverage | `CommunityQuestionControllerTest` (18 tests, MockMvc), `CommunityQuestionServiceImplTest` (6 tests, Mockito) |
| Known limitation | Mobile screen does not confirm role before submission; backend will return 403 if user role is not MOTHER. No error message for role mismatch is displayed. |

### UC-56 — Post Community Answer

| Item | Detail |
|---|---|
| Backend endpoint | `POST /api/v1/community/questions/{questionId}/answers` |
| Required role | Any authenticated user (`isAuthenticated()`) |
| Database tables | `community_answers`, `community_questions`, `contribution_points` |
| Web screen | ❌ No web page |
| Mobile screen | ✅ `post_answer_screen.dart` exists; uses `apiPost` |
| Real API or mocked | Real API |
| Test coverage | `CommunityAnswerControllerTest` (8 tests), `CommunityAnswerServiceImplTest` (6 tests) |
| Known limitation | No expert-label differentiation in mobile answer display |

### UC-82 — View Content and Checklist

| Item | Detail |
|---|---|
| Backend endpoint | `GET /api/v1/content/{id}`, `GET /api/v1/content/checklists` |
| Required role | Any authenticated |
| Database tables | `content_items`, `checklist_templates`, `checklist_items` |
| Web screen | ❌ No web content view page |
| Mobile screen | ✅ `view_content_screen.dart` calls `apiGet('/api/v1/content/{id}')` |
| Real API or mocked | Real API |
| Test coverage | `ContentControllerTest` (7 tests), `ContentServiceImplTest` (7 tests), `ContentIntegrationTest` (4 tests) |
| Known limitation | Mobile screen does not display checklist; only article body is shown |

### UC-99 — View Moderation Queue

| Item | Detail |
|---|---|
| Backend endpoint | `GET /api/v1/admin/moderation/queue` (paginated, filterable by contentType) |
| Required role | `MODERATOR` |
| Database tables | `content_reports`, `moderation_actions`, `content_items`, `community_questions`, `community_answers` |
| Web screen | ✅ `ModerationQueuePage.tsx` calls `GET /api/v1/admin/moderation/queue` with pagination |
| Mobile screen | ❌ No mobile moderator screen |
| Real API or mocked | Real API |
| Test coverage | `ModerationQueueIntegrationTest` (3), `ModerationControllerTest` (4), `ModerationControllerSecurityTest` (5), `ModerationServiceImplTest` (4) |
| Known limitation | Web `ModerationItem.id` typed as `number` but backend emits `UUID` string. Runtime serialization works (JSON string passes TypeScript), but TypeScript strict mode would flag `id: number` assignments. See Section 5 note below. |

### UC-105 — Create Content / FAQ / Checklist

| Item | Detail |
|---|---|
| Backend endpoint | `POST /api/v1/admin/content` |
| Required role | `CONTENT_ADMIN` |
| Database tables | `content_items`, `checklist_templates`, `checklist_items` |
| Web screen | ✅ `CreateContentPage.tsx` calls `POST /api/v1/admin/content` with form data |
| Mobile screen | ❌ No mobile admin screen |
| Real API or mocked | Real API |
| Test coverage | `AdminContentControllerTest` (8), `AdminContentServiceImplTest` (8) |
| Known limitation | None at current scope |

### UC-109 — Manage Community Topics

| Item | Detail |
|---|---|
| Backend endpoint | `GET /api/v1/community/topics`, `POST /api/v1/community/topics`, `PATCH /api/v1/community/topics/{id}` |
| Required role | GET: any authenticated; POST/PATCH: `MODERATOR` |
| Database tables | `community_topics` |
| Web screen | ✅ `ManageTopicsPage.tsx` — calls GET (list) and PATCH (toggle hidden) |
| Mobile screen | ❌ No moderator mobile screen |
| Real API or mocked | Real API |
| Test coverage | `CommunityTopicControllerTest` (9), `CommunityTopicServiceImplTest` (9) |
| Known limitation | `Topic.id` in TypeScript typed as `number`; backend emits UUID string. The PATCH `/topics/${topic.id}` call will send the UUID as a string in the URL — correct — but the TypeScript type is wrong. |

### UC-118 — Create Partner Profile

| Item | Detail |
|---|---|
| Backend endpoint | `POST /api/v1/partner/profile` |
| Required role | `PARTNER` |
| Database tables | `partner_organizations` |
| Web screen | ✅ `CreatePartnerProfilePage.tsx` submits form to this endpoint |
| Mobile screen | ❌ No mobile partner screen |
| Real API or mocked | Real API |
| Test coverage | `PartnerProfileControllerTest` (15), `PartnerProfileControllerSecurityTest` (10), `PartnerProfileServiceImplTest` (7), `PartnerProfileIntegrationTest` (3) |
| Known limitation | None at current scope |

### UC-132 — Generate RAG Answer

| Item | Detail |
|---|---|
| Backend endpoint | `POST /api/v1/rag/answer` |
| Required role | Any authenticated (no `@PreAuthorize`; covered by `.requestMatchers("/api/v1/**").authenticated()`) |
| Database tables | `triage_assessments`, `triage_answers` (for context retrieval); Gemini API for generation |
| Web screen | ❌ No web RAG screen |
| Mobile screen | ✅ `rag_chat_screen.dart` calls `apiPost('/api/v1/rag/answer', {'query': ...})` |
| Real API or mocked | Real API (Gemini); falls back to safe message when `GEMINI_API_KEY` is empty |
| Test coverage | `RagControllerTest` (6, WebMvcTest), `RagServiceTest` (8, Mockito), `ContentItemContextRetrieverTest` (4, Mockito) |
| Known limitation | `RagController` has no `@PreAuthorize` — currently falls through to global `authenticated()`. No role restriction. Any authenticated user (including FAMILY, PARTNER) can access RAG. Should add `@PreAuthorize("isAuthenticated()")` explicitly for documentation clarity. |

### UC-162 — Search Community Questions

| Item | Detail |
|---|---|
| Backend endpoint | `GET /api/v1/community/questions?keyword=&topicId=&page=&size=` |
| Required role | Any authenticated |
| Database tables | `community_questions`, `community_topics` |
| Web screen | ❌ No web search page |
| Mobile screen | ✅ `search_questions_screen.dart` calls `apiGet('/api/v1/community/questions?...')` |
| Real API or mocked | Real API |
| Test coverage | `CommunityQuestionControllerTest` (18), `CommunityQuestionSearchServiceImplTest` (6) |
| Known limitation | None |

### UC-198 — View Community Feed

| Item | Detail |
|---|---|
| Backend endpoint | `GET /api/v1/community/feed?page=&size=` |
| Required role | Any authenticated |
| Database tables | `community_questions`, `community_answers`, `community_topics` |
| Web screen | ❌ No web feed page |
| Mobile screen | ✅ `community_feed_screen.dart` calls `apiGet('/api/v1/community/feed?page=$_page&size=20')` (token auto-injected) |
| Real API or mocked | Real API |
| Test coverage | `CommunityFeedControllerTest` (6), `CommunityFeedServiceImplTest` (4) |
| Known limitation | Feed requires JWT. After app restart without persistence, user sees 401 error until re-login (see Section 3.3). |

### UC-224 — Search Verified Content

| Item | Detail |
|---|---|
| Backend endpoint | `GET /api/v1/content/search?keyword=&type=&stage=&page=&size=` |
| Required role | Any authenticated |
| Database tables | `content_items` |
| Web screen | ❌ No web search page |
| Mobile screen | ✅ `search_content_screen.dart` calls `apiGet('/api/v1/content/search?...')` |
| Real API or mocked | Real API |
| Test coverage | `ContentSearchControllerTest` (12), `ContentSearchServiceTest` (5), `ContentSearchSecurityTest` (1), `ContentSearchIntegrationTest` (4) |
| Known limitation | None |

### ID Type Mismatch Note (affects UC-99, UC-109)

Three web TypeScript interfaces define `id: number` while the backend emits UUID strings:
- `ModerationItem.id: number` (backend: `UUID id` in `ModerationQueueItemResponse`)
- `Topic.id: number` (backend: `UUID id` in `CommunityTopic` entity)
- Historical: `UserProfile.id: number` → **already fixed to `string`** in Phase B

UUID strings serialize transparently through JSON and TypeScript will coerce them at runtime, so functional API calls work. However, TypeScript's strict mode would emit errors on explicit number operations. These are **non-blocking but must be fixed** when those features go to production.

---

## 6. Test Quality Audit

### 6.1 Test count by module and type

| Module | Class | Type | Tests |
|---|---|---|---|
| **auth** | `AuthServiceGetProfileTest` | Mockito unit | 5 |
| **auth** | `UserMapperTest` | Pure unit (no annotation) | 11 |
| **content** | `ContentMapperTest` | Pure unit | 6 |
| **content** | `ContentServiceImplTest` | Mockito unit | 7 |
| **content** | `ContentSearchServiceTest` | Mockito unit | 5 |
| **content** | `AdminContentServiceImplTest` | Mockito unit | 8 |
| **content** | `ContentControllerTest` | WebMvcTest + Mockito | 7 |
| **content** | `ContentSearchControllerTest` | WebMvcTest + Mockito | 12 |
| **content** | `AdminContentControllerTest` | WebMvcTest + Mockito | 8 |
| **content** | `ContentSecurityTest` | WebMvcTest (security) | 5 |
| **content** | `ContentSearchSecurityTest` | WebMvcTest (security) | 1 |
| **content** | `ContentIntegrationTest` | WebMvcTest (named "integration") | 4 |
| **content** | `ContentSearchIntegrationTest` | WebMvcTest (named "integration") | 4 |
| **community** | `CommunityFeedMapperTest` | Pure unit | 4 |
| **community** | `CommunityQuestionMapperTest` | Pure unit | 4 |
| **community** | `CommunityTopicServiceImplTest` | Mockito unit | 9 |
| **community** | `CommunityQuestionServiceImplTest` | Mockito unit | 6 |
| **community** | `CommunityAnswerServiceImplTest` | Mockito unit | 6 |
| **community** | `CommunityFeedServiceImplTest` | Mockito unit | 4 |
| **community** | `CommunityQuestionSearchServiceImplTest` | Mockito unit | 6 |
| **community** | `CommunityTopicControllerTest` | WebMvcTest | 9 |
| **community** | `CommunityFeedControllerTest` | WebMvcTest | 6 |
| **community** | `CommunityAnswerControllerTest` | WebMvcTest | 8 |
| **community** | `CommunityQuestionControllerTest` | WebMvcTest | 18 |
| **moderation** | `ModerationMapperTest` | Pure unit | 3 |
| **moderation** | `ContentPreviewServiceTest` | Mockito unit | 5 |
| **moderation** | `ModerationServiceImplTest` | Mockito unit | 4 |
| **moderation** | `ModerationControllerTest` | WebMvcTest | 4 |
| **moderation** | `ModerationControllerSecurityTest` | WebMvcTest (security) | 5 |
| **moderation** | `ModerationQueueIntegrationTest` | WebMvcTest (named "integration") | 3 |
| **partner** | `PartnerProfileServiceImplTest` | Mockito unit | 7 |
| **partner** | `PartnerProfileControllerTest` | WebMvcTest | 15 |
| **partner** | `PartnerProfileControllerSecurityTest` | WebMvcTest (security) | 10 |
| **partner** | `PartnerProfileIntegrationTest` | WebMvcTest (named "integration") | 3 |
| **integration/gemini** | `RagServiceTest` | Mockito unit | 8 |
| **integration/gemini** | `ContentItemContextRetrieverTest` | Mockito unit | 4 |
| **integration/gemini** | `RagControllerTest` | WebMvcTest | 6 |
| **application** | `BackendApplicationTests` | `@SpringBootTest` context load | 1 |
| **TOTAL** | | | **241** |

### 6.2 Test type breakdown

| Type | Description | Count (approx) |
|---|---|---|
| `@SpringBootTest` context load | Full application context against local dev PostgreSQL | 1 |
| `@WebMvcTest` + `@MockitoBean` | HTTP stack (security → controller → mocked service) | ~150 |
| `@ExtendWith(MockitoExtension)` | Pure service/mapper unit tests, no Spring context | ~90 |

### 6.3 Note on "integration" test naming

`ContentIntegrationTest`, `ContentSearchIntegrationTest`, `ModerationQueueIntegrationTest`, and `PartnerProfileIntegrationTest` are labeled "integration" but use `@WebMvcTest` with `@MockitoBean`. They test the HTTP + security + controller layer with mocked services and repositories. **No database connection is made.** These are MockMvc integration tests (HTTP layer only), not true end-to-end integration tests.

### 6.4 V1 migration validation against PostgreSQL

The `@SpringBootTest` `contextLoads` test starts the full application context against the local dev PostgreSQL container (`compose.yaml`). Flyway runs V1__baseline.sql on startup. Hibernate `ddl-auto: validate` then verifies each mapped entity's table and columns exist. If validation passes, `contextLoads` passes. **This is the only automated database-level validation of V1__baseline.sql.**

`./mvnw test` was run and confirmed: **1 test passed** for `BackendApplicationTests` → V1__baseline.sql is valid against the dev PostgreSQL schema.

### 6.5 Supabase not touched during tests

No test file references `SUPABASE_*` environment variables or the supabase Spring profile. `BackendApplicationTests` runs against `SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/carebridge` (dev profile default). **Supabase was not touched during test execution.** ✅

### 6.6 Critical untested paths

| Path | Risk |
|---|---|
| Auth registration → OTP → login full flow | No end-to-end test; service and controller tested separately |
| Refresh token rotation (replay attack) | No token reuse test |
| Consent grant enforcement on community/content read | `ConsentService` exists but no tests asserting blocked reads |
| `AuthenticationPolicy.ensureCanAuthenticate` with locked user in HTTP flow | Covered by service test but no MockMvc test asserting 401 HTTP response |
| V1 migration against Supabase schema | Only tested against local PostgreSQL; Supabase has different schema state (72 tables + history entries V1-V12) |
| Web portal API calls after login | Non-functional due to token storage gap (Section 3.1) |
| Flutter community/content screens after app restart | Receive 401; no test |

### 6.7 Exact commands executed during audit

```
./mvnw test  (CareBridgeAPI)
→ Tests run: 241, Failures: 0, Errors: 0, Skipped: 0
→ BUILD SUCCESS

npm run build  (CareBridgeWebApp)
→ tsc -b && vite build
→ ✓ built in 104ms (93 modules, 0 TypeScript errors)

flutter build apk --debug  (CareBridgeMobileApp)
→ ✓ Built build/app/outputs/flutter-apk/app-debug.apk
```

---

## 7. Final Decision

### Blockers

Two concrete blockers prevent approval of the shared Supabase reset.

---

#### BLOCKER 1 — Web Portal Token Storage Dual-Gap

**Location**: `src/shared/api/apiClient.ts:9` and `src/features/auth/services/authApi.ts:10`

**Problem**: `apiClient.ts` reads `localStorage.getItem('accessToken')`. `authStore.ts` (via zustand persist) writes tokens to `localStorage['carebridge-auth']`. After login via `OtpPage.tsx`, the `accessToken` key is never written. All feature page API calls (`ModerationQueuePage`, `ManageTopicsPage`, `CreateContentPage`, `CreatePartnerProfilePage`, `AccountProfilePage`) send no Authorization header and receive 401. The web portal is non-functional post-login.

**Required Phase B.1 fix**: Update `apiClient.ts` line 9 from `localStorage.getItem('accessToken')` to `useAuthStore.getState().accessToken`, and apply the same fix to `authApi.ts`.

---

#### BLOCKER 2 — Reset Script Missing Confirmation Guard

**Location**: `docs/plans/claude/supabase-reset.sql`

**Problem**: The script has a checklist comment but no executable guard. Any user can paste the file into the Supabase SQL editor and run `DROP SCHEMA public CASCADE` unconditionally. The requirement states the script must not execute unless `CAREBRIDGE_DB_RESET_CONFIRMATION=RESET_SHARED_DEV_DATABASE` is set.

**Required Phase B.1 fix**: Add a `DO $$ BEGIN ... END $$;` guard block at the top of the script that checks a PostgreSQL GUC parameter (set by the caller immediately before execution) and raises an exception if it is not set to the expected value.

---

### Non-Blocking Issues (must be tracked)

| Issue | Severity | Recommended Phase |
|---|---|---|
| Flutter `AuthState` in-memory only (no persistence after restart) | MEDIUM | B.1 (add `shared_preferences`) |
| `flyway.clean-disabled: true` not explicit in config | LOW | B.1 |
| Stale misleading comment in `application.yaml` line 19 | LOW | B.1 |
| `Topic.id: number` and `ModerationItem.id: number` TypeScript type mismatch | MEDIUM | B.1 |
| `RagController` has no explicit `@PreAuthorize` annotation | LOW | B.1 |
| `ignore-migration-patterns: "*:missing"` must be removed after Phase C | MEDIUM | Phase C post-reset |
| Web portal public-content vs JWT-only GET requires product confirmation | MEDIUM | Product decision |
| JWT default fallback secret used if `JWT_SECRET` not set in Supabase env | MEDIUM | Phase C preflight |

---

```
BLOCKED_BEFORE_SHARED_DB_RESET
```

**Minimum required Phase B.1 fixes before re-audit:**

1. Fix `apiClient.ts` and `authApi.ts` token injection to read from `useAuthStore.getState().accessToken`
2. Add confirmation guard to `supabase-reset.sql`

After those two fixes: re-run `./mvnw test` and `npm run build`, then request a new release audit.
