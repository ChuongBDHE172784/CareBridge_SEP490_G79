# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC176 — Configure Emergency Contact

**Document ID:** `CB-SAFETY-TDD-010`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC176_ConfigureEmergencyContact/UC176_ConfigureEmergencyContact_TDS.md` (`CB-SAFETY-IMP-010`) — primary spec for this test document
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260706120000__create_emergency_contacts.sql` — new migration (this feature)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000005__create_safety_monitoring_config.sql` — confirms no pre-existing emergency-contact column
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.4.10` — UC-176 functional requirement
- `08_References/Template/PHASE-4_Test-Spec.md` — TDD Template source

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC176 Configure Emergency Contact |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
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
| **Feature / Gap ID** | `GAP-UC176` |
| **Module** | `Configure Emergency Contact — Bounded Context: safety` |
| **Spec gốc** | `CB-SAFETY-IMP-010` |
| **Priority** | 🔴 P0 (Critical, per SRS Priority field) |
| **Sprint** | `Sprint 2 (initial, per function-spec-task-allocation.md line 437) + Sprint 3 (AI Safety And IMU Emergency Integration, line 628)` |
| **Milestone** | `TV5-Chương AI & IMU Safety domain` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT ROLE_MOTHER)` |
| **Downstream Consumers** | `UC138 Send Emergency Alert (NOT wired in this scope — see TDS RG-3)`, `UC141 Open Emergency Support from Safety Alert (NOT wired in this scope — see TDS Phụ lục B)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC176_ConfigureEmergencyContact_TDS.md §17` (C1-C6) |
| **Constraints Injected** | New table only (no `safety_monitoring_config` mutation); ownership on every op; self-attestation-only verify; contiguous priority_order via two-phase reorder; MAX_CONTACTS=5; no UC138/UC141 wiring in this scope |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Task brief hypothesized table `safety_monitoring_settings.emergency_contact_user_id` (single FK, one contact only) | Real table is `safety_monitoring_config` (`V20260627000005`); confirmed via direct migration read it has **no** emergency-contact column at all, in any applied migration | Tests assert a brand-new `emergency_contacts` table is used; a dedicated test (`UC176-TC-016`) asserts `safety_monitoring_config`'s column set is unchanged after the new migration runs |
| L2 | SRS says "prioritizes" without specifying storage mechanism | No existing ordering pattern for this domain in the codebase | Tests encode two-phase (negative-then-positive) reorder transaction behavior, and assert `priority_order` stays contiguous 1..N after every add/remove/reorder |
| L3 | SRS says "verifies" without specifying who confirms (Mother vs contact person) | ADR-SAFETY-016 (TDS) resolves this as self-attestation only — no OTP/SMS to the contact, since no SMS provider exists in the current stack (only Gmail SMTP + FCM per CLAUDE.md) | Tests assert `verify()` requires only the owning Mother's call, produces no outbound notification side-effect, and is idempotent-guarded (SAFETY-204 on double-verify) |
| L4 | No explicit max-contacts limit in SRS | ADR-SAFETY-015 (TDS) proposes `MAX_CONTACTS = 5` as an Open/proposed value, not SRS-confirmed | Tests encode exactly 5 as the boundary (via constant reference, not magic number) and are structured so changing the constant does not require rewriting test logic beyond the boundary value |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Configure Emergency Contact bao gồm các layer:
├── Domain (EmergencyContact entity — pure invariants, no deps)
├── Service (EmergencyContactService — mock IEmergencyContactRepository với Mockito)
├── Controller (EmergencyContactController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full CRUD + reorder)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-176 §3.3.4.10` | add/verify/prioritize/remove behaviors, RBAC precondition, exception handling (E1/E2/E3) |
| `ADR-SAFETY-014` | new-table-only constraint; no mutation of `safety_monitoring_config` |
| `ADR-SAFETY-015` | priority ordering mechanics, max-contacts boundary |
| `ADR-SAFETY-016` | self-attestation verify semantics, idempotency guard |
| `BR-RBAC` | ownership checks on every operation |
| `PDPA / Luật 91/2025` | third-party PII minimization, no PII in logs |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother adds a contact within limit | `EmergencyContactService.add()` | `UC176-TC-001` |
| TC-COND-002 | Mother adds a contact at the max-contacts boundary | `EmergencyContactService.add()` | `UC176-TC-002`, `UC176-TC-003` |
| TC-COND-003 | Mother verifies a pending contact | `EmergencyContactService.verify()` | `UC176-TC-004` |
| TC-COND-004 | Mother re-verifies an already-verified contact | `EmergencyContactService.verify()` | `UC176-TC-005` |
| TC-COND-005 | Non-owner attempts to verify/remove/reorder | `EmergencyContactService.*()` | `UC176-TC-006`, `UC176-TC-007`, `UC176-TC-013` |
| TC-COND-006 | Mother reorders contacts (full permutation) | `EmergencyContactService.reorder()` | `UC176-TC-008` |
| TC-COND-007 | Mother reorders with a mismatched ID set | `EmergencyContactService.reorder()` | `UC176-TC-009` |
| TC-COND-008 | Mother removes a contact, remaining contacts re-sequence | `EmergencyContactService.remove()` | `UC176-TC-010` |
| TC-COND-009 | Mother removes a non-existent contact | `EmergencyContactService.remove()` | `UC176-TC-011` |
| TC-COND-010 | List endpoint returns contacts ordered by priority | `EmergencyContactController.GET` | `UC176-TC-012` |
| TC-COND-011 | Concurrent reorder does not violate unique constraint | `EmergencyContactService.reorder()` | `UC176-TC-014` |
| TC-COND-012 | Unauthenticated/wrong-role access is rejected | `EmergencyContactController.*` | `UC176-TC-015` |
| TC-COND-013 | `safety_monitoring_config` is unaffected by the new migration | Flyway migration `V20260706120000` | `UC176-TC-016` |
| TC-COND-014 | Add validation — required fields | `EmergencyContactController.POST` | `UC176-TC-017` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `contactUserId` present vs null (CareBridge user vs external contact) | Two genuinely different persistence/validation paths |
| Boundary Value Analysis | `MAX_CONTACTS` = 5: test at 4→5 (pass), 5→6 (fail) | ADR-SAFETY-015 boundary |
| State Transition Testing | `verificationStatus`: PENDING → VERIFIED → (no reverse transition) | §6.5 State Machine in TDS |
| Error Guessing | IDOR via `contactId` from another Mother; reorder with foreign/duplicate/missing IDs | BR-RBAC, SAFETY-201/202/205 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ userId: MOTHER_A, contactName: "Nguyen Van A", contactPhone: "+84901111111", priorityOrder: 1, verificationStatus: 'PENDING' }` | Happy path base fixture |
| `FX-002` | DB seed | 5 contacts pre-seeded for `MOTHER_A` (`priorityOrder` 1..5) | Max-contacts boundary |
| `FX-003` | DB seed | `{ userId: MOTHER_B, contactName: "Tran Thi B", ... }` | Cross-owner isolation test |
| `FX-004` | JWT | `{ sub: MOTHER_A_ID, role: 'ROLE_MOTHER' }` | Auth context — owner |
| `FX-005` | JWT | `{ sub: MOTHER_B_ID, role: 'ROLE_MOTHER' }` | Auth context — non-owner |
| `FX-006` | JWT | `{ sub: FAMILY_ID, role: 'ROLE_FAMILY' }` | Auth context — wrong role |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeContact()
// ═══════════════════════════════════════════════════════════

// EmergencyContactTestFactory.java
class EmergencyContactTestFactory {

    static final UUID MOTHER_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID MOTHER_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");

    // Giá trị baseline hợp lệ — đồng bộ với FX-001 (§3 TDS-05)
    static EmergencyContact makeContact() {
        return makeContact(c -> {});
    }

    static EmergencyContact makeContact(Consumer<EmergencyContact> overrides) {
        EmergencyContact contact = new EmergencyContact();
        contact.setId(UUID.randomUUID());
        contact.setUserId(MOTHER_A_ID);
        contact.setContactName("Nguyen Van A");
        contact.setContactPhone("+84901111111");
        contact.setRelationship("Husband");
        contact.setContactUserId(null);
        contact.setPriorityOrder((short) 1);
        contact.setVerificationStatus(VerificationStatus.PENDING);
        contact.setVerifiedAt(null);
        contact.setCreatedAt(Instant.now());
        contact.setUpdatedAt(Instant.now());
        contact.setCreatedBy(MOTHER_A_ID);
        overrides.accept(contact);
        return contact;
    }

    static AddEmergencyContactRequest makeAddRequest(Consumer<AddEmergencyContactRequest> overrides) {
        AddEmergencyContactRequest req = new AddEmergencyContactRequest();
        req.setContactName("Nguyen Van A");
        req.setContactPhone("+84901111111");
        req.setRelationship("Husband");
        overrides.accept(req);
        return req;
    }

    // 5 contacts pre-seeded, priorityOrder 1..5 — for max-contacts boundary tests
    static List<EmergencyContact> makeFullList(UUID userId) {
        return java.util.stream.IntStream.rangeClosed(1, 5)
            .mapToObj(i -> makeContact(c -> {
                c.setId(UUID.randomUUID());
                c.setUserId(userId);
                c.setPriorityOrder((short) i);
                c.setContactName("Contact " + i);
            }))
            .toList();
    }
}
```

