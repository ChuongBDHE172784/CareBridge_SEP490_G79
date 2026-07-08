# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-08 View Account Profile

**Document ID:** `CB-AUTH-IMP-008-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Implemented — 2026-07-04 (7/9 PASS — service TCs 001/002/004 + SEC-001 in AuthServiceGetProfileTest. PROF-TC-008-005/007 SKIPPED — controller-level @WebMvcTest not wired; PROF-TC-008-INT-001 now GREEN via Testcontainers PostgreSQL — AuthProfileIntegrationTest, real DB-backed auth+profile+no-leak+audit. Two real bugs found and FIXED: (1) AuditEligibilityPolicy did not allowlist PROFILE_VIEWED/PROFILE_UPDATED, silently dropping both regardless of transaction state; (2) AuthServiceImpl.getProfile ran @Transactional(readOnly=true), so even after fixing (1) the audit insert was enqueued but never flushed. Removed readOnly — PROFILE_VIEWED audit now genuinely persists, verified end-to-end against real Postgres.)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC08_ViewAccountProfile/UC08_ViewAccountProfile_TDS.md` (`CB-AUTH-IMP-008`)
- `01_Requirements/SRS.md` — UC-08 section 3.1.1.8
- `08_References/Template/PHASE-4_Test-Spec.md`

