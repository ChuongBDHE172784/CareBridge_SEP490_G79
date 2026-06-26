# TEST SPECIFICATION — UC-174 Investigate Security Incident
# Đặc tả Kiểm thử — Điều tra Sự cố Bảo mật

| Field | Value |
|-------|-------|
| **Document ID** | `CB-SEC-IMP-001-TEST` |
| **Version** | `1.0` |
| **Date** | `2026-06-26` |
| **Status** | `Draft` |
| **Document Owner** | `PhuongNT` |
| **Author** | `AI Agent` |
| **Reviewed by** | `[Tech Lead]` |
| **DPO Sign-off** | `[ ] Pending` |
| **Approved by** | `[Principal Architect]` |
| **Last Review** | `2026-06-26` |
| **Standard** | `ISO/IEC/IEEE 29119-3:2021` |
| **Based on EDS** | `v2.0` |
| **TDS Reference** | `CB-SEC-IMP-001` |

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo Test-Spec cho UC-174 |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / UC ID** | `UC-174` |
| **Module** | `SecurityIncidentInvestigation — audit (security sub-domain)` |
| **Spec gốc** | `CB-SEC-IMP-001` |
| **Priority** | 🔴 P0 (Security-critical module) |
| **Data Classification** | `Confidential` |
| **Compliance Scope** | `GDPR Art. 32, PDPA` |
| **Upstream Dependencies** | `IAM (JWT), AuditService, SecurityEvent entity` |
| **Downstream Consumers** | `UC-175 ReviewSecurityEvent, AdminDashboard` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SEC-IMP-001 §17` |
| **Constraints Injected** | C1 (ROLE_ADMIN only), C2 (no delete), C3 (no sensitive fields), C4 (meta-audit), C5 (page size clamp), C6 (adminId from JWT), C7 (JpaSpecificationExecutor) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (cần làm rõ) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SecurityEvent.id có kiểu Long (BIGINT) trong entity hiện tại | Task yêu cầu UUID — migration cần align | Test dùng UUID cho các endpoint mới; entity cũ vẫn dùng Long đến khi migration hoàn tất |
| L2 | SecurityEvent thiếu các cột: severity, status, correlationId, payload | Cần migration V2 trước khi test integration | Unit test dùng mock; integration test chạy sau migration V2 |
| L3 | Role trong codebase là `ROLE_SYSTEM_ADMIN` (từ AuditController) | Task spec dùng `ROLE_ADMIN` | Test dùng `hasRole('SYSTEM_ADMIN')` khớp với AuditController pattern hiện tại |
| L4 | Meta-audit action: codebase dùng `AuditAction.VIEW_AUDIT_LOG` | Enum `AuditAction` đã tồn tại | Test verify `AuditAction.VIEW_AUDIT_LOG` được ghi, không tự bịa action mới |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
UC-174 SecurityIncidentInvestigation bao gồm:
├── Unit Tests
│   ├── SecurityIncidentServiceImpl.searchEvents() — logic, pagination, meta-audit
│   ├── SecurityIncidentServiceImpl.getTimeline() — lookup, correlationId grouping
│   └── SecurityEventMapper — sensitive field exclusion
├── Integration Tests
│   ├── SecurityIncidentController → Service → Repository → PostgreSQL (Testcontainers)
│   └── Meta-audit persistence verification
└── Security / E2E Tests
    ├── ROLE_ADMIN enforcement (403 for non-admin)
    ├── 401 without JWT
    └── Sensitive field exclusion in actual HTTP response
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `UC-174` | Tìm kiếm/lọc security events, xem timeline |
| `ADR-174-001` | Pagination bắt buộc, max 100/page |
| `ADR-174-002` | Append-only: no update/delete |
| `BR-SEC-001` | ROLE_ADMIN only |
| `BR-SEC-003` | Meta-audit mọi admin query |
| `BR-SEC-006` | Sensitive field exclusion |
| `CB-SEC-IMP-001 §9` | API contract: filter params, response shape |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin tìm kiếm với filter hợp lệ | `searchEvents()` happy path | `SEC174-TC-001` |
| TC-COND-002 | Lọc theo khoảng ngày hợp lệ | `searchEvents()` date filter | `SEC174-TC-002` |
| TC-COND-003 | Lọc theo userId | `searchEvents()` userId filter | `SEC174-TC-003` |
| TC-COND-004 | Phân trang đúng với size max 100 | `searchEvents()` pagination | `SEC174-TC-004` |
| TC-COND-005 | Non-admin bị từ chối | Controller `@PreAuthorize` | `SEC174-TC-005` |
| TC-COND-006 | Meta-audit được ghi | `AuditService.log()` called | `SEC174-TC-006` |
| TC-COND-007 | Sensitive fields không trong response | Mapper exclusion | `SEC174-TC-007` |
| TC-COND-008 | Kết quả rỗng khi không có events | `searchEvents()` empty result | `SEC174-TC-008` |
| TC-COND-009 | Timeline hợp lệ theo correlationId | `getTimeline()` | `SEC174-TC-009` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| Equivalence Partitioning | filter params (valid/invalid/null) | Reduce redundant test cases |
| Boundary Value Analysis | page size (0, 1, 100, 101, 200) | Verify clamp at 100 |
| State Transition Testing | N/A — UC-174 là read-only, không có state machine | — |
| Error Guessing | Non-admin JWT, no JWT, malformed UUID | Security attack simulation |
| Decision Table Testing | Filter combinations (userId+eventType, dateRange only, etc.) | Ensure correct AND logic in Specification |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-SEC-001` | DB seed | 30 events: eventType=LOGIN_FAILED, userId=test-user-uuid-01, severity=MEDIUM | Happy path filter |
| `FX-SEC-002` | DB seed | 20 events: eventType=SUSPICIOUS_ACTIVITY, correlationId=test-corr-uuid-01 | Timeline test |
| `FX-SEC-003` | DB seed | 0 events khớp filter type=TOKEN_REVOKED AND userId=nonexistent-uuid | Empty result test |
| `FX-SEC-004` | JWT | `{ sub: 'admin-uuid', role: 'ROLE_SYSTEM_ADMIN' }` | Admin auth context |
| `FX-SEC-005` | JWT | `{ sub: 'user-uuid', role: 'ROLE_USER' }` | Non-admin auth context (expect 403) |
| `FX-SEC-006` | DB seed | 1 event với payload JSONB chứa `attempted_password_hash` synthetic | Sensitive field exclusion test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class SecurityIncidentTestFactory {
    static SecurityIncident makeValidIncident() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new SecurityIncident.SecurityIncidentBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static SecurityIncident makeValidIncident(Consumer<SecurityIncident> overrides) {
        var entity = makeValidIncident();
        overrides.accept(entity);
        return entity;
    }
}
```

> **TC ID format:** `SEC174-TC-[NNN]`
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

---

### SEC174-TC-001 — Tìm kiếm Security Events thành công với filter eventType

**Severity:** `HIGH`
**Feature Under Test:** `SecurityIncidentServiceImpl.searchEvents()`
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityIncidentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-174 AC-01`, `CB-SEC-IMP-001 §9.2`

