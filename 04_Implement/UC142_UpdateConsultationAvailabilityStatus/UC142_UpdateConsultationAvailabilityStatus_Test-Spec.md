# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC142 — Update Consultation Availability Status

**Document ID:** `CB-CONSULTATION-TDD-142`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending — not required (Internal data classification, see TDS §1)`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (`expert_availability` L817-826)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.5.1 (L3531-3548) — Functional requirement
- `04_Implement/UC142_UpdateConsultationAvailabilityStatus/UC142_UpdateConsultationAvailabilityStatus_TDS.md` — Technical Design Specification (this feature's TDS)
- `04_Implement/UC95_ManageConsultationSession/UC95_ManageConsultationSession_TDS.md` — sibling ADR precedent (app-level enum enforcement)
- CLAUDE.md — RBAC, audit, smallest-scoped-change rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` / `flutter test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC142 |

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
| **Feature / Gap ID** | `UC142` |
| **Module** | `Consultation — Expert Availability` |
| **Spec gốc** | `CB-CONSULTATION-IMP-142` |
| **Priority** | 🟡 P2 (Medium, per SRS) |
| **Sprint** | `S3 Cross-Domain Integration — TV4-Lâm` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | `expert_profiles.verification_status` (Verify Expert Profile, 3.2.2.5) |
| **Downstream Consumers** | `Book Private Consultation` (3.3.1.52), `Respond to Consultation Request` (UC143) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC142_TDS §17` (C1-C5), `ADR-AVAIL-001/002/003/004` |
| **Constraints Injected** | Online-status = derived aggregate (no new column); ownership+verification gate; overlap rejection; JWT-only identity; controller thin-layer rule |
| **Model** | `Claude (Technical Architect + Test Designer role)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS says "manage online status" implying a single toggle field | `expert_availability` has no `is_online` column — it is a time-slot table (`start_at`/`end_at`/`channel_type`/`status`) | Tests assert `toggleOnlineStatus()` mutates **all current-window rows**, not a nonexistent boolean column (ADR-AVAIL-001) |
| L2 | SRS silent on what happens with zero current-window rows | ADR-AVAIL-004 proposes a "walk-in" row creation (`now()` to `now()+2h`) — Proposed, Open | Tests assert walk-in row creation behavior is exercised but the exact 2h duration is asserted against the TDS-documented constant, flagged with a comment referencing the Open item |
| L3 | Schema `status` column has no CHECK constraint | Enum enforcement is application-level only (`ExpertAvailabilityPolicy`) | Tests assert invalid status strings are rejected by policy/service, not by a DB constraint (no DB-level test for invalid enum) |
| L4 | SRS E2 says "conflicting data is rejected" without specifying the exact conflict rule | ADR-AVAIL-003 defines overlap = same `expert_profile_id` + `channel_type` + intersecting `[start_at, end_at]` | Tests assert overlap rejection ONLY within the same channel; a non-overlapping different-channel window at the same time is accepted |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Consultation - Expert Availability module bao gồm các layer:
├── Domain (ExpertAvailabilityEntity — pure JPA mapping, no logic)
├── Policy (ExpertAvailabilityPolicy — mock repository với Mockito)
├── Service (ExpertAvailabilityService — mock repository/policy với Mockito)
├── Controller (ExpertAvailabilityController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-142` (§3.3.5.1, L3531-3548) | Online status/channel management, E1/E2/E3 exception flows |
| `ADR-AVAIL-001` | Bulk toggle over current-window rows, no new column |
| `ADR-AVAIL-002` | Ownership + verification gate |
| `ADR-AVAIL-003` | Overlap validation, idempotent PATCH |
| `ADR-AVAIL-004` | Walk-in row creation when no current window exists |
| `BR-RBAC` | Role/ownership scoping |
| `BR-CONSULTATION` | Auditable lifecycle state |
| `CB-CONSULTATION-IMP-142` §8, §9, §10, §16 | Interface contracts, API shapes, error codes, auth matrix |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified owner toggles online — current window exists | `ExpertAvailabilityService.toggleOnlineStatus()` | `AVAIL-TC-001` |
| TC-COND-002 | Verified owner toggles online — no current window (walk-in) | `ExpertAvailabilityService.toggleOnlineStatus()` | `AVAIL-TC-002` |
| TC-COND-003 | Verified owner toggles offline | `ExpertAvailabilityService.toggleOnlineStatus()` | `AVAIL-TC-003` |
| TC-COND-004 | Idempotent repeat toggle (same target state) | `ExpertAvailabilityService.toggleOnlineStatus()` | `AVAIL-TC-004` |
| TC-COND-005 | Unverified expert attempts toggle | `ExpertAvailabilityPolicy.assertIsOwnerAndVerified()` | `AVAIL-TC-005` |
| TC-COND-006 | Non-owner expert attempts toggle (IDOR) | `ExpertAvailabilityPolicy.assertIsOwnerAndVerified()` | `AVAIL-TC-006` |
| TC-COND-007 | Create window — happy path, no overlap | `ExpertAvailabilityService.createWindow()` | `AVAIL-TC-007` |
| TC-COND-008 | Create window — overlaps existing same-channel window | `ExpertAvailabilityPolicy.validateNoOverlap()` | `AVAIL-TC-008` |
| TC-COND-009 | Create window — same time, different channel (no conflict) | `ExpertAvailabilityPolicy.validateNoOverlap()` | `AVAIL-TC-009` |
| TC-COND-010 | Create window — endAt <= startAt validation | `CreateAvailabilityWindowRequest` bean validation | `AVAIL-TC-010` |
| TC-COND-011 | Get status summary — aggregates multiple current channels | `ExpertAvailabilityService.getStatusSummary()` | `AVAIL-TC-011` |
| TC-COND-012 | List own windows — excludes other experts' rows | `ExpertAvailabilityRepository.findByExpertProfileId()` | `AVAIL-TC-012` |
| TC-COND-013 | Controller — unauthenticated request | `ExpertAvailabilityController` (Spring Security filter) | `AVAIL-TC-SEC-001` |
| TC-COND-014 | Controller — non-EXPERT role attempts access | `ExpertAvailabilityController` | `AVAIL-TC-SEC-002` |
| TC-COND-015 | Integration — full create→toggle→summary flow | End-to-end | `AVAIL-TC-INT-001` |
| TC-COND-016 | Integration — overlap rejected at DB-backed service layer | End-to-end | `AVAIL-TC-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `channelType` (valid CHAT/VOICE_CALL/VIDEO_CALL vs. invalid string) | Enum boundary per ADR-AVAIL-001 §5.3 gap note 3 |
| Boundary Value Analysis | `endAt` == `startAt`, `endAt` = `startAt - 1s` | Time-window validity boundary |
| State Transition Testing | `AVAILABLE ↔ UNAVAILABLE`, `→ EXPIRED` (§6.4 state machine) | Core FSM under test |
| Error Guessing | IDOR via crafted `availabilityId` belonging to another expert | Security — ownership bypass attempt |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `expert_profiles{id: EXPERT_A, verification_status: 'VERIFIED'}` | Happy path owner |
| `FX-002` | DB seed | `expert_profiles{id: EXPERT_B, verification_status: 'PENDING'}` | Unverified rejection case |
| `FX-003` | DB seed | `expert_availability{expertProfileId: EXPERT_A, channelType: 'CHAT', status: 'AVAILABLE', startAt: now-1h, endAt: now+1h}` | Current-window toggle case |
| `FX-004` | DB seed | `expert_availability{expertProfileId: EXPERT_A, channelType: 'CHAT', status: 'AVAILABLE', startAt: now+2h, endAt: now+4h}` | Overlap-test target window |
| `FX-005` | JWT | `{ sub: EXPERT_A.userId, role: 'EXPERT' }` | Auth context — owner |
| `FX-006` | JWT | `{ sub: 'other-user-id', role: 'EXPERT' }` | Auth context — non-owner (IDOR attempt) |
| `FX-007` | JWT | `{ sub: 'mother-user-id', role: 'MOTHER' }` | Auth context — wrong role |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// ExpertAvailabilityTestFactory.java
class ExpertAvailabilityTestFactory {

    static final UUID EXPERT_A_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID EXPERT_A_USER_ID    = UUID.fromString("00000000-0000-0000-0000-0000000000A2");
    static final UUID EXPERT_B_USER_ID    = UUID.fromString("00000000-0000-0000-0000-0000000000B2");

    static ExpertProfileEntity makeVerifiedExpertProfile() {
        ExpertProfileEntity profile = new ExpertProfileEntity();
        profile.setExpertProfileId(EXPERT_A_PROFILE_ID);
        profile.setUserId(EXPERT_A_USER_ID);
        profile.setVerificationStatus("VERIFIED");
        return profile;
    }

    static ExpertProfileEntity makeUnverifiedExpertProfile() {
        ExpertProfileEntity profile = makeVerifiedExpertProfile();
        profile.setVerificationStatus("PENDING");
        return profile;
    }

    static ExpertAvailabilityEntity makeAvailabilityWindow(Consumer<ExpertAvailabilityEntity> overrides) {
        ExpertAvailabilityEntity window = new ExpertAvailabilityEntity();
        window.setAvailabilityId(UUID.randomUUID());
        window.setExpertProfileId(EXPERT_A_PROFILE_ID);
        window.setStartAt(Instant.now().minusSeconds(3600));
        window.setEndAt(Instant.now().plusSeconds(3600));
        window.setChannelType("CHAT");
        window.setStatus("AVAILABLE");
        overrides.accept(window);
        return window;
    }

    static CreateAvailabilityWindowRequest makeCreateRequest(Consumer<CreateAvailabilityWindowRequest> overrides) {
        CreateAvailabilityWindowRequest request = new CreateAvailabilityWindowRequest();
        request.setStartAt(Instant.now().plusSeconds(7200));
        request.setEndAt(Instant.now().plusSeconds(14400));
        request.setChannelType("CHAT");
        overrides.accept(request);
        return request;
    }
}
```

