# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test Design & Test Case Specification — UC-195 Delete Baby Daily Log

**Document ID:** `CB-BABY-TDD-004`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260707111000__add_baby_daily_log_status.sql` — companion migration created by this TDS (see TDS §5.2)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.12.4 (UC-195, Table 217)
- `04_Implement/UC195_DeleteBabyDailyLog/UC195_DeleteBabyDailyLog_TDS.md` — Technical Design Specification (this feature)
- `04_Implement/UC194_ViewBabyDailyLogDetail/UC194_ViewBabyDailyLogDetail_TDS.md` — companion TDS whose classes are EXTENDED here (no Test-Spec exists yet for UC194 at time of writing — see §3 Props Isolation note)
- `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` — `BabyAccessPolicy` origin (Approved, shipped code)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo tài liệu — TDD spec cho UC195 Delete Baby Daily Log, scoped ONLY tới method `deleteBabyDailyLog()` và `BabyAccessPolicy.canManage()` (methods MỚI) — KHÔNG cover lại UC194's `getDailyLogDetail()`/`canView()` |

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
9. [Mobile Widget Tests (Flutter)](#9-mobile-widget-tests-flutter)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-195` |
| **Module** | `DeleteBabyDailyLog` — Bounded Context `baby` |
| **Spec gốc** | `CB-BABY-IMP-004` (`04_Implement/UC195_DeleteBabyDailyLog/UC195_DeleteBabyDailyLog_TDS.md`) |
| **Priority** | 🟠 P1 (destructive action on Sensitive-PII infant health data) |
| **Sprint** | `Sprint 4 (Device Sync And Care Edge Cases)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SAFETY` |
| **Upstream Dependencies** | `UC194 (BabyDailyLog entity, BabyDailyLogRepository, BabyDailyLogServiceImpl, BabyDailyLogController)`, `UC192 (BabyProfileRepository, BabyAccessPolicy)`, `audit (AuditService, AuditAction)` |
| **Downstream Consumers** | `UC194 GET endpoint (must 404 on DELETED — regression-tested here)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC195_DeleteBabyDailyLog_TDS.md §17` (C1-C6), `ADR-BABY-006/007/008` |
| **Constraints Injected** | Soft-delete only (C4), owner-only via `canManage()` (C2), idempotent 404 on double-delete (C6), mandatory synchronous audit (C5), no parallel class creation (C1) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.12.4 mô tả generic flow ("actor confirms action... system applies business rules") không nói rõ soft vs hard delete | TDS ADR-BABY-006 xác định rõ: soft-delete qua `status` column, KHÔNG hard DELETE | Mọi test case assert `status == DELETED` sau khi gọi, KHÔNG assert `repository.findById()` trả `Optional.empty()` |
| L2 | UC194's TDS pre-designed `BabyDailyLog.status` field nhưng field này `nullable` (chưa có DB column) tại thời điểm UC194 viết | Migration `V20260707111000` (tạo bởi UC195) thêm cột `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` — sau migration, field KHÔNG còn null cho record cũ | Integration test `DAILYLOG-TC-INT-002` verify record insert TRƯỚC migration (nếu seed qua raw SQL không set status) vẫn nhận default `'ACTIVE'` |
| L3 | `BabyAccessPolicy.canView()` (UC192/UC194) cho phép ACCEPTED care member — nếu áp dụng nhầm cho delete sẽ là lỗ hổng IDOR/broken-access-control | TDS ADR-BABY-007 tách riêng `canManage()` owner-only | `DAILYLOG-TC-002` là test CRITICAL riêng biệt, PHẢI FAIL nếu implementation dùng nhầm `canView()` cho path xoá |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
DeleteBabyDailyLog bao gồm các layer, SCOPED ONLY tới method/logic MỚI của UC195:
├── Domain — BabyAccessPolicy.canManage() (pure logic, no deps ngoài BabyProfile)
├── Services — BabyDailyLogServiceImpl.deleteBabyDailyLog() (mock BabyDailyLogRepository,
│              BabyProfileRepository, BabyAccessPolicy, AuditService với Mockito)
├── Controller — BabyDailyLogController.deleteBabyDailyLog() (mock IBabyDailyLogService với @WebMvcTest)
└── Integration — Testcontainers PostgreSQL (@SpringBootTest), full DELETE + GET regression flow

