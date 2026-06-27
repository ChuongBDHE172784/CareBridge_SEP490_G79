# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC15 — Vô hiệu hóa Tài khoản (Deactivate Own Account)

**Document ID:** `CB-AUTH-IMP-015-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC15_DeactivateOwnAccount/UC15_DeactivateOwnAccount_TDS.md` (CB-AUTH-IMP-015 v1.0) — Technical Design Spec
- `01_Requirements/SRS.md` §3.1.1.15 — UC-15 Functional requirements
- ADR-015-001 through ADR-015-004 (embedded in TDS §3)
- Business Rules: BR-DEACT-001, BR-DEACT-002, BR-DEACT-003, BR-DEACT-004

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-06-26` | `AI Agent` | Khởi tạo tài liệu — TDD spec cho UC15 DeactivateOwnAccount |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-15` |
| **Module** | `Deactivate Own Account — Security / IAM Bounded Context` |
| **Spec gốc** | `CB-AUTH-IMP-015` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S3 (2026-07-01 → 2026-07-14)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA — 90-day retention (ADR-015-003)` |
| **Upstream Dependencies** | `IAM Module (JWT), RefreshTokenRepository, DeviceTokenRepository, AuditService` |
| **Downstream Consumers** | `Scheduled Deletion Job (ngoài scope), Admin Reactivation UC (ngoài scope)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-AUTH-IMP-015 §17`, `BR-DEACT-001 through BR-DEACT-004`, `ADR-015-001 through ADR-015-004` |
| **Constraints Injected** | `C1: BCrypt verify; C2: ADMIN check 403; C3: set BOTH accountStatus AND enabled; C4: revoke all tokens; C5: emit ACCOUNT_DEACTIVATED; C6: @Transactional` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> **Bắt buộc điền trước khi viết test.**
> Liệt kê mọi sai lệch giữa spec thiết kế và schema/policy/codebase thực tế.
> Test cases sẽ encode hành vi **đã sửa**, không phải hành vi trong spec gốc.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec có thể cho rằng ADMIN bị reject vì không tìm thấy quyền (`403 Forbidden` từ `@PreAuthorize`) | ADMIN bị reject explicitly tại SERVICE layer với error code `AUTH-083` — trước khi verify password — vì cần error code phân biệt | Test L1: mock ADMIN user → expect `AUTH-083` từ service (không phải Spring Security 403) |
| L2 | Spec có thể set chỉ `accountStatus = "DEACTIVATED"` | Phải set CẢ HAI: `accountStatus = "DEACTIVATED"` VÀ `enabled = false` — hai fields khác nhau, đồng bộ nhau theo schema | Integration test assert CẢ HAI fields trong DB sau deactivation |
| L3 | Spec không nói rõ deactivation có xóa dữ liệu ngay không | Deactivation là SOFT DELETE — dữ liệu giữ nguyên 90 ngày (ADR-015-003). Chỉ `accountStatus` và `enabled` thay đổi | Test KHÔNG assert record bị xóa; chỉ assert status change |
| L4 | Spec không nói rõ hành vi sau deactivation | Sau deactivation, login phải fail — nhưng current JWT còn valid đến expiry (15 phút). Subsequent login bằng credentials phải fail | Integration test L4: thử login sau deactivation → expect 401/400 với "Account deactivated" |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
UC15 DeactivateOwnAccount bao gồm các layer:
├── Domain (User entity — accountStatus, enabled fields; Role enum)
├── Application / Use Cases (mock IUserRepository, RefreshTokenRepo, DeviceTokenRepo)
├── Services (AuthServiceImpl.deactivate() — mock repos với Mockito)
├── Controller (AuthController.deactivate() — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — verify DB state)
```

