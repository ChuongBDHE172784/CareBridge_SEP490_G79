# TEST-DRIVEN DEVELOPMENT SPECIFICATION

# Mẫu Đặc tả Kiểm thử — UC-05 Forgot Password

**Document ID:** `CB-AUTH-TEST-005`
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

- `04_Implement/UC05_ForgotPassword/UC05_ForgotPassword_TDS.md` (CB-AUTH-IMP-005 v1.0)
- `ADR-AUTH-011`, `ADR-AUTH-012`, `ADR-AUTH-013`, `ADR-AUTH-014`, `ADR-AUTH-015`, `ADR-AUTH-016`
- `BR-AUTH-011` đến `BR-AUTH-017`

---

## CHANGELOG

| Ngày      | Người thực hiện | Nội dung thay đổi                          |
| ---------- | ------------------- | --------------------------------------------- |
| 2026-06-26 | AI Agent            | Khởi tạo TDD spec cho UC-05 Forgot Password |

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

| Field                           | Value                                        |
| ------------------------------- | -------------------------------------------- |
| **Feature / Gap ID**      | `UC-05`                                    |
| **Module**                | `ForgotPassword — auth`                   |
| **Spec gốc**             | `CB-AUTH-IMP-005`                          |
| **Priority**              | 🔴 P0                                        |
| **Sprint**                | `S1 (2026-06-26 → 2026-07-10)`            |
| **Milestone**             | `M1 Alpha — Auth Module`                  |
| **Data Classification**   | `Sensitive-PII`                            |
| **Compliance Scope**      | `BR-RBAC, BR-SECURITY, PDPA`               |
| **Upstream Dependencies** | `User service (findByEmailOrPhone)`        |
| **Downstream Consumers**  | `UC-06 ResetPassword, Audit, Notification` |

### 1.1 AI Generation Context (CASE 2.0)

| Field                          | Value                                                                                                                    |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                  |
| **Constraint Source**    | `CB-AUTH-IMP-005 §17`, `ADR-AUTH-011` — `ADR-AUTH-016`                                                           |
| **Constraints Injected** | anti-enumeration, sha256-token-hash, constant-time-compare, rate-limit-user+ip, notification-fallback, pii-log-avoidance |
| **Model**                | `claude-sonnet-4-6`                                                                                                    |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                          |

---

## 2. Logic Issues Resolved

| #  | Spec gốc (sai / thiếu)                                             | Thực tế (schema / policy)                                                                        | Fix áp dụng trong test                                                |
| -- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| L1 | Spec nói "generate token" nhưng không rõ lưu plaintext hay hash | ADR-AUTH-012: lưu SHA-256 hash, không bao giờ lưu plaintext                                    | Test assert DB contains 64-char hex, không phải UUID                  |
| L2 | Anti-enumeration chưa rõ khi nào response được trả về        | Policy: response 200 được trả về NGAY cả user không tồn tại, trước khi token generation | Test verify message giống nhau cho user tồn tại và không tồn tại |
| L3 | Rate limit chưa rõ thứ tự check                                  | ADR-AUTH-014: check rate limit TRƯỚC user lookup (chống timing enumeration)                     | Test rate limit reject trước khi UserRepository được gọi          |
| L4 | SMS fallback chưa có chi tiết                                     | ADR-AUTH-015: email primary, SMS fallback nếu email gửi fail và phone verified                  | Test email failure → SMS được gọi, verify fallback logic           |
| L5 | Token TTL chưa rõ là bao lâu                                     | ADR-AUTH-012: TTL 15 phút (900s)                                                                  | Test expiresAt = now()+15min                                            |
| L6 | Token consumption chưa rõ có xóa hay mark used                   | ADR-AUTH-012: append-only, mark usedAt, không xóa                                                | Test verify token exists với usedAt set, không bị xóa               |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```PlainText
ForgotPassword bao gồm các layer:
├── Domain (pure logic — token generation, constant-time compare)
├── Application / Use Cases (ForgotPasswordService — mock repos inline)
├── Services (rate limit, notification dispatch — mock external services)
├── Controller (HTTP layer — mock service)
└── Integration (Testcontainers PostgreSQL + Redis)
```

### TDS-02 — Test Basis

