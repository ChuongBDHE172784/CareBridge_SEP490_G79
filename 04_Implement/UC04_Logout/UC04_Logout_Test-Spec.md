# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Mẫu Đặc tả Kiểm thử — UC-04 Logout

**Document ID:** `CB-AUTH-TEST-004`
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
- `04_Implement/UC04_Logout/UC04_Logout_TDS.md` (CB-AUTH-IMP-004 v1.0)
- `04_Implement/UC03_Login/UC03_Login_TDS.md` (CB-AUTH-IMP-003)
- `ADR-AUTH-009`, `ADR-AUTH-010`
- `BR-LOGOUT-001` đến `BR-LOGOUT-007`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-04 Logout |

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
| **Feature / Gap ID** | `UC-04` |
| **Module** | `Logout — auth` |
| **Spec gốc** | `CB-AUTH-IMP-004` |
| **Priority** | 🔴 P0 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M1 Alpha — Auth Module` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-SECURITY, PDPA` |
| **Upstream Dependencies** | `UC-03 Login (user_sessions)` |
| **Downstream Consumers** | `AuditService, SecurityEventLog` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-AUTH-IMP-004 §17`, `ADR-AUTH-009`, `ADR-AUTH-010` |
| **Constraints Injected** | token-revoked-event, partial-logout-isolation, session-ownership, no-delete-append-only |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Không rõ access token có bị blacklist không | ADR-AUTH-010: KHÔNG blacklist, stateless | Test document rằng access token vẫn valid sau logout (expected behavior) |
| L2 | Spec không rõ khi refreshToken hash không tồn tại | Policy: AUTH-023 (not found) vs AUTH-021 (revoked) | Test 2 cases riêng biệt |
| L3 | Không rõ revokeAllByUserId có revoke session của mình không | Policy: có — revoke ALL sessions including current | Test verify current session cũng bị revoke trong logout-all |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
Logout bao gồm các layer:
├── AuthService.logout() — orchestration (mock SessionRepository)
├── AuthService.logoutSingle() — partial logout
├── AuthService.logoutAll() — logout all sessions
├── AuthController (HTTP layer — @PreAuthorize, mock service)
└── Integration (Testcontainers PostgreSQL)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-04` | Logout flow, partial vs all |
| `ADR-AUTH-009` | Partial + all logout behavior |
| `ADR-AUTH-010` | Stateless access token — no blacklist |
| `BR-LOGOUT-001` | TOKEN_REVOKED event |
| `BR-LOGOUT-002` | Partial: only current session |
| `BR-LOGOUT-003` | Logout-all: all sessions |
| `BR-LOGOUT-004` | Invalid refresh → reject |
| `BR-LOGOUT-006` | Revoked token → reject |
| `BR-LOGOUT-007` | Session ownership check |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid token, partial logout → revoke 1 session | `AuthService.logoutSingle()` | `LOGOUT-TC-001` |
| TC-COND-002 | Logout-all → revoke N sessions | `AuthService.logoutAll()` | `LOGOUT-TC-002` |
| TC-COND-003 | Revoked token reuse → AUTH-021 | `SessionRepository.findByHash()` check | `LOGOUT-TC-003` |
| TC-COND-004 | Session không tìm thấy → AUTH-023 | `findByRefreshTokenHash()` empty | `LOGOUT-TC-004` |
| TC-COND-005 | Session ownership mismatch → AUTH-022 | `validateSessionOwnership()` | `LOGOUT-TC-005` |
| TC-COND-006 | Không có access token → AUTH-020 | `@PreAuthorize` | `LOGOUT-TC-006` |
| TC-COND-007 | TOKEN_REVOKED event ghi đúng | `eventPublisher.publishEvent()` | `LOGOUT-TC-007` |
| TC-COND-008 | Access token vẫn valid sau logout (documented) | Stateless JWT | `LOGOUT-TC-008` |
| TC-COND-009 | Partial: session khác không bị ảnh hưởng | `revokeById()` isolation | `LOGOUT-TC-009` |
| TC-COND-010 | Integration: logout → refresh rejected | Full flow | `LOGOUT-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | Session: ACTIVE → REVOKED | Kiểm tra chuyển tiếp trạng thái |
| Boundary Value Analysis | revokedCount: 0, 1, N sessions | Logout-all với số sessions khác nhau |
| Error Guessing | Session ownership, replay attack | Security attack vectors |
| Equivalence Partitioning | partial vs all logout | 2 luồng logout chính |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-LOGOUT-001` | DB seed | `Session{userId:"u-001", revoked:false, hash: SHA256("token-A")}` | Partial logout happy path |
| `FX-LOGOUT-002` | DB seed | `3 Sessions{userId:"u-001", revoked:false}` | Logout-all happy path |
| `FX-LOGOUT-003` | DB seed | `Session{userId:"u-001", revoked:true}` | Revoked session replay |
| `FX-LOGOUT-004` | Input | `refreshToken: "non-existent-token"` | Session not found |
| `FX-LOGOUT-005` | DB seed | `Session{userId:"u-002", revoked:false}` — user u-001 tries to revoke | Ownership violation |
| `FX-LOGOUT-006` | Auth | `AccessToken for u-001` | Authenticated request |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// LogoutTestFactory.java
private static UserSession makeSession(String userId, boolean revoked) {
    UserSession session = new UserSession();
    session.setId(UUID.randomUUID());
    session.setUserId(UUID.fromString(userId));
    session.setRefreshTokenHash(DigestUtils.sha256Hex(UUID.randomUUID().toString()));
    session.setDeviceInfo("test-device");
    session.setIpAddress("127.0.0.1");
    session.setExpiresAt(LocalDateTime.now().plusDays(7));
    session.setRevoked(revoked);
    return session;
}