KHÔNG re-test: getDailyLogDetail() / canView() (đã spec bởi UC194 — coverage đó thuộc về
UC194's OWN Test-Spec khi được tạo; test file này chỉ IMPORT UC194's factory pattern nếu tồn tại).
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-195` (§3.3.12.4, Table 217) | Soft-delete hành vi người dùng, Preconditions PRE-1..4, Exceptions E1-E3 |
| `ADR-BABY-006` | Soft-delete-only invariant (C4), idempotent double-delete → 404 (C6) |
| `ADR-BABY-007` | Owner-only `canManage()` — care member EXCLUDED (C2) |
| `ADR-BABY-008` | Mandatory synchronous audit event on delete (C5) |
| `BR-RBAC / BR-PRIVACY / BR-SAFETY` | Toàn bộ security test cases (IDOR, no-hard-delete guard, audit completeness) |
| `CB-BABY-IMP-004 §10` | Error code assertions: `DAILYLOG-001`, `DAILYLOG-003` |
| `CB-BABY-IMP-004 §16` | Authorization Matrix — owner ✅, care member ❌, expert ❌, admin ❌ |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner gọi delete trên log ACTIVE của baby mình sở hữu | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-001` |
| TC-COND-002 | ACCEPTED care member (không phải owner) gọi delete | `BabyAccessPolicy.canManage()` | `DAILYLOG-TC-002`, `DAILYLOG-TC-007` |
| TC-COND-003 | Người dùng hoàn toàn không liên quan gọi delete | `BabyAccessPolicy.canManage()` | `DAILYLOG-TC-003` |
| TC-COND-004 | `babyLogId` không tồn tại | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-004` |
| TC-COND-005 | Log đã `status=DELETED`, gọi xoá lần 2 | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-005` |
| TC-COND-006 | `babyId` trong URL path không khớp `dailyLog.getBabyId()` thực tế (path tampering) | `BabyDailyLogController.deleteBabyDailyLog()` | `DAILYLOG-TC-006` |
| TC-COND-007 | Verify `canManage()` owner=true / owner=false ở mức unit thuần | `BabyAccessPolicy.canManage()` | `DAILYLOG-TC-007`, `DAILYLOG-TC-008` |
| TC-COND-008 | Verify KHÔNG bao giờ gọi hard-delete API của repository | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-009` |
| TC-COND-009 | Verify audit payload đúng field, không leak `note` (nội dung log) | `AuditService.log()` call site | `DAILYLOG-TC-010` |
| TC-COND-010 | IDOR attack simulation qua nhiều `babyId`/`logId` kết hợp | API layer | `DAILYLOG-TC-SEC-001` |
| TC-COND-011 | Không có JWT / JWT hết hạn | API layer (Spring Security filter) | `DAILYLOG-TC-SEC-002`, `DAILYLOG-TC-SEC-003` |
| TC-COND-012 | Full E2E: DELETE thành công → GET (UC194) trả 404 | Integration | `DAILYLOG-TC-INT-001` |
| TC-COND-013 | Migration backfill default `status='ACTIVE'` cho record cũ | Integration/Flyway | `DAILYLOG-TC-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Caller identity: owner / ACCEPTED member / unrelated user / no-auth | 4 lớp tương đương quyền truy cập rõ rệt theo Auth Matrix §16 TDS |
| Boundary Value Analysis | `status` transition ACTIVE→DELETED, DELETED→(no-op, 404) | Boundary duy nhất của state machine 2 trạng thái |
| State Transition Testing | `BabyDailyLogStatus` enum (ACTIVE ↔ DELETED) | Đảm bảo transition 1 chiều hợp lệ, không "un-delete" qua endpoint này |
| Error Guessing | Path `babyId` tampering, double-delete race, hard-delete regression | Các attack vector cụ thể từ ADR-BABY-006/007 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `BabyProfile{id=BABY-001, ownerUserId=MOTHER-001, status=ACTIVE}` | Baseline owner context |
| `FX-002` | DB seed | `BabyDailyLog{id=LOG-001, babyId=BABY-001, status=ACTIVE, recordedBy=MOTHER-001}` | Happy path delete target |
| `FX-003` | DB seed | `BabyDailyLog{id=LOG-002, babyId=BABY-001, status=DELETED}` | Double-delete idempotency test |
| `FX-004` | DB seed | `CareGroupMember{careGroupId=..., userId=MOTHER-002, inviteStatus=ACCEPTED}` linked to `BABY-001`'s care group | Non-owner-but-accepted-member IDOR case |
| `FX-005` | JWT | `{sub: 'MOTHER-001', role: 'MOTHER'}` | Owner auth context |
| `FX-006` | JWT | `{sub: 'MOTHER-002', role: 'MOTHER'}` | Care member auth context |
| `FX-007` | JWT | `{sub: 'MOTHER-003', role: 'MOTHER'}` | Unrelated user auth context |