| Source           | Items Derived                             |
| ---------------- | ----------------------------------------- |
| `SRS UC-05`    | Forgot password flow, anti-enumeration    |
| `ADR-AUTH-011` | User lookup without exposing existence    |
| `ADR-AUTH-012` | Token storage: SHA-256 hash, TTL 15min    |
| `ADR-AUTH-013` | Always return HTTP 200 generic message    |
| `ADR-AUTH-014` | Rate limits: user 3/h, IP 10/h            |
| `ADR-AUTH-015` | Notification: email primary, SMS fallback |
| `ADR-AUTH-016` | Audit event: PasswordResetRequested       |
| `BR-AUTH-011`  | Validate user exists & ACTIVE             |
| `BR-AUTH-012`  | Generate secure reset token               |
| `BR-AUTH-013`  | Store token as SHA-256 hash               |
| `BR-AUTH-014`  | Anti-enumeration response                 |
| `BR-AUTH-015`  | Rate limit enforcement                    |
| `BR-AUTH-016`  | Notification dispatch logic               |
| `BR-AUTH-017`  | Audit event logging                       |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                       | Coverage Item                                           | Test Cases            |
| ------------ | ---------------------------------------------------- | ------------------------------------------------------- | --------------------- |
| TC-COND-001  | Valid email → token generated + email sent          | `ForgotPasswordService.forgotPassword()`              | `FORGOT-TC-001`     |
| TC-COND-002  | Valid phone → token generated + SMS sent            | `ForgotPasswordService.forgotPassword()`              | `FORGOT-TC-002`     |
| TC-COND-003  | User not found → generic 200, no token              | `ForgotPasswordService.forgotPassword()`              | `FORGOT-TC-003`     |
| TC-COND-004  | User exists nhưng INACTIVE → generic 200, no token | `ForgotPasswordService.forgotPassword()`              | `FORGOT-TC-004`     |
| TC-COND-005  | Rate limit exceeded (user) → 429 AUTH-040           | `RateLimitService.checkUserLimit()`                   | `FORGOT-TC-005`     |
| TC-COND-006  | Rate limit exceeded (IP) → 429 AUTH-040             | `RateLimitService.checkIpLimit()`                     | `FORGOT-TC-006`     |
| TC-COND-007  | Invalid contact format → 400 AUTH-041               | `ForgotPasswordController.validateContact()`          | `FORGOT-TC-007`     |
| TC-COND-008  | Token hash stored in DB (64-char hex)                | `PasswordResetTokenRepository.saveHash()`             | `FORGOT-TC-008`     |
| TC-COND-009  | Constant-time token compare                          | `PasswordResetTokenRepository.findByHashAndNotUsed()` | `FORGOT-TC-SEC-001` |
| TC-COND-010a | emailVerified=false → SMS trực tiếp (email KHÔNG gọi)  | `NotificationService.dispatchPasswordReset()`         | `FORGOT-TC-009a`    |
| TC-COND-010b | emailVerified=true, email fail → SMS fallback           | `NotificationService.dispatchPasswordReset()`         | `FORGOT-TC-009b`    |
| TC-COND-011  | Audit event PASSWORD_RESET_REQUESTED published       | `AuditService.emit()`                                 | `FORGOT-TC-010`     |
| TC-COND-012  | Token expires sau 15 phút → invalid                | `ForgotPasswordService.validateToken()`               | `FORGOT-TC-011`     |
| TC-COND-013  | Token consume → mark usedAt, không xóa row        | `ForgotPasswordService.consumeToken()`                | `FORGOT-TC-012`     |
| TC-COND-014  | validateToken() với token đã used → reject       | `PasswordResetTokenRepository.findByHashAndNotUsed()` | `FORGOT-TC-013`     |
| TC-COND-015  | Full integration: forgot → validate → consume      | Full flow                                               | `FORGOT-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4)  | Applied To                                 | Rationale                        |
| ------------------------ | ------------------------------------------ | -------------------------------- |
| State Transition Testing | Token state: PENDING → CONSUMED / EXPIRED | Verify token lifecycle           |
| Boundary Value Analysis  | Rate limit: 3rd ok, 4th reject (user)      | Boundary of allowed requests     |
| Error Guessing           | Timing attack on token compare             | Security: constant-time required |
| Equivalence Partitioning | email vs phone contact                     | Both channels should work        |
| Negative Testing         | Invalid email format, expired token        | Error handling                   |

### TDS-05 — Test Data Requirements

| Fixture ID    | Type      | Value / Logic                                                                               | Mục đích                   |
| ------------- | --------- | ------------------------------------------------------------------------------------------- | ----------------------------- |
| `FX-FP-001` | DB seed   | `User{id:u1, email:"test@example.com", emailVerified:true, status:ACTIVE}`                | Happy path email              |
| `FX-FP-002` | DB seed   | `User{id:u2, phone:"+84912345678", phoneVerified:true, status:ACTIVE}`                    | Happy path SMS                |
| `FX-FP-003` | DB seed   | `User{id:u3, email:"inactive@example.com", status:INACTIVE}`                              | INACTIVE user                 |
| `FX-FP-004` | DB seed   | `User{id:u4, email:"unverified@example.com", emailVerified:false, phoneVerified:false}`   | No verified contact           |
| `FX-FP-005` | DB seed   | `PasswordResetToken{userId:u1, hash:SHA256("token-1"), expiresAt:now+15min, usedAt:null}` | Valid token                   |
| `FX-FP-006` | DB seed   | `PasswordResetToken{userId:u2, hash:SHA256("expired"), expiresAt:now-1min, usedAt:null}`  | Expired token                 |
| `FX-FP-007` | DB seed   | `PasswordResetToken{userId:u3, plaintext:"used-token-uuid", hash:SHA256("used-token-uuid"), expiresAt:now+15min, usedAt:now}` — giữ plaintext trong biến test để dùng trong TC-013 | Already used token |
| `FX-FP-008` | Clock     | `fixed clock: 2026-06-26T10:00:00Z`                                                       | TTL verification              |
| `FX-FP-009` | RateLimit | `userCount=3 (within limit)`, `userCount=4 (exceeded)`                                  | Boundary test                 |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ForgotPasswordTestFactory.java
private static User makeUser(UUID id, String email, String phone, 
                             boolean emailVerified, boolean phoneVerified, 
                             AccountStatus status) {
    User user = new User();
    user.setId(id);
    user.setEmail(email);
    user.setPhone(phone);
    user.setEmailVerified(emailVerified);
    user.setPhoneVerified(phoneVerified);
    user.setStatus(status);
    return user;
}

private static ForgotPasswordRequestDTO makeForgotRequest(String contact) {
    ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
    dto.setContact(contact);
    dto.setIpAddress("127.0.0.1");
    dto.setUserAgent("Test-Agent/1.0");
    return dto;
}

private static PasswordResetToken makeToken(UUID userId, String plaintextToken, 
                                            boolean used, LocalDateTime expiresAt) {
    PasswordResetToken token = new PasswordResetToken();
    token.setId(UUID.randomUUID());
    token.setUserId(userId);
    token.setTokenHash(DigestUtils.sha256Hex(plaintextToken));
    token.setExpiresAt(expiresAt);
    token.setUsedAt(used ? LocalDateTime.now() : null);
    token.setCreatedAt(LocalDateTime.now());
    return token;
}
```