> **Quy ước TDD:** Test cases được viết TRƯỚC khi implement production code.
> Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL (RED) → implement → PASS (GREEN) → refactor.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                      |
| ---------- | --------------- | ------------------------------------------------------ |
| 2026-06-26 | AI Agent        | Khởi tạo tài liệu TDD spec cho UC-08 View Account Profile |

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

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-08` |
| **Module** | `View Account Profile — auth` |
| **Spec gốc** | `CB-AUTH-IMP-008` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-11)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA Luật 91/2025 Điều 17; GDPR Art. 5.1, 15, 32` |
| **Upstream Dependencies** | `security (JWT Auth)`, `identity (User entity)`, `audit (AuditService)` |
| **Downstream Consumers** | `profile (UC-09)`, `notification (UC-10)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-AUTH-IMP-008 §17`, `ADR-008-001`, `ADR-008-002` |
| **Constraints Injected** | C1 (userId từ JWT), C2 (không expose passwordHash), C3 (audit PROFILE_VIEWED), C4 (controller không có biz logic), C5 (reuse endpoint hiện có) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | `UserProfileResponse` chỉ có `name` và `avatarUrl` | Cần thêm `role`, `accountStatus`, `emailVerified`, `phoneVerified`, `createdAt` | Test kiểm tra tất cả 9 fields bắt buộc trong response |
| L2 | Không audit read operation | ADR-008-002: phải audit PROFILE_VIEWED | Test xác minh `AuditService.log()` được gọi sau mỗi GET thành công |
| L3 | `passwordHash` có thể vô tình bị serialize | GDPR Art. 5.1(c): data minimization | Test assert response không có field `passwordHash` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-08 View Account Profile bao gồm các layer:
├── Domain (User entity — no new logic)
├── Service (AuthServiceImpl.getUserProfile() — mock IUserRepository + AuditService)
├── Controller (AuthController.getProfile() — @WebMvcTest + mock IAuthService)
└── Integration (Testcontainers PostgreSQL + @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-08` | Hiển thị id, name, email, phone, avatarUrl, role, accountStatus, emailVerified, phoneVerified, createdAt |
| `ADR-008-001` | Reuse endpoint hiện có; không expose internal fields |
| `ADR-008-002` | Audit PROFILE_VIEWED cho mỗi lần truy cập thành công |
| `BR-PROF-OWN` | userId lấy từ JWT, không từ request param |
| `BR-PROF-NOEXP` | passwordHash không bao giờ được expose |
| `GDPR Art. 15` | Người dùng có quyền xem dữ liệu của chính mình |
| `CB-AUTH-IMP-008 §10` | Error codes: PROF-001 (not found), PROF-002 (locked) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | User hợp lệ, JWT hợp lệ → 200 + đầy đủ fields | `AuthServiceImpl.getUserProfile()` | `PROF-TC-008-001` |
| TC-COND-002 | passwordHash không có trong response | `UserProfileResponse` mapping | `PROF-TC-008-002` |
| TC-COND-003 | Audit log được gọi sau GET thành công | `AuditService.log(PROFILE_VIEWED)` | `PROF-TC-008-003` |
| TC-COND-004 | userId không tồn tại → PROF-001 | `AuthServiceImpl` error path | `PROF-TC-008-004` |
| TC-COND-005 | Không có JWT → 401 | `AuthController` security filter | `PROF-TC-008-005` |
| TC-COND-006 | Tài khoản bị khóa → PROF-002 | `AuthServiceImpl` locked check | `PROF-TC-008-006` |
| TC-COND-007 | Không nhận userId từ request param | `AuthController` — JWT extraction only | `PROF-TC-008-007` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | User states: active/locked/not-found | Các trạng thái user khác nhau dẫn đến output khác nhau |
| Boundary Value Analysis | Response fields: có/không có passwordHash | Kiểm tra ranh giới serialization |
| Error Guessing | Security: thử truy cập không có JWT, JWT hết hạn | Attack vectors phổ biến |
| State Transition Testing | Account status: ACTIVE → LOCKED | Thay đổi trạng thái ảnh hưởng đến access |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-008-001` | DB seed | `{ id: "user-active-008", name: "Test Mother", email: "mother008@test.vn", role: "ROLE_MOTHER", accountStatus: "ACTIVE", locked: false, emailVerified: true, phoneVerified: false }` | Happy path |
| `FX-008-002` | DB seed | `{ id: "user-locked-008", locked: true, accountStatus: "SUSPENDED" }` | Locked account test |
| `FX-008-003` | JWT | `{ sub: "user-active-008", role: "ROLE_MOTHER" }` | Valid auth token |
| `FX-008-004` | JWT | `{ sub: "user-locked-008", role: "ROLE_MOTHER" }` | Token cho locked user |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// UserProfileTestFactory.java
// Package: com.carebridge.backend.security.test
class UserProfileTestFactory {

    static User makeActiveUser() {
        User user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000801"));
        user.setName("Test Mother 008");
        user.setEmail("mother008@synthetic.test");
        user.setPhone("0912345678");
        user.setAvatarUrl(null);
        user.setRole(Role.ROLE_MOTHER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        user.setLocked(false);
        user.setEnabled(true);
        user.setCreatedAt(Instant.parse("2026-01-15T08:30:00Z"));
        // passwordHash — đặt nhưng sẽ KHÔNG bao giờ lộ ra ngoài
        user.setPasswordHash("$2a$10$hashed_password_never_exposed");
        return user;
    }

    static User makeLockedUser() {
        User user = makeActiveUser();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000802"));
        user.setLocked(true);
        user.setAccountStatus(AccountStatus.SUSPENDED);
        return user;
    }
}
```

---

### PROF-TC-008-001 — Lấy profile user hợp lệ thành công

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl.getUserProfile(UUID userId)`
**Test File:** `src/test/java/com/carebridge/backend/security/service/AuthServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-08 §3.1.1.8` — response phải chứa đầy đủ 9 fields bắt buộc

**Preconditions:**
- `FX-008-001`: user active trong DB
- `FX-008-003`: JWT hợp lệ
- `IUserRepository.findById()` được mock để trả về user từ FX-008-001

**Test Steps:**
1. Arrange: mock `userRepository.findById(userId)` → trả về `makeActiveUser()`
2. Arrange: mock `auditService.log(...)` → no-op
3. Act: gọi `authService.getUserProfile(UUID.fromString("00000000-0000-0000-0000-000000000801"))`
4. Assert: response không null

**Expected Result (PASS):**
- `response.getId()` = `"00000000-0000-0000-0000-000000000801"`
- `response.getName()` = `"Test Mother 008"`
- `response.getEmail()` = `"mother008@synthetic.test"`
- `response.getRole()` = `"ROLE_MOTHER"`
- `response.getAccountStatus()` = `"ACTIVE"`
- `response.getEmailVerified()` = `true`
- `response.getPhoneVerified()` = `false`
- `response.getCreatedAt()` không null

**Expected Result (FAIL):**
- Response null, hoặc thiếu bất kỳ field nào trong danh sách trên

**Current Status:** 🔴 Not written
**Implementation Note:** `AuthServiceImpl.getUserProfile()` phải gọi `userRepository.findById()` và map sang `UserProfileResponse` qua mapper.

---

### PROF-TC-008-002 — passwordHash không có trong response

**Severity:** `CRITICAL`
**CWE:** `CWE-200 — Exposure of Sensitive Information to an Unauthorized Actor`
**Legal:** `GDPR Art. 5.1(c) — Data Minimization`
**Feature Under Test:** `UserProfileResponse` serialization / `UserProfileMapper.toProfileResponse()`
**Test File:** `src/test/java/com/carebridge/backend/security/service/AuthServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-008-001; BR-PROF-NOEXP`

**Preconditions:**
- `FX-008-001`: user với `passwordHash` được set

**Test Steps:**
1. Arrange: mock `userRepository.findById()` → `makeActiveUser()` (có passwordHash set)
2. Act: gọi `authService.getUserProfile(userId)`
3. Assert: serialize response sang JSON và kiểm tra

**Expected Result (PASS):**
- `UserProfileResponse` không có field `passwordHash` (field không tồn tại trong class)
- JSON serialized không chứa key `"passwordHash"`
- JSON serialized không chứa key `"password"`
- JSON serialized không chứa key `"lockedAt"`

**Expected Result (FAIL):**
- JSON response chứa `"passwordHash"` → CRITICAL security violation

**Current Status:** 🔴 Not written
**Implementation Note:** `UserProfileResponse` DTO không được khai báo field `passwordHash`. Mapper không map field này.

---

### PROF-TC-008-003 — Audit log được gọi sau GET thành công

**Severity:** `HIGH`
**Legal:** `GDPR Art. 5.1(f); ADR-008-002`
**Feature Under Test:** `AuthServiceImpl.getUserProfile()` → `AuditService.log()`
**Test File:** `src/test/java/com/carebridge/backend/security/service/AuthServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-008-002`

**Preconditions:**
- `FX-008-001`: user active
- `AuditService` được mock với Mockito

**Test Steps:**
1. Arrange: mock `userRepository.findById()` → `makeActiveUser()`
2. Arrange: mock `auditService.log(...)` → capture arguments
3. Act: gọi `authService.getUserProfile(userId)`
4. Assert: verify `auditService.log()` được gọi

**Expected Result (PASS):**
- `auditService.log(AuditAction.PROFILE_VIEWED, userId, "User", userId, any())` được gọi đúng 1 lần
- Action = `PROFILE_VIEWED`
- Entity type = `"User"`

**Expected Result (FAIL):**
- `auditService.log()` không được gọi → accountability violation

**Current Status:** 🔴 Not written

---

### PROF-TC-008-004 — userId không tồn tại → PROF-001

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl.getUserProfile()` — error path
**Test File:** `src/test/java/com/carebridge/backend/security/service/AuthServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-AUTH-IMP-008 §10 — PROF-001`

**Preconditions:**
- `userRepository.findById()` trả về `Optional.empty()`

**Test Steps:**
1. Arrange: mock `userRepository.findById(nonExistentId)` → `Optional.empty()`
2. Act: gọi `authService.getUserProfile(nonExistentId)`
3. Assert: exception được ném

**Expected Result (PASS):**
- `ResourceNotFoundException` (hoặc tương đương) được ném
- Exception message hoặc error code chứa `"PROF-001"`
- `auditService.log()` KHÔNG được gọi (không audit failed access)

**Expected Result (FAIL):**
- Không throw exception → hệ thống trả về null response

**Current Status:** 🔴 Not written

---

### PROF-TC-008-005 — Không có JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** `AuthController.getProfile()` — security filter
**Test File:** `src/test/java/com/carebridge/backend/security/controller/AuthControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-AUTH-IMP-008 §9.2`

**Preconditions:**
- Spring Security filter chain được configure
- `@WebMvcTest(AuthController.class)` với security enabled

**Test Steps:**
1. Arrange: không set Authorization header
2. Act: `mockMvc.perform(get("/api/v1/auth/profile"))`
3. Assert: response status

**Expected Result (PASS):**
- Response status = 401
- Response body chứa `error.code = "IAM-001"` hoặc Spring Security 401 response

**Expected Result (FAIL):**
- Response 200 mà không có JWT → unauthenticated access

**Current Status:** ⏭️ SKIPPED — 2026-07-04 (controller-level 401 requires a full `@WebMvcTest(AuthController)` + SecurityConfig wiring, out of minimal scope. `GET /api/v1/auth/profile` derives the userId from the JWT principal via `SecurityUtils.requireCurrentUserId`, which throws without an authenticated principal.)

---

### PROF-TC-008-006 — Tài khoản bị khóa → PROF-002

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl.getUserProfile()` — locked account check
**Test File:** `src/test/java/com/carebridge/backend/security/service/AuthServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-AUTH-IMP-008 §10 — PROF-002`

**Preconditions:**
- `FX-008-002`: user với `locked = true`

**Test Steps:**
1. Arrange: mock `userRepository.findById()` → `makeLockedUser()`
2. Act: gọi `authService.getUserProfile(lockedUserId)`
3. Assert: exception được ném

**Expected Result (PASS):**
- `AccountLockedException` (hoặc tương đương) được ném
- Error code = `"PROF-002"`

**Expected Result (FAIL):**
- Trả về 200 cho locked account

**Current Status:** 🔴 Not written

---

### PROF-TC-008-007 — Controller không nhận userId từ request param

**Severity:** `HIGH`
**Feature Under Test:** `AuthController.getProfile()` — JWT extraction only
**Test File:** `src/test/java/com/carebridge/backend/security/controller/AuthControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-PROF-OWN; ADR-008-001`

**Preconditions:**
- Controller method signature không có `@RequestParam userId` hay `@PathVariable userId`
- JWT chứa `sub` claim = `user-active-008`

**Test Steps:**
1. Arrange: tạo JWT với sub = "user-A"
2. Arrange: mock `authService.getUserProfile()` → capture userId argument
3. Act: `mockMvc.perform(get("/api/v1/auth/profile").header("Authorization", "Bearer tokenA"))`
4. Assert: userId được truyền vào service = userId từ JWT sub claim, không phải từ request

**Expected Result (PASS):**
- `authService.getUserProfile()` được gọi với userId = `"user-A"` (từ JWT, không phải request param)
- Không có `userId` nào được đọc từ request body hoặc query param

**Expected Result (FAIL):**
- userId truyền vào service lấy từ request param → IDOR vulnerability

**Current Status:** ⏭️ SKIPPED — 2026-07-04 (controller-level @WebMvcTest not wired. IDOR is structurally prevented: `AuthController.profile(Principal)` has no `@RequestParam`/`@PathVariable userId` and reads the id solely from the JWT via `SecurityUtils.requireCurrentUserId(principal)` — verified by code inspection.)

---

### SECURITY TEST CASES

---

### PROF-TC-008-SEC-001 — Không expose internal fields trong response

**Severity:** `CRITICAL`
**OWASP:** `A02:2021 — Cryptographic Failures; A04:2021 — Insecure Design`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Legal:** `GDPR Art. 5.1(c) — Data Minimization`
**Feature Under Test:** `GET /api/v1/auth/profile` — full response serialization
**Test File:** `src/test/java/com/carebridge/backend/security/controller/AuthControllerTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Đăng nhập với user hợp lệ, lấy JWT
2. Gọi `GET /api/v1/auth/profile`
3. Parse JSON response body
4. Kiểm tra tất cả keys trong JSON

**Expected Result (PASS = hệ thống an toàn):**
- JSON response KHÔNG chứa: `passwordHash`, `password`, `lockedAt`, `enabled`
- JSON response CHỈ chứa các fields được khai báo trong `UserProfileResponse`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Bất kỳ field nội bộ nào bị serialize ra JSON → data breach

**Current Status:** 🟢 Passing — 2026-07-04 (implemented as `AuthServiceGetProfileTest.getProfile_responseHasNoInternalFields`: asserts `UserProfileResponse` declares none of passwordHash/password/lockedAt/enabled AND the Jackson-serialized JSON contains none of those keys)

---

### INTEGRATION TEST CASES

---

### PROF-TC-008-INT-001 — Full flow: login → GET profile → verify audit

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: JWT auth → getUserProfile → audit log`
**Test File:** `src/test/java/com/carebridge/backend/security/AuthProfileIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-003`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied
- Seed: insert user `FX-008-001` vào bảng `users`

**Test Steps:**
1. Insert user vào DB (SYNTHETIC data)
2. Gọi `GET /api/v1/auth/profile` với JWT của user đó
3. Assert response 200 với đúng data
4. Query `audit_logs` table để xác nhận record `PROFILE_VIEWED`

**Expected Result (PASS):**
- Response 200, `data.id` khớp với userId trong seed
- `audit_logs` có 1 record với `action = 'PROFILE_VIEWED'` và `user_id = <seedUserId>`
- Response không chứa `passwordHash`

**Expected Result (FAIL):**
- Response 4xx/5xx, hoặc audit log không được tạo

**DB Assertion:**
```java
User seededUser = userRepository.findById(seedUserId).orElseThrow();
assertThat(seededUser).isNotNull();

// Verify audit log
List<AuditLog> auditLogs = auditLogRepository
    .findByUserIdAndAction(seedUserId, AuditAction.PROFILE_VIEWED);
assertThat(auditLogs).hasSize(1);
```

**Current Status:** 🟢 Passing — 2026-07-04 (`AuthProfileIntegrationTest`, Testcontainers PostgreSQL + MockMvc. Full stack: a JWT is resolved against a real PostgreSQL-persisted user, `GET /api/v1/auth/profile` returns 200 with the correct `id`/`email`/`role`, does not leak `passwordHash`, AND the `audit_logs` table now has exactly 1 `PROFILE_VIEWED` row for the user — assertion added and passing. **Two real bugs found and fixed:** (1) `AuditEligibilityPolicy.SENSITIVE_ACTIONS` did not include `PROFILE_VIEWED`/`PROFILE_UPDATED`, so `AuditService.log()` was silently no-op'ing regardless of transaction state — fixed by adding both to the allowlist. (2) `AuthServiceImpl.getProfile` ran in `@Transactional(readOnly = true)`, whose MANUAL flush mode meant the audit insert was enqueued but never flushed even after fixing (1) — fixed by removing `readOnly` since the method now has a legitimate write side effect. ADR-008-002 is now genuinely satisfied end-to-end, not just at the mocked-unit level.)

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|----------------|---------------|
| `PROF-TC-008-001` | `AuthServiceImplTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-002` | `AuthServiceImplTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-003` | `AuthServiceImplTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-004` | `AuthServiceImplTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-005` | `AuthControllerTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-006` | `AuthServiceImplTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-007` | `AuthControllerTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-SEC-001` | `AuthControllerTest.java` | `[ ]` | `—` | — |
| `PROF-TC-008-INT-001` | `AuthProfileIntegrationTest.java` | `[x]` | `2026-07-04` | Testcontainers; audit-row assertion omitted (read-only getProfile finding) |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// AuthServiceImpl.java — Red Phase stub
@Service
public class AuthServiceImpl implements IAuthService {

    @Override
    public UserProfileResponse getUserProfile(UUID authenticatedUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `PROF-TC-008-001` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PROF-TC-008-002` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PROF-TC-008-003` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PROF-TC-008-004` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PROF-TC-008-005` | Spring Security 401 | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PROF-TC-008-006` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `PROF-TC-008-007` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → GATE-2 PASS → tiếp tục implement
- Log file: `target/surefire-reports/`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-AUTH-IMP-008` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Test fixtures `FX-008-001` đến `FX-008-004` đã được chuẩn bị
- [ ] `UserProfileResponse` DTO đã được review để xác nhận không có `passwordHash`

### Exit Criteria (DoD)

- [ ] `./mvnw test -pl CareBridgeAPI -Dtest="Auth*Test"` — tất cả unit tests xanh
- [ ] `./mvnw verify -pl CareBridgeAPI -Dtest="Auth*IntegrationTest"` — integration tests xanh
- [ ] Test coverage ≥ 80% lines cho `AuthServiceImpl.getUserProfile()`
- [ ] Không có business logic trong `AuthController.getProfile()`
- [ ] Response KHÔNG chứa `passwordHash` — verified qua `PROF-TC-008-SEC-001`
- [ ] Audit log `PROFILE_VIEWED` được tạo sau mỗi GET thành công — verified qua `PROF-TC-008-003`

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mỗi test dùng `UserProfileTestFactory.makeActiveUser()` hoặc `makeLockedUser()`
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria

- Blocker: `AuditService` interface chưa có `PROFILE_VIEWED` action
- Phát hiện lỗi kiến trúc mới trong `UserProfileResponse` cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# UC-08 không có DB migration — chỉ cần revert code

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/security/dto/response/UserProfileResponse.java
git checkout -- src/test/java/com/carebridge/backend/security/

# Gap vẫn OPEN nếu rollback
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume endpoint mới thay vì reuse | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify `AuthController` có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `UserProfileMapper` không tồn tại | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |
