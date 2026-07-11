# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification â€” UC-194 View Baby Daily Log Detail

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
| 2026-07-03 | AI Agent | Táº¡o tÃ i liá»‡u láº§n Ä‘áº§u cho UC-194 View Baby Daily Log Detail |

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
16. [Authorization Matrix](#16-báº£ng-tá»•ng-há»£p-phÃ¢n-quyá»n-authorization-matrix)
17. [AI Prompt Constraints (CASE 2.0)](#17-ai-prompt-constraints-case-20)

---

## 1. Tá»•ng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | `ViewBabyDailyLogDetail` |
| **Bounded Context** | `baby` (reuse â€” same bounded context as UC192 `BabyController`/`BabyServiceImpl`, NOT the empty `babyCare` stub folder) |
| **UC ID** | `UC-194` |
| **SRS Reference** | `3.3.12.3` (`02_Requirements/SRS/3_Functional_Specification.md` lines 4175-4194) |
| **Primary Actor** | `Mother (ROLE_MOTHER)` |
| **Platform** | `Mobile App` |
| **Priority** | `Medium` |
| **Sprint** | `Sprint 4 â€” Device Sync And Care Edge Cases` |
| **Owner** | `TV2-BÃ¡ch` |
| **Data Classification** | `Sensitive-PII` (infant health/feeding/sleep data) |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SAFETY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy â€” UC192)`, `auth`, `baby_daily_logs` table |
| **Downstream Consumers** | `Baby Daily Log List (future UC)`, `UC195 Delete Baby Daily Log` |

**MÃ´ táº£:** Hiá»ƒn thá»‹ chi tiáº¿t Ä‘áº§y Ä‘á»§ (content, timestamp, type) cho Má»˜T báº£n ghi nháº­t kÃ½ háº±ng ngÃ y cá»§a baby (`baby_daily_logs`). Chá»‰ Mother lÃ  owner cá»§a baby profile liÃªn quan má»›i Ä‘Æ°á»£c xem â€” ownership resolved qua chain `baby_daily_logs.baby_id â†’ baby_profiles.owner_user_id`, tÃ¡i sá»­ dá»¥ng `BabyAccessPolicy` Ä‘Ã£ cÃ³ tá»« UC192. ÄÃ¢y lÃ  greenfield code: KHÃ”NG cÃ³ `BabyDailyLog` entity/controller/service nÃ o tá»“n táº¡i trong codebase hiá»‡n táº¡i (xÃ¡c nháº­n qua RG-3 bÃªn dÆ°á»›i).

---

## 2. Ma tráº­n Truy váº¿t (Traceability Matrix)

| Requirement ID | Loáº¡i | MÃ´ táº£ | ThÃ nh pháº§n Code | Compliance Target | ADR liÃªn quan |
|----------------|------|-------|-----------------|-------------------|---------------|
| UC-194 | Use Case | Mother xem chi tiáº¿t 1 baby daily log | `BabyDailyLogController.getDailyLogDetail()` | BR-RBAC | ADR-BABY-004 |
| BR-RBAC | Business Rule | Chá»‰ owner cá»§a baby profile má»›i xem Ä‘Æ°á»£c log | `BabyDailyLogServiceImpl.getDailyLogDetail()` + `BabyAccessPolicy.canView()` (reused from UC192) | BR-RBAC | ADR-BABY-004 |
| BR-PRIVACY | Business Rule | Response chá»‰ tráº£ field liÃªn quan (content/timestamp/type) â€” minimum-necessary | `BabyDailyLogDetailResponse` DTO | BR-PRIVACY | ADR-BABY-004 |
| BR-SAFETY | Business Rule | Log content lÃ  mÃ´ táº£ sinh hoáº¡t (feeding/sleep/diaper), khÃ´ng Ä‘Æ°á»£c diá»…n giáº£i thÃ nh cháº©n Ä‘oÃ¡n y táº¿ | `BabyDailyLogDetailResponse` â€” khÃ´ng cÃ³ trÆ°á»ng `diagnosis`/`interpretation` | BR-SAFETY | ADR-BABY-005 |

---

## 3. Architecture Decision Records (ADR)

### ADR-BABY-004 â€” Ownership Chain Reuse cho Baby Daily Log Access

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `TV2-BÃ¡ch, AI Agent` |
| **Date** | `2026-07-03` |
| **Supersedes** | â€” |

#### Bá»‘i cáº£nh (Context)
`baby_daily_logs` khÃ´ng cÃ³ `owner_user_id` trá»±c tiáº¿p â€” chá»‰ cÃ³ `baby_id` (FK â†’ `baby_profiles.baby_id`). UC192 Ä‘Ã£ thiáº¿t láº­p `BabyAccessPolicy.canView(BabyProfile, callerId)` Ä‘á»ƒ kiá»ƒm tra ownership + care group membership (ACCEPTED). Cáº§n quyáº¿t Ä‘á»‹nh: viáº¿t láº¡i logic ownership riÃªng cho daily log, hay tÃ¡i sá»­ dá»¥ng policy Ä‘Ã£ cÃ³.

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t (Options Considered)

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | Viáº¿t `BabyDailyLogAccessPolicy` riÃªng, duplicate logic ownership | Isolation module | TrÃ¹ng láº·p code, dá»… lá»‡ch pha khi UC192 policy thay Ä‘á»•i |
| B | Load `BabyProfile` qua `baby_id`, tÃ¡i sá»­ dá»¥ng `BabyAccessPolicy.canView()` hiá»‡n cÃ³ | Nháº¥t quÃ¡n 100% vá»›i UC192, má»™t nguá»“n sá»± tháº­t duy nháº¥t cho access rule | ThÃªm 1 query `BabyProfileRepository.findById()` má»—i request |

#### Quyáº¿t Ä‘á»‹nh (Decision)
Chá»n **PhÆ°Æ¡ng Ã¡n B**. `BabyDailyLogServiceImpl` inject `BabyProfileRepository` vÃ  `BabyAccessPolicy` (cáº£ hai Ä‘Ã£ tá»“n táº¡i tá»« UC192), load `BabyProfile` báº±ng `dailyLog.getBabyId()`, sau Ä‘Ã³ gá»i `accessPolicy.canView(profile, callerId)` y há»‡t UC192.

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:**
- Má»™t policy duy nháº¥t cho toÃ n bá»™ `baby` bounded context â€” sá»­a 1 nÆ¡i, Ã¡p dá»¥ng má»i UC.
- Giáº£m rá»§i ro IDOR do logic phÃ¢n máº£nh.

**TiÃªu cá»±c / Trade-offs:**
- ThÃªm 1 round-trip DB Ä‘á»ƒ load `BabyProfile` â€” cháº¥p nháº­n Ä‘Æ°á»£c vÃ¬ NFR p99 < 300ms.

**Compliance Impact:**
- Cá»§ng cá»‘ BR-RBAC báº±ng cÃ¡ch trÃ¡nh duplicate/divergent authorization logic (OWASP A01:2021 â€” Broken Access Control mitigation).

---

### ADR-BABY-005 â€” Read-Only, No New Domain Event cho View (nhÆ°ng cÃ³ Audit Log tuá»³ chá»n)

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Date** | `2026-07-03` |

#### Bá»‘i cáº£nh (Context)
UC192 (`getBabyProfile`) lÃ  read-only, KHÃ”NG emit audit event (Constraint C4 trong TDS UC192 Â§17.2: "Read-only endpoint â€” KHÃ”NG cÃ³ side effects"). Cáº§n quyáº¿t Ä‘á»‹nh UC194 cÃ³ nÃªn khÃ¡c Ä‘i, vÃ¬ Ä‘Ã¢y lÃ  dá»¯ liá»‡u sá»©c khoáº» tráº» sÆ¡ sinh (Sensitive-PII) â€” cÃ³ cáº§n audit trail cho viá»‡c "ai Ä‘Ã£ xem log nÃ o" khÃ´ng.

#### Quyáº¿t Ä‘á»‹nh (Decision)
Giá»¯ nháº¥t quÃ¡n vá»›i UC192: **KHÃ”NG báº¯t buá»™c audit event cho viá»‡c xem** (view baby daily log khÃ´ng side-effect, khÃ´ng thay Ä‘á»•i state). Domain event `BabyDailyLogViewed` Ä‘Æ°á»£c **thiáº¿t káº¿ nhÆ°ng KHÃ”NG kÃ­ch hoáº¡t máº·c Ä‘á»‹nh** trong láº§n triá»ƒn khai Ä‘áº§u â€” Ä‘Ã¡nh dáº¥u `Open` trong Domain Event Catalog (Â§7) Ä‘á»ƒ Tech Lead quyáº¿t Ä‘á»‹nh cÃ³ báº­t audit-on-read hay khÃ´ng (trade-off giá»¯a audit trail Ä‘áº§y Ä‘á»§ vÃ  write-amplification trÃªn báº£ng audit_logs cho má»™t hÃ nh Ä‘á»™ng Ä‘á»c táº§n suáº¥t cao).

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:** Nháº¥t quÃ¡n API pattern, khÃ´ng tÄƒng táº£i ghi DB cho thao tÃ¡c Ä‘á»c táº§n suáº¥t cao (Frequency of Use = Frequent theo SRS).

**TiÃªu cá»±c / Trade-offs:** Náº¿u sau nÃ y cáº§n audit "ai xem log nÃ o" cho compliance investigation, pháº£i bá»• sung sau â€” Ä‘Ã£ note `Open` item.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|------------------|
| Latency (p99) | GET response | `< 200ms` | k6 load test | â€” |
| Availability | Uptime | `99.9%` | Uptime monitor | â€” |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Consistency | `baby_id` FK luÃ´n resolve Ä‘Æ°á»£c `BabyProfile` | 100% | FK constraint `baby_daily_logs_baby_id_fkey` | â€” |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Access control | IDOR guard â€” ownership chain qua `baby_id` | 100% requests kiá»ƒm tra | `BabyAccessPolicy.canView()` reuse | BR-RBAC |
| Encryption in transit | TLS | TLS 1.3+ | SSL Labs scan | â€” |

### 4.4. Scalability & Capacity Planning

Dá»± kiáº¿n táº£i: má»—i Mother xem trung bÃ¬nh 5-20 daily logs/ngÃ y qua danh sÃ¡ch trÆ°á»›c khi má»Ÿ detail. Endpoint lÃ  single-row lookup theo PK (`baby_log_id`) â€” khÃ´ng cáº§n pagination hay caching riÃªng á»Ÿ giai Ä‘oáº¡n nÃ y.

---

## 5. Static Modeling (MÃ´ hÃ¬nh TÄ©nh)

### 5.1. Class Diagram (PlantUML)

```plantuml
@startuml ViewBabyDailyLogDetail_ClassDiagram
skinparam classAttributeIconSize 0
skinparam backgroundColor #FAFAFA

class BabyDailyLog {
  + id: UUID
  + babyId: UUID
  + logType: String
  + startedAt: Instant
  + endedAt: Instant
  + quantity: BigDecimal
  + unit: String
  + note: String
  + recordedBy: UUID
  + status: BabyDailyLogStatus
  + createdAt: Instant
  + updatedAt: Instant
}

enum BabyDailyLogStatus {
  ACTIVE
  DELETED
}

interface IBabyDailyLogService {
  + getDailyLogDetail(babyLogId: UUID, callerId: UUID): BabyDailyLogDetailResponse
}

class BabyDailyLogServiceImpl implements IBabyDailyLogService {
  - babyDailyLogRepository: BabyDailyLogRepository
  - babyProfileRepository: BabyProfileRepository
  - babyAccessPolicy: BabyAccessPolicy
  + getDailyLogDetail(babyLogId, callerId): BabyDailyLogDetailResponse
}

interface BabyDailyLogRepository {
  + findById(id: UUID): Optional<BabyDailyLog>
}

BabyDailyLogServiceImpl --> BabyDailyLogRepository : uses
BabyDailyLogServiceImpl --> "com.carebridge.backend.baby.repository.BabyProfileRepository" : reuse (UC192)
BabyDailyLogServiceImpl --> "com.carebridge.backend.baby.policy.BabyAccessPolicy" : reuse (UC192)
BabyDailyLog "many" --> "1" "com.carebridge.backend.baby.entity.BabyProfile" : baby_id FK

@enduml
```

### 5.2. Data Structure (Flyway SQL Migration)

> **CareBridge rule:** `V1__init_schema.sql` lÃ  baseline oracle. `baby_daily_logs` Ä‘Ã£ tá»“n táº¡i (xem trÃ­ch dáº«n dÆ°á»›i) nhÆ°ng KHÃ”NG cÃ³ `status` column â†’ UC195 cáº§n soft-delete nÃªn bá»• sung migration má»›i (xem UC195 TDS Â§5.2 cho migration `V20260707110000`). UC194 (view) KHÃ”NG cáº§n thay Ä‘á»•i schema â€” chá»‰ cáº§n Ä‘á»c, nhÆ°ng SERVICE cá»§a UC194 **pháº£i lá»c `status <> 'DELETED'`** sau khi migration UC195 cháº¡y, Ä‘á»ƒ Ä‘áº£m báº£o record Ä‘Ã£ soft-delete khÃ´ng hiá»ƒn thá»‹ láº¡i Ä‘Æ°á»£c (404) â€” coupling nÃ y Ä‘Æ°á»£c ghi nháº­n trong Â§3 ADR-BABY-004 companion.

**Existing schema (V1__init_schema.sql, dÃ²ng 621-633) â€” KHÃ”NG thay Ä‘á»•i bá»Ÿi UC194:**
```sql
CREATE TABLE public.baby_daily_logs (
    baby_log_id uuid        NOT NULL DEFAULT gen_random_uuid(),
    baby_id     uuid        NOT NULL,
    log_type    varchar(30) NOT NULL,
    started_at  timestamptz,
    ended_at    timestamptz,
    quantity    numeric,
    unit        varchar(20),
    note        text,
    recorded_by uuid,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
-- PK: baby_log_id
-- FK: baby_id -> baby_profiles(baby_id)
-- FK: recorded_by -> users(user_id)
-- INDEX: idx_baby_daily_logs_baby_id, idx_baby_daily_logs_started_at
```

> **Gap ghi nháº­n (RG-6):** `log_type` lÃ  `varchar(30)` KHÃ”NG cÃ³ DB `CHECK` constraint rÃ ng buá»™c enum â€” giá»‘ng style cá»§a `baby_profiles.status` (varchar app-level enum, khÃ´ng DB CHECK). Vocabulary chÃ­nh xÃ¡c (feeding/sleep/diaper/...) KHÃ”NG Ä‘Æ°á»£c Ä‘á»‹nh nghÄ©a á»Ÿ báº¥t ká»³ Ä‘Ã¢u trong SRS, migration, hay code hiá»‡n cÃ³ â†’ Ä‘Ã¡nh dáº¥u **Open Item** (xem Â§Open Items cuá»‘i tÃ i liá»‡u). Äá» xuáº¥t: entity dÃ¹ng `String logType` (KHÃ”NG `@Enumerated`) cho Ä‘áº¿n khi vocabulary Ä‘Æ°á»£c Product xÃ¡c nháº­n, trÃ¡nh hard-code enum sai.

> **UC194 KHÃ”NG táº¡o migration má»›i** â€” chá»‰ Ä‘á»c dá»¯ liá»‡u hiá»‡n cÃ³. Náº¿u UC195 Ä‘Æ°á»£c implement trÆ°á»›c/song song, cá»™t `status` sáº½ Ä‘Æ°á»£c thÃªm bá»Ÿi UC195's migration; UC194 service pháº£i cá»™ng thÃªm Ä‘iá»u kiá»‡n lá»c `status != DELETED` khi entity cÃ³ field Ä‘Ã³ (xem Interface Specification Â§8.1 ghi chÃº `@since UC195`).

---

## 6. Dynamic Modeling (MÃ´ hÃ¬nh Äá»™ng)

### 6.1. Sequence Diagram â€” Happy Path (PlantUML)

```plantuml
@startuml ViewBabyDailyLogDetail_HappyPath
skinparam backgroundColor #FAFAFA
actor "Mother" as Client
participant "BabyDailyLogController" as Controller
participant "BabyDailyLogServiceImpl" as Service
participant "BabyDailyLogRepository" as LogRepo
participant "BabyProfileRepository" as ProfileRepo
participant "BabyAccessPolicy" as Policy
database "PostgreSQL" as DB

Client -> Controller : GET /api/v1/babies/{babyId}/daily-logs/{logId}
activate Controller
Controller -> Controller : SecurityUtils.requireCurrentUserId(principal)
Controller -> Service : getDailyLogDetail(logId, callerId)
activate Service

Service -> LogRepo : findById(logId)
LogRepo -> DB : SELECT * FROM baby_daily_logs WHERE baby_log_id=?
DB --> LogRepo : BabyDailyLog row
LogRepo --> Service : Optional<BabyDailyLog>

Service -> ProfileRepo : findById(log.getBabyId())
ProfileRepo -> DB : SELECT * FROM baby_profiles WHERE baby_id=?
DB --> ProfileRepo : BabyProfile row
ProfileRepo --> Service : Optional<BabyProfile>

Service -> Policy : canView(profile, callerId)
Policy --> Service : true

Service -> Service : verify log.babyId == pathParam babyId (defense-in-depth)
Service --> Controller : BabyDailyLogDetailResponse
deactivate Service
Controller --> Client : 200 OK {content, timestamp, type}
deactivate Controller
@enduml
```

### 6.2. Sequence Diagram â€” Error Path (PlantUML)

```plantuml
@startuml ViewBabyDailyLogDetail_ErrorPath
skinparam backgroundColor #FAFAFA
actor "Unrelated User" as Client
participant "BabyDailyLogController" as Controller
participant "BabyDailyLogServiceImpl" as Service
participant "BabyDailyLogRepository" as LogRepo
participant "BabyProfileRepository" as ProfileRepo
participant "BabyAccessPolicy" as Policy

Client -> Controller : GET /api/v1/babies/{babyId}/daily-logs/{logId}
activate Controller
Controller -> Service : getDailyLogDetail(logId, callerId)
activate Service
Service -> LogRepo : findById(logId)
LogRepo --> Service : Optional<BabyDailyLog> (present)
Service -> ProfileRepo : findById(log.getBabyId())
ProfileRepo --> Service : Optional<BabyProfile> (present, owner = OTHER user)
Service -> Policy : canView(profile, callerId)
Policy --> Service : false
Service -> Service : throw BusinessException(403, "DAILYLOG-002")
deactivate Service
Controller --> Client : 403 Forbidden {code: DAILYLOG-002}
deactivate Controller

note over Service
  Alternative: logId not found -> BusinessException(404, "DAILYLOG-001")
  Alternative: log.status == DELETED (post-UC195) -> BusinessException(404, "DAILYLOG-001")
  (soft-deleted records behave as not-found for View, unlike ARCHIVED baby profiles
   which stay visible â€” see ADR-BABY-006 in UC195 TDS for the distinction)
end note
@enduml
```

---

## 7. Domain Event Catalog

### 7.1. Events Published (PhÃ¡t ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| `BabyDailyLogViewed` | (Open â€” NOT activated by default, xem ADR-BABY-005) | `BabyDailyLogServiceImpl` | `audit` (future) | `BabyDailyLogViewedEvent.java` | Yes (náº¿u báº­t) |

### 7.2. Events Consumed (TiÃªu thá»¥)

KhÃ´ng cÃ³ â€” module nÃ y khÃ´ng tiÃªu thá»¥ event nÃ o.

### 7.3. Payload Schema (dá»± phÃ²ng náº¿u ADR-BABY-005 Ä‘Æ°á»£c Ä‘áº£o ngÆ°á»£c)

```java
// BabyDailyLogViewedEvent.java â€” NOT wired by default (Open item)
public record BabyDailyLogViewedEvent(
    UUID    eventId,
    String  eventType,       // "BabyDailyLogViewed"
    Instant occurredAt,
    String  version,         // "1.0"
    Payload payload,
    Metadata metadata
) {
    public record Payload(
        UUID babyLogId,
        UUID babyId,
        UUID viewedByUserId
    ) {}

    public record Metadata(
        UUID   correlationId,
        String causedBy
    ) {}
}
```

---

## 8. Interface Specification (Äáº·c táº£ Giao diá»‡n)

### 8.1. Service Interface

```java
// BabyDailyLogDetailResponse.java â€” Output DTO
// @version 1.0
public class BabyDailyLogDetailResponse {
    private UUID id;
    private UUID babyId;
    private String logType;        // free-text/varchar; known values per UC34 ADR-BABY-007: FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE
    private Instant startedAt;
    private Instant endedAt;       // nullable
    private BigDecimal quantity;   // nullable
    private String unit;           // nullable
    private String note;           // maps to SRS "content"
    private UUID recordedBy;
    private Instant createdAt;
    private Instant updatedAt;
}

// IBabyDailyLogService.java â€” Service Contract
// @version 1.0
public interface IBabyDailyLogService {
    /**
     * @throws com.carebridge.backend.common.exception.BusinessException (DAILYLOG-001/404)
     *         khi babyLogId khÃ´ng tá»“n táº¡i, HOáº¶C record Ä‘Ã£ soft-deleted (status=DELETED, @since UC195)
     * @throws com.carebridge.backend.common.exception.BusinessException (DAILYLOG-002/403)
     *         khi caller khÃ´ng pháº£i owner/accepted care group member cá»§a baby liÃªn quan
     */
    BabyDailyLogDetailResponse getDailyLogDetail(UUID babyLogId, UUID callerId);
}
```

### 8.2. Entity & Repository Interface

```java
// BabyDailyLog.java â€” new entity, package com.carebridge.backend.baby.entity
// @version 1.0
@Entity
@Table(name = "baby_daily_logs")
public class BabyDailyLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "baby_log_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "baby_id", nullable = false)
    private UUID babyId;

    @Column(name = "log_type", nullable = false, length = 30)
    private String logType;   // NOT @Enumerated â€” read path stays permissive (see OI-1); write-side vocabulary defined by UC34 ADR-BABY-007

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "note")
    private String note;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    // @since UC195 migration V20260707110000 â€” nullable until that migration lands;
    // UC194 read path must null-check and treat legacy NULL as ACTIVE (backward compatible default).
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BabyDailyLogStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

// BabyDailyLogRepository.java
// @version 1.0
public interface BabyDailyLogRepository extends JpaRepository<BabyDailyLog, UUID> {
    // findById() inherited from JpaRepository is sufficient for UC194.
}
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `GET` | `/api/v1/babies/{babyId}/daily-logs/{logId}` | JWT Bearer | `ROLE_MOTHER` | 300/min | Yes |

> **Path design note:** URL nests under `/api/v1/babies/{babyId}/...` Ä‘á»ƒ nháº¥t quÃ¡n vá»›i `BabyController`'s `/api/v1/babies` base path (UC192 convention). `babyId` trong path Ä‘Æ°á»£c dÃ¹ng CHá»ˆ Ä‘á»ƒ Ä‘á»‹nh tuyáº¿n REST â€” service **KHÃ”NG Ä‘Æ°á»£c tin `babyId` tá»« path** cho authorization; ownership check luÃ´n dá»±a trÃªn `babyDailyLog.getBabyId()` Ä‘á»c tá»« DB (Constraint C2 Â§17).

### 9.2. Request / Response Schemas

#### `GET /api/v1/babies/{babyId}/daily-logs/{logId}`

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response â€” 200 OK:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "babyId": "660e8400-e29b-41d4-a716-446655440001",
    "logType": "feeding",
    "startedAt": "2026-07-03T08:00:00.000Z",
    "endedAt": "2026-07-03T08:20:00.000Z",
    "quantity": 120,
    "unit": "ml",
    "note": "BÃº bÃ¬nh Ä‘á»§ 120ml, khÃ´ng quáº¥y khÃ³c.",
    "recordedBy": "770e8400-e29b-41d4-a716-446655440002",
    "createdAt": "2026-07-03T08:21:00.000Z",
    "updatedAt": "2026-07-03T08:21:00.000Z"
  }
}
```

**Response â€” 403 Forbidden:**
```json
{
  "error": { "code": "DAILYLOG-002", "message": "Access denied to baby daily log" }
}
```

**Response â€” 404 Not Found:**
```json
{
  "error": { "code": "DAILYLOG-001", "message": "Baby daily log not found" }
}
```

---

## 10. Báº£ng mÃ£ lá»—i (Error Codes)

> Prefix `DAILYLOG-` dÃ¹ng riÃªng cho `baby_daily_logs` module Ä‘á»ƒ trÃ¡nh Ä‘á»¥ng vá»›i `BABY-xxx` (baby profile) Ä‘Ã£ cáº¥p phÃ¡t á»Ÿ UC192 (`BABY-001` 404, `BABY-003` 403 â€” xÃ¡c nháº­n tá»« code thá»±c táº¿ `BabyServiceImpl.java`). **(Cáº­p nháº­t 2026-07-03):** TDS UC192 Â§9-10 tá»«ng ghi nháº§m `BABY-002/BABY-004`; Ä‘Ã£ Ä‘Æ°á»£c sá»­a láº¡i khá»›p code tháº­t. Xem OI-3 (Ä‘Ã£ Ä‘Ã³ng) vÃ  Test-Spec Logic Issue L1.

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `DAILYLOG-001` | 404 | Baby daily log not found | KhÃ´ng tÃ¬m tháº¥y nháº­t kÃ½ háº±ng ngÃ y | `babyLogId` khÃ´ng tá»“n táº¡i HOáº¶C record cÃ³ `status=DELETED` (post-UC195) HOáº¶C `babyId` FK khÃ´ng resolve Ä‘Æ°á»£c `BabyProfile` (orphan â€” treat as 404, defense-in-depth) |
| `DAILYLOG-002` | 403 | Access denied to baby daily log | KhÃ´ng Ä‘á»§ quyá»n truy cáº­p nháº­t kÃ½ | Caller khÃ´ng pháº£i owner vÃ  khÃ´ng pháº£i ACCEPTED care group member cá»§a baby liÃªn quan |
| `DAILYLOG-005` | 500 | Internal error | Lá»—i há»‡ thá»‘ng | Unexpected DB error |

---

## 11. Quy trÃ¬nh Triá»ƒn khai (Step-by-Step)

### 11.1. Prerequisites
- [ ] TDS nÃ y Ä‘Æ°á»£c Approved
- [ ] UC195 migration (náº¿u triá»ƒn khai song song) Ä‘Ã£ review Ä‘á»ƒ trÃ¡nh xung Ä‘á»™t cá»™t `status`
- [ ] `BabyAccessPolicy`, `BabyProfileRepository` (UC192) Ä‘Ã£ cÃ³ sáºµn trong `main` â€” xÃ¡c nháº­n (Ä‘Ã£ cÃ³)

### 11.2. Pre-Migration Checklist
- KhÃ´ng Ã¡p dá»¥ng â€” UC194 khÃ´ng cÃ³ migration riÃªng (Ä‘á»c dá»¯ liá»‡u hiá»‡n cÃ³ + optional `status` column tá»« UC195).

### 11.3. Implementation Steps

#### Cháº·ng 1 â€” Entity + Repository
Táº¡o `BabyDailyLog.java`, `BabyDailyLogStatus.java` (enum ACTIVE/DELETED, dÃ¹ng cho tÆ°Æ¡ng thÃ­ch UC195), `BabyDailyLogRepository.java` trong `com.carebridge.backend.baby.{entity,repository}`.

#### Cháº·ng 2 â€” Service + DTO
Táº¡o `IBabyDailyLogService.java`, `BabyDailyLogServiceImpl.java`, `BabyDailyLogDetailResponse.java` trong `com.carebridge.backend.baby.{service, service.impl, dto}`. Inject `BabyDailyLogRepository`, `BabyProfileRepository`, `BabyAccessPolicy` (2 cÃ¡i sau tÃ¡i sá»­ dá»¥ng nguyÃªn váº¹n tá»« UC192 â€” KHÃ”NG táº¡o bean má»›i).

#### Cháº·ng 3 â€” Controller
ThÃªm `BabyDailyLogController.java` (`@RestController`, base path `/api/v1/babies/{babyId}/daily-logs`), method `getDailyLogDetail`.

#### Cháº·ng 4 â€” Verification sau deploy
```bash
curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 200 with content/timestamp/type
```

### 11.4. Deployment Checklist
- [ ] `./mvnw test` xanh
- [ ] Response khÃ´ng chá»©a `diagnosis`/`interpretation`/`condition` field (BR-SAFETY)
- [ ] IDOR test (non-owner â†’ 403) pass

---

## 12. Rollback & Incident Runbook

### 12.1. Äiá»u kiá»‡n kÃ­ch hoáº¡t Rollback

| Äiá»u kiá»‡n | NgÆ°á»¡ng | NgÆ°á»i quyáº¿t Ä‘á»‹nh |
|-----------|--------|------------------|
| Error rate tÄƒng Ä‘á»™t biáº¿n | > 5% trong 5 phÃºt | On-call Engineer |
| IDOR phÃ¡t hiá»‡n qua pentest/report | Báº¥t ká»³ case nÃ o | Tech Lead + DPO |

### 12.2. Rollback Procedure

```bash
# KhÃ´ng cÃ³ migration má»›i cho UC194 â€” rollback chá»‰ cáº§n revert code deploy
kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api
curl -X GET https://[host]/api/v1/health
```

### 12.3. Notification Protocol

| Thá»i Ä‘iá»ƒm | NgÆ°á»i nháº­n | KÃªnh |
|-----------|------------|------|
| Ngay khi phÃ¡t hiá»‡n IDOR | On-call + DPO | Slack `#incident` + Email |

