# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification â€” UC-193 Switch Active Baby Profile

| Field | Value |
|-------|-------|
| **Document ID** | `CB-BABY-IMP-003` |
| **Version** | `1.0` |
| **Date** | `2026-07-03` |
| **Status** | `Partially Implemented` |
| **Document Owner** | `TV2-BÃ¡ch` |
| **Author** | `AI Agent` |
| **Reviewed by** | `[Tech Lead]` |
| **DPO Sign-off** | `[ ] Pending` |
| **Approved by** | `TV2-BÃ¡ch` |
| **Last Review** | `2026-07-03` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented: targeted baby/carejourney backend tests PASS; full regression remains blocked by non-baby Family/Exercise/Auth/Triage failures. |
| 2026-07-03 | AI Agent | Táº¡o tÃ i liá»‡u láº§n Ä‘áº§u cho UC-193 Switch Active Baby Profile â€” Draft |

---

## Má»¤C Lá»¤C

1. [Tá»•ng quan Module](#1-tá»•ng-quan-module)
2. [Ma tráº­n Truy váº¿t](#2-ma-tráº­n-truy-váº¿t-traceability-matrix)
3. [Architecture Decision Records](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling](#5-static-modeling-mÃ´-hÃ¬nh-tÄ©nh)
6. [Dynamic Modeling](#6-dynamic-modeling-mÃ´-hÃ¬nh-Ä‘á»™ng)
7. [Domain Event Catalog](#7-domain-event-catalog)
8. [Interface Specification](#8-interface-specification-Ä‘áº·c-táº£-giao-diá»‡n)
9. [API Specification](#9-api-specification)
10. [Báº£ng mÃ£ lá»—i](#10-báº£ng-mÃ£-lá»—i-error-codes)
11. [Quy trÃ¬nh Triá»ƒn khai](#11-quy-trÃ¬nh-triá»ƒn-khai-step-by-step)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t](#13-ká»‹ch-báº£n-kiá»ƒm-thá»­-chi-tiáº¿t)
14. [PhÆ°Æ¡ng phÃ¡p XÃ¡c minh](#14-phÆ°Æ¡ng-phÃ¡p-xÃ¡c-minh)
15. [Máº«u thá»­ thá»±c táº¿](#15-máº«u-thá»­-thá»±c-táº¿-api-verification-samples)
16. [Báº£ng tá»•ng há»£p phÃ¢n quyá»n](#16-báº£ng-tá»•ng-há»£p-phÃ¢n-quyá»n-authorization-matrix)
17. [AI Prompt Constraints (CASE 2.0)](#17-ai-prompt-constraints-case-20)

---

## 1. Tá»•ng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | `SwitchActiveBabyProfile` |
| **Bounded Context** | `baby` (same bounded context as UC-192 View Baby Profile) |
| **UC ID** | `UC-193` |
| **SRS Reference** | `3.3.12.2` (lines 4154â€“4173 of `02_Requirements/SRS/3_Functional_Specification.md`) |
| **Primary Actor** | `Mother (ROLE_MOTHER)` |
| **Platform** | `Mobile App` |
| **Priority** | `Medium` |
| **Frequency of Use** | `Regular` |
| **Sprint** | `Sprint 4 â€” "Device Sync And Care Edge Cases"` |
| **Owner** | `TV2-BÃ¡ch` |
| **Data Classification** | `Sensitive-PII` (same classification as `baby_profiles` per UC-192 TDS) |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `baby_profiles table (extended by this UC), auth (JWT)` |
| **Downstream Consumers** | `Mobile home/dashboard screen (default baby context for daily log, growth tracking, reminders), baby daily log, vaccination, growth tracking` |

**MÃ´ táº£:** Cho phÃ©p Mother chá»n má»™t baby profile lÃ m "active" (há»“ sÆ¡ Ä‘ang Ä‘Æ°á»£c xem/thao tÃ¡c máº·c Ä‘á»‹nh) khi tÃ i khoáº£n quáº£n lÃ½ nhiá»u baby. ÄÃ¢y lÃ  thao tÃ¡c state-toggle, khÃ´ng táº¡o/xÃ³a/sá»­a dá»¯ liá»‡u baby profile khÃ¡c ngoÃ i cá» active.

**In Scope:**
- Äáº·t Ä‘Ãºng má»™t `BabyProfile` thuá»™c sá»Ÿ há»¯u cá»§a Mother thÃ nh `is_active = true`; má»i baby profile khÃ¡c cá»§a cÃ¹ng Mother tá»± Ä‘á»™ng chuyá»ƒn `is_active = false` trong cÃ¹ng transaction.
- Tráº£ vá» baby profile vá»«a Ä‘Æ°á»£c set active (dÃ¹ng láº¡i `BabyProfileDetailResponse` â€” reuse UC-192 contract).
- Audit log cho hÃ nh Ä‘á»™ng switch (state-changing, khÃ¡c UC-192 vá»‘n read-only).

**Out of Scope:**
- Care group member khÃ´ng sá»Ÿ há»¯u baby khÃ´ng Ä‘Æ°á»£c switch active há»™ Mother khÃ¡c (chá»‰ owner má»›i cÃ³ quyá»n switch â€” háº¹p hÆ¡n UC-192's view access, xem ADR-BABY-004 Â§3).
- KhÃ´ng thay Ä‘á»•i cÃ¡c trÆ°á»ng dá»¯ liá»‡u khÃ¡c cá»§a baby profile (nickname, birthDate, ...).
- KhÃ´ng tá»± Ä‘á»™ng switch active khi táº¡o má»›i baby profile (`createBabyProfile` giá»¯ nguyÃªn hÃ nh vi hiá»‡n táº¡i â€” xem Open Item OI-1 Â§11.3).
- ARCHIVED baby profiles khÃ´ng thá»ƒ Ä‘Æ°á»£c set lÃ m active (chá»‰ ACTIVE profiles).

---

## 2. Ma tráº­n Truy váº¿t (Traceability Matrix)

| Requirement ID | Loáº¡i | MÃ´ táº£ yÃªu cáº§u | ThÃ nh pháº§n Code | Compliance Target | ADR liÃªn quan |
|----------------|------|---------------|-----------------|--------------------|---------------|
| UC-193 | Use Case | Mother chá»n active baby profile khi quáº£n lÃ½ nhiá»u baby | `BabyController.switchActiveBabyProfile()` (NEW) | BR-RBAC | ADR-BABY-004 |
| BR-RBAC | Business Rule | Users chá»‰ truy cáº­p chá»©c nÄƒng trong pháº¡m vi role/permission | `BabyOwnershipPolicy.assertOwner()` (NEW) | BR-RBAC | ADR-BABY-004 |
| BR-PRIVACY | Business Rule | Dá»¯ liá»‡u sá»©c khá»e/gia Ä‘Ã¬nh tuÃ¢n thá»§ consent, purpose, minimum-necessary access | KhÃ´ng expose baby data ngoÃ i scope trong response | BR-PRIVACY | â€” |
| SRS Â§3.3.12.2 Postcondition POST-3 | Use Case Postcondition | "Sensitive actions are recorded for audit ... where required" | `AuditService.log(BABY_ACTIVE_PROFILE_SWITCHED, ...)` (NEW) | Audit | â€” |
| ADR-BABY-001 (reused from UC-192) | Decision | ACCEPTED-only care group membership check pattern | N/A â€” not applicable here (switch is owner-only, see ADR-BABY-004) | â€” | ADR-BABY-001 |
| ADR-BABY-003 (reused from UC-192) | Decision | Baby profile view access = owner OR ACCEPTED care group member | Contrast baseline â€” switch access is narrower (owner only) | BR-PRIVACY | ADR-BABY-003 |

---

## 3. Architecture Decision Records (ADR)

### ADR-BABY-004 â€” Active Baby Profile Tracking Mechanism

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent (Technical Architect role) â€” pending Principal Architect review` |
| **Date** | `2026-07-03` |
| **Supersedes** | â€” |

#### Bá»‘i cáº£nh (Context)
UC-193 requires the system to track exactly one "active" baby profile per Mother account when the account manages multiple babies (`BabyProfileRepository.countByOwnerUserId()` already supports multi-baby accounts per UC-32 create flow). Today, **no column or table tracks an "active baby"** anywhere in the schema:
- `baby_profiles` (V1__init_schema.sql line 607) has no `is_active`/`is_current` column â€” only `status` (`ACTIVE`/`ARCHIVED`, business lifecycle, not "currently selected").
- `mother_journeys` (line 559) has no `active_baby_id` FK.
- `users` (line 532) has no baby-selection preference column.
- The real (shipped) `BabyProfile` entity (`baby/entity/BabyProfile.java`) has no `isActive` field â€” note this **contradicts** the UC-192 TDS class diagram (`04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` Â§5.1) which documented an `isActive: Boolean` attribute that was never implemented. UC-192's actual shipped code (verified 2026-07-03) has no such field. This TDS treats the **real code as ground truth** per project policy and introduces `is_active` for the first time under UC-193, not as a fix to UC-192.

This is a genuine new concept requiring schema design (research gate RG-6).

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t (Options Considered)

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | Boolean `is_active` column directly on `baby_profiles`, enforced exactly-one-true-per-owner via **partial unique index** (`WHERE is_active = true`) + app-level transactional swap | + Smallest-scoped change â€” extends existing table, no new table/entity + Data locality: query "give me the active baby" is a single indexed lookup on the table already used by UC-192 + Partial unique index gives DB-level invariant enforcement, not just app-level trust | - Every switch requires 2 writes (old active â†’ false, new active â†’ true) inside one transaction |
| B | New `active_baby_id` FK column on `users` table (session-scoped single pointer) | + Only 1 write per switch (just update the pointer) | - Couples `baby` bounded context into `users` table (cross-context FK) â€” violates package-by-domain boundary in `CLAUDE.md` - `users` table is IAM/auth-owned; adding baby-domain FK there increases coupling and blast radius for auth changes - No natural place to migrate baby ownership transfer later |
| C | New standalone `user_active_baby` mapping table (`user_id` PK, `baby_id` FK) | + Fully decoupled from `baby_profiles` schema + Extensible to multi-device "last active per device" later | - Introduces a new table for a single boolean concept â€” disproportionate schema footprint - Extra JOIN needed on every "get active baby" read (used by mobile home screen frequently â€” NFR Â§4.1 latency budget) |

#### Quyáº¿t Ä‘á»‹nh (Decision)
Chá»n **PhÆ°Æ¡ng Ã¡n A** â€” add `is_active BOOLEAN NOT NULL DEFAULT false` column to `baby_profiles`, enforced by a **partial unique index** `ux_baby_profiles_owner_active ON baby_profiles(owner_user_id) WHERE is_active = true`. This keeps the smallest-scoped change consistent with `baby_profiles`' existing structure (per CLAUDE.md "Delivery Rules â€” smallest scoped change"), reuses the same repository/entity already extended by UC-192, and gives a hard DB-level invariant (at most one active row per owner) rather than relying solely on application logic.

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:**
- Reuses existing `BabyProfileRepository`/`BabyProfile` entity â€” no new bounded-context coupling.
- Partial unique index makes "exactly one active per owner" a database invariant, not just an application promise â€” protects against concurrent switch requests (see Â§6.3 Concurrency).
- `getBabyProfile`/`listBabyProfiles` (UC-192, UC-32) can trivially be extended later to surface `isActive` in the response DTO without further schema change.

**TiÃªu cá»±c / Trade-offs:**
- Switch operation requires an UPDATE across 2 rows within one transaction (old-activeâ†’false, new-activeâ†’false-then-true, or a single `UPDATE ... SET is_active = (baby_id = :targetId) WHERE owner_user_id = :ownerId`). Mitigated: implemented as one SQL statement (Â§6.1) to avoid a window where the partial unique index would reject a naive "set new true" before "set old false".
- First baby created via `createBabyProfile` (UC-32/UC-31) does not automatically become active under this TDS (`is_active` defaults `false` for all new rows) â€” flagged as **Open Item OI-1** (Â§11.3) for product decision; out of scope for this TDS per "make the smallest scoped change" (CLAUDE.md).

**Compliance Impact:**
- None â€” `is_active` is not itself PII; it is a UI/state flag scoped to the existing Sensitive-PII `baby_profiles` row.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|-------------------|
| Latency | `PATCH .../active` response (p99) | `< 200ms` | Manual timing / future k6 test | Consistent with UC-192 GET latency target (`< 150ms`) plus one extra write |
| Availability | Uptime | `99.9%` | Uptime monitor | Same as UC-192 |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|----------------------|-------------------|
| Invariant | Exactly one `is_active = true` row per `owner_user_id` at all times | 100% | Partial unique index `ux_baby_profiles_owner_active` (Â§5.2) | ADR-BABY-004 |
| Retention | Audit log for switch action | 7 nÄƒm (project baseline, same as other audit actions) | `audit_log` table (existing) | BR-PRIVACY |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|----------------------|-------------------|
| Access control | Owner-only switch (narrower than UC-192's owner+care-member view) | Least privilege | Authorization Matrix (Â§16) | BR-RBAC |
| IDOR prevention | Switching to a `babyId` not owned by caller must be rejected regardless of existence | 100% rejection | Test Spec â€” ownership/IDOR test cases | BR-RBAC |

### 4.4. Scalability & Capacity Planning
No material scale change â€” this endpoint reuses the existing `baby_profiles` table and index footprint used by UC-192/UC-32. Expected call volume: low-frequency, user-triggered (Mother switching between baby profiles on mobile), not a hot path.

---

## 5. Static Modeling (MÃ´ hÃ¬nh TÄ©nh)

### 5.1. Class Diagram (PlantUML)

> Existing (shipped) elements are marked `<<existing>>`; new elements for UC-193 are marked `<<NEW â€” UC-193>>`.

```plantuml
@startuml SwitchActiveBabyProfile_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class BabyProfile <<existing>> {
  + id: UUID
  + ownerUserId: UUID
  + relatedJourneyId: UUID
  + nickname: String
  + birthDate: LocalDate
  + gender: Gender
  + birthWeightKg: BigDecimal
  + birthLengthCm: BigDecimal
  + status: BabyProfileStatus
  + isActive: Boolean <<NEW â€” UC-193>>
  + createdAt: Instant
  + updatedAt: Instant
}

enum BabyProfileStatus <<existing>> {
  ACTIVE
  ARCHIVED
}

interface IBabyService <<existing, extended>> {
  + createBabyProfile(request, callerId): CreateBabyProfileResponse
  + listBabyProfiles(callerId): List<BabyProfileDetailResponse>
  + getBabyProfile(profileId, callerId): BabyProfileDetailResponse
  + switchActiveBabyProfile(babyId, callerId): BabyProfileDetailResponse <<NEW â€” UC-193>>
}

class BabyServiceImpl <<existing, extended>> implements IBabyService {
  - babyRepository: BabyProfileRepository
  - accessPolicy: BabyAccessPolicy
  - auditService: AuditService
  + createBabyProfile(request, callerId)
  + listBabyProfiles(callerId)
  + getBabyProfile(profileId, callerId)
  + switchActiveBabyProfile(babyId, callerId) <<NEW â€” UC-193>>
}

class BabyAccessPolicy <<existing, unchanged>> {
  - memberRepository: CareGroupMemberRepository
  + canView(profile, callerId): boolean
}

class BabyOwnershipPolicy <<NEW â€” UC-193>> {
  + assertOwner(profile: BabyProfile, callerId: UUID): void
}

interface BabyProfileRepository <<existing, extended>> extends JpaRepository {
  + countByOwnerUserId(ownerUserId): long
  + findByOwnerUserIdAndStatusOrderByCreatedAtAsc(ownerUserId, status): List<BabyProfile>
  + clearActiveForOwner(ownerUserId): int <<NEW â€” UC-193, @Modifying>>
  + findByIdAndOwnerUserId(babyId, ownerUserId): Optional<BabyProfile> <<NEW â€” UC-193>>
}

class BabyController <<existing, extended>> {
  + createBabyProfile(request, principal): ResponseEntity <<existing>>
  + listBabyProfiles(principal): ResponseEntity <<existing>>
  + getBabyProfile(babyId, principal): ResponseEntity <<existing>>
  + switchActiveBabyProfile(babyId, principal): ResponseEntity <<NEW â€” UC-193>>
}

BabyServiceImpl --> BabyProfileRepository : uses
BabyServiceImpl --> BabyOwnershipPolicy : uses <<NEW>>
BabyServiceImpl --> BabyAccessPolicy : uses (existing methods only)
BabyController --> IBabyService : uses
BabyProfileRepository ..> BabyProfile

@enduml
```

**CASE 2.0 constraint (structural):** UC-193 MUST NOT create a parallel `BabyController2`/`BabyActiveService`/new repository interface. It MUST extend the existing `BabyController`, `IBabyService`/`BabyServiceImpl`, and `BabyProfileRepository` files listed above (see Â§17 C-constraints).

### 5.2. Data Structure (Flyway SQL Migration)

> **CareBridge rule:** `V1__init_schema.sql` + approved Flyway migrations are the primary schema source (per CLAUDE.md). No `is_active`/`active_baby_id` concept exists anywhere in the current schema â€” confirmed by full-text search of `V1__init_schema.sql` and all files under `db/migration/` (latest version present: `V20260629000002`). This is a **genuine schema gap** (RG-6).

**New migration file:** `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260703100100__add_baby_profile_active_flag.sql`

```sql
-- === UC-193 Switch Active Baby Profile â€” Schema Delta ===
-- Adds an "is_active" flag to baby_profiles to track which single baby profile
-- is currently selected by its owner. Exactly one ACTIVE-eligible row per
-- owner_user_id may have is_active = true, enforced by a partial unique index.

ALTER TABLE public.baby_profiles
    ADD COLUMN is_active boolean NOT NULL DEFAULT false;

-- Enforce "at most one active baby per owner" as a DB-level invariant.
-- Partial index only covers rows where is_active = true, so multiple
-- is_active = false rows per owner remain unaffected.
CREATE UNIQUE INDEX ux_baby_profiles_owner_active
    ON public.baby_profiles (owner_user_id)
    WHERE is_active = true;

COMMENT ON COLUMN public.baby_profiles.is_active IS
    'UC-193: true when this profile is the Mother''s currently selected/active baby. At most one true row per owner_user_id (see ux_baby_profiles_owner_active).';
```

**Sync action for `V1__init_schema.sql`:** Per project convention (see `V2__spec_sync_from_tds.sql`, `V7__entity_schema_sync.sql` precedent), `V1__init_schema.sql` itself is **never edited** post-baseline; the delta lives in this new versioned migration file. `V1__init_schema.sql`'s header comment block should be updated by a future consolidated "spec sync" migration (out of scope for this TDS) to note the new column exists â€” no action required for UC-193 implementation to proceed.

**Collision check:** No `V20260703*` file exists yet in `db/migration/` (verified via directory listing 2026-07-03). Version `V20260703100100` avoids the reserved parallel-agent ranges `090000`/`110000`/`120000`/`130000` per task instructions, using the `+00100` offset from the `100000` base.

---

## 6. Dynamic Modeling (MÃ´ hÃ¬nh Äá»™ng)

### 6.1. Sequence Diagram â€” Happy Path (PlantUML)

```plantuml
@startuml SwitchActiveBabyProfile_SequenceDiagram_HappyPath
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam sequenceParticipant underline
skinparam backgroundColor #FAFAFA

actor       "Mother"            as Client
participant "BabyController"    as Controller
participant "BabyServiceImpl"   as Service
participant "BabyOwnershipPolicy" as Policy
participant "BabyProfileRepository" as Repo
database    "PostgreSQL"        as DB
participant "AuditService"      as Audit

Client -> Controller : PATCH /api/v1/babies/{babyId}/active
activate Controller

Controller -> Controller : callerId = SecurityUtils.requireCurrentUserId(principal)
Controller -> Service : switchActiveBabyProfile(babyId, callerId)
activate Service

Service -> Repo : findById(babyId)
Repo -> DB : SELECT FROM baby_profiles WHERE baby_id = ?
DB --> Repo : profile
Repo --> Service : Optional<BabyProfile>

Service -> Policy : assertOwner(profile, callerId)
Policy --> Service : OK (no exception)

Service -> Service : validate profile.status == ACTIVE (BR: cannot activate ARCHIVED)

Service -> Repo : clearActiveForOwner(callerId)
Repo -> DB : UPDATE baby_profiles SET is_active=false WHERE owner_user_id=? AND is_active=true
DB --> Repo : rows updated

Service -> Repo : save(profile with isActive=true)
Repo -> DB : UPDATE baby_profiles SET is_active=true, updated_at=now() WHERE baby_id=?
DB --> Repo : updated profile

Service -> Audit : log(BABY_ACTIVE_PROFILE_SWITCHED, callerId, "BabyProfile", babyId, "switched active")
Service --> Controller : BabyProfileDetailResponse (isActive=true)
deactivate Service

Controller --> Client : 200 OK { ...profile, isActive: true }
deactivate Controller

@enduml
```

### 6.2. Sequence Diagram â€” Error Path (PlantUML)

```plantuml
@startuml SwitchActiveBabyProfile_SequenceDiagram_ErrorPath
skinparam backgroundColor #FAFAFA

actor "Mother (attacker or wrong owner)" as Client
participant "BabyController" as Controller
participant "BabyServiceImpl" as Service
participant "BabyOwnershipPolicy" as Policy
participant "BabyProfileRepository" as Repo
database "PostgreSQL" as DB

== Case 1: babyId does not exist ==
Client -> Controller : PATCH /api/v1/babies/{nonexistentId}/active
Controller -> Service : switchActiveBabyProfile(nonexistentId, callerId)
Service -> Repo : findById(nonexistentId)
Repo -> DB : SELECT ...
DB --> Repo : empty
Service --> Controller : throws BusinessException(404, "BABY-001")
Controller --> Client : 404 Not Found

== Case 2: babyId exists but not owned by caller (IDOR attempt) ==
Client -> Controller : PATCH /api/v1/babies/{othersBabyId}/active
Controller -> Service : switchActiveBabyProfile(othersBabyId, callerId)
Service -> Repo : findById(othersBabyId)
Repo -> DB : SELECT ...
DB --> Repo : profile (ownerUserId != callerId)
Service -> Policy : assertOwner(profile, callerId)
Policy --> Service : throws BusinessException(403, "BABY-006")
Service --> Controller : propagate exception
Controller --> Client : 403 Forbidden

== Case 3: babyId belongs to caller but status = ARCHIVED ==
Client -> Controller : PATCH /api/v1/babies/{archivedBabyId}/active
Controller -> Service : switchActiveBabyProfile(archivedBabyId, callerId)
Service -> Repo : findById(archivedBabyId)
Repo -> DB : SELECT ...
DB --> Repo : profile (status=ARCHIVED, ownerUserId=callerId)
Service -> Policy : assertOwner(profile, callerId)
Policy --> Service : OK
Service -> Service : status check fails
Service --> Controller : throws BusinessException(409, "BABY-007")
Controller --> Client : 409 Conflict

@enduml
```

### 6.3. State Machine â€” `is_active` invariant

```plantuml
@startuml SwitchActiveBabyProfile_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> INACTIVE : baby profile created (UC-31/UC-32)\nis_active = false (default)

INACTIVE --> ACTIVE : Mother calls switchActiveBabyProfile(thisBabyId)\nAND profile.status == ACTIVE\nAND caller is owner
ACTIVE --> INACTIVE : Mother calls switchActiveBabyProfile(anotherBabyId)\n(same owner â€” this profile is superseded in same transaction)

note right of ACTIVE
  Invariant (ADR-BABY-004): at most one BabyProfile
  per owner_user_id may be in ACTIVE state at any time.
  Enforced by ux_baby_profiles_owner_active partial unique index.
end note

note left of INACTIVE
  A profile with status=ARCHIVED can never transition
  to is_active=true (BR: switch rejected with BABY-007
  if target profile.status != ACTIVE).
end note

@enduml
```

**âš ï¸ Invariant báº¥t biáº¿n:**
1. At most one `baby_profiles` row per `owner_user_id` has `is_active = true` (DB-enforced via partial unique index).
2. A `baby_profiles` row with `status = ARCHIVED` can never have `is_active = true` (app-enforced in `BabyServiceImpl.switchActiveBabyProfile()`; DB does not enforce this cross-column rule to keep migration minimal).
3. Switching a baby to active never mutates any field other than `is_active`/`updated_at` on the affected rows (no nickname/birthDate/etc. side effects).

### 6.4. Concurrency Note
Two concurrent `switchActiveBabyProfile` calls for the same owner (different `babyId`) are serialized by the `@Transactional` boundary in `BabyServiceImpl` plus row-level locking implied by the `UPDATE ... WHERE owner_user_id = ?` statement in `clearActiveForOwner`. The partial unique index guarantees that even under an implementation bug, the DB rejects a state where 2 rows are simultaneously `is_active = true` for the same owner (transaction would fail with a unique-violation and roll back â€” surfaced as `BABY-005` internal error, same code pattern as UC-192's generic 500).

---

## 7. Domain Event Catalog

### 7.1. Events Published (PhÃ¡t ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|----------------|-----------------|--------|
| `ActiveBabyProfileSwitched` | Mother successfully switches active baby via `PATCH /api/v1/babies/{babyId}/active` | `BabyServiceImpl` (via `AuditService.log(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED, ...)`) | Audit subsystem (existing `audit_log` sink); no other current subscribers | `ActiveBabyProfileSwitched.java` (Â§7.3) | No â€” synchronous `AuditService.log()` call, consistent with existing `BABY_PROFILE_CREATED` pattern in `BabyServiceImpl.createBabyProfile()` |

**Implementation note (CASE 2.0 constraint):** This project's `AuditService` (existing interface, `audit/service/AuditService.java`) does not implement a generic pub/sub `ApplicationEvent` bus â€” all "events" in this codebase are recorded via direct `AuditService.log(AuditAction, ...)` calls (see `BabyServiceImpl.createBabyProfile()` line 47 for the precedent). UC-193 follows this exact existing pattern rather than introducing a new event-bus mechanism. `AuditAction` enum (`audit/entity/AuditAction.java`) requires a new value: `BABY_ACTIVE_PROFILE_SWITCHED`.

### 7.2. Events Consumed (TiÃªu thá»¥)
None â€” UC-193 does not consume any domain event.

### 7.3. Payload Schema (conceptual â€” logged via `AuditService.log(action, userId, resourceType, resourceId, details)`)

```java
// Conceptual payload â€” NOT a new class. Logged via existing AuditService.log() signature:
// auditService.log(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED, callerId, "BabyProfile", babyId.toString(), details);
//
// "details" Object (existing 5th param, serialized by AuditService impl â€” pattern matches
// createBabyProfile's `"created"` string literal, or a small descriptive map/string):
//   details = "switched active baby profile from " + previousActiveBabyId + " to " + babyId
```

No new Java record/class is introduced for this event â€” it reuses the existing `AuditService.log(AuditAction, UUID, String, String, Object)` contract (confirmed real signature, `audit/service/AuditService.java` line 11) to avoid inventing an unsupported event-bus architecture (CASE 2.0 anti-pattern AP-AI-003 â€” Implicit Decision).

---

## 8. Interface Specification (Äáº·c táº£ Giao diá»‡n)

### 8.1. Service Interface

```java
// IBabyService.java â€” EXTEND existing interface (do not create IBabyActiveService)
// @version 1.1 â€” adds switchActiveBabyProfile (UC-193); existing 3 methods unchanged.
public interface IBabyService {

    CreateBabyProfileResponse createBabyProfile(CreateBabyProfileRequest request, UUID callerId); // <<existing>>

    /** Returns all ACTIVE baby profiles owned by the caller, ordered by creation date. */
    List<BabyProfileDetailResponse> listBabyProfiles(UUID callerId); // <<existing>>

    /** @throws com.carebridge.backend.common.exception.BusinessException (BABY-003/403) if no access */
    BabyProfileDetailResponse getBabyProfile(UUID profileId, UUID callerId); // <<existing>>

    /**
     * NEW â€” UC-193. Marks the given baby profile as the caller's single active profile.
     * Any other profile owned by the same caller with is_active=true is cleared in the
     * same transaction (ADR-BABY-004 invariant).
     *
     * @throws BusinessException (BABY-001/404) if babyId does not exist
     * @throws BusinessException (BABY-006/403) if caller is not the owner of babyId
     * @throws BusinessException (BABY-007/409) if target profile.status != ACTIVE (archived)
     */
    BabyProfileDetailResponse switchActiveBabyProfile(UUID babyId, UUID callerId); // <<NEW â€” UC-193>>
}
```

`BabyProfileDetailResponse` (existing DTO, `baby/dto/BabyProfileDetailResponse.java`) is **reused as-is** for the response of the new method â€” no new response DTO is introduced. It gains one new field:

```java
// BabyProfileDetailResponse.java â€” EXTEND existing DTO with 1 new field
@Data
@Builder
public class BabyProfileDetailResponse {
    private UUID id;
    private String nickname;
    private LocalDate birthDate;
    private String gender;
    private BigDecimal birthWeightKg;
    private BigDecimal birthLengthCm;
    private String status;
    private boolean isActive; // <<NEW â€” UC-193>> â€” true if this is the owner's currently active profile
    private Instant createdAt;
    private Instant updatedAt;
}
```

**No request body DTO is needed** â€” `babyId` is a path variable; there is no other input field for this action (mirrors `getBabyProfile`'s path-variable-only pattern).

### 8.2. Repository Interface

```java
// BabyProfileRepository.java â€” EXTEND existing repository interface
// @version 1.1
public interface BabyProfileRepository extends JpaRepository<BabyProfile, UUID> {

    long countByOwnerUserId(UUID ownerUserId); // <<existing>>

    List<BabyProfile> findByOwnerUserIdAndStatusOrderByCreatedAtAsc(UUID ownerUserId, BabyProfileStatus status); // <<existing>>

    /** NEW â€” UC-193. IDOR-safe combined lookup: returns empty if babyId exists but is not owned by ownerUserId. */
    Optional<BabyProfile> findByIdAndOwnerUserId(UUID id, UUID ownerUserId); // <<NEW â€” UC-193>>

    /**
     * NEW â€” UC-193. Clears is_active for all of this owner's other profiles before setting
     * the target profile active, avoiding a transient partial-unique-index violation.
     * @Modifying required since this is a bulk UPDATE, not a derived SELECT.
     */
    @Modifying
    @Query("UPDATE BabyProfile b SET b.isActive = false WHERE b.ownerUserId = :ownerUserId AND b.isActive = true")
    int clearActiveForOwner(@Param("ownerUserId") UUID ownerUserId); // <<NEW â€” UC-193>>
}
```

**Design note:** `switchActiveBabyProfile` in `BabyServiceImpl` uses `findById()` (existing, inherited from `JpaRepository`) â€” not the new `findByIdAndOwnerUserId` â€” for the initial lookup, so that a non-owned-but-existing `babyId` can be distinguished (â†’ 403 via `BabyOwnershipPolicy.assertOwner()`) from a genuinely non-existent `babyId` (â†’ 404), matching UC-192's existing `getBabyProfile()` pattern exactly (`findById()` then separate access check, not a combined `findByIdAndOwner`). `findByIdAndOwnerUserId` is documented for completeness/future reuse but the primary flow in Â§6.1 uses the two-step existing pattern for consistency with UC-192.

### 8.3. New Policy Class

```java
// BabyOwnershipPolicy.java â€” NEW file, same package pattern as existing BabyAccessPolicy
// Path: baby/policy/BabyOwnershipPolicy.java
package com.carebridge.backend.baby.policy;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BabyOwnershipPolicy {

    /**
     * UC-193 / ADR-BABY-004: switching the active profile is an OWNER-ONLY action â€”
     * narrower than BabyAccessPolicy.canView() which also allows ACCEPTED care group members.
     * @throws BusinessException (BABY-006/403) if callerId is not profile.ownerUserId
     */
    public void assertOwner(BabyProfile profile, UUID callerId) {
        if (!profile.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-006",
                    "Only the baby profile owner may switch the active profile");
        }
    }
}
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|-----------------|------------|-------------|
| `PATCH` | `/api/v1/babies/{babyId}/active` | JWT Bearer | `ROLE_MOTHER` (owner only â€” see Â§16) | 60/min | Yes â€” calling twice with the same `babyId` yields the same end state (`is_active=true` for that profile) |

**Path convention note:** Existing `BabyController` is mounted at `/api/v1/babies` (confirmed real code, not `/api/v1/baby-profiles` as UC-192's TDS text described â€” the shipped `@RequestMapping` is `"/api/v1/babies"`). This TDS uses the **real, shipped base path** `/api/v1/babies` for consistency (CG-7 compliance â€” TDS/code must agree on paths).

### 9.2. Request / Response Schemas

#### `PATCH /api/v1/babies/{babyId}/active` â€” Switch active baby profile

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:** None (path variable only).

**Response â€” 200 OK (Happy Path):**
```json
{
  "success": true,
  "data": {
    "id": "uuid-v4",
    "nickname": "Bean",
    "birthDate": "2026-01-15",
    "gender": "MALE",
    "birthWeightKg": 3.2,
    "birthLengthCm": 50.0,
    "status": "ACTIVE",
    "isActive": true,
    "createdAt": "2026-06-26T00:00:00.000Z",
    "updatedAt": "2026-07-03T00:00:00.000Z"
  },
  "message": "Active baby profile switched successfully",
  "timestamp": "2026-07-03T00:00:00.000Z"
}
```

**Response â€” 403 Forbidden (Not Owner):**
```json
{
  "error": {
    "code": "BABY-006",
    "message": "Only the baby profile owner may switch the active profile"
  }
}
```

**Response â€” 404 Not Found:**
```json
{
  "error": {
    "code": "BABY-001",
    "message": "Baby profile not found: <babyId>"
  }
}
```

**Response â€” 409 Conflict (Archived profile):**
```json
{
  "error": {
    "code": "BABY-007",
    "message": "Cannot set an archived baby profile as active"
  }
}
```

---

## 10. Báº£ng mÃ£ lá»—i (Error Codes)

> Reuses the existing `BABY-` prefix and existing codes `BABY-001`/`BABY-003` from the real shipped `BabyServiceImpl`. New codes `BABY-006`/`BABY-007` are assigned as the next unused numbers in the `BABY-` sequence (confirmed via full-text search: only `BABY-001` and `BABY-003` exist in current code).

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|---------------|----------------|--------------------|
| `BABY-001` | 404 | Baby profile not found | Há»“ sÆ¡ em bÃ© khÃ´ng tá»“n táº¡i | `babyId` not found in DB â€” **reused from existing `getBabyProfile()`**, same code |
| `BABY-003` | 403 | Access denied to baby profile | KhÃ´ng Ä‘á»§ quyá»n truy cáº­p | *(not used by this endpoint â€” reserved for UC-192's view-access check; listed here for cross-reference only)* |
| `BABY-006` | 403 | Only the baby profile owner may switch the active profile | Chá»‰ chá»§ sá»Ÿ há»¯u há»“ sÆ¡ má»›i Ä‘Æ°á»£c Ä‘á»•i há»“ sÆ¡ Ä‘ang hoáº¡t Ä‘á»™ng | Caller is authenticated but is not `ownerUserId` of the target `babyId` (care group members are NOT sufficient â€” narrower than view access) |
| `BABY-007` | 409 | Cannot set an archived baby profile as active | KhÃ´ng thá»ƒ Ä‘áº·t há»“ sÆ¡ Ä‘Ã£ lÆ°u trá»¯ lÃ m há»“ sÆ¡ Ä‘ang hoáº¡t Ä‘á»™ng | Target profile's `status == ARCHIVED` |
| `BABY-005` | 500 | Internal error | Lá»—i há»‡ thá»‘ng | Unexpected DB error, e.g. partial-unique-index violation surfaced due to a concurrency bug â€” **reused from UC-192 TDS Â§10 pattern number** (not present in current shipped code, but reserved by convention for `baby` bounded context internal errors) |

---

## 11. Quy trÃ¬nh Triá»ƒn khai (Step-by-Step)

### 11.1. Prerequisites
- [ ] ADR-BABY-004 reviewed (Â§3)
- [ ] DPO sign-off not required â€” `is_active` is a non-PII boolean flag on an existing Sensitive-PII row; no new PII field introduced
- [ ] TDS approved by Principal Architect (this document, currently `Draft`)
- [ ] Staging environment ready for Flyway migration `V20260703100100`

### 11.2. Pre-Migration Checklist
- [ ] Backup DB: `pg_dump -h $DB_HOST -U $DB_USER $DB_NAME > backup_20260703.sql`
- [ ] Migration tested on staging: `ALTER TABLE ... ADD COLUMN` + partial unique index creation is a metadata-only + index-build operation on `baby_profiles` (expected to be a small table â€” low lock risk)
- [ ] Rollback script tested on staging (Â§12.2)

### 11.3. Implementation Steps

#### Cháº·ng 1 â€” Flyway migration
Create `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260703100100__add_baby_profile_active_flag.sql` (full SQL â€” Â§5.2).
```bash
./mvnw flyway:migrate
```

#### Cháº·ng 2 â€” Entity + Repository
- Extend `BabyProfile.java`: add `@Column(name = "is_active", nullable = false) private boolean isActive;` field (with `@Builder.Default private boolean isActive = false;` to match the existing `status` field's default pattern).
- Extend `BabyProfileRepository.java`: add `clearActiveForOwner()` and `findByIdAndOwnerUserId()` (Â§8.2).

#### Cháº·ng 3 â€” Policy
Create `BabyOwnershipPolicy.java` (Â§8.3, new file).

#### Cháº·ng 4 â€” Service
Extend `BabyServiceImpl.java`: add `switchActiveBabyProfile(UUID babyId, UUID callerId)` implementing the sequence in Â§6.1. Extend `IBabyService.java` interface signature.

#### Cháº·ng 5 â€” DTO
Extend `BabyProfileDetailResponse.java`: add `isActive` field; update `getBabyProfile()` and `listBabyProfiles()` mapping code in `BabyServiceImpl` to populate `isActive` from the entity (currently they do not set this field â€” must be added so UC-192's existing endpoints also correctly report the new flag; this is a **required side-effect edit**, not scope creep, since leaving `isActive` unset would silently default to `false` even for the active baby).

#### Cháº·ng 6 â€” Audit
Add `BABY_ACTIVE_PROFILE_SWITCHED` to `AuditAction.java` enum.

#### Cháº·ng 7 â€” Controller
Extend `BabyController.java`: add `@PatchMapping("/{babyId}/active")` method (Â§9).

#### Cháº·ng 8 â€” Verification
```bash
curl -X PATCH https://[host]/api/v1/babies/[babyId]/active \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 200 {data: {..., isActive: true}}
```

### 11.4. Deployment Checklist
- [ ] Migration applied successfully
- [ ] `./mvnw test` green
- [ ] Existing UC-192 tests (`BabyServiceImplTest`) still pass after `BabyProfileDetailResponse.isActive` field addition (no breaking change to existing 7 test cases â€” additive field only)
- [ ] Partial unique index confirmed present: `\d baby_profiles` shows `ux_baby_profiles_owner_active`

**Open Item OI-1 (recorded, not resolved by this TDS):** Should the first baby profile created by a Mother automatically become `is_active = true`? SRS Â§3.3.12.2 does not specify this. Current TDS design leaves all new profiles `is_active = false` by default (smallest-scoped change â€” does not modify `createBabyProfile`). If product wants auto-activation of the first/only baby, that is a follow-up TDS change to `BabyServiceImpl.createBabyProfile()`, explicitly out of scope here.

---

## 12. Rollback & Incident Runbook

### 12.1. Äiá»u kiá»‡n kÃ­ch hoáº¡t Rollback

| Äiá»u kiá»‡n | NgÆ°á»¡ng | NgÆ°á»i quyáº¿t Ä‘á»‹nh |
|-----------|--------|---------------------|
| Error rate tÄƒng Ä‘á»™t biáº¿n trÃªn `PATCH .../active` | > 5% trong 5 phÃºt | On-call Engineer |
| Partial unique index violation errors in logs (indicates concurrency bug) | Any occurrence | Tech Lead |
| Dá»¯ liá»‡u khÃ´ng nháº¥t quÃ¡n (2 active babies cho cÃ¹ng 1 owner) | Báº¥t ká»³ case nÃ o | Tech Lead |

### 12.2. Rollback Procedure

```bash
# BÆ°á»›c 1: Revert migration
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS ux_baby_profiles_owner_active;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.baby_profiles DROP COLUMN IF EXISTS is_active;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260703100100';"

# BÆ°á»›c 2: Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/baby/
git checkout -- src/main/resources/db/migration/V20260703100100__add_baby_profile_active_flag.sql
git checkout -- src/test/java/com/carebridge/backend/baby/

# BÆ°á»›c 3: Verify
curl -X GET https://[host]/api/v1/health
```

### 12.3. Notification Protocol

| Thá»i Ä‘iá»ƒm | NgÆ°á»i nháº­n | KÃªnh |
|-----------|-------------|------|
| Ngay khi phÃ¡t hiá»‡n | On-call team | Slack `#incident` |
| Trong 30 phÃºt | Tech Lead | Internal chat |

---

## 13. Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t

> **Policy:** Má»i test scenario dÃ¹ng dá»¯ liá»‡u `SYNTHETIC`. Chi tiáº¿t Ä‘áº§y Ä‘á»§ náº±m trong `UC193_SwitchActiveBabyProfile_Test-Spec.md`.

```gherkin
Feature: Switch Active Baby Profile
  Background:
    Given test data classification: SYNTHETIC
    And MOTHER-001 owns BABY-001 (status=ACTIVE, isActive=false) and BABY-002 (status=ACTIVE, isActive=true)

  Scenario: Owner switches active baby â†’ 200, invariant holds
    When switchActiveBabyProfile(BABY-001, MOTHER-001)
    Then response 200 with isActive=true for BABY-001
    And BABY-002.isActive becomes false in the same transaction

  Scenario: Non-owner attempts switch â†’ 403
    Given MOTHER-002 is NOT owner of BABY-001
    When switchActiveBabyProfile(BABY-001, MOTHER-002)
    Then throws BusinessException 403 BABY-006

  Scenario: Non-existent baby â†’ 404
    When switchActiveBabyProfile(NONEXISTENT, MOTHER-001)
    Then throws BusinessException 404 BABY-001

  Scenario: Archived baby cannot become active â†’ 409
    Given BABY-003 owned by MOTHER-001 has status=ARCHIVED
    When switchActiveBabyProfile(BABY-003, MOTHER-001)
    Then throws BusinessException 409 BABY-007

  Scenario: Idempotent re-switch to already-active baby â†’ 200, no-op state
    Given BABY-002.isActive = true
    When switchActiveBabyProfile(BABY-002, MOTHER-001)
    Then response 200 with isActive=true for BABY-002
    And no other row changes
```

---

## 14. PhÆ°Æ¡ng phÃ¡p XÃ¡c minh

### 14.1. Database Inspection

```sql
-- Verify exactly one active baby per owner
SELECT owner_user_id, COUNT(*) FILTER (WHERE is_active = true) AS active_count
FROM baby_profiles
GROUP BY owner_user_id
HAVING COUNT(*) FILTER (WHERE is_active = true) > 1;
-- Expected: 0 rows (invariant holds)

-- Verify partial unique index exists
SELECT indexname, indexdef FROM pg_indexes
WHERE tablename = 'baby_profiles' AND indexname = 'ux_baby_profiles_owner_active';
```

### 14.2. Log / Audit Verification

```bash
# Verify audit log records the switch action
kubectl logs -l app=carebridge-api | grep '"action":"BABY_ACTIVE_PROFILE_SWITCHED"' | head -5
```

---

## 15. Máº«u thá»­ thá»±c táº¿ (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X PATCH https://[host]/api/v1/babies/[babyId]/active \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 200 {"success":true,"data":{"id":"...","isActive":true,...}}
```

### 15.2. Error Paths

```bash
# Non-owner â†’ 403
curl -X PATCH https://[host]/api/v1/babies/[othersBabyId]/active \
  -H "Authorization: Bearer [OTHER_MOTHER_JWT]"
# Expected: 403 {"error":{"code":"BABY-006",...}}

# Non-existent â†’ 404
curl -X PATCH https://[host]/api/v1/babies/00000000-0000-0000-0000-000000009999/active \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 404 {"error":{"code":"BABY-001",...}}

# Archived â†’ 409
curl -X PATCH https://[host]/api/v1/babies/[archivedBabyId]/active \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 409 {"error":{"code":"BABY-007",...}}

# No JWT â†’ 401
curl -X PATCH https://[host]/api/v1/babies/[babyId]/active
```

---

## 16. Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix)

| Endpoint | `GUEST` | `MOTHER (owner)` | `MOTHER (care member, non-owner)` | `EXPERT` | `ADMIN` |
|----------|---------|-------------------|-------------------------------------|----------|---------|
| `PATCH /api/v1/babies/{id}/active` | âŒ (401) | âœ… | âŒ (403, `BABY-006`) | âŒ (403) | âœ… All *(admin override â€” consistent with UC-192's `ADMIN âœ… All` pattern; not separately gated in code â€” same `isAuthenticated()` + service-level ownership check applies; ADMIN role does not bypass `BabyOwnershipPolicy` in this TDS â€” flagged as Open Item OI-2)* |

**Open Item OI-2:** UC-192's authorization matrix shows `ADMIN âœ… All` for `GET`, implying some admin override exists â€” but `BabyAccessPolicy.canView()` (real shipped code) has **no ADMIN branch**; it only checks owner-or-care-member. This TDS follows the **real code behavior** (no ADMIN bypass in `BabyOwnershipPolicy` either) rather than the UC-192 TDS documentation's aspirational matrix. If ADMIN override is actually required for support/moderation use cases, that is a separate cross-cutting change outside UC-193's scope.

**ChÃº thÃ­ch:**
- Owner: `owner_user_id` trong `baby_profiles` match JWT subject (`callerId`)
- Care group member: KHÃ”NG Ä‘á»§ quyá»n switch active (khÃ¡c UC-192 view access) â€” chá»‰ owner má»›i switch Ä‘Æ°á»£c (ADR-BABY-004)

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source (ADR/BR) | Last Verified |
|---|------------|-------------------|----------------|
| C1 | `switchActiveBabyProfile()` PHáº¢I Ä‘Æ°á»£c thÃªm vÃ o `IBabyService`/`BabyServiceImpl` HIá»†N CÃ“ â€” KHÃ”NG táº¡o interface/class song song (`BabyActiveService`, `BabyController2`, v.v.) | CLAUDE.md "smallest scoped change" + ADR-BABY-004 | 2026-07-03 |
| C2 | Chá»‰ owner (`profile.ownerUserId == callerId`) má»›i Ä‘Æ°á»£c switch active â€” care group member (ká»ƒ cáº£ ACCEPTED) PHáº¢I bá»‹ tá»« chá»‘i 403 `BABY-006` | ADR-BABY-004 | 2026-07-03 |
| C3 | ÄÃºng má»™t `is_active=true` row cho má»—i `owner_user_id` táº¡i má»i thá»i Ä‘iá»ƒm â€” PHáº¢I dÃ¹ng transaction bao cáº£ 2 bÆ°á»›c UPDATE (clear-then-set) | ADR-BABY-004 Â§Decision | 2026-07-03 |
| C4 | `callerId` PHáº¢I láº¥y tá»« JWT (`SecurityUtils.requireCurrentUserId(principal)`), KHÃ”NG tá»« path/body | BR-RBAC (káº¿ thá»«a pattern UC-192 C3) | 2026-07-03 |
| C5 | Baby profile vá»›i `status=ARCHIVED` KHÃ”NG bao giá» Ä‘Æ°á»£c set `is_active=true` â€” tráº£ vá» 409 `BABY-007` | Â§6.3 State Machine invariant | 2026-07-03 |
| C6 | HÃ nh Ä‘á»™ng switch PHáº¢I ghi audit log qua `AuditService.log(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED, ...)` â€” dÃ¹ng contract sáºµn cÃ³, KHÃ”NG táº¡o event bus má»›i | SRS Â§3.3.12.2 POST-3 + Â§7.1 | 2026-07-03 |

### 17.2 Constraint Injection Block

```
[CONSTRAINT BLOCK â€” Module: SwitchActiveBabyProfile (CB-BABY-IMP-003)]
Theo TDS CB-BABY-IMP-003 vÃ  ADR-BABY-004:

1. switchActiveBabyProfile() PHáº¢I thÃªm vÃ o IBabyService/BabyServiceImpl hiá»‡n cÃ³ (baby/service/IBabyService.java, baby/service/impl/BabyServiceImpl.java) â€” KHÃ”NG táº¡o controller/service song song â€” ADR-BABY-004
2. Chá»‰ profile.ownerUserId == callerId má»›i Ä‘Æ°á»£c switch â€” care group member bá»‹ tá»« chá»‘i 403 BABY-006 â€” ADR-BABY-004
3. ÄÃºng má»™t is_active=true row / owner_user_id táº¡i má»i thá»i Ä‘iá»ƒm â€” dÃ¹ng 1 transaction, clear-then-set â€” ADR-BABY-004, DB-enforced bá»Ÿi ux_baby_profiles_owner_active
4. callerId tá»« SecurityUtils.requireCurrentUserId(principal), KHÃ”NG tá»« path/body â€” BR-RBAC
5. status=ARCHIVED profile KHÃ”NG Ä‘Æ°á»£c set active â€” 409 BABY-007 â€” Â§6.3
6. Audit qua AuditService.log(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED, ...) â€” dÃ¹ng contract cÃ³ sáºµn â€” SRS Â§3.3.12.2

[CONTEXT BLOCK]
- Bounded Context: baby
- Data Classification: Sensitive-PII
- Existing interfaces: Â§8 Service/Repository Interface (BabyController "/api/v1/babies", IBabyService, BabyProfileRepository)
- Error codes: Â§10 Error Codes Table (BABY-001, BABY-006, BABY-007)
- Auth matrix: Â§16 Authorization Matrix

[TASK BLOCK]
Implement PATCH /api/v1/babies/{babyId}/active thá»a mÃ£n constraints trÃªn.
Output pháº£i tuÃ¢n thá»§ Â§8 Interface Specification.
Tests pháº£i cover Test-Spec UC193_SwitchActiveBabyProfile_Test-Spec.md.
```

### 17.3 Constraint Quality Checklist

- [x] Má»—i constraint traceable vá» ADR hoáº·c BR cá»¥ thá»ƒ
- [x] KhÃ´ng cÃ³ constraint generic
- [x] Constraint block cÃ³ â‰¥ 3 constraints cá»¥ thá»ƒ (6 constraints)
- [x] Constraint block reference Â§8 Interface
- [x] Constraint block reference Â§16 Auth Matrix

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | HÃ nh Ä‘á»™ng |
|-------|-------------|-----------|-----------|
| AP-AI-001 | Unconstrained Gen | Code khÃ´ng match constraint C1-C6 | Reject â€” inject láº¡i constraints |
| AP-AI-003 | Implicit Decision | Code assume architecture khÃ´ng cÃ³ ADR (e.g., new event bus, new table) | Reject â€” viáº¿t ADR trÆ°á»›c, xem Â§3/Â§7 |
| AP-AI-005 | Hallucinated Contract | Code import class khÃ´ng cÃ³ trong Â§8 (e.g., `BabyActiveService`) | Reject â€” verify contract exists in real codebase |
| AP-AI-006 (CASE 2.0, project-specific) | Duplicated Controller/Service | Táº¡o `BabyController2`/second `@RequestMapping("/api/v1/baby...")` | Reject â€” must extend `BabyController.java` at existing path |

---

## PHá»¤ Lá»¤C

### A. Glossary (Thuáº­t ngá»¯)

| Thuáº­t ngá»¯ | Äá»‹nh nghÄ©a |
|-----------|-------------|
| Active Baby Profile | Baby profile hiá»‡n Ä‘ang Ä‘Æ°á»£c chá»n lÃ m ngá»¯ cáº£nh máº·c Ä‘á»‹nh trÃªn mobile app cho má»™t Mother account (is_active=true) |
| BabyOwnershipPolicy | Policy class má»›i â€” kiá»ƒm tra CHá»ˆ ownership (khÃ¡c `BabyAccessPolicy` vá»‘n cho phÃ©p cáº£ care group member) |
| Partial Unique Index | Postgres unique index chá»‰ Ã¡p dá»¥ng cho cÃ¡c row thá»a `WHERE` clause â€” dÃ¹ng Ä‘á»ƒ enforce "tá»‘i Ä‘a 1 active row / owner" |

### B. TÃ i liá»‡u tham chiáº¿u

| Document | Path |
|----------|------|
| UC-192 TDS (Approved, shipped) | `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` |
| SRS Â§3.3.12.2 | `02_Requirements/SRS/3_Functional_Specification.md` (lines 4154â€“4173) |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |
| Real shipped code (verified 2026-07-03) | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/` |

---

*EDS v2.1 â€” TÃ­ch há»£p CASE 2.0 AI Prompt Constraints (Â§17). Status: Draft â€” chá» Principal Architect / user approval trÆ°á»›c khi implement.*