### FORGOT-TC-001 — ForgotPassword: valid email → token generated + email sent

**Severity:** `CRITICAL`
**CWE:** `CWE-640 — Weak Password Recovery Mechanism`
**Legal:** `PDPA Art. 32, BR-SECURITY`
**Feature Under Test:** `ForgotPasswordService.forgotPassword()`
**Test File:** `src/test/java/com/carebridge/backend/auth/ForgotPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-05 Happy Path`, `ADR-AUTH-012`, `ADR-AUTH-015`

**Preconditions:**

* `FX-FP-001`: User ACTIVE với email verified
* `RateLimitService` mock: `checkUserLimit()` → true, `checkIpLimit()` → true
* `EmailService` mock: ready to capture calls
* `AuditService` mock: capture published events

**Test Steps:**

1. **Arrange:** `makeForgotRequest("test@example.com")`
2. **Act:** `forgotPasswordService.forgotPassword(request)`
3. **Assert:**
   * Response status 200
   * `response.getMessage()` contains `"If the account exists"`
   * `response.getExpiresIn()` == 900
   * `tokenRepository.saveHash()` được gọi 1 lần với:
     * `userId = FX-FP-001.id`
     * `tokenHash` là 64-char hex string (pattern: `[0-9a-f]{64}`)
     * `expiresAt` ≈ `now() + 15 minutes` (± 1 second)
   * `emailService.sendPasswordResetEmail()` được gọi 1 lần với:
     * `user = FX-FP-001`
     * `token` là UUID string (không phải hash)
   * `auditService.emit()` được gọi với event `PasswordResetRequestedEvent{userId=FX-FP-001.id}`

**Expected Result (PASS):**

* Token hash lưu DB, email gửi thành công, audit event published, generic message trả về

**Expected Result (FAIL):**

* Token hash lưu DB nhưng email không gửi → forgotPassword ném exception → 500
* Response message khác nhau tùy user exists → vi phạm ADR-AUTH-013
* Token lưu DB dưới dạng plaintext UUID → vi phạm ADR-AUTH-012

**Current Status:** 🔴 Not written
**Implementation Note:**

* Mock `UserRepository.findByEmailOrPhone()` để return `FX-FP-001`
* Không forget gọi `rateLimitService` TRƯỚC `userRepository` — test verify order bằng Mockito `InOrder`
* Use `DigestUtils.sha256Hex()` để compute hash trong test fixture, so sánh với value lưu DB

---

### FORGOT-TC-002 — ForgotPassword: valid phone → token generated + SMS sent

**Severity:** `HIGH`
**CWE:** `CWE-640`
**Legal:** `PDPA Art. 32`
**Feature Under Test:** `ForgotPasswordService.forgotPassword()`
**Test File:** `src/test/java/com/carebridge/backend/auth/ForgotPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-AUTH-015`

**Preconditions:**

* `FX-FP-002`: User ACTIVE với `phoneVerified=true`, `emailVerified=false`
* All mocks ready

**Test Steps:**

1. **Arrange:** `makeForgotRequest("+84912345678")` (E.164 format)
2. **Act:** `forgotPasswordService.forgotPassword(request)`
3. **Assert:**
   * Response 200, generic message
   * `tokenRepository.saveHash()` called
   * `smsService.sendSms()` được gọi 1 lần (not `emailService`)
   * `auditService.emit()` với `contactMethod="sms"`

**Expected Result (PASS):**

* SMS sent, email not called, audit event correct

**Expected Result (FAIL):**

* Both email và SMS gửi → không tuân thủ fallback logic
* No SMS → fallback not implemented

---

### FORGOT-TC-003 — ForgotPassword: user not found (anti-enumeration)

**Severity:** `CRITICAL`
**CWE:** `CWE-204 — Observable Response Discrepancy`
**Legal:** `PDPA Art. 32`
**Feature Under Test:** `ForgotPasswordService.forgotPassword()`
**Test File:** `src/test/java/com/carebridge/backend/auth/ForgotPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-AUTH-013`

**Preconditions:**

* `UserRepository.findByEmailOrPhone("unknown@example.com")` → `null`
* All mocks ready

**Test Steps:**

1. **Arrange:** `makeForgotRequest("unknown@example.com")`
2. **Act:** `forgotPasswordService.forgotPassword(request)`
3. **Assert:**
   * Response 200 (not 404)
   * `response.getMessage()` == EXACTLY same string as `FORGOT-TC-001`
   * `tokenRepository.saveHash()` NOT called (0 times)
   * `emailService.sendPasswordResetEmail()` NOT called
   * `auditService.emit()` được gọi với `userId=null`

**Expected Result (PASS):**

* Generic message, no token saved, no notification sent, audit logged with null userId

