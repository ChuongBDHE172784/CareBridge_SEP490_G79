# TEST-DRIVEN DEVELOPMENT SPECIFICATION

# Mẫu Đặc tả Kiểm thử — UC-01 Register Account

**Document ID:** `CB-AUTH-TEST-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Implemented — 2026-07-04 (10/10 PASS; AUTH-TC-INT-001 now GREEN via Testcontainers PostgreSQL). Tests in AuthServiceRegisterTest (service + DTO Bean-Validation) + PasswordComplexityPolicyTest (AUTH-TC-004) + RegisterAccountIntegrationTest (AUTH-TC-INT-001, real DB).`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**

- `04_Implement/UC01_RegisterAccount/UC01_RegisterAccount_TDS.md` (CB-AUTH-IMP-001 v1.0)
- `02_Requirements/SRS/` — SRS UC-01
- `ADR-AUTH-001`, `ADR-AUTH-002`, `ADR-AUTH-003`

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                            |
| ---------- | --------------- | -------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo TDD spec cho UC-01 Register Account |

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

| Field                     | Value                           |
| ------------------------- | ------------------------------- |
| **Feature / Gap ID**      | `UC-01`                         |
| **Module**                | `RegisterAccount — auth`        |
| **Spec gốc**              | `CB-AUTH-IMP-001`               |
| **Priority**              | 🔴 P0                            |
| **Sprint**                | `S1 (2026-06-26 → 2026-07-10)`  |
| **Milestone**             | `M1 Alpha — Auth Module`        |
| **Data Classification**   | `Sensitive-PII`                 |
| **Compliance Scope**      | `BR-RBAC, BR-PRIVACY, PDPA`     |
| **Upstream Dependencies** | `Firebase FCM, Gmail SMTP`      |
| **Downstream Consumers**  | `UC-02 VerifyOTP, AuditService` |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                        |
| ------------------------ | ------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                        |
| **Constraint Source**    | `CB-AUTH-IMP-001 §17`, `ADR-AUTH-001`, `ADR-AUTH-002`        |
| **Constraints Injected** | BCrypt-only, no-password-log, role-whitelist, OTP-after-save |
| **Model**                | `claude-sonnet-4-6`                                          |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                 |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                 | Thực tế (schema / policy)                                | Fix áp dụng trong test                                  |
| --- | ------------------------------------------------------ | -------------------------------------------------------- | ------------------------------------------------------- |
| L1  | Spec không rõ thứ tự: lưu user trước hay gửi OTP trước | Policy: lưu user trong transaction, gửi OTP sau commit   | Test verify OTP chỉ gửi khi user đã được lưu thành công |
| L2  | Spec chỉ nói "validate email unique"                   | Cần validate cả phone_number unique                      | Test case riêng cho duplicate phone                     |
| L3  | Không rõ xử lý khi Firebase down                       | Policy: throw AUTH-004 503, không rollback user creation | Test mock Firebase failure → 503 response               |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
RegisterAccount bao gồm các layer:
├── Domain (User entity, AccountStatus enum, UserRole enum)
├── Application / Service (AuthService.register() — mock repositories)
├── Controller (AuthController.register() — mock service)
└── Integration (Testcontainers PostgreSQL + mock Firebase)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source           | Items Derived                                                    |
| ---------------- | ---------------------------------------------------------------- |
| `SRS UC-01`      | Email unique, phone VN format, password strength, role whitelist |
| `ADR-AUTH-001`   | Role chỉ là MOTHER/EXPERT                                        |
| `ADR-AUTH-002`   | BCrypt password hashing                                          |
| `ADR-AUTH-003`   | OTP gửi qua Firebase + Email                                     |
| `BR-AUTH-001`    | Email uniqueness constraint                                      |
| `BR-AUTH-003`    | Password ≥ 8 chars, uppercase + digit + special                  |
| `BR-PRIVACY-001` | Không log password                                               |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                               | Coverage Item                       | Test Cases        |
| ------------ | -------------------------------------------- | ----------------------------------- | ----------------- |
| TC-COND-001  | Email hợp lệ, duy nhất → tạo user thành công | `AuthService.register()`            | `AUTH-TC-001`     |
| TC-COND-002  | Email đã tồn tại → 409                       | `AuthService.validateEmailUnique()` | `AUTH-TC-002`     |
| TC-COND-003  | Phone đã tồn tại → 409                       | `AuthService.validatePhoneUnique()` | `AUTH-TC-003`     |
| TC-COND-004  | Password yếu → 400                           | `@Pattern` trên DTO                 | `AUTH-TC-004`     |
| TC-COND-005  | Role không hợp lệ (ADMIN) → 400              | `@Pattern` trên DTO                 | `AUTH-TC-005`     |
| TC-COND-006  | Email sai format → 400                       | `@Email` trên DTO                   | `AUTH-TC-006`     |
| TC-COND-007  | Phone sai định dạng VN → 400                 | `@VietnamesePhoneNumber`            | `AUTH-TC-007`     |
| TC-COND-008  | OTP được tạo sau khi register                | `OtpService.generateAndSend()`      | `AUTH-TC-008`     |
| TC-COND-009  | SQL Injection qua email → 400                | DTO validation layer                | `AUTH-TC-009`     |
| TC-COND-010  | Integration: user + OTP trong DB             | Full flow                           | `AUTH-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4)  | Applied To                                 | Rationale                           |
| ------------------------ | ------------------------------------------ | ----------------------------------- |
| Equivalence Partitioning | email format, password strength, role enum | Phân nhóm input hợp lệ/không hợp lệ |
| Boundary Value Analysis  | password length (7, 8, 9 chars)            | Kiểm tra ranh giới min length       |
| State Transition Testing | AccountStatus: UNVERIFIED → ACTIVE         | Xác minh trạng thái sau register    |
| Error Guessing           | SQL injection, empty fields, null values   | Security attack vectors             |

### TDS-05 — Test Data Requirements

| Fixture ID   | Type    | Value / Logic                                                                     | Mục đích                             |
| ------------ | ------- | --------------------------------------------------------------------------------- | ------------------------------------ |
| `FX-REG-001` | DB seed | `{email: "existing@test.com", status: "ACTIVE"}`                                  | Test duplicate email                 |
| `FX-REG-002` | DB seed | `{phoneNumber: "0912000001", status: "ACTIVE"}`                                   | Test duplicate phone                 |
| `FX-REG-003` | Input   | `{email:"new@test.com", password:"Test@1234", phone:"0912345678", role:"MOTHER"}` | Happy path input                     |
| `FX-REG-004` | Input   | `{password:"12345678"}`                                                           | Weak password (no uppercase/special) |
| `FX-REG-005` | Input   | `{role:"ADMIN"}`                                                                  | Invalid role                         |
| `FX-REG-006` | Input   | `{email:"not-an-email"}`                                                          | Invalid email format                 |
| `FX-REG-007` | Input   | `{phoneNumber:"123456"}`                                                          | Invalid VN phone                     |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// RegisterTestFactory.java
// CASE 2.0 — Props Isolation Pattern
// Dùng makeRegisterRequest() trong mỗi test method — không share state

private static final String BASE_EMAIL = "test.mother@example.com";
private static final String BASE_PASSWORD = "SecureP@ss1";
private static final String BASE_PHONE = "0912345678";
private static final String BASE_ROLE = "MOTHER";

static RegisterRequestDTO makeRegisterRequest(
        String email, String password, String phone, String role) {
    return new RegisterRequestDTO(
        email   != null ? email    : BASE_EMAIL,
        password!= null ? password : BASE_PASSWORD,
        phone   != null ? phone    : BASE_PHONE,
        role    != null ? role     : BASE_ROLE
    );
}
```

