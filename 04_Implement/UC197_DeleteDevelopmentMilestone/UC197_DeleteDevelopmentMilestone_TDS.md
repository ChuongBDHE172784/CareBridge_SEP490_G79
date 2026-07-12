# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification â€” UC-197 Delete Development Milestone

| Field | Value |
|-------|-------|
| **Document ID** | `CB-BABY-IMP-005` |
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
| 2026-07-03 | AI Agent | Táº¡o tÃ i liá»‡u láº§n Ä‘áº§u cho UC-197 Delete Development Milestone |

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
| **Module Name** | `DeleteDevelopmentMilestone` |
| **Bounded Context** | `baby` (reuse â€” cÃ¹ng bounded context vá»›i UC192/UC194/UC196; dÃ¹ng chung entity `DevelopmentMilestone` vá»›i UC196) |
| **UC ID** | `UC-197` |
| **SRS Reference** | `3.3.12.6` (`02_Requirements/SRS/3_Functional_Specification.md` lines 4238-4257) |
| **Primary Actor** | `Mother (ROLE_MOTHER)` |
| **Platform** | `Mobile App` |
| **Priority** | `Medium` |
| **Sprint** | `Sprint 4 â€” Device Sync And Care Edge Cases` |
| **Owner** | `TV2-BÃ¡ch` |
| **Data Classification** | `Sensitive-PII` (infant developmental/health data) |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy â€” UC192)`, `auth`, `development_milestones` table, `DevelopmentMilestone` entity (UC196, shared) |
| **Downstream Consumers** | `Development Milestone Timeline (future UC)` |

**MÃ´ táº£:** Cho phÃ©p Mother **soft-delete** Má»˜T development milestone (`development_milestones`) do chÃ­nh mÃ¬nh ghi nháº­n. Sau khi xoÃ¡ má»m, record váº«n tá»“n táº¡i trong DB (khÃ´ng hard-delete â€” phá»¥c vá»¥ audit/retention) nhÆ°ng bá»‹ áº©n khá»i má»i read/update path (coi nhÆ° 404). Ownership resolved qua chain `development_milestones.baby_id â†’ baby_profiles.owner_user_id`, dÃ¹ng `BabyAccessPolicy.canManage()` (method má»›i, bá»• sung á»Ÿ UC196 â€” xem ADR-BABY-007). ÄÃ¢y lÃ  greenfield code, dÃ¹ng chung entity/repository/migration vá»›i UC196 (companion feature) â€” **KHÃ”NG** táº¡o migration hay entity trÃ¹ng láº·p.

> â­ **Companion document:** TÃ i liá»‡u nÃ y PHá»¤ THUá»˜C vÃ o UC196 TDS (`04_Implement/UC196_UpdateDevelopmentMilestone/UC196_UpdateDevelopmentMilestone_TDS.md`) cho: migration `V20260707120000`, entity `DevelopmentMilestone`, enums `MilestoneAchievementStatus`/`MilestoneRecordStatus`, vÃ  `BabyAccessPolicy.canManage()`. ADR-BABY-006 (disambiguation) vÃ  ADR-BABY-007 (strict ownership) Ä‘Æ°á»£c Ä‘á»‹nh nghÄ©a Äáº¦Y Äá»¦ trong UC196 TDS Â§3 â€” tÃ i liá»‡u nÃ y chá»‰ tÃ³m táº¯t vÃ  bá»• sung ADR riÃªng cho hÃ nh vi xoÃ¡.

---

## 2. Ma tráº­n Truy váº¿t (Traceability Matrix)

| Requirement ID | Loáº¡i | MÃ´ táº£ | ThÃ nh pháº§n Code | Compliance Target | ADR liÃªn quan |
|----------------|------|-------|-----------------|-------------------|---------------|
| UC-197 | Use Case | Mother soft-delete 1 development milestone | `DevelopmentMilestoneController.deleteMilestone()` | BR-RBAC | ADR-BABY-008 |
| BR-RBAC | Business Rule | Chá»‰ owner (strict â€” khÃ´ng care group member) má»›i xoÃ¡ Ä‘Æ°á»£c | `DevelopmentMilestoneServiceImpl.deleteMilestone()` + `BabyAccessPolicy.canManage()` (reuse tá»« UC196) | BR-RBAC | ADR-BABY-007 (UC196) |
| BR-PRIVACY | Business Rule | XoÃ¡ má»m â€” KHÃ”NG hard-delete â€” phá»¥c vá»¥ audit/retention | `recordStatus = DELETED`, row váº«n tá»“n táº¡i trong DB | BR-PRIVACY | ADR-BABY-008 |
| â€” | Design Decision | `deleteMilestone()` CHá»ˆ Ä‘Æ°á»£c ghi `recordStatus`, TUYá»†T Äá»I KHÃ”NG Ä‘á»¥ng `milestoneStatus` | `DevelopmentMilestoneServiceImpl.deleteMilestone()` | Data Integrity | ADR-BABY-006 (UC196) |

---

## 3. Architecture Decision Records (ADR)

### ADR-BABY-006 (tham chiáº¿u tá»« UC196) â€” Achievement-Status vs Soft-Delete-Status Disambiguation

> **Xem Ä‘áº§y Ä‘á»§ táº¡i:** UC196 TDS Â§3, ADR-BABY-006.

**TÃ³m táº¯t Ã¡p dá»¥ng cho UC197:** `deleteMilestone()` CHá»ˆ Ä‘Æ°á»£c phÃ©p set `recordStatus = DELETED` trÃªn entity `DevelopmentMilestone` â€” **TUYá»†T Äá»I KHÃ”NG** Ä‘Æ°á»£c Ä‘á»c/ghi field `milestoneStatus` dÆ°á»›i báº¥t ká»³ hÃ¬nh thá»©c nÃ o (ká»ƒ cáº£ Ä‘á»ƒ "dá»n dáº¹p" hay "reset" giÃ¡ trá»‹). Sau khi xoÃ¡ má»m, `milestoneStatus` giá»¯ nguyÃªn giÃ¡ trá»‹ cuá»‘i cÃ¹ng trÆ°á»›c khi xoÃ¡ â€” Ä‘Ã¢y lÃ  dá»¯ liá»‡u lá»‹ch sá»­ cáº§n báº£o toÃ n cho má»¥c Ä‘Ã­ch audit (Mother cÃ³ thá»ƒ yÃªu cáº§u khÃ´i phá»¥c dá»¯ liá»‡u theo policy retention, dÃ¹ chá»©c nÄƒng "khÃ´i phá»¥c" chÆ°a náº±m trong pháº¡m vi UC197 â€” xem Open Items).

### ADR-BABY-007 (tham chiáº¿u tá»« UC196) â€” Strict Ownership cho Mutation

> **Xem Ä‘áº§y Ä‘á»§ táº¡i:** UC196 TDS Â§3, ADR-BABY-007.

**TÃ³m táº¯t Ã¡p dá»¥ng cho UC197:** `deleteMilestone()` dÃ¹ng `babyAccessPolicy.canManage(profile, callerId)` (strict ownership â€” method má»›i bá»• sung á»Ÿ UC196) â€” **KHÃ”NG** dÃ¹ng `canView()`. Care group member (ká»ƒ cáº£ ACCEPTED) khÃ´ng Ä‘Æ°á»£c xoÃ¡ milestone cá»§a Mother khÃ¡c.

---

### ADR-BABY-008 â€” Soft-Delete Idempotency & Row Retention Guard (UC197-specific)

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `TV2-BÃ¡ch, AI Agent` |
| **Date** | `2026-07-03` |
| **Supersedes** | â€” |

#### Bá»‘i cáº£nh (Context)

Hai cÃ¢u há»i thiáº¿t káº¿ riÃªng cho hÃ nh vi xoÃ¡ cáº§n quyáº¿t Ä‘á»‹nh: (1) XoÃ¡ má»™t milestone **Ä‘Ã£ bá»‹ xoÃ¡ má»m tá»« trÆ°á»›c** nÃªn tráº£ vá» gÃ¬ â€” 404 (record coi nhÆ° khÃ´ng tá»“n táº¡i) hay 409 Conflict (Ä‘Ã£ á»Ÿ tráº¡ng thÃ¡i Ä‘Ã­ch) hay 204 No Content (idempotent no-op)? (2) Endpoint xoÃ¡ cÃ³ nÃªn **hard-delete** row khá»i DB khÃ´ng, hay luÃ´n giá»¯ láº¡i (soft-delete only, nháº¥t quÃ¡n append-only pattern cá»§a cÃ¡c module PII khÃ¡c trong CareBridge)?

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t (Options Considered)

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | Hard-delete row khá»i DB (`DELETE FROM development_milestones WHERE ...`) | ÄÆ¡n giáº£n, giáº£i phÃ³ng storage | Máº¥t vÄ©nh viá»…n lá»‹ch sá»­ phÃ¡t triá»ƒn cá»§a tráº» â€” vi pháº¡m BR-PRIVACY retention, khÃ´ng thá»ƒ audit, khÃ´ng nháº¥t quÃ¡n vá»›i pattern soft-delete `ACTIVE/DELETED` Ä‘Ã£ dÃ¹ng cho `baby_daily_logs` (UC194/195) vÃ  `maternal_health_metrics` |
| B | Soft-delete (set `record_status = DELETED`), row váº«n tá»“n táº¡i | Nháº¥t quÃ¡n vá»›i pattern Ä‘Ã£ cÃ³ trong CareBridge; audit-friendly; há»— trá»£ khÃ´i phá»¥c trong tÆ°Æ¡ng lai náº¿u cáº§n | Row "rÃ¡c" tÃ­ch luá»¹ theo thá»i gian â€” cháº¥p nháº­n Ä‘Æ°á»£c vÃ¬ má»—i baby chá»‰ cÃ³ vÃ i chá»¥c milestone |
| Cho double-delete: A2 | Tráº£ `409 Conflict` náº¿u milestone Ä‘Ã£ DELETED | Ngá»¯ nghÄ©a REST chuáº©n (resource á»Ÿ tráº¡ng thÃ¡i khÃ´ng há»£p lá»‡ cho action) | KhÃ´ng nháº¥t quÃ¡n vá»›i pattern UC194 companion (`status=DELETED â†’ 404`, khÃ´ng pháº£i 409) â€” gÃ¢y khÃ³ Ä‘oÃ¡n cho client |
| Cho double-delete: B2 | Tráº£ `404 Not Found` náº¿u milestone Ä‘Ã£ DELETED (coi nhÆ° khÃ´ng tá»“n táº¡i) | Nháº¥t quÃ¡n 100% vá»›i UC194's documented pattern ("soft-deleted records behave as not-found for View... unlike ARCHIVED baby profiles") â€” Ã¡p dá»¥ng tÆ°Æ¡ng tá»± cho Delete: xoÃ¡ cÃ¡i "khÃ´ng tá»“n táº¡i" (theo gÃ³c nhÃ¬n API) â†’ 404 | Client cÃ³ thá»ƒ maská» nháº§m "chÆ°a tá»«ng tá»“n táº¡i" vá»›i "Ä‘Ã£ xoÃ¡" â€” cháº¥p nháº­n Ä‘Æ°á»£c vÃ¬ Ä‘Ã¢y Ä‘Ãºng lÃ  Ã½ Ä‘á»‹nh báº£o máº­t (khÃ´ng lá»™ tráº¡ng thÃ¡i Ä‘Ã£ xoÃ¡ cho caller khÃ´ng pháº£i chá»§ sá»Ÿ há»¯u) |

#### Quyáº¿t Ä‘á»‹nh (Decision)

Chá»n **PhÆ°Æ¡ng Ã¡n B** (soft-delete, khÃ´ng hard-delete) káº¿t há»£p **B2** (double-delete â†’ 404, khÃ´ng 409). LÃ½ do: nháº¥t quÃ¡n tuyá»‡t Ä‘á»‘i vá»›i pattern Ä‘Ã£ thiáº¿t láº­p á»Ÿ UC194/UC195 companion cho `baby_daily_logs` â€” "soft-deleted records behave as not-found," trÃ¡nh viá»‡c há»‡ thá»‘ng cÃ³ 2 ngá»¯ nghÄ©a khÃ¡c nhau cho cÃ¹ng má»™t khÃ¡i niá»‡m "record Ä‘Ã£ xoÃ¡" giá»¯a 2 module trong cÃ¹ng bounded context `baby`.

```java
// DevelopmentMilestoneServiceImpl.deleteMilestone() â€” pseudocode quyáº¿t Ä‘á»‹nh
DevelopmentMilestone milestone = milestoneRepository.findById(milestoneId)
        .filter(m -> m.getRecordStatus() == MilestoneRecordStatus.ACTIVE) // Ä‘Ã£ DELETED -> coi nhÆ° khÃ´ng tÃ¬m tháº¥y
        .orElseThrow(() -> new BusinessException(404, "MILESTONE-001", "..."));