**Expected Result (FAIL):**

* Message khác với TC-001 (ví dụ: "User not found" hoặc "Email not sent") → anti-enumeration fail
* `tokenRepository.saveHash()` được gọi → attacker có thể enumerate bằng timing/DB side-channel

---

### FORGOT-TC-004 — ForgotPassword: user INACTIVE → same generic response

**Severity:** `HIGH`
**Feature Under Test:** `ForgotPasswordService.forgotPassword()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-AUTH-011` (validate ACTIVE only), `ADR-AUTH-013` (generic response)

**Preconditions:**

* `FX-FP-003`: User với `status=INACTIVE`
* All mocks ready

**Test Steps:**

1. **Arrange:** `makeForgotRequest("inactive@example.com")`
2. **Act:** `forgotPasswordService.forgotPassword(request)`
3. **Assert:**
   * Response 200, same generic message
   * `tokenRepository.saveHash()` NOT called
   * `auditService.emit()` với `userId=FX-FP-003.id` (user tồn tại nhưng inactive vẫn log userId)

**Expected Result (PASS):**

* Inactive user không nhận email/SMS, nhưng audit log ghi userId để tracking

**Expected Result (FAIL):**

* Token được tạo cho INACTIVE user → security issue (reset token for non-active account)

---

### FORGOT-TC-005 — Rate limit: user exceeds 3 requests/hour → 429 AUTH-040

**Severity:** `CRITICAL`
**CWE:** `CWE-770 — Allocation of Resources Without Limits or Throttling`
**Legal:** `BR-SECURITY`
**Feature Under Test:** `RateLimitService` + `ForgotPasswordService`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-AUTH-014`

**Preconditions:**

* `RateLimitService` mock: `checkUserLimit(userId)` → `false` (exceeded)
* `UserRepository` mock: any user

**Test Steps:**

1. **Arrange:** `makeForgotRequest("test@example.com")`, `userId = FX-FP-001.id`
2. **Act:** `forgotPasswordService.forgotPassword(request)`
3. **Assert:**
   * Exception `RateLimitException` thrown
   * `errorCode = "AUTH-040"`
   * `auditService.emit(PasswordResetRateLimitedEvent{limitType="user"})` được gọi
   * `userRepository.findByEmailOrPhone()` KHÔNG được gọi (rate limit check trước user lookup)

**Expected Result (PASS):**

* Rate limit rejected before touching UserRepository, audit logged, correct error code

**Expected Result (FAIL):**

* UserRepository được gọi trước rate limit check → timing enumeration possible
* No audit event → missing security logging

---

### FORGOT-TC-006 — Rate limit: IP exceeds 10 requests/hour → 429 AUTH-040

**Severity:** `CRITICAL`
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-AUTH-014`

**Preconditions:**

* `RateLimitService.checkIpLimit(ip)` → `false`
* `userId` unknown (guest)

**Test Steps:**

1. **Arrange:** `request.setIpAddress("203.0.113.1")`
2. **Act:** `forgotPasswordService.forgotPassword(request)`
3. **Assert:**
   * `RateLimitException` với `AUTH-040`
   * `audit.emit(PasswordResetRateLimitedEvent{limitType="ip"})` called
   * UserRepository NOT called

**Expected Result (PASS):**

* IP rate limit enforced independently of user

---

### FORGOT-TC-007 — Invalid contact format → 400 AUTH-041

**Severity:** `HIGH`
**Feature Under Test:** `ForgotPasswordController` validation
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-AUTH-011` (validate input)

**Preconditions:**

* None

**Test Steps:**

1. **Arrange:** `makeForgotRequest("invalid-contact")` (neither email nor phone regex match)
2. **Act:** `controller.forgotPassword(request)`
3. **Assert:**
   * `MethodArgumentNotValidException` hoặc custom `InvalidContactFormatException`
   * Error response `400` với `{"error":{"code":"AUTH-041",...}}`
   * Service method NOT called

**Expected Result (PASS):**

* Validation fails fast, service không được gọi, correct error code

---

### FORGOT-TC-008 — Token hash stored in DB (64-char hex, not UUID)

**Severity:** `CRITICAL`
**CWE:** `CWE-327 — Use of a Broken or Risky Cryptographic Algorithm`
**Legal:** `GDPR Art. 32`
**Feature Under Test:** `PasswordResetTokenRepository.saveHash()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-AUTH-012`

**Preconditions:**

* `FX-FP-001` user
* `UUID token = UUID.randomUUID()`

**Test Steps:**

1. **Arrange:** Compute `expectedHash = DigestUtils.sha256Hex(token.toString())`
2. **Act:** `service.forgotPassword(request)` (happy path)
3. **Assert:**
   * `tokenRepository.saveHash()` called với `tokenHash` equals `expectedHash`
   * `tokenHash.length() == 64`
   * `tokenHash` matches pattern `[0-9a-f]{64}` (hex only)
   * DB record `tokenHash != token.toString()` (not plaintext)

**Expected Result (PASS):**

* SHA-256 hash stored, not UUID plaintext

**Expected Result (FAIL):**

* DB contains plaintext UUID → security vulnerability

---

### FORGOT-TC-SEC-001 — Constant-time token compare: verify MessageDigest.isEqual() được dùng

**Severity:** `CRITICAL`
**CWE:** `CWE-208 — Observable Timing Discrepancy`
**Legal:** `GDPR Art. 32`
**Feature Under Test:** `PasswordResetTokenRepository` — phương thức tìm kiếm theo token hash
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-AUTH-012` (constant-time requirement)

