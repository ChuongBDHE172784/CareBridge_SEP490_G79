# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC153 — Contact Nearby Expert

**Document ID:** `CB-MAP-IMP-006-TEST`
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
- `04_Implement/UC153_ContactNearbyExpert/UC153_ContactNearbyExpert_TDS.md` (`CB-MAP-IMP-006`) — Technical Specification (companion document)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.7.2` — Functional requirement source
- `04_Implement/UC149_FindNearbyAvailableExperts/UC149_FindNearbyAvailableExperts_TDS.md` — upstream contract (`IExpertLocationShareRepository`)
- PDPA / Luật 91/2025 — legal basis for location + message PII

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — Test-Spec cho UC153 Contact Nearby Expert |

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
| **Feature / Gap ID** | `GAP-UC153` |
| **Module** | `Contact Nearby Expert — Bounded Context: map (orchestration)` |
| **Spec gốc** | `CB-MAP-IMP-006` (`UC153_ContactNearbyExpert_TDS.md`) |
| **Priority** | 🟠 P1 |
| **Sprint** | `S2-S3 (per function-spec-task-allocation.md — TV4-Lâm nearby expert/map cluster)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` (vị trí Mother/Expert + `message` free-text có thể mang health context) |
| **Compliance Scope** | `PDPA / Luật 91/2025`, `BR-RBAC`, `BR-SAFETY` |
| **Upstream Dependencies** | `UC149 Find Nearby Available Experts (IExpertLocationShareRepository)`, `IAM (JWT ROLE_MOTHER)` |
| **Downstream Consumers** | Notification module (best-effort), tương lai: UC144/UC145/UC146/UC75 khi Expert accept (ngoài phạm vi test này) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-006 §17` (AI Prompt Constraints C1-C6) |
| **Constraints Injected** | C1 (re-validate expert), C2 (không gọi joinSession trực tiếp), C3 (không insert consultation_bookings), C4 (userId từ JWT), C5 (CHAT → NOT_IMPLEMENTED), C6 (dedup 5 phút → 409) |
| **Model** | `Claude (Sonnet) — Technical Architect + Test Designer role` |
| **Trust Level** | `T2 → T3 (pending Red Gate, §5)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS mô tả UC153 "sends a chat/voice/video/booking request" như thể mở kênh ngay lập tức | UC144 (Chat) chưa tồn tại; UC145/UC146 đòi hỏi `consultation_sessions` CONFIRMED đã có sẵn — UC153 không có `sessionId` để join | Test khẳng định UC153 chỉ tạo `nearby_expert_contact_requests` (status=PENDING), KHÔNG gọi `IConsultationSessionService.joinSession()`, KHÔNG throw lỗi vì thiếu session |
| L2 | SRS không nói rõ có bypass payment/booking flow (UC75/76) hay không | `consultation_bookings` có `expert_price_id NOT NULL`, `scheduled_start/end NOT NULL` — dữ liệu UC153 không có | Test khẳng định UC153 KHÔNG insert `consultation_bookings` — chỉ `nearby_expert_contact_requests` với `converted_booking_id = NULL` |
| L3 | SRS không có số cụ thể cho dedup window | TDS §4.2 đề xuất 5 phút (Open, chưa BR nguồn) | Test dùng giá trị 5 phút làm oracle tạm thời, đánh dấu comment "Open — TDS §18 RG-2" trong test code |
| L4 | Channel `CHAT` — UC144 không tồn tại | ADR-CONTACT-002 quyết định trả 202 kèm `channelImplementationStatus=NOT_IMPLEMENTED` thay vì lỗi cứng | Test khẳng định response 202 (KHÔNG 501/404) cho channel CHAT, với field `channelImplementationStatus` đúng giá trị |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ContactNearbyExpert module bao gồm các layer:
├── Domain (ChannelType enum, NearbyExpertContactRequest entity — pure logic)
├── Service (ContactNearbyExpertService — mock JPA Repository với Mockito)
├── Repository (IExpertLocationShareRepository.existsActiveVerifiedShare(),
│                INearbyExpertContactRequestRepository — mock với Mockito cho unit test)
├── Controller (ContactNearbyExpertController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest, verify Flyway migration
                  V20260705150100 + full contact() flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-153` (§3.3.7.2) | Mother chọn 1 expert nearby và gửi chat/voice/video/booking request |
