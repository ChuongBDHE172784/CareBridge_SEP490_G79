# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Mẫu Đặc tả Kiểm thử — UC-02 Verify OTP

**Document ID:** `CB-AUTH-TEST-002`
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
- `04_Implement/UC02_VerifyOTP/UC02_VerifyOTP_TDS.md` (CB-AUTH-IMP-002 v1.0)
- `04_Implement/UC01_RegisterAccount/UC01_RegisterAccount_TDS.md` (CB-AUTH-IMP-001)
- `ADR-AUTH-004`, `ADR-AUTH-005`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-02 Verify OTP |

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
| **Feature / Gap ID** | `UC-02` |
| **Module** | `VerifyOTP — auth` |
| **Spec gốc** | `CB-AUTH-IMP-002` |
| **Priority** | 🔴 P0 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M1 Alpha — Auth Module` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-SECURITY, PDPA` |
| **Upstream Dependencies** | `UC-01 RegisterAccount (otp_records table)` |
| **Downstream Consumers** | `UC-03 Login, AuditService` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-AUTH-IMP-002 §17`, `ADR-AUTH-004`, `ADR-AUTH-005` |
| **Constraints Injected** | constant-time-compare, pessimistic-lock, attempt-increment-order, security-event |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Không rõ thứ tự: tăng attempt trước hay kiểm tra limit trước | Policy: increment → save → check limit | Test verify attemptCount tăng trước khi throw exception |
| L2 | Spec không đề cập timing attack | ADR-AUTH-005: PHẢI dùng MessageDigest.isEqual() | Test verify không dùng String.equals() (code review check) |
| L3 | Không rõ khi OTP expired có tăng attemptCount không | Policy: KHÔNG tăng cho expired OTP (lỗi TTL ≠ wrong code) | Test verify expired → AUTH-007, attemptCount không thay đổi |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
VerifyOTP bao gồm các layer:
├── Domain (OtpRecord entity, OtpStatus logic)
├── OtpService (verify logic — mock repository)
├── AuthService.verifyOtp() (orchestration — mock OtpService)
├── AuthController (HTTP layer — mock service)
└── Integration (Testcontainers PostgreSQL + transaction lock test)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-02` | OTP verify flow, activation |
| `ADR-AUTH-004` | 6-digit, 10-min TTL, max-5 |
| `ADR-AUTH-005` | Pessimistic lock, constant-time compare |
| `BR-OTP-001` | TTL = 10 phút |
| `BR-OTP-002` | Max 5 attempts → lockout |
| `BR-OTP-003` | OTP không tái sử dụng |
| `BR-OTP-004` | Account ACTIVE khi verify OK |
| `BR-OTP-005` | SecurityEvent khi limit exceeded |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | OTP đúng + chưa hết hạn + chưa dùng → ACTIVE | `OtpService.verifyOtp()` | `OTP-TC-001` |
| TC-COND-002 | OTP hết hạn → AUTH-007 | `OtpService.isExpired()` | `OTP-TC-002` |
| TC-COND-003 | OTP sai → tăng attemptCount, AUTH-007 | `OtpService.verifyOtp()` | `OTP-TC-003` |
| TC-COND-004 | attemptCount = 4, sai thêm → 5 → AUTH-008 + SecurityEvent | `OtpService.checkAttemptLimit()` | `OTP-TC-004` |
| TC-COND-005 | OTP đã dùng (used=true) → AUTH-007 | `OtpRecord.used` | `OTP-TC-005` |
| TC-COND-006 | userId không tồn tại → AUTH-006 | `AuthService.verifyOtp()` | `OTP-TC-006` |
| TC-COND-007 | Account đã ACTIVE → AUTH-009 | `AuthService.verifyOtp()` | `OTP-TC-007` |
| TC-COND-008 | Replay: dùng lại OTP đã verify → AUTH-007 | Security | `OTP-TC-008` |
| TC-COND-009 | Race condition: 2 requests cùng OTP | Pessimistic lock | `OTP-TC-INT-001` |
| TC-COND-010 | Full integration: verify → DB ACTIVE | Full flow | `OTP-TC-INT-002` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | OTP states: VALID → USED/EXPIRED/LOCKED | Kiểm tra mọi chuyển tiếp trạng thái |
| Boundary Value Analysis | attemptCount: 4→5 (boundary), TTL: 9m59s / 10m01s | Kiểm tra ranh giới |
| Error Guessing | Replay attack, race condition | Security vectors |
| Equivalence Partitioning | Valid/Invalid/Expired OTP | Phân nhóm |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-OTP-001` | DB seed | `{userId:"u-001", code:"123456", used:false, expiresAt:+5min, attemptCount:0}` | Happy path |
| `FX-OTP-002` | DB seed | `{userId:"u-002", code:"123456", used:false, expiresAt:-15min, attemptCount:0}` | Expired OTP |
| `FX-OTP-003` | DB seed | `{userId:"u-003", code:"123456", used:false, expiresAt:+5min, attemptCount:4}` | One away from lockout |
| `FX-OTP-004` | DB seed | `{userId:"u-004", code:"123456", used:true, expiresAt:+5min}` | Already used |
| `FX-OTP-005` | DB seed | `{userId:"u-005", status:"ACTIVE"}` | Already active user |
| `FX-OTP-006` | Clock | `fixed clock at 2026-06-26T10:00:00Z` | Deterministic time test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// OtpTestFactory.java
private static OtpRecord makeOtpRecord(
        String userId, String code, boolean used,
        LocalDateTime expiresAt, int attemptCount) {
    OtpRecord otp = new OtpRecord();
    otp.setId(UUID.randomUUID());
    otp.setUserId(UUID.fromString(userId));
    otp.setOtpCode(code != null ? code : "123456");
    otp.setUsed(used);
    otp.setExpiresAt(expiresAt != null ? expiresAt : LocalDateTime.now().plusMinutes(5));
    otp.setAttemptCount(attemptCount);
    otp.setCreatedAt(LocalDateTime.now());
    return otp;
}
```

