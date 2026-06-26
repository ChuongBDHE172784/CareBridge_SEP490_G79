# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Đặc tả Kiểm thử Hướng Phát triển — UC-07 Change Password

**Document ID:** `CB-AUTH-IMP-007-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC07_ChangePassword/UC07_ChangePassword_TDS.md` (`CB-AUTH-IMP-007`)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.1.1.7
- `ADR-AUTH-031` đến `ADR-AUTH-036`

> **Quy ước TDD:** Viết test → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `.\mvnw.cmd test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                    |
| ---------- | --------------- | ---------------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo TDD spec cho UC-07 ChangePassword           |

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

| Field                     | Value                                                          |
| ------------------------- | -------------------------------------------------------------- |
| **Feature / Gap ID**      | `UC-07`                                                        |
| **Module**                | `ChangePassword — Bounded Context: auth`                       |
| **Spec gốc**              | `CB-AUTH-IMP-007`                                              |
| **Priority**              | 🟠 P1                                                          |
| **Sprint**                | `S1 (2026-06-26 → 2026-07-11)`                                 |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                        |
| **Data Classification**   | `Sensitive-PII`                                                |
| **Compliance Scope**      | `BR-RBAC, BR-SECURITY, PDPA`                                   |
| **Upstream Dependencies** | `UC-03 Login`, `PasswordComplexityPolicy`, `BCryptPasswordEncoder` |
| **Downstream Consumers**  | `AuditService`, `RefreshTokenRepository`, `SessionService`     |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                   |
| ------------------------ | ------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                   |
| **Constraint Source**    | `CB-AUTH-IMP-007 §17`, `ADR-AUTH-031 đến ADR-AUTH-036` |
| **Constraints Injected** | C1 (verify oldPassword), C3 (revoke sessions), C6 (@Transactional) |
| **Model**                | `Claude Sonnet 4.6`                                     |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                            |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                                         | Thực tế (schema / policy)                                      | Fix áp dụng trong test                                 |
| - | --------------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------ |
| L1 | `ChangePasswordRequest.java` không có `confirmPassword`        | ADR-AUTH-032: cần confirmPassword để UX validation            | Test phải include confirmPassword field                |
| L2 | SRS không specify session scope khi revoke                     | ADR-AUTH-035: revoke ALL refresh tokens, kể cả current        | Integration test: assert 0 active sessions sau change  |
| L3 | Không rõ có cần revoke current JWT access token không          | ADR-AUTH-035: AT là short-lived (15min), không revoke (no blacklist for AT) | Test verify AT còn dùng được tối đa 15min |
| L4 | SRS không mention audit event                                   | BR-AUTH-036: emit PASSWORD_CHANGED                            | Integration test verify audit_logs entry               |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ChangePassword module bao gồm các layer:
├── Domain (PasswordComplexityPolicy — pure logic, unit-tested trong UC-01)
├── Service (AuthServiceImpl.changePassword() — mock repositories)
├── Controller (AuthController.changePassword() — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL + Spring context)

Out of scope:
├── PasswordComplexityPolicy internals (tested trong UC-01/UC-06)
├── BCryptPasswordEncoder internals (Spring Security tested)
└── RefreshTokenRepository.revokeAll internals (tested trong UC-04/UC-06)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source               | Items Derived                                                  |
| -------------------- | -------------------------------------------------------------- |
| `SRS UC-07 §3.1.1.7` | Change password flow: verify old → check complexity → update  |
| `ADR-AUTH-031`       | oldPassword BCrypt verification required                       |
| `ADR-AUTH-032`       | PasswordComplexityPolicy must be called                        |
| `ADR-AUTH-033`       | newPassword != oldPassword invariant                           |
| `ADR-AUTH-035`       | All refresh tokens revoked after password change              |
| `BR-AUTH-036`        | PASSWORD_CHANGED audit event required                          |
| `CB-AUTH-IMP-007 §10` | Error codes AUTH-071 to AUTH-075                             |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID  | Test Condition                            | Coverage Item                    | Test Cases          |
| ------------- | ----------------------------------------- | -------------------------------- | ------------------- |
| TC-COND-07-01 | oldPassword đúng → đổi thành công         | `AuthServiceImpl.changePassword()` | AUTH-TC-007-001   |
| TC-COND-07-02 | oldPassword sai → AUTH-071               | `BCrypt.matches() == false`       | AUTH-TC-007-002   |
| TC-COND-07-03 | confirmPassword không khớp → AUTH-072    | `@AssertTrue isPasswordMatch()`   | AUTH-TC-007-003   |
| TC-COND-07-04 | newPassword yếu → AUTH-073               | `PasswordComplexityPolicy.validate()` | AUTH-TC-007-004 |
| TC-COND-07-05 | newPassword = oldPassword → AUTH-074     | Same password reuse check         | AUTH-TC-007-005   |
| TC-COND-07-06 | Không có JWT → 401 IAM-001               | Spring Security filter            | AUTH-TC-007-006   |
| TC-COND-07-07 | Refresh tokens bị revoke sau change      | `RefreshTokenRepository`          | AUTH-TC-007-INT-001 |
| TC-COND-07-08 | Audit event được emit                    | `AuditService.emit()`             | AUTH-TC-007-INT-002 |

### TDS-04 — Test Techniques

| Technique                 | Applied To                          | Rationale                              |
| ------------------------- | ----------------------------------- | -------------------------------------- |
| Equivalence Partitioning  | oldPassword (đúng/sai)              | Hai partition rõ ràng                  |
| Boundary Value Analysis   | newPassword length (7, 8, 100, 101) | Min/Max constraints                    |
| Error Guessing            | Same password reuse                 | Common security anti-pattern           |
| State Transition Testing  | Session state trước/sau change      | Verify revocation state                |

### TDS-05 — Test Data Requirements

| Fixture ID | Type   | Value / Logic                                     | Mục đích             |
| ---------- | ------ | ------------------------------------------------- | -------------------- |
| `FX-07-01` | DB seed | User ACTIVE, passwordHash = BCrypt("OldPass@123") | Happy path baseline  |
| `FX-07-02` | DB seed | User ACTIVE + 2 active refresh tokens             | Session revoke test  |
| `FX-07-03` | JWT    | Valid JWT for FX-07-01 user                       | Auth context         |
| `FX-07-04` | JWT    | Expired JWT                                       | Unauthorized test    |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ChangePasswordTestFactory.java
class ChangePasswordTestFactory {

    static User makeUser() {
        return User.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000007"))
            .phone("+84901234507")
            .email("test07@carebridge.vn")
            .passwordHash(new BCryptPasswordEncoder(12).encode("OldPass@123"))
            .name("Test User 07")
            .accountStatus("ACTIVE")
            .enabled(true)
            .locked(false)
            .role(Role.MOTHER)
            .build();
    }

    static ChangePasswordRequest makeRequest() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("OldPass@123");
        req.setNewPassword("NewPass@456");
        req.setConfirmPassword("NewPass@456");
        return req;
    }

    static ChangePasswordRequest makeRequest(Consumer<ChangePasswordRequest> overrides) {
        ChangePasswordRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }
}
```