---

## 4. Test Case Specification

> **TC ID format:** `DAILYLOG-TC-[NNN]` (backend) / `DAILYLOG-TC-SEC-[NNN]` (security) / `DAILYLOG-TC-INT-[NNN]` (integration) / `DAILYLOG-TC-MOB-[NNN]` (mobile, §9)
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ Đây là factory ĐẦU TIÊN cho bounded context `baby.dailylog` test suite — UC194 chưa có Test-Spec/factory tại thời điểm viết tài liệu này. **Khi UC194's Test-Spec được tạo, nó PHẢI reuse factory này** (không tạo `BabyDailyLogTestFactory` thứ 2) để tránh phân mảnh test fixtures.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// File: src/test/java/com/carebridge/backend/baby/BabyDailyLogTestFactory.java
// ═══════════════════════════════════════════════════════════

class BabyDailyLogTestFactory {

    static final UUID OWNER_ID   = UUID.fromString("00000000-0000-0000-0000-0000000000A1"); // MOTHER-001
    static final UUID MEMBER_ID  = UUID.fromString("00000000-0000-0000-0000-0000000000A2"); // MOTHER-002 (ACCEPTED, non-owner)
    static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A3"); // MOTHER-003 (unrelated)
    static final UUID BABY_ID    = UUID.fromString("00000000-0000-0000-0000-0000000000B1"); // BABY-001

    static BabyProfile makeBabyProfile() {
        return makeBabyProfile(p -> {});
    }

    static BabyProfile makeBabyProfile(Consumer<BabyProfile> overrides) {
        BabyProfile profile = BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Bé Bo")
                .status(BabyProfileStatus.ACTIVE)
                .build();
        overrides.accept(profile);
        return profile;
    }

    // Baseline ACTIVE log — synced with FX-002 (§3 TDS-05)
    static BabyDailyLog makeActiveLog() {
        return makeActiveLog(l -> {});
    }

    static BabyDailyLog makeActiveLog(Consumer<BabyDailyLog> overrides) {
        BabyDailyLog log = new BabyDailyLog();
        log.setId(UUID.fromString("00000000-0000-0000-0000-0000000000C1")); // LOG-001
        log.setBabyId(BABY_ID);
        log.setLogType("feeding");
        log.setNote("Bú bình 120ml"); // SYNTHETIC — never real infant data
        log.setRecordedBy(OWNER_ID);
        log.setStatus(BabyDailyLogStatus.ACTIVE);
        overrides.accept(log);
        return log;
    }

    // Already-deleted log — synced with FX-003, for double-delete tests
    static BabyDailyLog makeDeletedLog() {
        return makeActiveLog(l -> {
            l.setId(UUID.fromString("00000000-0000-0000-0000-0000000000C2")); // LOG-002
            l.setStatus(BabyDailyLogStatus.DELETED);
        });
    }
}
```

---

### DAILYLOG-TC-001 — Owner xoá log ACTIVE thành công → soft-delete + audit

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-BABY-006 (soft-delete)`, `ADR-BABY-008 (audit)` — TDS §6.1 sequence diagram

**Preconditions:**
- `FX-001` (BabyProfile, owner=OWNER_ID), `FX-002` (BabyDailyLog LOG-001, status=ACTIVE) mocked qua factory
- `babyDailyLogRepository.findById(LOG-001)` mock trả `Optional.of(makeActiveLog())`
- `babyProfileRepository.findById(BABY_ID)` mock trả `Optional.of(makeBabyProfile())`

**Test Steps:**
1. Arrange: mock `babyDailyLogRepository.save(any())` trả về argument nó nhận (captured qua `ArgumentCaptor<BabyDailyLog>`)
2. Act: gọi `service.deleteBabyDailyLog(LOG-001, OWNER_ID)`
3. Assert: captured entity có `status == BabyDailyLogStatus.DELETED`; `auditService.log(BABY_DAILY_LOG_DELETED, OWNER_ID, "BabyDailyLog", LOG-001.toString(), any())` được gọi đúng 1 lần

**Expected Result (PASS — hành vi đúng):**
- Không throw exception; `babyDailyLogRepository.save()` được gọi đúng 1 lần với `status=DELETED`; audit log gọi đúng 1 lần

**Expected Result (FAIL — dấu hiệu lỗi):**
- `status` vẫn `ACTIVE` sau save → thiếu logic set status; hoặc audit không được gọi → vi phạm ADR-BABY-008

