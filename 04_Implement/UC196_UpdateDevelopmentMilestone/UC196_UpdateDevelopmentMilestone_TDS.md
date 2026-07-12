# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification â€” UC-196 Update Development Milestone

| Field | Value |
|-------|-------|
| **Document ID** | `CB-BABY-IMP-004` |
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
| 2026-07-03 | AI Agent | Táº¡o tÃ i liá»‡u láº§n Ä‘áº§u cho UC-196 Update Development Milestone |

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
| **Module Name** | `UpdateDevelopmentMilestone` |
| **Bounded Context** | `baby` (reuse â€” same bounded context as UC192 `BabyController`/`BabyServiceImpl`/`BabyAccessPolicy`, and UC194's `BabyDailyLog` sibling classes) |
| **UC ID** | `UC-196` |
| **SRS Reference** | `3.3.12.5` (`02_Requirements/SRS/3_Functional_Specification.md` lines 4217-4236) |
| **Primary Actor** | `Mother (ROLE_MOTHER)` |
| **Platform** | `Mobile App` |
| **Priority** | `Medium` |
| **Sprint** | `Sprint 4 â€” Device Sync And Care Edge Cases` |
| **Owner** | `TV2-BÃ¡ch` |
| **Data Classification** | `Sensitive-PII` (infant developmental/health data) |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy â€” UC192)`, `auth`, `development_milestones` table |
| **Downstream Consumers** | `UC197 Delete Development Milestone`, `Development Milestone Timeline (future UC)` |

**MÃ´ táº£:** Cho phÃ©p Mother cáº­p nháº­t `achieved_date`, `note`, hoáº·c tráº¡ng thÃ¡i tiáº¿n triá»ƒn (achievement status) cá»§a Má»˜T development milestone (`development_milestones`) Ä‘Ã£ ghi nháº­n cho baby cá»§a mÃ¬nh. Ownership resolved qua chain `development_milestones.baby_id â†’ baby_profiles.owner_user_id`. ÄÃ¢y lÃ  greenfield code: KHÃ”NG cÃ³ `DevelopmentMilestone` entity/controller/service nÃ o tá»“n táº¡i trong codebase hiá»‡n táº¡i (xÃ¡c nháº­n qua RG-3 Â§3 ADR-BABY-006 bÃªn dÆ°á»›i). Báº£ng `development_milestones` hiá»‡n táº¡i **KHÃ”NG cÃ³ cá»™t `status` nÃ o cáº£** â€” cáº§n migration má»›i, vÃ  quyáº¿t Ä‘á»‹nh thiáº¿t káº¿ quan trá»ng nháº¥t cá»§a tÃ i liá»‡u nÃ y lÃ  phÃ¢n biá»‡t rÃµ hai khÃ¡i niá»‡m "status" khÃ¡c nhau (xem ADR-BABY-006).

---

## 2. Ma tráº­n Truy váº¿t (Traceability Matrix)

| Requirement ID | Loáº¡i | MÃ´ táº£ | ThÃ nh pháº§n Code | Compliance Target | ADR liÃªn quan |
|----------------|------|-------|-----------------|-------------------|---------------|
| UC-196 | Use Case | Mother cáº­p nháº­t date/notes/status cá»§a 1 development milestone | `DevelopmentMilestoneController.updateMilestone()` | BR-RBAC | ADR-BABY-007 |
| BR-RBAC | Business Rule | Chá»‰ owner cá»§a baby profile (strict â€” khÃ´ng care group member) má»›i update Ä‘Æ°á»£c | `DevelopmentMilestoneServiceImpl.updateMilestone()` + `BabyAccessPolicy.canManage()` (new method) | BR-RBAC | ADR-BABY-007 |
| BR-PRIVACY | Business Rule | Response chá»‰ tráº£ field liÃªn quan â€” minimum-necessary | `DevelopmentMilestoneDetailResponse` DTO | BR-PRIVACY | ADR-BABY-006 |
| â€” | Design Decision | `milestone_status` (achievement) vÃ  `record_status` (soft-delete) PHáº¢I lÃ  2 cá»™t Ä‘á»™c láº­p | `DevelopmentMilestone` entity â€” 2 enum fields riÃªng biá»‡t | Data Integrity | ADR-BABY-006 |

---

## 3. Architecture Decision Records (ADR)

### ADR-BABY-006 â€” Achievement-Status vs Soft-Delete-Status Disambiguation â­ MANDATORY

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `TV2-BÃ¡ch, AI Agent` |
| **Date** | `2026-07-03` |
| **Supersedes** | â€” |

#### Bá»‘i cáº£nh (Context)

SRS Â§3.3.12.5 mÃ´ táº£ UC-196: "Updates date, notes, **or status** for a development milestone." SRS Â§3.3.12.6 mÃ´ táº£ UC-197: "**Soft-deletes** a Mother-recorded development milestone." Äá»c lÆ°á»›t qua, cáº£ hai UC Ä‘á»u Ä‘á»™ng Ä‘áº¿n khÃ¡i niá»‡m "status" cá»§a cÃ¹ng má»™t báº£ng `development_milestones`, dáº«n Ä‘áº¿n rá»§i ro nháº§m láº«n nghiÃªm trá»ng: náº¿u implement báº±ng **má»™t cá»™t `status` duy nháº¥t** (giá»‘ng pattern UC194/UC195 dÃ¹ng cho `baby_daily_logs.status` ACTIVE/DELETED), thÃ¬ UC-196 update "status" (vd: Ä‘á»•i milestone tá»« "chÆ°a Ä‘áº¡t" sang "Ä‘Ã£ Ä‘áº¡t") sáº½ **vÃ´ tÃ¬nh ghi Ä‘Ã¨** giÃ¡ trá»‹ soft-delete marker, hoáº·c ngÆ°á»£c láº¡i UC-197 xoÃ¡ má»m sáº½ phÃ¡ huá»· thÃ´ng tin tiáº¿n triá»ƒn milestone mÃ  Mother Ä‘Ã£ ghi nháº­n.

**RG-3 xÃ¡c nháº­n (Research Gate):**
```bash
grep -rn "development_milestones\|DevelopmentMilestone" 05_Development/CareBridgeAPI/src/main/java
# 0 káº¿t quáº£ â€” KHÃ”NG cÃ³ Java mapping nÃ o tá»“n táº¡i. Entity/Repository/Service/Controller lÃ  greenfield.
```

**Schema thá»±c táº¿ (`V1__init_schema.sql` dÃ²ng 635-645) â€” KHÃ”NG cÃ³ cá»™t `status`:**
```sql
CREATE TABLE public.development_milestones (
    milestone_id   uuid        NOT NULL DEFAULT gen_random_uuid(),
    baby_id        uuid        NOT NULL,
    milestone_type varchar(80) NOT NULL,
    achieved_date  date,
    note           text,
    source_type    varchar(30),
    recorded_by    uuid,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);