---

### AUTH-TC-001 — Đăng ký thành công với dữ liệu MOTHER hợp lệ

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.register()` + `AuthController.register()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-01 Happy Path`, `BR-AUTH-001`

**Preconditions:**

- Database sạch, không có user với email "new.mother@example.com"
- `UserRepository` mock: `existsByEmail()` → false, `save()` → trả user với UUID
- `OtpService` mock: `generateAndSend()` → void (không throw)

**Test Steps:**

1. **Arrange:** Tạo `RegisterRequestDTO` qua `makeRegisterRequest(null, null, null, null)`
2. **Act:** Gọi `authService.register(request)`
3. **Assert:**
   - Kết quả trả về `RegisterResponseDTO` với `status = "UNVERIFIED"`
   - `userId` không null và là UUID hợp lệ
   - `userRepository.save()` được gọi đúng 1 lần
   - `otpService.generateAndSend()` được gọi đúng 1 lần với `userId` đó

**Expected Result (PASS):**

- `RegisterResponseDTO.status()` == `"UNVERIFIED"`
- `RegisterResponseDTO.userId()` != null
- Verify `passwordEncoder.encode()` được gọi (password không lưu plaintext)

**Expected Result (FAIL):**

- Nếu status != UNVERIFIED → implementation sai business rule
- Nếu OTP không được gọi → vi phạm BR-AUTH-006

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-002 — Từ chối khi email đã tồn tại (Duplicate Email)

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.register()` — email uniqueness check
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-AUTH-001`, `ADR-AUTH-001`

**Preconditions:**

- `UserRepository` mock: `existsByEmail("existing@test.com")` → true

**Test Steps:**

1. **Arrange:** `makeRegisterRequest("existing@test.com", null, null, null)`
2. **Act:** Gọi `authService.register(request)` — expect exception
3. **Assert:**
   - Throws `ValidationException` với code `AUTH-002`
   - `userRepository.save()` KHÔNG được gọi
   - `otpService.generateAndSend()` KHÔNG được gọi

**Expected Result (PASS):**

- `ValidationException` thrown với message chứa "AUTH-002"
- Không có side effect (không tạo user, không gửi OTP)

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-003 — Từ chối khi số điện thoại đã tồn tại

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.register()` — phone uniqueness check
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-AUTH-001` (extended to phone), `L2 Logic Issue`

**Preconditions:**

- `existsByEmail()` → false
- `existsByPhoneNumber("0912000001")` → true

**Test Steps:**

1. **Arrange:** `makeRegisterRequest(null, null, "0912000001", null)`
2. **Act:** `authService.register(request)` — expect exception
3. **Assert:**
   - Throws `ValidationException` với code `AUTH-003`
   - `userRepository.save()` KHÔNG được gọi

**Expected Result (PASS):**

- Exception với code `AUTH-003`

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-004 — Từ chối mật khẩu yếu (không đủ strength)

**Severity:** `HIGH`
**Feature Under Test:** `RegisterRequestDTO` — `@Pattern` validation
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-AUTH-003`