---

### AVAIL-TC-001 — Toggle online: current window exists is set to AVAILABLE

**Severity:** `HIGH`
**Feature Under Test:** `ExpertAvailabilityService.toggleOnlineStatus()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertAvailabilityServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-AVAIL-001 §Decision` (bulk toggle over current-window rows)

**Preconditions:**
- `FX-001` (verified Expert A), `FX-003` (current window, status=UNAVAILABLE via override)

**Test Steps:**
1. Arrange: mock repository returns 1 current window with `status='UNAVAILABLE'`
2. Act: call `toggleOnlineStatus(EXPERT_A_USER_ID, true)`
3. Assert: repository `save()` called with `status='AVAILABLE'`; response `online=true`

**Expected Result (PASS):**
- `ExpertAvailabilityStatusSummaryResponse.online == true`
- `activeChannels` contains `"CHAT"`

**Expected Result (FAIL):**
- `online` remains `false`, or repository never invoked, or wrong window mutated

**Current Status:** 🔴 Not written
**Implementation Note:** Query `findCurrentWindows(expertProfileId, now)` then bulk-save each with new status.

---

### AVAIL-TC-002 — Toggle online: no current window creates walk-in row

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertAvailabilityService.toggleOnlineStatus()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertAvailabilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-AVAIL-004 §Decision` (Proposed — walk-in row, 2h default, flagged Open)

**Preconditions:**
- `FX-001`; `findCurrentWindows()` mocked to return empty list

**Test Steps:**
1. Arrange: mock repository — `findCurrentWindows()` returns `[]`
2. Act: call `toggleOnlineStatus(EXPERT_A_USER_ID, true)`
3. Assert: `save()` called with a new entity where `startAt <= now`, `endAt == startAt + 2h` (TDS-documented Open default), `status='AVAILABLE'`

**Expected Result (PASS):**
- New row created with the walk-in shape; `online=true` returned

**Expected Result (FAIL):**
- No row created, or exception thrown instead of walk-in creation

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ Walk-in duration (2h) is an Open/Proposed default per ADR-AVAIL-004 — if Product changes this before implementation, update this test's assertion accordingly.

---

### AVAIL-TC-003 — Toggle offline: current AVAILABLE window set to UNAVAILABLE

**Severity:** `HIGH`
**Feature Under Test:** `ExpertAvailabilityService.toggleOnlineStatus()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertAvailabilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-AVAIL-001 §Decision`

**Preconditions:**
- `FX-001`, `FX-003` (status=AVAILABLE)

**Test Steps:**
1. Arrange: mock returns current window with `status='AVAILABLE'`
2. Act: call `toggleOnlineStatus(EXPERT_A_USER_ID, false)`
3. Assert: `save()` called with `status='UNAVAILABLE'`; response `online=false`

**Expected Result (PASS):** `online == false`, `activeChannels` empty
**Expected Result (FAIL):** Row unchanged or `online == true`

**Current Status:** 🔴 Not written

---

### AVAIL-TC-004 — Idempotent repeat toggle is a safe no-op

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertAvailabilityService.toggleOnlineStatus()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertAvailabilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-AVAIL-003 §Decision` (idempotent retry, no duplicate unsafe action, SRS E3)