private static String makeRefreshTokenForSession(UserSession session) {
    // Create a token whose SHA-256 hash matches session.refreshTokenHash
    // In tests, we store the raw token and compute hash
    String rawToken = "test-refresh-token-" + session.getId();
    // session.setRefreshTokenHash(DigestUtils.sha256Hex(rawToken)); -- set externally
    return rawToken;
}
```

---

### LOGOUT-TC-001 — Partial logout thành công

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.logoutSingle()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLogoutTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-LOGOUT-002`, `ADR-AUTH-009`

**Preconditions:**
- `FX-LOGOUT-001`: Session A, revoked=false, hash=SHA256("token-A")
- `SessionRepository` mock: `findByRefreshTokenHash(SHA256("token-A"))` → FX-LOGOUT-001
- Authenticated user: u-001 (matches session.userId)

**Test Steps:**
1. **Arrange:** `new LogoutRequestDTO("token-A", false)`, userId=u-001
2. **Act:** `authService.logout(request, u-001-UUID, "127.0.0.1")`
3. **Assert:**
   - `LogoutResponseDTO.revokedCount()` == 1
   - `sessionRepository.revokeById(session-A-UUID)` được gọi đúng 1 lần
   - `sessionRepository.revokeAllByUserId()` KHÔNG được gọi
   - `eventPublisher` được gọi với `UserLoggedOutEvent{logoutAll=false, revokedCount=1}`

**Expected Result (PASS):**
- 1 session revoked, partial, event published

**Expected Result (FAIL):**
- Nếu revokeAllByUserId() được gọi → không phải partial logout

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-002 — Logout-all thu hồi tất cả sessions của user

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthService.logoutAll()`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLogoutTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-LOGOUT-003`, `ADR-AUTH-009`

**Preconditions:**
- `FX-LOGOUT-002`: 3 active sessions cho user u-001
- `sessionRepository.revokeAllByUserId(u-001)` → returns 3