---

## 13. Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t

> **Policy (EDS v2.0):** Má»i test scenario dÃ¹ng dá»¯ liá»‡u `SYNTHETIC`.

```gherkin
Feature: View Baby Daily Log Detail
  Background:
    Given test data classification: SYNTHETIC
    And MOTHER-001 lÃ  owner cá»§a BABY-001
    And LOG-001 thuá»™c BABY-001 vá»›i logType=feeding, note="BÃº bÃ¬nh 120ml"

  Scenario: Owner xem chi tiáº¿t log â†’ 200
    When getDailyLogDetail(LOG-001, MOTHER-001)
    Then response 200 vá»›i content, timestamp, type Ä‘áº§y Ä‘á»§

  Scenario: Care group member (ACCEPTED) xem log â†’ 200
    Given MOTHER-002 lÃ  ACCEPTED member trong care group cá»§a BABY-001
    When getDailyLogDetail(LOG-001, MOTHER-002)
    Then response 200

  Scenario: Non-owner, non-member â†’ 403
    Given MOTHER-003 KHÃ”NG liÃªn quan BABY-001
    When getDailyLogDetail(LOG-001, MOTHER-003)
    Then throws BusinessException DAILYLOG-002 (403)

  Scenario: Log khÃ´ng tá»“n táº¡i â†’ 404
    When getDailyLogDetail(NONEXISTENT, MOTHER-001)
    Then throws BusinessException DAILYLOG-001 (404)

  Scenario: Log Ä‘Ã£ soft-deleted (post-UC195) â†’ 404
    Given LOG-002 thuá»™c BABY-001 vá»›i status=DELETED
    When getDailyLogDetail(LOG-002, MOTHER-001)
    Then throws BusinessException DAILYLOG-001 (404)

  Scenario: Response khÃ´ng chá»©a diagnosis/medical interpretation
    When getDailyLogDetail(LOG-001, MOTHER-001)
    Then response KHÃ”NG chá»©a "diagnosis", "interpretation", "condition"
```

