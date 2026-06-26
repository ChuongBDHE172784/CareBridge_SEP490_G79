# TEST SPECIFICATION — UC-175 Review Security Event
# Đặc tả Kiểm thử — Xét duyệt Sự kiện Bảo mật

| Field | Value |
|-------|-------|
| **Document ID** | `CB-SEC-IMP-002-TEST` |
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
| **TDS Reference** | `CB-SEC-IMP-002` |

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo Test-Spec cho UC-175 |

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
| **Feature / UC ID** | `UC-175` |
| **Module** | `SecurityEventReview — audit (security sub-domain)` |
| **Spec gốc** | `CB-SEC-IMP-002` |
| **Priority** | 🔴 P0 (Security-critical module) |
| **Data Classification** | `Confidential` |
| **Compliance Scope** | `GDPR Art. 32, PDPA` |
| **Upstream Dependencies** | `IAM (JWT), AuditService, SecurityEvent (UC-174), Firebase FCM` |
| **Downstream Consumers** | `AdminDashboard, FCM admin notifications` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SEC-IMP-002 §17` |
| **Constraints Injected** | C1 (ROLE_ADMIN only), C2 (no delete), C3 (state machine), C4 (FCM async), C5 (audit), C6 (sanitizePayload), C7 (adminId from JWT), C8 (note @PreRemove) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (cần làm rõ) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | State machine: spec chỉ nói "OPEN → UNDER_REVIEW → RESOLVED/FALSE_POSITIVE" | Re-open là hợp lệ: UNDER_REVIEW → OPEN | Test TC-003 phải verify UNDER_REVIEW → OPEN thành công |
| L2 | FCM gửi đồng bộ hay bất đồng bộ? | ADR-175-002: @TransactionalEventListener(AFTER_COMMIT) = async | Integration test phải verify FCM được gọi AFTER commit, không before |
| L3 | reviewedBy/reviewedAt được set khi nào? | Chỉ set khi transition sang RESOLVED hoặc FALSE_POSITIVE | Test verify reviewedBy null khi status=UNDER_REVIEW, non-null khi RESOLVED |
| L4 | Có thể thêm note vào event RESOLVED không? | Spec không cấm — notes có thể thêm ở bất kỳ status nào | Test TC-007 verify note có thể thêm vào event đang RESOLVED |
| L5 | Payload sanitization: key nào bị strip? | BR-REV-006 + C6: `password`, `token`, `hash`, `secret`, `key`, `credential` | Test TC-008 verify từng key trong danh sách này bị loại bỏ |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
UC-175 SecurityEventReview bao gồm:
├── Unit Tests
│   ├── SecurityEventReviewServiceImpl.getEventDetail() — detail view, payload sanitization
│   ├── SecurityEventReviewServiceImpl.addReviewNote() — note creation, audit
│   ├── SecurityEventReviewServiceImpl.changeStatus() — state machine validation, FCM trigger
│   └── SecurityEventDetailMapper.sanitizePayload() — sensitive field exclusion
├── Integration Tests
│   ├── Controller → Service → Repository → PostgreSQL (Testcontainers)
│   ├── FCM notification trigger on RESOLVED (mock Firebase)
│   └── Note immutability via @PreRemove hook
└── Security / E2E Tests
    ├── ROLE_ADMIN enforcement (403 for non-admin) — all 3 endpoints
    ├── Note immutability (cannot DELETE via repository)
    └── Terminal state enforcement (RESOLVED/FALSE_POSITIVE cannot transition)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `UC-175` | View detail, add note, change status |
| `ADR-175-001` | State machine: ALLOWED_TRANSITIONS, terminal states |
| `ADR-175-002` | FCM async via @TransactionalEventListener(AFTER_COMMIT) |
| `BR-REV-001` | ROLE_ADMIN only |
| `BR-REV-003` | Note immutability |
| `BR-REV-004` | FCM on RESOLVED |
| `BR-REV-005` | Audit all status changes |
| `BR-REV-006` | Payload sanitization |
| `CB-SEC-IMP-002 §6.4` | State machine diagram |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin xem chi tiết event thành công | `getEventDetail()` happy path | `SEC175-TC-001` |
| TC-COND-002 | Admin thêm review note thành công | `addReviewNote()` happy path | `SEC175-TC-002` |
| TC-COND-003 | Status transition hợp lệ | `changeStatus()` valid transition | `SEC175-TC-003` |
| TC-COND-004 | Status transition không hợp lệ | `changeStatus()` invalid transition | `SEC175-TC-004` |
| TC-COND-005 | Non-admin bị từ chối | Controller `@PreAuthorize` | `SEC175-TC-005` |
| TC-COND-006 | Review note immutability | `@PreRemove` hook | `SEC175-TC-006` |
| TC-COND-007 | FCM notification khi RESOLVED | `@TransactionalEventListener` | `SEC175-TC-007` |
| TC-COND-008 | Payload sanitization | `sanitizePayload()` | `SEC175-TC-008` |
| TC-COND-009 | Integration: Full flow add note + verify DB | Integration test | `SEC175-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| State Transition Testing | `SecurityEventStatus` FSM | Verify tất cả valid và invalid transitions |
| Boundary Value Analysis | noteText length (0, 1, 2000, 2001) | Verify @NotBlank và max length |
| Error Guessing | Terminal state bypass, delete attempt | Security attack simulation |
| Decision Table Testing | All transitions: OPEN/UNDER_REVIEW/RESOLVED/FALSE_POSITIVE → each other | Systematic coverage |
| Mock Object Testing | FcmNotificationService | Isolate FCM side effect |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-175-001` | DB seed | event{id:"e-uuid-01", status:OPEN, correlationId:"c-uuid-01"} | Happy path view/note |
| `FX-175-002` | DB seed | event{id:"e-uuid-02", status:UNDER_REVIEW, reviewedBy:"admin-uuid-01"} | Valid transition test |
| `FX-175-003` | DB seed | event{id:"e-uuid-03", status:RESOLVED, reviewedBy:"admin-uuid-01", reviewedAt:"2026-06-25"} | Terminal state test |
| `FX-175-004` | DB seed | event{id:"e-uuid-04", payload:{requestPath:"/api", attempted_password_hash:"bcrypt$..."}} | Payload sanitization test |
| `FX-175-005` | DB seed | notes[2] linked to e-uuid-01 | Notes history display |
| `FX-175-006` | JWT | {sub:"admin-uuid-01", role:"ROLE_SYSTEM_ADMIN"} | Valid admin auth |
| `FX-175-007` | JWT | {sub:"user-uuid-01", role:"ROLE_USER"} | Non-admin auth (expect 403) |
| `FX-175-008` | DB seed | event{id:"e-uuid-05", status:FALSE_POSITIVE} | FALSE_POSITIVE terminal state test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class SecurityEventTestFactory {
    static SecurityEvent makeValidEvent() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new SecurityEvent.SecurityEventBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static SecurityEvent makeValidEvent(Consumer<SecurityEvent> overrides) {
        var entity = makeValidEvent();
        overrides.accept(entity);
        return entity;
    }
}
```