---

### UC176-TC-001 — Add emergency contact — happy path, priority auto-assigned to end of list

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyContactService.add(UUID, AddEmergencyContactRequest)`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SAFETY-015 §Decision` (priority defaults to end of list)

**Preconditions:**
- Mother A (`MOTHER_A_ID`) has 2 existing contacts with `priorityOrder` 1 and 2
- Fixture: `FX-004` (JWT context), 2 seeded contacts via `EmergencyContactTestFactory.makeContact()`

**Test Steps:**
1. Arrange: mock `repository.countByUserId(MOTHER_A_ID)` returns `2`; mock `repository.findMaxPriorityOrderByUserId(MOTHER_A_ID)` returns `Optional.of((short) 2)`
2. Act: call `service.add(MOTHER_A_ID, makeAddRequest(r -> {}))`
3. Assert: saved entity has `priorityOrder == 3`, `verificationStatus == PENDING`, `verifiedAt == null`

**Expected Result (PASS — hành vi đúng):**
- Returned `EmergencyContactResponse.priorityOrder == 3`
- `EmergencyContactAdded` event published with `priorityOrder == 3`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Priority not auto-incremented, or duplicate priority assigned

**Current Status:** 🔴 Not written
**Implementation Note:** Use `findMaxPriorityOrderByUserId(userId).orElse((short) 0) + 1` per TDS §6.1 sequence diagram.

