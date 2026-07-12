# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification â€” UC-195 Delete Baby Daily Log

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
| 2026-07-03 | AI Agent | Táº¡o tÃ i liá»‡u láº§n Ä‘áº§u cho UC-195 Delete Baby Daily Log â€” extends UC194's `BabyDailyLog*` classes |

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
| **Module Name** | `DeleteBabyDailyLog` |
| **Bounded Context** | `baby` (SAME context as UC192 `BabyController`/`BabyServiceImpl` and UC194 `BabyDailyLogController`/`BabyDailyLogServiceImpl` â€” NOT the empty `babyCare` stub folder) |
| **UC ID** | `UC-195` |
| **SRS Reference** | `3.3.12.4` (`02_Requirements/SRS/3_Functional_Specification.md` lines 4196-4215) |
| **Primary Actor** | `Mother (ROLE_MOTHER)` |
| **Platform** | `Mobile App` |
| **Priority** | `Medium` |
| **Frequency of Use** | `Occasional` (per SRS Table 217, lower than UC194's `Frequent`) |
| **Sprint** | `Sprint 4 â€” Device Sync And Care Edge Cases` |
| **Owner** | `TV2-BÃ¡ch` |
| **Data Classification** | `Sensitive-PII` (infant health/feeding/sleep data) |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SAFETY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy, BabyProfileRepository â€” UC192)`, `UC194 (BabyDailyLog entity, BabyDailyLogRepository, BabyDailyLogServiceImpl, BabyDailyLogController â€” SAME classes, EXTENDED here)`, `auth`, `audit (AuditService, AuditAction)` |
| **Downstream Consumers** | `UC194 View Baby Daily Log Detail (must filter status=DELETED as 404 â€” coupling documented in UC194 TDS Â§5.2)`, `Baby Daily Log List (future UC)` |

**MÃ´ táº£:** Mother xoÃ¡ má»m (soft-delete) má»™t báº£n ghi `baby_daily_logs` do chÃ­nh há» nháº­p. ÄÃ¢y **KHÃ”NG pháº£i greenfield code** â€” UC195 **EXTENDS** cÃ¡c class Ä‘Ã£ Ä‘Æ°á»£c UC194 thiáº¿t káº¿ (`BabyDailyLog` entity, `BabyDailyLogStatus` enum, `BabyDailyLogRepository`, `IBabyDailyLogService`/`BabyDailyLogServiceImpl`, `BabyDailyLogController`) báº±ng cÃ¡ch thÃªm **method má»›i** `deleteBabyDailyLog()` vÃ o cÃ¡c class Ä‘Ã³. UC195 cÅ©ng lÃ  bÃªn chá»‹u trÃ¡ch nhiá»‡m táº¡o migration bá»• sung cá»™t `status` cho `baby_daily_logs` â€” gap Ä‘Ã£ Ä‘Æ°á»£c UC194's TDS (Â§5.2) xÃ¡c Ä‘á»‹nh nhÆ°ng cá»‘ Ã½ Ä‘á»ƒ ngá» cho UC195 xá»­ lÃ½, trÃ¡nh 2 UC cÃ¹ng táº¡o 2 migration trÃ¹ng láº·p cho cÃ¹ng 1 cá»™t.

---

## 2. Ma tráº­n Truy váº¿t (Traceability Matrix)

| Requirement ID | Loáº¡i | MÃ´ táº£ | ThÃ nh pháº§n Code | Compliance Target | ADR liÃªn quan |
|----------------|------|-------|-----------------|-------------------|---------------|
| UC-195 | Use Case | Mother xoÃ¡ má»m 1 baby daily log | `BabyDailyLogController.deleteBabyDailyLog()` (NEW method trÃªn existing controller) | BR-RBAC | ADR-BABY-006 |
| BR-RBAC | Business Rule | Chá»‰ OWNER cá»§a baby profile (khÃ´ng pháº£i care group member) má»›i Ä‘Æ°á»£c xoÃ¡ | `BabyAccessPolicy.canManage()` (NEW method, EXTENDS UC192's existing class) | BR-RBAC | ADR-BABY-007 |
| BR-PRIVACY | Business Rule | Soft-delete (KHÃ”NG physical DELETE) â€” giá»¯ dá»¯ liá»‡u cho retention/audit theo PDPA | `BabyDailyLog.status = DELETED` via `BabyDailyLogRepository.save()` | BR-PRIVACY | ADR-BABY-006 |
| BR-SAFETY | Business Rule | XoÃ¡ dá»¯ liá»‡u sinh hoáº¡t khÃ´ng pháº£i lÃ  "xoÃ¡ báº±ng chá»©ng y táº¿" â€” record váº«n truy váº¿t Ä‘Æ°á»£c qua audit log cho compliance investigation | `AuditService.log(BABY_DAILY_LOG_DELETED, ...)` | BR-SAFETY | ADR-BABY-008 |

---

## 3. Architecture Decision Records (ADR)

### ADR-BABY-006 â€” Soft-Delete Semantics cho `baby_daily_logs` (companion migration cho UC194)

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `TV2-BÃ¡ch, AI Agent` |
| **Date** | `2026-07-03` |
| **Supersedes** | â€” |

#### Bá»‘i cáº£nh (Context)
UC194's TDS (`CB-BABY-IMP-003` Â§5.2) Ä‘Ã£ xÃ¡c nháº­n `baby_daily_logs` (V1__init_schema.sql, dÃ²ng 621-633) KHÃ”NG cÃ³ cá»™t `status`, vÃ  Ä‘Ã£ pre-thiáº¿t káº¿ entity `BabyDailyLog` vá»›i field `status: BabyDailyLogStatus` (enum `ACTIVE`/`DELETED`, nullable cho tá»›i khi migration nÃ y cháº¡y) â€” chÃ­nh lÃ  companion migration Ä‘Ã³. Quyáº¿t Ä‘á»‹nh táº¡i Ä‘Ã¢y: xoÃ¡ váº­t lÃ½ (hard DELETE) hay xoÃ¡ má»m (soft-delete)?

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t (Options Considered)

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | Hard DELETE â€” `DELETE FROM baby_daily_logs WHERE baby_log_id=?` | ÄÆ¡n giáº£n, khÃ´ng cáº§n cá»™t má»›i | Vi pháº¡m PDPA retention/audit requirement cho Sensitive-PII; khÃ´ng thá»ƒ phá»¥c há»“i nháº§m láº«n; phÃ¡ vá»¡ FK integrity náº¿u cÃ³ báº£ng khÃ¡c reference log nÃ y trong tÆ°Æ¡ng lai |
| B | Soft-delete qua cá»™t `status VARCHAR(20) DEFAULT 'ACTIVE'`, giÃ¡ trá»‹ `DELETED` khi xoÃ¡ (giá»‘ng pattern `V20260627100200__add_maternal_metric_status.sql` cho `maternal_health_metrics`, cÅ©ng do TV2-BÃ¡ch sá»Ÿ há»¯u) | Nháº¥t quÃ¡n vá»›i pattern soft-delete Ä‘Ã£ cÃ³ trong codebase (UC-187/188); giá»¯ dá»¯ liá»‡u cho audit/DPO investigation; Ä‘Ãºng nhÆ° UC194 TDS Ä‘Ã£ pre-thiáº¿t káº¿ field `status` trÃªn entity | Cáº§n lá»c `status <> 'DELETED'` á»Ÿ Má»ŒI query Ä‘á»c (Ä‘Ã£ note trong UC194 TDS, giá» chÃ­nh thá»©c activate) |

#### Quyáº¿t Ä‘á»‹nh (Decision)
Chá»n **PhÆ°Æ¡ng Ã¡n B**, Ä‘Ãºng nhÆ° UC194's TDS Ä‘Ã£ dá»± Ä‘oÃ¡n. Migration má»›i: `V20260707111000__add_baby_daily_log_status.sql`, thÃªm cá»™t `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` + index `idx_baby_daily_logs_status`, **mirror chÃ­nh xÃ¡c pattern** cá»§a `V20260627100200__add_maternal_metric_status.sql`.

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:**
- Dá»¯ liá»‡u khÃ´ng bá»‹ máº¥t vÄ©nh viá»…n â€” há»— trá»£ DPO investigation, dispute resolution.
- Nháº¥t quÃ¡n 100% vá»›i UC194's entity design (khÃ´ng cáº§n sá»­a `BabyDailyLog.java` Ä‘Ã£ spec, chá»‰ activate field `status` khÃ´ng cÃ²n nullable-by-default).
- Nháº¥t quÃ¡n vá»›i pattern soft-delete hiá»‡n cÃ³ trong `maternal_health_metrics` â€” cÃ¹ng ngÆ°á»i sá»Ÿ há»¯u module (TV2-BÃ¡ch), giáº£m rá»§i ro lá»‡ch convention.

**TiÃªu cá»±c / Trade-offs:**
- Báº£ng `baby_daily_logs` sáº½ tÃ­ch luá»¹ dá»¯ liá»‡u DELETED theo thá»i gian â€” cháº¥p nháº­n Ä‘Æ°á»£c á»Ÿ giai Ä‘oáº¡n hiá»‡n táº¡i; retention/purge job lÃ  Open Item (xem cuá»‘i tÃ i liá»‡u).

**Compliance Impact:**
- Cá»§ng cá»‘ BR-PRIVACY: dá»¯ liá»‡u sá»©c khoáº» tráº» sÆ¡ sinh khÃ´ng bá»‹ xoÃ¡ khÃ´ng thá»ƒ phá»¥c há»“i mÃ  khÃ´ng qua audit trail.

---

### ADR-BABY-007 â€” Ownership Check cho Delete: Owner-Only (KHÃ”NG má»Ÿ rá»™ng cho Care Group Member)

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `TV2-BÃ¡ch, AI Agent` |
| **Date** | `2026-07-03` |
| **Supersedes** | â€” |

#### Bá»‘i cáº£nh (Context)
UC194's ADR-BABY-004 tÃ¡i sá»­ dá»¥ng `BabyAccessPolicy.canView()` â€” cho phÃ©p cáº£ OWNER vÃ  ACCEPTED care group member xem log. UC195 lÃ  hÃ nh Ä‘á»™ng **destructive** (xoÃ¡ dá»¯ liá»‡u, dÃ¹ lÃ  soft-delete). SRS mÃ´ táº£ UC-195 lÃ  "Soft-deletes a **Mother-entered** baby daily log record" â€” nháº¥n máº¡nh quyá»n sá»Ÿ há»¯u cá»§a ngÆ°á»i nháº­p liá»‡u. Cáº§n quyáº¿t Ä‘á»‹nh: dÃ¹ng láº¡i `canView()` (rá»§i ro: care group member cÃ³ thá»ƒ xoÃ¡ log khÃ´ng pháº£i do há» nháº­p) hay táº¡o rule cháº·t hÆ¡n?

#### CÃ¡c phÆ°Æ¡ng Ã¡n Ä‘Ã£ xem xÃ©t (Options Considered)

| PhÆ°Æ¡ng Ã¡n | MÃ´ táº£ | Æ¯u Ä‘iá»ƒm | NhÆ°á»£c Ä‘iá»ƒm |
|-----------|-------|----------|------------|
| A | TÃ¡i sá»­ dá»¥ng `canView()` y há»‡t UC194 â€” OWNER hoáº·c ACCEPTED care member Ä‘á»u xoÃ¡ Ä‘Æ°á»£c | Code tá»‘i giáº£n, nháº¥t quÃ¡n tuyá»‡t Ä‘á»‘i vá»›i UC194 | Vi pháº¡m least-privilege (BR-RBAC): 1 care member (vd: ngÆ°á»i thÃ¢n Ä‘Æ°á»£c má»i) cÃ³ thá»ƒ xoÃ¡ dá»¯ liá»‡u do Mother hoáº·c member khÃ¡c nháº­p, khÃ´ng cÃ³ concept "chá»‰ xoÃ¡ cÃ¡i mÃ¬nh táº¡o" |
| B | ThÃªm method má»›i `canManage(BabyProfile, callerId)` vÃ o `BabyAccessPolicy` (EXTEND class hiá»‡n cÃ³, KHÃ”NG táº¡o class má»›i) â€” tráº£ `true` CHá»ˆ khi `profile.getOwnerUserId().equals(callerId)` | ÄÃºng vá»›i ngá»¯ nghÄ©a "Mother-entered" trong SRS; giáº£m blast radius cá»§a thao tÃ¡c xoÃ¡; váº«n tÃ¡i sá»­ dá»¥ng chain ownership-resolution pattern (load `BabyProfile` qua `baby_id`) Ä‘Ã£ cÃ³ tá»« UC192/UC194 â€” chá»‰ khÃ¡c á»Ÿ *má»©c Ä‘á»™ cháº·t* cá»§a check, khÃ´ng pháº£i *cÃ¡ch* resolve ownership | ThÃªm 1 method vÃ o `BabyAccessPolicy` â€” cáº§n review ká»¹ Ä‘á»ƒ khÃ´ng phÃ¡ `canView()` hiá»‡n cÃ³ (chá»‰ ADD, khÃ´ng sá»­a signature cÅ©) |

#### Quyáº¿t Ä‘á»‹nh (Decision)
Chá»n **PhÆ°Æ¡ng Ã¡n B**. `BabyAccessPolicy` (class hiá»‡n cÃ³ tá»« UC192, Ä‘Ã£ dÃ¹ng láº¡i á»Ÿ UC194) Ä‘Æ°á»£c **EXTEND** thÃªm method `canManage(BabyProfile profile, UUID callerId): boolean`, dÃ¹ng riÃªng cho cÃ¡c thao tÃ¡c ghi/xoÃ¡ (write operations) trÃªn baby data thuá»™c bounded context nÃ y. `canView()` giá»¯ nguyÃªn, KHÃ”NG bá»‹ sá»­a Ä‘á»•i â€” Ä‘áº£m báº£o UC192/UC194 khÃ´ng bá»‹ áº£nh hÆ°á»Ÿng (backward compatible extension).

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:**
- NguyÃªn táº¯c least-privilege Ä‘Æ°á»£c tÃ´n trá»ng cho thao tÃ¡c destructive.
- `BabyAccessPolicy` trá»Ÿ thÃ nh single source of truth cho Cáº¢ hai má»©c quyá»n (view vs manage) trong bounded context `baby` â€” khÃ´ng phÃ¢n máº£nh logic ra class má»›i.

**TiÃªu cá»±c / Trade-offs:**
- Care group member (ká»ƒ cáº£ ACCEPTED) khÃ´ng thá»ƒ xoÃ¡ log há»™ Mother dÃ¹ cÃ³ thá»ƒ cáº§n trong má»™t sá»‘ tÃ¬nh huá»‘ng thá»±c táº¿ (vd: bá»‘ xoÃ¡ log do máº¹ nháº­p nháº§m) â€” ghi nháº­n lÃ  Open Item, cÃ³ thá»ƒ cáº§n vote/permission model tinh vi hÆ¡n (`permission_json` trÃªn `care_group_members` â€” hiá»‡n chÆ°a cÃ³ vocabulary xÃ¡c Ä‘á»‹nh) trong tÆ°Æ¡ng lai.

**Compliance Impact:**
- Cá»§ng cá»‘ BR-RBAC (OWASP A01:2021 â€” Broken Access Control mitigation), nháº¥t quÃ¡n vá»›i UC194's ADR-BABY-004 methodology (reuse-then-extend) nhÆ°ng Ã¡p dá»¥ng má»©c quyá»n khÃ¡c cho hÃ nh Ä‘á»™ng khÃ¡c.

---

### ADR-BABY-008 â€” Audit Event Báº®T BUá»˜C cho Delete (khÃ¡c vá»›i UC194's Read-Only "No Audit")

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Date** | `2026-07-03` |

#### Bá»‘i cáº£nh (Context)
UC194's ADR-BABY-005 quyáº¿t Ä‘á»‹nh KHÃ”NG audit hÃ nh Ä‘á»™ng xem (read-only, high-frequency, no side effect). UC195 lÃ  hÃ nh Ä‘á»™ng WRITE (thay Ä‘á»•i state tá»« ACTIVE â†’ DELETED) trÃªn dá»¯ liá»‡u Sensitive-PII â€” khÃ¡c biá»‡t cÄƒn báº£n vá» rá»§i ro vÃ  táº§n suáº¥t (Occasional, khÃ´ng pháº£i Frequent).

#### Quyáº¿t Ä‘á»‹nh (Decision)
**Báº¯t buá»™c** emit audit event thÃ´ng qua `AuditService.log(AuditAction.BABY_DAILY_LOG_DELETED, callerId, "BabyDailyLog", babyLogId.toString(), details)` â€” pattern giá»‘ng há»‡t `BabyServiceImpl.createBabyProfile()` (dÃ¹ng `AuditAction.BABY_PROFILE_CREATED`). Cáº§n thÃªm 1 háº±ng sá»‘ enum má»›i `BABY_DAILY_LOG_DELETED` vÃ o `com.carebridge.backend.audit.entity.AuditAction` (file hiá»‡n cÃ³, CHá»ˆ thÃªm 1 dÃ²ng â€” khÃ´ng refactor enum hiá»‡n táº¡i, tuÃ¢n thá»§ Delivery Rules "smallest scoped change").

#### Há»‡ quáº£ (Consequences)

**TÃ­ch cá»±c:** Äáº§y Ä‘á»§ audit trail cho thao tÃ¡c xoÃ¡ dá»¯ liá»‡u sá»©c khoáº» tráº» sÆ¡ sinh â€” há»— trá»£ DPO investigation, dispute resolution náº¿u Mother khiáº¿u náº¡i dá»¯ liá»‡u bá»‹ máº¥t.

**TiÃªu cá»±c / Trade-offs:** ThÃªm 1 write vÃ o `audit_logs` má»—i láº§n xoÃ¡ â€” cháº¥p nháº­n Ä‘Æ°á»£c vÃ¬ `Frequency of Use = Occasional`.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|------------------|
| Latency (p99) | DELETE response | `< 300ms` (bao gá»“m audit write) | k6 load test | â€” |
| Availability | Uptime | `99.9%` | Uptime monitor | â€” |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Consistency | Sau delete, `status='DELETED'`, KHÃ”NG máº¥t `baby_id`/`note`/`recorded_by` (soft-delete, khÃ´ng xoÃ¡ dá»¯ liá»‡u) | 100% | DB assertion sau delete | BR-PRIVACY |
| Idempotency | Gá»i delete láº§n 2 trÃªn record Ä‘Ã£ DELETED â†’ 404 (khÃ´ng lá»—i 500, khÃ´ng lá»™ tráº¡ng thÃ¡i Ä‘Ã£ xoÃ¡ theo cÃ¡ch khÃ¡c) | 100% | Integration test | BR-SAFETY |
| Audit completeness | Má»—i láº§n xoÃ¡ thÃ nh cÃ´ng â†’ Ä‘Ãºng 1 `audit_logs` entry `BABY_DAILY_LOG_DELETED` | 100% | DB assertion trÃªn `audit_logs` | BR-SAFETY |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Access control | IDOR guard â€” CHá»ˆ owner (khÃ´ng pháº£i care member) qua `BabyAccessPolicy.canManage()` | 100% requests kiá»ƒm tra | Unit + integration test | BR-RBAC |
| Encryption in transit | TLS | TLS 1.3+ | SSL Labs scan | â€” |

### 4.4. Scalability & Capacity Planning

Táº£i dá»± kiáº¿n tháº¥p (`Frequency of Use = Occasional` theo SRS Table 217) â€” khÃ´ng cáº§n rate-limit Ä‘áº·c biá»‡t ngoÃ i má»©c chung 60/min cho mutating endpoint (theo convention `PATCH` trong template Â§9.1).

---

## 5. Static Modeling (MÃ´ hÃ¬nh TÄ©nh)

### 5.1. Class Diagram (PlantUML) â€” Existing (UC194) vs New (UC195)

```plantuml
@startuml DeleteBabyDailyLog_ClassDiagram
skinparam classAttributeIconSize 0
skinparam backgroundColor #FAFAFA