BabyProfile profile = babyProfileRepository.findById(milestone.getBabyId())
        .orElseThrow(() -> new BusinessException(404, "MILESTONE-001", "..."));

if (!babyAccessPolicy.canManage(profile, callerId)) {
    throw new BusinessException(403, "MILESTONE-002", "...");
}

milestone.setRecordStatus(MilestoneRecordStatus.DELETED); // CHá»ˆ field nÃ y
// milestone.setMilestoneStatus(...) -- TUYá»†T Äá»I KHÃ”NG Ä‘Æ°á»£c gá»i á»Ÿ Ä‘Ã¢y
milestoneRepository.save(milestone);
```

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:**
- Nháº¥t quÃ¡n tuyá»‡t Ä‘á»‘i giá»¯a `baby_daily_logs` (UC194/195) vÃ  `development_milestones` (UC196/197) cho hÃ nh vi "Ä‘Ã£ xoÃ¡ má»m â†’ 404".
- KhÃ´ng máº¥t dá»¯ liá»‡u â€” há»— trá»£ audit/investigation, tuÃ¢n thá»§ BR-PRIVACY retention.
- Idempotent vá» máº·t hiá»‡u á»©ng cuá»‘i (double-delete khÃ´ng gÃ¢y lá»—i 500, chá»‰ 404 â€” an toÃ n cho client retry).

**TiÃªu cá»±c / Trade-offs:**
- KhÃ´ng cÃ³ endpoint "khÃ´i phá»¥c" (`un-delete`) trong pháº¡m vi UC197 â€” náº¿u Mother xoÃ¡ nháº§m, cáº§n liÃªn há»‡ support (ngoÃ i pháº¡m vi UC hiá»‡n táº¡i â€” ghi vÃ o Open Items).
- Table `development_milestones` sáº½ tÃ­ch luá»¹ record `DELETED` theo thá»i gian â€” cháº¥p nháº­n Ä‘Æ°á»£c á»Ÿ quy mÃ´ dá»¯ liá»‡u hiá»‡n táº¡i (vÃ i chá»¥c record/baby).

**Compliance Impact:**
- Cá»§ng cá»‘ BR-PRIVACY: dá»¯ liá»‡u sá»©c khoáº» tráº» em khÃ´ng bá»‹ xoÃ¡ vÄ©nh viá»…n ngoÃ i Ã½ muá»‘n â€” há»— trá»£ nghÄ©a vá»¥ lÆ°u trá»¯ tá»‘i thiá»ƒu theo chÃ­nh sÃ¡ch CareBridge.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|------------------|
| Latency (p99) | DELETE response | `< 250ms` | k6 load test | â€” |
| Availability | Uptime | `99.9%` | Uptime monitor | â€” |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Retention | Soft-delete only â€” row váº«n tá»“n táº¡i sau xoÃ¡ | 100% â€” khÃ´ng hard-delete nÃ o Ä‘Æ°á»£c thá»±c thi | DB row count trÆ°á»›c/sau khÃ´ng Ä‘á»•i | ADR-BABY-008, BR-PRIVACY |
| Consistency | `deleteMilestone()` KHÃ”NG Ä‘á»¥ng `milestoneStatus` | 100% | Unit test disambiguation (Â§13) | ADR-BABY-006 |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Access control | Strict ownership guard (khÃ´ng care group) | 100% requests kiá»ƒm tra qua `canManage()` | Unit + security test | BR-RBAC, ADR-BABY-007 |
| Encryption in transit | TLS | TLS 1.3+ | SSL Labs scan | â€” |

### 4.4. Scalability & Capacity Planning

Táº£i tháº¥p, "Occasional" theo SRS Frequency of Use. Endpoint single-row soft-delete theo PK â€” khÃ´ng cáº§n batch delete hay caching riÃªng.

---

## 5. Static Modeling (MÃ´ hÃ¬nh TÄ©nh)

### 5.1. Class Diagram (PlantUML)

```plantuml
@startuml DeleteDevelopmentMilestone_ClassDiagram
skinparam classAttributeIconSize 0
skinparam backgroundColor #FAFAFA

