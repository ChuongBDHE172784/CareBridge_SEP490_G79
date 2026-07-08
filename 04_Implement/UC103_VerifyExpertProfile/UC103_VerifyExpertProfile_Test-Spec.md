# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-103 Verify Expert Profile — Test Specification

**Document ID:** `CB-EXPGOV-TDD-103`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.5 (UC-103, lines 1092-1111)
- `04_Implement/UC103_VerifyExpertProfile/UC103_VerifyExpertProfile_TDS.md` (CB-EXPGOV-IMP-103) — Technical Specification
- `CLAUDE.md` — BR-RBAC, audit requirements for expert/moderation/safety workflows

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.test.tsx`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo Test-Spec cho UC-103 |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-103` |
| **Module** | `VerifyExpertProfile — Bounded Context: expert` |
| **Spec gốc** | `CB-EXPGOV-IMP-103` |
| **Priority** | 🔴 P0 (High per SRS) |
| **Sprint** | `S3 Cross-Domain Integration — TV4-Lâm` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA`, `BR-RBAC` |
| **Upstream Dependencies** | `expert_profiles`/`expert_credentials` rows seeded by UC-87/UC-89 |
| **Downstream Consumers** | UC-104 Revoke Expert Badge (operates on VERIFIED profiles), UC-112 View Expert Dashboard (aggregates counts) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXPGOV-IMP-103 §17`, ADR-EXP-201/202/203 |
| **Constraints Injected** | C1-C8 per TDS §17.1 (JWT-derived adminUserId, SYSTEM_ADMIN-only, 4-value `ExpertVerificationStatus` enum, policy-centralized transitions, conditional note requirement, `verified_at`/`verified_by` set on every decision, reuse existing `AuditAction.EXPERT_VERIFICATION`, no new migration) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS generic template says "actor enters information, selects an item, or confirms the requested action" — no explicit list of 3 decision types | TDS §3 ADR-EXP-201 derives exactly 3 actions (`APPROVE`/`REJECT`/`REQUEST_MORE_INFO`) from SRS Description field wording "Approves, rejects, or requests additional... information" | Tests assert exactly these 3 `VerificationDecisionAction` enum values are accepted; no 4th action exists |
| L2 | `expert_profiles.verification_status` has no DB CHECK constraint (verified directly in `V1__init_schema.sql` lines 786-800, 1799-1809 — no CHECK clause) | Enum enforcement is application-layer only via `ExpertVerificationPolicy` | Tests assert invalid transitions are rejected at the SERVICE layer (EXPV-102), not relying on any DB constraint |
| L3 | UC-88's prior-batch TDS (`UC88_UpdateExpertProfile_TDS.md` §18 OI-5) flagged that `UC87_CreateExpertProfile_TDS.md` uses a fictitious schema (`account_id`, `display_name`, `status` enum `DRAFT/PENDING_VERIFICATION/VERIFIED/SUSPENDED`) that does NOT match `V1__init_schema.sql` | Real schema confirmed independently in this Test-Spec's own research pass: `expert_profile_id`, `user_id`, `verification_status varchar(30) DEFAULT 'PENDING'` | Test data factories (`ExpertGovernanceTestFactory`) use ONLY real column names; never reference `account_id`/`display_name`/UC-87's fictitious enum |
| L4 | SRS POST-3 says "sensitive actions recorded for audit... where required" (generic) — no specific `AuditAction` enum value named | `AuditAction.java` already has `EXPERT_VERIFICATION` (line 13) as an existing, reusable value | Tests assert `AuditService.log()` is called with `AuditAction.EXPERT_VERIFICATION`, not a newly-invented value |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
VerifyExpertProfile bao gồm các layer:
├── Domain (ExpertVerificationPolicy — pure logic, no deps)
├── Services (ExpertVerificationServiceImpl — mock JPA Repository + AuditService với Mockito)
├── Controller (ExpertVerificationController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL với @SpringBootTest)
└── Web (ExpertVerificationQueuePage/DetailPage — Vitest + Testing Library, mock API client)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-103` (§3.2.2.5, lines 1092-1111) | Approve/Reject/Request-info actions; PRE-3 auth; POST-3 audit; E1/E2/E3 exceptions |
| `ADR-EXP-201` | State machine transitions; invalid transition rejection |
| `ADR-EXP-202` | Conditional note requirement; `verified_at`/`verified_by` set on every decision; domain event + audit emission |
| `ADR-EXP-203` | SYSTEM_ADMIN-only authorization; no MODERATOR access |
| `BR-RBAC` | Role-scoped access enforcement |
| `V1__init_schema.sql` (lines 786-815) | Real column names/types for persistence assertions |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin approves a PENDING profile | `ExpertVerificationServiceImpl.decide()` | `EXPV-TC-001` |
| TC-COND-002 | Admin rejects a PENDING profile with note | `ExpertVerificationServiceImpl.decide()` | `EXPV-TC-002` |
| TC-COND-003 | Admin requests more info with note | `ExpertVerificationServiceImpl.decide()` | `EXPV-TC-003` |
| TC-COND-004 | Reject/request-info without note | `ExpertVerificationPolicy.assertNoteRequiredIfNeeded()` | `EXPV-TC-004`, `EXPV-TC-005` |
| TC-COND-005 | Invalid transition (already VERIFIED/REJECTED) | `ExpertVerificationPolicy.assertTransitionAllowed()` | `EXPV-TC-006`, `EXPV-TC-007` |
| TC-COND-006 | Unknown expertProfileId | `ExpertVerificationServiceImpl.decide()`/`getDetail()` | `EXPV-TC-008` |
| TC-COND-007 | Role-based access (SYSTEM_ADMIN vs others) | `ExpertVerificationController` + Spring Security | `EXPV-TC-009` to `EXPV-TC-013` |
| TC-COND-008 | Audit + domain event emitted on decision | `ExpertVerificationServiceImpl.decide()` | `EXPV-TC-014` |
| TC-COND-009 | Queue list filter by status | `ExpertVerificationServiceImpl.listQueue()` | `EXPV-TC-015` |
| TC-COND-010 | Web: decision form requires note conditionally | `ExpertVerificationDetailPage.tsx` | `EXPV-TC-WEB-001`, `EXPV-TC-WEB-002` |
| TC-COND-011 | Full integration: PENDING → REJECTED via API | `ExpertVerificationController` E2E | `EXPV-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `decision` enum values (valid 3 vs invalid strings) | Confirms only APPROVE/REJECT/REQUEST_MORE_INFO accepted |
| Boundary Value Analysis | `note` length (0, 1, 2000, 2001 chars) | Confirms `@Size(max=2000)` boundary |
| State Transition Testing | `ExpertVerificationStatus` FSM (§ADR-EXP-201) | Core of this UC — every edge and non-edge must be tested |
| Error Guessing | Role bypass attempts, duplicate decision calls | Security/idempotency assurance |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-EXPV-001` | DB seed | `expert_profiles` row with `verification_status='PENDING'` | Happy path approve/reject/request-info |
| `FX-EXPV-002` | DB seed | `expert_profiles` row with `verification_status='VERIFIED'` | Invalid-transition test (already decided) |
| `FX-EXPV-003` | DB seed | `expert_profiles` row with `verification_status='REJECTED'` | Invalid-transition test (terminal state) |
| `FX-EXPV-004` | DB seed | 2x `expert_credentials` rows linked to `FX-EXPV-001`'s `expert_profile_id` | Detail view credential list assertion |
| `FX-EXPV-005` | JWT | `{ sub: 'admin-001', role: 'SYSTEM_ADMIN' }` | Auth context for admin actions |
| `FX-EXPV-006` | JWT | `{ sub: 'expert-001', role: 'EXPERT' }` | Negative auth test |
| `FX-EXPV-007` | JWT | `{ sub: 'mod-001', role: 'MODERATOR' }` | Negative auth test (no MODERATOR access, ADR-EXP-203) |

---

## 4. Test Case Specification

> **TC ID format:** `EXPV-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ExpertGovernanceTestFactory.java — SHARED across UC-103/UC-104 test suites
// ═══════════════════════════════════════════════════════════