> **Lý do thay đổi thiết kế test:** SHA-256 hash của hai token khác nhau sẽ cho ra chuỗi hoàn toàn khác nhau — đo timing trên chuỗi hex không chứng minh được constant-time compare. Đúng cách để verify là kiểm tra trực tiếp implementation code dùng `MessageDigest.isEqual()` thay vì `.equals()`.

**Preconditions:**

* `FX-FP-005`: Valid token với plaintext `"valid-token-uuid"`, hash = `SHA256("valid-token-uuid")`
* Spy/Mock trên `MessageDigest` hoặc inspect source code

**Test Steps (2 phần):**

**Phần A — Unit test verify implementation dùng constant-time compare:**

```java
@Test
void findByHash_usesConstantTimeCompare() throws Exception {
    // Arrange
    String plaintext = "valid-token-uuid";
    String hash = DigestUtils.sha256Hex(plaintext);

    // Spy trên MessageDigest để verify isEqual được gọi
    MockedStatic<MessageDigest> mdMock = mockStatic(MessageDigest.class);
    mdMock.when(() -> MessageDigest.isEqual(any(), any())).thenReturn(true);

    // Act
    tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hash, LocalDateTime.now());

    // Assert: MessageDigest.isEqual() phải được gọi trong quá trình compare
    // (Nếu dùng .equals() thay vì isEqual thì test này sẽ fail vì isEqual chưa bị gọi)
    mdMock.verify(() -> MessageDigest.isEqual(any(), any()), atLeastOnce());
    mdMock.close();
}
```

**Phần B — Code inspection check (chạy trong CI):**

```bash
# Kiểm tra KHÔNG có .equals() được dùng để so sánh token hash trong repository
grep -r "\.equals(" \
  src/main/java/com/carebridge/backend/auth/repository/PasswordResetTokenRepository* \
  | grep -i "hash\|token"
# Expected: không có output (nếu có output → fail)

# Kiểm tra MessageDigest.isEqual() được dùng trong service/repository
grep -r "MessageDigest.isEqual" \
  src/main/java/com/carebridge/backend/auth/
# Expected: ít nhất 1 kết quả
```

**Expected Result (PASS):**

* `MessageDigest.isEqual()` được verify là gọi trong hash comparison
* CI grep không tìm thấy `.equals()` trên token hash
* Source code dùng constant-time compare

**Expected Result (FAIL):**

* `MessageDigest.isEqual()` không bao giờ được gọi → `.equals()` đang được dùng → timing attack possible
* CI grep tìm thấy `.equals(hash)` hoặc `.equals(tokenHash)` trong repo/service code

**Implementation Note:**

```java
// WRONG — early-exit, timing attack vulnerable:
if (storedHash.equals(inputHash)) { ... }

// CORRECT — constant-time, không phụ thuộc vào nội dung chuỗi:
if (MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                           inputHash.getBytes(StandardCharsets.UTF_8))) { ... }
```

---

### FORGOT-TC-009a — Notification: emailVerified=false → SMS trực tiếp (email KHÔNG gọi)

**Severity:** `MEDIUM`
**Feature Under Test:** `NotificationService.dispatchPasswordReset()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-AUTH-015` — email chỉ gửi IF `emailVerified=true`

**Preconditions:**

* `FX-FP-002`: User với `phoneVerified=true`, `emailVerified=false`
* `EmailService` mock: sẵn sàng capture (không được gọi)
* `SmsService` mock: sẵn sàng capture

**Test Steps:**

1. **Arrange:** `makeForgotRequest("+84912345678")`
2. **Act:** `service.forgotPassword(request)`
3. **Assert:**
   * Response 200, generic message
   * `emailService.sendPasswordResetEmail()` **KHÔNG được gọi** (0 invocations)
   * `smsService.sendSms()` được gọi 1 lần
   * `auditService.emit()` logged với `contactMethod="sms"`

**Expected Result (PASS):**

* SMS gửi trực tiếp, email không được thử, audit đúng channel

**Expected Result (FAIL):**

* `emailService` được gọi khi `emailVerified=false` → vi phạm ADR-AUTH-015
* SMS không gửi → fallback không hoạt động

---

### FORGOT-TC-009b — Notification: emailVerified=true, email fail → fallback SMS

**Severity:** `MEDIUM`
**Feature Under Test:** `NotificationService.dispatchPasswordReset()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-AUTH-015` — SMS fallback nếu email fail VÀ phone verified

**Preconditions:**

* User với `emailVerified=true`, `phoneVerified=true`
* `EmailService.sendPasswordResetEmail()` mock: throw `EmailSendException`
* `SmsService` mock: sẵn sàng capture

**Test Steps:**

1. **Arrange:** `makeForgotRequest("test@example.com")`, user có cả email + phone verified
2. **Act:** `service.forgotPassword(request)`
3. **Assert:**
   * `emailService.sendPasswordResetEmail()` được gọi 1 lần → throws exception
   * `smsService.sendSms()` được gọi 1 lần (fallback triggered)
   * Response vẫn 200 (notification failure không block response)
   * `auditService.emit()` logged với `contactMethod="sms"` (actual channel used)

**Expected Result (PASS):**

* Email failure → SMS fallback thành công, audit phản ánh kênh thực tế

**Expected Result (FAIL):**