class DevelopmentMilestone {
  + id: UUID
  + babyId: UUID
  + milestoneType: String
  + achievedDate: LocalDate
  + note: String
  + milestoneStatus: MilestoneAchievementStatus
  + recordStatus: MilestoneRecordStatus
  + createdAt: Instant
  + updatedAt: Instant
}
note right of DevelopmentMilestone
  Entity SHARED vá»›i UC196 â€” Ä‘á»‹nh nghÄ©a
  Ä‘áº§y Ä‘á»§ táº¡i UC196 TDS Â§8.2. KHÃ”NG táº¡o
  báº£n sao/duplicate class.
end note

interface IDevelopmentMilestoneService {
  + deleteMilestone(milestoneId: UUID, callerId: UUID): void
}

class DevelopmentMilestoneServiceImpl implements IDevelopmentMilestoneService {
  - milestoneRepository: DevelopmentMilestoneRepository
  - babyProfileRepository: BabyProfileRepository
  - babyAccessPolicy: BabyAccessPolicy
  - auditService: AuditService
  + deleteMilestone(milestoneId, callerId): void
}
note right of DevelopmentMilestoneServiceImpl
  Class SHARED vá»›i UC196 â€” method
  deleteMilestone() thÃªm vÃ o CÃ™NG
  class DevelopmentMilestoneServiceImpl
  Ä‘Ã£ táº¡o á»Ÿ UC196 (khÃ´ng táº¡o Impl má»›i).