-- PK: milestone_id
-- FK: baby_id -> baby_profiles(baby_id); recorded_by -> users(user_id)
-- INDEX: idx_development_milestones_baby_id
```
KhÃ´ng cÃ³ báº¥t ká»³ cá»™t `status` nÃ o â€” cáº£ achievement-status láº«n soft-delete marker Ä‘á»u **thiáº¿u hoÃ n toÃ n**. ÄÃ¢y lÃ  gap tháº­t sá»± (khÃ´ng pháº£i tÃ i liá»‡u sai), cáº§n má»™t migration má»›i bá»• sung **HAI cá»™t Ä‘á»™c láº­p**.

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t (Options Considered)

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | Má»™t cá»™t `status` duy nháº¥t, dÃ¹ng chung giÃ¡ trá»‹ enum má»Ÿ rá»™ng (vd: `PENDING`, `ACHIEVED`, `DELAYED`, `DELETED`) â€” giá»‘ng style `baby_daily_logs` (UC194/195) | Ãt cá»™t hÆ¡n, Ä‘Æ¡n giáº£n schema | **Conflict nghiÃªm trá»ng**: UC-196 set status=`ACHIEVED` vÃ  UC-197 set status=`DELETED` ghi Ä‘Ã¨ láº«n nhau â€” khÃ´ng thá»ƒ vá»«a biáº¿t milestone Ä‘Ã£ Ä‘áº¡t hay chÆ°a, vá»«a biáº¿t record cÃ³ bá»‹ xoÃ¡ hay khÃ´ng, cÃ¹ng lÃºc. Vi pháº¡m nguyÃªn táº¯c Single Responsibility per column. |
| B | Hai cá»™t Ä‘á»™c láº­p: `milestone_status` (achievement progress: `PENDING`/`ACHIEVED`/`DELAYED`) vÃ  `record_status` (lifecycle: `ACTIVE`/`DELETED`) | TÃ¡ch biá»‡t hoÃ n toÃ n 2 khÃ¡i niá»‡m nghiá»‡p vá»¥ khÃ¡c nhau â€” UC-196 CHá»ˆ Ä‘Æ°á»£c ghi vÃ o `milestone_status`, UC-197 CHá»ˆ Ä‘Æ°á»£c ghi vÃ o `record_status`. Dá»… audit, dá»… test Ä‘á»™c láº­p, khÃ´ng cÃ³ write-conflict giá»¯a 2 UC. | ThÃªm 1 cá»™t so vá»›i phÆ°Æ¡ng Ã¡n A; cáº§n migration má»›i rÃµ rÃ ng hÆ¡n. |

#### Quyáº¿t Ä‘á»‹nh (Decision)

Chá»n **PhÆ°Æ¡ng Ã¡n B**. Bá»• sung migration `V20260707120000__add_development_milestone_status_columns.sql` thÃªm HAI cá»™t Ä‘á»™c láº­p vÃ o `development_milestones`:

```sql
-- UC-196 Update Development Milestone / UC-197 Delete Development Milestone
-- TÃ¡ch biá»‡t 2 khÃ¡i niá»‡m "status" khÃ¡c nhau hoÃ n toÃ n trÃªn development_milestones:
--   milestone_status = achievement progress (PENDING/ACHIEVED/DELAYED) â€” CHá»ˆ mutate bá»Ÿi UC-196
--   record_status    = soft-delete lifecycle (ACTIVE/DELETED)          â€” CHá»ˆ mutate bá»Ÿi UC-197
-- Hai cá»™t nÃ y PHáº¢I Ä‘á»™c láº­p tuyá»‡t Ä‘á»‘i â€” xem ADR-BABY-006, UC196 TDS Â§3.

ALTER TABLE public.development_milestones
    ADD COLUMN IF NOT EXISTS milestone_status VARCHAR(20) NOT NULL DEFAULT 'ACHIEVED';

ALTER TABLE public.development_milestones
    ADD COLUMN IF NOT EXISTS record_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_development_milestones_record_status
    ON public.development_milestones USING btree (record_status);
```

**Backfill rationale cho DEFAULT:** CÃ¡c record hiá»‡n cÃ³ trong DB Ä‘á»u Ä‘Æ°á»£c ghi nháº­n kÃ¨m `achieved_date` (theo mÃ´ táº£ nghiá»‡p vá»¥ hiá»‡n táº¡i â€” Mother ghi milestone sau khi xáº£y ra), nÃªn default `milestone_status = 'ACHIEVED'` lÃ  an toÃ n cho backfill. `record_status = 'ACTIVE'` lÃ  default chuáº©n cho má»i soft-delete pattern trong CareBridge (xem `V20260627100200__add_maternal_metric_status.sql` â€” cÃ¹ng convention `DEFAULT 'ACTIVE'`).

**Quy táº¯c code báº¯t buá»™c (enforced á»Ÿ Service layer, KHÃ”NG chá»‰ á»Ÿ DB):**
1. `DevelopmentMilestoneServiceImpl.updateMilestone()` (UC-196) chá»‰ Ä‘Æ°á»£c phÃ©p ghi vÃ o field `milestoneStatus` cá»§a entity â€” **KHÃ”NG BAO GIá»œ** Ä‘Æ°á»£c set `recordStatus`.
2. `DevelopmentMilestoneServiceImpl.deleteMilestone()` (UC-197, xem TDS riÃªng) chá»‰ Ä‘Æ°á»£c phÃ©p ghi vÃ o field `recordStatus` (set `DELETED`) â€” **KHÃ”NG BAO GIá»œ** Ä‘Æ°á»£c Ä‘á»•i `milestoneStatus`.
3. Cáº£ hai method PHáº¢I kiá»ƒm tra `recordStatus == ACTIVE` trÆ°á»›c khi cho phÃ©p thao tÃ¡c â€” náº¿u `recordStatus == DELETED`, tráº£ `404 MILESTONE-001` (record coi nhÆ° khÃ´ng tá»“n táº¡i), **khÃ´ng phÃ¢n biá»‡t** Ä‘Ã³ lÃ  do Ä‘Ã£ bá»‹ UC-197 xoÃ¡ trÆ°á»›c Ä‘Ã³ hay do input sai.

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:**
- Loáº¡i bá» hoÃ n toÃ n kháº£ nÄƒng UC-196 vÃ´ tÃ¬nh "há»“i sinh" má»™t record Ä‘Ã£ bá»‹ soft-delete, hoáº·c UC-197 vÃ´ tÃ¬nh xoÃ¡ máº¥t lá»‹ch sá»­ tiáº¿n triá»ƒn milestone.
- Hai UC cÃ³ thá»ƒ Ä‘Æ°á»£c test, deploy, vÃ  audit Ä‘á»™c láº­p mÃ  khÃ´ng sá»£ side-effect chÃ©o.
- Nháº¥t quÃ¡n vá»›i nguyÃªn táº¯c Single Responsibility Ã¡p dá»¥ng á»Ÿ cáº¥p Ä‘á»™ column.

**TiÃªu cá»±c / Trade-offs:**
- ThÃªm 1 cá»™t so vá»›i phÆ°Æ¡ng Ã¡n tá»‘i giáº£n â€” cháº¥p nháº­n Ä‘Æ°á»£c, chi phÃ­ storage khÃ´ng Ä‘Ã¡ng ká»ƒ.
- Developer PHáº¢I nhá»› dÃ¹ng Ä‘Ãºng field â€” giáº£m thiá»ƒu rá»§i ro báº±ng cÃ¡ch Ä‘áº·t tÃªn rÃµ rÃ ng (`milestoneStatus` vs `recordStatus`, khÃ´ng dÃ¹ng tÃªn chung `status`) vÃ  báº±ng unit test disambiguation báº¯t buá»™c (xem Test-Spec Â§MILESTONE-UPD-TC-DISAMB).

**Compliance Impact:**
- Cá»§ng cá»‘ BR-PRIVACY: dá»¯ liá»‡u sá»©c khoáº» phÃ¡t triá»ƒn cá»§a tráº» khÃ´ng bá»‹ máº¥t hoáº·c sai lá»‡ch do nháº§m láº«n logic â€” giáº£m rá»§i ro data integrity incident cáº§n bÃ¡o cÃ¡o.

---

### ADR-BABY-007 â€” Strict Ownership (KHÃ”NG Care-Group) cho Milestone Mutation

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `TV2-BÃ¡ch, AI Agent` |
| **Date** | `2026-07-03` |
| **Supersedes** | â€” |

#### Bá»‘i cáº£nh (Context)

`BabyAccessPolicy.canView()` (UC192, Ä‘Ã£ ship) cho phÃ©p **cáº£ owner LáºªN care group member (ACCEPTED)** xem baby profile â€” vÃ  UC194 Ä‘Ã£ tÃ¡i sá»­ dá»¥ng y há»‡t cho viá»‡c xem `baby_daily_logs`. Tuy nhiÃªn, SRS Â§3.3.12.5/3.3.12.6 xÃ¡c Ä‘á»‹nh rÃµ **Primary Actor = Mother** (khÃ´ng cÃ³ Secondary Actor), vÃ  mÃ´ táº£ "Mother-recorded development milestone" â€” ngá»¥ Ã½ quyá»n **sá»­a/xoÃ¡** nÃªn háº¹p hÆ¡n quyá»n **xem**. Náº¿u tÃ¡i sá»­ dá»¥ng nguyÃªn `canView()` cho UC-196/UC-197, má»™t Family member chá»‰ Ä‘Æ°á»£c má»i xem (ACCEPTED nhÆ°ng khÃ´ng pháº£i owner) sáº½ cÃ³ thá»ƒ sá»­a/xoÃ¡ dá»¯ liá»‡u milestone cá»§a Mother khÃ¡c â€” vi pháº¡m nguyÃªn táº¯c least-privilege vÃ  táº¡o lá»— há»•ng IDOR-adjacent (Broken Access Control â€” quyá»n ghi bá»‹ cáº¥p quÃ¡ rá»™ng).

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t (Options Considered)

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | TÃ¡i sá»­ dá»¥ng nguyÃªn `BabyAccessPolicy.canView()` cho cáº£ UC-196/UC-197 | Tá»‘i giáº£n, khÃ´ng sá»­a code UC192 | Cáº¥p quyá»n ghi quÃ¡ rá»™ng cho care group member â€” vi pháº¡m least-privilege, khÃ´ng khá»›p SRS Primary Actor |
| B | Viáº¿t `DevelopmentMilestoneAccessPolicy` riÃªng, duplicate ownership check | Isolation module | TrÃ¹ng láº·p logic ownership Ä‘Ã£ cÃ³ trong `BabyAccessPolicy`, dá»… lá»‡ch pha |
| C | Bá»• sung method má»›i `canManage(BabyProfile, callerId)` vÃ o `BabyAccessPolicy` hiá»‡n cÃ³ â€” strict ownership only (khÃ´ng check care group) | Má»™t class duy nháº¥t cho toÃ n bá»™ authorization logic cá»§a bounded context `baby`; thay Ä‘á»•i additive, KHÃ”NG sá»­a `canView()` hiá»‡n cÃ³ (khÃ´ng breaking UC192/UC194); rÃµ rÃ ng phÃ¢n biá»‡t "quyá»n xem" vs "quyá»n sá»­a" | ThÃªm 1 method vÃ o class Ä‘Ã£ Approved â€” cáº§n review ká»¹ Ä‘á»ƒ khÃ´ng phÃ¡ vá»¡ há»£p Ä‘á»“ng cÅ© |

#### Quyáº¿t Ä‘á»‹nh (Decision)

Chá»n **PhÆ°Æ¡ng Ã¡n C**. Bá»• sung method má»›i vÃ o `BabyAccessPolicy` (additive, khÃ´ng sá»­a `canView()` hiá»‡n cÃ³):

```java
// Bá»• sung vÃ o com.carebridge.backend.baby.policy.BabyAccessPolicy (file Ä‘Ã£ tá»“n táº¡i tá»« UC192)
/**
 * Returns true CHá»ˆ KHI caller lÃ  account owner cá»§a baby profile â€” strict ownership,
 * KHÃ”NG cháº¥p nháº­n care group member dÃ¹ ACCEPTED. DÃ¹ng cho cÃ¡c thao tÃ¡c MUTATION
 * (update/delete) trÃªn dá»¯ liá»‡u do Mother tá»± ghi nháº­n â€” khÃ¡c vá»›i canView() vá»‘n cho phÃ©p
 * cáº£ care group member xem. ADR-BABY-007.
 */
