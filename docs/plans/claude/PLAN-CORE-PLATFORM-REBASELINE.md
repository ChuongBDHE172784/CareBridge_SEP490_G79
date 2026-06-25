---
Status: DRAFT
Approval: PENDING_USER_APPROVAL
Allowed: READ_ONLY_ANALYSIS_ONLY
Created: 2026-06-25
Author: AI Agent (Phase A Discovery)
---

# CareBridge Core Platform — Rebaseline Plan

---

## Section A: Repository Inventory

### A.1 Backend Source Files

| Category | Count | Key Notes |
|---|---|---|
| Java source files | ~80 | Under `com.carebridge.backend` |
| Domain packages with entities | ~20 | community, content, partner, security, audit, consent, triage, identity, etc. |
| Domain packages empty / scaffold-only | ~13 | babycare, carejourney, carecoordination, consultation, expert, payment, emergency, safety, exercise, device, reminder, healthrecord, partner (controllers only for some) |
| Test files | 38 | See A.4 |
| Migration files | 12 | V1–V12, see Section B |

### A.2 Implemented Java Entities (have real entity classes)

| Package | Entities |
|---|---|
| `security.entity` | `User`, `RefreshToken`, `OtpVerification` |
| `audit.entity` | `AuditLog`, `SecurityEvent` |
| `consent.entity` | `ConsentGrant` |
| `identity.entity` | `Role`, `UserRole`, `UserSession`, `CommunityProfile`, `NotificationPreference`, `Notification`, `DataPermission` |
| `community.entity` | `CommunityTopic`, `CommunityQuestion`, `CommunityAnswer`, `ContributionPoint` |
| `content.entity` | `ContentItem`, `ContentReport`, `ModerationAction`, `ChecklistTemplate`, `ChecklistItem` |
| `partner.entity` | `PartnerOrganization`, `PartnerExpertLink`, `PartnerService`, `SponsoredCampaign`, `CareFacility` |
| `triage.entity` | `TriageAssessment`, `TriageAnswer` |

> All other packages (carejourney, babycare, healthrecord, carecoordination, consultation, expert, payment, emergency, safety, exercise, device, reminder) have entity class stubs or no entities at all.

### A.3 Active Controllers and Services

| Module | Controller | Service |
|---|---|---|
| Auth | `AuthController` | `AuthServiceImpl` |
| Community | `CommunityTopicController`, `CommunityQuestionController`, `CommunityAnswerController`, `CommunityFeedController` | `CommunityTopicServiceImpl`, etc. |
| Content | `AdminContentController` | `AdminContentServiceImpl` |
| Moderation | `ModerationController` | `ModerationServiceImpl` |
| Partner | `PartnerProfileController` | `PartnerProfileServiceImpl` |
| RAG/Gemini | `RagController` | `RagServiceImpl` |

### A.4 Test Files (38 total, by module)

| Module | Test Files |
|---|---|
| `community` | CommunityTopicControllerTest, CommunityTopicServiceImplTest, CommunityAnswerControllerTest, CommunityFeedControllerTest, CommunityQuestionControllerTest, mapper tests, service tests |
| `content` | AdminContentControllerTest, AdminContentServiceImplTest, ContentIntegrationTest, ContentSearchIntegrationTest, ContentSecurityTest, ContentSearchSecurityTest, unit tests |
| `integration/gemini` | ContentItemContextRetrieverTest, RagControllerTest, RagServiceTest |
| `moderation` | ContentPreviewServiceTest, ModerationControllerSecurityTest, ModerationControllerTest, ModerationMapperTest, ModerationQueueIntegrationTest, ModerationServiceImplTest |
| `partner` | PartnerProfileControllerSecurityTest, PartnerProfileControllerTest, PartnerProfileServiceImplTest, PartnerProfileIntegrationTest |
| `security` | UserMapperTest, AuthServiceGetProfileTest |

### A.5 Frontend State

**Web Portal** (`05_Development/CareBridgeWebApp`):
- React 19 + TypeScript + Vite 8
- `app/router`, `app/guards`, `app/providers`, `app/layouts` are **all empty** (only `.gitkeep`)
- `App.tsx` uses a manual tab-state pattern — **no react-router-dom wired**
- `shared/auth`, `shared/components`, `shared/hooks` are **empty**
- Functional pages exist: ModerationQueuePage, ManageTopicsPage, CreateContentPage, CreatePartnerProfilePage, AccountProfilePage

