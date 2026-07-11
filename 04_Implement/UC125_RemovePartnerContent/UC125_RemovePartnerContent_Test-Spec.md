# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-125: Remove Partner Content

**Document ID:** `CB-PTR-TEST-008`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Partially Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — visibility flag, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC125_RemovePartnerContent/UC125_RemovePartnerContent_TDS.md` (`CB-PTR-IMP-008`)
- SRS Section 3.2.3.8
- Siblings (Draft): UC-120 (`CB-PTR-IMP-003`), UC-121 (`CB-PTR-IMP-004`), UC-124 (`CB-PTR-IMP-007`, sibling admin-decision pattern)
- CLAUDE.md §3, §5 (Flyway: never modify an applied migration)

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-125 Remove Partner Content (Status=Draft)   |
| 2026-07-11 | AI Agent — Amelia   | Functional/service/controller/security coverage passes (11/13 conditions). DB migration/atomicity integration cases remain unverified because no Docker-compatible runtime is available. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                     | Value                                                                                                                    |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-125`                                                                                                                |
| **Module**                | `Remove Partner Content — partner (admin soft-remove, targetType-dispatched)`                                          |
| **Spec gốc**              | `CB-PTR-IMP-008`                                                                                                          |
| **Priority**              | `P0 — High, Occasional`                                                                                                     |
| **Sprint / Milestone**    | `Open`                                                                                                                     |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `partner (PartnerService [UC-120], SponsoredCampaign [UC-121] + new removal columns)`, `security`, `audit`                 |
| **Downstream Consumers**  | Public catalog (must filter is_removed=false), UC-122 counts                                                               |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-PTR-IMP-008 §17`, `ADR-001..ADR-004`                                                                                           |
| **Constraints Injected** | `C1 (RBAC)`, `C2 (soft-remove, no hard delete)`, `C3 (reason required)`, `C4 (idempotent guard)`, `C5 (migration, 2 tables)`, `C6 (downstream filter)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                | Thực tế                                                                            | Fix áp dụng trong test                                                    |
| --- | --------------------------------------------------------- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| L1  | Dossier gợi ý tái dùng approval_status="REMOVED"           | ADR-001: chọn cột riêng (is_removed/removed_at/removed_by/removal_reason) — cần migration | Test PHẢI assert row vẫn tồn tại (soft) VÀ approval_status KHÔNG bị đổi bởi remove |
| L2  | Hard-delete hay soft-remove?                              | ADR-002: chỉ soft-remove; KHÔNG delete row                                            | Test PRC-TC-805 (CRITICAL): assert `repository.delete()` KHÔNG BAO GIỜ được gọi     |
| L3  | Remove lại item đã removed?                               | ADR-002: idempotent guard → PTR-028                                                   | Test PRC-TC-803: already-removed → PTR-028                                        |
| L4  | Nội dung đã gỡ còn hiển thị public không?                 | ADR-001 C6: downstream queries PHẢI filter is_removed=false                          | Integration test PRC-TC-INT-002: removed item KHÔNG xuất hiện trong catalog/list   |
| L5  | Reason bắt buộc?                                          | ADR-003: bắt buộc non-blank                                                          | Test PRC-TC-804: blank reason → PTR-029                                           |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
├── Controller (PartnerContentRemovalController.remove() — @WebMvcTest)
├── Service (PartnerContentRemovalServiceImpl.remove() — mock both repos + audit)
├── Repository (PartnerServiceRepository / SponsoredCampaignRepository findById/save; delete NEVER called)
└── Integration (Full POST both targetTypes — Testcontainers, MIGRATION APPLIED)
```

### TDS-02 — Test Basis

| Source | Items Derived |
| --- | --- |
| `SRS 3.2.3.8` | Admin removes partner content |
| `TDS ADR-001` | New removal columns (migration); no reuse of approval_status |
| `TDS ADR-002` | Soft-remove only; idempotent guard |
| `TDS ADR-003` | reason required |
| `V20260701130000__add_partner_content_removal.sql` | is_removed/removed_at/removed_by/removal_reason oracle |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                             | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | ----------------------------------------------- | ------------------- |
| TC-COND-001  | Remove SERVICE → is_removed=true + metadata; approval_status unchanged     | `PartnerContentRemovalServiceImpl.remove()`     | `PRC-TC-801`        |
| TC-COND-002  | Remove CAMPAIGN → is_removed=true + metadata                              | service                                          | `PRC-TC-802`        |
| TC-COND-003  | Already-removed → PTR-028 (409)                                            | service                                          | `PRC-TC-803`        |
| TC-COND-004  | Blank reason → PTR-029 (400)                                              | controller/service                               | `PRC-TC-804`        |
| TC-COND-005  | repository.delete() NEVER called (hard-delete guard)                       | service                                          | `PRC-TC-805`        |
| TC-COND-006  | Unsupported targetType → PTR-027 (400)                                     | controller/service                               | `PRC-TC-806`        |
| TC-COND-007  | target not found → PTR-026 (404)                                          | service                                          | `PRC-TC-807`        |
| TC-COND-008  | Audit called once                                                         | `AuditService` mock verify                       | `PRC-TC-808`        |
| TC-COND-009  | Non-admin → 403                                                            | `@PreAuthorize`                                   | `PRC-TC-809`        |
| TC-COND-010  | PARTNER → 403 (cannot remove even own content)                        | `@PreAuthorize`                                   | `PRC-TC-810`        |
| TC-COND-011  | No JWT → 401                                                               | entry point                                       | `PRC-TC-811`        |
| TC-COND-012  | Full POST integration — row still exists, is_removed=true; atomicity      | Testcontainers                                    | `PRC-TC-INT-001`    |
| TC-COND-013  | Removed item excluded from catalog/list query (downstream filter)         | Testcontainers                                    | `PRC-TC-INT-002`    |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
| --- | --- | --- |
| Negative / Hard-Delete Guard | repository.delete() never invoked | ADR-002 CRITICAL — data-loss prevention |
| Equivalence Partitioning | targetType (SERVICE/CAMPAIGN/unsupported) | ADR-001 |
| State/Idempotency | is_removed false→true; already-true→PTR-028 | ADR-002 |
| Downstream/Regression | catalog query excludes is_removed=true | ADR-001 C6 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                     | Mục đích                                  |
| ----------- | -------- | ------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-1201`  | DB seed | `PartnerService{approvalStatus: APPROVED, isRemoved: false}`        | Happy path remove                              |
| `FX-1202`  | DB seed | `SponsoredCampaign{approvalStatus: APPROVED, isRemoved: false}`     | Happy path remove (campaign)                   |
| `FX-1203`  | DB seed | `PartnerService{isRemoved: true, removedAt: <past>}`                | PTR-028 already-removed                        |
| `FX-1204`  | JWT     | `{role: "ROLE_SYSTEM_ADMIN"}`                                       | Auth happy path                                |
| `FX-1205`  | JWT     | `{role: "ROLE_PARTNER"}`                                       | Auth failure (403)                             |
| `FX-1206`  | JWT     | `{role: "ROLE_MODERATOR"}`                                         | Auth failure (403, no hierarchy)               |
| `FX-1207`  | none    | No `Authorization`                                                 | Auth failure (401)                             |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
class RemovePartnerContentTestFactory {
    static final UUID ADMIN_ID   = UUID.fromString("f0100000-0000-0000-0000-0000000000ad");
    static final UUID SERVICE_ID = UUID.fromString("f0200000-0000-0000-0000-00000000000d");
    static final UUID CAMPAIGN_ID = UUID.fromString("f0200000-0000-0000-0000-00000000000c");