public boolean canManage(BabyProfile profile, UUID callerId) {
    return profile.getOwnerUserId().equals(callerId);
}
```

`DevelopmentMilestoneServiceImpl` gá»i `babyAccessPolicy.canManage(profile, callerId)` (KHÃ”NG gá»i `canView()`) cho cáº£ `updateMilestone()` vÃ  `deleteMilestone()`.

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:**
- Care group member (ká»ƒ cáº£ ACCEPTED) khÃ´ng thá»ƒ sá»­a/xoÃ¡ milestone cá»§a Mother khÃ¡c â€” Ä‘Ãºng nguyÃªn táº¯c least-privilege.
- Má»™t class `BabyAccessPolicy` duy nháº¥t chá»©a toÃ n bá»™ authorization logic â€” khÃ´ng phÃ¢n máº£nh.
- KhÃ´ng breaking change cho UC192/UC194 (chá»‰ thÃªm method má»›i).

**TiÃªu cá»±c / Trade-offs:**
- UC194's `ADR-BABY-004` pattern (reuse `canView()`) KHÃ”NG Ã¡p dá»¥ng trá»±c tiáº¿p cho UC196/UC197 â€” cáº§n lÆ°u Ã½ khi review Ä‘á»ƒ trÃ¡nh nháº§m láº«n giá»¯a 2 pattern (view vs manage) trong cÃ¹ng bounded context. Ghi chÃº rÃµ trong Constraint Block Â§17.

**Compliance Impact:**
- Cá»§ng cá»‘ BR-RBAC (OWASP A01:2021 â€” Broken Access Control mitigation) báº±ng cÃ¡ch thu háº¹p Ä‘Ãºng pháº¡m vi quyá»n ghi theo SRS Primary Actor.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|------------------|
| Latency (p99) | PATCH response | `< 300ms` | k6 load test | â€” |
| Availability | Uptime | `99.9%` | Uptime monitor | â€” |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Consistency | `milestone_status` vÃ  `record_status` Ä‘á»™c láº­p tuyá»‡t Ä‘á»‘i | 100% â€” khÃ´ng láº§n update nÃ o ghi chÃ©o cá»™t | Unit test disambiguation (Â§13) | ADR-BABY-006 |
| Consistency | `baby_id` FK luÃ´n resolve Ä‘Æ°á»£c `BabyProfile` | 100% | FK constraint `development_milestones_baby_id_fkey` | â€” |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Access control | Strict ownership guard (khÃ´ng care group) | 100% requests kiá»ƒm tra qua `canManage()` | Unit + security test | BR-RBAC, ADR-BABY-007 |
| Encryption in transit | TLS | TLS 1.3+ | SSL Labs scan | â€” |

### 4.4. Scalability & Capacity Planning

Táº£i dá»± kiáº¿n tháº¥p: má»—i baby thÆ°á»ng cÃ³ < 50 milestone record trong 2 nÄƒm Ä‘áº§u Ä‘á»i, update táº§n suáº¥t "Regular" theo SRS (khÃ´ng pháº£i "Frequent"). Endpoint single-row PATCH theo PK â€” khÃ´ng cáº§n pagination/caching riÃªng.

---

## 5. Static Modeling (MÃ´ hÃ¬nh TÄ©nh)

### 5.1. Class Diagram (PlantUML)

```plantuml
@startuml UpdateDevelopmentMilestone_ClassDiagram
skinparam classAttributeIconSize 0
skinparam backgroundColor #FAFAFA

class DevelopmentMilestone {
  + id: UUID
  + babyId: UUID
  + milestoneType: String
  + achievedDate: LocalDate
  + note: String
  + sourceType: String
  + recordedBy: UUID
  + milestoneStatus: MilestoneAchievementStatus
  + recordStatus: MilestoneRecordStatus
  + createdAt: Instant
  + updatedAt: Instant
}

