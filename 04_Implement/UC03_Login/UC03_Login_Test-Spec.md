# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Mẫu Đặc tả Kiểm thử — UC-03 Login

**Document ID:** `CB-AUTH-TEST-003`
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
- `04_Implement/UC03_Login/UC03_Login_TDS.md` (CB-AUTH-IMP-003 v1.0)
- `ADR-AUTH-006`, `ADR-AUTH-007`, `ADR-AUTH-008`
- `BR-LOGIN-001` đến `BR-LOGIN-008`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-03 Login |

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
| **Feature / Gap ID** | `UC-03` |
| **Module** | `Login — auth` |
| **Spec gốc** | `CB-AUTH-IMP-003` |
| **Priority** | 🔴 P0 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M1 Alpha — Auth Module` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-SECURITY, PDPA` |
| **Upstream Dependencies** | `UC-02 VerifyOTP (users.status=ACTIVE)` |
| **Downstream Consumers** | `All protected endpoints, UC-04 Logout` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-AUTH-IMP-003 §17`, `ADR-AUTH-006`, `ADR-AUTH-007`, `ADR-AUTH-008` |
| **Constraints Injected** | jwt-env-secret, token-ttl, refresh-hash, lockout-5, anti-enumeration |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Spec nói "reset failedLoginCount khi login thành công" nhưng không rõ thời điểm | Policy: reset TRƯỚC khi issue token, TRONG cùng transaction | Test verify count=0 sau login thành công |
| L2 | Không rõ message khi user không tồn tại vs sai password | Policy: cùng message AUTH-011 (anti-enumeration) | Test cả 2 case cùng expect AUTH-011 với message giống nhau |
| L3 | Refresh token lưu dưới dạng gì trong DB | ADR-AUTH-008: SHA-256 hash | Test verify DB chứa hex string, không phải JWT plaintext |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
Login bao gồm các layer:
├── JwtService (token generation — mock clock)
├── AuthService.login() (orchestration — mock repos, mock JwtService)
├── AuthController (HTTP layer — mock service)
└── Integration (Testcontainers PostgreSQL + real JwtService)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-03` | Login flow, token issuance |
| `ADR-AUTH-006` | JWT TTL: access=15min, refresh=7d |
| `ADR-AUTH-007` | Lockout after 5 failures |
| `ADR-AUTH-008` | Session tracking |
| `BR-LOGIN-001` | ACTIVE account only |
| `BR-LOGIN-002` | Max 5 failures → LOCKED |
| `BR-LOGIN-003` | Audit every attempt |
| `BR-LOGIN-004` | Access token 15 min |
| `BR-LOGIN-005` | Refresh token 7 days |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Credentials đúng, account ACTIVE → tokens | `AuthService.login()` | `LOGIN-TC-001` |
| TC-COND-002 | Login bằng phone number | `AuthService.findUserByIdentifier()` | `LOGIN-TC-002` |
| TC-COND-003 | Account UNVERIFIED → AUTH-015 | `AuthService.validateAccountStatus()` | `LOGIN-TC-003` |
| TC-COND-004 | Account LOCKED → AUTH-013 | `AuthService.validateAccountStatus()` | `LOGIN-TC-004` |
| TC-COND-005 | Password sai → AUTH-011 + failedCount++ | `AuthService.handleLoginFailure()` | `LOGIN-TC-005` |
| TC-COND-006 | Lần thứ 5 sai → LOCKED + SecurityEvent | `AuthService.handleLoginFailure()` | `LOGIN-TC-006` |
| TC-COND-007 | JWT access token TTL = 900s | `JwtService.generateAccessToken()` | `LOGIN-TC-007` |
| TC-COND-008 | Session record trong DB | `SessionRepository.save()` | `LOGIN-TC-008` |
| TC-COND-009 | Anti-enumeration: same message | Error path | `LOGIN-TC-009` |
| TC-COND-010 | Brute force rate limit | Rate limiter | `LOGIN-TC-SEC-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | failedLoginCount: 0→1→5 + LOCKED | Kiểm tra lockout state machine |
| Boundary Value Analysis | failedCount: 4→5 (lockout boundary) | Ranh giới khóa tài khoản |
| Equivalence Partitioning | email vs phone identifier | Login bằng cả 2 loại |
| Error Guessing | Credential enumeration | Security testing |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-LOGIN-001` | DB seed | `{email:"active@test.com", status:ACTIVE, failedCount:0, passwordHash: BCrypt("SecureP@ss1")}` | Happy path |
| `FX-LOGIN-002` | DB seed | `{phone:"0912345678", status:ACTIVE, failedCount:0}` | Login by phone |
| `FX-LOGIN-003` | DB seed | `{email:"unverified@test.com", status:UNVERIFIED}` | UNVERIFIED account |
| `FX-LOGIN-004` | DB seed | `{email:"locked@test.com", status:LOCKED}` | LOCKED account |
| `FX-LOGIN-005` | DB seed | `{email:"almost@test.com", failedCount:4, status:ACTIVE}` | One away from lockout |
| `FX-LOGIN-006` | ENV | `JWT_SECRET=dGVzdHNlY3JldHZhbHVlZm9ydGVzdGluZw==` | Fixed JWT secret |
| `FX-LOGIN-007` | Clock | `fixed clock: 2026-06-26T10:00:00Z` | Deterministic token TTL test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// LoginTestFactory.java
private static User makeActiveUser(String email, String rawPassword) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(email);
    user.setPasswordHash(new BCryptPasswordEncoder(12).encode(rawPassword));
    user.setRole(UserRole.MOTHER);
    user.setStatus(AccountStatus.ACTIVE);
    user.setFailedLoginCount(0);
    return user;
}