**Current Status:** 🔴 Not written
**Implementation Note:** Method PHẢI: `findById` → check status → `canManage` → `setStatus(DELETED)` → `save()` → `auditService.log()`, TRONG CÙNG `@Transactional`.

---

### DAILYLOG-TC-002 — ACCEPTED care member (KHÔNG phải owner) xoá → 403 DAILYLOG-003 (IDOR guard — critical)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog(UUID, UUID)` + `BabyAccessPolicy.canManage()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-BABY-007` — explicitly excludes care members from delete, unlike UC194's `canView()`

**Preconditions:**
- `FX-002` (LOG-001, status=ACTIVE), `FX-001` (BabyProfile owner=OWNER_ID)
- `FX-004`: `MEMBER_ID` là ACCEPTED member trong care group của `BABY_ID` (would pass `canView()` if wrongly reused)

**Test Steps:**
1. Arrange: mock repos as in TC-001
2. Act: gọi `service.deleteBabyDailyLog(LOG-001, MEMBER_ID)`
3. Assert: throws `BusinessException` với `getHttpStatus() == FORBIDDEN`, `getCode() == "DAILYLOG-003"`
4. Assert: `babyDailyLogRepository.save()` KHÔNG được gọi (no side effect); `auditService.log()` KHÔNG được gọi

**Expected Result (PASS — hệ thống an toàn):**
- 403 `DAILYLOG-003` — chứng minh `canManage()` được dùng, KHÔNG phải `canView()`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Nếu implementation nhầm dùng `canView()`, test này sẽ PASS xoá thành công (200) thay vì 403 → **AP-AI-003 detected** theo ADR-BABY-007

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test case quan trọng nhất của toàn bộ UC195 — bảo vệ trực tiếp ADR-BABY-007's decision.

---

### DAILYLOG-TC-003 — Người dùng không liên quan xoá → 403 DAILYLOG-003

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** `FX-001`, `FX-002`; `STRANGER_ID` không có bất kỳ quan hệ nào với `BABY_ID`

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-001, STRANGER_ID)`
2. Assert: throws `BusinessException(403, "DAILYLOG-003")`

**Expected Result (PASS):** 403 DAILYLOG-003
**Expected Result (FAIL):** 200 hoặc exception khác code

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-004 — Log không tồn tại → 404 DAILYLOG-001

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-BABY-IMP-004 §10 Error Codes`

**Preconditions:** `babyDailyLogRepository.findById(NONEXISTENT)` mock trả `Optional.empty()`

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(NONEXISTENT, OWNER_ID)`
2. Assert: throws `BusinessException(404, "DAILYLOG-001")`
3. Assert: `babyProfileRepository.findById()` KHÔNG được gọi (short-circuit trước khi resolve ownership — hiệu năng)

**Expected Result (PASS):** 404 DAILYLOG-001
**Expected Result (FAIL):** NullPointerException hoặc 500

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-005 — Double-delete (log đã DELETED) → 404 idempotent-safe, KHÔNG audit trùng

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-BABY-006` §Consequences — idempotent-safe repeat calls

**Preconditions:** `FX-003` (LOG-002, status=DELETED) mocked

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-002, OWNER_ID)`
2. Assert: throws `BusinessException(404, "DAILYLOG-001")` — KHÔNG mã lỗi riêng "already deleted"
3. Assert: `babyDailyLogRepository.save()` KHÔNG được gọi; `auditService.log()` KHÔNG được gọi

**Expected Result (PASS):** 404, no duplicate audit entry, no 500
**Expected Result (FAIL):** 500 (NPE trên status transition) hoặc 200 (silently re-deletes, tạo audit trùng)

**Current Status:** 🔴 Not written
**Implementation Note:** Check `if (log.getStatus() == DELETED) throw 404` PHẢI xảy ra TRƯỚC khi load `BabyProfile`/gọi `canManage()`.

---

### DAILYLOG-TC-006 — `babyId` path param tampering không ảnh hưởng authorization (defense-in-depth)

**Severity:** `HIGH`
**CWE:** `CWE-639`
**Feature Under Test:** `BabyDailyLogController.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/BabyDailyLogControllerDeleteTest.java` (`@WebMvcTest`)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-BABY-004 (UC194) Constraint C3 — reused C3 in UC195 TDS §17.1`

**Preconditions:** Mock `IBabyDailyLogService.deleteBabyDailyLog(logId, callerId)` — verify method KHÔNG nhận `babyId` từ path như 1 tham số dùng cho authorization (chỉ dùng routing)

