# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification â€” UC-84 View Shared Data

| Field | Value |
|-------|-------|
| **Document ID** | `CB-FAM-IMP-004` |
| **Version** | `1.0` |
| **Date** | `2026-07-02` |
| **Status** | `Approved` |
| **Document Owner** | `TV2-Bach` |
| **Author** | `AI Agent` |
| **Reviewed by** | `[Tech Lead]` |
| **DPO Sign-off** | `[ ] Pending` |
| **Approved by** | `[Principal Architect]` |
| **Last Review** | `2026-07-02` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

> **Policy 4.4 â€” Immutable History:** Never delete prior information. All changes must be logged here.

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Initial draft for UC-84 View Shared Data |
| 2026-07-08 | AI Agent â€” Amelia (Dev Agent) | Implemented shared data read with permission-flag filtering: new `SharedDataCategory` enum (CALENDAR/LOGS/ALERTS), `SharedDataItemDto`/`SharedDataResponse` DTOs, `ISharedDataService`/`SharedDataServiceImpl`, `SharedDataController` at `GET /api/v1/care-groups/{groupId}/shared-data`. LOGS category deferred (cross-domain join with postpartum_logs â€” OI-4); ALERTS reads from `notification_records` (EMERGENCY type). Unit tests not yet run (Red Gate pending). |
| 2026-07-09 | Codex Quick Dev | Verified current UC-84 service-test evidence: `SharedDataServiceImplTest` ran with 9 tests, 0 failures, 0 errors, 0 skipped. This confirms the implemented service subset only; unresolved LOGS/checklists/cross-member alert open items remain open. |

---

## Má»¤C Lá»¤C