**Ngoài phạm vi:**
- Scheduled Deletion Job (90 ngày sau deactivation)
- Admin reactivation flow
- Notification email khi account deactivated
- Current JWT invalidation (token blacklist — separate mechanism)

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS §3.1.1.15` | UC-15 actor: MOTHER, EXPERT; platform: Mobile + Web |
| `BR-DEACT-001` | Yêu cầu password re-authentication |
| `BR-DEACT-002` | Thu hồi tất cả refresh + FCM tokens |
| `BR-DEACT-003` | 90-day retention — NO immediate data delete |
| `BR-DEACT-004` | ADMIN accounts cannot self-deactivate |
| `ADR-015-002` | BCrypt 12 rounds for password verify |
| `ADR-015-004` | Immediate token revocation |
| `CB-AUTH-IMP-015 §10` | Error codes AUTH-081, AUTH-082, AUTH-083 |
| `CB-AUTH-IMP-015 §17` | Constraint C3: BOTH fields must be set atomically |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | MOTHER user, ACTIVE account, correct password, no prior deactivation | `AuthServiceImpl.deactivate()` — happy path | `DEACT-TC-015-001` |
| TC-COND-002 | Correct JWT, wrong confirmPassword | `AuthServiceImpl` — BCrypt verify fail | `DEACT-TC-015-002` |
| TC-COND-003 | User accountStatus = "DEACTIVATED" already | `AuthServiceImpl` — pre-check status | `DEACT-TC-015-003` |
| TC-COND-004 | JWT role = ADMIN, any password | `AuthServiceImpl` — role guard | `DEACT-TC-015-004` |
| TC-COND-005 | No JWT Bearer token | Spring Security filter | `DEACT-TC-015-005` |
| TC-COND-006 | Both DB fields updated atomically | `UserRepository.save()` — DB state after call | `DEACT-TC-015-INT-001` |
| TC-COND-007 | All refresh tokens revoked after deactivation | `RefreshTokenRepository.revokeAllByUserId()` | `DEACT-TC-015-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | accountStatus: ACTIVE → DEACTIVATED | Đảm bảo state machine hoạt động đúng (§6.3 TDS) |
| Equivalence Partitioning | Role: MOTHER/EXPERT (allowed) vs ADMIN (forbidden) | Phân loại rõ roles |
| Error Guessing | Wrong password, already deactivated, no JWT | Các lỗi phổ biến trong account management |
| Boundary Value Analysis | Boundary test: account ACTIVE (deactivatable) vs DEACTIVATED (not) | Status boundary |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-AUTH-001` | DB seed / mock | User MOTHER, id=MOTHER_USER_ID, ACTIVE, enabled=true, passwordHash=BCrypt("MyPassword@123") | Happy path + error paths |
| `FX-AUTH-002` | DB seed / mock | User MOTHER, id=MOTHER_USER_ID, DEACTIVATED, enabled=false | AUTH-082 test |
| `FX-AUTH-003` | DB seed / mock | User ADMIN, id=ADMIN_USER_ID, ACTIVE, role=ADMIN | AUTH-083 test |
| `FX-AUTH-004` | JWT | `{ sub: MOTHER_USER_ID, role: "ROLE_MOTHER" }` | Auth context cho happy path |
| `FX-AUTH-005` | JWT | `{ sub: ADMIN_USER_ID, role: "ROLE_ADMIN" }` | Auth context cho ADMIN test |
| `FX-AUTH-006` | DB seed | 2 RefreshToken records với userId=MOTHER_USER_ID, revoked=false | INT-002: verify revocation |

---

## 4. Test Case Specification

> **TC ID format:** `DEACT-TC-015-NNN`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua factory. Không shared mutable state giữa các test cases.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng factory method
// ═══════════════════════════════════════════════════════════

// DeactivateAccountTestFactory.java
class DeactivateAccountTestFactory {

    static UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
    static UUID ADMIN_USER_ID  = UUID.fromString("00000000-0000-0000-0001-000000000015");

    static User makeMothwerUser() {
        return User.builder()
            .id(MOTHER_USER_ID)
            .phone("+84901234515")
            .passwordHash(new BCryptPasswordEncoder(12).encode("MyPassword@123"))
            .accountStatus("ACTIVE")
            .enabled(true)
            .role(Role.MOTHER)
            .build();
    }

    // Overload để override specific fields
    static User makeMothwerUser(Consumer<User> overrides) {
        User user = makeMothwerUser();
        overrides.accept(user);
        return user;
    }

    static User makeAdminUser() {
        return User.builder()
            .id(ADMIN_USER_ID)
            .phone("+84901234516")
            .passwordHash(new BCryptPasswordEncoder(12).encode("AdminPass@123"))
            .accountStatus("ACTIVE")
            .enabled(true)
            .role(Role.ADMIN)
            .build();
    }

    static DeactivateRequest makeRequest() {
        DeactivateRequest req = new DeactivateRequest();
        req.setConfirmPassword("MyPassword@123");
        return req;
    }

    static DeactivateRequest makeRequest(Consumer<DeactivateRequest> overrides) {
        DeactivateRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }
}
```