**Mobile App** (`05_Development/CareBridgeMobileApp`):
- Flutter with Material 3 / Warm Claymorphism theme
- `MainShell` in `main.dart` uses a 5-tab BottomNavigationBar — **no login screen, no auth state**
- `core/network/api_client.dart` has no token management
- 7 functional screens exist across community, healthRecords, and aiTriage features

### A.6 Key Configuration

| Setting | Current Value | Required Value |
|---|---|---|
| `spring.flyway.enabled` | `false` | `true` (Phase B) |
| `spring.jpa.hibernate.ddl-auto` | `update` | `validate` (Phase B) |
| Active profile (from .env) | `supabase` | `supabase` |
| DB credentials | Via `SUPABASE_DB_*` env vars | Unchanged |
| `.env.example` | **DOES NOT EXIST** | Must be created (Phase B) |

---

## Section B: Migration Reconciliation Matrix

### B.1 How the Live Schema Was Built

The current Supabase schema was **NOT** built by Flyway. The actual sequence:

1. Hibernate `ddl-auto: update` was run — it created all tables from all entity classes (including all 72 tables in the full ERD through entity definitions)
2. `flyway baseline` was run — this inserted a V1 row in `flyway_schema_history` marked as `<< Flyway Baseline >>`, meaning "the schema as-it-exists is version 1" — the V1 SQL file was **never executed**
3. V2–V12 were applied on top of the Hibernate-created baseline

**Live Flyway history (verified against Supabase):**

| Version | Description | Applied | Success |
|---|---|---|---|
| 1 | `<< Flyway Baseline >>` | 2026-06-24 | ✅ (not run — marked as existing) |
| 2 | alter community topics | 2026-06-24 | ✅ |
| 3 | add stage to content items | 2026-06-24 | ✅ |
| 4 | create partner organizations | 2026-06-24 | ✅ |
| 5 | create community questions | 2026-06-24 | ✅ |
| 6 | add description to checklist templates | 2026-06-24 | ✅ |
| 7 | create community answers | 2026-06-24 | ✅ |
| 8 | add content search indexes | 2026-06-24 | ✅ |
| 9 | alter community user refs to uuid | 2026-06-24 | ✅ |
| 10 | alter partner representative to uuid | 2026-06-24 | ✅ |
| 11 | add enabled locked to users | 2026-06-24 | ✅ |
| 12 | add role to users | 2026-06-24 | ✅ |

### B.2 Why V1–V12 Are Incompatible on a Clean DB

If Flyway is enabled fresh and attempts to run V1 through V12 on an empty database, the following failures occur:

| Conflict | Root Cause |
|---|---|
| V5 fails after V1 | V1 already creates `community_questions`; V5 has `CREATE TABLE community_questions` (no `IF NOT EXISTS`) — **duplicate table error** |
| V7 fails after V1 | V1 already creates `community_answers`; V7 has `CREATE TABLE community_answers` — **duplicate table error** |
| V11 fails after V1 | V1 already has `enabled`, `locked` columns on `users` — **column already exists error** |
| V12 fails after V1 | V1 already has `role` column on `users` — **column already exists error** |
| V2 vs V1 | V1 has `topic_id`; V2 renames it to `id` — logical conflict with V5 which FKs to `community_topics(id)` |

### B.3 V1–V12 → New V1 Mapping

The new consolidated V1 must be derived **strictly from the current live schema**, not from concatenating the historical migration files.