---

### AUTH-TC-007-001 — Happy path: đổi mật khẩu hợp lệ

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl.changePassword(UUID, ChangePasswordRequest)`
**Test File:** `src/test/java/com/carebridge/backend/security/service/AuthServiceImplChangePasswordTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-07-01`
**Oracle Source:** `ADR-AUTH-031 §Decision`, `BR-AUTH-036`

**Preconditions:**
- User tồn tại, ACTIVE, không bị locked
- BCrypt hash của "OldPass@123" lưu trong DB

**Test Steps:**
1. Arrange: mock UserRepository trả về FX-07-01 User; mock PasswordComplexityPolicy pass; mock RefreshTokenRepository; mock AuditService
2. Act: `authService.changePassword(userId, makeRequest())`
3. Assert:
   - Không có exception
   - `userRepository.save()` được gọi 1 lần với passwordHash mới (khác ban đầu)
   - `refreshTokenRepository.revokeAllByUserId()` được gọi 1 lần
   - `auditService.emit()` được gọi với action `PASSWORD_CHANGED`

**Expected Result (PASS):** Method hoàn thành không exception, repo calls đúng
**Expected Result (FAIL):** Exception thrown hoặc repo calls thiếu
**Current Status:** 🔴 Not written

---

### AUTH-TC-007-002 — oldPassword sai → AUTH-071

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl.changePassword()`
**Test File:** `...AuthServiceImplChangePasswordTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-07-02`
**Oracle Source:** `ADR-AUTH-031 §Decision`, Error code `AUTH-071`

**Preconditions:**
- User tồn tại với passwordHash("OldPass@123")

**Test Steps:**
1. Arrange: mock UserRepository trả về user; request.oldPassword = "WrongPass"
2. Act: `authService.changePassword(userId, makeRequest(r -> r.setOldPassword("WrongPass")))`
3. Assert: throws `ValidationException` với code "AUTH-071"