---

### OTP-TC-001 — Xác minh OTP đúng → tài khoản ACTIVE

**Severity:** `CRITICAL`
**Feature Under Test:** `OtpService.verifyOtp()` + `AuthService.verifyOtp()`
**Test File:** `src/test/java/com/carebridge/backend/auth/OtpServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-OTP-004`, `UC-02 Happy Path`

**Preconditions:**
- `FX-OTP-001`: OTP record hợp lệ (code="123456", used=false, expires+5min, attempt=0)
- `UserRepository` mock: findById → User{status=UNVERIFIED}
- `OtpRepository` mock: findActiveByUserIdWithLock → FX-OTP-001

**Test Steps:**
1. **Arrange:** Mock setup như trên
2. **Act:** `authService.verifyOtp(new VerifyOtpRequestDTO(userId, "123456"))`
3. **Assert:**
   - Kết quả `VerifyOtpResponseDTO.status()` == `"ACTIVE"`
   - `userRepository.save()` được gọi với user.status = ACTIVE
   - `otpRepository.save()` được gọi với otp.used = true
   - Event `AccountActivated` được publish

**Expected Result (PASS):**
- status = "ACTIVE", OTP marked as used, account activated

**Current Status:** 🔴 Not written

---

### OTP-TC-002 — Từ chối OTP hết hạn (expired)

**Severity:** `HIGH`
**Feature Under Test:** `OtpService.isExpired()`
**Test File:** `src/test/java/com/carebridge/backend/auth/OtpServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-OTP-001`, `ADR-AUTH-004`

**Preconditions:**
- `FX-OTP-002`: OTP với expiresAt = -15 phút (đã hết hạn)

**Test Steps:**
1. **Act:** `otpService.verifyOtp(userId, "123456")`
2. **Assert:**
   - Throws `ValidationException` với code `AUTH-007`
   - `attemptCount` KHÔNG thay đổi (L3 Logic Issue)
   - User status vẫn UNVERIFIED

**Expected Result (PASS):**
- AUTH-007, no attemptCount increment for expired OTP

**Expected Result (FAIL):**
- Nếu attemptCount tăng → L3 logic issue không được fix

**Current Status:** 🔴 Not written

---

### OTP-TC-003 — OTP sai → tăng attemptCount, trả AUTH-007

**Severity:** `HIGH`
**Feature Under Test:** `OtpService.verifyOtp()` — mismatch path
**Test File:** `src/test/java/com/carebridge/backend/auth/OtpServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-OTP-002`

**Preconditions:**
- `FX-OTP-001` với attemptCount=0

**Test Steps:**
1. **Act:** `otpService.verifyOtp(userId, "000000")` (sai)
2. **Assert:**
   - Throws `ValidationException` với code `AUTH-007`
   - Message chứa "Còn 4 lần thử"
   - `otpRepository.save()` được gọi với `attemptCount = 1`

**Expected Result (PASS):**
- attemptCount++ → 1, remaining = 4