class ExpertGovernanceTestFactory {

    static ExpertProfile makePendingProfile() {
        return makePendingProfile(p -> {});
    }

    static ExpertProfile makePendingProfile(Consumer<ExpertProfile> overrides) {
        ExpertProfile profile = new ExpertProfile();
        profile.setExpertProfileId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        profile.setUserId(UUID.fromString("00000000-0000-0000-0000-0000000000f1"));
        profile.setSpecialty("Obstetrics");
        profile.setProfessionalTitle("BS. CKI");
        profile.setExperienceYears((short) 5);
        profile.setWorkplace("Bệnh viện Từ Dũ");
        profile.setConsultationScope("Tư vấn thai kỳ");
        profile.setVerificationStatus(ExpertVerificationStatus.PENDING);
        profile.setVerifiedAt(null);
        profile.setVerifiedBy(null);
        profile.setCreatedAt(Instant.parse("2026-06-30T09:00:00Z"));
        profile.setUpdatedAt(Instant.parse("2026-06-30T09:00:00Z"));
        overrides.accept(profile);
        return profile;
    }

    static ExpertProfile makeVerifiedProfile() {
        return makePendingProfile(p -> {
            p.setVerificationStatus(ExpertVerificationStatus.VERIFIED);
            p.setVerifiedAt(Instant.parse("2026-06-25T09:00:00Z"));
            p.setVerifiedBy(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
        });
    }

    static ExpertProfile makeRejectedProfile() {
        return makePendingProfile(p -> p.setVerificationStatus(ExpertVerificationStatus.REJECTED));
    }

    static ExpertCredential makeCredential(UUID expertProfileId) {
        ExpertCredential credential = new ExpertCredential();
        credential.setCredentialId(UUID.randomUUID());
        credential.setExpertProfileId(expertProfileId);
        credential.setCredentialType("MEDICAL_LICENSE");
        credential.setCredentialNumber("VN-12345");
        credential.setIssuer("Bộ Y Tế");
        credential.setIssuedDate(LocalDate.of(2020, 1, 1));
        credential.setExpiryDate(LocalDate.of(2030, 1, 1));
        credential.setFileUrl("https://storage.example.com/cred-1.pdf");
        credential.setReviewStatus("PENDING");
        return credential;
    }

    static VerificationDecisionRequest makeApproveRequest() {
        VerificationDecisionRequest req = new VerificationDecisionRequest();
        req.setDecision(VerificationDecisionAction.APPROVE);
        return req;
    }

    static VerificationDecisionRequest makeRejectRequest(String note) {
        VerificationDecisionRequest req = new VerificationDecisionRequest();
        req.setDecision(VerificationDecisionAction.REJECT);
        req.setNote(note);
        return req;
    }

    static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
}
```

---

### EXPV-TC-001 — Approve decision transitions PENDING → VERIFIED

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertVerificationServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertVerificationServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-EXP-201 §Decision table` — `PENDING → VERIFIED` on APPROVE

**Preconditions:**
- `FX-EXPV-001` seeded (mocked repository returns `makePendingProfile()`)

**Test Steps:**
1. Arrange: `expertProfileRepository.findById(id)` returns `Optional.of(makePendingProfile())`
2. Act: call `service.decide(id, makeApproveRequest(), ADMIN_USER_ID)`
3. Assert: returned DTO has `verificationStatus == VERIFIED`; `expertProfileRepository.save()` called with entity where `verifiedAt != null` and `verifiedBy.equals(ADMIN_USER_ID)`

**Expected Result (PASS):** Status transitions to `VERIFIED`; `verifiedAt`/`verifiedBy` populated.
**Expected Result (FAIL):** Status unchanged, or `verifiedBy` null/mismatched.

**Current Status:** 🔴 Not written
**Implementation Note:** Must call `ExpertVerificationPolicy.assertTransitionAllowed(PENDING, APPROVE)` before mutation.

---

### EXPV-TC-002 — Reject decision transitions PENDING → REJECTED with note

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertVerificationServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertVerificationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-EXP-201`, `ADR-EXP-202` (note required on REJECT)

**Preconditions:** `FX-EXPV-001` seeded.

**Test Steps:**
1. Arrange: mocked repo returns `makePendingProfile()`
2. Act: `service.decide(id, makeRejectRequest("Certificate expired"), ADMIN_USER_ID)`
3. Assert: result `verificationStatus == REJECTED`; saved entity's `verifiedBy == ADMIN_USER_ID`

**Expected Result (PASS):** Status = `REJECTED`, `verifiedAt`/`verifiedBy` set (per ADR-EXP-202 — set on EVERY decision, not just APPROVE).
**Expected Result (FAIL):** Status unchanged, or `verifiedAt`/`verifiedBy` left null on REJECT.

**Current Status:** 🔴 Not written

---

### EXPV-TC-003 — Request-more-info decision transitions PENDING → NEEDS_MORE_INFO with note

**Severity:** `HIGH`
**Feature Under Test:** `ExpertVerificationServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertVerificationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-EXP-201 §Decision table`

**Test Steps:**
1. Act: `service.decide(id, {decision: REQUEST_MORE_INFO, note: "Missing license photo"}, ADMIN_USER_ID)`
2. Assert: result `verificationStatus == NEEDS_MORE_INFO`

**Expected Result (PASS):** Status = `NEEDS_MORE_INFO`.
**Expected Result (FAIL):** Status stuck at `PENDING` or wrong value.

**Current Status:** 🔴 Not written

---

### EXPV-TC-004 — Reject without note is rejected (EXPV-101)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertVerificationPolicy.assertNoteRequiredIfNeeded()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertVerificationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-EXP-202`, `TDS §10 EXPV-101`

**Test Steps:**
1. Act: `policy.assertNoteRequiredIfNeeded(REJECT, null)` and `policy.assertNoteRequiredIfNeeded(REJECT, "  ")` (blank)
2. Assert: both throw `ValidationException` with code `EXPV-101`

**Expected Result (PASS):** `ValidationException("EXPV-101")` thrown for both null and blank note.
**Expected Result (FAIL):** No exception thrown, or wrong error code.

**Current Status:** 🔴 Not written

---

### EXPV-TC-005 — Request-more-info without note is rejected (EXPV-101)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertVerificationPolicy.assertNoteRequiredIfNeeded()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertVerificationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-EXP-202`

**Test Steps:**
1. Act: `policy.assertNoteRequiredIfNeeded(REQUEST_MORE_INFO, null)`
2. Assert: throws `ValidationException("EXPV-101")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** No exception; note remains optional incorrectly.

**Current Status:** 🔴 Not written

---

### EXPV-TC-006 — Approve on already-VERIFIED profile rejected (EXPV-102)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertVerificationPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertVerificationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-EXP-201` — `VERIFIED` has no outgoing edge via UC-103

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(VERIFIED, APPROVE)`
2. Assert: throws `ConflictException("EXPV-102")`

**Expected Result (PASS):** Exception thrown; no repository save invoked in the service-level equivalent test.
**Expected Result (FAIL):** Silently returns a new status, or allows the transition.

**Current Status:** 🔴 Not written
**Implementation Note:** Guards against re-approving/re-processing a decided profile — critical for UC-104 boundary integrity (only UC-104 may act on VERIFIED).

---

### EXPV-TC-007 — Approve on REJECTED profile rejected (EXPV-102, no auto re-entry)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertVerificationPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertVerificationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-EXP-201` — REJECTED has no automatic re-entry edge

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(REJECTED, APPROVE)`
2. Assert: throws `ConflictException("EXPV-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** Transition silently allowed, violating the documented state machine invariant.

**Current Status:** 🔴 Not written

---

### EXPV-TC-008 — Decide on unknown expertProfileId returns 404 (EXPV-104)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertVerificationServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertVerificationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §10 EXPV-104`, `TDS §8.1 @throws NotFoundException`

**Test Steps:**
1. Arrange: `expertProfileRepository.findById(unknownId)` returns `Optional.empty()`
2. Act/Assert: `service.decide(unknownId, makeApproveRequest(), ADMIN_USER_ID)` throws `NotFoundException("EXPV-104")`

**Expected Result (PASS):** `NotFoundException("EXPV-104")` thrown; repository `save()` never called.
**Expected Result (FAIL):** NPE or wrong error code.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### EXPV-TC-009 — SYSTEM_ADMIN can call verification-decisions endpoint

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertVerificationController.decide()` + Spring Security chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertVerificationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-EXPV-005` (SYSTEM_ADMIN JWT).

**Test Steps (Attack Simulation):** N/A — positive case.
1. Send `POST /api/v1/admin/expert-profiles/{id}/verification-decisions` with SYSTEM_ADMIN JWT and valid body
2. Assert `200 OK`

**Expected Result (PASS):** `200 OK`.
**Expected Result (FAIL):** `403 Forbidden` incorrectly returned for a valid admin.

**Current Status:** 🔴 Not written

---

### EXPV-TC-010 — EXPERT role forbidden from verification-decisions endpoint

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertVerificationController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertVerificationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-EXPV-006` (EXPERT JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../verification-decisions` with EXPERT JWT
2. Assert `403 Forbidden`, body `error.code == "EXPV-103"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK` — an expert could self-verify or tamper with any profile.

**Current Status:** 🔴 Not written

---

### EXPV-TC-011 — MODERATOR role forbidden from verification-decisions endpoint (ADR-EXP-203)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`, `ADR-EXP-203`
**Feature Under Test:** `ExpertVerificationController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertVerificationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `SRS §3.2.2.5 Primary Actor field ("System Admin", Secondary Actors: "None")`, `ADR-EXP-203`

**Preconditions:** `FX-EXPV-007` (MODERATOR JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../verification-decisions` with MODERATOR JWT
2. Assert `403 Forbidden`, body `error.code == "EXPV-103"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`. This is the KEY test distinguishing UC-103 from moderation-adjacent use cases — MODERATOR must NOT be treated as equivalent to SYSTEM_ADMIN here.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK` — privilege scope creep beyond SRS.

**Current Status:** 🔴 Not written

---

### EXPV-TC-012 — Unauthenticated request rejected (401)

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertVerificationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `SRS E1`

**Test Steps (Attack Simulation):**
1. Send `POST .../verification-decisions` with no `Authorization` header
2. Assert `401 Unauthorized`

**Expected Result (PASS):** `401 Unauthorized`.
**Expected Result (FAIL):** Request processed without authentication.

**Current Status:** 🔴 Not written

---

### EXPV-TC-013 — GET queue/detail endpoints also SYSTEM_ADMIN-only

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `ExpertVerificationController.listQueue()` / `getDetail()`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertVerificationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Test Steps (Attack Simulation):**
1. Send `GET /api/v1/admin/expert-profiles` with MOTHER JWT
2. Assert `403 Forbidden`

**Expected Result (PASS):** `403 Forbidden` — read-only queue/detail views are equally protected, not just the write endpoint.
**Expected Result (FAIL):** `200 OK` — leaks other experts' PII/credentials to a non-admin.

**Current Status:** 🔴 Not written

---

### EXPV-TC-014 — Successful decision emits audit log and domain event

**Severity:** `CRITICAL`
**Legal:** `PDPA`, `BR-CONSULTATION` (auditable lifecycle)
**Feature Under Test:** `ExpertVerificationServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertVerificationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-EXP-202`, `AuditAction.EXPERT_VERIFICATION` (existing enum value)

**Test Steps:**
1. Act: `service.decide(id, makeApproveRequest(), ADMIN_USER_ID)`
2. Assert: `verify(auditService).log(eq(AuditAction.EXPERT_VERIFICATION), eq(ADMIN_USER_ID), eq("expert_profiles"), eq(id.toString()), any())`
3. Assert: `verify(applicationEventPublisher).publishEvent(argThat(e -> e instanceof ExpertProfileVerified))`

**Expected Result (PASS):** Both audit log call and domain event publish occur exactly once.
**Expected Result (FAIL):** Missing audit call (PDPA gap) or missing/wrong event type.

**Current Status:** 🔴 Not written

---

### EXPV-TC-015 — Queue list filters by status

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertVerificationServiceImpl.listQueue()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertVerificationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §8.1 listQueue()`, `SRS AF3 (optional filters)`

**Test Steps:**
1. Arrange: `expertProfileRepository.findByVerificationStatus(PENDING, pageable)` returns a page with one item
2. Act: `service.listQueue(PENDING, pageable)`
3. Assert: returned page contains exactly the mocked item, mapped via `ExpertVerificationMapper.toQueueItem()`

**Expected Result (PASS):** Filtered results returned correctly mapped.
**Expected Result (FAIL):** Wrong repository method invoked, or unmapped/raw entity leaked to DTO.

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### EXPV-TC-WEB-001 — Decision form disables submit until note provided for REJECT

**Severity:** `HIGH`
**Feature Under Test:** `ExpertVerificationDetailPage.tsx` (decision form)
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/ExpertVerificationDetailPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §11.3 Chặng 5 verificationDecisionSchema.refine()`

**Test Steps:**
1. Render `ExpertVerificationDetailPage` with a PENDING profile fixture
2. Select decision = REJECT, leave note empty, attempt submit
3. Assert: Zod validation error shown, `submitVerificationDecision` API call NOT invoked

**Expected Result (PASS):** Form blocks submission; error message rendered for `note` field.
**Expected Result (FAIL):** Form submits with empty note, mismatching backend's EXPV-101 rule.

**Current Status:** 🔴 Not written

---

### EXPV-TC-WEB-002 — Decision form allows submit with empty note for APPROVE

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertVerificationDetailPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/ExpertVerificationDetailPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §11.3 Chặng 5`

**Test Steps:**
1. Render page, select decision = APPROVE, leave note empty
2. Submit
3. Assert: `submitVerificationDecision` called with `{decision: "APPROVE"}` (no note required)

**Expected Result (PASS):** Submission succeeds without note.
**Expected Result (FAIL):** Form incorrectly blocks APPROVE without note.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### EXPV-TC-INT-001 — Full flow: PENDING profile rejected via real API + DB

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /api/v1/admin/expert-profiles/{id}/verification-decisions → DB update`
**Test File:** `src/test/java/com/carebridge/backend/expert/ExpertVerificationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start
- Seed: one `users` row (role SYSTEM_ADMIN), one `users` row (role EXPERT), one `expert_profiles` row with `verification_status='PENDING'` referencing the EXPERT user

**Test Steps:**
1. Seed data as above
2. `POST /api/v1/admin/expert-profiles/{id}/verification-decisions` with SYSTEM_ADMIN JWT, body `{decision:"REJECT", note:"Missing document"}`
3. Assert response `200`, `data.verificationStatus == "REJECTED"`
4. Query DB directly for the row

**Expected Result (PASS):**
- API response `200` with `verificationStatus: "REJECTED"`
- DB row: `verification_status = 'REJECTED'`, `verified_at IS NOT NULL`, `verified_by = <admin user_id>`

**Expected Result (FAIL):** DB row not updated, or updated with wrong values.

**DB Assertion:**
```java
ExpertProfile record = expertProfileRepository.findById(savedId).orElseThrow();
assertThat(record.getVerificationStatus()).isEqualTo(ExpertVerificationStatus.REJECTED);
assertThat(record.getVerifiedBy()).isEqualTo(adminUserId);
assertThat(record.getVerifiedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXPV-TC-001` | `ExpertVerificationServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-002` | `ExpertVerificationServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-003` | `ExpertVerificationServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-004` | `ExpertVerificationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-005` | `ExpertVerificationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-006` | `ExpertVerificationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-007` | `ExpertVerificationPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-008` | `ExpertVerificationServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-009` | `ExpertVerificationControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-010` | `ExpertVerificationControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-011` | `ExpertVerificationControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-012` | `ExpertVerificationControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-013` | `ExpertVerificationControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-014` | `ExpertVerificationServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-015` | `ExpertVerificationServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-WEB-001` | `ExpertVerificationDetailPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-WEB-002` | `ExpertVerificationDetailPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EXPV-TC-INT-001` | `ExpertVerificationIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ExpertVerificationServiceImpl implements IExpertVerificationService {

    @Override
    public Page<ExpertVerificationQueueItemResponse> listQueue(ExpertVerificationStatus filter, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertVerificationDetailResponse getDetail(UUID expertProfileId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertVerificationDetailResponse decide(UUID expertProfileId, VerificationDecisionRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class ExpertVerificationPolicy {
    public ExpertVerificationStatus assertTransitionAllowed(ExpertVerificationStatus current, VerificationDecisionAction action) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertNoteRequiredIfNeeded(VerificationDecisionAction action, String note) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXPV-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-009` to `013` | `403/401 forced by missing controller wiring` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-WEB-001/002` | `component not implemented / API not wired` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPV-TC-INT-001` | `500 from stub exception` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXPGOV-IMP-103` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] `expert` package skeleton confirmed present (`.gitkeep` in all layers, verified)
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] `npm run test:run` — web tests xanh
- [ ] Test coverage ≥ 80% lines cho `ExpertVerificationServiceImpl`, `ExpertVerificationPolicy`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Tất cả 4 trạng thái `ExpertVerificationStatus` và mọi transition edge trong ADR-EXP-201 có test case tương ứng

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` không lỗi
- [ ] **Props Isolation** — mọi test dùng `ExpertGovernanceTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (ADR/SRS/schema)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-87/UC-89 chưa cung cấp cách seed `expert_profiles` PENDING rows cho integration test (mitigated: integration test seeds directly via repository, không phụ thuộc UC-87 API)
- Phát hiện lỗi kiến trúc mới cần Tech Lead review

---

## 7. Rollback Plan

```bash
# Revert implementation files (no migration exists for this UC)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/

# Gap vẫn OPEN → giữ nguyên Status: Draft trong UC103_VerifyExpertProfile_TDS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-EXP-201/202/203 | ☑ Not detected — every TC cites an Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Pending Red Gate run | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes MODERATOR access without ADR | ☑ Not detected — EXPV-TC-011 explicitly tests MODERATOR denial per ADR-EXP-203 | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller has business logic | ☑ Not detected — transition logic tested against `ExpertVerificationPolicy`, not controller | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports service/type not in TDS §8 | ☑ Not detected — all types match TDS §8.1/8.2/8.3 | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-authoring → TDD spec approved for Red Gate execution
- [ ] AP-AI-002 (Green-from-Birth) check pending actual Red Gate run once stubs are committed

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending Tech Lead / TV4-Lâm review and Red Gate execution.*