---

### UC176-TC-002 — Add contact at boundary — 5th contact succeeds

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyContactService.add()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SAFETY-015 §Decision (MAX_CONTACTS = 5)`

**Preconditions:**
- Mother A has exactly 4 existing contacts (`FX-002` minus 1)

**Test Steps:**
1. Arrange: mock `countByUserId` returns `4`
2. Act: call `service.add(MOTHER_A_ID, makeAddRequest(r -> {}))`
3. Assert: no exception thrown, contact persisted as the 5th

**Expected Result (PASS):** 201-equivalent success, `EmergencyContactAdded` published
**Expected Result (FAIL):** `SafetyException(SAFETY-203)` thrown prematurely at count=4 (off-by-one bug)

**Current Status:** 🔴 Not written

---

### UC176-TC-003 — Add contact beyond boundary — 6th contact rejected (SAFETY-203)

**Severity:** `CRITICAL`
**CWE:** `CWE-770 — Allocation of Resources Without Limits`
**Feature Under Test:** `EmergencyContactService.add()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SAFETY-015 §Decision`, TDS `§10 SAFETY-203`

**Preconditions:**
- Mother A has exactly 5 existing contacts (`FX-002`)

**Test Steps:**
1. Arrange: mock `countByUserId` returns `5`
2. Act: call `service.add(MOTHER_A_ID, makeAddRequest(r -> {}))`
3. Assert: `SafetyException` thrown with `code == "SAFETY-203"`, `httpStatus == 409`

**Expected Result (PASS):** Exception thrown, `repository.save()` never called (verify via Mockito `verify(repo, never()).save(any())`)
**Expected Result (FAIL):** Contact silently persisted as 6th, breaking `ADR-SAFETY-015` boundary

**Current Status:** 🔴 Not written

---

### UC176-TC-004 — Verify pending contact — happy path (self-attestation)

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyContactService.verify(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SAFETY-016 §Decision`