end note

class BabyAccessPolicy {
  + canView(profile, callerId): boolean
  + canManage(profile, callerId): boolean
}

DevelopmentMilestoneServiceImpl --> DevelopmentMilestoneRepository : uses
DevelopmentMilestoneServiceImpl --> BabyAccessPolicy : uses canManage() [reuse UC196]

@enduml
```

### 5.2. Data Structure (Flyway SQL Migration)

> â­ **KHÃ”NG táº¡o migration má»›i cho UC197.** Cá»™t `record_status` (dÃ¹ng Ä‘á»ƒ soft-delete) Ä‘Ã£ Ä‘Æ°á»£c thÃªm bá»Ÿi migration `V20260707120000__add_development_milestone_status_columns.sql` â€” Ä‘Æ°á»£c sá»Ÿ há»¯u vÃ  mÃ´ táº£ Ä‘áº§y Ä‘á»§ táº¡i **UC196 TDS Â§5.2**, vÃ¬ cáº£ hai cá»™t (`milestone_status` cho UC196, `record_status` cho UC197) Ä‘Æ°á»£c thÃªm CÃ™NG má»™t migration Ä‘á»ƒ trÃ¡nh 2 migration Ä‘á»¥ng Ä‘á»™ trÃªn cÃ¹ng báº£ng.

**TrÃ­ch dáº«n (khÃ´ng thay Ä‘á»•i bá»Ÿi UC197):**
```sql
ALTER TABLE public.development_milestones
    ADD COLUMN IF NOT EXISTS milestone_status VARCHAR(20) NOT NULL DEFAULT 'ACHIEVED'; -- owned by UC196
ALTER TABLE public.development_milestones
    ADD COLUMN IF NOT EXISTS record_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';      -- owned by UC197
CREATE INDEX IF NOT EXISTS idx_development_milestones_record_status
    ON public.development_milestones USING btree (record_status);
```

> **Gap ghi nháº­n:** Náº¿u UC196 chÆ°a deploy khi UC197 Ä‘Æ°á»£c implement (thá»© tá»± ngÆ°á»£c), migration `V20260707120000` váº«n PHáº¢I Ä‘Æ°á»£c táº¡o trÆ°á»›c â€” UC197 khÃ´ng tá»± táº¡o migration riÃªng Ä‘á»ƒ trÃ¡nh 2 file cÃ¹ng thÃªm cá»™t trÃ¹ng tÃªn (Flyway sáº½ lá»—i náº¿u 2 migration Ä‘á»™c láº­p cÃ¹ng target 1 cá»™t).

---

## 6. Dynamic Modeling (MÃ´ hÃ¬nh Äá»™ng)

### 6.1. Sequence Diagram â€” Happy Path (PlantUML)

```plantuml
@startuml DeleteDevelopmentMilestone_HappyPath
skinparam backgroundColor #FAFAFA
actor "Mother (owner)" as Client
participant "DevelopmentMilestoneController" as Controller
participant "DevelopmentMilestoneServiceImpl" as Service
participant "DevelopmentMilestoneRepository" as MRepo
participant "BabyProfileRepository" as PRepo
participant "BabyAccessPolicy" as Policy
participant "AuditService" as Audit
database "PostgreSQL" as DB

Client -> Controller : DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}
activate Controller
Controller -> Controller : SecurityUtils.requireCurrentUserId(principal)
Controller -> Service : deleteMilestone(milestoneId, callerId)
activate Service

Service -> MRepo : findById(milestoneId)
MRepo -> DB : SELECT * FROM development_milestones WHERE milestone_id=?
DB --> MRepo : DevelopmentMilestone row (recordStatus=ACTIVE)
MRepo --> Service : Optional<DevelopmentMilestone>

Service -> Service : verify recordStatus == ACTIVE (else -> 404 MILESTONE-001)

Service -> PRepo : findById(milestone.getBabyId())
PRepo -> DB : SELECT * FROM baby_profiles WHERE baby_id=?
DB --> PRepo : BabyProfile row
PRepo --> Service : Optional<BabyProfile>

Service -> Policy : canManage(profile, callerId)
Policy --> Service : true

Service -> Service : milestone.setRecordStatus(DELETED)\n(milestoneStatus KHÃ”NG Ä‘á»¥ng â€” ADR-BABY-006)
Service -> MRepo : save(milestone)
MRepo -> DB : UPDATE development_milestones\nSET record_status='DELETED', updated_at=now()\nWHERE milestone_id=?
DB --> MRepo : updated row
MRepo --> Service : DevelopmentMilestone

Service -> Audit : log(DEVELOPMENT_MILESTONE_DELETED, callerId, milestoneId)
Service --> Controller : void
deactivate Service
Controller --> Client : 200 OK {message: "deleted"}
deactivate Controller
@enduml
```

### 6.2. Sequence Diagram â€” Error Path (PlantUML)

```plantuml
@startuml DeleteDevelopmentMilestone_ErrorPath
skinparam backgroundColor #FAFAFA
actor "Mother (owner) â€” double delete attempt" as Client
participant "DevelopmentMilestoneController" as Controller
participant "DevelopmentMilestoneServiceImpl" as Service
participant "DevelopmentMilestoneRepository" as MRepo

Client -> Controller : DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}\n(second call, already deleted)
activate Controller
Controller -> Service : deleteMilestone(milestoneId, callerId)
activate Service
Service -> MRepo : findById(milestoneId)
MRepo --> Service : Optional<DevelopmentMilestone> (present, recordStatus=DELETED)
Service -> Service : recordStatus != ACTIVE -> throw BusinessException(404, "MILESTONE-001")
deactivate Service
Controller --> Client : 404 Not Found {code: MILESTONE-001}
deactivate Controller

note over Service
  Alternative: care group member (ACCEPTED, non-owner) -> canManage() false -> 403 MILESTONE-002
  Alternative: milestoneId not found at all -> 404 MILESTONE-001
  ADR-BABY-008: double-delete tráº£ 404 (KHÃ”NG 409) â€” nháº¥t quÃ¡n vá»›i UC194/195 pattern