enum MilestoneAchievementStatus {
  PENDING
  ACHIEVED
  DELAYED
}

enum MilestoneRecordStatus {
  ACTIVE
  DELETED
}

interface IDevelopmentMilestoneService {
  + updateMilestone(milestoneId: UUID, request: UpdateDevelopmentMilestoneRequest, callerId: UUID): DevelopmentMilestoneDetailResponse
}

class DevelopmentMilestoneServiceImpl implements IDevelopmentMilestoneService {
  - milestoneRepository: DevelopmentMilestoneRepository
  - babyProfileRepository: BabyProfileRepository
  - babyAccessPolicy: BabyAccessPolicy
  - auditService: AuditService
  + updateMilestone(milestoneId, request, callerId): DevelopmentMilestoneDetailResponse
}

class BabyAccessPolicy {
  + canView(profile: BabyProfile, callerId: UUID): boolean
  + canManage(profile: BabyProfile, callerId: UUID): boolean
}

interface DevelopmentMilestoneRepository {
  + findById(id: UUID): Optional<DevelopmentMilestone>
  + save(entity: DevelopmentMilestone): DevelopmentMilestone
}

DevelopmentMilestoneServiceImpl --> DevelopmentMilestoneRepository : uses
DevelopmentMilestoneServiceImpl --> "com.carebridge.backend.baby.repository.BabyProfileRepository" : reuse (UC192)
DevelopmentMilestoneServiceImpl --> BabyAccessPolicy : uses canManage() [NEW]
DevelopmentMilestone "many" --> "1" "com.carebridge.backend.baby.entity.BabyProfile" : baby_id FK
DevelopmentMilestone *-- MilestoneAchievementStatus
DevelopmentMilestone *-- MilestoneRecordStatus

@enduml
```

### 5.2. Data Structure (Flyway SQL Migration)

> **CareBridge rule:** `V1__init_schema.sql` lÃ  baseline oracle. `development_milestones` Ä‘Ã£ tá»“n táº¡i (dÃ²ng 635-645) nhÆ°ng KHÃ”NG cÃ³ báº¥t ká»³ cá»™t status nÃ o â†’ migration má»›i báº¯t buá»™c.

Táº¡o file: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260707120000__add_development_milestone_status_columns.sql`

```sql
-- UC-196 Update Development Milestone / UC-197 Delete Development Milestone
-- Xem ADR-BABY-006 (UC196 TDS Â§3) cho lÃ½ do tÃ¡ch 2 cá»™t Ä‘á»™c láº­p.
ALTER TABLE public.development_milestones
    ADD COLUMN IF NOT EXISTS milestone_status VARCHAR(20) NOT NULL DEFAULT 'ACHIEVED';

ALTER TABLE public.development_milestones
    ADD COLUMN IF NOT EXISTS record_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_development_milestones_record_status
    ON public.development_milestones USING btree (record_status);
```

> **Quy táº¯c Ä‘áº·t tÃªn:** snake_case cho SQL DDL â€” nháº¥t quÃ¡n vá»›i toÃ n bá»™ `V1__init_schema.sql`.
> **Migration version:** `V20260707120000` â€” theo dáº£i version Ä‘Æ°á»£c chá»‰ Ä‘á»‹nh cho batch nÃ y (`V20260707120000`+, trÃ¡nh dáº£i `090000/100000/110000/130000` Ä‘Ã£ dÃ¹ng cho batch khÃ¡c).

---

## 6. Dynamic Modeling (MÃ´ hÃ¬nh Äá»™ng)

### 6.1. Sequence Diagram â€” Happy Path (PlantUML)

```plantuml
@startuml UpdateDevelopmentMilestone_HappyPath
skinparam backgroundColor #FAFAFA
actor "Mother (owner)" as Client
participant "DevelopmentMilestoneController" as Controller
participant "DevelopmentMilestoneServiceImpl" as Service
participant "DevelopmentMilestoneRepository" as MRepo
participant "BabyProfileRepository" as PRepo
participant "BabyAccessPolicy" as Policy
participant "AuditService" as Audit
database "PostgreSQL" as DB

Client -> Controller : PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}\n{achievedDate?, note?, status?}
activate Controller
Controller -> Controller : SecurityUtils.requireCurrentUserId(principal)
Controller -> Service : updateMilestone(milestoneId, request, callerId)
activate Service

Service -> MRepo : findById(milestoneId)
MRepo -> DB : SELECT * FROM development_milestones WHERE milestone_id=?
DB --> MRepo : DevelopmentMilestone row
MRepo --> Service : Optional<DevelopmentMilestone>

Service -> Service : verify recordStatus == ACTIVE (náº¿u DELETED -> 404 MILESTONE-001)

Service -> PRepo : findById(milestone.getBabyId())
PRepo -> DB : SELECT * FROM baby_profiles WHERE baby_id=?
DB --> PRepo : BabyProfile row
PRepo --> Service : Optional<BabyProfile>

Service -> Policy : canManage(profile, callerId)
Policy --> Service : true

Service -> Service : validate request (>=1 field present;\nif status=ACHIEVED requires achievedDate)
Service -> Service : apply partial update ONLY to\nachievedDate/note/milestoneStatus\n(recordStatus untouched â€” ADR-BABY-006)
Service -> MRepo : save(milestone)
MRepo -> DB : UPDATE development_milestones SET ... updated_at=now()
DB --> MRepo : updated row
MRepo --> Service : DevelopmentMilestone

Service -> Audit : log(DEVELOPMENT_MILESTONE_UPDATED, callerId, milestoneId)
Service --> Controller : DevelopmentMilestoneDetailResponse
deactivate Service
Controller --> Client : 200 OK
deactivate Controller
@enduml
```

### 6.2. Sequence Diagram â€” Error Path (PlantUML)

```plantuml
@startuml UpdateDevelopmentMilestone_ErrorPath
skinparam backgroundColor #FAFAFA
actor "Care Group Member (ACCEPTED, non-owner)" as Client
participant "DevelopmentMilestoneController" as Controller
participant "DevelopmentMilestoneServiceImpl" as Service
participant "DevelopmentMilestoneRepository" as MRepo
participant "BabyProfileRepository" as PRepo
participant "BabyAccessPolicy" as Policy

Client -> Controller : PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}
activate Controller
Controller -> Service : updateMilestone(milestoneId, request, callerId)
activate Service
Service -> MRepo : findById(milestoneId)
MRepo --> Service : Optional<DevelopmentMilestone> (present, recordStatus=ACTIVE)
Service -> PRepo : findById(milestone.getBabyId())
PRepo --> Service : Optional<BabyProfile> (present, owner = OTHER user)
Service -> Policy : canManage(profile, callerId)
Policy --> Service : false
Service -> Service : throw BusinessException(403, "MILESTONE-002")
deactivate Service
Controller --> Client : 403 Forbidden {code: MILESTONE-002}
deactivate Controller

note over Service
  Alternative: milestoneId not found -> BusinessException(404, "MILESTONE-001")
  Alternative: recordStatus == DELETED (post-UC197) -> BusinessException(404, "MILESTONE-001")
  Alternative: empty request body -> BusinessException(400, "MILESTONE-003")
  Alternative: status=ACHIEVED but achievedDate null (existing AND new) -> BusinessException(400, "MILESTONE-003")
  IMPORTANT: canManage() dÃ¹ng strict ownership â€” care group member ACCEPTED
  váº«n bá»‹ 403 á»Ÿ Ä‘Ã¢y, khÃ¡c vá»›i canView() á»Ÿ UC192/UC194 (ADR-BABY-007)
end note
@enduml
```

### 6.3. State Machine â€” `milestoneStatus` (achievement progress)