| `ADR-CONTACT-001` | Re-validate expert VERIFIED + active share tại thời điểm contact |
| `ADR-CONTACT-002` | Channel dispatch — CHAT trả NOT_IMPLEMENTED, VOICE/VIDEO/BOOKING trả AVAILABLE |
| `ADR-CONTACT-003` | KHÔNG tạo `consultation_bookings` — dùng `nearby_expert_contact_requests` |
| `ADR-CONTACT-004` | userId từ JWT SecurityContext; `location_snapshots` best-effort |
| `BR-RBAC` | Chỉ ROLE_MOTHER được gọi endpoint |
| PDPA / Luật 91/2025 | Không log PII (message/toạ độ) ở mức INFO; `location_snapshots` có TTL |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Contact expert VERIFIED + active share → tạo request thành công | `ContactNearbyExpertService.contact()` | `MAP153-TC-001` |
| TC-COND-002 | Contact expert KHÔNG VERIFIED hoặc share hết hạn → 404 MAP-303 | `ContactNearbyExpertService.contact()` | `MAP153-TC-002`, `MAP153-TC-003` |
| TC-COND-003 | Channel=VOICE → `channelImplementationStatus=AVAILABLE` | `IConsultationChannelDispatcher.resolveImplementationStatus()` | `MAP153-TC-004` |
| TC-COND-004 | Channel=VIDEO → `channelImplementationStatus=AVAILABLE` | `IConsultationChannelDispatcher.resolveImplementationStatus()` | `MAP153-TC-005` |
| TC-COND-005 | Channel=BOOKING → `channelImplementationStatus=AVAILABLE`, KHÔNG tạo `consultation_bookings` | `ContactNearbyExpertService.contact()` | `MAP153-TC-006` |
| TC-COND-006 | Channel=CHAT → `channelImplementationStatus=NOT_IMPLEMENTED`, response vẫn 202 | `IConsultationChannelDispatcher.resolveImplementationStatus()` | `MAP153-TC-007` |
| TC-COND-007 | KHÔNG có insert nào vào `consultation_bookings` từ toàn bộ luồng UC153 | Integration test DB assertion | `MAP153-TC-INT-001` |
| TC-COND-008 | userId lấy từ JWT SecurityContext, không từ request body | `ContactNearbyExpertController` | `MAP153-TC-008` |
| TC-COND-009 | `location_snapshots` ghi best-effort — lỗi ghi KHÔNG chặn response | `ContactNearbyExpertService.contact()` | `MAP153-TC-009` |
| TC-COND-010 | Duplicate PENDING request (cùng requester+expert+channel trong 5 phút) → 409 MAP-302 | `ContactNearbyExpertService.contact()` | `MAP153-TC-010` |
| TC-COND-011 | Không có JWT / sai role → 401/403 | `ContactNearbyExpertController` | `MAP153-TC-011`, `MAP153-TC-012` |
| TC-COND-012 | `channelType` thiếu/không hợp lệ → 400 MAP-301 | `ContactNearbyExpertController` (`@Valid`) | `MAP153-TC-013` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `channelType` (CHAT / VOICE / VIDEO / BOOKING / invalid) | 4 kênh có hành vi `channelImplementationStatus` khác nhau, cần test riêng từng partition |
| Boundary Value Analysis | Dedup window (4:59 vs 5:01 phút kể từ request trước) | Xác nhận đúng ranh giới thời gian dedup |
| State Transition Testing | `nearby_expert_contact_requests.status` (chỉ tạo PENDING, không test transition khác — ngoài phạm vi) | UC153 chỉ ghi PENDING; test khẳng định KHÔNG tự chuyển trạng thái khác |
| Error Guessing | Expert đã bị revoke (UC104) giữa lúc search (UC149) và contact (UC153); share hết hạn đúng lúc gọi | Race condition thời gian thực giữa UC149 và UC153 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `expert_profiles { verification_status: 'VERIFIED' }` + `expert_location_shares { expires_at: now()+1h }` | Happy path — expert hợp lệ |
| `FX-002` | DB seed | `expert_profiles { verification_status: 'PENDING' }` | Expert chưa verified → MAP-303 |
| `FX-003` | DB seed | `expert_location_shares { expires_at: now()-1h }` (đã hết hạn) | Share hết hạn → MAP-303 |
| `FX-004` | DB seed | `nearby_expert_contact_requests { status: 'PENDING', created_at: now()-2min }` cùng requester+expert+channel | Duplicate dedup test |
| `FX-005` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context happy path |
| `FX-006` | JWT | `{ sub: 'expert-001', role: 'ROLE_EXPERT' }` | Auth context — wrong role → 403 |
| `FX-007` | Request body | `{ channelType: 'CHAT', message: 'Xin chào' }` | Channel not-implemented test |
| `FX-008` | Request body | `{ channelType: null }` | Validation failure test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// Naming convention reused from UC149's TestFactory pattern
// ═══════════════════════════════════════════════════════════