**Preconditions:**
- Repository mock trả về Page<SecurityEvent> với 5 events có eventType=LOGIN_FAILED
- AuditService mock sẵn sàng
- adminId = "test-admin-uuid-001"

**Test Steps:**
1. Arrange: Mock `SecurityIncidentRepository.findAll(Specification, Pageable)` trả về 5 events LOGIN_FAILED
2. Act: Gọi `service.searchEvents(filter{eventType=LOGIN_FAILED, page=0, size=20}, adminId)`
3. Assert: Kiểm tra kết quả

**Expected Result (PASS):**
- Trả về `Page<SecurityEventSummaryResponse>` với 5 phần tử
- Tất cả phần tử có `eventType = LOGIN_FAILED`
- `AuditService.log()` được gọi đúng 1 lần với `AuditAction.VIEW_AUDIT_LOG`
- Response DTO không có trường `passwordHash`, `rawToken`, `details` nhạy cảm

**Expected Result (FAIL — dấu hiệu lỗi):**
- Service không gọi AuditService → vi phạm BR-SEC-003
- Response chứa sensitive fields → vi phạm BR-SEC-006

**Current Status:** 🔴 Not written

---

### SEC174-TC-002 — Lọc theo khoảng ngày hợp lệ

**Severity:** `MEDIUM`
**Feature Under Test:** `SecurityIncidentServiceImpl.searchEvents()` — date range filter
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityIncidentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-SEC-IMP-001 §9.2` — filter params spec

**Preconditions:**
- Repository mock kiểm tra Specification được truyền vào chứa date range predicate
- fromDate = 2026-06-01T00:00:00Z, toDate = 2026-06-26T23:59:59Z

**Test Steps:**
1. Arrange: Spy on `SecurityIncidentRepository` để capture Specification được truyền
2. Act: Gọi `service.searchEvents(filter{fromDate, toDate}, adminId)`
3. Assert: Verify Specification có predicate cho `occurred_at BETWEEN fromDate AND toDate`

**Expected Result (PASS):**
- Repository được gọi với Specification không null
- Tất cả events trả về có `occurredAt` trong khoảng [fromDate, toDate]
- Meta-audit được ghi với filters chứa fromDate/toDate

**Expected Result (FAIL):**
- Repository được gọi với Specification bỏ qua date range
- Events ngoài khoảng ngày xuất hiện trong kết quả

**Current Status:** 🔴 Not written

---

### SEC174-TC-003 — Lọc theo userId

**Severity:** `MEDIUM`
**Feature Under Test:** `SecurityIncidentServiceImpl.searchEvents()` — userId filter
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityIncidentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `UC-174`, `CB-SEC-IMP-001 §9.2`

**Preconditions:**
- targetUserId = UUID.fromString("test-user-uuid-01")
- Repository mock trả về events chỉ của targetUserId

**Test Steps:**
1. Arrange: Mock repository trả về 3 events với userId=targetUserId
2. Act: Gọi `service.searchEvents(filter{userId=targetUserId}, adminId)`
3. Assert: Tất cả events có userId = targetUserId

**Expected Result (PASS):**
- Tất cả `SecurityEventSummaryResponse` có `userId = targetUserId`
- Page trả về đúng count

**Current Status:** 🔴 Not written

---

### SEC174-TC-004 — Pagination: page size bị clamp tại 100

**Severity:** `HIGH`
**Feature Under Test:** `SecurityIncidentServiceImpl.searchEvents()` — page size enforcement
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityIncidentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-174-001`, `BR-SEC-002`