| Historical Migration | Final Schema Destination in New V1 |
|---|---|
| V1 `users` shape | Preserved; decision needed on 4 extra columns (see B.4) |
| V1 `community_topics` (old shape) | **Superseded** by V2 shape; new V1 uses V2-final columns: `id`, `name`, `description`, `icon`, `is_hidden`, `sort_order`, `created_by`, `created_at`, `updated_at` |
| V1 `community_questions` (old shape) | **Superseded** by V5+V9 shape; new V1 uses final columns: `id`, `topic_id`, `author_id UUID`, `title`, `body`, `stage`, `pregnancy_week`, `baby_age_months`, `urgency`, `is_anonymous`, `status`, `like_count`, `answer_count`, `created_at`, `updated_at` |
| V1 `community_answers` (old shape) | **Superseded** by V7+V9 shape; new V1 uses final columns: `id`, `question_id`, `author_id UUID`, `body`, `is_expert_labeled`, `is_personal_experience`, `status`, `like_count`, `created_at`, `updated_at` |
| V3 | Stage column absorbed into `content_items` definition |
| V4 `partner_organizations` | New column set with `representative_user_id UUID`; V10 UUID change absorbed |
| V6 | `description` absorbed into `checklist_templates` definition |
| V8 | Index definitions included in new V1 |
| V9, V10 | UUID FKs already in table definitions above |
| V11, V12 | `enabled`, `locked`, `role` already in `users` definition |

### B.4 `users` Table — Extra Columns (Decision Required)

Live `users` table has 4 columns NOT present in the Java `User` entity:

| Extra Column | Type | In Entity? | Recommendation |
|---|---|---|---|
| `account_status` | VARCHAR | ❌ | **Drop from new V1** — entity uses `enabled`/`locked` booleans; `account_status` is redundant and unmapped |
| `email_verified` | BOOLEAN | ❌ | **Drop from new V1** — not used by any service or test |
| `phone_verified` | BOOLEAN | ❌ | **Drop from new V1** — not used by any service or test |
| `last_login_at` | TIMESTAMP | ❌ | **Drop from new V1** — not used by any service or test |

> **Decision for user approval**: Phase B will drop these 4 columns from the consolidated V1. Since the DB is empty (zero rows), this is a safe structural change. If you disagree and want to keep any of these for future use, annotate below.

### B.5 Role Name Mapping (Decision Required)

The code uses these role names; the CLAUDE.md architecture document uses different names:

| Code (`Role.java`) | CLAUDE.md | Recommendation |
|---|---|---|
| `MOTHER` | `MOTHER` | Match — keep |
| `FAMILY` | `FAMILY_MEMBER` | **Code wins** — preserve `FAMILY`; document as intentional deviation |
| `EXPERT` | `EXPERT` | Match — keep |
| `MODERATOR` | `MODERATOR` | Match — keep |
| `CONTENT_ADMIN` | `CONTENT_ADMIN` | Match — keep |
| `SYSTEM_ADMIN` | `ADMIN` | **Code wins** — preserve `SYSTEM_ADMIN`; document as intentional deviation |
| `PARTNER` | `PARTNER_REPRESENTATIVE` | **Code wins** — preserve `PARTNER`; document as intentional deviation |

> **Decision for user approval**: Phase B will keep code-side names (`FAMILY`, `SYSTEM_ADMIN`, `PARTNER`) as authoritative. No renames needed across SecurityConfig, JWT claims, or UI guards.

### B.6 Speculative ERD Tables (Decision Required)

The live Supabase has 72 tables total. 20 are backed by implemented entities. The remaining 52 were created by Hibernate from entity class stubs or planned ERD tables. These include: `consultation_bookings`, `expert_profiles`, `payment_transactions`, `baby_profiles`, `care_groups`, `health_records`, etc.

**Options:**
- **Option A (Recommended)**: Include all 72 tables in the consolidated V1. This matches the current live schema exactly. A clean reset produces an identical schema. Hibernate `validate` ignores tables without entity mappings.
- **Option B**: Include only the 20 entity-backed tables. Smaller V1, more honest baseline. Requires deleting/ignoring 52 tables from live DB on reset.

> **Recommendation**: Option A. Keep all 72 tables in new V1 to avoid discarding future entity scaffolding. Phase C reset will reproduce exact current schema. Hibernate `validate` is content as long as all mapped entity columns exist.

---

## Section C: Supabase Safety Report

### C.1 Verified Live State (Read-Only Introspection — 2026-06-25)

| Check | Result |
|---|---|
| Connection | ✅ PostgreSQL 17.6 (Supabase) |
| `flyway_schema_history` exists | ✅ YES — 12 rows, all `success = true` |
| V1 applied as baseline | ✅ `<< Flyway Baseline >>` — schema existed before Flyway took ownership |
| Total tables | 72 (including `flyway_schema_history`) |
| **Row count in ALL tested tables** | ✅ **ZERO — database is completely empty** |
| Extra `users` columns not in entity | ⚠️ `account_status`, `email_verified`, `phone_verified`, `last_login_at` |

### C.2 Data Loss Risk Assessment

| Risk | Assessment |
|---|---|
| User data loss on reset | **NONE** — zero rows in `users` |
| Community data loss | **NONE** — zero rows |
| Content data loss | **NONE** — zero rows |
| Operational data loss | **NONE** — all tables empty |
| Schema recovery risk | **LOW** — new V1 derived from live schema; reset reproduces identical structure |

### C.3 Reset Pre-Conditions for Phase C

The following must be true before Phase C executes:
- [ ] Phase B code changes compile (`./mvnw compile` passes)
- [ ] Phase B test suite passes (`./mvnw test` passes, 0 failures)
- [ ] New V1 migration file reviewed and approved by user
- [ ] `.env` is clean (no secrets committed to git)
- [ ] `git status` shows clean working tree on `HuyND`
- [ ] User has typed `APPROVE_SHARED_DB_RESET` in this session

### C.4 Reset Script (To Be Created in Phase B — NOT Executed)

Phase B will create `docs/plans/claude/supabase-reset.sql` containing:

```sql
-- GUARDED RESET: run only after APPROVE_SHARED_DB_RESET
-- Requires: zero live users, Phase B tests passing, V1 reviewed
-- Step 1: Drop Flyway history
DROP TABLE IF EXISTS flyway_schema_history;
-- Step 2: Drop all application tables (CASCADE handles FK order)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
-- Step 3: Flyway will apply new V1 on next application startup
```

> **IMPORTANT**: This script is documentation only in Phase B. Phase C executes it only after the safety checklist above is confirmed.

---

## Section D: Core Implementation Plan (Phase B)

All steps are ordered; each has a verify gate before proceeding to the next.

### D.1 Step Order

| Step | Action | Verify Gate |
|---|---|---|
| D.1.1 | Flyway + JPA config: enable Flyway, set `ddl-auto: validate`, configure for supabase profile | Application context loads without error against current schema |
| D.1.2 | Consolidate V1–V12 → new `V1__baseline.sql` | New V1 is self-consistent; no V2–V12 needed |
| D.1.3 | Delete V2–V12 migration files | `db/migration/` contains only new V1 |
| D.1.4 | Create `.env.example` (no real values) | File exists at `05_Development/CareBridgeAPI/.env.example` |
| D.1.5 | Fix `User` entity: remove unmapped columns from schema alignment (`account_status`, `email_verified`, `phone_verified`, `last_login_at`) | `validate` mode passes with current entity |
| D.1.6 | Add account state enforcement: `AuthenticationPolicy.ensureCanAuthenticate` already exists; wire it into login + refresh token flows | Login rejects locked/disabled accounts |
| D.1.7 | Fix ownership checks: add `@PreAuthorize` owner checks to community question/answer write endpoints | Non-owner delete attempt returns 403 |
| D.1.8 | Add Notification entity + service stub (identity module) | `NotificationRepository` exists; no test failures |
| D.1.9 | Add ConsentGrant enforcement hook in community/content read paths | Consent module wired; policy checked in service layer |
| D.1.10 | Preserve + repair community/content/partner/RAG modules: ensure all 38 existing tests compile and pass | `./mvnw test` → 0 failures |
| D.1.11 | Create Supabase reset script at `docs/plans/claude/supabase-reset.sql` (guarded, no execution) | File created with header guard and safety checklist |
| D.1.12 | Web Portal: wire react-router-dom v7 routing with role-aware guards and layout shells | `npm run build` passes; routes load correct pages |
| D.1.13 | Mobile App: add Flutter auth flow (login screen, token storage, role routing) | App shows login; navigates to correct shell per role |
| D.1.14 | Run full test suite + compile checks | Backend: `./mvnw test`; Web: `npm run build`; Mobile: `flutter build apk --debug` |
| D.1.15 | Output `AWAITING_APPROVE_SHARED_DB_RESET` | User reviews and types `APPROVE_SHARED_DB_RESET` |