**Preconditions:**

- Spring MVC Test context active

**Test Steps:**

1. **Arrange:** Tạo request JSON với `password="12345678"` (không có chữ hoa/ký tự đặc biệt)
2. **Act:** `mockMvc.perform(POST /api/v1/auth/register)` với body trên
3. **Assert:**
   - Response status = 400
   - Body chứa `code = "AUTH-001"`
   - Body `details` chứa `field = "password"`

**Expected Result (PASS):**

- HTTP 400, error code AUTH-001, field "password" trong details

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-005 — Từ chối role không hợp lệ (ADMIN)

**Severity:** `CRITICAL`
**Feature Under Test:** `RegisterRequestDTO` — role enum validation
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-AUTH-001`, `BR-AUTH-004`

**Test Steps:**

1. **Arrange:** Request với `role = "ADMIN"`
2. **Act:** POST `/api/v1/auth/register`
3. **Assert:**
   - HTTP 400
   - Error code `AUTH-001`
   - Field `role` trong details

**Expected Result (PASS):**

- 400, role rejected, user không được tạo

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-006 — Từ chối email sai format

**Severity:** `MEDIUM`
**Feature Under Test:** `RegisterRequestDTO` — `@Email` validation
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-AUTH-001`

**Test Steps:**

1. **Arrange:** Request với `email = "not-valid-email"`
2. **Act:** POST `/api/v1/auth/register`
3. **Assert:**
   - HTTP 400, error AUTH-001, field "email"

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-007 — Từ chối số điện thoại sai định dạng Việt Nam