**Current Status:** 🔴 Not written

---

### OTP-TC-004 — Vượt giới hạn 5 lần → AUTH-008 + SecurityEvent

**Severity:** `CRITICAL`
**Feature Under Test:** `OtpService.verifyOtp()` — attempt limit
**Test File:** `src/test/java/com/carebridge/backend/auth/OtpServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-OTP-002`, `BR-OTP-005`

**Preconditions:**
- `FX-OTP-003`: attemptCount = 4 (lần thử thứ 5 sẽ trigger lockout)
- `ApplicationEventPublisher` mock: capture events

**Test Steps:**
1. **Act:** `otpService.verifyOtp(userId, "000000")` (sai)
2. **Assert:**
   - Throws `RateLimitExceededException` với code `AUTH-008`
   - `otpRepository.save()` với attemptCount = 5
   - `eventPublisher.publishEvent()` được gọi với event chứa `securityEventType = "OTP_ATTEMPT_LIMIT_EXCEEDED"`

**Expected Result (PASS):**
- AUTH-008 thrown, SecurityEvent published

**Expected Result (FAIL):**
- Nếu SecurityEvent không được publish → vi phạm BR-OTP-005 (audit requirement)

**Current Status:** 🔴 Not written

---

### OTP-TC-005 — Từ chối OTP đã được dùng (used=true)

**Severity:** `HIGH`
**Feature Under Test:** `OtpService.verifyOtp()` — replay check
**Test File:** `src/test/java/com/carebridge/backend/auth/OtpServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-OTP-003`

**Preconditions:**
- `FX-OTP-004`: OTP với used=true

**Test Steps:**
1. **Act:** `otpService.verifyOtp(userId, "123456")`
2. **Assert:**
   - Throws `ValidationException` với code `AUTH-007`
   - Message chứa "đã được sử dụng"

**Current Status:** 🔴 Not written

---

### OTP-TC-006 — userId không tồn tại → AUTH-006

**Severity:** `HIGH`
**Feature Under Test:** `AuthService.verifyOtp()` — userId validation
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC-02 Error Path`

**Preconditions:**
- `userRepository.findById("non-existent-id")` → Optional.empty()

**Test Steps:**
1. **Act:** `authService.verifyOtp(new VerifyOtpRequestDTO(nonExistentUUID, "123456"))`
2. **Assert:**
   - Throws `ResourceNotFoundException` với code `AUTH-006`

**Current Status:** 🔴 Not written

---

### OTP-TC-007 — Tài khoản đã ACTIVE → AUTH-009

**Severity:** `MEDIUM`
**Feature Under Test:** `AuthService.verifyOtp()` — status check
**Test File:** `src/test/java/com/carebridge/backend/auth/AuthServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-OTP-004`

**Preconditions:**
- `FX-OTP-005`: User với status=ACTIVE

**Test Steps:**
1. **Act:** `authService.verifyOtp(request)`
2. **Assert:**
   - Throws `ValidationException` với code `AUTH-009`
   - `otpService.verifyOtp()` KHÔNG được gọi (early return)

**Current Status:** 🔴 Not written

---

### OTP-TC-008 — Replay Attack: Dùng lại OTP đã verify

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-294 — Authentication Bypass by Capture-replay`
**Feature Under Test:** `OtpRecord.used` flag check
**Test File:** `src/test/java/com/carebridge/backend/auth/OtpSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-OTP-003`, `ADR-AUTH-005`

**Test Steps (Attack Simulation):**
1. **Step 1:** Verify OTP thành công (account → ACTIVE, otp.used = true)
2. **Step 2:** Gửi lại cùng OTP code
3. **Assert:**
   - Second request: HTTP 400, code AUTH-007
   - Account status không bị reset

**Expected Result (PASS = hệ thống an toàn):**
- Second attempt → 400, used flag prevents replay

**Current Status:** 🔴 Not written

---

### OTP-TC-INT-001 — Race Condition: 2 requests verify cùng OTP đồng thời

**Severity:** `CRITICAL`
**Feature Under Test:** `OtpRepository` pessimistic lock
**Test File:** `src/test/java/com/carebridge/backend/auth/OtpRaceConditionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-AUTH-005`

**Preconditions:**
- PostgreSQL Testcontainer
- 1 OTP record, used=false
- 2 concurrent threads

**Test Steps:**
1. Tạo 2 CompletableFuture gọi `authService.verifyOtp()` đồng thời
2. Collect kết quả
3. **Assert:**
   - Đúng 1 request thành công (200/ACTIVE)
   - Request kia nhận AUTH-007 (used=true)
   - DB: otp_records.used = true, users.status = ACTIVE (1 lần)