**Preconditions:**
- Repository mock sẵn sàng
- Request với size=500 (vượt giới hạn)

**Test Steps:**
1. Arrange: Capture Pageable được truyền vào Repository
2. Act: Gọi `service.searchEvents(filter{size=500}, adminId)`
3. Assert: Pageable được truyền vào Repository có `pageSize ≤ 100`

**Expected Result (PASS):**
- Pageable.getPageSize() = 100 (không phải 500)
- Service không throw exception, tự clamp size
- Page kết quả trả về có `size ≤ 100`

**Expected Result (FAIL):**
- Repository được gọi với size=500 → DB query không kiểm soát

**Current Status:** 🔴 Not written

---

### SEC174-TC-005 — Non-Admin bị từ chối truy cập (403)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `SecurityIncidentController` — `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/audit/controller/SecurityIncidentControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-SEC-001`, `ADR-174-001`

**Preconditions:**
- User với ROLE_USER (không có ROLE_ADMIN)
- JWT hợp lệ nhưng role sai

**Test Steps:**
1. Arrange: MockMvc setup với JWT có claims `{role: "ROLE_USER"}`
2. Act: `GET /api/v1/admin/security-events` với JWT đó
3. Assert: Response status, error code

**Expected Result (PASS = hệ thống an toàn):**
- HTTP status = 403
- Response body chứa `"code": "SEC-004"`
- Service layer KHÔNG được gọi (Spring Security chặn trước)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- HTTP status = 200 → Broken Access Control nghiêm trọng

**Current Status:** 🔴 Not written

---

### SEC174-TC-006 — Meta-Audit được ghi sau mỗi Admin Query