> **TC ID format:** `SEC175-TC-[NNN]`
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

---

### SEC175-TC-001 — Xem Chi tiết Security Event Thành công

**Severity:** `HIGH`
**Feature Under Test:** `SecurityEventReviewServiceImpl.getEventDetail()`
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityEventReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-175 AC-01`, `CB-SEC-IMP-002 §9.2`

**Preconditions:**
- Mock `SecurityIncidentRepository.findById("e-uuid-01")` trả về FX-175-001
- Mock `SecurityEventNoteRepository.findByEventIdOrderByCreatedAtAsc("e-uuid-01")` trả về FX-175-005 (2 notes)
- Mock AuditService

**Test Steps:**
1. Arrange: Setup mocks như trên
2. Act: Gọi `service.getEventDetail(UUID("e-uuid-01"), UUID("admin-uuid-01"))`
3. Assert: Kiểm tra response

**Expected Result (PASS):**
- Response không null, chứa `id = "e-uuid-01"`
- `response.notes.size() = 2`
- `response.notes` sắp xếp theo `createdAt ASC`
- `response.payload` không chứa key `attempted_password_hash` (sanitized)
- `AuditService.log()` được gọi 1 lần với action = `VIEW_AUDIT_LOG`

**Expected Result (FAIL):**
- Notes không xuất hiện trong response
- Payload chứa sensitive fields

**Current Status:** 🔴 Not written
**Implementation Note:** `getEventDetail()` phải gọi cả `EventRepository.findById()` VÀ `NoteRepository.findByEventId()`, rồi gọi `sanitizePayload()` trước khi build response.

---

### SEC175-TC-002 — Thêm Review Note Thành công

**Severity:** `HIGH`
**Feature Under Test:** `SecurityEventReviewServiceImpl.addReviewNote()`
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityEventReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC-175 AC-02`, `BR-REV-003`