**Expected Result (PASS):**
- Chỉ 1 activation, không có duplicate

**DB Assertion:**
```java
// After concurrent execution
long activatedCount = userRepository.findAll().stream()
    .filter(u -> u.getStatus() == AccountStatus.ACTIVE)
    .count();
assertThat(activatedCount).isEqualTo(1);

OtpRecord otp = otpRepository.findLatestByUserId(userId).orElseThrow();
assertThat(otp.isUsed()).isTrue();
```

**Current Status:** 🔴 Not written

---

### OTP-TC-INT-002 — Integration: Full flow verify → DB state

**Severity:** `HIGH`
**Feature Under Test:** Full flow: OTP verify → ACTIVE
**Test File:** `src/test/java/com/carebridge/backend/auth/VerifyOtpIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainer
- User seeded với status=UNVERIFIED
- OTP record seeded: code="123456", used=false, expires+5min

**Test Steps:**
1. POST `/api/v1/auth/verify-otp` với `{userId, otpCode:"123456"}`
2. Query DB

**Expected Result (PASS):**
```java
User user = userRepository.findById(userId).orElseThrow();
assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);

OtpRecord otp = otpRepository.findLatestByUserId(userId).orElseThrow();
assertThat(otp.isUsed()).isTrue();
assertThat(otp.getAttemptCount()).isEqualTo(0);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `OTP-TC-001` | `OtpServiceTest.java` | `[ ]` | `—` | — |
| `OTP-TC-002` | `OtpServiceTest.java` | `[ ]` | `—` | — |
| `OTP-TC-003` | `OtpServiceTest.java` | `[ ]` | `—` | — |
| `OTP-TC-004` | `OtpServiceTest.java` | `[ ]` | `—` | — |
| `OTP-TC-005` | `OtpServiceTest.java` | `[ ]` | `—` | — |
| `OTP-TC-006` | `AuthServiceTest.java` | `[ ]` | `—` | — |
| `OTP-TC-007` | `AuthServiceTest.java` | `[ ]` | `—` | — |
| `OTP-TC-008` | `OtpSecurityTest.java` | `[ ]` | `—` | — |
| `OTP-TC-INT-001` | `OtpRaceConditionIntegrationTest.java` | `[ ]` | `—` | — |
| `OTP-TC-INT-002` | `VerifyOtpIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class OtpService implements IOtpService {
    @Override
    public boolean verifyOtp(UUID userId, String otpCode) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public void generateAndSend(UUID userId, String channel) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|----------|--------|----------------------------------|
| `OTP-TC-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `OTP-TC-004` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `OTP-TC-INT-001` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS CB-AUTH-IMP-002 đã được approve
- [ ] UC-01 đã implement (otp_records table tồn tại)
- [ ] Logic Issues L1, L2, L3 đã được confirm
- [ ] `SecurityEventType.OTP_ATTEMPT_LIMIT_EXCEEDED` có trong enum

### Exit Criteria (DoD)

- [ ] Tất cả 10 test cases xanh
- [ ] Test coverage ≥ 80% trên `OtpService`, `AuthService.verifyOtp()`
- [ ] OtpCode KHÔNG xuất hiện trong logs
- [ ] Pessimistic lock được dùng — verified qua code review
- [ ] Race condition test (`OTP-TC-INT-001`) xanh
- [ ] SecurityEvent ghi đúng khi attempt limit đạt

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate** — tất cả tests FAIL với stub
- [ ] **Oracle Source** — mọi expected value có ghi nguồn BR/ADR
- [ ] `MessageDigest.isEqual()` được dùng — không phải `String.equals()`

---

## 7. Rollback Plan

```bash
# Revert OtpService và AuthService changes
git checkout -- src/main/java/com/carebridge/backend/auth/service/OtpService.java
git checkout -- src/main/java/com/carebridge/backend/auth/service/AuthService.java

# Không cần rollback migration (UC-02 không thêm bảng mới)
# UC-02 vẫn OPEN trong backlog
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference BR-OTP-* | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | OTP-TC-001 PASS với stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume String.equals() cho OTP compare | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify Controller có OTP validation logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `OtpVerifier` không có trong §8 TDS | ☐ | G-3 |

---

*TDD Spec CB-AUTH-TEST-002 v1.0 — UC-02 Verify OTP*
*Tuân theo EDS v2.0 + CASE 2.0*