**Test Steps:**
1. **Arrange:** `new LogoutRequestDTO("token-A", true)`, userId=u-001
2. **Act:** `authService.logout(request, u-001, "127.0.0.1")`
3. **Assert:**
   - `LogoutResponseDTO.revokedCount()` == 3
   - `sessionRepository.revokeAllByUserId(u-001)` được gọi đúng 1 lần
   - `eventPublisher` với `UserLoggedOutEvent{logoutAll=true, revokedCount=3}`
   - `sessionRepository.revokeById()` KHÔNG được gọi riêng lẻ

**Expected Result (PASS):**
- Tất cả 3 sessions revoked

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-003 — Từ chối refresh token đã revoked (replay)

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-294 — Authentication Bypass by Capture-replay`
**Feature Under Test:** `AuthService.logoutSingle()` — revoked check
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLogoutTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-LOGOUT-006`

**Preconditions:**
- `FX-LOGOUT-003`: Session với revoked=true

**Test Steps:**
1. **Arrange:** LogoutRequestDTO với token tương ứng session đã revoked
2. **Act:** `authService.logout(request, userId, ip)` — expect exception
3. **Assert:**
   - Throws `AuthenticationException` với code `AUTH-021`
   - `sessionRepository.revokeById()` KHÔNG được gọi (không revoke lại)
   - `eventPublisher` KHÔNG gọi `UserLoggedOutEvent` (không audit double-logout)

**Expected Result (PASS = an toàn):**
- 401 AUTH-021, không có side effect

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-004 — Session không tìm thấy trong DB

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.logoutSingle()` — token lookup
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLogoutTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-LOGOUT-004`

**Preconditions:**
- `sessionRepository.findByRefreshTokenHash(any)` → Optional.empty()

**Test Steps:**
1. **Act:** `authService.logout(new LogoutRequestDTO("non-existent", false), userId, ip)`
2. **Assert:**
   - Throws `ResourceNotFoundException` với code `AUTH-023`

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-005 — Session ownership: user A không thể logout session của user B

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `AuthService.validateSessionOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/auth/LogoutSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-LOGOUT-007`

**Preconditions:**
- `FX-LOGOUT-005`: Session thuộc về u-002, revoked=false
- Request authenticated as u-001

**Test Steps (Attack Simulation):**
1. `sessionRepository.findByRefreshTokenHash()` → returns session{userId=u-002}
2. `authService.logout(request, u-001-UUID, ip)`
3. **Assert:**
   - Throws `AuthorizationException` với code `AUTH-022`
   - `sessionRepository.revokeById()` KHÔNG được gọi
   - Session của u-002 vẫn active

**Expected Result (PASS = an toàn):**
- 403 AUTH-022, session của u-002 intact

**Expected Result (FAIL = lỗ hổng):**
- Session bị revoke → attacker có thể kick out bất kỳ user

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-006 — Từ chối request không có access token (401)

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthController.logout()` — `@PreAuthorize("isAuthenticated()")`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthControllerLogoutTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-RBAC`

**Test Steps:**
1. **Act:** POST `/api/v1/auth/logout` WITHOUT Authorization header
2. **Assert:**
   - HTTP 401
   - Error code `AUTH-020`
   - `authService.logout()` KHÔNG được gọi

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-007 — SecurityEvent TOKEN_REVOKED được ghi đúng

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.logoutSingle()` → `eventPublisher`
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLogoutTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-LOGOUT-001`, `SecurityEventType.TOKEN_REVOKED`

**Preconditions:**
- Happy path setup (FX-LOGOUT-001)
- `ApplicationEventPublisher` mock: capture events

**Test Steps:**
1. **Act:** `authService.logout(validRequest, userId, "10.0.0.1")`
2. Capture published event
3. **Assert:**
   - Exactly 1 event published
   - `event.payload().securityEventType()` == `"TOKEN_REVOKED"`
   - `event.payload().userId()` == authenticatedUserId
   - `event.payload().ipAddress()` == `"10.0.0.1"`
   - `event.payload().logoutAll()` == false