// ContactNearbyExpertTestFactory.java
class ContactNearbyExpertTestFactory {

    static final UUID DEFAULT_MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID DEFAULT_EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    static ExpertProfile makeVerifiedExpertProfile() {
        ExpertProfile p = new ExpertProfile();
        p.setExpertProfileId(DEFAULT_EXPERT_PROFILE_ID);
        p.setVerificationStatus("VERIFIED");
        return p;
    }

    static ExpertProfile makeVerifiedExpertProfile(Consumer<ExpertProfile> overrides) {
        ExpertProfile p = makeVerifiedExpertProfile();
        overrides.accept(p);
        return p;
    }

    static ExpertLocationShare makeActiveShare() {
        ExpertLocationShare s = new ExpertLocationShare();
        s.setExpertProfileId(DEFAULT_EXPERT_PROFILE_ID);
        s.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        return s;
    }

    static ExpertLocationShare makeActiveShare(Consumer<ExpertLocationShare> overrides) {
        ExpertLocationShare s = makeActiveShare();
        overrides.accept(s);
        return s;
    }

    static ContactNearbyExpertRequest makeContactRequest(ChannelType channel) {
        ContactNearbyExpertRequest r = new ContactNearbyExpertRequest();
        r.setChannelType(channel);
        r.setMessage("Test message — SYNTHETIC data only");
        return r;
    }