**Preconditions:**
- Event FX-175-001 tồn tại (status=OPEN)
- request = `AddReviewNoteRequest{noteText = "Ghi chú điều tra synthetic — không chứa PII"}`
- adminId = "admin-uuid-01"

**Test Steps:**
1. Arrange: Mock EventRepository trả về FX-175-001; Capture NoteRepository.save() argument
2. Act: Gọi `service.addReviewNote(UUID("e-uuid-01"), request, UUID("admin-uuid-01"))`
3. Assert: Kiểm tra note được persist và response

**Expected Result (PASS):**
- `NoteRepository.save()` được gọi 1 lần
- Note được save có `authorId = "admin-uuid-01"`, `noteText = "Ghi chú điều tra synthetic..."`
- Response `noteId` không null
- `AuditService.log()` được gọi với `AuditAction.SECURITY_EVENT`, entityId = "e-uuid-01"
- Response `createdAt` = thời điểm tạo (gần với `now()`)

**Expected Result (FAIL):**
- Note không được persist
- authorId trong note khác với adminId

**Current Status:** 🔴 Not written

---

### SEC175-TC-003 — Status Transition Hợp lệ: UNDER_REVIEW → RESOLVED

**Severity:** `CRITICAL`
**Feature Under Test:** `SecurityEventReviewServiceImpl.changeStatus()` — state machine + FCM trigger
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityEventReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-175-001`, `BR-REV-004`

**Preconditions:**
- Event FX-175-002 tồn tại (status=UNDER_REVIEW)
- Mock `ApplicationEventPublisher` để verify SecurityIncidentResolvedEvent
- request = `ChangeStatusRequest{newStatus=RESOLVED, reason="Đã xử lý synthetic"}`

**Test Steps:**
1. Arrange: Mock EventRepository, mock ApplicationEventPublisher
2. Act: Gọi `service.changeStatus(UUID("e-uuid-02"), request, UUID("admin-uuid-01"))`
3. Assert: Kiểm tra status change và event publication

**Expected Result (PASS):**
- `EventRepository.save()` được gọi với event có `status = RESOLVED`
- `event.reviewedBy = "admin-uuid-01"`
- `event.reviewedAt` không null và gần với `Instant.now()`
- `ApplicationEventPublisher.publishEvent()` được gọi 1 lần với `SecurityIncidentResolvedEvent`
- `SecurityIncidentResolvedEvent.eventId = "e-uuid-02"`
- `AuditService.log()` được gọi với `AuditAction.SECURITY_EVENT`
- Response `status = RESOLVED`

**Expected Result (FAIL):**
- Status không thay đổi
- FCM event không được publish
- reviewedBy = null sau khi RESOLVED

**Current Status:** 🔴 Not written

---

### SEC175-TC-004 — Status Transition Không Hợp lệ: RESOLVED → OPEN (Terminal State)

**Severity:** `CRITICAL`
**OWASP:** `A04:2021 — Insecure Design`
**Feature Under Test:** `SecurityEventReviewServiceImpl.changeStatus()` — terminal state enforcement
**Test File:** `src/test/java/com/carebridge/backend/audit/service/SecurityEventReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-175-001 §Decision`, `CB-SEC-IMP-002 §6.4`

**Preconditions:**
- Event FX-175-003 tồn tại (status=RESOLVED)
- request = `ChangeStatusRequest{newStatus=OPEN}`

**Test Steps:**
1. Arrange: Mock EventRepository trả về FX-175-003 (status=RESOLVED)
2. Act: Gọi `service.changeStatus(UUID("e-uuid-03"), request, UUID("admin-uuid-01"))`
3. Assert: Exception behavior và side effects

**Expected Result (PASS = hệ thống an toàn):**
- `ValidationException` được throw với error code `SEC-007`
- Message chứa thông tin về terminal state
- `EventRepository.save()` KHÔNG được gọi
- `ApplicationEventPublisher.publishEvent()` KHÔNG được gọi
- `AuditService.log()` KHÔNG được gọi (lỗi xảy ra trước khi audit)

**Expected Result (FAIL = lỗ hổng):**
- Status được thay đổi thành OPEN → vi phạm nghiêm trọng ADR-175-001

**Current Status:** 🔴 Not written

**Decision Table — Tất cả invalid transitions phải FAIL với SEC-007:**
| From | To | Expected |
|------|----|----------|
| RESOLVED | OPEN | SEC-007 |
| RESOLVED | UNDER_REVIEW | SEC-007 |
| RESOLVED | FALSE_POSITIVE | SEC-007 |
| FALSE_POSITIVE | OPEN | SEC-007 |
| FALSE_POSITIVE | UNDER_REVIEW | SEC-007 |
| FALSE_POSITIVE | RESOLVED | SEC-007 |
| OPEN | RESOLVED | SEC-007 (must go through UNDER_REVIEW first) |
| OPEN | FALSE_POSITIVE | SEC-007 |

---

### SEC175-TC-005 — Non-Admin Bị Từ chối Truy cập — Tất cả 3 Endpoints (403)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `SecurityEventReviewController` — `@PreAuthorize` trên tất cả 3 endpoints
**Test File:** `src/test/java/com/carebridge/backend/audit/controller/SecurityEventReviewControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-REV-001`

**Preconditions:**
- User với ROLE_USER (không có ROLE_ADMIN)
- JWT hợp lệ nhưng role không đủ

**Test Steps (3 sub-scenarios):**

_Sub-scenario A: GET detail_
1. Arrange: MockMvc với JWT{role: "ROLE_USER"}
2. Act: `GET /api/v1/admin/security-events/e-uuid-01`
3. Assert: HTTP 403, body chứa `"code": "SEC-004"`

_Sub-scenario B: POST note_
1. Arrange: JWT{role: "ROLE_USER"}
2. Act: `POST /api/v1/admin/security-events/e-uuid-01/notes` với valid body
3. Assert: HTTP 403, body chứa `"code": "SEC-004"`

_Sub-scenario C: PATCH status_
1. Arrange: JWT{role: "ROLE_USER"}
2. Act: `PATCH /api/v1/admin/security-events/e-uuid-01/status` với valid body
3. Assert: HTTP 403, body chứa `"code": "SEC-004"`

**Expected Result (PASS — tất cả 3 sub-scenarios):**
- HTTP 403 cho cả 3 endpoints
- Service layer KHÔNG được gọi trong cả 3 cases
- Không có side effect (không có note tạo, không có status change, không có audit)

**Expected Result (FAIL):**
- Bất kỳ endpoint nào trả về 200 cho non-admin → Broken Access Control

**Current Status:** 🔴 Not written

---

### SEC175-TC-006 — Review Note Immutability: @PreRemove Hook

**Severity:** `CRITICAL`
**OWASP:** `A04:2021 — Insecure Design`
**CWE:** `CWE-284 — Improper Access Control`
**Feature Under Test:** `SecurityEventNote.rejectMutation()` — JPA `@PreRemove` hook
**Test File:** `src/test/java/com/carebridge/backend/audit/entity/SecurityEventNoteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-REV-003`, `ADR-175-002`, AuditLog pattern (`AuditLog.rejectMutation()`)

**Preconditions:**
- `SecurityEventNote` entity có `@PreRemove` và `@PreUpdate` lifecycle hooks
- Note đã được persist vào test DB (hoặc dùng mock entity manager)

**Test Steps:**
1. Arrange: Tạo và persist `SecurityEventNote` synthetic
2. Act: Gọi `entityManager.remove(note)` hoặc `noteRepository.deleteById(noteId)`
3. Assert: Exception được throw

**Expected Result (PASS = note không thể xóa):**
- `UnsupportedOperationException` được throw với message chứa "append-only" hoặc tương tự
- Note vẫn tồn tại trong DB sau attempt xóa
- Không có side effect

**Expected Result (FAIL = lỗ hổng):**
- Note bị xóa thành công → Vi phạm BR-REV-003, GDPR Art. 5.1(e)

**Current Status:** 🔴 Not written
**Implementation Note:** Pattern phải giống `AuditLog.rejectMutation()` trong `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/AuditLog.java:65`

---

### SEC175-TC-007 — FCM Notification Gửi Sau Khi RESOLVED (Integration)

**Severity:** `HIGH`
**Feature Under Test:** `SecurityEventReviewServiceImpl.changeStatus()` → `@TransactionalEventListener(AFTER_COMMIT)` → `FcmNotificationService`
**Test File:** `src/test/java/com/carebridge/backend/audit/controller/SecurityEventReviewControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-175-002`, `BR-REV-004`

**Preconditions:**
- PostgreSQL Testcontainers running, migration V2 applied
- `FcmNotificationService` mock (spy để verify call)
- Event FX-175-002 seeded (status=UNDER_REVIEW)
- Admin JWT hợp lệ

**Test Steps:**
1. Seed event với status=UNDER_REVIEW
2. `PATCH /api/v1/admin/security-events/e-uuid-02/status` với body `{newStatus:"RESOLVED"}`
3. Verify DB state
4. Verify FCM call (sau transaction commit)

**Expected Result (PASS):**
- HTTP 200
- DB: `security_events.status = 'RESOLVED'`
- DB: `security_events.reviewed_by` = adminId, `reviewed_at` không null
- DB: `audit_logs` có entry với `action='SECURITY_EVENT'`, `entity_id='e-uuid-02'`
- `FcmNotificationService.notifyAdminsSecurityEventResolved()` được gọi 1 lần với eventId="e-uuid-02"
- FCM call xảy ra AFTER DB commit (verify order via spy)

**Expected Result (FAIL):**
- FCM gọi trước khi DB commit → ADR-175-002 violation
- FCM failure gây rollback DB → ADR-175-002 violation (FCM failure phải non-blocking)

**DB Assertion:**
```sql
SELECT status, reviewed_by, reviewed_at
FROM security_events
WHERE id = 'e-uuid-02';
-- Expected: status='RESOLVED', reviewed_by IS NOT NULL, reviewed_at IS NOT NULL