end note
@enduml
```

### 6.3. State Machine â€” `recordStatus` (soft-delete lifecycle)

```plantuml
@startuml DeleteDevelopmentMilestone_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : milestone táº¡o má»›i (default)

ACTIVE --> DELETED : Mother xoÃ¡ má»m (UC-197)\nrecord_status = 'DELETED', updated_at = now()

note right of DELETED
  Invariant: DELETED lÃ  tráº¡ng thÃ¡i CUá»I (terminal) â€”
  KHÃ”NG cÃ³ transition DELETED -> ACTIVE trong pháº¡m vi
  UC197 (khÃ´ng cÃ³ "undo"/"restore" endpoint).
  Row váº«n tá»“n táº¡i váº­t lÃ½ trong DB (soft-delete, ADR-BABY-008).
end note

note right of ACTIVE
  Invariant: recordStatus KHÃ”NG BAO GIá»œ Ä‘Æ°á»£c set
  bá»Ÿi UC-196 (Update) â€” chá»‰ UC-197 Ä‘Æ°á»£c ghi field nÃ y.
  ADR-BABY-006.
end note

@enduml
```

> **âš ï¸ Invariant báº¥t biáº¿n:** `milestoneStatus` (FSM riÃªng, xem UC196 TDS Â§6.3) hoÃ n toÃ n Ä‘á»™c láº­p vá»›i `recordStatus` â€” khÃ´ng cÃ³ transition chÃ©o giá»¯a 2 FSM.

---

## 7. Domain Event Catalog

### 7.1. Events Published (PhÃ¡t ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| `DevelopmentMilestoneDeleted` | Sau khi `deleteMilestone()` commit thÃ nh cÃ´ng (`recordStatus â†’ DELETED`) | `DevelopmentMilestoneServiceImpl` | `audit` | `DevelopmentMilestoneDeletedEvent.java` | No (Ä‘á»“ng bá»™ qua `AuditService.log()`, nháº¥t quÃ¡n pattern hiá»‡n cÃ³) |

### 7.2. Events Consumed (TiÃªu thá»¥)

KhÃ´ng cÃ³.

### 7.3. Payload Schema

```java
// DevelopmentMilestoneDeletedEvent.java
public record DevelopmentMilestoneDeletedEvent(
    UUID    eventId,
    String  eventType,       // "DevelopmentMilestoneDeleted"
    Instant occurredAt,
    String  version,         // "1.0"
    Payload payload,
    Metadata metadata
) {
    public record Payload(
        UUID milestoneId,
        UUID babyId,
        UUID deletedByUserId,
        Instant deletedAt
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
// IDevelopmentMilestoneService.java â€” bá»• sung method vÃ o interface Ä‘Ã£ táº¡o á»Ÿ UC196
// @version 1.1 (breaking? NO â€” additive method)
public interface IDevelopmentMilestoneService {
    // ... updateMilestone() tá»« UC196 giá»¯ nguyÃªn ...

    /**
     * Soft-deletes a development milestone (sets recordStatus = DELETED).
     * Row is NEVER hard-deleted (ADR-BABY-008).
     * @throws BusinessException (MILESTONE-001/404) khi milestoneId khÃ´ng tá»“n táº¡i,
     *         HOáº¶C recordStatus Ä‘Ã£ lÃ  DELETED (double-delete treated as not-found)
     * @throws BusinessException (MILESTONE-002/403) khi caller khÃ´ng pháº£i account owner
     *         (canManage() strict ownership â€” ADR-BABY-007)
     */
    void deleteMilestone(UUID milestoneId, UUID callerId);
}
```

### 8.2. Entity & Repository Interface

> Entity `DevelopmentMilestone`, enums `MilestoneAchievementStatus`/`MilestoneRecordStatus`, vÃ  `DevelopmentMilestoneRepository` Ä‘Æ°á»£c Ä‘á»‹nh nghÄ©a Äáº¦Y Äá»¦ táº¡i **UC196 TDS Â§8.2** â€” UC197 KHÃ”NG táº¡o báº£n sao. Repository method `findById()` káº¿ thá»«a tá»« `JpaRepository` lÃ  Ä‘á»§ (Ä‘Ã£ dÃ¹ng chung á»Ÿ UC196).

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `DELETE` | `/api/v1/babies/{babyId}/milestones/{milestoneId}` | JWT Bearer | `ROLE_MOTHER` | 30/min | Yes (idempotent vá» hiá»‡u á»©ng cuá»‘i â€” double-delete â†’ 404, khÃ´ng side-effect thÃªm) |

> **Path design note:** CÃ¹ng resource path vá»›i UC196's `PATCH`. `babyId` trong path CHá»ˆ dÃ¹ng routing â€” authorization luÃ´n dá»±a trÃªn `milestone.getBabyId()` Ä‘á»c tá»« DB.

### 9.2. Request / Response Schemas

#### `DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}`

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response â€” 200 OK:**
```json
{
  "success": true,
  "message": "Development milestone deleted successfully",
  "data": null
}
```

**Response â€” 403 Forbidden:**
```json
{
  "error": { "code": "MILESTONE-002", "message": "Access denied to delete this development milestone" }
}
```

**Response â€” 404 Not Found:**
```json
{
  "error": { "code": "MILESTONE-001", "message": "Development milestone not found" }
}
```

---

## 10. Báº£ng mÃ£ lá»—i (Error Codes)

> DÃ¹ng chung prefix `MILESTONE-` vá»›i UC196 â€” cÃ¹ng module `development_milestones`.

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `MILESTONE-001` | 404 | Development milestone not found | KhÃ´ng tÃ¬m tháº¥y má»‘c phÃ¡t triá»ƒn | `milestoneId` khÃ´ng tá»“n táº¡i HOáº¶C `recordStatus` Ä‘Ã£ lÃ  `DELETED` (double-delete, ADR-BABY-008) HOáº¶C `baby_id` FK khÃ´ng resolve Ä‘Æ°á»£c `BabyProfile` |
| `MILESTONE-002` | 403 | Access denied to delete this development milestone | KhÃ´ng Ä‘á»§ quyá»n xoÃ¡ má»‘c phÃ¡t triá»ƒn | Caller KHÃ”NG pháº£i account owner (`canManage()` false â€” strict) |
| `MILESTONE-004` | 500 | Internal error | Lá»—i há»‡ thá»‘ng | Unexpected DB error |

---

## 11. Quy trÃ¬nh Triá»ƒn khai (Step-by-Step)

### 11.1. Prerequisites
- [ ] TDS nÃ y (UC197) vÃ  TDS UC196 (companion) Ä‘á»u Approved
- [ ] Migration `V20260707120000` (sá»Ÿ há»¯u bá»Ÿi UC196) Ä‘Ã£ cháº¡y thÃ nh cÃ´ng
- [ ] `DevelopmentMilestone` entity, `DevelopmentMilestoneRepository`, `BabyAccessPolicy.canManage()` (UC196) Ä‘Ã£ cÃ³ trong codebase

### 11.2. Pre-Migration Checklist
- KhÃ´ng Ã¡p dá»¥ng â€” UC197 KHÃ”NG cÃ³ migration riÃªng (dÃ¹ng chung `V20260707120000` vá»›i UC196, xem Â§5.2).

### 11.3. Implementation Steps

#### Cháº·ng 1 â€” Service method (thÃªm vÃ o class Ä‘Ã£ cÃ³ tá»« UC196)
ThÃªm method `deleteMilestone(UUID, UUID)` vÃ o `DevelopmentMilestoneServiceImpl.java` (class Ä‘Ã£ tá»“n táº¡i tá»« UC196 â€” KHÃ”NG táº¡o Impl má»›i).

#### Cháº·ng 2 â€” Controller method (thÃªm vÃ o class Ä‘Ã£ cÃ³ tá»« UC196)
ThÃªm method `deleteMilestone` (`@DeleteMapping("/{milestoneId}")`) vÃ o `DevelopmentMilestoneController.java` (Ä‘Ã£ tá»“n táº¡i tá»« UC196).

#### Cháº·ng 3 â€” Audit
Bá»• sung `DEVELOPMENT_MILESTONE_DELETED` vÃ o `AuditAction.java` (additive, cÃ¹ng Ä‘á»£t vá»›i `DEVELOPMENT_MILESTONE_UPDATED` cá»§a UC196).

#### Cháº·ng 4 â€” Verification sau deploy
```bash
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 200 {"message": "Development milestone deleted successfully"}

# Verify soft-delete (not hard-delete)
psql -c "SELECT milestone_id, record_status FROM development_milestones WHERE milestone_id='[milestoneId]'"
# Expected: 1 row returned, record_status = 'DELETED'
```

### 11.4. Deployment Checklist
- [ ] `./mvnw test` xanh
- [ ] Disambiguation test PASS: delete KHÃ”NG Ä‘á»•i `milestone_status` trong DB
- [ ] Row count verification: xoÃ¡ KHÃ”NG giáº£m `COUNT(*)` cá»§a báº£ng `development_milestones`
- [ ] IDOR test (non-owner, ká»ƒ cáº£ care group ACCEPTED â†’ 403) pass
- [ ] Double-delete test: gá»i DELETE 2 láº§n liÃªn tiáº¿p â†’ láº§n 2 tráº£ 404 (khÃ´ng 500, khÃ´ng side-effect)

---

## 12. Rollback & Incident Runbook

### 12.1. Äiá»u kiá»‡n kÃ­ch hoáº¡t Rollback

| Äiá»u kiá»‡n | NgÆ°á»¡ng | NgÆ°á»i quyáº¿t Ä‘á»‹nh |
|-----------|--------|------------------|
| Error rate tÄƒng Ä‘á»™t biáº¿n | > 5% trong 5 phÃºt | On-call Engineer |
| PhÃ¡t hiá»‡n hard-delete xáº£y ra ngoÃ i Ã½ muá»‘n (row count giáº£m) | Báº¥t ká»³ case nÃ o | Tech Lead + DPO |

### 12.2. Rollback Procedure

```bash
# KhÃ´ng cÃ³ migration riÃªng cho UC197 (dÃ¹ng chung V20260707120000 vá»›i UC196)
# Rollback chá»‰ cáº§n revert code deploy
kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api
curl -X GET https://[host]/api/v1/health

# Náº¿u cáº§n "khÃ´i phá»¥c" record Ä‘Ã£ bá»‹ soft-delete nháº§m do bug (data recovery, KHÃ”NG pháº£i rollback code):
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE development_milestones SET record_status='ACTIVE' WHERE milestone_id='[affectedId]';"
# âš ï¸ Thao tÃ¡c thá»§ cÃ´ng â€” CHá»ˆ thá»±c hiá»‡n dÆ°á»›i sá»± giÃ¡m sÃ¡t Tech Lead + ghi vÃ o incident log
```

### 12.3. Notification Protocol

| Thá»i Ä‘iá»ƒm | NgÆ°á»i nháº­n | KÃªnh |
|-----------|------------|------|
| Ngay khi phÃ¡t hiá»‡n hard-delete ngoÃ i Ã½ muá»‘n | Tech Lead + DPO | Slack `#incident` + Email |

---

## 13. Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t

> **Policy (EDS v2.0):** Má»i test scenario dÃ¹ng dá»¯ liá»‡u `SYNTHETIC`.

```gherkin
Feature: Delete Development Milestone
  Background:
    Given test data classification: SYNTHETIC
    And MOTHER-001 lÃ  owner cá»§a BABY-001
    And MILESTONE-001 thuá»™c BABY-001 vá»›i milestoneStatus=ACHIEVED, recordStatus=ACTIVE

  Scenario: Owner xoÃ¡ má»m milestone â†’ 200
    When deleteMilestone(MILESTONE-001, MOTHER-001)
    Then response 200
    And DB row MILESTONE-001 váº«n tá»“n táº¡i (COUNT khÃ´ng giáº£m)
    And record_status = 'DELETED'

  Scenario: [DISAMBIGUATION â€” CRITICAL] XoÃ¡ KHÃ”NG Ä‘á»¥ng milestoneStatus
    When deleteMilestone(MILESTONE-001, MOTHER-001)
    Then DB row cÃ³ milestone_status váº«n = 'ACHIEVED' (giá»¯ nguyÃªn, khÃ´ng bá»‹ reset)
    And record_status = 'DELETED'

  Scenario: Care group member (ACCEPTED, non-owner) â†’ 403
    Given MOTHER-002 lÃ  ACCEPTED member trong care group cá»§a BABY-001 (khÃ´ng pháº£i owner)
    When deleteMilestone(MILESTONE-001, MOTHER-002)
    Then throws BusinessException MILESTONE-002 (403)

  Scenario: Non-owner, non-member â†’ 403
    Given MOTHER-003 KHÃ”NG liÃªn quan BABY-001
    When deleteMilestone(MILESTONE-001, MOTHER-003)
    Then throws BusinessException MILESTONE-002 (403)

  Scenario: Milestone khÃ´ng tá»“n táº¡i â†’ 404
    When deleteMilestone(NONEXISTENT, MOTHER-001)
    Then throws BusinessException MILESTONE-001 (404)

  Scenario: Double-delete (Ä‘Ã£ DELETED tá»« trÆ°á»›c) â†’ 404
    Given MILESTONE-002 thuá»™c BABY-001 vá»›i recordStatus=DELETED
    When deleteMilestone(MILESTONE-002, MOTHER-001)
    Then throws BusinessException MILESTONE-001 (404)
    And khÃ´ng cÃ³ side-effect nÃ o khÃ¡c (idempotent â€” DB khÃ´ng Ä‘á»•i thÃªm)

  Scenario: XoÃ¡ KHÃ”NG hard-delete row
    Given development_milestones cÃ³ N rows trÆ°á»›c khi gá»i deleteMilestone
    When deleteMilestone(MILESTONE-001, MOTHER-001)
    Then COUNT(*) FROM development_milestones váº«n = N (khÃ´ng giáº£m)
```

---

## 14. PhÆ°Æ¡ng phÃ¡p XÃ¡c minh

### 14.1. Database Inspection

```sql
-- Verify soft-delete applied (row still present)
SELECT milestone_id, baby_id, milestone_status, record_status, updated_at
FROM development_milestones WHERE milestone_id = '[milestoneId]';
-- Expected: 1 row, record_status = 'DELETED', milestone_status UNCHANGED

-- Verify NOT hard-deleted (row count unchanged before/after)
SELECT COUNT(*) FROM development_milestones WHERE baby_id = '[babyId]';

-- Verify ownership chain
SELECT bp.owner_user_id
FROM development_milestones dm
JOIN baby_profiles bp ON bp.baby_id = dm.baby_id
WHERE dm.milestone_id = '[milestoneId]';
```

### 14.2. Access Policy Verification

```bash
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [OWNER_JWT]"
# Expected: 200

curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [CARE_GROUP_MEMBER_JWT]"
# Expected: 403

# Double-delete
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [OWNER_JWT]"
# Expected: 404 (second call)
```

---

## 15. Máº«u thá»­ thá»±c táº¿ (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 200 {"success": true, "message": "Development milestone deleted successfully"}
```

### 15.2. Error Paths

```bash
# Non-existent milestone -> 404
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/non-existent-uuid \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"

# Care group member (ACCEPTED, non-owner) -> 403
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [CARE_GROUP_MEMBER_JWT]"

# Double-delete -> 404 second time
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"

# No JWT -> 401
curl -X DELETE https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId]
```

---

## 16. Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix)

| Endpoint | `GUEST` | `MOTHER (owner)` | `MOTHER (care member, ACCEPTED)` | `EXPERT` | `ADMIN` |
|----------|---------|-------------------|-----------------------------------|----------|---------|
| `DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}` | âŒ (401) | âœ… | âŒ (403) | âŒ (403) | âœ… All |

**ChÃº thÃ­ch:**
- Owner: `baby_profiles.owner_user_id` == JWT subject (via `development_milestones.baby_id` FK) â€” `canManage()` strict (ADR-BABY-007).
- Care member (ká»ƒ cáº£ ACCEPTED): KHÃ”NG Ä‘Æ°á»£c xoÃ¡ â€” cÃ¹ng nguyÃªn táº¯c vá»›i UC196.
- Expert: khÃ´ng cÃ³ quyá»n, ngoÃ i pháº¡m vi UC.

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source | Last Verified |
|---|-----------|--------|---------------|
| C1 | `deleteMilestone()` CHá»ˆ Ä‘Æ°á»£c ghi vÃ o field `recordStatus` â€” TUYá»†T Äá»I KHÃ”NG Ä‘á»¥ng `milestoneStatus` | ADR-BABY-006 | 2026-07-03 |
| C2 | XoÃ¡ LUÃ”N LUÃ”N lÃ  soft-delete (`recordStatus = DELETED`) â€” TUYá»†T Äá»I KHÃ”NG dÃ¹ng `repository.delete()`/hard-delete | ADR-BABY-008 | 2026-07-03 |
| C3 | Authorization PHáº¢I dÃ¹ng `BabyAccessPolicy.canManage()` (strict ownership) â€” KHÃ”NG dÃ¹ng `canView()` | ADR-BABY-007 | 2026-07-03 |
| C4 | Double-delete (record Ä‘Ã£ `DELETED`) tráº£ 404 `MILESTONE-001` â€” KHÃ”NG tráº£ 409, KHÃ”NG tráº£ 500 | ADR-BABY-008 | 2026-07-03 |
| C5 | KHÃ”NG táº¡o migration má»›i cho UC197 â€” dÃ¹ng chung `V20260707120000` vá»›i UC196 (cá»™t `record_status` Ä‘Ã£ cÃ³ sáºµn) | UC196 TDS Â§5.2 | 2026-07-03 |

### 17.2 Constraint Injection Block

```
[CONSTRAINT BLOCK â€” Module: DeleteDevelopmentMilestone (CB-BABY-IMP-005)]
1. deleteMilestone() CHá»ˆ ghi vÃ o entity field recordStatus (=DELETED) â€” TUYá»†T Äá»I KHÃ”NG Ä‘á»¥ng milestoneStatus â€” ADR-BABY-006
2. XoÃ¡ LUÃ”N lÃ  soft-delete â€” KHÃ”NG BAO GIá»œ gá»i repository.delete()/deleteById() â€” chá»‰ save() vá»›i recordStatus=DELETED â€” ADR-BABY-008
3. Authorization dÃ¹ng babyAccessPolicy.canManage(profile, callerId) â€” strict ownership, KHÃ”NG dÃ¹ng canView() â€” ADR-BABY-007
4. recordStatus Ä‘Ã£ lÃ  DELETED (double-delete) -> 404 MILESTONE-001, KHÃ”NG 409/500
5. KHÃ”NG táº¡o file migration má»›i â€” reuse V20260707120000 Ä‘Ã£ táº¡o bá»Ÿi UC196 (cá»™t record_status)