private static LoginRequestDTO makeLoginRequest(String identifier, String password) {
    return new LoginRequestDTO(
        identifier != null ? identifier : "active@test.com",
        password   != null ? password   : "SecureP@ss1",
        "test-device"
    );
}
```

---

### LOGIN-TC-001 — Login thành công → nhận access và refresh token

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.login()` + `JwtService.generateAccessToken()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLoginTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-03 Happy Path`, `BR-LOGIN-004`, `BR-LOGIN-005`

**Preconditions:**
- `FX-LOGIN-001`: user ACTIVE
- `JwtService` real implementation với `FX-LOGIN-006` + `FX-LOGIN-007`
- `SessionRepository` mock: capture saved entity
- `UserRepository` mock: return FX-LOGIN-001

**Test Steps:**
1. **Arrange:** `makeLoginRequest(null, null)` với password "SecureP@ss1"
2. **Act:** `authService.login(request, "127.0.0.1")`
3. **Assert:**
   - `accessToken` không null và là JWT hợp lệ
   - `refreshToken` không null và là JWT hợp lệ
   - `expiresIn` == 900
   - `role` == "MOTHER"
   - `sessionRepository.save()` được gọi 1 lần
   - `eventPublisher.publishEvent()` được gọi với `UserLoggedIn` event

**Expected Result (PASS):**
- Đầy đủ tokens, session saved, audit event published

**Expected Result (FAIL):**
- Nếu session không được save → BR-LOGIN-006 vi phạm

**Current Status:** 🔴 Not written

---

### LOGIN-TC-002 — Login thành công bằng số điện thoại

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.findUserByIdentifier()` — phone lookup
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLoginTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-LOGIN-008`

**Preconditions:**
- `FX-LOGIN-002`: user với phone="0912345678"
- `userRepository.findByPhoneNumber("0912345678")` → FX-LOGIN-002

**Test Steps:**
1. **Act:** `authService.login(new LoginRequestDTO("0912345678", "SecureP@ss1", "test"), "127.0.0.1")`
2. **Assert:**
   - Response không null, accessToken hợp lệ
   - userId khớp với FX-LOGIN-002

**Current Status:** 🔴 Not written

---

### LOGIN-TC-003 — Từ chối tài khoản UNVERIFIED

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.validateAccountStatus()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLoginTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-LOGIN-001`

**Test Steps:**
1. **Arrange:** `FX-LOGIN-003` — user UNVERIFIED
2. **Act:** `authService.login(request, ip)` — expect exception
3. **Assert:**
   - Throws `AuthenticationException` với code `AUTH-015`
   - `sessionRepository.save()` KHÔNG được gọi
   - `jwtService.generateAccessToken()` KHÔNG được gọi

**Current Status:** 🔴 Not written

---

### LOGIN-TC-004 — Từ chối tài khoản LOCKED

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.validateAccountStatus()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLoginTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-LOGIN-001`, `ADR-AUTH-007`

**Test Steps:**
1. **Arrange:** `FX-LOGIN-004` — user LOCKED
2. **Act:** `authService.login(request, ip)`
3. **Assert:**
   - Throws `AccountLockedException` với code `AUTH-013`
   - `failedLoginCount` KHÔNG tăng thêm (đã locked, không cần đếm)