### D.2 Files to Change in Phase B

**Backend — configuration:**
- `src/main/resources/application.yaml` — enable Flyway, set `ddl-auto: validate`
- `src/main/resources/db/migration/V1__baseline.sql` — new consolidated baseline (**replaces all V1–V12**)
- Delete: `V2__alter_community_topics.sql` through `V12__add_role_to_users.sql`
- Create: `05_Development/CareBridgeAPI/.env.example`

**Backend — entity:**
- `security/entity/User.java` — no change to Java fields needed; schema change handles extra DB columns on reset

**Backend — service/policy:**
- `security/service/impl/AuthServiceImpl.java` — ensure `ensureCanAuthenticate` is called in login + refresh flows
- Community write endpoints — add ownership validation
- `consent/` — wire policy check in community/content services

**Web Portal:**
- `src/App.tsx` — refactor to use react-router-dom v7 `<RouterProvider>`
- `src/app/router/` — create route definitions with role guards
- `src/app/guards/` — create `ProtectedRoute` and `RoleGuard` components
- `src/app/layouts/` — create `AdminLayout`, `ModeratorLayout`, `ContentLayout` shells
- `src/app/providers/` — create `AuthProvider` wrapping router
- `src/shared/auth/` — create `useAuth` hook and auth store (zustand)

**Mobile App:**
- `lib/main.dart` — wire auth state check before BottomNav
- `lib/features/auth/screens/` — create `LoginScreen`, `OtpVerifyScreen`
- `lib/core/auth/` — create token storage and auth state
- `lib/core/network/api_client.dart` — add Authorization header injection

### D.3 Files NOT to Change in Phase B

The following must be preserved exactly as found (to keep existing tests green):

- All 38 existing test files
- `community/entity/`, `community/controller/`, `community/service/`, `community/repository/`, `community/dto/`, `community/mapper/`
- `content/entity/`, `content/controller/`, `content/service/`, `content/repository/`, `content/dto/`, `content/mapper/`
- `partner/entity/`, `partner/controller/`, `partner/service/`, `partner/repository/`, `partner/dto/`, `partner/mapper/`
- `integration/gemini/` (RAG) — entire package
- `security/jwt/`, `security/rbac/`, `security/otp/`
- All `04_Implement/` TDS + Test-Spec files

---

## Section E: UI Foundation Plan (Phase B)

### E.1 Web Portal — React Router v7 Structure

**Route layout (role-gated):**

```
/                        → redirect to /login (if unauthenticated) or /dashboard
/login                   → LoginPage (public)
/dashboard               → AdminLayout → DashboardPage (role: SYSTEM_ADMIN, MODERATOR, CONTENT_ADMIN)
/moderator/queue         → ModeratorLayout → ModerationQueuePage (role: MODERATOR)
/moderator/topics        → ModeratorLayout → ManageTopicsPage (role: MODERATOR)
/content/create          → ContentLayout → CreateContentPage (role: CONTENT_ADMIN)
/partner/create          → PartnerLayout → CreatePartnerProfilePage (role: PARTNER)
/profile                 → AuthenticatedLayout → AccountProfilePage (any authenticated)
```

**Key components to create:**
- `ProtectedRoute` — redirects to `/login` if no token
- `RoleGuard` — renders 403 page if role mismatch
- `AuthProvider` — reads token from localStorage, decodes role from JWT
- `useAuth` hook — exposes `user`, `role`, `login`, `logout`

**UI Style:** All new components must follow Warm Claymorphism tokens from `.claude/skills/ui-skill-system/02-tokens/react_design_system.tsx`. No Tailwind or custom CSS — use the design system tokens.

### E.2 Mobile App — Flutter Auth Shell

**Navigation flow:**
```
App start
  └─ AuthGate (check stored token)
       ├─ No token → LoginScreen
       │    └─ OtpVerifyScreen → save token → role routing
       └─ Token valid → RoleShell
            ├─ MOTHER / FAMILY → MothersShell (5 tabs)
            ├─ EXPERT → ExpertShell (placeholder)
            └─ Other roles → GenericShell (placeholder)
```