    static NearbyExpertContactRequest makeExistingPendingRequest(Instant createdAt, ChannelType channel) {
        NearbyExpertContactRequest r = new NearbyExpertContactRequest();
        r.setContactRequestId(UUID.randomUUID());
        r.setRequesterUserId(DEFAULT_MOTHER_USER_ID);
        r.setExpertProfileId(DEFAULT_EXPERT_PROFILE_ID);
        r.setChannelType(channel.name());
        r.setStatus("PENDING");
        r.setCreatedAt(createdAt);
        return r;
    }
}
```

---

### MAP153-TC-001 — Happy path: contact VERIFIED expert with active share via VOICE creates PENDING request

**Severity:** `HIGH`
**Feature Under Test:** `ContactNearbyExpertService.contact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ContactNearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-CONTACT-001 §Decision`, `ADR-CONTACT-002 §Decision`

**Preconditions:**
- `FX-001` (VERIFIED expert + active share), `FX-005` (Mother JWT)

**Test Steps:**
1. Mock `shareRepo.existsActiveVerifiedShare(expertProfileId)` → `true`
2. Mock `contactRepo.existsPendingDuplicate(...)` → `false`
3. Call `service.contact(makeContactRequest(VOICE), DEFAULT_EXPERT_PROFILE_ID, DEFAULT_MOTHER_USER_ID)`
4. Assert response

**Expected Result (PASS — hành vi đúng):**
- Response `status == "PENDING"`, `channelType == VOICE`, `channelImplementationStatus == "AVAILABLE"`
- `contactRepo.save(...)` called exactly once with `status=PENDING`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception thrown, or `contactRepo.save()` not invoked, or status != PENDING

**Current Status:** 🔴 Not written
**Implementation Note:** Verify service does NOT call any `IConsultationSessionService` mock (must not exist as a dependency at all per ADR-CONTACT-002).

---

### MAP153-TC-002 — Expert NOT verified → 404 MAP-303

**Severity:** `CRITICAL`
**Feature Under Test:** `ContactNearbyExpertService.contact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ContactNearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-CONTACT-001 §Decision`

**Preconditions:** `FX-002` (PENDING verification expert)

**Test Steps:**
1. Mock `shareRepo.existsActiveVerifiedShare(expertProfileId)` → `false`
2. Call `service.contact(makeContactRequest(CHAT), expertProfileId, motherUserId)`

**Expected Result (PASS):**
- Throws `NotFoundException` with error code `MAP-303`
- `contactRepo.save(...)` is NEVER called (no side effect on rejected contact)

**Expected Result (FAIL):**
- Request saved despite expert not verified (security bypass)

**Current Status:** 🔴 Not written

---

### MAP153-TC-003 — Expert location share expired → 404 MAP-303

**Severity:** `CRITICAL`
**Feature Under Test:** `ContactNearbyExpertService.contact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ContactNearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-CONTACT-001 §Decision` (mirrors `FX-003`)

**Preconditions:** `FX-003` (expired share)

**Test Steps:**
1. Mock `shareRepo.existsActiveVerifiedShare(expertProfileId)` → `false` (repo query already filters `expires_at > now()`)
2. Call `service.contact(...)`

**Expected Result (PASS):** `NotFoundException` `MAP-303`, no save.
**Expected Result (FAIL):** Contact succeeds against an expert whose share expired — PDPA violation.

**Current Status:** 🔴 Not written

---

### MAP153-TC-004 — Channel VOICE → channelImplementationStatus=AVAILABLE

**Severity:** `MEDIUM`
**Feature Under Test:** `IConsultationChannelDispatcher.resolveImplementationStatus()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ConsultationChannelDispatcherTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-CONTACT-002 §Decision`, `TDS §8.3`

**Test Steps:**
1. Call `dispatcher.resolveImplementationStatus(ChannelType.VOICE)`

**Expected Result (PASS):** Returns `"AVAILABLE"` (UC145 exists at time of writing).
**Expected Result (FAIL):** Returns `"NOT_IMPLEMENTED"` or throws.

**Current Status:** 🔴 Not written
**Implementation Note:** This mapping is a static/config-driven table per TDS §8.3 — MUST be re-verified whenever sibling UC144/145/146 implementation status changes (see TDS §18 RG-3).

---

### MAP153-TC-005 — Channel VIDEO → channelImplementationStatus=AVAILABLE

**Severity:** `MEDIUM`
**Feature Under Test:** `IConsultationChannelDispatcher.resolveImplementationStatus()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ConsultationChannelDispatcherTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-CONTACT-002 §Decision`

**Test Steps:**
1. Call `dispatcher.resolveImplementationStatus(ChannelType.VIDEO)`

**Expected Result (PASS):** Returns `"AVAILABLE"` (UC146 exists at time of writing).

**Current Status:** 🔴 Not written

---

### MAP153-TC-006 — Channel BOOKING → AVAILABLE, and NO consultation_bookings row created

**Severity:** `CRITICAL`
**Feature Under Test:** `ContactNearbyExpertService.contact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ContactNearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-CONTACT-003 §Decision`

**Preconditions:** `FX-001`

**Test Steps:**
1. Mock repos for happy path
2. Call `service.contact(makeContactRequest(BOOKING), expertProfileId, motherUserId)`
3. Assert no interaction with any `ConsultationBookingRepository` mock (inject a strict mock that is never touched — verifies the constraint at compile/mock level, not just DB)

**Expected Result (PASS):**
- Response `channelType=BOOKING`, `channelImplementationStatus=AVAILABLE`, `status=PENDING`
- `verifyNoInteractions(consultationBookingRepositoryMock)` — if such a repository is even injected (it should NOT be, per ADR-CONTACT-003; the mock's presence in the test at all is a smell — the correct test asserts the service constructor/dependencies simply do not include a booking repository)

**Expected Result (FAIL):**
- A `consultation_bookings` row created, or `ConsultationBookingRepository` injected into `ContactNearbyExpertService`

**Current Status:** 🔴 Not written
**Implementation Note:** Fastest correctness check is a class-dependency audit: `ContactNearbyExpertService`'s constructor signature must NOT include any `IConsultationBookingRepository` type.

---

### MAP153-TC-007 — Channel CHAT → channelImplementationStatus=NOT_IMPLEMENTED, response still 202 (not an error)

**Severity:** `HIGH`
**Feature Under Test:** `ContactNearbyExpertService.contact()` + `IConsultationChannelDispatcher`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ContactNearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-CONTACT-002 §Decision` (Logic Issue L4, §2)