1. [Tá»•ng quan Module](#1-tá»•ng-quan-module)
2. [Ma tráº­n Truy váº¿t (Traceability Matrix)](#2-ma-tráº­n-truy-váº¿t-traceability-matrix)
3. [Architecture Decision Records (ADR)](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling (MÃ´ hÃ¬nh TÄ©nh)](#5-static-modeling-mÃ´-hÃ¬nh-tÄ©nh)
6. [Dynamic Modeling (MÃ´ hÃ¬nh Äá»™ng)](#6-dynamic-modeling-mÃ´-hÃ¬nh-Ä‘á»™ng)
7. [Domain Event Catalog](#7-domain-event-catalog)
8. [Interface Specification (Äáº·c táº£ Giao diá»‡n)](#8-interface-specification-Ä‘áº·c-táº£-giao-diá»‡n)
9. [API Specification](#9-api-specification)
10. [Báº£ng mÃ£ lá»—i (Error Codes)](#10-báº£ng-mÃ£-lá»—i-error-codes)
11. [Quy trÃ¬nh Triá»ƒn khai (Step-by-Step)](#11-quy-trÃ¬nh-triá»ƒn-khai-step-by-step)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t](#13-ká»‹ch-báº£n-kiá»ƒm-thá»­-chi-tiáº¿t)
14. [PhÆ°Æ¡ng phÃ¡p XÃ¡c minh](#14-phÆ°Æ¡ng-phÃ¡p-xÃ¡c-minh)
15. [Máº«u thá»­ thá»±c táº¿ (API Verification Samples)](#15-máº«u-thá»­-thá»±c-táº¿-api-verification-samples)
16. [Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix)](#16-báº£ng-tá»•ng-há»£p-phÃ¢n-quyá»n-authorization-matrix)
17. [AI Prompt Constraints (CASE 2.0)](#17-ai-prompt-constraints-case-20)

---

## 1. Tá»•ng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | `ViewSharedData` |
| **Bounded Context** | `family` |
| **UC ID** | `UC-84` |
| **SRS Reference** | `Â§3.3.3.2, lines 3254-3273` |
| **Primary Actor** | `Family Member` |
| **Secondary Actors** | `Firebase Cloud Messaging` (downstream/adjacent only â€” see Â§7.2 and Open Item OI-5) |
| **Platform** | `Mobile App` |
| **Source group** | `Mobile App - Family Sync` |
| **Priority** | `Medium` |
| **Frequency** | `Frequent` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` |
| **Upstream Dependencies** | `auth, care_groups, care_group_members (permission_json), care_tasks, baby_daily_logs, postpartum_logs, safety_alerts, family_alert_log` |
| **Downstream Consumers** | `Family Sync hub UI, push-notification module (FCM, out of scope)` |

**MÃ´ táº£:** Displays shared calendar, logs, checklists, or alerts to a Family Member according to the permissions granted on their `care_group_members` record. General-purpose, category-filtered, read-only aggregation view â€” the "hub" equivalent of the narrower UC-74 (calendar-only) use case. See ADR-FAM-005 for the category scope decision and the cross-reference to UC-74 below.

**Cross-reference to UC-74 (sibling, drafted in parallel, NOT available to this agent):** UC-74 `ViewSharedCareCalendar` is a calendar/task-specific specialization (Primary Actor: User, includes BR-CONSULTATION, Priority High) â€” narrower in scope than UC-84. UC-84 is the general-purpose multi-category view (calendar + logs + checklists + alerts; Primary Actor: Family Member; BR-CONSULTATION does NOT apply; Priority Medium; includes FCM as secondary actor). Potential overlap exists on the "calendar" category â€” **Open Item OI-1**: product decision needed on whether UC-84's calendar category delegates to / reuses UC-74's underlying calendar query service to avoid duplicate implementation, or maintains an independent (but schema-identical) query. This TDS does **not** merge the two artifacts; UC-84 remains a standalone spec and only cross-references UC-74 conceptually.

---

## 2. Ma tráº­n Truy váº¿t (Traceability Matrix)

| Requirement ID | Loáº¡i | MÃ´ táº£ | ThÃ nh pháº§n Code | Compliance Target | ADR liÃªn quan |
|----------------|------|-------|-----------------|-------------------|---------------|
| UC-84 (SRS Â§3.3.3.2, L3254-3273) | Use Case | Display shared calendar/logs/checklists/alerts per granted permission | `SharedDataController.getSharedData()` | BR-RBAC, BR-PRIVACY | ADR-FAM-003, ADR-FAM-004, ADR-FAM-005 |
| BR-RBAC (SRS L3269) | Business Rule | Only ACCEPTED care-group members with the category-specific permission flag may view that category's data | `CareGroupAccessPolicy.isMember()` + `hasPermission()` | BR-RBAC | ADR-FAM-003 |
| BR-PRIVACY (SRS L3269) | Business Rule | No PII beyond what the requesting member is entitled to see (e.g., no email/phone in log/alert payloads) | Response DTO mapping | PDPA | ADR-FAM-003 |
| BR-FAM-020 | Business Rule (new, UC-84-local) | `category` param restricted to `calendar\|logs\|alerts` in v1; `checklists` rejected/Open | `SharedDataController` validation | â€” | ADR-FAM-005 |
| BR-FAM-021 | Business Rule (new, UC-84-local) | REVOKED / PENDING members always receive 403, regardless of `permission_json` content | `CareGroupAccessPolicy.isMember()` | BR-RBAC | ADR-FAM-003 |

> **Explicit note:** Per SRS source text at line 3269, only **BR-RBAC** and **BR-PRIVACY** are referenced for UC-84. **BR-CONSULTATION does NOT apply** to this use case (unlike sibling UC-74, which does reference it). Do not add BR-CONSULTATION-derived logic (e.g., "consult an expert" prompts) to this feature.

---

## 3. Architecture Decision Records (ADR)

### ADR-FAM-003 â€” Permission-filtered query design per data category

| Field | Value |
|-------|-------|
| **Status** | `Proposed` |
| **Deciders** | `Tech Lead (pending)` |
| **Date** | `2026-07-02` |

#### Bá»‘i cáº£nh
UC-84 must show different data categories (calendar, logs, checklists, alerts) to a Family Member, but visibility must be gated per-category by the member's granted permissions, not just group membership. `care_group_members.permission_json` (jsonb) is the only column reserved for this, but it currently has **zero consumers or producers anywhere in the codebase** (verified by search) â€” it is owned by the sibling workstream **UC-72 ManageFamilyPermission**, which has not landed yet.

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | Group-membership-only gating (ignore `permission_json`) | Simple, no dependency on UC-72 | Violates the UC-84 description ("according to granted permissions") â€” too coarse |
| B | Category-keyed boolean flags in `permission_json`, e.g. `{"calendar": true, "logs": false, "checklists": false, "alerts": true}` | Matches UC-84's per-category intent; aligned with UC-74's assumed shape for cross-consistency | Exact key names/shape are NOT confirmed â€” owned by UC-72 |

#### Quyáº¿t Ä‘á»‹nh
Adopt **PhÆ°Æ¡ng Ã¡n B** as a **RECOMMENDED, FLAGGED assumption**: boolean flags keyed by data category on `care_group_members.permission_json`. This TDS treats the DB column `care_group_members.permission_json jsonb` as the contract â€” NOT a specific service method or exact key name â€” so the design is resilient to UC-72 finalizing the precise shape.

**Open Item OI-2 (CRITICAL):** *Exact `permission_json` key names/shape are owned by UC-72 (sibling workstream) â€” this TDS assumes boolean flags keyed by data category (`calendar`, `logs`, `checklists`, `alerts`), aligned with UC-74's assumption for cross-consistency. Must be reconciled when UC-72 lands. If UC-72 lands with a different shape (e.g., nested objects, enum lists), `PermissionCategory` resolution logic in `CareGroupAccessPolicy.hasPermission()` must be updated â€” no other component should read `permission_json` directly.*

#### Há»‡ quáº£

**TÃ­ch cá»±c:**
- Single well-encapsulated policy method (`hasPermission`) isolates the assumption; only one class needs to change when UC-72 lands.
- Matches SRS wording ("according to granted permissions") rather than coarse group-membership-only gating.

**TiÃªu cá»±c / Trade-offs:**
- If `permission_json` is `NULL` (row created before UC-72 ships, or created by current UC-70 flow which does not populate it), all category flags MUST default to a safe value. **Decision: default missing/NULL `permission_json` or missing category key to `false` (deny-by-default)** â€” this is the least-privilege-safe choice and is documented as **BR-FAM-021**.

**Compliance Impact:**
- Deny-by-default protects against accidentally over-exposing family data before UC-72's permission UI exists â€” supports PDPA data-minimization.

---

### ADR-FAM-004 â€” On-demand query vs. materialized view

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `Tech Lead (pending)` |
| **Date** | `2026-07-02` |

#### Bá»‘i cáº£nh
Shared data spans multiple tables with different keys (`care_group_id` direct FK for `care_tasks`; `baby_id`/`owner_user_id` for logs; `recipient_user_id`/session join for alerts). A materialized/denormalized view could simplify reads but adds refresh-lag risk and migration complexity.

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | On-demand JOIN query per category, executed per request | No staleness, no extra migration, matches UC-84's "Frequent but Medium priority" profile | Slightly higher per-request query cost across categories |
| B | Materialized view refreshed on write | Faster reads at scale | Refresh-lag risk on safety-relevant alert data (unacceptable), adds operational complexity, requires new migration |

#### Quyáº¿t Ä‘á»‹nh
Chá»n **PhÆ°Æ¡ng Ã¡n A** (on-demand query), given low expected v1 volume (per-family-group data, not global feed) and the requirement in this feature family to avoid unnecessary migrations/infrastructure per CLAUDE.md delivery rules.

#### Há»‡ quáº£

**TÃ­ch cá»±c:** No new migration required for v1 categories (calendar/logs/alerts); simplest to reason about; no stale-alert risk.

**TiÃªu cá»±c / Trade-offs:** If group sizes or history depth grow substantially, pagination/indexing tuning will be needed (see Â§4.1 NFR). No trade-off mitigation implemented in v1 beyond existing indexes (e.g., `idx_care_tasks_status`).

---

### ADR-FAM-005 â€” Category scope decision (v1: calendar/logs/alerts; checklists deferred)

| Field | Value |
|-------|-------|
| **Status** | `Proposed` |
| **Deciders** | `Tech Lead (pending), Principal Architect (for checklist join path)` |
| **Date** | `2026-07-02` |

#### Bá»‘i cáº£nh
UC-84's SRS description lists four categories: calendar, logs, checklists, alerts. Verified against `V1__init_schema.sql`: no table maps 1:1 to any of these as a care-group-scoped feed.

- **calendar** â†’ `care_tasks` (direct `care_group_id` FK â€” clean join path)
- **logs** â†’ `baby_daily_logs` / `postpartum_logs` (keyed by `baby_id` / `owner_user_id`, NOT `care_group_id` â€” requires an indirect join via `care_groups.baby_id` or `care_groups.owner_user_id`)
- **checklists** â†’ `checklist_items` / `checklist_templates` (line 49/65 of `V1__init_schema.sql`) â€” **no visible FK to `care_group_id` or `baby_id` in V1**. No confirmed join path exists.
- **alerts** â†’ `family_alert_log` (session-scoped via `emergency_sessions.user_id`, aggregate-only columns: `recipient_count`, `location_included`, no per-recipient row or message content) or `safety_alerts` (`recipient_user_id`-keyed, per-user, not per-care-group). Neither table is care-group-scoped natively.

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | Ship all 4 categories in v1, inventing new FK/join columns as needed | Matches SRS description fully | Requires unreviewed schema changes for checklists; premature ahead of UC-72 permission model confirmation |
| B | Ship calendar/logs/alerts in v1 via existing joins; defer checklists as an Open Item | No premature migration; unblocks v1 delivery; checklist support added later once UC-72 lands and a real join path is confirmed | SRS description not 100% covered in v1 |

#### Quyáº¿t Ä‘á»‹nh
Chá»n **PhÆ°Æ¡ng Ã¡n B**. v1 scope = `category` query param in `{calendar, logs, alerts}`. `checklists` is explicitly **excluded from v1 and flagged as Open Item OI-3** â€” inventing a checklist-sharing join without sibling UC-72 (permission model) confirmed first is premature per task guidance. No migration is proposed in this draft for checklists.

**On "alerts" specifically â€” Open Item OI-4:** Neither `family_alert_log` (session/emergency-triage-scoped, aggregate-only) nor `safety_alerts` (recipient_user_id-keyed, per-user fall-detection/safety-monitoring) is care-group-scoped. This TDS recommends the "alerts" category join `safety_alerts.recipient_user_id` against the requesting member's own `user_id` intersected with `care_groups` membership (i.e., a family member sees safety alerts recipient-addressed to group members they have "alerts" permission for), with `family_alert_log` surfaced only as supplementary session-level metadata (sent_at, recipient_count) if the corresponding `emergency_sessions.user_id` belongs to a linked baby/journey owner in the same care group. **This is a genuine schema gap and MUST be reviewed by Principal Architect before implementation** â€” do not treat this join as confirmed.

#### Há»‡ quáº£

**TÃ­ch cá»±c:** No invented schema for v1; delivery unblocked; explicit gap tracked for follow-up.

**TiÃªu cá»±c / Trade-offs:** UC-84 does not fully satisfy its SRS description in v1 (checklists missing). Documented as Open Item, not silently dropped.

**Compliance Impact:** None â€” deferring a category is a scope decision, not a compliance risk, provided the UI clearly does not claim checklist-sharing support until it ships.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|------------------|
| Latency | API response (p99), single category | `< 400ms` | k6 load test | â€” |
| Availability | Uptime (monthly) | `99.9%` | Uptime monitor | â€” |
| Page size | Default page size / max | `20 / 100 items` | Product requirement | â€” |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Durability | Read-only endpoint, no write | N/A | â€” | â€” |
| Consistency | Permission check re-evaluated on every request (no caching of `permission_json` across requests) | 100% | Code review + integration test | PDPA |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Access control | Category-specific permission-flag enforcement | 100% | Auth Matrix Â§16 | BR-RBAC |
| PII masking | Logs/alerts category responses exclude email/phone | 100% | Response schema review | BR-PRIVACY |
| Deny-by-default | Missing/NULL `permission_json` or missing category key â†’ deny | 100% | Unit test (see Test-Spec) | ADR-FAM-003 |

### 4.4. Scalability & Capacity Planning

> Expected v1 load: per-family-group reads, not a global feed â€” bounded by care group size (product-observed max ~20 members per UC-216 NFR) and per-category record counts. On-demand query design (ADR-FAM-004) is expected to scale acceptably at this volume; revisit if group sizes or per-category history depth grow materially.

---

## 5. Static Modeling (MÃ´ hÃ¬nh TÄ©nh)

### 5.1. Class Diagram (PlantUML)

```plantuml
@startuml ViewSharedData_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

enum SharedDataCategory {
  CALENDAR
  LOGS
  ALERTS
  ' CHECKLISTS intentionally omitted in v1 â€” see ADR-FAM-005, Open Item OI-3
}

class SharedDataItemDto {
  + itemId: UUID
  + category: SharedDataCategory
  + title: String
  + occurredOrDueAt: Instant
  + status: String
  + sourceType: String
  + summary: String
}

class SharedDataResponse {
  + groupId: UUID
  + category: SharedDataCategory
  + totalItems: Integer
  + items: List<SharedDataItemDto>
  + generatedAt: Instant
}

interface ISharedDataService <<interface>> {
  + getSharedData(groupId: UUID, accountId: UUID, category: SharedDataCategory, page: int, size: int): SharedDataResponse
}

class SharedDataServiceImpl implements ISharedDataService {
  - careGroupRepository: ICareGroupRepository
  - careTaskRepository: ICareTaskRepository
  - babyDailyLogRepository: IBabyDailyLogRepository
  - postpartumLogRepository: IPostpartumLogRepository
  - safetyAlertRepository: ISafetyAlertRepository
  - accessPolicy: CareGroupAccessPolicy
  + getSharedData(groupId, accountId, category, page, size): SharedDataResponse
}

class CareGroupAccessPolicy {
  + isMember(groupId: UUID, accountId: UUID): boolean
  + hasPermission(groupId: UUID, accountId: UUID, category: PermissionCategory): boolean
}

enum PermissionCategory {
  CALENDAR
  LOGS
  CHECKLISTS
  ALERTS
}

interface ICareTaskRepository <<interface>> {
  + findByCareGroupId(groupId: UUID, pageable: Pageable): Page<CareTask>
}

SharedDataServiceImpl --> CareGroupAccessPolicy : uses
SharedDataServiceImpl --> ICareTaskRepository : uses (category=CALENDAR)
CareGroupAccessPolicy --> PermissionCategory : evaluates
SharedDataResponse *-- SharedDataItemDto : contains
SharedDataItemDto --> SharedDataCategory : tagged with

@enduml
```

### 5.2. Data Structure (Flyway SQL Migration)

> **No migration required for the v1 scope (calendar / logs / alerts categories).** All three categories are served via existing tables and existing FKs:
> - calendar: `care_tasks.care_group_id` (direct FK, already indexed via `idx_care_tasks_status` for status filtering)
> - logs: `baby_daily_logs.baby_id` / `postpartum_logs.owner_user_id`, joined via `care_groups.baby_id` / `care_groups.owner_user_id`
> - alerts: `safety_alerts.recipient_user_id` intersected with care-group membership `user_id`s (see ADR-FAM-005, Open Item OI-4 â€” join path pending Principal Architect review)
>
> **Checklists category has no confirmed join path** (`checklist_items` / `checklist_templates` have no visible FK to `care_group_id` or `baby_id` in `V1__init_schema.sql`). **No migration is proposed in this draft.** If a future architecture review confirms checklists must be care-group-shareable, a proposal would use migration version `V20260702100100` (second slot in the assigned range, since UC-74's sibling agent may have claimed `V20260702100000`) â€” but this is explicitly **not** part of this draft's scope.

```sql
-- Reference only â€” no new tables/columns in this migration-free draft.
-- care_tasks(care_task_id, care_group_id, assigned_by, assigned_to, title, status, due_at, ...)
-- baby_daily_logs(..., baby_id, ...)
-- postpartum_logs(..., owner_user_id, ...)
-- safety_alerts(safety_alert_id, safety_event_id, recipient_user_id, alert_reason, delivery_status, ...)
-- family_alert_log(id, session_id -> emergency_sessions.id, sent_at, recipient_count, location_included)  -- supplementary only, see OI-4
```

---

## 6. Dynamic Modeling (MÃ´ hÃ¬nh Äá»™ng)

### 6.1. Sequence Diagram â€” Happy Path, multi-category permission-filtered fetch (PlantUML)

```plantuml
@startuml ViewSharedData_HappyPath
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam sequenceParticipant underline
skinparam backgroundColor #FAFAFA

actor "Family Member" as Client
participant "SharedDataController" as Controller
participant "SharedDataServiceImpl" as Service
participant "CareGroupAccessPolicy" as Policy
participant "CareTaskRepository" as TaskRepo
database "PostgreSQL" as DB

Client -> Controller : GET /api/v1/care-groups/{groupId}/shared-data?category=calendar\nAuthorization: Bearer <JWT>
activate Controller

Controller -> Controller : Extract accountId from JWT (SecurityUtils.requireCurrentUserId)
Controller -> Service : getSharedData(groupId, accountId, CALENDAR, page, size)
activate Service

Service -> Policy : isMember(groupId, accountId)
activate Policy
Policy -> DB : SELECT WHERE group_id=? AND user_id=? AND invitation_status='ACCEPTED'
DB --> Policy : 1 row
Policy --> Service : true
deactivate Policy

Service -> Policy : hasPermission(groupId, accountId, CALENDAR)
activate Policy
Policy -> DB : SELECT permission_json WHERE group_id=? AND user_id=?
DB --> Policy : {"calendar": true, ...}
Policy --> Service : true
deactivate Policy

Service -> TaskRepo : findByCareGroupId(groupId, pageable)
activate TaskRepo
TaskRepo -> DB : SELECT * FROM care_tasks WHERE care_group_id=?
DB --> TaskRepo : task rows
deactivate TaskRepo

Service --> Controller : SharedDataResponse(category=CALENDAR, items=[...])
deactivate Service

Controller --> Client : 200 OK\n{groupId, category, totalItems, items[...]}
deactivate Controller

@enduml
```

### 6.2. Sequence Diagram â€” Alt path: no permission / empty result (PlantUML)

```plantuml
@startuml ViewSharedData_AltPath_NoPermission
skinparam backgroundColor #FAFAFA

actor "Family Member (ACCEPTED, no logs permission)" as Client
participant "SharedDataController" as Controller
participant "SharedDataServiceImpl" as Service
participant "CareGroupAccessPolicy" as Policy
database "PostgreSQL" as DB

Client -> Controller : GET /api/v1/care-groups/{groupId}/shared-data?category=logs
Controller -> Service : getSharedData(groupId, accountId, LOGS, page, size)
Service -> Policy : isMember(groupId, accountId)
Policy -> DB : SELECT ... invitation_status='ACCEPTED'
DB --> Policy : 1 row
Policy --> Service : true

Service -> Policy : hasPermission(groupId, accountId, LOGS)
Policy -> DB : SELECT permission_json WHERE group_id=? AND user_id=?
DB --> Policy : {"logs": false} (or missing key / NULL permission_json)
Policy --> Service : false (deny-by-default, ADR-FAM-003)

Service -> Service : throw ForbiddenException(FAM-011)
Service --> Controller : ForbiddenException
Controller --> Client : 403 Forbidden\n{"error": {"code": "FAM-011"}}

note right of Service
  AF2 (no matching data, e.g. category has
  permission=true but zero rows) is a
  DIFFERENT path: returns 200 with empty
  items[] â€” NOT 403. See Â§9.2 empty-state example.
end note

@enduml
```

### 6.3. State Machine

> Not applicable â€” UC-84 is a stateless, read-only view. No entity state transitions are owned by this module. `care_group_members.invitation_status` and `care_tasks.status` state machines are owned by their respective write-side use cases (UC-71/72/73/83), not by UC-84.

---

## 7. Domain Event Catalog

### 7.1. Events Published (PhÃ¡t ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| â€” | Read-only endpoint â€” no events published | â€” | â€” | â€” | â€” |

### 7.2. Events Consumed (TiÃªu thá»¥)

| Event Name | Source | Handler | Action thá»±c hiá»‡n |
|------------|--------|---------|------------------|
| â€” | None directly consumed | â€” | Relies on sibling UC-72 (`ManageFamilyPermission`) permission updates and UC-73 (`AssignFamilyTask`) task assignment to keep source data current. **No formal event contract exists yet â€” Open Item OI-6.** UC-84 always reads the current DB state at request time (no event-driven cache invalidation needed given ADR-FAM-004's on-demand query design). |

> **FCM relationship (downstream/adjacent, explicitly OUT OF SCOPE for this UC):** Firebase Cloud Messaging is listed as a secondary actor in the SRS for UC-84. This implies that when new shared data appears (e.g., a new care task or alert), a push notification MAY be sent to family members. **UC-84 itself does not send FCM notifications** â€” that responsibility belongs to separate "Receive X Notification" use cases (e.g., UC-158/159/160/161 per `04_Implement/`). UC-84 is a pull/read endpoint only. Any FCM triggering logic must NOT be implemented as part of this TDS's scope â€” if requested, it requires a new ADR and is out of bounds for `SharedDataServiceImpl`.

---

## 8. Interface Specification (Äáº·c táº£ Giao diá»‡n)

### 8.1. Service Interface

```java
// SharedDataCategory.java â€” v1 scope only
// @version 1.0
public enum SharedDataCategory {
    CALENDAR,
    LOGS,
    ALERTS
    // CHECKLISTS intentionally omitted â€” ADR-FAM-005, Open Item OI-3
}

// SharedDataItemDto.java
public class SharedDataItemDto {
    private UUID itemId;             // source-table primary key (care_task_id / log id / safety_alert_id)
    private SharedDataCategory category;
    private String title;            // e.g. care_tasks.title, or a derived label for logs/alerts
    private Instant occurredOrDueAt; // due_at (calendar) / measured_date-derived (logs) / sent_at (alerts)
    private String status;           // raw status string â€” NOTE: care_tasks.status has NO CHECK constraint (app-level only)
    private String sourceType;       // "CARE_TASK" | "BABY_DAILY_LOG" | "POSTPARTUM_LOG" | "SAFETY_ALERT"
    private String summary;          // short, PII-safe description â€” NEVER includes email/phone
    // getters / setters
}

// SharedDataResponse.java
public class SharedDataResponse {
    private UUID groupId;
    private SharedDataCategory category;
    private Integer totalItems;
    private List<SharedDataItemDto> items;
    private Instant generatedAt;
    // getters / setters
}

// ISharedDataService.java â€” Service Contract
// @version 1.0
public interface ISharedDataService {
    /**
     * Returns permission-filtered shared data for one category.
     * @throws NotFoundException (FAM-010) khi care group khÃ´ng tá»“n táº¡i
     * @throws ForbiddenException (FAM-003) khi caller khÃ´ng pháº£i ACCEPTED member (reused from UC-216's code, not renumbered)
     * @throws ForbiddenException (FAM-011) khi caller lÃ  ACCEPTED member nhÆ°ng category permission flag = false / missing / permission_json NULL
     * @throws BusinessException (FAM-012) khi category param khÃ´ng há»£p lá»‡ (khÃ´ng thuá»™c calendar|logs|alerts) hoáº·c = "checklists" (explicitly deferred, Open Item OI-3)
     */
    SharedDataResponse getSharedData(UUID groupId, UUID accountId, SharedDataCategory category, int page, int size);
}
```

### 8.2. Repository Interface

```java
// Extends existing family-package repositories; no new tables.
// ICareTaskRepository.java (new, family-adjacent â€” or reuse existing if UC-73/UC-74 sibling already defines it; contract is the method signature below)
public interface ICareTaskRepository extends JpaRepository<CareTask, UUID> {
    Page<CareTask> findByCareGroupId(UUID careGroupId, Pageable pageable);
}

// IBabyDailyLogRepository.java / IPostpartumLogRepository.java â€” existing/likely-existing repositories, reused read-only
public interface IBabyDailyLogRepository extends JpaRepository<BabyDailyLog, UUID> {
    Page<BabyDailyLog> findByBabyId(UUID babyId, Pageable pageable);
}

public interface IPostpartumLogRepository extends JpaRepository<PostpartumLog, UUID> {
    Page<PostpartumLog> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}

// ISafetyAlertRepository.java â€” existing/likely-existing, reused read-only
// Open Item OI-4: exact join predicate (recipient_user_id vs care-group membership set) pending Principal Architect review
public interface ISafetyAlertRepository extends JpaRepository<SafetyAlert, UUID> {
    Page<SafetyAlert> findByRecipientUserIdIn(List<UUID> recipientUserIds, Pageable pageable);
}

// CareGroupAccessPolicy.java â€” extended from UC-216's ADR-FAM-002 pattern
public interface PermissionCategoryCheck {
    boolean hasPermission(UUID groupId, UUID accountId, PermissionCategory category);
}
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `GET` | `/api/v1/care-groups/{groupId}/shared-data?category={calendar\|logs\|alerts}&page={n}&size={n}` | JWT Bearer | Any authenticated account that is an ACCEPTED care-group member (`ROLE_MOTHER`, `ROLE_FAMILY`, etc. â€” role-agnostic, membership-gated) | 120/min | Yes |

### 9.2. Request / Response Schemas

#### `GET /api/v1/care-groups/{groupId}/shared-data?category=calendar`

**Response â€” 200 OK (Happy Path):**
```json
{
  "groupId": "group-uuid",
  "category": "CALENDAR",
  "totalItems": 2,
  "generatedAt": "2026-07-02T08:00:00.000Z",
  "items": [
    {
      "itemId": "task-uuid-1",
      "category": "CALENDAR",
      "title": "Prenatal checkup",
      "occurredOrDueAt": "2026-07-05T09:00:00.000Z",
      "status": "OPEN",
      "sourceType": "CARE_TASK",
      "summary": "Assigned to Tran Van B"
    },
    {
      "itemId": "task-uuid-2",
      "category": "CALENDAR",
      "title": "Buy prenatal vitamins",
      "occurredOrDueAt": "2026-07-03T00:00:00.000Z",
      "status": "COMPLETED",
      "sourceType": "CARE_TASK",
      "summary": "Completed by Nguyen Thi A"
    }
  ]
}
```

**Response â€” 200 OK (AF2: Empty state, permission granted but no matching data):**
```json
{
  "groupId": "group-uuid",
  "category": "ALERTS",
  "totalItems": 0,
  "generatedAt": "2026-07-02T08:00:00.000Z",
  "items": []
}
```

**Response â€” 400 Bad Request (invalid/deferred category):**
```json
{
  "error": {
    "code": "FAM-012",
    "message": "Unsupported category. Supported: calendar, logs, alerts. 'checklists' is not yet available.",
    "details": [{ "field": "category", "message": "must be one of [calendar, logs, alerts]" }]
  }
}
```

**Response â€” 403 Forbidden (not a member / PENDING / REVOKED):**
```json
{
  "error": {
    "code": "FAM-003",
    "message": "Not a group member"
  }
}
```

**Response â€” 403 Forbidden (member, but category permission = false/missing):**
```json
{
  "error": {
    "code": "FAM-011",
    "message": "You do not have permission to view this category"
  }
}
```

**Response â€” 404 Not Found:**
```json
{
  "error": {
    "code": "FAM-010",
    "message": "Care group not found"
  }
}
```

---

## 10. Báº£ng mÃ£ lá»—i (Error Codes)

> **FAM-003 is reused verbatim from UC-216 (`Not a group member`) for consistency â€” not re-numbered.** All new UC-84-specific codes start at `FAM-010` to reduce collision risk with sibling UC-74's likely-claimed range. **Final numbering is TBD at implementation time** if a collision with UC-74's actual chosen codes is discovered.

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `FAM-003` | 403 | Not a group member | KhÃ´ng pháº£i thÃ nh viÃªn cá»§a nhÃ³m | Caller's `invitation_status` â‰  ACCEPTED (reused from UC-216) |
| `FAM-010` | 404 | Care group not found | KhÃ´ng tÃ¬m tháº¥y nhÃ³m | `groupId` khÃ´ng tá»“n táº¡i trong DB |
| `FAM-011` | 403 | Category permission denied | KhÃ´ng cÃ³ quyá»n xem danh má»¥c nÃ y | ACCEPTED member, nhÆ°ng `permission_json` category flag = false, missing, hoáº·c `permission_json` NULL (deny-by-default, ADR-FAM-003) |
| `FAM-012` | 400 | Unsupported category | Danh má»¥c khÃ´ng Ä‘Æ°á»£c há»— trá»£ | `category` param khÃ´ng thuá»™c `{calendar, logs, alerts}` (bao gá»“m `checklists`, deferred) |
| `FAM-013` | 500 | Internal error | Lá»—i há»‡ thá»‘ng | Lá»—i DB khÃ´ng xÃ¡c Ä‘á»‹nh |

---

## 11. Quy trÃ¬nh Triá»ƒn khai (Step-by-Step)

### 11.1. Prerequisites

- [ ] Tables `care_groups`, `care_group_members`, `care_tasks`, `baby_daily_logs`, `postpartum_logs`, `safety_alerts` already exist (verified in `V1__init_schema.sql`)
- [ ] `CareGroupAccessPolicy.isMember()` already exists (from UC-216, ADR-FAM-002) â€” reused, not reimplemented
- [ ] No new migration required for v1 scope (calendar/logs/alerts) â€” see Â§5.2
- [ ] Open Item OI-4 (alerts join path) reviewed/confirmed by Principal Architect before `ALERTS` category is implemented â€” if unresolved at implementation time, ship `CALENDAR` + `LOGS` first and gate `ALERTS` behind a feature flag or defer

### 11.2. Pre-Migration Checklist

Not applicable â€” no migration in this draft.

### 11.3. Implementation Steps

#### Cháº·ng 1 â€” Extend `CareGroupAccessPolicy` with `hasPermission()`

```java
public boolean hasPermission(UUID groupId, UUID accountId, PermissionCategory category) {
    CareGroupMember member = memberRepository.findByCareGroupIdAndUserIdAndInvitationStatus(
        groupId, accountId, InviteStatus.ACCEPTED
    ).orElse(null);
    if (member == null || member.getPermissionJson() == null) {
        return false; // deny-by-default, ADR-FAM-003
    }
    JsonNode flags = parsePermissionJson(member.getPermissionJson());
    JsonNode flag = flags.get(category.name().toLowerCase());
    return flag != null && flag.asBoolean(false);
}
```

#### Cháº·ng 2 â€” Implement `SharedDataServiceImpl.getSharedData()`

```java
@Override
public SharedDataResponse getSharedData(UUID groupId, UUID accountId, SharedDataCategory category, int page, int size) {
    careGroupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("FAM-010"));
    if (!accessPolicy.isMember(groupId, accountId)) {
        throw new ForbiddenException("FAM-003");
    }
    PermissionCategory permCategory = PermissionCategory.valueOf(category.name());
    if (!accessPolicy.hasPermission(groupId, accountId, permCategory)) {
        throw new ForbiddenException("FAM-011");
    }
    List<SharedDataItemDto> items = switch (category) {
        case CALENDAR -> mapTasks(careTaskRepository.findByCareGroupId(groupId, PageRequest.of(page, size)));
        case LOGS -> mapLogs(groupId, PageRequest.of(page, size));
        case ALERTS -> mapAlerts(groupId, PageRequest.of(page, size)); // Open Item OI-4 join
    };
    return new SharedDataResponse(groupId, category, items.size(), items, Instant.now());
}
```

#### Cháº·ng 3 â€” Implement `SharedDataController`

```java
@GetMapping("/api/v1/care-groups/{groupId}/shared-data")
public ResponseEntity<ApiResponse<SharedDataResponse>> getSharedData(
    @PathVariable UUID groupId,
    @RequestParam String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    Principal principal
) {
    UUID accountId = SecurityUtils.requireCurrentUserId(principal);
    SharedDataCategory parsed = parseCategoryOrThrow(category); // throws BusinessException FAM-012 if invalid/checklists
    return ResponseEntity.ok(ApiResponse.success(
        sharedDataService.getSharedData(groupId, accountId, parsed, page, size)
    ));
}
```

#### Cháº·ng 4 â€” Verification sau deploy

```bash
curl -X GET https://[host]/api/v1/health
# Expected: {"status": "ok"}
```

### 11.4. Deployment Checklist

- [ ] Health check endpoint tráº£ vá» 200
- [ ] KhÃ´ng cÃ³ migration má»›i cáº§n cháº¡y
- [ ] Verify `category=checklists` returns 400 FAM-012 (not silently ignored)
- [ ] Verify a member with `permission_json` = NULL receives 403 FAM-011 for every category (deny-by-default)
- [ ] Log khÃ´ng chá»©a PII (email/phone) trong `summary` fields

---

## 12. Rollback & Incident Runbook

### 12.1. Äiá»u kiá»‡n kÃ­ch hoáº¡t Rollback

| Äiá»u kiá»‡n | NgÆ°á»¡ng | NgÆ°á»i quyáº¿t Ä‘á»‹nh |
|-----------|--------|------------------|
| Error rate tÄƒng Ä‘á»™t biáº¿n | > 5% trong 5 phÃºt | On-call Engineer |
| Latency p99 vÆ°á»£t ngÆ°á»¡ng | > 2x baseline (800ms) | On-call Engineer |
| Permission bypass detected (member sees category with flag=false) | Báº¥t ká»³ case nÃ o | Tech Lead + DPO |

### 12.2. Rollback Procedure

No DB migration for this module â†’ rollback is a code-only revert:

```bash
# BÆ°á»›c 1: Re-deploy phiÃªn báº£n cÅ©
kubectl rollout undo deployment/carebridge-api

# BÆ°á»›c 2: Verify rollback thÃ nh cÃ´ng
kubectl rollout status deployment/carebridge-api
curl -X GET https://[host]/api/v1/health

# BÆ°á»›c 3: Smoke test
curl -X GET https://[host]/api/v1/care-groups/{groupId}/shared-data?category=calendar \
  -H "Authorization: Bearer <valid_member_token>"
# Expected: 200 OK
```

### 12.3. Notification Protocol

| Thá»i Ä‘iá»ƒm | NgÆ°á»i nháº­n | KÃªnh | Template |
|-----------|------------|------|----------|
| Ngay khi phÃ¡t hiá»‡n | On-call team | Slack `#incident` | "ðŸš¨ UC-84 incident: [mÃ´ táº£]" |
| Náº¿u permission bypass / PII leak | DPO | Email | Báº¯t buá»™c â€” PDPA |

### 12.4. Post-Incident Review (PIR)

> Báº¯t buá»™c hoÃ n thÃ nh trong vÃ²ng 48 giá» sau khi resolve incident.

- **Timeline:** Diá»…n biáº¿n chi tiáº¿t
- **Root Cause:** 5 Whys analysis
- **Impact:** Sá»‘ family members bá»‹ áº£nh hÆ°á»Ÿng, category nÃ o bá»‹ lá»™ sai quyá»n?
- **Prevention:** Action items Ä‘á»ƒ trÃ¡nh tÃ¡i diá»…n (e.g., add permission-flag regression test)

---

## 13. Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t

> **Policy (EDS v2.0):** All test scenarios use `SYNTHETIC` data. Full detail lives in the companion Test-Spec `CB-FAM-TDD-004`.

### 13.1. Unit Tests (summary â€” see Test-Spec for full cases)

#### TC-UNIT-001 â€” Member with calendar=true views calendar category

```gherkin
Feature: View Shared Data
  Background:
    Given test data classification: SYNTHETIC
    And care group CG-001 exists with ACC-001 as ACCEPTED member
    And ACC-001's permission_json = {"calendar": true, "logs": false, "alerts": true}

  Scenario: calendar=true â†’ 200 with items
    When getSharedData(CG-001, ACC-001, CALENDAR) is called
    Then response contains care_tasks scoped to CG-001

  Scenario: logs=false â†’ 403 FAM-011
    When getSharedData(CG-001, ACC-001, LOGS) is called
    Then throws ForbiddenException FAM-011
```

**HÃ m Ä‘Æ°á»£c test:** `SharedDataServiceImpl.getSharedData()`
**Invariant kiá»ƒm tra:** Permission flag is re-evaluated every call; false/missing/NULL always denies.

### 13.2. Integration Tests

#### TC-INT-001 â€” Full flow with DB, mixed permission combos

```gherkin
  Scenario: Service + Repository phá»‘i há»£p Ä‘Ãºng cho tá»«ng category
    Given database has CG-001 with permission_json combos: all-true, all-false, mixed, NULL
    When SharedDataServiceImpl.getSharedData is called for each combo Ã— each category
    Then results match the deny-by-default and category-scoping rules exactly
```

### 13.3. E2E / Security Tests

#### TC-E2E-001 â€” Full API flow

```gherkin
  Scenario: ACCEPTED member with alerts=true â†’ 200
    Given ACC-001 has valid JWT, invitation_status=ACCEPTED, permission_json.alerts=true
    When GET /api/v1/care-groups/{groupId}/shared-data?category=alerts is called
    Then response status is 200

  Scenario: checklists category â†’ 400
    When GET .../shared-data?category=checklists is called
    Then response status is 400 with code FAM-012
```

---

## 14. PhÆ°Æ¡ng phÃ¡p XÃ¡c minh

### 14.1. Database Inspection

```sql
-- Verify permission_json shape assumption (Open Item OI-2) against real seeded data
SELECT care_group_member_id, user_id, permission_json
FROM care_group_members
WHERE care_group_id = '<groupId>';

-- Verify care_tasks scoping (calendar category)
SELECT care_task_id, care_group_id, status FROM care_tasks WHERE care_group_id = '<groupId>';
```

### 14.2. Log / Audit Verification

```bash
# Verify no PII in logs
kubectl logs -l app=carebridge-api | grep -i "email\|phone\|@"
# Expected: No output related to shared-data responses

# Verify FAM-011 denial events are logged (for security monitoring, not a formal audit trail per POST-3 unless flagged sensitive)
kubectl logs -l app=carebridge-api | grep "FAM-011" | head -5
```

### 14.3. Tool-based Verification

```bash
echo "<JWT_TOKEN>" | cut -d'.' -f2 | base64 -d | jq '.sub'
openssl s_client -connect [host]:443 -tls1_3 2>&1 | grep "Protocol"
```

---

## 15. Máº«u thá»­ thá»±c táº¿ (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X GET "https://[host]/api/v1/care-groups/CG-001/shared-data?category=calendar" \
  -H "Authorization: Bearer <ACCEPTED_MEMBER_JWT>" \
  -H "X-Correlation-Id: $(uuidgen)"
```

**Expected Response (200):** see Â§9.2 example.

### 15.2. Error Paths

```bash
# Permission flag false â†’ 403 FAM-011
curl -X GET "https://[host]/api/v1/care-groups/CG-001/shared-data?category=logs" \
  -H "Authorization: Bearer <MEMBER_WITH_LOGS_FALSE_JWT>"
```

**Expected Response (403):**
```json
{ "error": { "code": "FAM-011", "message": "You do not have permission to view this category" } }
```

```bash
# Invalid/deferred category â†’ 400 FAM-012
curl -X GET "https://[host]/api/v1/care-groups/CG-001/shared-data?category=checklists" \
  -H "Authorization: Bearer <ACCEPTED_MEMBER_JWT>"
```

**Expected Response (400):**
```json
{ "error": { "code": "FAM-012", "message": "Unsupported category. Supported: calendar, logs, alerts. 'checklists' is not yet available." } }
```

---

## 16. Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix)

> NguyÃªn táº¯c **Least Privilege**: ACCEPTED membership is necessary but NOT sufficient â€” the category-specific permission flag must also be `true`. Deny-by-default applies when `permission_json` is NULL or missing the category key (ADR-FAM-003).

| Endpoint | `GUEST` | `MEMBER (ACCEPTED, flag=true)` | `MEMBER (ACCEPTED, flag=false/missing)` | `MEMBER (PENDING)` | `MEMBER (REVOKED)` | `ADMIN` |
|----------|---------|-------------------------------|------------------------------------------|---------------------|----------------------|---------|
| `GET /api/v1/care-groups/:id/shared-data?category=calendar` | âŒ 401 | âœ… 200 | âŒ 403 FAM-011 | âŒ 403 FAM-003 | âŒ 403 FAM-003 | âœ… All |
| `GET /api/v1/care-groups/:id/shared-data?category=logs` | âŒ 401 | âœ… 200 | âŒ 403 FAM-011 | âŒ 403 FAM-003 | âŒ 403 FAM-003 | âœ… All |
| `GET /api/v1/care-groups/:id/shared-data?category=alerts` | âŒ 401 | âœ… 200 | âŒ 403 FAM-011 | âŒ 403 FAM-003 | âŒ 403 FAM-003 | âœ… All |
| `GET /api/v1/care-groups/:id/shared-data?category=checklists` | âŒ 401 | âŒ 400 FAM-012 (all roles) | âŒ 400 FAM-012 | âŒ 400 FAM-012 | âŒ 400 FAM-012 | âŒ 400 FAM-012 |

**ChÃº thÃ­ch:**
- âœ… = Allowed
- âŒ = Denied
- Membership check (FAM-003) is evaluated **before** permission-flag check (FAM-011) â€” consistent ordering with UC-216's `isMember` pattern.
- `checklists` is rejected at input-validation time (400), before any authorization check runs, since it is not a supported category in v1 â€” this is a deliberate, consistent choice; justification: it avoids leaking whether a group "would" have granted checklist permission, since the category doesn't exist as a queryable resource yet.

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source (ADR/BR) | Last Verified |
|---|-----------|-----------------|---------------|
| C1 | `getSharedData()` MUST check `isMember()` (ACCEPTED only) BEFORE checking `hasPermission()` â€” never skip or reorder | ADR-FAM-002 (reused), BR-RBAC | 2026-07-02 |
| C2 | `hasPermission()` MUST deny (return `false`) when `permission_json` is NULL or the category key is missing â€” NEVER default to `true` | ADR-FAM-003 | 2026-07-02 |
| C3 | Only categories `{calendar, logs, alerts}` are implemented in v1; `checklists` MUST be rejected with `FAM-012` (400) at the controller validation layer, not silently ignored or treated as `true`/`false` | ADR-FAM-005 | 2026-07-02 |
| C4 | `accountId` MUST come from JWT SecurityContext (`SecurityUtils.requireCurrentUserId`) â€” NEVER from URL path or request body | BR-RBAC | 2026-07-02 |
| C5 | Response DTOs (`SharedDataItemDto`) MUST NOT include email/phone or other fields beyond `title`/`status`/`summary`/timestamps â€” no raw entity passthrough | BR-PRIVACY | 2026-07-02 |
| C6 | No FCM-sending logic may be added to `SharedDataServiceImpl` or `SharedDataController` â€” this UC is read-only; FCM triggering belongs to separate notification use cases | SRS Â§3.3.3.2 secondary actor note | 2026-07-02 |
| C7 | The "alerts" category join (Open Item OI-4) is NOT confirmed â€” implementer MUST get Principal Architect sign-off on the `safety_alerts`/`family_alert_log` join predicate before shipping `ALERTS`; if unconfirmed at implementation time, ship `CALENDAR`+`LOGS` only and defer `ALERTS` | ADR-FAM-005, OI-4 | 2026-07-02 |

### 17.2 Constraint Injection Block (Copy-Paste vÃ o AI Prompt)

```
[CONSTRAINT BLOCK â€” Module: ViewSharedData (CB-FAM-IMP-004)]
Theo TDS CB-FAM-IMP-004 vÃ  cÃ¡c ADR liÃªn quan:

1. (C1 â€” ADR-FAM-002/BR-RBAC) isMember() (ACCEPTED only) pháº£i Ä‘Æ°á»£c check TRÆ¯á»šC hasPermission().
2. (C2 â€” ADR-FAM-003) hasPermission() pháº£i deny (false) khi permission_json NULL hoáº·c thiáº¿u category key â€” KHÃ”NG default true.
3. (C3 â€” ADR-FAM-005) Chá»‰ implement categories {calendar, logs, alerts}. "checklists" pháº£i tráº£ 400 FAM-012, khÃ´ng Ä‘Æ°á»£c silently allow/deny.
4. (C4 â€” BR-RBAC) accountId láº¥y tá»« JWT SecurityContext, KHÃ”NG tá»« URL/body.
5. (C5 â€” BR-PRIVACY) SharedDataItemDto KHÃ”NG chá»©a email/phone hay entity passthrough.
6. (C6) KHÃ”NG thÃªm FCM-sending logic vÃ o module nÃ y â€” chá»‰ Ä‘á»c dá»¯ liá»‡u.
7. (C7 â€” Open Item OI-4) "alerts" category join CHÆ¯A Ä‘Æ°á»£c confirm â€” cáº§n Principal Architect sign-off trÆ°á»›c khi ship; náº¿u chÆ°a cÃ³, chá»‰ ship calendar+logs.

[CONTEXT BLOCK]
- Bounded Context: family
- Data Classification: PII
- Compliance: PDPA, BR-RBAC, BR-PRIVACY
- Existing interfaces: Â§8 Service Interface + Repository Interface
- Error codes: FAM-003 (403, reused), FAM-010 (404), FAM-011 (403), FAM-012 (400), FAM-013 (500)
- Auth matrix: Â§16

[TASK BLOCK]
Implement SharedDataServiceImpl.getSharedData() thá»a mÃ£n constraints trÃªn.
Output pháº£i tuÃ¢n thá»§ Â§8 Interface Specification.
Tests pháº£i cover Â§13 Test Scenarios vÃ  companion Test-Spec CB-FAM-TDD-004.
```

### 17.3 Constraint Quality Checklist

- [x] Má»—i constraint traceable vá» ADR hoáº·c BR cá»¥ thá»ƒ
- [x] KhÃ´ng cÃ³ constraint generic
- [x] Má»—i constraint cÃ³ `Last Verified` date
- [x] Constraint block cÃ³ â‰¥ 5 constraints cá»¥ thá»ƒ (7 total)
- [x] Constraint block reference Â§8 Interface
- [x] Constraint block reference Â§16 Auth Matrix

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | HÃ nh Ä‘á»™ng |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code khÃ´ng check permission_json flag trÆ°á»›c khi tráº£ data | Reject â€” inject láº¡i C1/C2 |
| AP-AI-003 | Implicit Decision | Code tá»± bá»‹a checklist join column mÃ  khÃ´ng cÃ³ ADR review | Reject â€” C3/C7 violation, escalate to Principal Architect |
| AP-AI-005 | Hallucinated Contract | Code import repository/service method khÃ´ng cÃ³ trong Â§8 | Reject â€” verify contract existence |

---

## PHá»¤ Lá»¤C

### A. Glossary (Thuáº­t ngá»¯)

| Thuáº­t ngá»¯ | Äá»‹nh nghÄ©a |
|-----------|------------|
| Care Group | Family group consisting of a mother and support members |
| permission_json | jsonb column on `care_group_members`, reserved for UC-72's fine-grained permission model; shape ASSUMED in this TDS (Open Item OI-2) |
| Deny-by-default | Missing/NULL permission data results in denial, not implicit allow |
| PII | Personally Identifiable Information |
| Least Privilege | Grant only the minimum access necessary |

### B. TÃ i liá»‡u tham chiáº¿u

| Document | Link / Path |
|----------|-------------|
| SRS Â§3.3.3.2 UC-84 | `02_Requirements/SRS/3_Functional_Specification.md` (L3254-3273) |
| UC-216 TDS (sibling pattern reference) | `04_Implement/UC216_ViewCareGroupMembers/UC216_ViewCareGroupMembers_TDS.md` |
| Function-spec task allocation | `04_Implement/implement_artifacts/function-spec-task-allocation.md` (L476-518) |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |
| DB schema baseline | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` |
| family_alert_log migration | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000004__create_family_alert_log.sql` |

---

## OPEN ITEMS SUMMARY (for reviewer convenience)

| ID | Description | Blocking? | Owner |
|----|-------------|-----------|-------|
| OI-1 | UC-84 calendar category vs. UC-74's calendar query service â€” delegate or duplicate? | No (v1 can proceed independently) | Product / Tech Lead |
| OI-2 | `permission_json` exact shape not confirmed â€” assumed boolean flags per category | Yes, for hasPermission() correctness | UC-72 owner |
| OI-3 | Checklists category has no confirmed join path â€” deferred from v1 | No (excluded from scope) | Principal Architect |
| OI-4 | "Alerts" category join predicate (`safety_alerts` vs `family_alert_log` vs care-group membership) not confirmed | Yes, for ALERTS category specifically | Principal Architect |
| OI-5 | FCM secondary-actor relationship â€” confirm no overlap with existing notification UCs | No (explicitly out of scope) | Product |
| OI-6 | No formal event contract with UC-72/UC-73 â€” relies on read-time DB consistency only | No (accepted for v1 given on-demand query design) | Tech Lead |

---

*EDS v2.1 â€” TÃ­ch há»£p CASE 2.0 AI Prompt Constraints (Â§17).*