**Preconditions:**
- `FX-001`, `FX-003` (status already `AVAILABLE`)

**Test Steps:**
1. Arrange: mock returns current window already `AVAILABLE`
2. Act: call `toggleOnlineStatus(EXPERT_A_USER_ID, true)` twice
3. Assert: second call does not throw, does not create a duplicate row, returns `online=true`

**Expected Result (PASS):** Both calls return 200-equivalent with `online=true`; no duplicate row created
**Expected Result (FAIL):** Second call throws or creates a new row

**Current Status:** 🔴 Not written

---

### AVAIL-TC-005 — Unverified expert cannot toggle status

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `ExpertAvailabilityPolicy.assertIsOwnerAndVerified()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertAvailabilityPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-AVAIL-002 §Decision`, error code `AVAIL-004` (TDS §10)

**Preconditions:**
- `FX-002` (unverified Expert)

**Test Steps:**
1. Arrange: mock profile with `verification_status='PENDING'`
2. Act: call `assertIsOwnerAndVerified(profileId, EXPERT_A_USER_ID)`
3. Assert: throws `AvailabilityAuthorizationException` with code `AVAIL-004`

**Expected Result (PASS):** Exception thrown, no mutation attempted
**Expected Result (FAIL):** No exception — unverified expert allowed to proceed