**Preconditions:** `FX-001`, `FX-007`

**Test Steps:**
1. Mock happy-path repos
2. Call `service.contact(makeContactRequest(CHAT), expertProfileId, motherUserId)`

**Expected Result (PASS):**
- NO exception thrown (this is NOT an error path)
- Response `channelType=CHAT`, `status=PENDING`, `channelImplementationStatus="NOT_IMPLEMENTED"`
- Request IS persisted (`contactRepo.save()` called) — audit trail preserved even though channel unavailable

**Expected Result (FAIL):**
- Service throws `UnsupportedOperationException`/501, or fails to persist the request, or returns `AVAILABLE` for CHAT (false positive — misleads client that chat works)

**Current Status:** 🔴 Not written

---

### MAP153-TC-008 — userId sourced from JWT SecurityContext, not from request body/param

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ContactNearbyExpertController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/ContactNearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-CONTACT-004 §Decision`

**Test Steps (Attack Simulation):**
1. Authenticate as Mother A (`FX-005`, `sub=mother-001`)
2. Send `POST /api/v1/map/experts/{expertProfileId}/contact` with a body attempting to inject `requesterUserId: "mother-999"` (a field the DTO does not officially declare, or spoof via a different mechanism if the DTO were to include one)
3. Verify service is invoked with `userId` extracted from `SecurityContextHolder`/JWT `sub` claim (`mother-001`), never from request body

**Expected Result (PASS = hệ thống an toàn):**
- `nearby_expert_contact_requests.requester_user_id == mother-001` (from JWT), regardless of any body content
- `ContactNearbyExpertRequest` DTO (per TDS §8.1) has NO `requesterUserId`/`userId` field at all — compile-time enforcement

**Expected Result (FAIL = lỗ hổng tồn tại):**
- `requester_user_id` reflects a client-supplied value

**Current Status:** 🔴 Not written

---

### MAP153-TC-009 — location_snapshots write failure does NOT block the contact response (best-effort)

**Severity:** `MEDIUM`
**Feature Under Test:** `ContactNearbyExpertService.contact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ContactNearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-CONTACT-004 §Decision` (mirrors UC149 ADR-MAP-204, UC63 ADR-MAP-002)

**Preconditions:** `FX-001`

**Test Steps:**
1. Mock `locationSnapshotRepository.save(...)` to throw `DataAccessException`
2. Call `service.contact(...)`

**Expected Result (PASS):**
- Method returns normally with `202`-equivalent `ContactNearbyExpertResponse` (status=PENDING)
- `contactRepo.save()` for the actual contact request still succeeded

**Expected Result (FAIL):**
- Exception propagates and the entire contact request fails because a best-effort audit write failed

**Current Status:** 🔴 Not written

---

### MAP153-TC-010 — Duplicate PENDING request within 5 minutes → 409 MAP-302

**Severity:** `HIGH`
**Feature Under Test:** `ContactNearbyExpertService.contact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/ContactNearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §4.2 Data Integrity` (Open — TDS §18 RG-2, value 5min is a proposed default pending Product Owner confirmation)

**Preconditions:** `FX-004` (existing PENDING request created 2 minutes ago, same requester+expert+channel)

**Test Steps:**
1. Mock `shareRepo.existsActiveVerifiedShare(...)` → `true`
2. Mock `contactRepo.existsPendingDuplicate(motherUserId, expertProfileId, "VOICE", <now-5min>)` → `true`
3. Call `service.contact(makeContactRequest(VOICE), expertProfileId, motherUserId)`

**Expected Result (PASS):**
- Throws `ConflictException` with error code `MAP-302`
- No new row saved (`contactRepo.save()` never called)

**Expected Result (FAIL):**
- A duplicate PENDING row is created, allowing request spam to the same expert

**Current Status:** 🔴 Not written

---

### MAP153-TC-010b — Boundary: request 5 min 1 sec after prior PENDING request → allowed (not duplicate)

**Severity:** `MEDIUM`
**Feature Under Test:** `INearbyExpertContactRequestRepository.existsPendingDuplicate()`
**Test File:** `src/test/java/com/carebridge/backend/map/repository/NearbyExpertContactRequestRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §4.2` (boundary value analysis, TDS-04)

**Preconditions:** Testcontainers PostgreSQL; seed a PENDING row with `created_at = now() - 5min - 1sec`

**Test Steps:**
1. Call `repo.existsPendingDuplicate(userId, expertProfileId, "VOICE", now().minus(5, MINUTES))`

**Expected Result (PASS):** Returns `false` (outside the 5-minute window — allowed to retry).
**Expected Result (FAIL):** Returns `true` (window boundary off-by-one).

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### MAP153-TC-011 — No JWT → 401 IAM-001

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ContactNearbyExpertController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/ContactNearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** No `Authorization` header.

**Test Steps (Attack Simulation):**
1. `POST /api/v1/map/experts/{id}/contact` without JWT

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`, body `error.code == "IAM-001"`.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request processed without authentication.