---

### DEACT-TC-015-001 — Happy path: MOTHER user deactivates với đúng password → 200

**Severity:** `CRITICAL`
**Feature Under Test:** `POST /api/v1/auth/deactivate — AuthServiceImpl.deactivate()`
**Test File:** `src/test/java/com/carebridge/backend/security/AuthServiceDeactivateTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS §3.1.1.15`, `CB-AUTH-IMP-015 §9` — HTTP 200 + message

**Preconditions:**
- JWT userId = MOTHER_USER_ID, role = ROLE_MOTHER
- User trong DB/mock: accountStatus = "ACTIVE", enabled = true, role = MOTHER
- `confirmPassword = "MyPassword@123"` khớp với passwordHash

**Test Steps:**
1. Arrange: mock `userRepository.findById(MOTHER_USER_ID)` trả về `DeactivateAccountTestFactory.makeMothwerUser()`; mock `passwordEncoder.matches()` trả về `true`; mock `userRepository.save()`, `refreshTokenRepository.revokeAllByUserId()`, `deviceTokenRepository.deactivateAllByUserId()`, `auditService.emit()` — tất cả void/no-op
2. Act: gọi `authService.deactivate(MOTHER_USER_ID, "MyPassword@123")`
3. Assert: không có exception throw; `userRepository.save()` được gọi 1 lần với user có `accountStatus = "DEACTIVATED"` và `enabled = false`; `refreshTokenRepository.revokeAllByUserId()` được gọi 1 lần; `auditService.emit()` được gọi 1 lần

**Expected Result (PASS — hành vi đúng):**
- Method completes without exception
- `userRepository.save(user)` được gọi với `user.getAccountStatus() == "DEACTIVATED"` AND `user.isEnabled() == false` (cả hai fields)
- `refreshTokenRepository.revokeAllByUserId(MOTHER_USER_ID)` được gọi
- `deviceTokenRepository.deactivateAllByUserId(MOTHER_USER_ID)` được gọi
- `auditService.emit(event)` được gọi với `eventType = "ACCOUNT_DEACTIVATED"`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception được throw không mong muốn
- `userRepository.save()` được gọi với chỉ 1 trong 2 fields thay đổi (L2 violation)
- Token revocation methods không được gọi

**Current Status:** 🔴 Not written
**Implementation Note:** C3 — PHẢI set ĐỒNG THỜI cả `accountStatus` và `enabled`. Dùng ArgumentCaptor để assert cả hai fields trong saved User object.

---