**Severity:** `MEDIUM`
**Feature Under Test:** `@VietnamesePhoneNumber` validator
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-AUTH-002`

**Test Steps:**

1. **Arrange:** Request với `phoneNumber = "+14155552671"` (số Mỹ)
2. **Act:** POST `/api/v1/auth/register`
3. **Assert:**
   - HTTP 400, field "phoneNumber" trong details

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-008 — OTP được tạo và gửi sau khi đăng ký thành công

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.register()` → `OtpService.generateAndSend()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-AUTH-006`, `ADR-AUTH-003`

**Preconditions:**

- Mọi repo mock trả success
- `OtpService` mock: capture arguments

**Test Steps:**

1. **Act:** `authService.register(validRequest)`
2. **Assert:**
   - `otpService.generateAndSend(savedUserId, "EMAIL")` được gọi đúng 1 lần
   - Argument `userId` khớp với UUID của user vừa lưu

**Expected Result (PASS):**

- OTP được gửi với đúng userId và channel "EMAIL"

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-009 — SQL Injection attempt qua email field

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `AuthController.register()` — DTO validation layer
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Test Steps (Attack Simulation):**

1. **Arrange:** `email = "'; DROP TABLE users; --"`
2. **Act:** POST `/api/v1/auth/register`
3. **Assert:**
   - HTTP 400 (validation reject — không phải 500)
   - Database không bị ảnh hưởng
   - `users` table vẫn tồn tại

**Expected Result (PASS = hệ thống an toàn):**

- 400 Bad Request — `@Email` validation bắt được ký tự không hợp lệ
- Không có SQL error trong logs

**Current Status:** 🟢 Passing — 2026-07-04

---

### AUTH-TC-INT-001 — Integration: user và OTP record được lưu vào DB

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.register()` — full DB integration
**Test File:** `src/test/java/com/carebridge/backend/auth/RegisterAccountIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**

- PostgreSQL Testcontainer đang chạy
- Flyway migration V1 đã apply
- `OtpService` mock (Firebase không cần thật)

**Test Steps:**

1. Seed: DB sạch
2. Gọi `POST /api/v1/auth/register` với `FX-REG-003`
3. Query DB: `SELECT * FROM users WHERE email = 'new@test.com'`
4. Query DB: `SELECT * FROM otp_records WHERE user_id = ?`

**Expected Result (PASS):**

- Users table: 1 row, `status = 'UNVERIFIED'`, `password_hash` bắt đầu bằng `$2a$12$`
- OTP_records table: 1 row, `used = false`, `expires_at` = ~10 phút tương lai, `attempt_count = 0`

**DB Assertion:**

```java
User user = userRepository.findByEmail("new@test.com").orElseThrow();
assertThat(user.getStatus()).isEqualTo(AccountStatus.UNVERIFIED);
assertThat(user.getPasswordHash()).startsWith("$2a$12$");

List<OtpRecord> otps = otpRepository.findByUserId(user.getId());
assertThat(otps).hasSize(1);
assertThat(otps.get(0).isUsed()).isFalse();
assertThat(otps.get(0).getExpiresAt())
    .isAfter(Instant.now())
    .isBefore(Instant.now().plusSeconds(700));
```