**Severity:** `HIGH`
**Feature Under Test:** `SecurityIncidentServiceImpl.searchEvents()` — meta-audit side effect
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityIncidentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-SEC-003`, `GDPR Art. 32`

**Preconditions:**
- AuditService mock (verify interaction)
- Valid admin call

**Test Steps:**
1. Arrange: Mock AuditService, mock Repository
2. Act: Gọi `service.searchEvents(anyFilter, adminId)` 3 lần
3. Assert: AuditService.log() được gọi đúng 3 lần

**Expected Result (PASS):**
- `verify(auditService, times(3)).log(eq(AuditAction.VIEW_AUDIT_LOG), eq(adminId), any(), any())`
- Mỗi call có `entityType = "SECURITY_EVENT_QUERY"`

**Expected Result (FAIL):**
- AuditService.log() bị bỏ qua → vi phạm GDPR Art. 32 audit requirement

**Current Status:** 🔴 Not written

---

### SEC174-TC-007 — Sensitive Fields bị loại trừ khỏi Response

**Severity:** `CRITICAL`
**OWASP:** `A02:2021 — Cryptographic Failures`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Feature Under Test:** `SecurityEventMapper` — sensitive field exclusion
**Test File:** `src/test/java/com/carebridge/backend/audit/mapper/SecurityEventMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-SEC-006`, `GDPR Art. 5.1(c)`

**Preconditions:**
- SecurityEvent entity có payload JSONB chứa `{"attempted_password_hash": "bcrypt$...", "user_agent": "Mozilla..."}`
- `details` field chứa thông tin nhạy cảm synthetic

**Test Steps:**
1. Arrange: Tạo SecurityEvent với sensitive payload
2. Act: Gọi `SecurityEventMapper.toSummaryResponse(event)`
3. Assert: Inspect toàn bộ fields của SecurityEventSummaryResponse

**Expected Result (PASS):**
- `SecurityEventSummaryResponse` KHÔNG có field nào tên `passwordHash`, `rawToken`, `hash`, `secret`
- `payload` field trong response là null hoặc sanitized version
- Các fields hợp lệ vẫn có mặt: `id`, `eventType`, `userId`, `ipAddress`, `severity`, `status`, `occurredAt`

**Expected Result (FAIL):**
- Response chứa `passwordHash` hoặc bất kỳ sensitive field nào

**Current Status:** 🔴 Not written

---

### SEC174-TC-008 — Kết quả rỗng khi không có events khớp filter

**Severity:** `MEDIUM`
**Feature Under Test:** `SecurityIncidentServiceImpl.searchEvents()` — empty result
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityIncidentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-SEC-IMP-001 §9.2`

**Preconditions:**
- Repository mock trả về Page.empty()
- Filter với userId không tồn tại

**Test Steps:**
1. Arrange: Mock repository trả về `Page.empty(pageable)`
2. Act: Gọi `service.searchEvents(filter{userId=nonexistent-uuid}, adminId)`
3. Assert: Kết quả

**Expected Result (PASS):**
- Trả về Page rỗng (không throw exception)
- `totalElements = 0`, `content = []`
- Meta-audit vẫn được ghi (dù kết quả rỗng)
- HTTP 200 (không phải 404)

**Current Status:** 🔴 Not written

---

### SEC174-TC-009 — Integration: Timeline trả về đúng events theo correlationId

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → Repository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/audit/controller/SecurityIncidentControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL container running (Testcontainers)
- Migration V2 đã apply
- Seed: 5 events với correlationId="test-corr-uuid-01", 3 events với correlationId khác
- Seed: 1 event gốc với id="test-event-uuid-01", correlationId="test-corr-uuid-01"
- Admin JWT hợp lệ

**Test Steps:**
1. Seed database với 8 events (5+3)
2. `GET /api/v1/admin/security-events/test-event-uuid-01/timeline` với admin JWT
3. Assert response

**Expected Result (PASS):**
- HTTP 200
- `data.correlationId = "test-corr-uuid-01"`
- `data.totalEvents = 5`
- `data.events` có đúng 5 phần tử, tất cả có `correlationId = "test-corr-uuid-01"`
- `data.events` được sắp xếp theo `occurredAt ASC`
- `audit_logs` có 1 bản ghi mới với `action = VIEW_AUDIT_LOG`