**Preconditions:**
- Contact exists, owned by `MOTHER_A_ID`, `verificationStatus == PENDING`

**Test Steps:**
1. Arrange: mock `repository.findByIdAndUserId(contactId, MOTHER_A_ID)` returns the PENDING contact
2. Act: call `service.verify(MOTHER_A_ID, contactId)`
3. Assert: saved entity has `verificationStatus == VERIFIED`, `verifiedAt != null`

**Expected Result (PASS):**
- `EmergencyContactVerified` event published
- No FCM/SMS/notification call made to any port (verify via Mockito no-interaction check on any notification port — none should be injected per ADR-SAFETY-016)

**Expected Result (FAIL):**
- State not transitioned, or an unexpected notification side-effect occurs (violates C3)

**Current Status:** 🔴 Not written

---

### UC176-TC-005 — Verify already-verified contact — idempotency guard (SAFETY-204)

**Severity:** `MEDIUM`
**Feature Under Test:** `EmergencyContactService.verify()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** TDS `§6.2 note`, `§10 SAFETY-204`

**Preconditions:**
- Contact exists, owned by `MOTHER_A_ID`, `verificationStatus == VERIFIED`, `verifiedAt` already set to `T0`

**Test Steps:**
1. Arrange: mock `findByIdAndUserId` returns the already-VERIFIED contact
2. Act: call `service.verify(MOTHER_A_ID, contactId)` a second time
3. Assert: `SafetyException(SAFETY-204)` thrown, `httpStatus == 409`

**Expected Result (PASS):** Exception thrown, `verifiedAt` remains `T0` (not overwritten), `repository.save()` never called
**Expected Result (FAIL):** `verifiedAt` silently overwritten with a new timestamp, or no exception thrown

**Current Status:** 🔴 Not written

---

### UC176-TC-006 — Non-owner cannot verify another Mother's contact

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Legal:** `BR-RBAC`
**Feature Under Test:** `EmergencyContactService.verify()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Contact exists, owned by `MOTHER_A_ID`