[CONTEXT BLOCK]
- Bounded Context: baby (reuse UC192/UC194/UC196 package â€” com.carebridge.backend.baby)
- Data Classification: Sensitive-PII
- Error codes: Â§10 Error Codes Table (dÃ¹ng chung prefix MILESTONE- vá»›i UC196)
- Auth matrix: Â§16 Authorization Matrix
- Reused classes: DevelopmentMilestone entity, DevelopmentMilestoneRepository, BabyAccessPolicy.canManage() (Táº¤T Cáº¢ tá»« UC196 â€” khÃ´ng táº¡o báº£n sao)
- Companion: UC196 Update Development Milestone â€” method deleteMilestone() thÃªm vÃ o CÃ™NG class DevelopmentMilestoneServiceImpl/Controller Ä‘Ã£ tá»“n táº¡i
```

### 17.3 Constraint Quality Checklist

- [x] Má»—i constraint traceable vá» ADR hoáº·c BR cá»¥ thá»ƒ
- [x] KhÃ´ng cÃ³ constraint generic
- [x] Constraint block cÃ³ â‰¥ 3 constraints cá»¥ thá»ƒ

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | HÃ nh Ä‘á»™ng |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code khÃ´ng match constraint C1-C5 | Reject â€” inject láº¡i constraints |
| AP-AI-003 | Implicit Decision | Code gá»i `repository.delete()` (hard-delete) thay vÃ¬ soft-delete, hoáº·c táº¡o migration má»›i trÃ¹ng cá»™t | Reject â€” vi pháº¡m ADR-BABY-008 |
| AP-AI-005 | Hallucinated Contract | Code import class khÃ´ng cÃ³ trong Â§8/UC196 Â§8.2 | Reject â€” verify contract |

---

## PHá»¤ Lá»¤C

### A. Glossary (Thuáº­t ngá»¯)

| Thuáº­t ngá»¯ | Äá»‹nh nghÄ©a |
|-----------|------------|
| Soft-delete | ÄÃ¡nh dáº¥u record lÃ  "Ä‘Ã£ xoÃ¡" qua cá»™t tráº¡ng thÃ¡i (`record_status = DELETED`) mÃ  khÃ´ng xoÃ¡ váº­t lÃ½ khá»i DB |
| Hard-delete | XoÃ¡ váº­t lÃ½ row khá»i DB (`DELETE FROM ...`) â€” KHÃ”NG dÃ¹ng cho module nÃ y |
| Double-delete | Gá»i delete láº§n thá»© 2 trÃªn cÃ¹ng record Ä‘Ã£ bá»‹ soft-delete tá»« trÆ°á»›c |
| `recordStatus` | Tráº¡ng thÃ¡i vÃ²ng Ä‘á»i record (ACTIVE/DELETED) â€” tÃ¡ch biá»‡t hoÃ n toÃ n khá»i `milestoneStatus` (achievement) |

### B. TÃ i liá»‡u tham chiáº¿u

| Document | Path |
|----------|------|
| UC196 TDS (companion â€” sá»Ÿ há»¯u entity/migration/canManage(), ADR-BABY-006/007 Ä‘áº§y Ä‘á»§) | `04_Implement/UC196_UpdateDevelopmentMilestone/UC196_UpdateDevelopmentMilestone_TDS.md` |
| UC192 TDS (Approved, shipped code reference) | `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` |
| UC194 TDS (soft-delete "treat as 404" precedent) | `04_Implement/UC194_ViewBabyDailyLogDetail/UC194_ViewBabyDailyLogDetail_TDS.md` |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |
| Schema baseline | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` |