**Current Status:** 🔴 Not written

---

### LOGIN-TC-005 — Sai password lần 1 → failedCount tăng + SecurityEvent

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.handleLoginFailure()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLoginTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-LOGIN-002`, `BR-LOGIN-003`

**Preconditions:**
- `FX-LOGIN-001` với failedLoginCount=0
- `passwordEncoder.matches()` → false (wrong password)

**Test Steps:**
1. **Act:** `authService.login(makeLoginRequest(null, "WrongPass"), "127.0.0.1")`
2. **Assert:**
   - Throws `AuthenticationException` với code `AUTH-011`
   - `userRepository.save()` gọi với user.failedLoginCount = 1
   - `eventPublisher` gọi với `LoginFailedEvent{securityEventType="LOGIN_FAILED"}`
   - user.status vẫn ACTIVE (chưa đến 5)

**Current Status:** 🔴 Not written

---

### LOGIN-TC-006 — Lần thứ 5 sai → LOCKED + SecurityEvent LOGIN_FAILED

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.handleLoginFailure()` — lockout threshold
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLoginTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-LOGIN-002`, `ADR-AUTH-007`

**Preconditions:**
- `FX-LOGIN-005`: user ACTIVE với failedLoginCount=4

**Test Steps:**
1. **Act:** `authService.login(makeLoginRequest(null, "WrongPass"), "ip")`
2. **Assert:**
   - Throws `AuthenticationException` với code `AUTH-013` (LOCKED message)
   - `userRepository.save()` với:
     - `failedLoginCount = 5`
     - `status = LOCKED`
     - `lockedAt` != null
   - `LoginFailedEvent` published với securityEventType=LOGIN_FAILED

**Expected Result (FAIL — dấu hiệu lỗi):**
- Nếu status vẫn ACTIVE → lockout không hoạt động
- Nếu SecurityEvent không ghi → vi phạm BR-LOGIN-003 audit requirement

**Current Status:** 🔴 Not written

---

### LOGIN-TC-007 — JWT access token có TTL đúng 900 giây

**Severity:** `HIGH`
**Feature Under Test:** `JwtService.generateAccessToken()`
**Test File:** `src/test/java/com/carebridge/backend/auth/JwtServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-LOGIN-004`, `ADR-AUTH-006`

**Preconditions:**
- Fixed clock: `2026-06-26T10:00:00Z`
- `JWT_SECRET` = FX-LOGIN-006

**Test Steps:**
1. **Act:** `jwtService.generateAccessToken(user)` với fixed clock
2. **Assert:**
   - Parse JWT, kiểm tra claims:
     - `sub` == userId.toString()
     - `role` == "MOTHER"
     - `exp` == `iat + 900` (chính xác đến giây)

**Expected Result (PASS):**
```java
Claims claims = jwtService.validateToken(accessToken);
long iat = claims.getIssuedAt().getTime() / 1000;
long exp = claims.getExpiration().getTime() / 1000;
assertThat(exp - iat).isEqualTo(900L);
```

**Current Status:** 🔴 Not written

---

### LOGIN-TC-008 — Session record lưu SHA-256 hash của refresh token

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.login()` — session persistence
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLoginTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-AUTH-008`, `BR-LOGIN-006`

**Preconditions:**
- `SessionRepository` mock: capture argument của `save()`

**Test Steps:**
1. **Act:** `authService.login(validRequest, "127.0.0.1")`
2. Capture `savedSession` từ `sessionRepository.save()` argument
3. **Assert:**
   - `savedSession.getRefreshTokenHash()` != response.getRefreshToken()
   - `savedSession.getRefreshTokenHash()` là 64-char hex string (SHA-256)
   - `DigestUtils.sha256Hex(response.getRefreshToken())` == `savedSession.getRefreshTokenHash()`

**Expected Result (PASS):**
- Hash stored, plain token in response

**Expected Result (FAIL):**
- Nếu plain JWT được lưu → ADR-AUTH-008 vi phạm, security risk

**Current Status:** 🔴 Not written

---

### LOGIN-TC-009 — Anti-enumeration: cùng message cho "user not found" và "wrong password"

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-204 — Observable Response Discrepancy`
**Feature Under Test:** `AuthService.login()` — error message uniformity
**Test File:** `src/test/java/com/carebridge/backend/auth/LoginSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TC-SEC-001` TDS §13