### DEACT-TC-015-002 — Wrong password confirmation → 400 AUTH-081

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl.deactivate()` — BCrypt password verification
**Test File:** `src/test/java/com/carebridge/backend/security/AuthServiceDeactivateTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-DEACT-001`, `CB-AUTH-IMP-015 §10` — AUTH-081

**Preconditions:**
- JWT userId = MOTHER_USER_ID
- User: ACTIVE, MOTHER, passwordHash = BCrypt("MyPassword@123")
- `confirmPassword = "WrongPassword456"` (không khớp)

**Test Steps:**
1. Arrange: mock `userRepository.findById()` trả về active MOTHER user; mock `passwordEncoder.matches("WrongPassword456", hash)` trả về `false`
2. Act: gọi `authService.deactivate(MOTHER_USER_ID, "WrongPassword456")`
3. Assert: throw `DeactivationException` (hoặc tương đương) với `errorCode = "AUTH-081"`; `userRepository.save()` KHÔNG được gọi; token revocation KHÔNG được gọi

**Expected Result (PASS — hành vi đúng):**
- `DeactivationException` với `errorCode = "AUTH-081"` được throw
- HTTP 400 Bad Request tại controller layer
- Response: `{ "error": { "code": "AUTH-081", "message": "Mật khẩu xác nhận không đúng" } }`
- Không có side effects (no DB writes)

**Expected Result (FAIL — dấu hiệu lỗi):**
- Method completes without exception (deactivation xảy ra với wrong password)
- Exception code sai (không phải AUTH-081)
- DB được modified dù password sai

**Current Status:** 🔴 Not written
**Implementation Note:** C1 — password check là step CUỐI trong validation chain (sau role check và status check). Nếu pass sai, early return với AUTH-081.

---

### DEACT-TC-015-003 — Account already DEACTIVATED → 400 AUTH-082

**Severity:** `MEDIUM`
**Feature Under Test:** `AuthServiceImpl.deactivate()` — pre-condition status check
**Test File:** `src/test/java/com/carebridge/backend/security/AuthServiceDeactivateTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-AUTH-IMP-015 §10` — AUTH-082

**Preconditions:**
- JWT userId = MOTHER_USER_ID
- User trong DB/mock: accountStatus = "DEACTIVATED", enabled = false, role = MOTHER

**Test Steps:**
1. Arrange: mock `userRepository.findById()` trả về `DeactivateAccountTestFactory.makeMothwerUser(u -> { u.setAccountStatus("DEACTIVATED"); u.setEnabled(false); })`
2. Act: gọi `authService.deactivate(MOTHER_USER_ID, "MyPassword@123")`
3. Assert: throw `DeactivationException` với `errorCode = "AUTH-082"`; password verification KHÔNG được thực hiện; `userRepository.save()` KHÔNG được gọi

**Expected Result (PASS — hành vi đúng):**
- `DeactivationException` với `errorCode = "AUTH-082"` được throw
- HTTP 400 Bad Request
- Response: `{ "error": { "code": "AUTH-082", "message": "Tài khoản đã bị vô hiệu hóa trước đó" } }`
- `passwordEncoder.matches()` KHÔNG được gọi (fail-fast trước khi check password — performance optimization)

**Expected Result (FAIL — dấu hiệu lỗi):**
- Method completes (deactivation bị duplicate)
- AUTH-081 được throw thay vì AUTH-082
- Password được verify dù account đã deactivated

**Current Status:** 🔴 Not written
**Implementation Note:** Status check thực hiện TRƯỚC password check để fail-fast. Không waste BCrypt computation trên deactivated accounts.

---

### DEACT-TC-015-004 — ADMIN tries to deactivate own account → 403 AUTH-083

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `AuthServiceImpl.deactivate()` — role guard (BR-DEACT-004)
**Test File:** `src/test/java/com/carebridge/backend/security/AuthServiceDeactivateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-DEACT-004`, `CB-AUTH-IMP-015 §10` — AUTH-083

**Preconditions:**
- JWT userId = ADMIN_USER_ID, role = ROLE_ADMIN
- User: accountStatus = "ACTIVE", role = ADMIN

**Test Steps (Attack Simulation):**
1. Arrange: mock `userRepository.findById(ADMIN_USER_ID)` trả về `DeactivateAccountTestFactory.makeAdminUser()`
2. Act: gọi `authService.deactivate(ADMIN_USER_ID, "AdminPass@123")`
3. Assert: throw `DeactivationException` với `errorCode = "AUTH-083"`; `passwordEncoder.matches()` KHÔNG được gọi (ADMIN check trước password check); `userRepository.save()` KHÔNG được gọi

**Expected Result (PASS = hệ thống an toàn):**
- `DeactivationException` với `errorCode = "AUTH-083"` được throw
- HTTP 403 Forbidden
- Response: `{ "error": { "code": "AUTH-083", "message": "Tài khoản Admin không thể tự vô hiệu hóa qua endpoint này" } }`
- Không có DB writes

**Expected Result (FAIL = lỗ hổng tồn tại):**
- ADMIN account bị deactivated thành công (nghiêm trọng — có thể lock out toàn bộ admin)
- AUTH-081 được throw thay vì AUTH-083 (sai order)

**Current Status:** 🔴 Not written
**Implementation Note:** C2 — ADMIN role check là BƯỚC ĐẦU TIÊN trong validation chain, trước cả status check và password check. Thứ tự: ADMIN check → status check → password verify → persist.

---

### DEACT-TC-015-005 — Không có JWT Bearer token → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `POST /api/v1/auth/deactivate` — Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/security/AuthControllerDeactivateTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-AUTH-IMP-015 §9` — Auth: JWT Bearer required; `CB-AUTH-IMP-015 §16` — GUEST = ❌