**Expected Result (PASS):** `ValidationException(AUTH-071)` thrown
**Expected Result (FAIL):** Không throw hoặc throw exception khác
**Current Status:** 🔴 Not written

---

### AUTH-TC-007-003 — confirmPassword không khớp → 400 (Bean Validation)

**Severity:** `MEDIUM`
**Feature Under Test:** `ChangePasswordRequest.isPasswordMatch()` + `AuthController`
**Test File:** `src/test/java/com/carebridge/backend/security/controller/AuthControllerChangePasswordTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-07-03`
**Oracle Source:** `BR-AUTH-034`

**Test Steps:**
1. Arrange: `@WebMvcTest` với mock AuthService; request với newPassword="A", confirmPassword="B"
2. Act: `MockMvc.perform(PUT("/api/v1/auth/change-password").content(...))`
3. Assert: response status 400, body chứa field error cho `isPasswordMatch`

**Expected Result (PASS):** HTTP 400 với validation error
**Current Status:** 🔴 Not written

---

### AUTH-TC-007-004 — Mật khẩu mới yếu → AUTH-073

**Severity:** `HIGH`
**Feature Under Test:** `PasswordComplexityPolicy` + `AuthServiceImpl`
**Test File:** `...AuthServiceImplChangePasswordTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-07-04`
**Oracle Source:** `ADR-AUTH-032`, Error code `AUTH-073`

**Test Steps:**
1. Arrange: oldPassword đúng; `PasswordComplexityPolicy.validate("weak")` throws `ValidationException(AUTH-073)`
2. Act: `authService.changePassword(userId, makeRequest(r -> r.setNewPassword("weak")))`
3. Assert: throws `ValidationException` với code "AUTH-073"

**Expected Result (PASS):** `ValidationException(AUTH-073)` thrown
**Current Status:** 🔴 Not written

---

### AUTH-TC-007-005 — Mật khẩu mới trùng cũ → AUTH-074

**Severity:** `HIGH`
**Feature Under Test:** `AuthServiceImpl` same-password reuse check
**Test File:** `...AuthServiceImplChangePasswordTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-07-05`
**Oracle Source:** `ADR-AUTH-033`, Error code `AUTH-074`

**Test Steps:**
1. Arrange: oldPassword đúng; request với newPassword = "OldPass@123" (trùng)
2. Act: `authService.changePassword(userId, makeRequest(r -> r.setNewPassword("OldPass@123")))`
3. Assert: throws `ValidationException` với code "AUTH-074"

**Expected Result (PASS):** `ValidationException(AUTH-074)` thrown
**Current Status:** 🔴 Not written

---

### AUTH-TC-007-006 — Không có JWT → 401 (Security Test)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `...AuthControllerChangePasswordTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-07-06`
**Oracle Source:** `BR-RBAC`

**Test Steps:**
1. Arrange: `@WebMvcTest`, không gửi Authorization header
2. Act: `MockMvc.perform(PUT("/api/v1/auth/change-password").content(...))`
3. Assert: response status 401

**Expected Result (PASS):** HTTP 401 — Security filter rejects unauthenticated request
**Expected Result (FAIL):** HTTP 200 hoặc 400 — endpoint accessible without auth
**Current Status:** 🔴 Not written

---

### AUTH-TC-007-INT-001 — Integration: Sessions bị revoke sau đổi mật khẩu

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller → Service → DB (revokeAllByUserId)`
**Test File:** `src/test/java/com/carebridge/backend/security/ChangePasswordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-07-07`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied
- Seed: 1 user ACTIVE + 2 active refresh tokens

**Test Steps:**
1. Seed user và 2 refresh tokens qua JPA
2. Call `PUT /api/v1/auth/change-password` với valid JWT và correct credentials
3. Assert response 200
4. Assert DB: `SELECT COUNT(*) FROM refresh_tokens WHERE user_id=? AND revoked_at IS NULL` = 0

**Expected Result (PASS):**
- HTTP 200
- 0 active refresh tokens sau change

**DB Assertion:**
```java
int activeTokens = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM refresh_tokens WHERE user_id=? AND revoked_at IS NULL",
    Integer.class, userId);