---

## 14. PhÆ°Æ¡ng phÃ¡p XÃ¡c minh

### 14.1. Database Inspection

```sql
-- Verify log exists and belongs to expected baby
SELECT baby_log_id, baby_id, log_type, started_at, note
FROM baby_daily_logs WHERE baby_log_id = '[logId]';

-- Verify ownership chain
SELECT bp.owner_user_id
FROM baby_daily_logs bdl
JOIN baby_profiles bp ON bp.baby_id = bdl.baby_id
WHERE bdl.baby_log_id = '[logId]';
```

### 14.2. Access Policy Verification

```bash
curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [OWNER_JWT]"
# Expected: 200

curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [UNRELATED_USER_JWT]"
# Expected: 403
```

---

## 15. Máº«u thá»­ thá»±c táº¿ (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 200 {id, babyId, logType, startedAt, note, ...}
```

### 15.2. Error Paths

```bash
# Non-existent log -> 404
curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/non-existent-uuid \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"

# Unrelated user -> 403
curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [OTHER_USER_JWT]"

# No JWT -> 401
curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/[logId]
```

---

## 16. Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix)

| Endpoint | `GUEST` | `MOTHER (owner)` | `MOTHER (care member, ACCEPTED)` | `EXPERT` | `ADMIN` |
|----------|---------|-------------------|-----------------------------------|----------|---------|
| `GET /api/v1/babies/{babyId}/daily-logs/{logId}` | âŒ (401) | âœ… | âœ… | âŒ (403) | âœ… All |

**ChÃº thÃ­ch:**
- Owner: `baby_profiles.owner_user_id` == JWT subject (via `baby_daily_logs.baby_id` FK)
- Care member: `care_group_members.invite_status = ACCEPTED` cho group cá»§a owner (reuse `BabyAccessPolicy`)
- Expert: khÃ´ng cÃ³ quyá»n xem trá»±c tiáº¿p, chá»‰ qua consultation sharing (ngoÃ i pháº¡m vi UC194)

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source | Last Verified |
|---|-----------|--------|---------------|
| C1 | `BabyDailyLogServiceImpl` PHáº¢I load `BabyProfile` qua `dailyLog.getBabyId()` rá»“i gá»i `BabyAccessPolicy.canView()` Ä‘Ã£ cÃ³ tá»« UC192 â€” KHÃ”NG viáº¿t logic ownership má»›i | ADR-BABY-004 | 2026-07-03 |
| C2 | `babyId` trong URL path CHá»ˆ dÃ¹ng Ä‘á»ƒ routing â€” authorization luÃ´n dá»±a trÃªn `babyDailyLog.getBabyId()` Ä‘á»c tá»« DB, KHÃ”NG tin path param | ADR-BABY-004, BR-RBAC | 2026-07-03 |
| C3 | Náº¿u `status=DELETED` (post-UC195), tráº£ 404 (`DAILYLOG-001`) â€” KHÃ”NG tráº£ 403 hay lá»™ thÃ´ng tin Ä‘Ã£ xoÃ¡ | ADR trong UC195 TDS Â§3 | 2026-07-03 |
| C4 | Read-only endpoint â€” KHÃ”NG audit event máº·c Ä‘á»‹nh (nháº¥t quÃ¡n UC192); `BabyDailyLogViewed` lÃ  Open item, chÆ°a kÃ­ch hoáº¡t | ADR-BABY-005 | 2026-07-03 |
| C5 | Response DTO KHÃ”NG chá»©a trÆ°á»ng `diagnosis`/`interpretation`/`condition` â€” chá»‰ content/timestamp/type theo SRS 3.3.12.3 | BR-SAFETY | 2026-07-03 |

### 17.2 Constraint Injection Block

```
[CONSTRAINT BLOCK â€” Module: ViewBabyDailyLogDetail (CB-BABY-IMP-003)]
1. BabyDailyLogServiceImpl PHáº¢I: load BabyProfile qua dailyLog.getBabyId(), gá»i BabyAccessPolicy.canView(profile, callerId) â€” TÃI Sá»¬ Dá»¤NG class cÃ³ sáºµn tá»« UC192, KHÃ”NG viáº¿t policy má»›i â€” ADR-BABY-004
2. babyId trong URL path KHÃ”NG Ä‘Æ°á»£c dÃ¹ng Ä‘á»ƒ authorization â€” chá»‰ dÃ¹ng Ä‘á»ƒ route; ownership check luÃ´n dá»±a trÃªn dá»¯ liá»‡u Ä‘á»c tá»« DB â€” BR-RBAC
3. status=DELETED (náº¿u cÃ³, post-UC195 migration) PHáº¢I tráº£ 404 DAILYLOG-001, KHÃ”NG lá»™ log Ä‘Ã£ xoÃ¡ dÆ°á»›i báº¥t ká»³ hÃ¬nh thá»©c nÃ o
4. Read-only â€” KHÃ”NG side effect DB write, KHÃ”NG audit event máº·c Ä‘á»‹nh â€” nháº¥t quÃ¡n UC192 pattern
5. Response DTO KHÃ”NG chá»©a diagnosis/interpretation/condition â€” chá»‰ id, babyId, logType, startedAt, endedAt, quantity, unit, note, recordedBy, timestamps â€” BR-SAFETY