**Test Steps (Attack Simulation):**
1. **Case A:** POST `/api/v1/auth/login` với email không tồn tại
2. **Case B:** POST `/api/v1/auth/login` với email đúng nhưng sai password
3. **Assert:**
   - Case A: HTTP 401, code `AUTH-011`, message `"Email/số điện thoại hoặc mật khẩu không đúng"`
   - Case B: HTTP 401, code `AUTH-011`, SAME message

**Expected Result (PASS = an toàn):**
- Attacker không phân biệt được user có tồn tại không

**Expected Result (FAIL = lỗ hổng):**
- Case A trả `"User not found"` → attacker có thể enumerate users

**Current Status:** 🔴 Not written

---

### LOGIN-TC-INT-001 — Integration: Login → session trong DB → logout flow

**Severity:** `HIGH`
**Feature Under Test:** Full auth flow
**Test File:** `src/test/java/com/carebridge/backend/auth/LoginIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** Full integration

**Preconditions:**
- PostgreSQL Testcontainer
- Migrations V1, V2, V3 applied
- User seeded: ACTIVE, password="SecureP@ss1"

**Test Steps:**
1. POST `/api/v1/auth/login` → capture accessToken, refreshToken
2. Query DB: `SELECT * FROM user_sessions WHERE user_id=?`
3. Verify access token JWT claims

**Expected Result (PASS):**
```java
// Session in DB
UserSession session = sessionRepository.findByUserIdAndRevokedFalse(userId).get(0);
assertThat(session.isRevoked()).isFalse();
assertThat(session.getRefreshTokenHash()).isNotBlank();
assertThat(session.getRefreshTokenHash().length()).isEqualTo(64); // SHA-256 hex

// JWT claims
Claims claims = jwtService.validateToken(loginResponse.accessToken());
assertThat(claims.getSubject()).isEqualTo(userId.toString());
assertThat(claims.get("role", String.class)).isEqualTo("MOTHER");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `LOGIN-TC-001` | `AuthServiceLoginTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-002` | `AuthServiceLoginTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-003` | `AuthServiceLoginTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-004` | `AuthServiceLoginTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-005` | `AuthServiceLoginTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-006` | `AuthServiceLoginTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-007` | `JwtServiceTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-008` | `AuthServiceLoginTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-009` | `LoginSecurityTest.java` | `[ ]` | `—` | — |
| `LOGIN-TC-INT-001` | `LoginIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class AuthService implements IAuthService {
    @Override
    public LoginResponseDTO login(LoginRequestDTO request, String ipAddress) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Service
public class JwtServiceImpl implements JwtService {
    @Override
    public String generateAccessToken(User user) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|----------|--------|----------------------------------|
| `LOGIN-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `LOGIN-TC-006` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `LOGIN-TC-007` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `LOGIN-TC-INT-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS CB-AUTH-IMP-003 đã approve
- [ ] UC-01 + UC-02 đã implement (users table với status, failedLoginCount)
- [ ] Migration V3 (user_sessions) đã approve
- [ ] `JWT_SECRET` ENV variable đã được cấu hình cho test env

### Exit Criteria (DoD)

- [ ] Tất cả 10 test cases xanh
- [ ] Test coverage ≥ 80% trên `AuthService.login()`, `JwtServiceImpl`
- [ ] JWT secret không xuất hiện trong source code (chỉ ENV)
- [ ] Refresh token lưu dưới dạng SHA-256 hash trong DB (code review)
- [ ] Anti-enumeration: same message (LOGIN-TC-009 green)
- [ ] Account lockout sau đúng 5 lần thất bại

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate confirmed: tất cả tests FAIL với stub
- [ ] LOGIN-TC-008: hash verification pass (refresh token không lưu plaintext)

---

## 7. Rollback Plan

```bash
# Revert service và migration
git checkout -- src/main/java/com/carebridge/backend/auth/service/AuthService.java
git checkout -- src/main/java/com/carebridge/backend/auth/service/JwtServiceImpl.java
./mvnw flyway:undo  # Revert V3 user_sessions
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-LOGIN-* | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | LOGIN-TC-001 PASS với stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | JWT secret hard-coded trong implementation | ☐ | G-1 |
| AP-AI-004 | Layer Violation | AuthController chứa JWT generation logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `TokenFactory` không có trong §8 TDS | ☐ | G-3 |

---

*TDD Spec CB-AUTH-TEST-003 v1.0 — UC-03 Login*
*Tuân theo EDS v2.0 + CASE 2.0*