* `EmailSendException` propagate → 500 response (user bị block)
* SMS không được gọi → fallback chưa implement

---

### FORGOT-TC-010 — Audit event PASSWORD_RESET_REQUESTED published với minimal PII

**Severity:** `HIGH`
**Legal:** `PDPA Art. 30 (Records of processing)`
**Feature Under Test:** `AuditService.emit()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-AUTH-016`

**Preconditions:**

* Any user scenario

**Test Steps:**

1. **Arrange:** `makeForgotRequest("test@example.com")`
2. **Act:** `service.forgotPassword(request)`
3. **Assert:**
   * `auditService.emit()` called với event type `PasswordResetRequestedEvent`
   * Event payload:
     * `userId` = `FX-FP-001.id` (hoặc `null` nếu user not found)
     * `contactMethod` = `"email"` hoặc `"sms"`
     * `contactValue` = masked pattern (ví dụ: `"te***@example.com"` hoặc `"+84******678"`)
     * `ipAddress` = request.ip
     * `requestId` = not null UUID
   * **PII check:** `event.payload` KHÔNG chứa full email/phone string

**Expected Result (PASS):**

* Audit event with masked contact, no full PII in logs

**Expected Result (FAIL):**

* Full email `"test@example.com"` logged in audit → GDPR violation

---

### FORGOT-TC-011 — Token expires after 15 minutes → validateToken() rejects

**Severity:** `HIGH`
**Feature Under Test:** `ForgotPasswordService.validateToken()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-AUTH-012` (TTL 15min)

**Preconditions:**

* `FX-FP-006`: Token với `expiresAt = now() - 1 minute`
* `usedAt = null`

**Test Steps:**

1. **Arrange:** `plaintextToken = "expired-token-uuid"`
2. **Act:** `service.validateToken(plaintextToken)`
3. **Assert:**
   * `InvalidTokenException` thrown với `AUTH-050`
   * `tokenRepository.findByHashAndNotUsed()` được gọi với hash, before=now()
   * Returned null do `expiresAt < now()`

**Expected Result (PASS):**

* Expired token rejected

---

### FORGOT-TC-012 — consumeToken() marks token as used (append-only)

**Severity:** `CRITICAL`
**Feature Under Test:** `ForgotPasswordService.consumeToken()`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-AUTH-012` (append-only)

**Preconditions:**

* `FX-FP-005`: Token với `usedAt = null`

**Test Steps:**

1. **Arrange:** `plaintextToken = FX-FP-005.getPlaintext()`
2. **Act:** `boolean result = service.consumeToken(plaintextToken)`
3. **Assert:**
   * `result == true`
   * `tokenRepository.markAsUsed(tokenId, now())` được gọi 1 lần
   * DB record: `usedAt` is set, row still exists (not deleted)
   * Second call to `consumeToken()` same token → `false` (already used)

**Expected Result (PASS):**

* Token marked used, not deleted, idempotent second call returns false

---

### FORGOT-TC-013 — validateToken() with already-used token → reject

**Severity:** `CRITICAL`
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-AUTH-012` (one-time use)

**Preconditions:**

* `FX-FP-007`: Token với `usedAt = now() - 5min` (already used)

**Test Steps:**

1. **Arrange:** `plaintextToken = FX-FP-007.getPlaintext()`
2. **Act:** `service.validateToken(plaintextToken)`
3. **Assert:**
   * `InvalidTokenException(AUTH-050)` thrown
   * `tokenRepository.findByHashAndNotUsed()` query includes `usedAt IS NULL` condition

**Expected Result (PASS):**

* Used token rejected

---

### FORGOT-TC-INT-001 — Integration: forgot → validate token → consume (full flow)

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow — `ForgotPasswordService` + `PasswordResetTokenRepository` + DB (Testcontainers)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `UC-05 → UC-06 flow`, `ADR-AUTH-012` (token lifecycle)
**Test File:** `src/test/java/com/carebridge/backend/auth/ForgotPasswordIntegrationTest.java`

**Preconditions:**

* Testcontainers: PostgreSQL running với Flyway migration `V{n}__create_password_reset_tokens` applied
* Testcontainers: Redis running cho rate limit
* `FX-FP-001`: User ACTIVE với `email="test@example.com"`, `emailVerified=true` đã được insert vào DB
* `EmailService` mock: capture `token` từ lần gọi đầu tiên
* Rate limit disabled trong test profile (`RATE_LIMIT_ENABLED=false`)

**Test Steps:**

1. **Step 1 — Request forgot password:**
   ```java
   mockMvc.perform(post("/api/v1/auth/forgot-password")
       .contentType(APPLICATION_JSON)
       .content("{\"contact\":\"test@example.com\"}")
       .header("X-Forwarded-For", "127.0.0.1"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.message").value(containsString("If the account exists")))
       .andExpect(jsonPath("$.expiresIn").value(900));
   ```

2. **Step 2 — Lấy plaintext token từ DB (để simulate nhận email):**
   ```java
   // Vì email là mock, lấy token từ ArgumentCaptor
   ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
   verify(emailService).sendPasswordResetEmail(any(User.class), tokenCaptor.capture());
   String plaintextToken = tokenCaptor.getValue();

   // Verify hash trong DB (không lưu plaintext)
   String expectedHash = DigestUtils.sha256Hex(plaintextToken);
   PasswordResetToken stored = tokenRepository
       .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(expectedHash, LocalDateTime.now())
       .orElseThrow();
   assertThat(stored.getTokenHash()).isEqualTo(expectedHash);
   assertThat(stored.getTokenHash()).hasSize(64);
   assertThat(stored.getUsedAt()).isNull();
   ```