**Preconditions:**
- Không có `Authorization` header

**Test Steps:**
1. Arrange: không cấu hình SecurityContext
2. Act: `POST /api/v1/auth/deactivate` KHÔNG có `Authorization: Bearer ...` header, body = `{ "confirmPassword": "MyPassword@123" }`
3. Assert: HTTP 401; `AuthServiceImpl.deactivate()` KHÔNG được gọi

**Expected Result (PASS = hệ thống an toàn):**
- HTTP status: `401 Unauthorized`
- `authService.deactivate()` không được gọi (Spring Security reject trước khi vào controller)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- HTTP 200 hoặc 400 (deactivation bị trigger mà không có JWT)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSQLContainer`). Timeout: 120s.

---

### DEACT-TC-015-INT-001 — Integration: accountStatus=DEACTIVATED, enabled=false trong DB sau call

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: POST /api/v1/auth/deactivate → AuthServiceImpl → UserRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/security/DeactivateAccountIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-006`
**Oracle Source:** `CB-AUTH-IMP-015 §14` — DB Verification, `CB-AUTH-IMP-015 §17 C3`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migration applied (users table với account_status và enabled columns)
- Seed: user MOTHER_USER_ID với accountStatus="ACTIVE", enabled=true inserted via JPA

**Test Steps:**
1. Insert `DeactivateAccountTestFactory.makeMothwerUser()` vào DB
2. Tạo JWT token với userId=MOTHER_USER_ID
3. `POST /api/v1/auth/deactivate` với `{ "confirmPassword": "MyPassword@123" }` và Authorization header
4. Assert HTTP 200
5. Query DB để verify ĐỒNG THỜI cả hai fields

**Expected Result (PASS):**
- HTTP status `200 OK`
- DB assertion: CẢ HAI fields thay đổi
- `response.message = "Tài khoản đã được vô hiệu hóa"`

**Expected Result (FAIL):**
- Chỉ một trong hai fields thay đổi (L2 violation)
- Record bị xóa khỏi DB (L3 violation — soft delete, không phải hard delete)