**Test Steps:**
1. Act: `DELETE /api/v1/babies/{RANDOM_UNRELATED_BABY_ID}/daily-logs/{LOG-001}` với JWT của `OWNER_ID` (owner thật của `BABY_ID`, không phải `RANDOM_UNRELATED_BABY_ID`)
2. Assert: Controller vẫn gọi `service.deleteBabyDailyLog(LOG-001, OWNER_ID)` — KHÔNG dùng `RANDOM_UNRELATED_BABY_ID` để authorize; kết quả phụ thuộc hoàn toàn vào service logic (mà service đã tự resolve `babyId` thật từ `dailyLog.getBabyId()`)

**Expected Result (PASS):** Controller không có logic authorization riêng dựa trên path `babyId` — chỉ pass-through
**Expected Result (FAIL):** Controller tự chặn/tự cho phép dựa trên path `babyId` mà không qua service — vi phạm layering rule (`CLAUDE.md`: "Controller: validation, request/response mapping only; no business logic")

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-007 — `BabyAccessPolicy.canManage()` trả `false` cho ACCEPTED care member (unit thuần)

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canManage(BabyProfile, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/baby/policy/BabyAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** `profile.ownerUserId = OWNER_ID`; caller = `MEMBER_ID` (khác `OWNER_ID`)

**Test Steps:**
1. Act: `policy.canManage(makeBabyProfile(), MEMBER_ID)`
2. Assert: kết quả `== false` — **KHÔNG** query `CareGroupMemberRepository` (khác `canView()`, `canManage()` không cần check care group membership)

**Expected Result (PASS):** `false`, không có Mockito interaction với `memberRepository`
**Expected Result (FAIL):** `true` (bug — đang tái sử dụng logic của `canView()`)

**Current Status:** 🔴 Not written
**Implementation Note:** `canManage()` = `profile.getOwnerUserId().equals(callerId)` — 1 dòng, KHÔNG gọi `memberRepository`.

---

### DAILYLOG-TC-008 — `BabyAccessPolicy.canManage()` trả `true` cho owner

**Severity:** `HIGH`
**Feature Under Test:** `BabyAccessPolicy.canManage(BabyProfile, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/baby/policy/BabyAccessPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-BABY-007`

**Test Steps:**
1. Act: `policy.canManage(makeBabyProfile(), OWNER_ID)`
2. Assert: kết quả `== true`

**Expected Result (PASS):** `true`
**Expected Result (FAIL):** `false`

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-009 — Verify KHÔNG bao giờ gọi hard-delete (`delete()`/`deleteById()`)

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-BABY-006` — soft-delete-only invariant

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-001, OWNER_ID)` (happy path)
2. Assert: `Mockito.verify(babyDailyLogRepository, never()).delete(any())`
3. Assert: `Mockito.verify(babyDailyLogRepository, never()).deleteById(any())`
4. Assert: `Mockito.verify(babyDailyLogRepository, times(1)).save(any())`

**Expected Result (PASS):** hard-delete methods never invoked
**Expected Result (FAIL):** `delete()`/`deleteById()` called — CRITICAL violation of ADR-BABY-006 & BR-PRIVACY (unrecoverable data loss on Sensitive-PII)

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-010 — Audit payload đúng field, không leak `note` (nội dung log)

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()` → `AuditService.log()` call
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-BABY-008`, BR-PRIVACY minimum-necessary

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-001, OWNER_ID)`
2. Assert (via `ArgumentCaptor` trên `auditService.log(action, userId, resourceType, resourceId, details)`): `action == BABY_DAILY_LOG_DELETED`; `userId == OWNER_ID`; `resourceType == "BabyDailyLog"`; `resourceId == LOG-001.toString()`
3. Assert: `details` object KHÔNG chứa field `note`/nội dung nhật ký gốc (chỉ metadata: babyLogId, babyId, deletedByUserId — theo Payload schema TDS §7.3)

**Expected Result (PASS):** audit payload minimum-necessary, không có PII content
**Expected Result (FAIL):** `details` chứa `log.getNote()` plaintext — vi phạm BR-PRIVACY

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### DAILYLOG-TC-SEC-001 — IDOR attack simulation: enumerate `logId` across nhiều `babyId` không sở hữu

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639`
**Legal:** `BR-RBAC, PDPA minimum-necessary access`
**Feature Under Test:** `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` (full stack)
**Test File:** `src/test/java/com/carebridge/backend/baby/DeleteBabyDailyLogSecurityTest.java` (`@SpringBootTest`, Testcontainers)
**TDD Phase:** 🔴 RED