**Current Status:** 🔴 Not written

---

### MAP153-TC-012 — Wrong role (ROLE_EXPERT) → 403 MAP-204

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ContactNearbyExpertController` (`@PreAuthorize("hasRole('MOTHER')")`)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/ContactNearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** `FX-006` (Expert JWT)

**Test Steps (Attack Simulation):**
1. Authenticate as `ROLE_EXPERT`
2. `POST /api/v1/map/experts/{id}/contact`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`, `error.code == "MAP-204"`.
**Expected Result (FAIL = lỗ hổng tồn tại):** Expert can create a contact request meant for Mother-initiated flow only.

**Current Status:** 🔴 Not written

---

### MAP153-TC-013 — channelType null/invalid → 400 MAP-301

**Severity:** `MEDIUM`
**Feature Under Test:** `ContactNearbyExpertController` (`@Valid ContactNearbyExpertRequest`)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/ContactNearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §9.2 400 Bad Request example`

**Preconditions:** `FX-005`, `FX-008` (`channelType: null`)

**Test Steps:**
1. `POST /api/v1/map/experts/{id}/contact` with `{ "channelType": null }`

**Expected Result (PASS):** `400 Bad Request`, `error.code == "MAP-301"`, `details[0].field == "channelType"`.
**Expected Result (FAIL):** Request accepted with null channel, or 500 instead of 400.

**Current Status:** 🔴 Not written

---

### MAP153-TC-013b — message exceeds 1000 chars → 400 MAP-301

**Severity:** `LOW`
**Feature Under Test:** `ContactNearbyExpertController` (`@Size(max=1000)`)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/ContactNearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Test Steps:**
1. `POST` with `message` = 1001-character SYNTHETIC string, valid `channelType`

**Expected Result (PASS):** `400 MAP-301`.
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### MAP153-TC-INT-001 — Full flow: seed VERIFIED expert + active share → contact via BOOKING → row persisted, no consultation_bookings side effect

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /contact → DB assertion`
**Test File:** `src/test/java/com/carebridge/backend/map/ContactNearbyExpertIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-005, TC-COND-007`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically on Spring context start, including `V20260705150100__create_nearby_expert_contact_requests.sql`
- Seed: `expert_profiles` (VERIFIED), `expert_location_shares` (active), `users` (mother)