SELECT COUNT(*) FROM audit_logs
WHERE entity_id = 'e-uuid-02' AND action = 'SECURITY_EVENT';
-- Expected: 1
```

**Current Status:** 🔴 Not written

---

### SEC175-TC-008 — Payload Sanitization: Sensitive Keys Bị Loại bỏ

**Severity:** `CRITICAL`
**OWASP:** `A02:2021 — Cryptographic Failures`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Feature Under Test:** `SecurityEventDetailMapper.sanitizePayload()`
**Test File:** `src/test/java/com/carebridge/backend/audit/mapper/SecurityEventDetailMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-REV-006`, `BR-REV-007`, `CB-SEC-IMP-002 §17 C6`

**Preconditions:**
- Raw payload JSONB (synthetic):
  ```json
  {
    "requestPath": "/api/v1/health",
    "attempted_password_hash": "bcrypt$2a$...",
    "access_token": "eyJhbGci...",
    "session_secret": "s3cr3t-k3y",
    "api_key": "sk-live-...",
    "credential": "base64encoded...",
    "requestMethod": "GET",
    "responseStatus": 403
  }
  ```

**Test Steps:**
1. Arrange: SecurityEvent entity với payload = raw payload trên
2. Act: Gọi `mapper.sanitizePayload(event.getPayload())`
3. Assert: Inspect keys trong Map kết quả

**Expected Result (PASS):**
- Keys bị loại bỏ (KHÔNG có trong result): `attempted_password_hash`, `access_token`, `session_secret`, `api_key`, `credential`
- Keys được giữ lại: `requestPath`, `requestMethod`, `responseStatus`
- Map không null, không empty (vẫn có safe fields)

**Test Matrix — Phải verify từng key bị strip:**

| Sensitive Key | Present in Input | Present in Output |
|---------------|-----------------|-------------------|
| `attempted_password_hash` | Yes | No (PASS) |
| `access_token` | Yes | No (PASS) |
| `session_secret` | Yes | No (PASS) |
| `api_key` | Yes | No (PASS) |
| `credential` | Yes | No (PASS) |
| `requestPath` | Yes | Yes (safe — kept) |
| `requestMethod` | Yes | Yes (safe — kept) |
| `responseStatus` | Yes | Yes (safe — kept) |

**Expected Result (FAIL):**
- Bất kỳ sensitive key nào vẫn còn trong output → Potential data exposure

**Current Status:** 🔴 Not written
**Implementation Note:** `sanitizePayload()` nên dùng set-based exclusion: `SENSITIVE_KEYS = Set.of("password", "token", "hash", "secret", "key", "credential")` và filter based on `key.toLowerCase()` contains bất kỳ member nào.

---

### SEC175-TC-009 — Integration: Thêm Note và Verify DB Persistence

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → NoteRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/audit/controller/SecurityEventReviewControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainers running, migration V2 applied
- Event FX-175-001 seeded (status=OPEN, 0 notes)
- Admin JWT hợp lệ

**Test Steps:**
1. Seed database với event có id="e-uuid-01", 0 notes
2. `POST /api/v1/admin/security-events/e-uuid-01/notes` với body `{noteText:"Integration test note"}`
3. `GET /api/v1/admin/security-events/e-uuid-01` để verify note xuất hiện
4. Assert DB state

**Expected Result (PASS):**
- POST trả về HTTP 201
- GET trả về HTTP 200 với `data.notes.length = 1`
- DB: `SELECT COUNT(*) FROM security_event_notes WHERE event_id = 'e-uuid-01'` = 1
- DB: note `author_id = adminId`, `note_text = "Integration test note"`
- DB: note không thể bị UPDATE sau khi insert (verify via `n_tup_upd` stat)
- audit_logs có entry cho ADD_REVIEW_NOTE action

**DB Assertion:**
```sql
SELECT note_id, author_id, note_text, created_at
FROM security_event_notes
WHERE event_id = 'e-uuid-01';
-- Expected: 1 row với note_text = 'Integration test note'