```plantuml
@startuml UpdateDevelopmentMilestone_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> PENDING : milestone táº¡o má»›i, chÆ°a cÃ³ achievedDate

PENDING --> ACHIEVED : Mother set achievedDate + status=ACHIEVED (UC-196)
PENDING --> DELAYED  : Mother set status=DELAYED (UC-196)
DELAYED --> ACHIEVED : Mother set achievedDate + status=ACHIEVED (UC-196)
ACHIEVED --> PENDING : Mother sá»­a láº¡i náº¿u ghi nháº§m (UC-196, edge case)

note right of ACHIEVED
  Invariant: milestoneStatus KHÃ”NG BAO GIá»œ Ä‘Æ°á»£c set
  bá»Ÿi UC-197 (Delete) â€” chá»‰ UC-196 Ä‘Æ°á»£c ghi field nÃ y.
  ADR-BABY-006.
end note

@enduml
```

> **âš ï¸ Invariant báº¥t biáº¿n:** `recordStatus` (ACTIVE/DELETED) lÃ  má»™t FSM hoÃ n toÃ n tÃ¡ch biá»‡t, chá»‰ chuyá»ƒn `ACTIVE â†’ DELETED` má»™t chiá»u, vÃ  chá»‰ Ä‘Æ°á»£c thao tÃ¡c bá»Ÿi UC-197 (xem UC197 TDS Â§6.3). KhÃ´ng cÃ³ transition nÃ o giá»¯a `milestoneStatus` vÃ  `recordStatus`.

---

## 7. Domain Event Catalog

### 7.1. Events Published (PhÃ¡t ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| `DevelopmentMilestoneUpdated` | Sau khi `updateMilestone()` commit thÃ nh cÃ´ng | `DevelopmentMilestoneServiceImpl` | `audit` | `DevelopmentMilestoneUpdatedEvent.java` | No (Ä‘á»“ng bá»™, nháº¥t quÃ¡n vá»›i `AuditService.log()` pattern hiá»‡n cÃ³ trong `BabyServiceImpl`) |

### 7.2. Events Consumed (TiÃªu thá»¥)

KhÃ´ng cÃ³ â€” module nÃ y khÃ´ng tiÃªu thá»¥ event nÃ o.

### 7.3. Payload Schema

```java
// DevelopmentMilestoneUpdatedEvent.java
public record DevelopmentMilestoneUpdatedEvent(
    UUID    eventId,
    String  eventType,       // "DevelopmentMilestoneUpdated"
    Instant occurredAt,
    String  version,         // "1.0"
    Payload payload,
    Metadata metadata
) {
    public record Payload(
        UUID   milestoneId,
        UUID   babyId,
        String oldMilestoneStatus,  // nullable náº¿u status khÃ´ng Ä‘á»•i
        String newMilestoneStatus,  // nullable náº¿u status khÃ´ng Ä‘á»•i
        UUID   updatedByUserId
    ) {}

    public record Metadata(
        UUID   correlationId,
        String causedBy
    ) {}
}
```

> **Ghi chÃº triá»ƒn khai:** á»ž láº§n triá»ƒn khai Ä‘áº§u, event nÃ y Ä‘Æ°á»£c emit thÃ´ng qua `AuditService.log(AuditAction.DEVELOPMENT_MILESTONE_UPDATED, ...)` (cáº§n bá»• sung giÃ¡ trá»‹ enum má»›i `DEVELOPMENT_MILESTONE_UPDATED` vÃ o `AuditAction.java` hiá»‡n cÃ³ â€” additive change, khÃ´ng sá»­a giÃ¡ trá»‹ cÅ©). Viá»‡c phÃ¡t Spring `ApplicationEvent` riÃªng lÃ  **Open item** â€” xem Open Items cuá»‘i tÃ i liá»‡u.

---

## 8. Interface Specification (Äáº·c táº£ Giao diá»‡n)

### 8.1. Service Interface

```java
// UpdateDevelopmentMilestoneRequest.java â€” Input DTO
// @version 1.0
public class UpdateDevelopmentMilestoneRequest {
    @PastOrPresent
    private LocalDate achievedDate;    // optional â€” null náº¿u khÃ´ng Ä‘á»•i
    @Size(max = 2000)
    private String note;               // optional â€” null náº¿u khÃ´ng Ä‘á»•i
    private MilestoneAchievementStatus status; // optional â€” PENDING/ACHIEVED/DELAYED, null náº¿u khÃ´ng Ä‘á»•i
    // getters/setters; @AssertTrue custom validator: at-least-one-field-present
}

// DevelopmentMilestoneDetailResponse.java â€” Output DTO
public class DevelopmentMilestoneDetailResponse {
    private UUID id;
    private UUID babyId;
    private String milestoneType;
    private LocalDate achievedDate;
    private String note;
    private String sourceType;
    private UUID recordedBy;
    private String status;       // maps to entity.milestoneStatus â€” KHÃ”NG lá»™ recordStatus ra response
    private Instant createdAt;
    private Instant updatedAt;
}

// IDevelopmentMilestoneService.java â€” Service Contract
// @version 1.0
public interface IDevelopmentMilestoneService {
    /**
     * @throws BusinessException (MILESTONE-001/404) khi milestoneId khÃ´ng tá»“n táº¡i,
     *         HOáº¶C recordStatus == DELETED (post-UC197, treat as not-found â€” ADR-BABY-006)
     * @throws BusinessException (MILESTONE-002/403) khi caller khÃ´ng pháº£i account owner
     *         (canManage() strict ownership â€” ADR-BABY-007)
     * @throws BusinessException (MILESTONE-003/400) khi request rá»—ng (0 field) HOáº¶C
     *         status=ACHIEVED mÃ  achievedDate (cÅ© láº«n má»›i) Ä‘á»u null
     */
    DevelopmentMilestoneDetailResponse updateMilestone(
            UUID milestoneId, UpdateDevelopmentMilestoneRequest request, UUID callerId);
}
```

### 8.2. Entity & Repository Interface

```java
// DevelopmentMilestone.java â€” new entity, package com.carebridge.backend.baby.entity
// @version 1.0
@Entity
@Table(name = "development_milestones")
public class DevelopmentMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "milestone_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "baby_id", nullable = false)
    private UUID babyId;

    @Column(name = "milestone_type", nullable = false, length = 80)
    private String milestoneType;   // known values per UC37 ADR-BABY-007-001: ROLLING, CRAWLING, WALKING, SPEAKING, TEETHING, WEANING, FIRST_SMILE, SITTING, STANDING (validated BABY-063 on write); String, NOT @Enumerated â€” update path stays permissive, same rationale as UC194 OI-1

    @Column(name = "achieved_date")
    private LocalDate achievedDate;

    @Column(name = "note")
    private String note;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    // ADR-BABY-006: achievement progress â€” CHá»ˆ mutate bá»Ÿi UC-196
    @Enumerated(EnumType.STRING)
    @Column(name = "milestone_status", nullable = false, length = 20)
    private MilestoneAchievementStatus milestoneStatus;

    // ADR-BABY-006: soft-delete lifecycle â€” CHá»ˆ mutate bá»Ÿi UC-197
    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 20)
    private MilestoneRecordStatus recordStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

// MilestoneAchievementStatus.java
public enum MilestoneAchievementStatus { PENDING, ACHIEVED, DELAYED }

// MilestoneRecordStatus.java
public enum MilestoneRecordStatus { ACTIVE, DELETED }

// DevelopmentMilestoneRepository.java
// @version 1.0
public interface DevelopmentMilestoneRepository extends JpaRepository<DevelopmentMilestone, UUID> {
    // findById() káº¿ thá»«a tá»« JpaRepository lÃ  Ä‘á»§ cho UC196/UC197 (single-row lookup theo PK).
}
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `PATCH` | `/api/v1/babies/{babyId}/milestones/{milestoneId}` | JWT Bearer | `ROLE_MOTHER` | 60/min | Yes |

> **Path design note:** Nháº¥t quÃ¡n vá»›i `BabyController`'s `/api/v1/babies` base path (UC192) vÃ  `BabyDailyLogController`'s nested pattern (UC194). `babyId` trong path CHá»ˆ dÃ¹ng Ä‘á»ƒ routing â€” service KHÃ”NG tin `babyId` tá»« path cho authorization; ownership check luÃ´n dá»±a trÃªn `milestone.getBabyId()` Ä‘á»c tá»« DB.

### 9.2. Request / Response Schemas

#### `PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}`

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body (partial â€” má»i field optional, tá»‘i thiá»ƒu 1 field):**
```json
{
  "achievedDate": "2026-07-01",
  "note": "BÃ© Ä‘Ã£ biáº¿t bÃ² thÃ nh tháº¡o",
  "status": "ACHIEVED"
}
```

**Response â€” 200 OK:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "babyId": "660e8400-e29b-41d4-a716-446655440001",
    "milestoneType": "crawling",
    "achievedDate": "2026-07-01",
    "note": "BÃ© Ä‘Ã£ biáº¿t bÃ² thÃ nh tháº¡o",
    "sourceType": "manual",
    "recordedBy": "770e8400-e29b-41d4-a716-446655440002",
    "status": "ACHIEVED",
    "createdAt": "2026-06-20T08:00:00.000Z",
    "updatedAt": "2026-07-03T10:15:00.000Z"
  }
}
```