**Expected Result (FAIL):**
- Nếu event không published → vi phạm BR-LOGOUT-001 (audit requirement)

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-008 — Access token vẫn valid sau logout (documented trade-off)

**Severity:** `MEDIUM`
**Feature Under Test:** JWT stateless validation (documented behavior, NOT a bug)
**Test File:** `src/test/java/com/carebridge/backend/auth/LogoutStatelessJwtTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-AUTH-010` — documented trade-off

**Preconditions:**
- User logged in, access token valid (TTL: 900s)
- User logged out (session revoked)

**Test Steps:**
1. Logout thành công (session revoked)
2. Gọi protected endpoint với access token đã logout
3. **Assert:**
   - HTTP 200 (access token vẫn valid — stateless JWT)
   - NOTE: Đây là EXPECTED behavior per ADR-AUTH-010, không phải bug

**Expected Result (PASS = documented behavior):**
- Access token valid cho đến khi hết hạn (≤ 15 phút sau logout)

**Implementation Note:**
```
// Test này document behavior, không phải bug.
// Comment rõ trong test: "Per ADR-AUTH-010: access token stateless, no blacklist"
// Window: tối đa 15 phút sau logout
```

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-009 — Partial logout: session khác không bị ảnh hưởng

**Severity:** `HIGH`
**Feature Under Test:** `SessionRepository.revokeById()` isolation
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceLogoutTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-LOGOUT-002`

**Preconditions:**
- User u-001 có 3 sessions: session-A, session-B, session-C
- Logout request cho session-A

**Test Steps:**
1. **Act:** `authService.logout(requestForSessionA, userId, ip)`
2. **Assert:**
   - `sessionRepository.revokeById(session-A-ID)` được gọi
   - `sessionRepository.revokeById(session-B-ID)` KHÔNG được gọi
   - `sessionRepository.revokeById(session-C-ID)` KHÔNG được gọi
   - `revokedCount` == 1

**Current Status:** 🔴 Not written

---

### LOGOUT-TC-INT-001 — Integration: Logout → refresh token bị từ chối

**Severity:** `HIGH`
**Feature Under Test:** Full flow: UC-03 login → UC-04 logout → refresh rejected
**Test File:** `src/test/java/com/carebridge/backend/auth/LogoutIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainer
- Migrations V1, V2, V3 applied
- User seeded: ACTIVE

**Test Steps:**
1. `POST /api/v1/auth/login` → capture `accessToken`, `refreshToken`
2. `POST /api/v1/auth/logout` với `refreshToken`, `logoutAll: false`
3. `POST /api/v1/auth/token/refresh` với cùng `refreshToken`