**DB Assertion:**
```java
// DeactivateAccountIntegrationTest.java
User user = userRepository.findById(DeactivateAccountTestFactory.MOTHER_USER_ID)
    .orElseThrow(() -> new AssertionError("User không được xóa — phải còn trong DB (soft delete)"));

// L2: PHẢI kiểm tra CẢ HAI fields
assertThat(user.getAccountStatus()).isEqualTo("DEACTIVATED");
assertThat(user.isEnabled()).isFalse();

// L3: Record không bị xóa — chỉ status thay đổi
assertThat(user.getId()).isEqualTo(DeactivateAccountTestFactory.MOTHER_USER_ID);
assertThat(user.getPhone()).isEqualTo("+84901234515");
```

**Current Status:** 🔴 Not written

---

### DEACT-TC-015-INT-002 — Integration: Tất cả refresh tokens bị revoke sau deactivation

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl.deactivate()` → `RefreshTokenRepository.revokeAllByUserId()`
**Test File:** `src/test/java/com/carebridge/backend/security/DeactivateAccountIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-DEACT-002`, `ADR-015-004`, `CB-AUTH-IMP-015 §14` — DB Verification

**Preconditions:**
- PostgreSQL Testcontainer running
- Seed: user MOTHER_USER_ID (ACTIVE)
- Seed: 2 RefreshToken records với `userId = MOTHER_USER_ID, revoked = false`

**Test Steps:**
1. Insert user + 2 refresh tokens vào DB
2. Tạo JWT, gọi `POST /api/v1/auth/deactivate` với correct password
3. Assert HTTP 200
4. Query refresh_tokens table để verify tất cả bị revoke

**Expected Result (PASS):**
- HTTP 200 OK
- DB assertion: 0 active (non-revoked) refresh tokens cho MOTHER_USER_ID
- DB assertion: tất cả device tokens của user bị deactivated

**Expected Result (FAIL):**
- Active refresh tokens vẫn còn sau deactivation (ADR-015-004 violation)
- Chỉ 1 trong nhiều tokens bị revoke (partial revocation)

**DB Assertion:**
```java
// DeactivateAccountIntegrationTest.java

// Verify tất cả refresh tokens bị revoke
long activeTokenCount = refreshTokenRepository
    .countByUserIdAndRevokedFalse(DeactivateAccountTestFactory.MOTHER_USER_ID);
assertThat(activeTokenCount)
    .as("Tất cả refresh tokens phải bị revoke sau deactivation — ADR-015-004")
    .isEqualTo(0L);

// Verify tất cả device tokens bị deactivated
long activeDeviceTokenCount = deviceTokenRepository
    .countByUserIdAndActiveTrue(DeactivateAccountTestFactory.MOTHER_USER_ID);
assertThat(activeDeviceTokenCount)
    .as("Tất cả FCM device tokens phải bị deactivate sau deactivation — ADR-015-004")
    .isEqualTo(0L);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `DEACT-TC-015-001` | `AuthServiceDeactivateTest.java` | `[ ]` | ` ` | Extract validation chain sang private methods |