**Response â€” 400 Bad Request:**
```json
{
  "error": { "code": "MILESTONE-003", "message": "At least one field (achievedDate, note, status) must be provided, and status=ACHIEVED requires achievedDate" }
}
```

**Response â€” 403 Forbidden:**
```json
{
  "error": { "code": "MILESTONE-002", "message": "Access denied to update this development milestone" }
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

> Prefix `MILESTONE-` dÃ¹ng riÃªng cho `development_milestones` module â€” trÃ¡nh Ä‘á»¥ng `BABY-xxx` (UC192) vÃ  `DAILYLOG-xxx` (UC194).

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `MILESTONE-001` | 404 | Development milestone not found | KhÃ´ng tÃ¬m tháº¥y má»‘c phÃ¡t triá»ƒn | `milestoneId` khÃ´ng tá»“n táº¡i HOáº¶C `recordStatus = DELETED` (post-UC197) HOáº¶C `baby_id` FK khÃ´ng resolve Ä‘Æ°á»£c `BabyProfile` (orphan, defense-in-depth) |
| `MILESTONE-002` | 403 | Access denied to update this development milestone | KhÃ´ng Ä‘á»§ quyá»n cáº­p nháº­t má»‘c phÃ¡t triá»ƒn | Caller KHÃ”NG pháº£i account owner (`canManage()` false â€” strict, ká»ƒ cáº£ care group ACCEPTED member) |
| `MILESTONE-003` | 400 | Invalid update request | YÃªu cáº§u cáº­p nháº­t khÃ´ng há»£p lá»‡ | Request rá»—ng (0 field) HOáº¶C `status=ACHIEVED` mÃ  `achievedDate` (cÅ© vÃ  má»›i) Ä‘á»u null HOáº¶C `status` khÃ´ng thuá»™c enum há»£p lá»‡ |
| `MILESTONE-004` | 500 | Internal error | Lá»—i há»‡ thá»‘ng | Unexpected DB error |

---

## 11. Quy trÃ¬nh Triá»ƒn khai (Step-by-Step)

### 11.1. Prerequisites
- [ ] TDS nÃ y (UC196) vÃ  TDS UC197 (companion) Ä‘á»u Approved
- [ ] `BabyProfileRepository`, `BabyAccessPolicy` (UC192) Ä‘Ã£ cÃ³ sáºµn trong `main`

### 11.2. Pre-Migration Checklist
- [ ] Backup DB dev/staging trÆ°á»›c khi cháº¡y `V20260707120000`
- [ ] XÃ¡c nháº­n UC197 KHÃ”NG cháº¡y migration riÃªng trÃ¹ng cá»™t (UC197 tÃ¡i sá»­ dá»¥ng chung migration nÃ y â€” xem UC197 TDS Â§5.2)

### 11.3. Implementation Steps

#### Cháº·ng 1 â€” Migration
Táº¡o `V20260707120000__add_development_milestone_status_columns.sql` (Â§5.2). Cháº¡y `./mvnw flyway:migrate`.

#### Cháº·ng 2 â€” Entity + Enums + Repository
Táº¡o `DevelopmentMilestone.java`, `MilestoneAchievementStatus.java`, `MilestoneRecordStatus.java`, `DevelopmentMilestoneRepository.java` trong `com.carebridge.backend.baby.{entity,repository}`.

#### Cháº·ng 3 â€” Policy extension
Bá»• sung method `canManage(BabyProfile, UUID)` vÃ o `BabyAccessPolicy.java` hiá»‡n cÃ³ (KHÃ”NG sá»­a `canView()`).

#### Cháº·ng 4 â€” Service + DTO
Táº¡o `IDevelopmentMilestoneService.java`, `DevelopmentMilestoneServiceImpl.java`, `UpdateDevelopmentMilestoneRequest.java`, `DevelopmentMilestoneDetailResponse.java` trong `com.carebridge.backend.baby.{service, service.impl, dto}`.

#### Cháº·ng 5 â€” Controller
Táº¡o `DevelopmentMilestoneController.java` (`@RestController`, base path `/api/v1/babies/{babyId}/milestones`), method `updateMilestone` (`@PatchMapping("/{milestoneId}")`).

#### Cháº·ng 6 â€” Audit
Bá»• sung `DEVELOPMENT_MILESTONE_UPDATED` vÃ o `AuditAction.java` (additive).

#### Cháº·ng 7 â€” Verification sau deploy
```bash
curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACHIEVED", "achievedDate": "2026-07-01"}'
# Expected: 200 with status=ACHIEVED
```

### 11.4. Deployment Checklist
- [ ] `./mvnw test` xanh
- [ ] Disambiguation test PASS: update status KHÃ”NG Ä‘á»•i `record_status` trong DB
- [ ] IDOR test (non-owner, ká»ƒ cáº£ care group ACCEPTED â†’ 403) pass

---

## 12. Rollback & Incident Runbook

### 12.1. Äiá»u kiá»‡n kÃ­ch hoáº¡t Rollback

| Äiá»u kiá»‡n | NgÆ°á»¡ng | NgÆ°á»i quyáº¿t Ä‘á»‹nh |
|-----------|--------|------------------|
| Error rate tÄƒng Ä‘á»™t biáº¿n | > 5% trong 5 phÃºt | On-call Engineer |
| PhÃ¡t hiá»‡n `milestone_status` vÃ  `record_status` bá»‹ ghi chÃ©o (data corruption) | Báº¥t ká»³ case nÃ o | Tech Lead + DPO |

### 12.2. Rollback Procedure

```bash
# Rollback migration (dev/staging only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE development_milestones DROP COLUMN IF EXISTS milestone_status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE development_milestones DROP COLUMN IF EXISTS record_status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260707120000';"

kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api
curl -X GET https://[host]/api/v1/health
```

> âš ï¸ **Cáº£nh bÃ¡o:** KHÃ”NG cháº¡y `DROP COLUMN` trÃªn production náº¿u UC197 Ä‘Ã£ deploy song song vÃ  cÃ³ dá»¯ liá»‡u `record_status = DELETED` â€” sáº½ máº¥t thÃ´ng tin xoÃ¡ má»m. Coordinate rollback giá»¯a UC196/UC197 trÆ°á»›c khi thá»±c thi.

### 12.3. Notification Protocol

| Thá»i Ä‘iá»ƒm | NgÆ°á»i nháº­n | KÃªnh |
|-----------|------------|------|
| Ngay khi phÃ¡t hiá»‡n data corruption giá»¯a 2 cá»™t status | Tech Lead + DPO | Slack `#incident` + Email |