3. **Step 3 — Validate token:**
   ```java
   mockMvc.perform(post("/api/v1/auth/validate-reset-token")
       .contentType(APPLICATION_JSON)
       .content("{\"token\":\"" + plaintextToken + "\"}"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.valid").value(true))
       .andExpect(jsonPath("$.expiresIn").value(greaterThan(0)));
   ```

4. **Step 4 — Consume token (simulate UC-06 ResetPassword):**
   ```java
   boolean consumed = forgotPasswordService.consumeToken(plaintextToken);
   assertThat(consumed).isTrue();

   // Verify: token vẫn tồn tại nhưng usedAt được set (append-only)
   PasswordResetToken afterConsume = tokenRepository.findById(stored.getId()).orElseThrow();
   assertThat(afterConsume.getUsedAt()).isNotNull();
   ```

5. **Step 5 — Verify second use rejected:**
   ```java
   mockMvc.perform(post("/api/v1/auth/validate-reset-token")
       .contentType(APPLICATION_JSON)
       .content("{\"token\":\"" + plaintextToken + "\"}"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.error.code").value("AUTH-050"));
   ```

**Expected Result (PASS):**

* Toàn bộ flow forgot → validate → consume hoạt động end-to-end
* DB: hash được lưu (64 chars hex), không phải plaintext UUID
* Token được mark used (not deleted) sau consume
* Second validate trả về AUTH-050

**Expected Result (FAIL):**

* Token không lưu trong DB sau step 1 → saveHash() chưa implement
* Validate step trả về 400 ngay → validateToken() sai hash logic
* Token bị xóa thay vì mark used → append-only violation
* Used token vẫn validate thành công → one-time-use không enforce

**External Dependencies:**
* PostgreSQL (Testcontainers — `@Testcontainers`)
* Redis (Testcontainers — nếu rate limit enabled)
* EmailService mock (`@MockBean`)

---

## 5. Red-Green-Refactor Tracker