class BabyDailyLog <<UC194 â€” existing>> {
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

enum BabyDailyLogStatus <<UC194 â€” existing>> {
  ACTIVE
  DELETED
}

interface IBabyDailyLogService <<UC194 â€” existing interface, UC195 adds method>> {
  + getDailyLogDetail(babyLogId, callerId): BabyDailyLogDetailResponse
  + deleteBabyDailyLog(babyLogId: UUID, callerId: UUID): void <<NEW â€” UC195>>
}

class BabyDailyLogServiceImpl <<UC194 â€” existing class, UC195 adds method>> implements IBabyDailyLogService {
  - babyDailyLogRepository: BabyDailyLogRepository
  - babyProfileRepository: BabyProfileRepository
  - babyAccessPolicy: BabyAccessPolicy
  - auditService: AuditService
  + getDailyLogDetail(babyLogId, callerId): BabyDailyLogDetailResponse
  + deleteBabyDailyLog(babyLogId, callerId): void <<NEW â€” UC195>>
}

interface BabyDailyLogRepository <<UC194 â€” existing, no new method needed>> {
  + findById(id: UUID): Optional<BabyDailyLog>
  + save(entity: BabyDailyLog): BabyDailyLog
}

class BabyAccessPolicy <<UC192 â€” existing class, UC195 adds method>> {
  + canView(profile: BabyProfile, callerId: UUID): boolean
  + canManage(profile: BabyProfile, callerId: UUID): boolean <<NEW â€” UC195>>
}

class BabyDailyLogController <<UC194 â€” existing class, UC195 adds endpoint>> {
  + getDailyLogDetail(babyId, logId, principal): ResponseEntity<...>
  + deleteBabyDailyLog(babyId: UUID, logId: UUID, principal: Principal): ResponseEntity<ApiResponse<Void>> <<NEW â€” UC195>>
}

BabyDailyLogServiceImpl --> BabyDailyLogRepository : uses
BabyDailyLogServiceImpl --> "com.carebridge.backend.baby.repository.BabyProfileRepository" : reuse (UC192)
BabyDailyLogServiceImpl --> BabyAccessPolicy : uses (canView from UC194, canManage NEW)
BabyDailyLogServiceImpl --> "com.carebridge.backend.audit.service.AuditService" : NEW dependency (UC195)
BabyDailyLogController --> BabyDailyLogServiceImpl : uses
BabyDailyLog "many" --> "1" "com.carebridge.backend.baby.entity.BabyProfile" : baby_id FK

@enduml
```

**RG-3 â€” Danh sÃ¡ch method hiá»‡n cÃ³ (UC194) vs method má»›i (UC195), trÃ¡nh trÃ¹ng láº·p:**

| Class | Method (UC194 â€” EXISTING, khÃ´ng sá»­a) | Method (UC195 â€” NEW, thÃªm vÃ o) |
|-------|----------------------------------------|----------------------------------|
| `BabyDailyLogController` | `getDailyLogDetail(babyId, logId, principal)` | `deleteBabyDailyLog(babyId, logId, principal)` |
| `IBabyDailyLogService` / `BabyDailyLogServiceImpl` | `getDailyLogDetail(babyLogId, callerId)` | `deleteBabyDailyLog(babyLogId, callerId)` |
| `BabyDailyLogRepository` | `findById(id)` (káº¿ thá»«a `JpaRepository`) | KhÃ´ng cáº§n method má»›i â€” `save(entity)` (káº¿ thá»«a sáºµn) Ä‘á»§ Ä‘á»ƒ persist `status=DELETED` |
| `BabyAccessPolicy` | `canView(profile, callerId)` (tá»« UC192) | `canManage(profile, callerId)` (NEW) |
| `BabyDailyLog` entity, `BabyDailyLogStatus` enum | ToÃ n bá»™ field Ä‘Ã£ spec á»Ÿ UC194 | KhÃ´ng thÃªm field â€” chá»‰ **activate** cá»™t `status` (migration UC195 táº¡o) |

> **CASE 2.0 Constraint:** KHÃ”NG táº¡o `BabyDailyLogController`/`BabyDailyLogServiceImpl`/`BabyDailyLogRepository` má»›i â€” báº¯t buá»™c `implements`/má»Ÿ rá»™ng file UC194 Ä‘Ã£ thiáº¿t káº¿ táº¡i `com.carebridge.backend.baby.{controller,service,service.impl,repository}`.

### 5.2. Data Structure (Flyway SQL Migration)

> **CareBridge rule:** `V1__init_schema.sql` lÃ  baseline oracle; hiá»‡n táº¡i (xÃ¡c nháº­n láº¡i qua Read trá»±c tiáº¿p) `baby_daily_logs` (dÃ²ng 621-633) **KHÃ”NG cÃ³ cá»™t `status`** â€” Ä‘Ãºng nhÆ° UC194's TDS Ä‘Ã£ ghi nháº­n. UC195 lÃ  bÃªn chÃ­nh thá»©c láº¥p gap nÃ y.

**Migration má»›i â€” `V20260707111000__add_baby_daily_log_status.sql`:**

```sql
-- UC-195: DeleteBabyDailyLog â€” soft-delete support (companion to UC194's pre-designed `status` field)
-- Mirrors V20260627100200__add_maternal_metric_status.sql pattern (same owner: TV2-BÃ¡ch)
ALTER TABLE public.baby_daily_logs
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_baby_daily_logs_status ON public.baby_daily_logs(status);
```

> **Version note:** `V20260707111000` náº±m trong sub-range `110000`-series Ä‘Ã£ Ä‘Æ°á»£c UC194's TDS dÃ nh riÃªng cho companion migration nÃ y (xem UC194 TDS Â§5.2, Â§11.1 checklist item "UC195 migration ... Ä‘Ã£ review"). KHÃ”NG dÃ¹ng `090000`/`100000`/`120000`/`130000` (cÃ¡c range Ä‘Ã£ dÃ¹ng bá»Ÿi cÃ¡c UC khÃ¡c).

> **KhÃ´ng cáº§n** thÃªm `deleted_at TIMESTAMPTZ` â€” pattern hiá»‡n cÃ³ trong codebase (`maternal_health_metrics`) chá»‰ dÃ¹ng `status` Ä‘Æ¡n thuáº§n, KHÃ”NG cÃ³ timestamp riÃªng cho soft-delete. Thá»i Ä‘iá»ƒm xoÃ¡ Ä‘Æ°á»£c suy ra tá»« `updated_at` (Ä‘Ã£ cÃ³ sáºµn, cáº­p nháº­t tá»± Ä‘á»™ng khi `save()`).

**Existing schema (V1__init_schema.sql, dÃ²ng 621-633) â€” tráº¡ng thÃ¡i TRÆ¯á»šC migration nÃ y (KHÃ”NG chá»‰nh sá»­a migration Ä‘Ã£ apply):**
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
```

---

## 6. Dynamic Modeling (MÃ´ hÃ¬nh Äá»™ng)

### 6.1. Sequence Diagram â€” Happy Path (PlantUML)

```plantuml
@startuml DeleteBabyDailyLog_HappyPath
skinparam backgroundColor #FAFAFA
actor "Mother (Owner)" as Client
participant "BabyDailyLogController" as Controller
participant "BabyDailyLogServiceImpl" as Service
participant "BabyDailyLogRepository" as LogRepo
participant "BabyProfileRepository" as ProfileRepo
participant "BabyAccessPolicy" as Policy
participant "AuditService" as Audit
database "PostgreSQL" as DB

Client -> Controller : DELETE /api/v1/babies/{babyId}/daily-logs/{logId}
activate Controller
Controller -> Controller : SecurityUtils.requireCurrentUserId(principal)
Controller -> Service : deleteBabyDailyLog(logId, callerId)
activate Service

Service -> LogRepo : findById(logId)
LogRepo -> DB : SELECT * FROM baby_daily_logs WHERE baby_log_id=?
DB --> LogRepo : BabyDailyLog row (status=ACTIVE)
LogRepo --> Service : Optional<BabyDailyLog>

Service -> Service : if (log.status == DELETED) throw 404 DAILYLOG-001

Service -> ProfileRepo : findById(log.getBabyId())
ProfileRepo -> DB : SELECT * FROM baby_profiles WHERE baby_id=?
DB --> ProfileRepo : BabyProfile row
ProfileRepo --> Service : Optional<BabyProfile>

Service -> Policy : canManage(profile, callerId)
Policy --> Service : true (callerId == profile.ownerUserId)

Service -> Service : log.setStatus(DELETED)
Service -> LogRepo : save(log)
LogRepo -> DB : UPDATE baby_daily_logs SET status='DELETED', updated_at=now() WHERE baby_log_id=?
DB --> LogRepo : ok

Service -> Audit : log(BABY_DAILY_LOG_DELETED, callerId, "BabyDailyLog", logId, details)
Audit -> DB : INSERT INTO audit_logs (...)

Service --> Controller : void
deactivate Service
Controller --> Client : 200 OK {success: true, message: "Baby daily log deleted successfully"}
deactivate Controller
@enduml
```

### 6.2. Sequence Diagram â€” Error Path (PlantUML)

```plantuml
@startuml DeleteBabyDailyLog_ErrorPath
skinparam backgroundColor #FAFAFA
actor "Care Group Member (ACCEPTED, non-owner)" as Client
participant "BabyDailyLogController" as Controller
participant "BabyDailyLogServiceImpl" as Service
participant "BabyDailyLogRepository" as LogRepo
participant "BabyProfileRepository" as ProfileRepo
participant "BabyAccessPolicy" as Policy

Client -> Controller : DELETE /api/v1/babies/{babyId}/daily-logs/{logId}
activate Controller
Controller -> Service : deleteBabyDailyLog(logId, callerId)
activate Service
Service -> LogRepo : findById(logId)
LogRepo --> Service : Optional<BabyDailyLog> (present, status=ACTIVE)
Service -> ProfileRepo : findById(log.getBabyId())
ProfileRepo --> Service : Optional<BabyProfile> (present, owner = OTHER user)
Service -> Policy : canManage(profile, callerId)
Policy --> Service : false (caller is ACCEPTED member, NOT owner â€” ADR-BABY-007)
Service -> Service : throw BusinessException(403, "DAILYLOG-003")
deactivate Service
Controller --> Client : 403 Forbidden {code: DAILYLOG-003}
deactivate Controller

note over Service
  Alternative 1: logId not found -> BusinessException(404, "DAILYLOG-001")
  Alternative 2: log.status == DELETED (Ä‘Ã£ xoÃ¡ trÆ°á»›c Ä‘Ã³, double-delete) -> BusinessException(404, "DAILYLOG-001")
   (idempotent-safe: tráº¡ng thÃ¡i cuá»‘i cÃ¹ng váº«n lÃ  DELETED dÃ¹ gá»i 1 hay nhiá»u láº§n â€” KHÃ”NG lá»™
    "record Ä‘Ã£ tá»“n táº¡i nhÆ°ng bá»‹ xoÃ¡" qua status code khÃ¡c, nháº¥t quÃ¡n UC194's C3 no-leak rule)
end note
@enduml
```

---

## 7. Domain Event Catalog

### 7.1. Events Published (PhÃ¡t ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| `BabyDailyLogDeleted` | Soft-delete thÃ nh cÃ´ng (`status` chuyá»ƒn `ACTIVE` â†’ `DELETED`) | `BabyDailyLogServiceImpl` | `audit` (qua `AuditService.log`, Ä‘á»“ng bá»™ trong cÃ¹ng transaction) | `BabyDailyLogDeletedEvent.java` | No (Ä‘á»“ng bá»™, trong cÃ¹ng `@Transactional`, khÃ¡c UC194's optional async design) |

### 7.2. Events Consumed (TiÃªu thá»¥)

KhÃ´ng cÃ³ â€” module nÃ y khÃ´ng tiÃªu thá»¥ event nÃ o.

### 7.3. Payload Schema

```java
// BabyDailyLogDeletedEvent.java â€” documented for future async audit/notification consumers;
// v1.0 implementation calls AuditService.log() directly (synchronous), matching UC192's
// createBabyProfile() pattern â€” NOT a Spring ApplicationEvent publisher in this iteration.
public record BabyDailyLogDeletedEvent(
    UUID    eventId,
    String  eventType,       // "BabyDailyLogDeleted"
    Instant occurredAt,
    String  version,         // "1.0"
    Payload payload,
    Metadata metadata
) {
    public record Payload(
        UUID babyLogId,
        UUID babyId,
        UUID deletedByUserId
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
// IBabyDailyLogService.java â€” EXISTING interface (UC194), method ADDED here (UC195)
// @version 1.1 (bumped from UC194's 1.0 â€” additive change, non-breaking)
public interface IBabyDailyLogService {
    // --- UC194 â€” existing, unchanged ---
    BabyDailyLogDetailResponse getDailyLogDetail(UUID babyLogId, UUID callerId);

    // --- UC195 â€” NEW ---
    /**
     * Soft-deletes a baby daily log (status ACTIVE -> DELETED). Idempotent-safe: repeat calls
     * on an already-deleted record return 404 (DAILYLOG-001), not a distinct "already deleted" code.
     * @throws com.carebridge.backend.common.exception.BusinessException (DAILYLOG-001/404)
     *         khi babyLogId khÃ´ng tá»“n táº¡i HOáº¶C record Ä‘Ã£ status=DELETED
     * @throws com.carebridge.backend.common.exception.BusinessException (DAILYLOG-003/403)
     *         khi caller khÃ´ng pháº£i OWNER cá»§a baby profile liÃªn quan (ADR-BABY-007 â€” stricter
     *         than getDailyLogDetail's canView(), care group members are NOT permitted)
     */
    void deleteBabyDailyLog(UUID babyLogId, UUID callerId);
}
```

### 8.2. Entity & Repository Interface

```java
// BabyDailyLog.java â€” EXISTING entity (UC194), com.carebridge.backend.baby.entity â€” NO field changes.
// The `status` field designed by UC194 (nullable placeholder) becomes fully backed by DB column
// after this UC's migration (V20260707111000) runs â€” no Java code change needed on the entity itself.

// BabyDailyLogRepository.java â€” EXISTING (UC194), com.carebridge.backend.baby.repository â€” NO new
// query method needed. save() (inherited from JpaRepository) persists the status transition.
public interface BabyDailyLogRepository extends JpaRepository<BabyDailyLog, UUID> {
    // findById() and save() inherited from JpaRepository are sufficient for UC195.
}

// BabyAccessPolicy.java â€” EXISTING class (UC192), com.carebridge.backend.baby.policy â€” method ADDED.
// @version 1.1
@Component
@RequiredArgsConstructor
public class BabyAccessPolicy {

    private final CareGroupMemberRepository memberRepository;

    // --- UC192 â€” existing, unchanged ---
    public boolean canView(BabyProfile profile, UUID callerId) {
        if (profile.getOwnerUserId().equals(callerId)) {
            return true;
        }
        return memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                profile.getId(), callerId, InviteStatus.ACCEPTED);
    }

    // --- UC195 â€” NEW ---
    /**
     * Stricter than canView(): ONLY the baby profile owner may perform write/destructive
     * operations (delete). ADR-BABY-007 â€” care group members (even ACCEPTED) are excluded.
     */
    public boolean canManage(BabyProfile profile, UUID callerId) {
        return profile.getOwnerUserId().equals(callerId);
    }
}
```

### 8.3. Audit Enum Extension (prerequisite code change, outside `baby` package)

```java
// com.carebridge.backend.audit.entity.AuditAction â€” EXISTING file, ONE constant ADDED at end
// (append-only edit, does not reorder or remove existing constants â€” smallest scoped change)
public enum AuditAction {
    // ... all existing UC192-and-earlier constants unchanged ...
    NOTIFICATIONS_READ,
    BABY_DAILY_LOG_DELETED   // NEW â€” UC195
}
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `DELETE` | `/api/v1/babies/{babyId}/daily-logs/{logId}` | JWT Bearer | `ROLE_MOTHER` (owner only) | 60/min | Yes (soft-delete; repeat calls return 404, no unsafe side effect) |

> **Path design note:** Nested under `BabyDailyLogController`'s EXISTING base path `/api/v1/babies/{babyId}/daily-logs` (UC194 convention) â€” new `@DeleteMapping("/{logId}")` method on the SAME controller class. `babyId` in path is routing-only; authorization is based on `babyDailyLog.getBabyId()` read from DB (reuse of UC194's Constraint C2).

### 9.2. Request / Response Schemas

#### `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}`

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response â€” 200 OK:**
```json
{
  "success": true,
  "message": "Baby daily log deleted successfully",
  "data": null
}
```

**Response â€” 403 Forbidden:**
```json
{
  "error": { "code": "DAILYLOG-003", "message": "Only the baby profile owner can delete this daily log" }
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

> Tiáº¿p tá»¥c prefix `DAILYLOG-` do UC194 khá»Ÿi táº¡o â€” UC195 REUSE `DAILYLOG-001` (má»Ÿ rá»™ng trigger condition) vÃ  THÃŠM `DAILYLOG-003` (mÃ£ 403 riÃªng cho delete, phÃ¢n biá»‡t vá»›i `DAILYLOG-002` lÃ  403 cá»§a view).

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition | Nguá»“n |
|------|-------------|--------------|--------------|-------------------|-------|
| `DAILYLOG-001` | 404 | Baby daily log not found | KhÃ´ng tÃ¬m tháº¥y nháº­t kÃ½ háº±ng ngÃ y | `babyLogId` khÃ´ng tá»“n táº¡i HOáº¶C `status=DELETED` (Ä‘Ã£ xoÃ¡ trÆ°á»›c Ä‘Ã³ â€” idempotent 404, KHÃ”NG mÃ£ riÃªng) HOáº¶C `babyId` FK orphan | UC194 (reused, trigger má»Ÿ rá»™ng bá»Ÿi UC195) |
| `DAILYLOG-002` | 403 | Access denied to baby daily log | KhÃ´ng Ä‘á»§ quyá»n truy cáº­p nháº­t kÃ½ | (View only) Caller khÃ´ng pháº£i owner/ACCEPTED care member | UC194 (unchanged) |
| `DAILYLOG-003` | 403 | Only the baby profile owner can delete this daily log | Chá»‰ chá»§ sá»Ÿ há»¯u há»“ sÆ¡ bÃ© má»›i Ä‘Æ°á»£c xoÃ¡ nháº­t kÃ½ | (Delete only) Caller KHÃ”NG pháº£i `profile.ownerUserId` â€” ká»ƒ cáº£ khi lÃ  ACCEPTED care member (ADR-BABY-007) | **NEW â€” UC195** |
| `DAILYLOG-005` | 500 | Internal error | Lá»—i há»‡ thá»‘ng | Unexpected DB error (bao gá»“m audit write failure â€” xem Â§12 rollback) | UC194 (reused) |

---

## 11. Quy trÃ¬nh Triá»ƒn khai (Step-by-Step)

### 11.1. Prerequisites
- [ ] TDS nÃ y Ä‘Æ°á»£c Approved
- [ ] UC194's TDS/code (náº¿u implement song song hoáº·c trÆ°á»›c) Ä‘Ã£ cÃ³ `BabyDailyLog`, `BabyDailyLogStatus`, `BabyDailyLogRepository`, `IBabyDailyLogService`, `BabyDailyLogServiceImpl`, `BabyDailyLogController` â€” UC195 EXTENDS cÃ¡c file nÃ y, khÃ´ng táº¡o má»›i
- [ ] `BabyAccessPolicy`, `BabyProfileRepository` (UC192) Ä‘Ã£ cÃ³ sáºµn trong `main` â€” xÃ¡c nháº­n (Ä‘Ã£ cÃ³, verified qua Read trá»±c tiáº¿p)

### 11.2. Pre-Migration Checklist
- [ ] Backup DB staging trÆ°á»›c khi cháº¡y `V20260707111000__add_baby_daily_log_status.sql`
- [ ] XÃ¡c nháº­n KHÃ”NG cÃ³ migration nÃ o khÃ¡c trong khoáº£ng `V20260707100000`-`V20260707120000` xung Ä‘á»™t tÃªn cá»™t `status` trÃªn `baby_daily_logs`
- [ ] Migration test trÃªn staging trÆ°á»›c khi merge

### 11.3. Implementation Steps

#### Cháº·ng 1 â€” Migration
Táº¡o `V20260707111000__add_baby_daily_log_status.sql` (xem Â§5.2). Cháº¡y `./mvnw flyway:migrate`.

#### Cháº·ng 2 â€” Audit Enum
ThÃªm `BABY_DAILY_LOG_DELETED` vÃ o cuá»‘i `com.carebridge.backend.audit.entity.AuditAction` (append-only, xem Â§8.3).

#### Cháº·ng 3 â€” Policy Extension
ThÃªm method `canManage(BabyProfile, UUID)` vÃ o `BabyAccessPolicy.java` hiá»‡n cÃ³ (KHÃ”NG sá»­a `canView()`).

#### Cháº·ng 4 â€” Service + Interface
ThÃªm `deleteBabyDailyLog(UUID, UUID)` vÃ o `IBabyDailyLogService` vÃ  implement trong `BabyDailyLogServiceImpl` â€” inject thÃªm `AuditService` (náº¿u chÆ°a cÃ³ tá»« UC194's constructor).

#### Cháº·ng 5 â€” Controller
ThÃªm `@DeleteMapping("/{logId}")` method `deleteBabyDailyLog` vÃ o `BabyDailyLogController` hiá»‡n cÃ³.

#### Cháº·ng 6 â€” Verification sau deploy
```bash
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [JWT_MOTHER_OWNER_TOKEN]"
# Expected: 200 {"success": true, "message": "Baby daily log deleted successfully"}

curl -X GET https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [JWT_MOTHER_OWNER_TOKEN]"
# Expected: 404 DAILYLOG-001 (confirms UC194's read path filters DELETED)
```

### 11.4. Deployment Checklist
- [ ] `./mvnw test` xanh (bao gá»“m UC194's existing tests â€” khÃ´ng regress)
- [ ] IDOR test (ACCEPTED care member â†’ 403 DAILYLOG-003) pass
- [ ] Double-delete test (gá»i delete 2 láº§n) â†’ láº§n 2 tráº£ 404, khÃ´ng 500
- [ ] `audit_logs` chá»©a Ä‘Ãºng 1 entry `BABY_DAILY_LOG_DELETED` sau má»—i láº§n xoÃ¡ thÃ nh cÃ´ng
- [ ] UC194's `getDailyLogDetail` tráº£ 404 cho record Ä‘Ã£ DELETED (regression check)

---

## 12. Rollback & Incident Runbook

### 12.1. Äiá»u kiá»‡n kÃ­ch hoáº¡t Rollback

| Äiá»u kiá»‡n | NgÆ°á»¡ng | NgÆ°á»i quyáº¿t Ä‘á»‹nh |
|-----------|--------|------------------|
| Error rate tÄƒng Ä‘á»™t biáº¿n | > 5% trong 5 phÃºt | On-call Engineer |
| Soft-delete khÃ´ng nháº¥t quÃ¡n (status khÃ´ng Ä‘á»•i sau 200 OK) | Báº¥t ká»³ case nÃ o | Tech Lead |
| IDOR phÃ¡t hiá»‡n (care member xoÃ¡ Ä‘Æ°á»£c log khÃ´ng pháº£i cá»§a há») | Báº¥t ká»³ case nÃ o | Tech Lead + DPO |

### 12.2. Rollback Procedure

```bash
# BÆ°á»›c 1: Revert migration (dev/staging only â€” KHÃ”NG cháº¡y trÃªn production Ä‘Ã£ cÃ³ dá»¯ liá»‡u status)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.baby_daily_logs DROP COLUMN IF EXISTS status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260707111000';"

# BÆ°á»›c 2: Re-deploy phiÃªn báº£n trÆ°á»›c (revert BabyDailyLogController/ServiceImpl/AccessPolicy changes)
kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api
curl -X GET https://[host]/api/v1/health
```

> **Cáº£nh bÃ¡o:** Náº¿u production Ä‘Ã£ cÃ³ record `status='DELETED'`, KHÃ”NG drop cá»™t `status` â€” sáº½ máº¥t thÃ´ng tin Ä‘Ã£ xoÃ¡ logic. Chá»‰ rollback code (controller/service), giá»¯ nguyÃªn migration.

### 12.3. Notification Protocol

| Thá»i Ä‘iá»ƒm | NgÆ°á»i nháº­n | KÃªnh |
|-----------|------------|------|
| Ngay khi phÃ¡t hiá»‡n IDOR trÃªn delete | On-call + DPO | Slack `#incident` + Email |

---

## 13. Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t

> **Policy (EDS v2.0):** Má»i test scenario dÃ¹ng dá»¯ liá»‡u `SYNTHETIC`.

```gherkin
Feature: Delete Baby Daily Log
  Background:
    Given test data classification: SYNTHETIC
    And MOTHER-001 lÃ  owner cá»§a BABY-001
    And LOG-001 thuá»™c BABY-001 vá»›i status=ACTIVE, logType=feeding

  Scenario: Owner xoÃ¡ log â†’ 200, status chuyá»ƒn DELETED
    When deleteBabyDailyLog(LOG-001, MOTHER-001)
    Then response 200 vá»›i message "Baby daily log deleted successfully"
    And LOG-001.status == DELETED trong DB
    And audit_logs chá»©a 1 entry BABY_DAILY_LOG_DELETED cho LOG-001

  Scenario: Care group member (ACCEPTED, khÃ´ng pháº£i owner) xoÃ¡ log â†’ 403
    Given MOTHER-002 lÃ  ACCEPTED member trong care group cá»§a BABY-001
    When deleteBabyDailyLog(LOG-001, MOTHER-002)
    Then throws BusinessException DAILYLOG-003 (403)
    And LOG-001.status váº«n lÃ  ACTIVE (khÃ´ng cÃ³ side effect)

  Scenario: Non-owner, non-member â†’ 403
    Given MOTHER-003 KHÃ”NG liÃªn quan BABY-001
    When deleteBabyDailyLog(LOG-001, MOTHER-003)
    Then throws BusinessException DAILYLOG-003 (403)

  Scenario: Log khÃ´ng tá»“n táº¡i â†’ 404
    When deleteBabyDailyLog(NONEXISTENT, MOTHER-001)
    Then throws BusinessException DAILYLOG-001 (404)

  Scenario: Double-delete (log Ä‘Ã£ DELETED) â†’ 404, idempotent-safe
    Given LOG-002 thuá»™c BABY-001 vá»›i status=DELETED (Ä‘Ã£ xoÃ¡ trÆ°á»›c Ä‘Ã³)
    When deleteBabyDailyLog(LOG-002, MOTHER-001)
    Then throws BusinessException DAILYLOG-001 (404)
    And KHÃ”NG throw 500, KHÃ”NG táº¡o thÃªm audit_logs entry

  Scenario: Sau khi xoÃ¡, UC194's getDailyLogDetail cÅ©ng tráº£ 404 (regression/coupling check)
    When deleteBabyDailyLog(LOG-001, MOTHER-001)
    And getDailyLogDetail(LOG-001, MOTHER-001)
    Then getDailyLogDetail throws BusinessException DAILYLOG-001 (404)
```

---

## 14. PhÆ°Æ¡ng phÃ¡p XÃ¡c minh

### 14.1. Database Inspection

```sql
-- Verify soft-delete applied correctly
SELECT baby_log_id, baby_id, status, updated_at
FROM baby_daily_logs WHERE baby_log_id = '[logId]';
-- Expected: status = 'DELETED', updated_at = thá»i Ä‘iá»ƒm gá»i delete

-- Verify audit trail
SELECT action, actor_user_id, resource_type, resource_id, created_at
FROM audit_logs
WHERE action = 'BABY_DAILY_LOG_DELETED' AND resource_id = '[logId]'
ORDER BY created_at DESC LIMIT 1;
```

### 14.2. Access Policy Verification

```bash
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [OWNER_JWT]"
# Expected: 200

curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId2] \
  -H "Authorization: Bearer [CARE_MEMBER_ACCEPTED_JWT]"
# Expected: 403 DAILYLOG-003 (NOT DAILYLOG-002 â€” distinct code confirms ADR-BABY-007 applied)
```

---

## 15. Máº«u thá»­ thá»±c táº¿ (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [JWT_MOTHER_OWNER_TOKEN]"
# Expected: 200 {success: true, message: "Baby daily log deleted successfully"}
```

### 15.2. Error Paths

```bash
# Non-existent log -> 404
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/non-existent-uuid \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"

# Care group member (non-owner) -> 403 DAILYLOG-003
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [CARE_MEMBER_JWT]"

# No JWT -> 401
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId]

# Double-delete -> 404 on second call
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [JWT_MOTHER_OWNER_TOKEN]"
curl -X DELETE https://[host]/api/v1/babies/[babyId]/daily-logs/[logId] \
  -H "Authorization: Bearer [JWT_MOTHER_OWNER_TOKEN]"
# Expected 2nd call: 404 DAILYLOG-001
```

---

## 16. Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix)

| Endpoint | `GUEST` | `MOTHER (owner)` | `MOTHER (care member, ACCEPTED)` | `EXPERT` | `ADMIN` |
|----------|---------|-------------------|-----------------------------------|----------|---------|
| `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` | âŒ (401) | âœ… | âŒ (403 DAILYLOG-003 â€” ADR-BABY-007) | âŒ (403) | âŒ (out of scope for UC195 â€” admin-initiated erasure, if required by PDPA subject-access-request, is handled by a separate admin/DPO tooling flow, not this consumer endpoint) |

**ChÃº thÃ­ch:**
- Owner: `baby_profiles.owner_user_id` == JWT subject (via `baby_daily_logs.baby_id` FK) â€” CHá»ˆ owner, khÃ¡c vá»›i UC194's view matrix (owner + ACCEPTED member Ä‘á»u xem Ä‘Æ°á»£c).
- Care member: dÃ¹ `ACCEPTED`, KHÃ”NG cÃ³ quyá»n xoÃ¡ â€” chá»‰ cÃ³ quyá»n xem (`canView`), theo ADR-BABY-007.
- Admin: khÃ´ng cÃ³ route xoÃ¡ trá»±c tiáº¿p qua endpoint nÃ y; Ä‘Ã¡nh dáº¥u Open Item náº¿u Product yÃªu cáº§u admin override sau nÃ y.

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source | Last Verified |
|---|-----------|--------|---------------|
| C1 | `deleteBabyDailyLog()` PHáº¢I thÃªm vÃ o EXISTING `BabyDailyLogController`/`BabyDailyLogServiceImpl`/`IBabyDailyLogService` (UC194's classes) â€” TUYá»†T Äá»I KHÃ”NG táº¡o class song song (vd: `BabyDailyLogDeleteController`) | RG-3, CASE 2.0 Â§5.1 constraint | 2026-07-03 |
| C2 | Ownership check PHáº¢I dÃ¹ng `BabyAccessPolicy.canManage()` (NEW method, owner-only) â€” KHÃ”NG dÃ¹ng `canView()` cho delete | ADR-BABY-007 | 2026-07-03 |
| C3 | `babyId` trong URL path CHá»ˆ dÃ¹ng Ä‘á»ƒ routing â€” authorization luÃ´n dá»±a trÃªn `babyDailyLog.getBabyId()` Ä‘á»c tá»« DB, KHÃ”NG tin path param (káº¿ thá»«a UC194's C2) | ADR-BABY-004 (UC194), BR-RBAC | 2026-07-03 |
| C4 | XoÃ¡ PHáº¢I lÃ  soft-delete (`status = DELETED` qua `save()`) â€” TUYá»†T Äá»I KHÃ”NG gá»i `repository.delete()`/`deleteById()` (hard DELETE) | ADR-BABY-006 | 2026-07-03 |
| C5 | Má»—i láº§n xoÃ¡ thÃ nh cÃ´ng PHáº¢I emit `AuditService.log(BABY_DAILY_LOG_DELETED, ...)` trong CÃ™NG transaction (Ä‘á»“ng bá»™, khÃ´ng async) | ADR-BABY-008 | 2026-07-03 |
| C6 | Record `status=DELETED` khi bá»‹ xoÃ¡ láº§n ná»¯a PHáº¢I tráº£ 404 `DAILYLOG-001` (idempotent-safe), KHÃ”NG tráº£ mÃ£ lá»—i riÃªng "already deleted" â€” trÃ¡nh leak thÃ´ng tin tráº¡ng thÃ¡i | ADR-BABY-006, UC194's C3 no-leak rule | 2026-07-03 |

### 17.2 Constraint Injection Block

```
[CONSTRAINT BLOCK â€” Module: DeleteBabyDailyLog (CB-BABY-IMP-004)]
1. deleteBabyDailyLog() PHáº¢I Ä‘Æ°á»£c thÃªm vÃ o file EXISTING BabyDailyLogController.java / BabyDailyLogServiceImpl.java / IBabyDailyLogService.java (Ä‘Ã£ táº¡o bá»Ÿi UC194) â€” KHÃ”NG táº¡o controller/service/repository má»›i cho delete â€” CASE 2.0 duplication guard
2. Ownership check PHáº¢I dÃ¹ng BabyAccessPolicy.canManage(profile, callerId) â€” method Má»šI, owner-only, KHÃC vá»›i canView() (view cho phÃ©p cáº£ care member) â€” ADR-BABY-007
3. babyId trong URL path KHÃ”NG Ä‘Æ°á»£c dÃ¹ng Ä‘á»ƒ authorization â€” chá»‰ dÃ¹ng Ä‘á»ƒ route; ownership check luÃ´n dá»±a trÃªn dailyLog.getBabyId() Ä‘á»c tá»« DB â€” BR-RBAC
4. XoÃ¡ PHáº¢I lÃ  soft-delete: set entity.status = BabyDailyLogStatus.DELETED rá»“i gá»i repository.save() â€” TUYá»†T Äá»I KHÃ”NG gá»i delete()/deleteById() â€” ADR-BABY-006, BR-PRIVACY
5. Sau khi soft-delete thÃ nh cÃ´ng, PHáº¢I gá»i AuditService.log(AuditAction.BABY_DAILY_LOG_DELETED, callerId, "BabyDailyLog", babyLogId.toString(), details) trong cÃ¹ng @Transactional method â€” ADR-BABY-008, BR-SAFETY
6. Double-delete (record Ä‘Ã£ status=DELETED) PHáº¢I tráº£ BusinessException(404, DAILYLOG-001) â€” KHÃ”NG tráº£ 409/410 hay mÃ£ riÃªng â€” ADR-BABY-006

[CONTEXT BLOCK]
- Bounded Context: baby (reuse UC192/UC194 package â€” com.carebridge.backend.baby)
- Data Classification: Sensitive-PII
- Error codes: Â§10 Error Codes Table (DAILYLOG-001 reused+extended, DAILYLOG-003 NEW)
- Auth matrix: Â§16 Authorization Matrix (owner-only â€” stricter than UC194's view matrix)
- Reused classes: BabyDailyLog, BabyDailyLogStatus, BabyDailyLogRepository, IBabyDailyLogService, BabyDailyLogServiceImpl, BabyDailyLogController (UC194); BabyProfileRepository, BabyAccessPolicy (UC192, canManage() extended here); AuditService, AuditAction (audit module, BABY_DAILY_LOG_DELETED constant added)
- Migration: V20260707111000__add_baby_daily_log_status.sql (Â§5.2) â€” companion to UC194's pre-designed status field
```

### 17.3 Constraint Quality Checklist

- [x] Má»—i constraint traceable vá» ADR hoáº·c BR cá»¥ thá»ƒ
- [x] KhÃ´ng cÃ³ constraint generic
- [x] Constraint block cÃ³ â‰¥ 3 constraints cá»¥ thá»ƒ
- [x] Constraint C1 explicitly forbids parallel-class duplication (CASE 2.0 RG-3 requirement)

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | HÃ nh Ä‘á»™ng |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code khÃ´ng match constraint C1-C6 | Reject â€” inject láº¡i constraints |
| AP-AI-003 | Implicit Decision | Code viáº¿t `BabyDailyLogAccessPolicy` má»›i thay vÃ¬ extend `BabyAccessPolicy`, hoáº·c dÃ¹ng `canView()` thay vÃ¬ `canManage()` | Reject â€” vi pháº¡m ADR-BABY-007 |
| AP-AI-005 | Hallucinated Contract | Code import class khÃ´ng cÃ³ trong Â§8, hoáº·c gá»i `repository.delete()` (hard DELETE) | Reject â€” verify contract, vi pháº¡m ADR-BABY-006 |

---

## PHá»¤ Lá»¤C

### A. Glossary

| Thuáº­t ngá»¯ | Äá»‹nh nghÄ©a |
|-----------|------------|
| BabyDailyLog | Báº£n ghi nháº­t kÃ½ sinh hoáº¡t háº±ng ngÃ y cá»§a baby (feeding/sleep/diaper/...) â€” entity thiáº¿t káº¿ bá»Ÿi UC194 |
| Soft-delete | ÄÃ¡nh dáº¥u record lÃ  Ä‘Ã£ xoÃ¡ qua cá»™t `status`, KHÃ”NG xoÃ¡ váº­t lÃ½ khá»i DB |
| canManage | Method má»›i trÃªn `BabyAccessPolicy` â€” kiá»ƒm tra quyá»n thao tÃ¡c ghi/xoÃ¡, cháº·t hÆ¡n `canView` |
| IDOR | Insecure Direct Object Reference â€” truy cáº­p trÃ¡i phÃ©p báº±ng cÃ¡ch Ä‘oÃ¡n/thay Ä‘á»•i ID |

### B. TÃ i liá»‡u tham chiáº¿u

| Document | Path |
|----------|------|
| UC194 TDS (companion, EXTENDED bá»Ÿi UC195) | `04_Implement/UC194_ViewBabyDailyLogDetail/UC194_ViewBabyDailyLogDetail_TDS.md` |
| UC192 TDS (Approved, shipped code reference cho `BabyAccessPolicy`) | `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` |
| Soft-delete precedent (UC-187/188, same owner) | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627100200__add_maternal_metric_status.sql` |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |
| Schema baseline | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` |

---

## Open Items (chÆ°a resolve â€” cáº§n Tech Lead / Product xÃ¡c nháº­n trÆ°á»›c khi Approve)

| # | Item | MÃ´ táº£ | Äá» xuáº¥t táº¡m thá»i |
|---|------|-------|-------------------|
| OI-1 | Care group member khÃ´ng xoÃ¡ Ä‘Æ°á»£c log cá»§a Mother | ADR-BABY-007 chá»n owner-only; trong thá»±c táº¿ bá»‘/ngÆ°á»i thÃ¢n (ACCEPTED member) cÃ³ thá»ƒ cáº§n xoÃ¡ log nháº­p nháº§m | Giá»¯ owner-only á»Ÿ v1.0; revisit náº¿u Product xÃ¡c nháº­n cáº§n permission model chi tiáº¿t hÆ¡n (`permission_json` trÃªn `care_group_members` hiá»‡n chÆ°a cÃ³ vocabulary) |
| OI-2 | KhÃ´ng cÃ³ retention/purge job cho record `status=DELETED` | Dá»¯ liá»‡u DELETED tÃ­ch luá»¹ vÃ´ háº¡n trong `baby_daily_logs` | NgoÃ i pháº¡m vi UC195; Ä‘á» xuáº¥t PDPA retention policy job riÃªng (theo dÃµi á»Ÿ module `audit`/`account`) |
| OI-3 | ADMIN khÃ´ng cÃ³ quyá»n xoÃ¡ qua endpoint nÃ y | CÃ³ thá»ƒ cáº§n cho compliance/erasure request xá»­ lÃ½ bá»Ÿi DPO | Out of scope UC195; náº¿u cáº§n, xá»­ lÃ½ qua admin tooling riÃªng (tÆ°Æ¡ng tá»± `UC114_ManageUserAccounts`) |
| OI-4 (káº¿ thá»«a tá»« UC194 OI-3) | ~~Mismatch mÃ£ lá»—i UC192 tÃ i liá»‡u vs code~~ **RESOLVED (2026-07-03)** | UC195 dÃ¹ng prefix `DAILYLOG-` riÃªng, khÃ´ng bá»‹ áº£nh hÆ°á»Ÿng trá»±c tiáº¿p. TDS UC192 Ä‘Ã£ Ä‘Æ°á»£c sá»­a khá»›p code tháº­t (`BABY-001`=404, `BABY-003`=403); UC194 OI-3 Ä‘Ã£ Ä‘Ã³ng. | ÄÃ£ Ä‘Ã³ng â€” khÃ´ng cáº§n hÃ nh Ä‘á»™ng thÃªm. |

---

*EDS v2.1 â€” TÃ­ch há»£p CASE 2.0 AI Prompt Constraints (Â§17).*