---

## 13. Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t

> **Policy (EDS v2.0):** Má»i test scenario dÃ¹ng dá»¯ liá»‡u `SYNTHETIC`.

```gherkin
Feature: Update Development Milestone
  Background:
    Given test data classification: SYNTHETIC
    And MOTHER-001 lÃ  owner cá»§a BABY-001
    And MILESTONE-001 thuá»™c BABY-001 vá»›i milestoneStatus=PENDING, recordStatus=ACTIVE

  Scenario: Owner cáº­p nháº­t status â†’ ACHIEVED (kÃ¨m achievedDate) â†’ 200
    When updateMilestone(MILESTONE-001, {status: ACHIEVED, achievedDate: 2026-07-01}, MOTHER-001)
    Then response 200 vá»›i status=ACHIEVED, achievedDate=2026-07-01

  Scenario: Owner cáº­p nháº­t chá»‰ note â†’ 200
    When updateMilestone(MILESTONE-001, {note: "cáº­p nháº­t ghi chÃº"}, MOTHER-001)
    Then response 200, milestoneStatus khÃ´ng Ä‘á»•i

  Scenario: Care group member (ACCEPTED, non-owner) â†’ 403
    Given MOTHER-002 lÃ  ACCEPTED member trong care group cá»§a BABY-001 (khÃ´ng pháº£i owner)
    When updateMilestone(MILESTONE-001, {note: "x"}, MOTHER-002)
    Then throws BusinessException MILESTONE-002 (403)

  Scenario: Non-owner, non-member â†’ 403
    Given MOTHER-003 KHÃ”NG liÃªn quan BABY-001
    When updateMilestone(MILESTONE-001, {note: "x"}, MOTHER-003)
    Then throws BusinessException MILESTONE-002 (403)

  Scenario: Milestone khÃ´ng tá»“n táº¡i â†’ 404
    When updateMilestone(NONEXISTENT, {note: "x"}, MOTHER-001)
    Then throws BusinessException MILESTONE-001 (404)

  Scenario: Milestone Ä‘Ã£ soft-deleted (post-UC197) â†’ 404
    Given MILESTONE-002 thuá»™c BABY-001 vá»›i recordStatus=DELETED
    When updateMilestone(MILESTONE-002, {note: "x"}, MOTHER-001)
    Then throws BusinessException MILESTONE-001 (404)

  Scenario: Request rá»—ng â†’ 400
    When updateMilestone(MILESTONE-001, {}, MOTHER-001)
    Then throws BusinessException MILESTONE-003 (400)

  Scenario: status=ACHIEVED khÃ´ng cÃ³ achievedDate (cÅ© láº«n má»›i Ä‘á»u null) â†’ 400
    Given MILESTONE-003 thuá»™c BABY-001 vá»›i achievedDate=null, milestoneStatus=PENDING
    When updateMilestone(MILESTONE-003, {status: ACHIEVED}, MOTHER-001)
    Then throws BusinessException MILESTONE-003 (400)

  Scenario: [DISAMBIGUATION â€” CRITICAL] Update status KHÃ”NG Ä‘á»¥ng Ä‘áº¿n recordStatus
    When updateMilestone(MILESTONE-001, {status: DELAYED}, MOTHER-001)
    Then DB row cÃ³ milestone_status=DELAYED VÃ€ record_status váº«n=ACTIVE (khÃ´ng Ä‘á»•i)
```

---

## 14. PhÆ°Æ¡ng phÃ¡p XÃ¡c minh

### 14.1. Database Inspection

```sql
-- Verify milestone exists and belongs to expected baby
SELECT milestone_id, baby_id, milestone_type, achieved_date, note, milestone_status, record_status
FROM development_milestones WHERE milestone_id = '[milestoneId]';

-- Verify ownership chain
SELECT bp.owner_user_id
FROM development_milestones dm
JOIN baby_profiles bp ON bp.baby_id = dm.baby_id
WHERE dm.milestone_id = '[milestoneId]';

-- [DISAMBIGUATION CHECK] Verify record_status untouched after status update
SELECT milestone_status, record_status, updated_at
FROM development_milestones WHERE milestone_id = '[milestoneId]';
-- Expected: record_status = 'ACTIVE' (unless UC197 was separately invoked)
```

### 14.2. Access Policy Verification

```bash
curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [OWNER_JWT]" -H "Content-Type: application/json" \
  -d '{"status":"ACHIEVED","achievedDate":"2026-07-01"}'
# Expected: 200

curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [CARE_GROUP_MEMBER_JWT]" -H "Content-Type: application/json" \
  -d '{"note":"x"}'
# Expected: 403 (strict ownership â€” different from UC192/UC194 canView() behavior)
```

---

## 15. Máº«u thá»­ thá»±c táº¿ (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]" -H "Content-Type: application/json" \
  -d '{"status":"ACHIEVED","achievedDate":"2026-07-01","note":"BÃ© Ä‘Ã£ biáº¿t bÃ²"}'
# Expected: 200 {id, babyId, milestoneType, achievedDate, status: "ACHIEVED", ...}
```

### 15.2. Error Paths

```bash
# Empty body -> 400
curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]" -H "Content-Type: application/json" -d '{}'

# Non-existent milestone -> 404
curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/non-existent-uuid \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]" -H "Content-Type: application/json" -d '{"note":"x"}'

# Care group member (ACCEPTED, non-owner) -> 403
curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId] \
  -H "Authorization: Bearer [CARE_GROUP_MEMBER_JWT]" -H "Content-Type: application/json" -d '{"note":"x"}'