**DB Assertion:**
```sql
-- Verify timeline events
SELECT COUNT(*) FROM security_events
WHERE correlation_id = 'test-corr-uuid-01';
-- Expected: 5

-- Verify meta-audit
SELECT COUNT(*) FROM audit_logs
WHERE action = 'VIEW_AUDIT_LOG'
  AND new_value_json::jsonb ->> 'action' = 'VIEW_TIMELINE';
-- Expected: >= 1
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SEC174-TC-001` | `SecurityIncidentServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-002` | `SecurityIncidentServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-003` | `SecurityIncidentServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-004` | `SecurityIncidentServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-005` | `SecurityIncidentControllerTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-006` | `SecurityIncidentServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-007` | `SecurityEventMapperTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-008` | `SecurityIncidentServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC174-TC-009` | `SecurityIncidentControllerIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với stub sau. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// SecurityIncidentServiceImpl.java — Red Phase stub
@Service
public class SecurityIncidentServiceImpl implements ISecurityIncidentService {

    @Override
    public Page<SecurityEventSummaryResponse> searchEvents(
            SecurityEventFilterRequest filter, UUID adminId, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SecurityEventTimelineResponse getTimeline(UUID eventId, UUID adminId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Root Cause nếu PASS bất thường |
|-------|-------------|----------|--------------------------------|
| `SEC174-TC-001` | throws UnsupportedOperationException | 🔴 FAIL | ☐ Tautology ☐ Shared state |
| `SEC174-TC-002` | throws UnsupportedOperationException | 🔴 FAIL | ☐ Hallucinated import |
| `SEC174-TC-003` | throws UnsupportedOperationException | 🔴 FAIL | — |
| `SEC174-TC-004` | throws UnsupportedOperationException | 🔴 FAIL | — |
| `SEC174-TC-005` | 403 từ Spring Security (trước khi vào stub) | 🔴 FAIL (nếu @PreAuthorize chưa config) | ☐ Security config missing |
| `SEC174-TC-006` | throws UnsupportedOperationException | 🔴 FAIL | — |
| `SEC174-TC-007` | NullPointerException hoặc compile error | 🔴 FAIL | — |
| `SEC174-TC-008` | throws UnsupportedOperationException | 🔴 FAIL | — |
| `SEC174-TC-009` | 404 hoặc 500 | 🔴 FAIL | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SEC-IMP-001` đã được approve
- [ ] Flyway migration V2 đã được review và test trên staging
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị (synthetic data only)
- [ ] Testcontainers dependency đã có trong `pom.xml`

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test` — tất cả 9 test cases xanh (không có skip)
- [ ] `./mvnw test -pl 05_Development/CareBridgeAPI` — không có integration test fail
- [ ] Test coverage ≥ 80% lines cho: `SecurityIncidentServiceImpl`, `SecurityIncidentController`, `SecurityEventMapper`
- [ ] Không có sensitive field (`password`, `token`, `hash`, `secret`) xuất hiện trong bất kỳ test response
- [ ] Meta-audit test (TC-006, TC-009) xác nhận `audit_logs` có entry sau mỗi admin query
- [ ] TC-005 confirm 403 cho non-admin access

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 9 tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — mọi class/interface import trong test đều compile thành công
  ```bash
  ./mvnw compile -pl 05_Development/CareBridgeAPI 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn BR/AC/ADR trong test comment

### Suspension Criteria

- Migration V2 chưa sẵn sàng (integration tests bị block)
- `SecurityEvent` entity chưa được cập nhật với các fields mới
- Phát hiện lỗi kiến trúc mới cần Principal Architect review

---

## 7. Rollback Plan

```bash
# Revert migration nếu cần (chỉ trên dev/staging)
./mvnw flyway:undo -pl 05_Development/CareBridgeAPI

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/db/migration/V2__security_events_enhanced.sql

# UC-174 vẫn OPEN → giữ Status = Draft trong header document
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong Test Spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume delete endpoint tồn tại | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify service có Spring annotation logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `SecurityIncidentService` chưa tồn tại | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*Test-Spec v1.0 — UC-174 Investigate Security Incident*
*Tích hợp CASE 2.0 Red Gate Protocol và Anti-Pattern Detection.*
*Sections đánh dấu ⭐ là bổ sung từ CASE 2.0 methodology.*