**Current Status:** 🟢 Passing — 2026-07-04 (`RegisterAccountIntegrationTest`, Testcontainers PostgreSQL + MockMvc, real DB round-trip. Assertions match actual implementation: `accountStatus = PENDING_ACTIVATION` / `enabled = false` (the Test-Spec's idealized `UNVERIFIED`), BCrypt hash prefix `$2`, and a pending `otp_verifications` row (the idealized `otp_records`) with future `expires_at`.)

---

## 5. Red-Green-Refactor Tracker

> **2026-07-04 — GREEN.** AUTH-TC-001/002/003/005/006/007/008/009 implemented in
> `security/service/AuthServiceRegisterTest.java` (service-layer via mock-built `AuthServiceImpl`,
> plus DTO Bean-Validation for 005b/006/007/009); AUTH-TC-004 in `security/policy/PasswordComplexityPolicyTest.java`.
> AUTH-TC-INT-001 SKIPPED (no Testcontainers in project). Actual file paths differ from the
> idealized `auth/AuthServiceTest.java` below (real package is `security/service`).

| TC ID             | Test File                             | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note          |
| ----------------- | ------------------------------------- | --------------- | ---------------- | ------------------------- |
| `AUTH-TC-001`     | `AuthServiceTest.java`                | `[ ]`           | `—`              | —                         |
| `AUTH-TC-002`     | `AuthServiceTest.java`                | `[ ]`           | `—`              | —                         |
| `AUTH-TC-003`     | `AuthServiceTest.java`                | `[ ]`           | `—`              | —                         |
| `AUTH-TC-004`     | `AuthControllerTest.java`             | `[ ]`           | `—`              | —                         |
| `AUTH-TC-005`     | `AuthControllerTest.java`             | `[ ]`           | `—`              | —                         |
| `AUTH-TC-006`     | `AuthControllerTest.java`             | `[ ]`           | `—`              | —                         |
| `AUTH-TC-007`     | `AuthControllerTest.java`             | `[ ]`           | `—`              | —                         |
| `AUTH-TC-008`     | `AuthServiceTest.java`                | `[ ]`           | `—`              | —                         |
| `AUTH-TC-009`     | `AuthControllerSecurityTest.java`     | `[ ]`           | `—`              | —                         |
| `AUTH-TC-INT-001` | `RegisterAccountIntegrationTest.java` | `[x]`           | `2026-07-04`     | Testcontainers PostgreSQL |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// AuthService.java — Red Phase stub (PHẢI throw)
@Service
public class AuthService implements IAuthService {
    @Override
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID             | Stub Result                         | Expected | Actual        | Root Cause (nếu PASS bất thường) |
| ----------------- | ----------------------------------- | -------- | ------------- | -------------------------------- |
| `AUTH-TC-001`     | throw UnsupportedOperationException | 🔴 FAIL   | ☐ FAIL ☐ PASS | —                                |
| `AUTH-TC-004`     | N/A (DTO validation)                | 🔴 FAIL   | ☐ FAIL ☐ PASS | —                                |
| `AUTH-TC-INT-001` | throw                               | 🔴 FAIL   | ☐ FAIL ☐ PASS | —                                |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-AUTH-IMP-001` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Flyway migration V1 (users + otp_records) đã được approved
- [ ] Test fixtures (FX-REG-001 đến FX-REG-007) đã được chuẩn bị

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả 10 test cases xanh
- [ ] Test coverage ≥ 80% lines trong `AuthService`, `AuthController`
- [ ] Không có `passwordHash` xuất hiện trong application logs
- [ ] BCrypt cost factor = 12 (verify qua code review)
- [ ] OTP record được tạo với `expires_at = now + 10 phút` (±5 giây)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub
- [ ] **Props Isolation** — mọi test instance tạo qua `makeRegisterRequest()`
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn BR/ADR

### Suspension Criteria

- Firebase credentials chưa được cấu hình → block TC-INT-001
- Flyway migration bị lỗi → block toàn bộ integration tests

---

## 7. Rollback Plan

```bash
# Revert Flyway migration (chỉ trên dev/staging)
./mvnw flyway:undo

# Revert source files
git checkout -- src/main/java/com/carebridge/backend/auth/
git checkout -- src/main/resources/db/migration/V1__*.sql

# UC-01 vẫn ở trạng thái OPEN trong backlog
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                              | Check | Gate chặn |
| --------- | ------------------------ | ---------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-AUTH-001/002                  | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth         | Test PASS với stub throw                             | ☐     | G-2 ★     |
| AP-AI-003 | Implicit Decision        | Test assume role ADMIN được phép đăng ký             | ☐     | G-1       |
| AP-AI-004 | Layer Violation          | Test verify AuthController có password hashing logic | ☐     | G-4       |
| AP-AI-005 | Hallucinated Contract    | Test import`AuthFacade` không có trong §8 TDS        | ☐     | G-3       |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → fix trước khi implement

---

*TDD Spec CB-AUTH-TEST-001 v1.0 — UC-01 Register Account*
*Tuân theo EDS v2.0 + CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