---

## Open Items (chÆ°a resolve â€” cáº§n Tech Lead / Product xÃ¡c nháº­n trÆ°á»›c khi Approve)

| # | Item | MÃ´ táº£ | Äá» xuáº¥t táº¡m thá»i |
|---|------|-------|-------------------|
| OI-1 | "Undo delete" / restore endpoint | UC197 khÃ´ng Ä‘á»‹nh nghÄ©a cÃ¡ch khÃ´i phá»¥c milestone Ä‘Ã£ xoÃ¡ nháº§m qua API â€” chá»‰ cÃ³ thao tÃ¡c thá»§ cÃ´ng DB (Â§12.2) | NgoÃ i pháº¡m vi UC197 â€” ghi nháº­n cho future UC "RestoreDevelopmentMilestone" náº¿u Product yÃªu cáº§u |
| OI-2 | Row retention/cleanup policy cho record `DELETED` lÃ¢u nÄƒm | ChÆ°a cÃ³ job dá»n dáº¹p/archival cho record soft-delete tÃ­ch luá»¹ lÃ¢u dÃ i | Theo dÃµi tÄƒng trÆ°á»Ÿng báº£ng; Ä‘á» xuáº¥t archival job náº¿u > 6 thÃ¡ng vÃ  > 10K rows |
| OI-3 | Thá»© tá»± triá»ƒn khai UC196/UC197 | Náº¿u UC197 Ä‘Æ°á»£c code trÆ°á»›c UC196 (song song), migration `V20260707120000` cáº§n coordinate Ä‘á»ƒ trÃ¡nh 2 PR cÃ¹ng táº¡o file trÃ¹ng tÃªn | Coordinate qua PR review â€” chá»‰ 1 PR Ä‘Æ°á»£c táº¡o file migration, PR cÃ²n láº¡i rebase |

---

*EDS v2.1 â€” TÃ­ch há»£p CASE 2.0 AI Prompt Constraints (Â§17).*