| TC ID                  | Test File                                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note                                          |
| ---------------------- | -------------------------------------------------- | ---------------- | ----------------- | --------------------------------------------------------- |
| `FORGOT-TC-001`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Extract `generateTokenAndHash()` helper                 |
| `FORGOT-TC-002`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Reuse factory từ TC-001                                 |
| `FORGOT-TC-003`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Assert response equality via `.equals()` not `==`     |
| `FORGOT-TC-004`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Reuse INACTIVE user fixture                             |
| `FORGOT-TC-005`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Verify InOrder: rateLimit trước userRepository          |
| `FORGOT-TC-006`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Reuse IP rate limit mock từ TC-005                      |
| `FORGOT-TC-007`      | `ForgotPasswordControllerTest.java`              | ☐               | ☐                | Controller test — validation layer only                 |
| `FORGOT-TC-008`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Dùng `assertThat(hash).matches("[0-9a-f]{64}")`       |
| `FORGOT-TC-009a`     | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Verify emailService never() khi emailVerified=false     |
| `FORGOT-TC-009b`     | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Chú ý thứ tự: email try → fail → sms call             |
| `FORGOT-TC-010`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Dùng `ArgumentCaptor<PasswordResetRequestedEvent>`    |
| `FORGOT-TC-011`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Dùng fixed clock hoặc Instant.now().minusMinutes(16)    |
| `FORGOT-TC-012`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Verify row vẫn tồn tại sau consume (append-only)       |
| `FORGOT-TC-013`      | `ForgotPasswordServiceTest.java`                 | ☐               | ☐                | Dùng `FX-FP-007.getPlaintext()` từ fixture             |
| `FORGOT-TC-SEC-001`  | `ForgotPasswordConstantTimeTest.java`            | ☐               | ☐                | CI grep check trong pipeline                            |
| `FORGOT-TC-INT-001`  | `ForgotPasswordIntegrationTest.java`             | ☐               | ☐                | Testcontainers — full flow E2E                          |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// ForgotPasswordService.java — RED PHASE STUB (PHẢI throw)
@Service
public class ForgotPasswordService {
  public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
  }
  
  public User validateToken(String token) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
  }
  
  public boolean consumeToken(String token) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
  }
}
```

**Red Gate Verification:**

| TC ID                 | Stub Result                              | Expected | Actual          | Root Cause (nếu PASS bất thường)                 |
| --------------------- | ---------------------------------------- | -------- | --------------- | ---------------------------------------------------- |
| `FORGOT-TC-001`     | `throw(UnsupportedOperationException)` | 🔴 FAIL  | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import  |
| `FORGOT-TC-002`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-003`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-004`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-005`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-006`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-007`     | `throw(...) / 400`                     | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-008`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-009a`    | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-009b`    | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-010`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-011`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-012`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-013`     | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS |                                                      |
| `FORGOT-TC-SEC-001` | grep finds nothing / mock not called   | 🔴 FAIL  | ☐ FAIL ☐ PASS | ☐ isEqual chưa được gọi                           |
| `FORGOT-TC-INT-001` | `throw(...)`                           | 🔴 FAIL  | ☐ FAIL ☐ PASS | ☐ Testcontainers chưa start                        |

**Red Gate Evidence:**

* Stub commit hash: `___` (to be filled after stub commit)
* Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
* Log file: `build/red-gate-evidence.log` (to be generated)

**Nếu bất kỳ test PASS:**

* Dừng lại.
* Xác định root cause: có thể test đang dùng default return thay vì gọi method → test không thực sự invoke stub.
* Rewrite test với Props Isolation Pattern (factory method), đảm bảo mỗi test gọi rõ ràng `forgotPassword()` method.

---

## 6. Entry / Exit Criteria (cont.)

### Entry Criteria (Điều kiện bắt đầu)

* [ ] TDS `CB-AUTH-IMP-005` đã được review và approve
* [ ] Logic Issues (§2) đã được confirm với Principal Architect
* [ ] Prisma migration `add_password_reset_tokens` đã approved
* [ ] Test fixtures (FX-FP-001 to FX-FP-010) đã được chuẩn bị
* [ ] Redis container running cho rate limit tests
* [ ] Red Gate (§5.1) — tất cả tests FAIL với stub trước khi implement

### Exit Criteria (Điều kiện kết thúc — DoD)

* [ ] `mvn test` — tất cả unit tests xanh (không skip)
* [ ] `mvn verify` — integration tests xanh (Testcontainers PostgreSQL + Redis)
* [ ] Test coverage ≥ 80% lines cho ForgotPasswordService
* [ ] Không có `any` type trong production code liên quan
* [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
* [ ] **Constant-time verification** — timing test FORGOT-TC-SEC-001 PASS (variance < 1ms)
* [ ] **Anti-enumeration check** — response messages identical cho user exists/not exists
* [ ] **Token hash check** — DB contains 64-char hex, không plaintext UUID
* [ ] **Rate limit enforcement** — user limit checked trước user lookup

### Suspension Criteria (Điều kiện tạm dừng)

* Redis infrastructure unavailable (rate limit depends on it)
* Prisma migration chưa sẵn sàng trên staging
* ADR chưa finalized (security review pending)
* CI pipeline broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
npx prisma migrate resolve --rolled-back <migration_hash>

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/auth/ForgotPasswordService.java
git checkout -- src/main/java/com/carebridge/backend/auth/ForgotPasswordController.java
git checkout -- src/main/resources/application.yml (feature flags)

# Revert test files (nếu cần)
git checkout -- src/test/java/com/carebridge/backend/auth/ForgotPasswordServiceTest.java

# Feature flag: disable forgot password
kubectl set env deployment/carebridge-api FORGOT_PASSWORD_ENABLED=false

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection

### Anti-Pattern Checklist

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                | Check | Gate chặn |
| --------- | ------------------------ | -------------------------------------------------------- | ----- | ---------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào              | ☐    | G-0        |
| AP-AI-002 | Green-from-Birth         | Test PASS với empty/throw stub (§5.1)                  | ☐    | G-2 ★     |
| AP-AI-003 | Implicit Decision        | Test assume architecture không có trong §3 ADR        | ☐    | G-1        |
| AP-AI-004 | Layer Violation          | Test verify controller có business logic                | ☐    | G-4        |
| AP-AI-005 | Hallucinated Contract    | Test import service/type không tồn tại trong codebase | ☐    | G-3        |

**Kết quả review:**

* [X] Không phát hiện anti-pattern nào → TDD spec approved
* [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ------- | ---------- | ------ |
| —          | —    | —      | —         | —     |

---

## PHỤ LỤC

### A. Glossary (Thuật ngữ)

| Thuật ngữ               | Định nghĩa                                                                           |
| ------------------------- | --------------------------------------------------------------------------------------- |
| PII                       | Personally Identifiable Information                                                     |
| Anti-enumeration          | Chống tra cứu để xác định user tồn tại — luôn trả về response đồng nhất |
| Constant-time compare     | So sánh hash mà không phụ thuộc vào số ký tự khớp (chống timing attack)      |
| Sliding window rate limit | Dùng timestamp để đếm request trong window X giây, sliding thay vì fixed bucket  |
| Append-only               | Chỉ INSERT, không UPDATE/DELETE — token markAsUsed nhưng không xóa row            |
| Red Gate                  | Gate xác minh test sensitivity — tests phải FAIL trước khi implement               |
| Props Isolation           | Mỗi test tạo fresh instance qua factory, không shared mutable state                  |

### B. Tài liệu tham chiếu

| Document                              | Link / Path                                                                                        |
| ------------------------------------- | -------------------------------------------------------------------------------------------------- |
| GDPR Art. 32 (Security of processing) | [GDPR Article 32](https://gdpr.eu/article-32/)                                                        |
| OWASP A04:2021 — Insecure Design     | [OWASP Top 10](https://owasp.org/Top10/A04_2021-Insecure_Design/)                                     |
| ADR-AUTH-011 đến ADR-AUTH-016       | `03_Design/Architecture/ADR/`                                                                    |
| Password Reset Token Blueprint        | `03_implement/AUTH_PASSWORD_RESET.md`                                                            |
| Rate Limiting Service                 | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/RateLimitService.java` |
| CASE 2.0 Methodology                  | `vii_reports/FPT-EDU-REP-METH-002_CASE_AI_METHODOLOGY_v1.1.md`                                   |
| TDD Template (CASE 2.0)               | `08_References/Template/PHASE-4_Test-Spec.md`                                                    |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Sections đánh dấu ⭐ là bổ sung mới từ CASE 2.0 methodology.*