-- Verify immutability stat
SELECT n_tup_upd, n_tup_del
FROM pg_stat_user_tables
WHERE tablename = 'security_event_notes';
-- Expected: n_tup_upd = 0, n_tup_del = 0
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SEC175-TC-001` | `SecurityEventReviewServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-002` | `SecurityEventReviewServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-003` | `SecurityEventReviewServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-004` | `SecurityEventReviewServiceImplTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-005` | `SecurityEventReviewControllerTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-006` | `SecurityEventNoteTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-007` | `SecurityEventReviewControllerIntegrationTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-008` | `SecurityEventDetailMapperTest.java` | `[ ]` | `—` | — |
| `SEC175-TC-009` | `SecurityEventReviewControllerIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với stub sau. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// SecurityEventReviewServiceImpl.java — Red Phase stub
@Service
public class SecurityEventReviewServiceImpl implements ISecurityEventReviewService {

    @Override
    public SecurityEventDetailResponse getEventDetail(UUID eventId, UUID adminId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SecurityEventNoteResponse addReviewNote(
            UUID eventId, AddReviewNoteRequest request, UUID adminId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SecurityEventDetailResponse changeStatus(
            UUID eventId, ChangeStatusRequest request, UUID adminId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// SecurityEventNote.java — Stub @PreRemove (KHÔNG có = test TC-006 phải FAIL)
// Để không có @PreRemove → note có thể bị xóa → TC-006 FAIL (expected)
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Root Cause nếu PASS bất thường |
|-------|-------------|----------|--------------------------------|
| `SEC175-TC-001` | throws UnsupportedOperationException | 🔴 FAIL | ☐ Shared state ☐ Tautology |
| `SEC175-TC-002` | throws UnsupportedOperationException | 🔴 FAIL | — |
| `SEC175-TC-003` | throws UnsupportedOperationException | 🔴 FAIL | — |
| `SEC175-TC-004` | throws UnsupportedOperationException (không phải SEC-007) | 🔴 FAIL | ☐ Wrong exception type expected |
| `SEC175-TC-005` | 403 từ Spring Security nếu @PreAuthorize config | 🔴 FAIL (service chưa có) | ☐ @PreAuthorize missing |
| `SEC175-TC-006` | Note bị xóa thành công (chưa có @PreRemove) | 🔴 FAIL | — |
| `SEC175-TC-007` | HTTP 500 hoặc no FCM call | 🔴 FAIL | — |
| `SEC175-TC-008` | sanitizePayload() chưa tồn tại | 🔴 FAIL (compile error) | ☐ Hallucinated Contract |
| `SEC175-TC-009` | HTTP 500 | 🔴 FAIL | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SEC-IMP-002` đã được approve
- [ ] CB-SEC-IMP-001 (UC-174) đã được implement (UC-175 depends on SecurityEvent entity)
- [ ] Migration V2 (`V2__security_events_enhanced.sql`) đã apply thành công
- [ ] Firebase FCM credentials đã cấu hình trong application-test.yml (mock/emulator)
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test` — tất cả 9 test cases xanh
- [ ] Integration tests xanh với Testcontainers
- [ ] Test coverage ≥ 80% lines cho: `SecurityEventReviewServiceImpl`, `SecurityEventReviewController`, `SecurityEventDetailMapper`
- [ ] TC-004 confirm `ValidationException` với `SEC-007` cho tất cả invalid transitions trong Decision Table
- [ ] TC-005 confirm 403 cho tất cả 3 endpoints với non-admin
- [ ] TC-006 confirm `UnsupportedOperationException` khi attempt delete note
- [ ] TC-007 confirm FCM được gọi AFTER DB commit (không trước)
- [ ] TC-008 confirm tất cả sensitive keys bị strip từ payload response
- [ ] Không có sensitive field (`password`, `token`, `hash`, `secret`, `key`, `credential`) xuất hiện trong bất kỳ response

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 9 tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — compile thành công:
  ```bash
  ./mvnw compile -pl 05_Development/CareBridgeAPI 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **State Machine Complete** — Decision Table từ TC-004 được test đủ 8 invalid transitions
- [ ] **Oracle Source** — mọi expected value có nguồn BR/ADR rõ ràng trong test comment

### Suspension Criteria

- `SecurityEvent` entity chưa được extend với fields mới (migration V2 block)
- Firebase FCM credentials chưa cấu hình cho test environment
- UC-174 chưa implement (dependency)
- Phát hiện lỗi kiến trúc mới ở state machine cần Principal Architect review

---

## 7. Rollback Plan

```bash
# Revert implementation files (không revert migration V2 — shared với UC-174)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityEventReviewServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityEventReviewController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/entity/SecurityEventNote.java

# Verify security_event_notes integrity sau rollback
psql -h [host] -U [user] -d carebridge -c "
SELECT COUNT(*) FROM security_event_notes;
SELECT n_tup_upd, n_tup_del FROM pg_stat_user_tables WHERE tablename='security_event_notes';
"

# UC-175 vẫn OPEN → giữ Status = Draft trong header document
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong Test Spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | TC-006 PASS với note có thể xóa | ☐ | G-2 ★ |
| AP-AI-003 | Terminal State Bypass | TC-004 không test tất cả invalid transitions | ☐ | G-1 |
| AP-AI-004 | Sync FCM | TC-007 không verify AFTER_COMMIT ordering | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `sanitizePayload()` nhưng method chưa có | ☐ | G-3 |
| AP-AI-006 | Partial Sanitization | TC-008 chỉ test 1-2 sensitive keys thay vì tất cả | ☐ | G-4 |
| AP-AI-007 | AdminId Spoofing | Test không verify adminId lấy từ JWT (accept từ body) | ☐ | G-1 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

## PHỤ LỤC: Transition Test Matrix Đầy đủ

> Tất cả transitions phải được cover trong TC-004 (hoặc extend thành TC-004a, TC-004b, ...).

| From \ To | OPEN | UNDER_REVIEW | RESOLVED | FALSE_POSITIVE |
|-----------|------|--------------|----------|----------------|
| **OPEN** | — (same) | ✅ ALLOWED | ❌ SEC-007 | ❌ SEC-007 |
| **UNDER_REVIEW** | ✅ ALLOWED (re-open) | — (same) | ✅ ALLOWED | ✅ ALLOWED |
| **RESOLVED** | ❌ SEC-007 | ❌ SEC-007 | — (same) | ❌ SEC-007 |
| **FALSE_POSITIVE** | ❌ SEC-007 | ❌ SEC-007 | ❌ SEC-007 | — (same) |

> ✅ = Transition hợp lệ, thành công
> ❌ = Transition bị từ chối với SEC-007

---

*Test-Spec v1.0 — UC-175 Review Security Event*
*Tích hợp CASE 2.0 Red Gate Protocol, Anti-Pattern Detection và State Machine Transition Matrix.*
*Sections đánh dấu ⭐ là bổ sung từ CASE 2.0 methodology.*