[CONTEXT BLOCK]
- Bounded Context: baby (reuse UC192 package â€” com.carebridge.backend.baby)
- Data Classification: Sensitive-PII
- Error codes: Â§10 Error Codes Table (prefix DAILYLOG-, KHÃ”NG trÃ¹ng BABY-xxx)
- Auth matrix: Â§16 Authorization Matrix
- Reused classes: BabyProfileRepository, BabyAccessPolicy (tá»« UC192 â€” KHÃ”NG táº¡o báº£n sao)
```

### 17.3 Constraint Quality Checklist

- [x] Má»—i constraint traceable vá» ADR hoáº·c BR cá»¥ thá»ƒ
- [x] KhÃ´ng cÃ³ constraint generic
- [x] Constraint block cÃ³ â‰¥ 3 constraints cá»¥ thá»ƒ

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | HÃ nh Ä‘á»™ng |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code khÃ´ng match constraint C1-C5 | Reject â€” inject láº¡i constraints |
| AP-AI-003 | Implicit Decision | Code viáº¿t `BabyDailyLogAccessPolicy` má»›i thay vÃ¬ tÃ¡i sá»­ dá»¥ng `BabyAccessPolicy` | Reject â€” vi pháº¡m ADR-BABY-004 |
| AP-AI-005 | Hallucinated Contract | Code import class khÃ´ng cÃ³ trong Â§8 | Reject â€” verify contract |

---

## PHá»¤ Lá»¤C

### A. Glossary

| Thuáº­t ngá»¯ | Äá»‹nh nghÄ©a |
|-----------|------------|
| BabyDailyLog | Báº£n ghi nháº­t kÃ½ sinh hoáº¡t háº±ng ngÃ y cá»§a baby (feeding/sleep/diaper/...) |
| Ownership Chain | Chuá»—i resolve quyá»n sá»Ÿ há»¯u: `baby_daily_logs.baby_id â†’ baby_profiles.owner_user_id` |
| IDOR | Insecure Direct Object Reference â€” truy cáº­p trÃ¡i phÃ©p báº±ng cÃ¡ch Ä‘oÃ¡n/thay Ä‘á»•i ID |

### B. TÃ i liá»‡u tham chiáº¿u

| Document | Path |
|----------|------|
| UC192 TDS (Approved, shipped code reference) | `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` |
| UC195 TDS (companion â€” soft-delete migration) | `04_Implement/UC195_DeleteBabyDailyLog/UC195_DeleteBabyDailyLog_TDS.md` |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |
| Schema baseline | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` |