| `DEACT-TC-015-002` | `AuthServiceDeactivateTest.java` | `[ ]` | ` ` | ` ` |
| `DEACT-TC-015-003` | `AuthServiceDeactivateTest.java` | `[ ]` | ` ` | ` ` |
| `DEACT-TC-015-004` | `AuthServiceDeactivateTest.java` | `[ ]` | ` ` | Consider `@PreAuthorize("!hasRole('ADMIN')")` vs service check |
| `DEACT-TC-015-005` | `AuthControllerDeactivateTest.java` | `[ ]` | ` ` | ` ` |
| `DEACT-TC-015-INT-001` | `DeactivateAccountIntegrationTest.java` | `[ ]` | ` ` | ` ` |
| `DEACT-TC-015-INT-002` | `DeactivateAccountIntegrationTest.java` | `[ ]` | ` ` | ` ` |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Trước khi implement, chạy toàn bộ test suite với empty/throw stub.
> Mọi test PHẢI FAIL. Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class AuthServiceImpl implements IAuthService {

    @Override
    public void deactivate(UUID userId, String confirmPassword) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    // Các methods khác (login, register, etc.) vẫn implement bình thường
    // Chỉ stub method deactivate()
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `DEACT-TC-015-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `DEACT-TC-015-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEACT-TC-015-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEACT-TC-015-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEACT-TC-015-005` | N/A (Spring Security) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEACT-TC-015-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `DEACT-TC-015-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `target/surefire-reports/red-gate-evidence-uc15.log`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause. Rewrite test với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-AUTH-IMP-015` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm: L1 (ADMIN 403 source), L2 (BOTH fields), L3 (soft delete), L4 (login fails post-deactivation)
- [ ] Verify `users` table có `account_status` và `enabled` columns trên staging
- [ ] Verify `refresh_tokens` và `device_tokens` tables tồn tại và có `revoked`/`active` columns
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test -pl 05_Development/CareBridgeAPI` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify -pl 05_Development/CareBridgeAPI` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `AuthServiceImpl.deactivate()` method
- [ ] Không có business logic trong `AuthController.deactivate()` (chỉ validation + mapping)
- [ ] CẢ HAI fields `accountStatus` VÀ `enabled` được set trong cùng `@Transactional`
- [ ] Tất cả refresh tokens VÀ FCM device tokens bị revoke sau deactivation
- [ ] Audit event `ACCOUNT_DEACTIVATED` được emit sau mỗi successful deactivation
- [ ] Login thất bại sau deactivation (verified qua integration test L4)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — `DeactivateRequest`, `DeactivateResponse`, `IAuthService.deactivate()` tồn tại:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests:
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " \
    src/test/java/com/carebridge/backend/security/AuthServiceDeactivateTest.java
  # Mọi instance PHẢI nằm trong @Test hoặc dùng DeactivateAccountTestFactory
  ```
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR-DEACT-XXX, AUTH-08X, ADR-015-XXX)

### Suspension Criteria (Điều kiện tạm dừng)

- `refresh_tokens` hoặc `device_tokens` table chưa tồn tại trên staging
- `revokeAllByUserId` method chưa khả thi với schema hiện tại
- CI pipeline bị broken bởi thay đổi khác trên branch `dev`

---

## 7. Rollback Plan

```bash
# UC-15 không tạo migration mới — rollback chỉ là revert code

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/AuthServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/DeactivateRequest.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/DeactivateResponse.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/

# Nếu user bị deactivate sai — reactivate thủ công (Rollback Runbook §12.2 của TDS)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE users SET account_status='ACTIVE', enabled=true WHERE id='<userId>';"

# Restore revoked tokens nếu cần
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE refresh_tokens SET revoked=false WHERE user_id='<userId>';"

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> ⭐ Checklist cho reviewer khi test cases được AI hỗ trợ generate.

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(chưa có)_ | ` ` | ` ` | ` ` | ☐ |

**Ghi chú CASE 2.0 cho UC-15:**

- **AP-AI-005 risk cao:** `DeactivateRequest`, `DeactivateResponse`, `DeactivationException` là NEW classes — PHẢI verify chúng tồn tại trong codebase trước khi chạy test (`./mvnw compile`). Nếu compile fail với `cannot find symbol` → class chưa được tạo → tạo DTO trước, sau đó mới viết test (§11.3 Chặng 1).
- **AP-AI-003 risk:** Test DEACT-TC-015-004 assume ADMIN check xảy ra tại service layer (không phải Spring Security `@PreAuthorize`). Đây là architecture decision documented trong C2 (TDS §17). Nếu reviewer muốn dùng `@PreAuthorize` thay thế — cần viết ADR mới trước.
- **AP-AI-002 risk cho TC-015-005:** Spring Security filter sẽ PASS test này ngay cả với stub service (filter reject trước khi vào service). Đây là behavior mong muốn — không phải Green-from-Birth. Document trong Red Gate Evidence.

---

*TDD Spec v1.0 — UC15 DeactivateOwnAccount*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Document ID: CB-AUTH-IMP-015-TEST*