    static PartnerService makeService(boolean isRemoved) {
        return PartnerService.builder().serviceId(SERVICE_ID).partnerId(UUID.randomUUID())
                .approvalStatus(ServiceApprovalStatus.APPROVED).isRemoved(isRemoved).build();
    }
    static SponsoredCampaign makeCampaign(boolean isRemoved) {
        return SponsoredCampaign.builder().campaignId(CAMPAIGN_ID).partnerId(UUID.randomUUID())
                .approvalStatus(CampaignApprovalStatus.APPROVED).isRemoved(isRemoved).build();
    }
    static RemovalRequest makeRequest(String reason) { return new RemovalRequest(reason); }
}
```

---

### PRC-TC-801 — Remove SERVICE → is_removed=true + metadata; approval_status unchanged

**Severity:** `HIGH`
**Feature Under Test:** `PartnerContentRemovalServiceImpl.remove(SERVICE, id, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §5.2`, `ADR-001/002`

**Preconditions:** `partnerServiceRepository.findById(SERVICE_ID)` → `FX-1201` (not removed)

**Test Steps:** request `{reason: "Vi phạm chính sách"}` → Act → capture saved entity

**Expected Result (PASS):**
- Saved `isRemoved == true`, `removedAt` non-null, `removedBy == ADMIN_ID`, `removalReason == "Vi phạm chính sách"`
- Saved `approvalStatus` **unchanged** (still `APPROVED`) — remove does NOT touch approval status (L1)

**Current Status:** 🔴 Not written

---

### PRC-TC-802 — Remove CAMPAIGN → is_removed=true + metadata

**Severity:** `HIGH`
**Feature Under Test:** `remove()` — campaign path
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §5.2`

**Preconditions:** `sponsoredCampaignRepository.findById(CAMPAIGN_ID)` → `FX-1202`

**Expected Result (PASS):** same as PRC-TC-801, for `SponsoredCampaign`.

**Current Status:** 🔴 Not written

---

### PRC-TC-803 — Already-removed → 409 PTR-028

**Severity:** `HIGH`
**Feature Under Test:** `remove()` — idempotent guard (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS ADR-002`

**Preconditions:** `findById` → `FX-1203` (isRemoved=true)

**Expected Result (PASS):** throws `PTR-028`, 409; no save (metadata unchanged from original removal).

**Current Status:** 🔴 Not written

---

### PRC-TC-804 — Blank reason → 400 PTR-029

**Severity:** `MEDIUM`
**Feature Under Test:** `remove()` — reason guard (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 PTR-029`

**Test Steps:** `{reason: ""}` / `{reason: null}`

**Expected Result (PASS):** 400, `error.code == "PTR-029"`; no removal.

**Current Status:** 🔴 Not written

---

### PRC-TC-805 — repository.delete() NEVER called (hard-delete guard)

**Severity:** `CRITICAL`
**Feature Under Test:** `remove()` — soft-remove-only guarantee (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-002 §Decision (soft-remove only)`

**Test Steps:** perform a valid remove on SERVICE and on CAMPAIGN

**Expected Result (PASS):** `verify(partnerServiceRepository, never()).delete(any())` and `verify(partnerServiceRepository, never()).deleteById(any())`; same for `sponsoredCampaignRepository`. Only `save()` is invoked.

**Expected Result (FAIL):** any `delete()`/`deleteById()` call → **data loss**, direct violation of ADR-002, BLOCKING severity.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This is the single most important test in this spec — it is the only thing
standing between "moderation action" and "irrecoverable data loss" for partner catalog content.

---

### PRC-TC-806 — Unsupported targetType → 400 PTR-027

**Severity:** `MEDIUM`
**Feature Under Test:** targetType dispatch
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §10 PTR-027`

**Expected Result (PASS):** 400, `error.code == "PTR-027"` (or path-enum 400 — assert 400 minimum).

**Current Status:** 🔴 Not written

---

### PRC-TC-807 — target not found → 404 PTR-026

**Severity:** `HIGH`
**Feature Under Test:** `remove()` — lookup
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §10 PTR-026`

**Expected Result (PASS):** `PTR-026`, 404.

**Current Status:** 🔴 Not written

---

### PRC-TC-808 — Audit called once

**Severity:** `MEDIUM`
**Feature Under Test:** `remove()` — audit (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/partner/RemovePartnerContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS ADR-004`

**Expected Result (PASS):** `verify(auditService, times(1)).log(...)` with admin id + targetType + targetId + reason.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### PRC-TC-809 — Non-admin → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/RemovePartnerContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16`, ADR-004 (role resolved = SYSTEM_ADMIN)

**Preconditions:** JWT role MODERATOR (FX-1206)

**Expected Result (PASS):** 403 (assert status; verify role — MODERATOR has no hierarchy over this endpoint).

**Current Status:** 🔴 Not written

---

### PRC-TC-810 — PARTNER → 403 (cannot remove even own content)

**Severity:** `CRITICAL`
**Feature Under Test:** `@PreAuthorize` — partner never self-removes
**Test File:** `src/test/java/com/carebridge/backend/security/RemovePartnerContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §16 (PARTNER = ❌)`

**Preconditions:** JWT role PARTNER (FX-1205)

**Expected Result (PASS):** 403 — removal is a moderation action, not partner self-service, even for the
partner's own listing.

**Current Status:** 🔴 Not written

---

### PRC-TC-811 — No JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** JWT entry point
**Test File:** `src/test/java/com/carebridge/backend/security/RemovePartnerContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** UC-118 §10 (PTR-006/bodiless — verify)

**Expected Result (PASS):** 401.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### PRC-TC-INT-001 — Full POST — row still exists, is_removed=true; atomicity

**Severity:** `CRITICAL`
**Feature Under Test:** `POST /admin/partner-content/{type}/{id}/remove` end to end + migration + transaction
**Test File:** `src/test/java/com/carebridge/backend/integration/RemovePartnerContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §5.2`, `V20260701130000__add_partner_content_removal.sql`

**Preconditions:** Testcontainer + Flyway **with `V20260701130000` applied**; seed `FX-1201`; admin JWT

**Test Steps:**
1. POST SERVICE remove with reason
2. Assert 200; re-fetch `partner_services` row directly
3. (Atomicity) force audit failure → assert `is_removed` rolled back to false

**Expected Result (PASS):**
- DB: `is_removed=true`, `removed_at`/`removed_by`/`removal_reason` set
- DB: `SELECT count(*) FROM partner_services WHERE service_id=<id>` == **1** (row NOT deleted — proves soft-remove)
- Forced failure: `is_removed` + audit both roll back together

**Current Status:** 🔴 Not written
**Implementation Note:** This is the first test requiring `V20260701130000` — if missing, the removal
columns won't exist and this test fails at the persistence layer (useful Red-phase signal).

---

### PRC-TC-INT-002 — Removed item excluded from catalog/list query (downstream filter)

**Severity:** `CRITICAL`
**Feature Under Test:** downstream `is_removed=false` filter (ADR-001 C6)
**Test File:** `src/test/java/com/carebridge/backend/integration/RemovePartnerContentIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS ADR-001 §Decision (downstream filter requirement)`

**Preconditions:** seed 2 services for the same partner: one removed (`FX-1203`), one not (`FX-1201`)

**Test Steps:** call the partner's service-listing query/endpoint (from UC-120, or a catalog list if one
exists) and assert the removed item is excluded from results.

**Expected Result (PASS):** removed item does NOT appear; non-removed item does.
**Expected Result (FAIL):** removed item still visible → moderation-removal bypass, the exact anti-pattern
ADR-001 C6 warns about, BLOCKING.

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This test may require a small downstream change to UC-120's list/read path
(add `WHERE is_removed = false`) — flag to the implementer if that query does not yet exist as of UC-125's
implementation; do not skip this test silently if the downstream query hasn't been updated.

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `PRC-TC-801`      | `RemovePartnerContentServiceImplTest.java`           | `[ ]`               | —                     | —                    |
| `PRC-TC-802`      | `RemovePartnerContentServiceImplTest.java`           | `[ ]`               | —                     | —                    |
| `PRC-TC-803`      | `RemovePartnerContentServiceImplTest.java`           | `[ ]`               | —                     | —                    |
| `PRC-TC-804`      | `RemovePartnerContentControllerTest.java`            | `[ ]`               | —                     | —                    |
| `PRC-TC-805`      | `RemovePartnerContentServiceImplTest.java`           | `[ ]`               | —                     | ★★ hard-delete guard |
| `PRC-TC-806`      | `RemovePartnerContentControllerTest.java`            | `[ ]`               | —                     | —                    |
| `PRC-TC-807`      | `RemovePartnerContentServiceImplTest.java`           | `[ ]`               | —                     | —                    |
| `PRC-TC-808`      | `RemovePartnerContentServiceImplTest.java`           | `[ ]`               | —                     | —                    |
| `PRC-TC-809`      | `RemovePartnerContentControllerSecurityTest.java`    | `[ ]`               | —                     | —                    |
| `PRC-TC-810`      | `RemovePartnerContentControllerSecurityTest.java`    | `[ ]`               | —                     | ★ anti-self-remove   |
| `PRC-TC-811`      | `RemovePartnerContentControllerSecurityTest.java`    | `[ ]`               | —                     | —                    |
| `PRC-TC-INT-001`  | `RemovePartnerContentIntegrationTest.java`           | `[ ]`               | —                     | ★ soft-remove + atomicity |
| `PRC-TC-INT-002`  | `RemovePartnerContentIntegrationTest.java`           | `[ ]`               | —                     | ★★ downstream filter |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

```java
// PartnerContentRemovalServiceImpl.java — Red Phase stub
@Override
public RemovalResponse remove(PartnerContentTargetType targetType, UUID targetId,
                              RemovalRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------- |
| `PRC-TC-801`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PRC-TC-803`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PRC-TC-805`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PRC-TC-809`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `PRC-TC-810`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —              |
| `PRC-TC-INT-001`  | migration absent OR throw stub              | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |
| `PRC-TC-INT-002`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —              |

**Red Gate Evidence:** Stub commit `___`; Tất cả FAIL? ☐ Yes → GATE-2 PASS; Log `Open`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] **ADR-001 (schema delta) approved by Tech Lead/DBA** — BLOCKING
- [ ] UC-120/121 deployed (entities/repos)
- [ ] Migration `V20260701130000__add_partner_content_removal.sql` applied on test environment
- [ ] ADR-004 (admin role) confirmed
- [ ] Fixtures FX-1201..FX-1207 prepared

### Exit Criteria (DoD)
- [ ] `./mvnw test -Dtest=RemovePartnerContentServiceImplTest` — all PASS
- [ ] `./mvnw test -Dtest=RemovePartnerContentControllerSecurityTest` — all PASS
- [ ] `./mvnw verify -Dtest=RemovePartnerContentIntegrationTest` — all PASS (Testcontainers, migration applied)
- [ ] PRC-TC-805: hard-delete NEVER invoked — VERIFIED (CRITICAL data-loss gate, highest priority in this spec)
- [ ] PRC-TC-INT-002: removed content excluded from downstream views — VERIFIED (CRITICAL moderation-bypass gate)
- [ ] PRC-TC-803: idempotent already-removed guard — VERIFIED (CRITICAL)
- [ ] PRC-TC-810: PARTNER cannot remove — VERIFIED (CRITICAL)
- [ ] PRC-TC-INT-001: atomicity + soft-remove persistence — VERIFIED (CRITICAL)
- [ ] No business logic in controller

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate — all FAIL with throw stub / missing migration
- [ ] Contract Existence — `./mvnw compile` clean (`RemovalRequest/Response`, `PartnerContentTargetType`, PTR-026/027/028/029, 4 new entity fields ×2)
- [ ] Props Isolation — factory used
- [ ] Oracle Source — cited (ADR + migration file)
- [ ] Migration verification — `\d partner_services` / `\d sponsored_campaigns` show the 4 new columns before RED-phase integration tests run

### Suspension Criteria
- ADR-001 (schema delta) not approved; migration not applied; ADR-004 (role) unresolved; CI broken

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/partner/
# Migration is forward-only (CLAUDE.md — never modify an applied migration). If removal columns must be
# dropped, write a NEW migration to DROP COLUMN; do NOT edit/delete V20260701130000 once applied.
# Emergency mitigation for a wrongly-removed item (Tech Lead sign-off required):
#   UPDATE partner_services SET is_removed=false, removed_at=NULL, removed_by=NULL, removal_reason=NULL
#   WHERE service_id = '<id>';  -- logged as a SECURITY_EVENT / audit entry
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                     | Check | Gate chặn |
| --------- | ------------------------- | ------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-002/ADR-003                                      | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | PRC-TC-801..808 PASS với throw stub                                            | `[x]`   | G-2 ★       |
| AP-AI-002 | Hard Delete              | Không có TC assert delete() never called (thiếu PRC-TC-805)                     | `[x]`   | G-2 ★★      |
| AP-AI-003 | Moderation Bypass        | Không có TC assert removed item excluded downstream (thiếu PRC-TC-INT-002)      | `[x]`   | G-1 ★★      |
| AP-AI-004 | Layer Violation           | TC verify Controller gọi repository trực tiếp                                  | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | TC import class không có trong §8                                              | `[x]`   | G-3         |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào trong bản thân spec này → approved-for-RED-phase

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0) — Status: Draft. Admin soft-remove over UC-120/121 — the only Cluster B UC with a
schema delta (`V20260701130000`). Hard-delete guard (PRC-TC-805) and downstream-filter guard (PRC-TC-INT-002)
are the two highest-severity gates in this batch — a failure of either means real data loss or a moderation
bypass in production. Entry Criteria requires Tech Lead/DBA sign-off on ADR-001 before RED phase.*