**Preconditions:** Seed 3 babies thuộc 3 owner khác nhau, mỗi baby có 1 log ACTIVE; attacker = `STRANGER_ID` với JWT hợp lệ nhưng không sở hữu baby nào

**Test Steps (Attack Simulation):**
1. Attacker lặp `DELETE /api/v1/babies/{babyId_N}/daily-logs/{logId_N}` cho cả 3 cặp `(babyId, logId)` với JWT của mình
2. Kiểm tra response và DB state sau mỗi lần gọi

**Expected Result (PASS = hệ thống an toàn):**
- Cả 3 lần đều trả `403 Forbidden` với `code: DAILYLOG-003`
- DB: cả 3 log vẫn `status=ACTIVE` sau attack (no side effect)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Bất kỳ log nào bị `status=DELETED` bởi attacker

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-SEC-002 — Không có JWT → 401

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/BabyDailyLogControllerDeleteTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. `DELETE` không có header `Authorization`
2. Assert: `401 Unauthorized`

**Expected Result (PASS):** 401
**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-SEC-003 — JWT hết hạn/invalid → 401

**Severity:** `MEDIUM`
**OWASP:** `A07:2021`
**Feature Under Test:** `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/BabyDailyLogControllerDeleteTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. `DELETE` với `Authorization: Bearer <expired-or-malformed-token>`
2. Assert: `401 Unauthorized`

**Expected Result (PASS):** 401
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### DAILYLOG-TC-INT-001 — Full flow: DELETE thành công → UC194's GET trả 404 (cross-UC regression)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: seed log ACTIVE → DELETE (UC195) → GET (UC194) → DB assert`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start (bao gồm `V20260707111000`)
- Seed: `BabyProfile` (owner=OWNER_ID) + `BabyDailyLog` (status=ACTIVE) qua JPA repository trực tiếp