**Test Steps:**
1. Seed minimal data (expert VERIFIED + active share, mother user)
2. `POST /api/v1/map/experts/{expertProfileId}/contact` with `Authorization: Bearer <MOTHER_JWT>`, body `{ channelType: "BOOKING", message: "..." }`
3. Assert HTTP 202
4. Query `nearby_expert_contact_requests` — assert 1 row with `status=PENDING`, `converted_booking_id IS NULL`
5. Query `consultation_bookings` — assert row count unchanged (0 new rows) before/after step 2

**Expected Result (PASS):**
- Response 202 with `contactRequestId`
- `nearby_expert_contact_requests` has exactly 1 new row, correct FK values
- `consultation_bookings` row count identical before and after (no side effect)

**Expected Result (FAIL):**
- `consultation_bookings` gains a row (ADR-CONTACT-003 violation), or `nearby_expert_contact_requests` missing/wrong values

**DB Assertion:**
```java
NearbyExpertContactRequest record = contactRepo.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo("PENDING");
assertThat(record.getConvertedBookingId()).isNull();
assertThat(consultationBookingRepo.count()).isEqualTo(countBeforeCall);
```

**Current Status:** 🔴 Not written

---

### MAP153-TC-INT-002 — Full flow: expert with expired share → 404, no row persisted

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /contact with expired share → DB assertion`
**Test File:** `src/test/java/com/carebridge/backend/map/ContactNearbyExpertIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`

**Preconditions:** Seed expert VERIFIED but `expert_location_shares.expires_at = now() - 1h`.

**Test Steps:**
1. `POST /api/v1/map/experts/{expertProfileId}/contact` with valid Mother JWT
2. Assert HTTP 404, `error.code == "MAP-303"`
3. Query `nearby_expert_contact_requests` — assert 0 rows for this expert/requester pair

**Expected Result (PASS):** No row created; correct error code.
**Expected Result (FAIL):** Row created despite invalid expert eligibility.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MAP153-TC-001` | `ContactNearbyExpertServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-002` | `ContactNearbyExpertServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-003` | `ContactNearbyExpertServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-004` | `ConsultationChannelDispatcherTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-005` | `ConsultationChannelDispatcherTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-006` | `ContactNearbyExpertServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-007` | `ContactNearbyExpertServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-008` | `ContactNearbyExpertControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-009` | `ContactNearbyExpertServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-010` | `ContactNearbyExpertServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-010b` | `NearbyExpertContactRequestRepositoryIntegrationTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-011` | `ContactNearbyExpertControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-012` | `ContactNearbyExpertControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-013` | `ContactNearbyExpertControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-013b` | `ContactNearbyExpertControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-INT-001` | `ContactNearbyExpertIntegrationTest.java` | `[ ]` | `[ ]` | |
| `MAP153-TC-INT-002` | `ContactNearbyExpertIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ContactNearbyExpertService implements IContactNearbyExpertService {

    @Override
    public ContactNearbyExpertResponse contact(ContactNearbyExpertRequest request, UUID expertProfileId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Service
public class ConsultationChannelDispatcher implements IConsultationChannelDispatcher {

    @Override
    public String resolveImplementationStatus(ChannelType channelType) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MAP153-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `MAP153-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP153-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___ (chưa thực hiện — chờ Approve TDS/Test-Spec)`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log — chưa tạo]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-006` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect / TV4-Lâm