**Key files to create:**
- `lib/features/auth/screens/login_screen.dart`
- `lib/features/auth/screens/otp_verify_screen.dart`
- `lib/core/auth/auth_repository.dart` — login API + token storage
- `lib/core/auth/auth_state.dart` — riverpod/provider state (or simple ValueNotifier)
- `lib/core/network/api_client.dart` — add token injection

**UI Style:** Warm Claymorphism per `.claude/skills/ui-skill-system/02-tokens/flutter_design_system.dart`. All new screens use `WarmColors`, `WarmTypography`, `WarmSpacing` tokens.

---

## Section F: Acceptance Criteria

### F.1 Phase B Exit Gates

| Gate | Check Command | Pass Condition |
|---|---|---|
| Backend compiles | `./mvnw compile -f 05_Development/CareBridgeAPI/pom.xml` | 0 errors |
| All tests pass | `./mvnw test -f 05_Development/CareBridgeAPI/pom.xml` | 0 failures, 0 errors |
| Web builds | `npm run build` (in `05_Development/CareBridgeWebApp`) | 0 TypeScript errors, bundle created |
| Mobile compiles | `flutter build apk --debug` (in `05_Development/CareBridgeMobileApp`) | 0 compilation errors |
| Flyway validates | Application startup with `ddl-auto: validate` | No `SchemaManagementException` |
| No `.env` committed | `git status` + `git diff --cached` | `.env` not in staged or committed files |
| No secrets in code | `grep -r "password\|secret\|apikey" src/main --include="*.java" -i` | No hardcoded values; only `${ENV_VAR}` references |

### F.2 Phase B Deliverables Checklist

- [ ] `src/main/resources/db/migration/V1__baseline.sql` — new consolidated, self-consistent baseline
- [ ] V2–V12 migration files deleted
- [ ] `application.yaml` — Flyway enabled, `ddl-auto: validate`
- [ ] `.env.example` — created with all variable names, no values
- [ ] Auth flows enforce `enabled`/`locked` account state
- [ ] Community write endpoints enforce ownership
- [ ] Consent policy wired into service reads (community + content)
- [ ] Web Portal: react-router-dom v7 routing wired with `ProtectedRoute` + `RoleGuard`
- [ ] Web Portal: `AuthProvider`, `useAuth`, zustand auth store created
- [ ] Mobile App: `LoginScreen`, `OtpVerifyScreen` created with Warm Claymorphism styling
- [ ] Mobile App: token storage + API client auth wired
- [ ] `docs/plans/claude/supabase-reset.sql` — guarded reset script (no execution)
- [ ] All 38 existing tests still passing

### F.3 Phase C Entry Conditions (Supabase Reset)

Phase C is unlocked only when ALL of the following are confirmed:

1. Phase B exit gates F.1 all pass
2. `git status` shows clean working tree on `HuyND`
3. No `.env` in git history or staged changes
4. User confirms DB is still empty (or has only test/seed data acceptable to wipe)
5. User explicitly types `APPROVE_SHARED_DB_RESET`

---

## Section G: Open Decisions Requiring User Input

The following decisions are flagged for explicit approval or override before Phase B begins:

| # | Decision | Recommendation | Override Option |
|---|---|---|---|
| G.1 | Drop extra `users` columns (`account_status`, `email_verified`, `phone_verified`, `last_login_at`) from new V1 | ✅ DROP — not mapped in entity, not used | Keep them: annotate with "KEEP: [reason]" |
| G.2 | Preserve code-side role names (`FAMILY`, `SYSTEM_ADMIN`, `PARTNER`) as authoritative | ✅ PRESERVE CODE — no renames needed | Rename to spec names: annotate with "RENAME: [mapping]" |
| G.3 | Include all 72 tables in new V1 (Option A) vs entity-only tables (Option B) | ✅ Option A — include all 72 | Override to Option B: annotate with "OPTION B" |
| G.4 | New V1 file name: `V1__baseline.sql` | ✅ `V1__baseline.sql` | Override with preferred name |

---

*End of PLAN-CORE-PLATFORM-REBASELINE.md*