assertThat(activeTokens).isEqualTo(0);
```

**Current Status:** 🔴 Not written

---

### AUTH-TC-007-INT-002 — Integration: Audit log được ghi

**Severity:** `HIGH`
**Feature Under Test:** `AuditService.emit(PASSWORD_CHANGED, ...)`
**Test File:** `...ChangePasswordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-07-08`

**Test Steps:**
1. Seed user ACTIVE
2. Call `PUT /api/v1/auth/change-password` thành công
3. Assert DB: `audit_logs` chứa row với `action = 'SECURITY_EVENT'` và `actor_user_id = userId`

**Expected Result (PASS):** Audit log entry tồn tại với đúng fields
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID                  | Test File                                              | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ---------------------- | ------------------------------------------------------ | ---------------- | ----------------- | ---------------- |
| `AUTH-TC-007-001`      | `AuthServiceImplChangePasswordTest.java`               | `[ ]`            | —                 | —                |
| `AUTH-TC-007-002`      | `AuthServiceImplChangePasswordTest.java`               | `[ ]`            | —                 | —                |
| `AUTH-TC-007-003`      | `AuthControllerChangePasswordTest.java`                | `[ ]`            | —                 | —                |
| `AUTH-TC-007-004`      | `AuthServiceImplChangePasswordTest.java`               | `[ ]`            | —                 | —                |
| `AUTH-TC-007-005`      | `AuthServiceImplChangePasswordTest.java`               | `[ ]`            | —                 | —                |
| `AUTH-TC-007-006`      | `AuthControllerChangePasswordTest.java`                | `[ ]`            | —                 | —                |
| `AUTH-TC-007-INT-001`  | `ChangePasswordIntegrationTest.java`                   | `[ ]`            | —                 | —                |
| `AUTH-TC-007-INT-002`  | `ChangePasswordIntegrationTest.java`                   | `[ ]`            | —                 | —                |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// AuthServiceImpl.java — Red Phase stub
@Override
public void changePassword(UUID userId, ChangePasswordRequest request) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID               | Stub Result       | Expected  | Actual          |
| ------------------- | ----------------- | --------- | --------------- |
| `AUTH-TC-007-001`   | throw('Not impl') | 🔴 FAIL   | ☐ FAIL ☐ PASS  |
| `AUTH-TC-007-002`   | throw('Not impl') | 🔴 FAIL   | ☐ FAIL ☐ PASS  |
| `AUTH-TC-007-006`   | N/A (Security)    | 🔴 FAIL   | ☐ FAIL ☐ PASS  |
| `AUTH-TC-007-INT-001` | throw('Not impl') | 🔴 FAIL | ☐ FAIL ☐ PASS  |

**Red Gate Evidence:** Stub commit hash: `___` | Tất cả FAIL? ☐ Yes → GATE-2 PASS

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-AUTH-IMP-007` đã được review
- [ ] `PasswordComplexityPolicy` đang hoạt động (verified từ UC-01/UC-06)
- [ ] `RefreshTokenRepository.revokeAllByUserId()` đã được implement (từ UC-06)
- [ ] Test fixtures (FX-07-01 đến FX-07-04) đã chuẩn bị

### Exit Criteria (DoD)

- [ ] `.\mvnw.cmd test` — tất cả unit tests xanh
- [ ] `.\mvnw.cmd verify` — integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `AuthServiceImpl.changePassword()`
- [ ] Không có business logic trong `AuthController.changePassword()`
- [ ] Không có plaintext password trong logs
- [ ] Audit log `PASSWORD_CHANGED` được ghi đúng format

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate** — tất cả tests FAIL với empty/throw stub
- [ ] **Contract Existence** — `./mvnw.cmd compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mỗi @Test dùng `ChangePasswordTestFactory.makeRequest()`

### Suspension Criteria

- Blocker: `PasswordComplexityPolicy` hoặc `RefreshTokenRepository` chưa implement
- CI pipeline broken bởi thay đổi khác trong auth module

---

## 7. Rollback Plan

```bash
# Revert implementation (không có migration)
git checkout -- src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/security/controller/AuthController.java
git checkout -- src/main/java/com/carebridge/backend/security/dto/request/ChangePasswordRequest.java
git checkout -- src/test/java/com/carebridge/backend/security/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern           | Dấu hiệu trong TDD spec                          | Check | Gate chặn |
| --------- | ---------------------- | ------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Gen      | TC không reference ADR/TDS constraint             | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth       | Test PASS với throw stub (§5.1)                   | ☐     | G-2 ★     |
| AP-AI-003 | Implicit Decision      | Test assume BCrypt không cần verify oldPassword   | ☐     | G-1       |
| AP-AI-004 | Layer Violation        | Service test verify controller logic              | ☐     | G-4       |
| AP-AI-005 | Hallucinated Contract  | Test import class không tồn tại                   | ☐     | G-3       |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern → TDD spec approved
- [ ] Phát hiện AP → fix trước khi implement

---

*TDD Template v2.0 — UC-07 Change Password*
*Status: Draft — chờ Tech Lead review.*