**Test Steps (Attack Simulation):**
1. Arrange: mock `repository.findByIdAndUserId(contactId, MOTHER_B_ID)` returns `Optional.empty()` (repository query is scoped by both id AND userId — the row exists for A but the B-scoped query correctly returns nothing)
2. Act: call `service.verify(MOTHER_B_ID, contactId)` (Mother B attempting to verify Mother A's contact)
3. Assert: `SafetyException(SAFETY-201)` thrown (404 — same code as "not found", never revealing the contact exists for another user)

**Expected Result (PASS = hệ thống an toàn):** `404 SAFETY-201`, no state mutation, no information disclosure distinguishing "not found" from "not yours"
**Expected Result (FAIL = lỗ hổng tồn tại):** Verify succeeds for non-owner, or a distinct error code leaks contact existence

**Current Status:** 🔴 Not written

---

### UC176-TC-007 — Non-owner cannot remove another Mother's contact

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Feature Under Test:** `EmergencyContactService.remove()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Contact exists, owned by `MOTHER_A_ID`

**Test Steps (Attack Simulation):**
1. Arrange: mock `findByIdAndUserId(contactId, MOTHER_B_ID)` returns `Optional.empty()`
2. Act: call `service.remove(MOTHER_B_ID, contactId)`
3. Assert: `SafetyException(SAFETY-201)` thrown, `repository.delete()` never called

**Expected Result (PASS):** 404, no deletion occurs, Mother A's contact remains intact
**Expected Result (FAIL):** Contact deleted by non-owner

**Current Status:** 🔴 Not written

---

### UC176-TC-008 — Reorder — full permutation happy path, two-phase transaction

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyContactService.reorder(UUID, ReorderEmergencyContactsRequest)`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SAFETY-015 §Decision`, TDS `§6.3` sequence diagram

**Preconditions:**
- Mother A has 3 contacts: `id1` (priority 1), `id2` (priority 2), `id3` (priority 3)

**Test Steps:**
1. Arrange: mock `findByUserIdOrderByPriorityOrderAsc(MOTHER_A_ID)` returns `[id1, id2, id3]`
2. Act: call `service.reorder(MOTHER_A_ID, new ReorderEmergencyContactsRequest(List.of(id3, id1, id2)))`
3. Assert: final state — `id3.priorityOrder == 1`, `id1.priorityOrder == 2`, `id2.priorityOrder == 3`

**Expected Result (PASS):**
- Result list returned in new order `[id3, id1, id2]` with correct `priorityOrder` values
- `EmergencyContactPriorityChanged` event published with `newOrderContactIds == [id3, id1, id2]`
- No `uk_emergency_contacts_user_priority` violation (verified at integration level, `UC176-TC-INT-002`)

**Expected Result (FAIL):** Wrong final order, or unique constraint violation surfaces as unhandled exception

**Current Status:** 🔴 Not written
**Implementation Note:** Two-phase negative-then-positive update per TDS §6.3 — critical to avoid `uk_emergency_contacts_user_priority` violation mid-sequence.

---

### UC176-TC-009 — Reorder — mismatched ID set rejected (SAFETY-202)

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyContactService.reorder()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** TDS `§10 SAFETY-202`

**Preconditions:**
- Mother A has 3 contacts: `id1`, `id2`, `id3`

**Test Steps:**
1. Arrange: mock `findByUserIdOrderByPriorityOrderAsc` returns `[id1, id2, id3]`
2. Act (variant a): call `reorder(MOTHER_A_ID, [id1, id2])` (missing `id3`)
3. Act (variant b): call `reorder(MOTHER_A_ID, [id1, id2, id3, UUID.randomUUID()])` (foreign ID injected)
4. Act (variant c): call `reorder(MOTHER_A_ID, [id1, id1, id2])` (duplicate ID)
5. Assert: all 3 variants throw `SafetyException(SAFETY-202)`, no partial write occurs

**Expected Result (PASS):** 400 `SAFETY-202` for all variants, zero `save()` calls made (all-or-nothing validated before any mutation)
**Expected Result (FAIL):** Partial reorder applied, or a foreign/duplicate ID silently accepted (would corrupt another Mother's contact if the foreign ID belongs to Mother B — critical IDOR-adjacent risk)

**Current Status:** 🔴 Not written

---

### UC176-TC-010 — Remove contact — remaining contacts re-sequence, no gap

**Severity:** `HIGH`
**Feature Under Test:** `EmergencyContactService.remove(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** TDS `§6.4` sequence diagram, invariant note after `§6.5`

**Preconditions:**
- Mother A has 3 contacts: `id1`(1), `id2`(2), `id3`(3)

**Test Steps:**
1. Arrange: mock `findByIdAndUserId(id2, MOTHER_A_ID)` returns the contact; after delete, mock `findByUserIdOrderByPriorityOrderAsc` returns remaining `[id1(1), id3(3)]` (gap at 2)
2. Act: call `service.remove(MOTHER_A_ID, id2)`
3. Assert: `id1.priorityOrder == 1`, `id3.priorityOrder == 2` (re-sequenced, gap closed)

**Expected Result (PASS):** Remaining list is contiguous 1..N after removal; `EmergencyContactRemoved` published
**Expected Result (FAIL):** Gap left at priority 2, violating the contiguity invariant (would break future reorder/add logic relying on `MAX(priorityOrder)`)

**Current Status:** 🔴 Not written

---

### UC176-TC-011 — Remove non-existent contact — SAFETY-201

**Severity:** `MEDIUM`
**Feature Under Test:** `EmergencyContactService.remove()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:** none seeded for the given `contactId`

**Test Steps:**
1. Arrange: mock `findByIdAndUserId` returns `Optional.empty()`
2. Act: call `service.remove(MOTHER_A_ID, randomContactId)`
3. Assert: `SafetyException(SAFETY-201)` thrown, `httpStatus == 404`

**Expected Result (PASS):** 404 thrown, no delete attempted
**Expected Result (FAIL):** NullPointerException or silent no-op without error signaled to caller

**Current Status:** 🔴 Not written

---

### UC176-TC-012 — List endpoint returns contacts ordered by priority

**Severity:** `MEDIUM`
**Feature Under Test:** `EmergencyContactController.GET /api/v1/safety/emergency-contacts`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/EmergencyContactControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:** Mother A has 3 contacts seeded with priorities 1, 2, 3 (in non-insertion order to prove sort)

**Test Steps:**
1. Arrange: `@WebMvcTest`, mock `service.list(MOTHER_A_ID)` returns contacts sorted ascending by `priorityOrder`
2. Act: `GET /api/v1/safety/emergency-contacts` with Mother A's JWT
3. Assert: response body array is in ascending `priorityOrder`

**Expected Result (PASS):** 200 OK, array ordered ascending
**Expected Result (FAIL):** Unordered or reverse-ordered array

**Current Status:** 🔴 Not written

---

### UC176-TC-013 — Non-owner cannot reorder another Mother's contacts

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Feature Under Test:** `EmergencyContactService.reorder()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/EmergencyContactServiceTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Mother A's contacts `id1, id2, id3` exist; Mother B has zero contacts

**Test Steps (Attack Simulation):**
1. Arrange: mock `findByUserIdOrderByPriorityOrderAsc(MOTHER_B_ID)` returns `[]` (Mother B owns none of these)
2. Act: call `service.reorder(MOTHER_B_ID, new ReorderEmergencyContactsRequest(List.of(id1, id2, id3)))`
3. Assert: `SafetyException(SAFETY-202)` thrown (mismatched set — Mother B's existing set `[]` does not match requested `[id1,id2,id3]`)

**Expected Result (PASS = hệ thống an toàn):** 400 `SAFETY-202`, Mother A's contacts unaffected
**Expected Result (FAIL = lỗ hổng tồn tại):** Mother B successfully reassigns priority on Mother A's contacts (cross-tenant mutation)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### UC176-TC-INT-001 — Full CRUD + reorder flow via Testcontainers

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: add → verify → reorder → remove`
**Test File:** `src/test/java/com/carebridge/backend/safety/EmergencyContactIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-003, TC-COND-006, TC-COND-008`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration `V20260706120000` applied automatically on Spring context start

**Test Steps:**
1. `POST /api/v1/safety/emergency-contacts` (Mother A JWT) — add contact 1
2. `POST /api/v1/safety/emergency-contacts` — add contact 2
3. `PATCH /api/v1/safety/emergency-contacts/{contact1Id}/verify`
4. `PUT /api/v1/safety/emergency-contacts/reorder` — swap order
5. `DELETE /api/v1/safety/emergency-contacts/{contact2Id}`
6. `GET /api/v1/safety/emergency-contacts` — assert final state

**Expected Result (PASS):**
- Final list has exactly 1 contact (contact1), `verificationStatus == VERIFIED`, `priorityOrder == 1`
- DB row for contact2 physically deleted (hard delete per ADR §4.2 Retention)

**Expected Result (FAIL):** Any step returns unexpected status code, or final DB state diverges from API response

**DB Assertion:**
```java
List<EmergencyContact> remaining = emergencyContactRepository.findByUserIdOrderByPriorityOrderAsc(motherAId);
assertThat(remaining).hasSize(1);
assertThat(remaining.get(0).getPriorityOrder()).isEqualTo((short) 1);
assertThat(remaining.get(0).getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
```

**Current Status:** 🔴 Not written

---

### UC176-TC-INT-002 — Concurrent reorder does not violate unique constraint (SAFETY-014, C4)

**Severity:** `CRITICAL`
**Feature Under Test:** `EmergencyContactService.reorder()` under transactional two-phase update
**Test File:** `src/test/java/com/carebridge/backend/safety/EmergencyContactIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- Mother A has 5 contacts seeded (priority 1..5) via Testcontainers

**Test Steps:**
1. Call `reorder()` with a fully reversed order `[id5, id4, id3, id2, id1]`
2. Assert no `DataIntegrityViolationException` (`uk_emergency_contacts_user_priority`) is thrown mid-transaction
3. Re-fetch and assert final `priorityOrder` values are exactly `1..5` matching the reversed order, no duplicates

**Expected Result (PASS):** Transaction commits cleanly, `SELECT priority_order FROM emergency_contacts WHERE user_id = ? ORDER BY priority_order` returns `[1,2,3,4,5]` with no gaps/dupes
**Expected Result (FAIL):** Unique constraint violation surfaces, or transaction partially commits leaving inconsistent state

**Current Status:** 🔴 Not written

---

### UC176-TC-INT-003 — Ownership isolation — Mother A cannot see Mother B's contacts via list()

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `GET /api/v1/safety/emergency-contacts` cross-tenant isolation
**Test File:** `src/test/java/com/carebridge/backend/safety/EmergencyContactIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Preconditions:** Mother A has 2 contacts; Mother B has 1 contact, seeded independently

**Test Steps:**
1. `GET /api/v1/safety/emergency-contacts` with Mother A's JWT
2. Assert response contains exactly Mother A's 2 contacts, none of Mother B's

**Expected Result (PASS):** Response array length == 2, all `id`s belong to Mother A's seeded contacts
**Expected Result (FAIL):** Mother B's contact leaks into Mother A's response (missing `WHERE user_id = :userId` filter)

**Current Status:** 🔴 Not written

---

### UC176-TC-014 — Unauthenticated / wrong-role access rejected

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `EmergencyContactController` — Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/EmergencyContactControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** none

**Test Steps (Attack Simulation):**
1. `GET /api/v1/safety/emergency-contacts` with no `Authorization` header → expect `401`
2. `GET /api/v1/safety/emergency-contacts` with `ROLE_FAMILY` JWT (`FX-006`) → expect `403`
3. `POST /api/v1/safety/emergency-contacts` with `ROLE_PARTNER` JWT → expect `403`

**Expected Result (PASS = hệ thống an toàn):** `401`/`403` as specified per §16 Authorization Matrix
**Expected Result (FAIL = lỗ hổng tồn tại):** Any of the above returns `200`/`201`

**Current Status:** 🔴 Not written

---

### UC176-TC-015 — Add contact — required field validation

**Severity:** `MEDIUM`
**Feature Under Test:** `EmergencyContactController.POST` — `@Valid AddEmergencyContactRequest`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/EmergencyContactControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** none

**Test Steps:**
1. `POST /api/v1/safety/emergency-contacts` with `{}` (empty body)
2. Assert `400` with validation error details for `contactName` and `contactPhone` (both `@NotBlank`)

**Expected Result (PASS):** `400`, error details list both missing required fields
**Expected Result (FAIL):** Request accepted with null `contactName`/`contactPhone`, or 500 instead of 400

**Current Status:** 🔴 Not written

---

### UC176-TC-016 — Migration does not modify `safety_monitoring_config`

**Severity:** `HIGH`
**Feature Under Test:** Flyway migration `V20260706120000__create_emergency_contacts.sql`
**Test File:** `src/test/java/com/carebridge/backend/safety/EmergencyContactMigrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-SAFETY-014 §Decision` — "no cột nào bị deprecate", TDS `§14` verification SQL

**Preconditions:** Testcontainers PostgreSQL with Flyway migrations applied through `V20260706120000`

**Test Steps:**
1. Query `information_schema.columns` for `table_name = 'safety_monitoring_config'`
2. Assert column set is exactly: `id, user_id, fall_detection_enabled, sensitivity_level, emergency_auto_alert, updated_at, updated_by, countdown_seconds` (last one added by UC137's `V20260705090000`, if that migration is present in the test classpath — otherwise the 7 columns from `V20260627000005` alone)
3. Query `information_schema.tables` for `table_name = 'emergency_contacts'` — assert it exists with the 12 columns defined in TDS §5.2

**Expected Result (PASS):** No unexpected column added/removed on `safety_monitoring_config`; `emergency_contacts` exists as a fully independent new table
**Expected Result (FAIL):** Any column added to `safety_monitoring_config`, indicating the forbidden "column bolt-on" anti-pattern (AP-AI-001) was implemented instead of the ADR-mandated new table

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UC176-TC-001` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-002` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-003` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-004` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-005` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-006` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-007` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-008` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-009` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-010` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-011` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-012` | `EmergencyContactControllerTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-013` | `EmergencyContactServiceTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-014` | `EmergencyContactControllerTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-015` | `EmergencyContactControllerTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-016` | `EmergencyContactMigrationTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-INT-001` | `EmergencyContactIntegrationTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-INT-002` | `EmergencyContactIntegrationTest.java` | `[ ]` | `[ ]` | |
| `UC176-TC-INT-003` | `EmergencyContactIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class EmergencyContactService implements IEmergencyContactService {

    @Override
    public List<EmergencyContactResponse> list(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public EmergencyContactResponse add(UUID userId, AddEmergencyContactRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public EmergencyContactResponse verify(UUID userId, UUID contactId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<EmergencyContactResponse> reorder(UUID userId, ReorderEmergencyContactsRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void remove(UUID userId, UUID contactId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UC176-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC176-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC176-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC176-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC176-TC-016` | *(N/A — migration-only test, does not exercise the stub)* | 🔴 FAIL until migration file exists | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled at implementation time)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___` (to be filled at implementation time)

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-SAFETY-IMP-010` đã được review và approve (Status = Approved)
- [ ] Logic Issues (§2) đã được confirm với Principal Architect
- [ ] Flyway migration `V20260706120000__create_emergency_contacts.sql` đã được approved
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `EmergencyContactService`
- [ ] Không có business logic trong `EmergencyContactController` (chỉ validation + mapping)
- [ ] Không có PII (contactPhone/contactName) xuất hiện plaintext trong logs
- [ ] `priority_order` contiguity invariant verified qua `UC176-TC-INT-002`
- [ ] `safety_monitoring_config` schema unchanged, verified qua `UC176-TC-016`

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — mọi test dùng `EmergencyContactTestFactory.makeContact()`, không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn ADR/BR (đã điền ở mỗi TC trên)

### Suspension Criteria (Điều kiện tạm dừng)

- TDS chưa Approved
- RG-3 (UC138 integration) hoặc RG-6 (verify mechanism) cần quyết định chặn implementation của toàn bộ feature (hiện tại KHÔNG chặn — cả hai đã có baseline decision trong TDS, chỉ là follow-up items)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS emergency_contacts CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260706120000';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/safety/entity/EmergencyContact.java
git checkout -- src/main/java/com/carebridge/backend/safety/repository/IEmergencyContactRepository.java
git checkout -- src/main/java/com/carebridge/backend/safety/service/
git checkout -- src/main/java/com/carebridge/backend/safety/controller/EmergencyContactController.java
git checkout -- src/main/resources/db/migration/V20260706120000__create_emergency_contacts.sql
git checkout -- src/test/java/com/carebridge/backend/safety/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md nếu áp dụng
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (mọi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ (to verify at implementation time) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (tất cả map tới ADR-SAFETY-014/015/016) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests chỉ verify HTTP status/validation, không verify priority-ordering logic) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (all types defined in TDS §8, to be created alongside tests) | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [x] Cần verify tại implementation time (Red Gate §5.1 chưa chạy — spec vẫn Draft)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| *(none yet — pre-implementation)* | — | — | — | — |

---

*Test-Spec v1.0 — Draft, chờ review cùng TDS `CB-SAFETY-IMP-010`.*