---

## Open Items (chÆ°a resolve â€” cáº§n Tech Lead / Product xÃ¡c nháº­n trÆ°á»›c khi Approve)

| # | Item | MÃ´ táº£ | Äá» xuáº¥t táº¡m thá»i |
|---|------|-------|-------------------|
| OI-1 | ~~`log_type` enum vocabulary~~ **RESOLVED (2026-07-03)** | Ban Ä‘áº§u tÆ°á»Ÿng khÃ´ng cÃ³ tÃ i liá»‡u nÃ o Ä‘á»‹nh nghÄ©a `log_type`. RÃ  soÃ¡t láº¡i phÃ¡t hiá»‡n sibling spec `UC34_AddFeedingSleepDiaperLog` (ADR-BABY-007) Ä‘Ã£ Ä‘á»‹nh nghÄ©a vocabulary cho Ä‘Ãºng cá»™t `baby_daily_logs.log_type` nÃ y: `FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE` (validated qua `BABY-033` á»Ÿ write path). Cá»™t váº«n lÃ  `varchar(30)` khÃ´ng CHECK constraint á»Ÿ DB. | UC194 lÃ  read-only nÃªn váº«n giá»¯ `String` (khÃ´ng `@Enumerated`) á»Ÿ entity â€” khÃ´ng reject giÃ¡ trá»‹ láº¡ khi Ä‘á»c, Ä‘á»ƒ khÃ´ng vá»¡ náº¿u cÃ³ dá»¯ liá»‡u cÅ©/há»£p lá»‡ khÃ¡c náº±m ngoÃ i whitelist. Whitelist enforcement thuá»™c trÃ¡ch nhiá»‡m write path (UC34), khÃ´ng pháº£i UC194. |
| OI-2 | `BabyDailyLogViewed` audit event | ADR-BABY-005 Ä‘á»ƒ ngá» viá»‡c cÃ³ nÃªn audit-on-read cho dá»¯ liá»‡u sá»©c khoáº» tráº» sÆ¡ sinh hay khÃ´ng. | KhÃ´ng kÃ­ch hoáº¡t máº·c Ä‘á»‹nh; revisit náº¿u compliance yÃªu cáº§u. |
| OI-3 | ~~Mismatch mÃ£ lá»—i UC192 tÃ i liá»‡u vs code~~ **RESOLVED (2026-07-03)** | TDS UC192 Â§9-10 tá»«ng ghi `BABY-002/BABY-004`, code thá»±c táº¿ dÃ¹ng `BABY-003/BABY-001`. UC194 dÃ¹ng prefix `DAILYLOG-` riÃªng nÃªn khÃ´ng bá»‹ áº£nh hÆ°á»Ÿng trá»±c tiáº¿p. TDS UC192 Ä‘Ã£ Ä‘Æ°á»£c sá»­a láº¡i khá»›p code tháº­t (`BABY-001`=404, `BABY-003`=403) trong toÃ n bá»™ báº£ng mÃ£ lá»—i, JSON examples vÃ  Gherkin scenarios. | ÄÃ£ Ä‘Ã³ng â€” khÃ´ng cáº§n hÃ nh Ä‘á»™ng thÃªm. |

---

*EDS v2.1 â€” TÃ­ch há»£p CASE 2.0 AI Prompt Constraints (Â§17).*