# No JWT -> 401
curl -X PATCH https://[host]/api/v1/babies/[babyId]/milestones/[milestoneId]
```

---

## 16. Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix)

| Endpoint | `GUEST` | `MOTHER (owner)` | `MOTHER (care member, ACCEPTED)` | `EXPERT` | `ADMIN` |
|----------|---------|-------------------|-----------------------------------|----------|---------|
| `PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}` | âŒ (401) | âœ… | âŒ (403) | âŒ (403) | âœ… All |

**ChÃº thÃ­ch:**
- Owner: `baby_profiles.owner_user_id` == JWT subject (via `development_milestones.baby_id` FK) â€” kiá»ƒm tra báº±ng `canManage()` **strict** (ADR-BABY-007).
- Care member (ká»ƒ cáº£ ACCEPTED): **KHÃ”NG** Ä‘Æ°á»£c sá»­a â€” khÃ¡c vá»›i UC192/UC194 vá»‘n cho phÃ©p xem. ÄÃ¢y lÃ  khÃ¡c biá»‡t cá»‘ Ã½, cÃ³ ADR.
- Expert: khÃ´ng cÃ³ quyá»n, ngoÃ i pháº¡m vi UC.

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source | Last Verified |
|---|-----------|--------|---------------|
| C1 | `DevelopmentMilestoneServiceImpl.updateMilestone()` CHá»ˆ Ä‘Æ°á»£c ghi vÃ o field `milestoneStatus` â€” TUYá»†T Äá»I KHÃ”NG Ä‘Æ°á»£c set `recordStatus` | ADR-BABY-006 | 2026-07-03 |
| C2 | Authorization PHáº¢I dÃ¹ng `BabyAccessPolicy.canManage()` (strict ownership) â€” KHÃ”NG dÃ¹ng `canView()` (vá»‘n cho phÃ©p care group member) | ADR-BABY-007 | 2026-07-03 |
| C3 | Náº¿u `recordStatus == DELETED`, tráº£ 404 `MILESTONE-001` â€” KHÃ”NG tráº£ 403, KHÃ”NG cho phÃ©p "há»“i sinh" record Ä‘Ã£ xoÃ¡ má»m qua update | ADR-BABY-006 | 2026-07-03 |
| C4 | `babyId` trong URL path CHá»ˆ dÃ¹ng Ä‘á»ƒ routing â€” authorization luÃ´n dá»±a trÃªn `milestone.getBabyId()` Ä‘á»c tá»« DB | BR-RBAC | 2026-07-03 |
| C5 | Request rá»—ng (0 field) hoáº·c `status=ACHIEVED` thiáº¿u `achievedDate` (cáº£ cÅ© vÃ  má»›i) PHáº¢I reject 400 `MILESTONE-003` | SRS Â§3.3.12.5 | 2026-07-03 |

### 17.2 Constraint Injection Block

```
[CONSTRAINT BLOCK â€” Module: UpdateDevelopmentMilestone (CB-BABY-IMP-004)]
1. updateMilestone() CHá»ˆ ghi vÃ o entity field milestoneStatus â€” KHÃ”NG BAO GIá»œ set recordStatus â€” ADR-BABY-006
2. Authorization dÃ¹ng babyAccessPolicy.canManage(profile, callerId) â€” strict ownership, KHÃ”NG dÃ¹ng canView() â€” ADR-BABY-007
3. recordStatus == DELETED -> 404 MILESTONE-001 (record coi nhÆ° khÃ´ng tá»“n táº¡i, khÃ´ng "há»“i sinh" Ä‘Æ°á»£c qua update)
4. babyId trong URL path KHÃ”NG dÃ¹ng cho authorization â€” chá»‰ dÃ¹ng Ä‘á»ƒ route; ownership dá»±a trÃªn dá»¯ liá»‡u Ä‘á»c tá»« DB
5. Request rá»—ng hoáº·c status=ACHIEVED thiáº¿u achievedDate -> 400 MILESTONE-003

[CONTEXT BLOCK]
- Bounded Context: baby (reuse UC192/UC194 package â€” com.carebridge.backend.baby)
- Data Classification: Sensitive-PII
- Error codes: Â§10 Error Codes Table (prefix MILESTONE-, KHÃ”NG trÃ¹ng BABY-xxx/DAILYLOG-xxx)
- Auth matrix: Â§16 Authorization Matrix
- Reused classes: BabyProfileRepository (UC192); BabyAccessPolicy.canView() giá»¯ nguyÃªn, CHá»ˆ thÃªm method canManage() má»›i
- Companion: UC197 Delete Development Milestone dÃ¹ng chung entity/migration, xem UC197 TDS
```

### 17.3 Constraint Quality Checklist

- [x] Má»—i constraint traceable vá» ADR hoáº·c BR cá»¥ thá»ƒ
- [x] KhÃ´ng cÃ³ constraint generic
- [x] Constraint block cÃ³ â‰¥ 3 constraints cá»¥ thá»ƒ

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | HÃ nh Ä‘á»™ng |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code khÃ´ng match constraint C1-C5 | Reject â€” inject láº¡i constraints |
| AP-AI-003 | Implicit Decision | Code dÃ¹ng 1 cá»™t `status` chung thay vÃ¬ 2 cá»™t tÃ¡ch biá»‡t | Reject â€” vi pháº¡m ADR-BABY-006 |
| AP-AI-005 | Hallucinated Contract | Code import class khÃ´ng cÃ³ trong Â§8 | Reject â€” verify contract |

---

## PHá»¤ Lá»¤C

### A. Glossary (Thuáº­t ngá»¯)

| Thuáº­t ngá»¯ | Äá»‹nh nghÄ©a |
|-----------|------------|
| DevelopmentMilestone | Báº£n ghi má»‘c phÃ¡t triá»ƒn cá»§a baby (vd: biáº¿t bÃ², biáº¿t Ä‘i, má»c rÄƒng) |
| `milestoneStatus` (achievement) | Tráº¡ng thÃ¡i tiáº¿n triá»ƒn cá»§a milestone: PENDING/ACHIEVED/DELAYED â€” mutate bá»Ÿi UC-196 |
| `recordStatus` (lifecycle) | Tráº¡ng thÃ¡i vÃ²ng Ä‘á»i record: ACTIVE/DELETED (soft-delete) â€” mutate CHá»ˆ bá»Ÿi UC-197 |
| `canManage()` | Method má»›i trong `BabyAccessPolicy` â€” strict ownership check cho mutation, khÃ¡c `canView()` |

### B. TÃ i liá»‡u tham chiáº¿u

| Document | Path |
|----------|------|
| UC192 TDS (Approved, shipped code reference) | `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` |
| UC194 TDS (companion pattern reference â€” ownership chain, soft-delete precedent) | `04_Implement/UC194_ViewBabyDailyLogDetail/UC194_ViewBabyDailyLogDetail_TDS.md` |
| UC197 TDS (companion â€” soft-delete, cÃ¹ng migration) | `04_Implement/UC197_DeleteDevelopmentMilestone/UC197_DeleteDevelopmentMilestone_TDS.md` |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |
| Schema baseline | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` |

---

## Open Items (chÆ°a resolve â€” cáº§n Tech Lead / Product xÃ¡c nháº­n trÆ°á»›c khi Approve)

| # | Item | MÃ´ táº£ | Äá» xuáº¥t táº¡m thá»i |
|---|------|-------|-------------------|
| OI-1 | ~~`milestone_type` enum vocabulary~~ **RESOLVED (2026-07-03)** | Ban Ä‘áº§u tÆ°á»Ÿng khÃ´ng cÃ³ tÃ i liá»‡u nÃ o Ä‘á»‹nh nghÄ©a vocabulary. RÃ  soÃ¡t láº¡i phÃ¡t hiá»‡n sibling spec `UC37_RecordDevelopmentMilestone` (ADR-BABY-007-001) Ä‘Ã£ Ä‘á»‹nh nghÄ©a cho Ä‘Ãºng cá»™t `development_milestones.milestone_type` nÃ y: `ROLLING, CRAWLING, WALKING, SPEAKING, TEETHING, WEANING, FIRST_SMILE, SITTING, STANDING` (validated qua `BABY-063` á»Ÿ write path). Cá»™t váº«n lÃ  `varchar(80)` khÃ´ng CHECK constraint á»Ÿ DB. | DÃ¹ng `String` (khÃ´ng `@Enumerated`) á»Ÿ entity cho update path â€” nháº¥t quÃ¡n vá»›i UC194 OI-1 (permissive read/update, whitelist enforcement thuá»™c UC37). |
| OI-2 | `DevelopmentMilestoneUpdated` Spring `ApplicationEvent` riÃªng | Hiá»‡n chá»‰ emit qua `AuditService.log()`, chÆ°a cÃ³ event bus riÃªng cho consumer khÃ¡c (vd: timeline aggregator). | KhÃ´ng kÃ­ch hoáº¡t máº·c Ä‘á»‹nh; revisit náº¿u cÃ³ downstream consumer cá»¥ thá»ƒ. |
| OI-3 | `milestoneStatus` default cho record Má»šI (khÃ´ng pháº£i backfill) | DTO validation cÃ³ nÃªn báº¯t buá»™c `status` khi táº¡o má»›i milestone (ngoÃ i pháº¡m vi UC196, thuá»™c UC "Create Development Milestone" chÆ°a cÃ³ trong batch nÃ y)? | NgoÃ i pháº¡m vi UC196/UC197 â€” ghi nháº­n cho future UC "CreateDevelopmentMilestone". |

---

*EDS v2.1 â€” TÃ­ch há»£p CASE 2.0 AI Prompt Constraints (Â§17).*