- [ ] Flyway migration `V20260705150100__create_nearby_expert_contact_requests.sql` đã được approved và chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị
- [ ] Open Items §18 của TDS (RG-3, RG-6) đã có phản hồi từ Product Owner/TV4-Lâm (ít nhất tạm chấp nhận thiết kế hiện tại để tiếp tục implement, hoặc chính thức thay đổi ADR)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ContactNearbyExpertService`, `ConsultationChannelDispatcher`
- [ ] Không có business logic trong `ContactNearbyExpertController` (chỉ có validation + mapping)
- [ ] Không có PII/message content xuất hiện plaintext trong logs
- [ ] Xác nhận KHÔNG có bất kỳ `IConsultationBookingRepository`/`IConsultationSessionService` dependency nào bị inject vào `ContactNearbyExpertService` (kiểm tra constructor — ADR-CONTACT-002/003)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (kiểm tra `ContactNearbyExpertTestFactory` dùng đúng cách)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR-CONTACT-00X)

### Suspension Criteria (Điều kiện tạm dừng)

- UC144 (Consult via Chat) TDS xuất hiện với thiết kế mâu thuẫn với ADR-CONTACT-002 giả định hiện tại (`channelImplementationStatus` mapping cần cập nhật)
- UC75/UC76 (Book Private Consultation / Pay Consultation Fee) TDS xuất hiện và xác nhận UC153 PHẢI redirect sang luồng booking đầy đủ thay vì tạo `nearby_expert_contact_requests` đơn giản (ADR-CONTACT-003 cần Supersede)
- Migration `V20260705150000` (UC149's proposed geo-index) được approve trước và xung đột numbering với `V20260705150100`

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS nearby_expert_contact_requests CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260705150100';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/map/controller/ContactNearbyExpertController.java
git checkout -- src/main/java/com/carebridge/backend/map/service/
git checkout -- src/main/java/com/carebridge/backend/map/repository/INearbyExpertContactRequestRepository.java
git checkout -- src/main/resources/db/migration/V20260705150100__create_nearby_expert_contact_requests.sql
git checkout -- src/test/java/com/carebridge/backend/map/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu áp dụng cho dự án này)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có `Oracle Source` | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — mọi assumption trace về ADR-CONTACT-00X | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — MAP153-TC-008/011/012/013 chỉ test auth/validation ở controller layer | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — tất cả types (`IContactNearbyExpertService`, `IConsultationChannelDispatcher`, `INearbyExpertContactRequestRepository`) được định nghĩa trong TDS §8 companion document trước khi test này viết ra | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-writing → TDD spec approved for Red Gate execution
- [ ] Red Gate (§5.1) execution pending — AP-AI-002 check chỉ có thể xác nhận sau khi stub code tồn tại

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none tại thời điểm viết spec)_ | — | — | — | — |

---

*TDD Spec v1.0 — Companion document cho `CB-MAP-IMP-006` (`UC153_ContactNearbyExpert_TDS.md`).*
*Status: Draft. Chờ Approve trước khi bắt đầu Red Gate (§5.1).*