**Expected Result (PASS):**
```java
// Step 2: logout
assertThat(logoutResponse.statusCode()).isEqualTo(200);

// Step 3: try to refresh with revoked token
assertThat(refreshResponse.statusCode()).isEqualTo(401);
// Extract error code
String errorCode = extractErrorCode(refreshResponse.body());
assertThat(errorCode).isEqualTo("AUTH-021");

// DB assertion
UserSession session = sessionRepository.findByRefreshTokenHash(
    DigestUtils.sha256Hex(refreshToken)).orElseThrow();
assertThat(session.isRevoked()).isTrue();
assertThat(session.getRevokedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `LOGOUT-TC-001` | `AuthServiceLogoutTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-002` | `AuthServiceLogoutTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-003` | `AuthServiceLogoutTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-004` | `AuthServiceLogoutTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-005` | `LogoutSecurityTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-006` | `AuthControllerLogoutTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-007` | `AuthServiceLogoutTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-008` | `LogoutStatelessJwtTest.java` | `[ ]` | `—` | ADR-AUTH-010 behavior |
| `LOGOUT-TC-009` | `AuthServiceLogoutTest.java` | `[ ]` | `—` | — |
| `LOGOUT-TC-INT-001` | `LogoutIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class AuthService implements IAuthService {
    @Override
    public LogoutResponseDTO logout(LogoutRequestDTO request, UUID userId, String ipAddress) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|----------|--------|----------------------------------|
| `LOGOUT-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `LOGOUT-TC-005` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `LOGOUT-TC-007` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `LOGOUT-TC-INT-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Đặc biệt — LOGOUT-TC-008:**
```
TC-008 test access token vẫn valid sau logout.
Với stub (throw), protected endpoint có thể vẫn return 200 nếu auth guard pass JWT.
→ Kiểm tra: TC-008 test phải FAIL về assertion rằng refresh bị reject (không phải access token).
```

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS CB-AUTH-IMP-004 đã approve
- [ ] UC-03 Login đã implement (user_sessions với revoked, revoked_at)
- [ ] `SecurityEventType.TOKEN_REVOKED` có trong enum
- [ ] `SessionRepository.revokeById()` và `revokeAllByUserId()` có sẵn

### Exit Criteria (DoD)

- [ ] Tất cả 10 test cases xanh
- [ ] Test coverage ≥ 80% trên `AuthService.logoutSingle()` và `logoutAll()`
- [ ] Không có `refreshToken` plain text xuất hiện trong logs
- [ ] Session records KHÔNG bị DELETE — chỉ UPDATE revoked=true (code review)
- [ ] LOGOUT-TC-005 (ownership) xanh — critical security test
- [ ] SecurityEvent TOKEN_REVOKED ghi đúng payload (LOGOUT-TC-007 green)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate confirmed: tất cả tests FAIL với stub
- [ ] ADR-AUTH-010 documented trong LOGOUT-TC-008 comment

### Suspension Criteria

- UC-03 Login chưa implement → block toàn bộ
- `user_sessions` table chưa có `revoked`, `revoked_at` columns → block migration

---

## 7. Rollback Plan

```bash
# Revert logout implementation
git checkout -- src/main/java/com/carebridge/backend/auth/service/AuthService.java
git checkout -- src/main/java/com/carebridge/backend/auth/controller/AuthController.java

# Không cần undo migration (UC-04 không thêm bảng mới)

# Nếu sessions bị revoke sai do bug:
# UPDATE user_sessions SET revoked=false, revoked_at=NULL
# WHERE revoked_at BETWEEN '[deploy_time]' AND '[rollback_time]';
# -- CẢNH BÁO: Chỉ dùng trong trường hợp khẩn cấp
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | LOGOUT-TC-007 không reference BR-LOGOUT-001 | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | LOGOUT-TC-001 PASS với stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code thêm token blacklist không có ADR-AUTH-010 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller chứa session revocation logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `TokenRevoker` không có trong §8 TDS | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| *(chưa phát hiện)* | — | — | — | — |

---

## Phụ lục: Cross-Feature Test Coverage Map

> Ánh xạ 4 UC để đảm bảo full auth flow được test end-to-end.

| Flow | UC-01 | UC-02 | UC-03 | UC-04 |
|------|-------|-------|-------|-------|
| Register → Verify → Login → Logout | ✅ (TC-INT-001) | ✅ (OTP-TC-INT-002) | ✅ (LOGIN-TC-INT-001) | ✅ (LOGOUT-TC-INT-001) |
| Login thất bại 5 lần → LOCKED | — | — | ✅ (LOGIN-TC-006) | — |
| Replay OTP sau verify | — | ✅ (OTP-TC-008) | — | — |
| Replay refresh token sau logout | — | — | — | ✅ (LOGOUT-TC-003) |
| Session ownership protection | — | — | — | ✅ (LOGOUT-TC-005) |
| Brute force protection | — | ✅ (OTP-TC-004) | ✅ (LOGIN-TC-006) | — |

---

*TDD Spec CB-AUTH-TEST-004 v1.0 — UC-04 Logout*
*Tuân theo EDS v2.0 + CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