**Test Steps:**
1. `POST`/seed baby + log trực tiếp qua repository (không qua API, vì UC195 không có create endpoint)
2. `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` với JWT owner → assert `200`
3. DB assert: `SELECT status FROM baby_daily_logs WHERE baby_log_id=?` == `'DELETED'`
4. DB assert: `SELECT COUNT(*) FROM audit_logs WHERE action='BABY_DAILY_LOG_DELETED' AND resource_id=?` == `1`
5. `GET /api/v1/babies/{babyId}/daily-logs/{logId}` (UC194's endpoint) với cùng JWT → assert `404` với `code: DAILYLOG-001`

**Expected Result (PASS):**
- Bước 2: 200; Bước 3-4: DB state đúng; Bước 5: 404 (chứng minh UC194's read path tôn trọng `status` filter đã activate bởi migration này)

**Expected Result (FAIL):**
- Bước 5 vẫn trả 200 (UC194's `getDailyLogDetail` chưa filter `status <> DELETED`) → cần fix UC194's service code khi implement song song, ghi vào Logic Issues nếu phát sinh

**DB Assertion:**
```java
BabyDailyLog record = babyDailyLogRepository.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(BabyDailyLogStatus.DELETED);

long auditCount = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM audit_logs WHERE action = ? AND resource_id = ?",
    Long.class, "BABY_DAILY_LOG_DELETED", savedId.toString());
assertThat(auditCount).isEqualTo(1L);
```

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-INT-002 — Migration backfill: record cũ (pre-migration) nhận `status='ACTIVE'` mặc định

**Severity:** `MEDIUM`
**Feature Under Test:** `V20260707111000__add_baby_daily_log_status.sql`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogMigrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:** Testcontainers PostgreSQL, insert 1 row qua raw SQL (`INSERT INTO baby_daily_logs (baby_id, log_type, ...) VALUES (...)` — KHÔNG set `status`) TRƯỚC KHI Flyway apply `V20260707111000` (hoặc: insert sau migration nhưng qua raw SQL không set status, verify `DEFAULT 'ACTIVE'` áp dụng)

**Test Steps:**
1. Insert raw row không có `status`
2. Query lại: `SELECT status FROM baby_daily_logs WHERE baby_log_id=?`
3. Assert: `status == 'ACTIVE'`

**Expected Result (PASS):** `DEFAULT 'ACTIVE'` hoạt động đúng — record cũ KHÔNG bị coi là DELETED
**Expected Result (FAIL):** `status` là `NULL` (migration thiếu `NOT NULL DEFAULT`) → UC194's GET path có thể lỗi NPE khi map DTO

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `DAILYLOG-TC-001` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-002` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-003` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-004` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-005` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-006` | `BabyDailyLogControllerDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-007` | `BabyAccessPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-008` | `BabyAccessPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-009` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-010` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-SEC-001` | `DeleteBabyDailyLogSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-SEC-002` | `BabyDailyLogControllerDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-SEC-003` | `BabyDailyLogControllerDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-INT-001` | `BabyDailyLogDeleteIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-INT-002` | `BabyDailyLogMigrationTest.java:TBD` | `[ ]` | `[ ]` | |

**Total test cases:** 15 (10 unit/component + 3 security + 2 integration), plus 4 mobile widget tests (§9) = **19 total**.

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Scoped ONLY tới method MỚI của UC195 (`deleteBabyDailyLog`, `canManage`) — KHÔNG áp dụng lại lên UC194's `getDailyLogDetail`/`canView` (đã ngoài phạm vi tài liệu này).

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw). Applies ONLY to the two NEW methods.

// IBabyDailyLogService.java — deleteBabyDailyLog() stub added alongside UC194's EXISTING
// getDailyLogDetail() (which is assumed already implemented if UC194 ships first; if not,
// getDailyLogDetail() keeps its own UC194 Red Gate, untouched here).
@Service
public class BabyDailyLogServiceImpl implements IBabyDailyLogService {

    @Override
    public BabyDailyLogDetailResponse getDailyLogDetail(UUID babyLogId, UUID callerId) {
        // UC194 scope — not re-stubbed here
        throw new UnsupportedOperationException("Not implemented — Red Phase stub (UC194 scope)");
    }

    @Override
    public void deleteBabyDailyLog(UUID babyLogId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub (UC195 scope)");
    }
}

// BabyAccessPolicy.java — canManage() stub added alongside UC192's EXISTING canView()
@Component
public class BabyAccessPolicy {

    public boolean canView(BabyProfile profile, UUID callerId) {
        // UC192 — already shipped, NOT re-stubbed
        return /* existing shipped logic, untouched */ false;
    }

    public boolean canManage(BabyProfile profile, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub (UC195 scope)");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `DAILYLOG-TC-001` | `throw('Not implemented — UC195 scope')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `DAILYLOG-TC-002` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DAILYLOG-TC-003` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DAILYLOG-TC-004` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DAILYLOG-TC-005` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DAILYLOG-TC-007` | `throw(...)` (canManage stub) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DAILYLOG-TC-008` | `throw(...)` (canManage stub) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DAILYLOG-TC-009` | `throw(...)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled when implementation begins)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` (to be generated by `./mvnw test` run against stub)

> **Nếu bất kỳ test PASS bất thường trên stub:** Dừng lại — dấu hiệu tautology hoặc test không thực sự gọi vào `deleteBabyDailyLog()`/`canManage()`. Rewrite theo Props Isolation Pattern (§4).

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-BABY-IMP-004` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Tech Lead
- [ ] Flyway migration `V20260707111000__add_baby_daily_log_status.sql` đã được approved và chạy thành công trên staging
- [ ] UC194's `BabyDailyLog`, `BabyDailyLogRepository`, `IBabyDailyLogService`, `BabyDailyLogServiceImpl`, `BabyDailyLogController` tồn tại trong codebase (implement trước hoặc song song với UC195 — coordination cần thiết vì UC195 EXTENDS các file này)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị qua `BabyDailyLogTestFactory`

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả 13 unit/security tests xanh (không có skip)
- [ ] `./mvnw verify` — 2 integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `deleteBabyDailyLog()` method mới và `canManage()` method mới
- [ ] Không có business logic trong `BabyDailyLogController` (chỉ validation + mapping) — verify qua `DAILYLOG-TC-006`
- [ ] Không có PII (`note` content) xuất hiện plaintext trong audit logs — verify qua `DAILYLOG-TC-010`
- [ ] `flutter test` — 4 mobile widget tests (§9) xanh

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với `UnsupportedOperationException` stub trước khi implement `deleteBabyDailyLog()`/`canManage()`
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests:
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " BabyDailyLogServiceImplDeleteTest.java
  # Mọi instance PHẢI nằm trong @Test hoặc dùng BabyDailyLogTestFactory
  ```
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/ADR) — đã thực hiện ở mỗi TC trên
- [ ] **No hard-delete regression** — `DAILYLOG-TC-009` PASS xác nhận `repository.delete()`/`deleteById()` never called

### Suspension Criteria (Điều kiện tạm dừng)

- UC194's classes chưa tồn tại trong codebase (blocker dependency — UC195 EXTENDS chúng, không thể compile trước)
- Migration `V20260707111000` bị conflict với migration khác trong cùng version range
- Phát hiện lỗi kiến trúc mới (vd: `permission_json` model cần cho OI-1) cần Tech Lead review

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production nếu đã có status=DELETED data)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.baby_daily_logs DROP COLUMN IF EXISTS status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260707111000';"

# Revert implementation files (CHỈ các đoạn UC195 thêm vào, KHÔNG revert UC194's existing code)
git diff HEAD -- src/main/java/com/carebridge/backend/baby/controller/BabyDailyLogController.java
git diff HEAD -- src/main/java/com/carebridge/backend/baby/service/impl/BabyDailyLogServiceImpl.java
git diff HEAD -- src/main/java/com/carebridge/backend/baby/policy/BabyAccessPolicy.java
# Revert chỉ các hunk liên quan deleteBabyDailyLog()/canManage() — dùng git apply -R với patch cụ thể,
# KHÔNG git checkout toàn file (sẽ xoá luôn UC194's code nếu đã merge chung file)

git checkout -- src/main/resources/db/migration/V20260707111000__add_baby_daily_log_status.sql
git checkout -- src/test/java/com/carebridge/backend/baby/*Delete*Test.java

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu có)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (mọi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với `UnsupportedOperationException` stub (§5.1) | ☐ (verify khi implement) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume `canManage()` == `canView()` semantics không có ADR | ☑ (`DAILYLOG-TC-002`/`007` explicitly guard against this) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic (path `babyId` used for auth) | ☑ (`DAILYLOG-TC-006` explicitly guards this) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ (verify khi implement — `AuditService`/`AuditAction.BABY_DAILY_LOG_DELETED` cần được thêm trước, xem TDS §8.3) | G-3 |

**Kết quả review:**

- [x] Constraint traceability confirmed cho toàn bộ 15 backend test cases — chưa phát hiện anti-pattern ở giai đoạn spec (pre-implementation)
- [ ] Cần re-review sau khi Red Gate (§5.1) chạy thực tế

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none tại thời điểm viết — pending implementation)_ | | | | |

---

## 9. Mobile Widget Tests (Flutter)

> Package: `05_Development/CareBridgeMobileApp/lib/features/baby/` (screens/, services/, models/) — greenfield cho delete action, KHÔNG dùng `babyCare/` stub folder, nhất quán với UC194 TDS's note.

### DAILYLOG-TC-MOB-001 — Xoá log qua confirmation dialog → gọi API DELETE → thành công

**Feature Under Test:** `BabyDailyLogService.deleteDailyLog()` + UI confirmation flow (Flutter widget, screen name TBD — extends UC194's daily-log-detail screen with a delete action)
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** 🔴 RED

```dart
testWidgets('tapping delete + confirm calls DELETE endpoint and pops with success', (tester) async {
  // Arrange: mock BabyService/BabyDailyLogService.deleteDailyLog() returns success
  // Act: pump daily log detail screen, tap delete icon, confirm in AlertDialog
  // Assert: service.deleteDailyLog(babyId, logId) called once; success SnackBar shown; screen pops
});
```

**Expected Result (PASS):** API called exactly once after explicit confirmation (no accidental delete on single tap — dialog required)
**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-MOB-002 — Huỷ confirmation dialog → KHÔNG gọi API

**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** 🔴 RED

```dart
testWidgets('cancelling the confirmation dialog does not call delete API', (tester) async {
  // Act: tap delete icon, tap "Cancel" in dialog
  // Assert: service.deleteDailyLog() never called; screen state unchanged
});
```

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-MOB-003 — API trả 403 → hiển thị thông báo "not permitted", KHÔNG pop screen

**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** 🔴 RED

```dart
testWidgets('403 DAILYLOG-003 response shows permission-denied message, screen stays', (tester) async {
  // Arrange: mock service throws ApiException(403, 'DAILYLOG-003')
  // Act: confirm delete
  // Assert: error SnackBar/dialog shown with permission message; screen NOT popped
});
```

**Current Status:** 🔴 Not written

---

### DAILYLOG-TC-MOB-004 — Network failure → retry-able error state, không crash

**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** 🔴 RED

```dart
testWidgets('network failure during delete shows retry option, does not crash', (tester) async {
  // Arrange: mock service throws SocketException/timeout
  // Act: confirm delete
  // Assert: error state with retry action displayed; no unhandled exception in widget tree
});
```

**Current Status:** 🔴 Not written

---

*TDD Spec v1.0 — CASE 2.0 Anti-Pattern Detection & Red Gate Protocol, scoped tới UC195's NEW methods only.*