**Current Status:** 🔴 Not written

---

### AVAIL-TC-006 — Non-owner expert cannot modify another expert's availability (IDOR)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExpertAvailabilityPolicy.assertIsOwnerAndVerified()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertAvailabilityPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-AVAIL-002 §Decision`, error code `AVAIL-004`

**Preconditions:**
- `FX-001` (Expert A's profile); `FX-006` JWT for a different user (`EXPERT_B_USER_ID`)

**Test Steps:**
1. Arrange: profile owned by `EXPERT_A_USER_ID`
2. Act: call `assertIsOwnerAndVerified(EXPERT_A_PROFILE_ID, EXPERT_B_USER_ID)`
3. Assert: throws `AvailabilityAuthorizationException` (`AVAIL-004`)

**Expected Result (PASS = safe):** `403`-equivalent exception thrown
**Expected Result (FAIL = vulnerability):** No exception — cross-account write allowed

**Current Status:** 🔴 Not written

---

### AVAIL-TC-007 — Create window happy path (no overlap)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertAvailabilityService.createWindow()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertAvailabilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** TDS §8.1 `createWindow()` contract, §9.2 `201 Created` sample

**Preconditions:**
- `FX-001`; no existing overlapping window

**Test Steps:**
1. Arrange: `makeCreateRequest()` factory, no overlap found by repository mock
2. Act: call `createWindow(request, EXPERT_A_USER_ID)`
3. Assert: `save()` called once; response has `status='AVAILABLE'`, matches request fields

**Expected Result (PASS):** `201`-equivalent response with persisted fields matching request
**Expected Result (FAIL):** No save call, or wrong default status

**Current Status:** 🔴 Not written

---

### AVAIL-TC-008 — Create window rejects overlap on same channel

**Severity:** `HIGH`
**Feature Under Test:** `ExpertAvailabilityPolicy.validateNoOverlap()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertAvailabilityPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-AVAIL-003 §Decision`, error code `AVAIL-002`

**Preconditions:**
- `FX-004` (existing `CHAT` window at `now+2h..now+4h`)

**Test Steps:**
1. Arrange: repository mock `findOverlapping()` returns `FX-004` for request `now+3h..now+5h`, `channelType=CHAT`
2. Act: call `validateNoOverlap(EXPERT_A_PROFILE_ID, "CHAT", now+3h, now+5h)`
3. Assert: throws `AvailabilityConflictException` (`AVAIL-002`)

**Expected Result (PASS):** Exception thrown, `createWindow()` never persists
**Expected Result (FAIL):** No exception — overlapping row silently created

**Current Status:** 🔴 Not written

---

### AVAIL-TC-009 — Create window at same time, different channel is accepted (no false-positive conflict)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertAvailabilityPolicy.validateNoOverlap()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ExpertAvailabilityPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-AVAIL-003 §Decision` — overlap scoped to same `channel_type`

**Preconditions:**
- `FX-004` (existing `CHAT` window at `now+2h..now+4h`)

**Test Steps:**
1. Arrange: repository mock `findOverlapping(profileId, "VOICE_CALL", now+2h, now+4h)` returns empty (different channel, not queried against CHAT row)
2. Act: call `validateNoOverlap(EXPERT_A_PROFILE_ID, "VOICE_CALL", now+2h, now+4h)`
3. Assert: no exception thrown

**Expected Result (PASS):** Method returns normally
**Expected Result (FAIL):** Exception thrown incorrectly — false-positive conflict across channels

**Current Status:** 🔴 Not written

---

### AVAIL-TC-010 — Create window rejects endAt <= startAt

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateAvailabilityWindowRequest` bean validation
**Test File:** `src/test/java/com/carebridge/backend/consultation/dto/CreateAvailabilityWindowRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** TDS §8.1 DTO contract, error code `AVAIL-001`

**Preconditions:** None (pure validation test)

**Test Steps:**
1. Arrange: `makeCreateRequest(r -> { r.setStartAt(now+4h); r.setEndAt(now+2h); })`
2. Act: run `Validator.validate(request)` (or service-level check if not bean-validation-enforced)
3. Assert: validation violation / `400 AVAIL-001` raised

**Expected Result (PASS):** Request rejected before reaching overlap check
**Expected Result (FAIL):** Invalid window silently accepted

**Current Status:** 🔴 Not written

---

### AVAIL-TC-011 — Status summary aggregates multiple current channels

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertAvailabilityService.getStatusSummary()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertAvailabilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** TDS §8.1 `ExpertAvailabilityStatusSummaryResponse` contract

**Preconditions:**
- Two current `AVAILABLE` windows: `CHAT` and `VOICE_CALL`

**Test Steps:**
1. Arrange: mock `findCurrentWindows()` returns 2 rows, both `AVAILABLE`, distinct channels
2. Act: call `getStatusSummary(EXPERT_A_USER_ID)`
3. Assert: `online=true`, `activeChannels` contains both `"CHAT"` and `"VOICE_CALL"`, no duplicates

**Expected Result (PASS):** Correct aggregate with distinct channel list
**Expected Result (FAIL):** Missing channel or duplicated entries

**Current Status:** 🔴 Not written

---

### AVAIL-TC-012 — List own windows excludes other experts' rows

**Severity:** `HIGH`
**CWE:** `CWE-639`
**Feature Under Test:** `ExpertAvailabilityRepository.findByExpertProfileId()` via `ExpertAvailabilityService.listOwnWindows()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ExpertAvailabilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-AVAIL-002` ownership scoping

**Preconditions:**
- Two experts (A, B) each with availability rows

**Test Steps:**
1. Arrange: repository mock returns only Expert A's rows for `findByExpertProfileId(EXPERT_A_PROFILE_ID)`
2. Act: call `listOwnWindows(EXPERT_A_USER_ID)`
3. Assert: result contains only rows with `expertProfileId == EXPERT_A_PROFILE_ID`

**Expected Result (PASS):** No cross-expert row leakage
**Expected Result (FAIL):** Other expert's row present in result

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### AVAIL-TC-SEC-001 — Unauthenticated request rejected

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertAvailabilityController` (Spring Security chain)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ExpertAvailabilityControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- No `Authorization` header

**Test Steps (Attack Simulation):**
1. Call `PATCH /api/v1/experts/me/availability/status` without JWT
2. Inspect response

**Expected Result (PASS = hệ thống an toàn):**
- `401 Unauthorized`, `IAM-001`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request processed without authentication

**Current Status:** 🔴 Not written

---

### AVAIL-TC-SEC-002 — Non-EXPERT role rejected

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertAvailabilityController` role guard
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ExpertAvailabilityControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- `FX-007` (MOTHER role JWT)

**Test Steps (Attack Simulation):**
1. Call `PATCH /api/v1/experts/me/availability/status` with a valid MOTHER-role JWT
2. Inspect response

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request processed for a non-EXPERT role

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### AVAIL-TC-INT-001 — Full create → toggle → summary flow

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: create window → toggle online → toggle offline → get summary`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ExpertAvailabilityIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migration applied automatically at Spring context start
- Seed: `expert_profiles` row with `verification_status='VERIFIED'` (via JPA insert, SYNTHETIC data)

**Test Steps:**
1. `POST /api/v1/experts/me/availability` with a future window → expect `201`
2. `PATCH /api/v1/experts/me/availability/status {"online": false}` → expect `200`, `online=false`
3. `PATCH /api/v1/experts/me/availability/status {"online": true}` → expect `200`, `online=true`
4. `GET /api/v1/experts/me/availability/status` → expect `online=true`, `activeChannels=["CHAT"]`

**Expected Result (PASS):**
- DB row's `status` column reflects the final `AVAILABLE` state
- No duplicate rows created across the 3 PATCH/GET calls

**Expected Result (FAIL):**
- DB state diverges from API response, or duplicate rows appear

**DB Assertion:**
```java
ExpertAvailabilityEntity record = availabilityRepository.findByExpertProfileId(expertProfileId).get(0);
assertThat(record.getStatus()).isEqualTo("AVAILABLE");
assertThat(availabilityRepository.findByExpertProfileId(expertProfileId)).hasSize(1);
```

**Current Status:** 🔴 Not written

---

### AVAIL-TC-INT-002 — Overlap rejected at DB-backed service layer

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: create window → create overlapping window (expect 409)`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ExpertAvailabilityIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:**
- PostgreSQL Testcontainer running
- Seed: verified Expert with one `CHAT` window `now+2h..now+4h`

**Test Steps:**
1. `POST /api/v1/experts/me/availability` with overlapping `CHAT` window `now+3h..now+5h`
2. Inspect response and DB row count

**Expected Result (PASS):**
- `409 Conflict`, error code `AVAIL-002`
- DB still contains only the original 1 row (no partial insert)

**Expected Result (FAIL):**
- `201 Created` returned, or 2 overlapping rows persisted

**DB Assertion:**
```java
List<ExpertAvailabilityEntity> rows = availabilityRepository.findByExpertProfileId(expertProfileId);
assertThat(rows).hasSize(1);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `AVAIL-TC-001` | `ExpertAvailabilityServiceTest.java` | `[ ]` | — | — |
| `AVAIL-TC-002` | `ExpertAvailabilityServiceTest.java` | `[ ]` | — | — |
| `AVAIL-TC-003` | `ExpertAvailabilityServiceTest.java` | `[ ]` | — | — |
| `AVAIL-TC-004` | `ExpertAvailabilityServiceTest.java` | `[ ]` | — | — |
| `AVAIL-TC-005` | `ExpertAvailabilityPolicyTest.java` | `[ ]` | — | — |
| `AVAIL-TC-006` | `ExpertAvailabilityPolicyTest.java` | `[ ]` | — | — |
| `AVAIL-TC-007` | `ExpertAvailabilityServiceTest.java` | `[ ]` | — | — |
| `AVAIL-TC-008` | `ExpertAvailabilityPolicyTest.java` | `[ ]` | — | — |
| `AVAIL-TC-009` | `ExpertAvailabilityPolicyTest.java` | `[ ]` | — | — |
| `AVAIL-TC-010` | `CreateAvailabilityWindowRequestValidationTest.java` | `[ ]` | — | — |
| `AVAIL-TC-011` | `ExpertAvailabilityServiceTest.java` | `[ ]` | — | — |
| `AVAIL-TC-012` | `ExpertAvailabilityServiceTest.java` | `[ ]` | — | — |
| `AVAIL-TC-SEC-001` | `ExpertAvailabilityControllerSecurityTest.java` | `[ ]` | — | — |
| `AVAIL-TC-SEC-002` | `ExpertAvailabilityControllerSecurityTest.java` | `[ ]` | — | — |
| `AVAIL-TC-INT-001` | `ExpertAvailabilityIntegrationTest.java` | `[ ]` | — | — |
| `AVAIL-TC-INT-002` | `ExpertAvailabilityIntegrationTest.java` | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ExpertAvailabilityService implements IExpertAvailabilityService {

    @Override
    public ExpertAvailabilityResponse createWindow(CreateAvailabilityWindowRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertAvailabilityStatusSummaryResponse toggleOnlineStatus(UUID currentUserId, boolean online) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertAvailabilityResponse updateWindowStatus(UUID availabilityId, String targetStatus, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertAvailabilityStatusSummaryResponse getStatusSummary(UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<ExpertAvailabilityResponse> listOwnWindows(UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class ExpertAvailabilityPolicy {
    public void assertIsOwnerAndVerified(UUID expertProfileId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void validateNoOverlap(UUID expertProfileId, String channelType, Instant startAt, Instant endAt) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `AVAIL-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-010` | Bean validation (no service stub needed) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-SEC-001` | 401 expected regardless of stub | 🔴 FAIL (until wired) | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-SEC-002` | 403 expected regardless of stub | 🔴 FAIL (until wired) | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-INT-001` | `throw('Not implemented')` propagates as 500 | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `AVAIL-TC-INT-002` | `throw('Not implemented')` propagates as 500 | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CONSULTATION-IMP-142` đã được review và approve
- [ ] `ADR-AVAIL-001` and `ADR-AVAIL-004` confirmed (currently `Proposed`)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] No new migration required — confirmed against `V1__init_schema.sql`
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ExpertAvailabilityService`/`ExpertAvailabilityPolicy`
- [ ] Không có business logic trong `ExpertAvailabilityController` (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Overlap validation confirmed against real Postgres (not just mocked)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (factory-based only)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/TDS)

### Suspension Criteria (Điều kiện tạm dừng)

- `ADR-AVAIL-001`/`ADR-AVAIL-004` chưa được Product/Tech Lead xác nhận `Accepted`
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No new migration to revert — no schema change (TDS §5.3).

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/consultation/controller/ExpertAvailabilityController.java
git checkout -- src/main/java/com/carebridge/backend/consultation/service/ExpertAvailabilityService.java
git checkout -- src/main/java/com/carebridge/backend/consultation/policy/ExpertAvailabilityPolicy.java
git checkout -- src/main/java/com/carebridge/backend/consultation/repository/ExpertAvailabilityRepository.java
git checkout -- src/main/java/com/carebridge/backend/consultation/entity/ExpertAvailabilityEntity.java
git checkout -- src/test/java/com/carebridge/backend/consultation/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu tồn tại)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume `is_online` column exists (violates ADR-AVAIL-001 C1) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
