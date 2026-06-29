# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-16: Manage Own Sessions

| Field              | Value                                                              |
| ------------------ | ------------------------------------------------------------------ |
| **Document ID**    | `CB-SES-TDD-001`                                                   |
| **Version**        | `1.0`                                                              |
| **Date**           | `2026-06-26`                                                       |
| **Status**         | `Approved`                                                         |
| **Spec gốc**       | `CB-SES-IMP-001` (UC16_ManageOwnSessions_TDS.md)                   |
| **Standard**       | ISO/IEC/IEEE 29119-3:2021                                          |
| **Author**         | `AI Agent`                                                         |
| **Reviewed by**    | `[ ] [Tech Lead] — Pending`                                        |
| **DPO Sign-off**   | `[ ] Pending`                                                      |
| **Approved by**    | `[ ] Pending`                                                      |
| **Classification** | `Internal`                                                         |

**References:**
- `CB-SES-IMP-001` — TDS UC-16 Manage Own Sessions
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java`
- `ADR-003` (Pagination max 50), `ADR-004` (Revoke current via Logout only)
- `SecurityEventType.TOKEN_REVOKED`

> **Quy ước TDD:** Viết test TRƯỚC khi sửa production code.
> Thứ tự: viết test → FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh. Test data dùng SYNTHETIC.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                           |
| ---------- | --------------- | ----------------------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo TDD spec cho UC-16 Manage Own Sessions             |

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

| Field                     | Value                                                                              |
| ------------------------- | ---------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-16`                                                                            |
| **Module**                | `identity/auth — SessionService`                                                   |
| **Spec gốc**              | `CB-SES-IMP-001`                                                                   |
| **Priority**              | 🟠 P1                                                                               |
| **Sprint**                | `S1 (2026-06-26 → 2026-07-11)`                                                     |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                            |
| **Data Classification**   | `Internal`                                                                         |
| **Compliance Scope**      | `PDPA Luật 91/2025 Điều 7; GDPR Art. 7.3`                                          |
| **Upstream Dependencies** | `security (JwtAuthenticationToken, JwtTokenProvider)`, `audit (AuditService, SecurityEventService)` |
| **Downstream Consumers**  | `security (token validation — TokenBlacklist lookup)`                              |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                  |
| ------------------------ | ------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                  |
| **Constraint Source**    | `CB-SES-IMP-001 §17`, `ADR-003`, `ADR-004`                                                            |
| **Constraints Injected** | C1: userId từ JWT, C2: own-resource, C3: no revoke current, C4: blacklist in-transaction, C5: TOKEN_REVOKED event |
| **Model**                | `claude-sonnet-4-6`                                                                                    |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                           |

---

## 2. Logic Issues Resolved

| #  | Spec gốc (sai / thiếu)                                            | Thực tế (schema / codebase)                                                                 | Fix áp dụng trong test                                               |
| -- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| L1 | `getActiveSessions()` trả về `List<SessionInfo>` không phân trang | UC-16 yêu cầu pagination, max 50. Cần thêm overload `getActiveSessionsPaged(userId, Pageable)` | Test verify `PaginatedResponse<SessionInfo>` được trả về           |
| L2 | `user_sessions` schema V1 thiếu `browser`, `location`, `last_activity_at` | `SessionServiceImpl.getActiveSessions()` map `session.getBrowser()` — cần verify field tồn tại | Test mock UserSession với `browser` field; migration V5 cần trước  |
| L3 | `SecurityEventService.record(TOKEN_REVOKED)` chưa được gọi trong `revokeSession()` | Spec yêu cầu `TOKEN_REVOKED` phải được emit; hiện tại chỉ có `auditService.log(SESSION_REVOKED)` | Test verify `securityEventService.record(TOKEN_REVOKED, ...)` được gọi |
| L4 | Không có `DELETE /api/v1/sessions` (revoke-all) endpoint         | Chỉ có `DELETE /api/v1/sessions/{sessionId}`. UC-16 cần thêm endpoint mới                  | Test TC-SES-007 cover `revokeAllExceptCurrent()` mới                |
| L5 | Error code khi revoke current session là generic `IllegalArgumentException` | Cần chuẩn hóa thành error code `SES-005` trong API response                                | Test verify response body có `"code": "SES-005"` không phải 500    |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Session module (com.carebridge.backend.identity) bao gồm các layer:
├── Controller    (SessionController — mock ISessionService)
├── Service       (SessionServiceImpl — mock Repository + SecurityEventService + AuditService)
├── Repository    (UserSessionRepository — Testcontainers PostgreSQL)
└── Integration   (Full stack: Controller → Service → Repository → DB + TokenBlacklist)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source             | Items Derived                                                              |
| ------------------ | -------------------------------------------------------------------------- |
| `UC-16 (SRS 3.1.1.16)` | List sessions, revoke single, revoke all except current            |
| `BR-PRIVACY`       | Own-resource: userId JWT == session.userId                                  |
| `BR-SES-CURRENT`   | Cannot revoke current session via DELETE /sessions/{id}                    |
| `BR-SES-EXPIRE`    | Session > 30 days inactive → treated as expired                            |
| `BR-SES-PAGINATION`| Max 50 sessions per page                                                   |
| `BR-SES-TOKEN`     | Revoke → blacklist refresh token in same transaction                       |
| `BR-SES-AUDIT`     | `SecurityEventType.TOKEN_REVOKED` emitted on revoke                        |
| `ADR-003`          | Pagination chuẩn: `PaginatedResponse<SessionInfo>`                         |
| `ADR-004`          | Current session detection via JWT `sid` claim                              |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID  | Test Condition                                             | Coverage Item                                          | Test Cases        |
| ------------- | ---------------------------------------------------------- | ------------------------------------------------------ | ----------------- |
| TC-COND-001   | size <= 50 → trả về đúng PaginatedResponse                 | `SessionServiceImpl.getActiveSessionsPaged()`          | `SES-TC-001`      |
| TC-COND-002   | size > 50 → throw SES-001                                  | `SessionServiceImpl.getActiveSessionsPaged()`          | `SES-TC-002`      |
| TC-COND-003   | Revoke session của chính mình → thành công                 | `SessionServiceImpl.revokeSession()`                   | `SES-TC-003`      |
| TC-COND-004   | Revoke session của người khác → SES-004                    | `SessionServiceImpl.revokeSession()` — ownership check | `SES-TC-004`      |
| TC-COND-005   | Revoke current session → SES-005                           | `SessionServiceImpl.revokeSession()` — current check   | `SES-TC-005`      |
| TC-COND-006   | Revoke already-revoked session → SES-002                   | `SessionServiceImpl.revokeSession()`                   | `SES-TC-006`      |
| TC-COND-007   | Revoke-all-except-current → bulk revoke thành công         | `SessionServiceImpl.revokeAllExceptCurrent()`          | `SES-TC-007`      |
| TC-COND-008   | TOKEN_REVOKED SecurityEvent được emit sau revoke           | `SecurityEventService.record(TOKEN_REVOKED, ...)`      | `SES-TC-008`      |
| TC-COND-009   | Integration: DELETE /sessions/{id} → DB revoked + blacklist| Full stack                                             | `SES-TC-INT-001`  |
| TC-COND-010   | isCurrent field đúng cho session hiện tại                  | `SessionServiceImpl.mapToSessionInfo()` + JWT sid      | `SES-TC-010`      |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4)  | Applied To                          | Rationale                                    |
| ------------------------ | ----------------------------------- | -------------------------------------------- |
| Boundary Value Analysis  | `size` param (49, 50, 51)           | Enforce max 50                               |
| State Transition Testing | ACTIVE → REVOKED                    | Session lifecycle                            |
| Error Guessing           | Race condition (double revoke)      | Concurrent request protection                |
| Use Case Testing         | Revoke-all-except-current           | UC-16 bulk operation                         |

### TDS-05 — Test Data Requirements

| Fixture ID | Type     | Value / Logic                                                                                              | Mục đích                       |
| ---------- | -------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------ |
| `FX-001`   | DB seed  | `user_sessions: {session_id: "ses-001", user_id: "user-ses-001", status: "active"}`                        | Current session                |
| `FX-002`   | DB seed  | `user_sessions: {session_id: "ses-002", user_id: "user-ses-001", status: "active"}`                        | Another session same user      |
| `FX-003`   | DB seed  | `user_sessions: {session_id: "ses-010", user_id: "user-ses-002", status: "active"}`                        | Another user's session         |
| `FX-004`   | DB seed  | `user_sessions: {session_id: "ses-003", status: "REVOKED", revoked_at: now()}`                              | Already revoked session        |
| `FX-005`   | JWT mock | `{sub: "user-ses-001", role: "ROLE_MOTHER", sid: "ses-001"}`                                               | Auth context (ses-001 current) |
| `FX-006`   | DB seed  | `user_sessions: 5 rows for user-ses-001 (ses-001 current, ses-002 to ses-005 active)`                      | Revoke-all test                |
| `FX-007`   | DB seed  | `user_sessions: session with last_activity_at = now() - 31 days (inactive/expired)`                        | 30-day expiry test             |

---

## 4. Test Case Specification

> **TC ID format:** `SES-TC-[NNN]`
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════════════

private static final UUID USER_ID         = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID OTHER_USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
private static final UUID CURRENT_SES_ID  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
private static final UUID OTHER_SES_ID    = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
private static final UUID REVOKED_SES_ID  = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

// Factory: fresh UserSession instance mỗi test
private UserSession buildActiveSession(UUID sessionId, UUID userId) {
    UserSession s = new UserSession();
    s.setSessionId(sessionId);
    s.setUserId(userId);
    s.setStatus("active");
    s.setDeviceName("Test Device");
    s.setBrowser("Test/1.0");
    s.setIpAddress("127.0.0.1");
    s.setRefreshTokenHash("hash-" + sessionId.toString().substring(0, 8));
    s.setExpiresAt(Instant.now().plusSeconds(3600));
    s.setLastActivityAt(Instant.now().minusSeconds(60));
    return s;
}
```

---

### SES-TC-001 — Lấy sessions phân trang thành công (Happy Path)

**Severity:** `HIGH`
**Feature Under Test:** `SessionServiceImpl.getActiveSessionsPaged(UUID, int, int)`
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-003` — pagination với size <= 50 phải trả về PaginatedResponse

**Preconditions:**
- User `USER_ID` có 5 active sessions trong mock repository
- `extractCurrentSessionId()` trả về `CURRENT_SES_ID`

**Test Steps (JUnit 5):**
```java
@Test
void getActiveSessionsPaged_validRequest_returnsPaginatedResponse() {
    // Arrange — 5 sessions, page 0, size 3
    List<UserSession> sessions = List.of(
        buildActiveSession(CURRENT_SES_ID, USER_ID),
        buildActiveSession(UUID.randomUUID(), USER_ID),
        buildActiveSession(UUID.randomUUID(), USER_ID),
        buildActiveSession(UUID.randomUUID(), USER_ID),
        buildActiveSession(UUID.randomUUID(), USER_ID)
    );
    Page<UserSession> sessionPage = new PageImpl<>(
        sessions.subList(0, 3),
        PageRequest.of(0, 3),
        5
    );
    when(sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(
        eq(USER_ID), any(Pageable.class))).thenReturn(sessionPage);

    // Act
    PaginatedResponse<SessionInfo> response = sessionService.getActiveSessionsPaged(USER_ID, 0, 3);

    // Assert
    assertNotNull(response);
    assertEquals(3, response.getData().size());
    assertEquals(5, response.getTotalElements());
    assertEquals(2, response.getTotalPages());

    // Verify isCurrent flag
    boolean hasCurrentSession = response.getData().stream()
        .anyMatch(s -> s.getSessionId().equals(CURRENT_SES_ID) && s.isCurrent());
    assertTrue(hasCurrentSession);
}
```

**Expected Result (PASS):** `totalElements = 5`, `data.size() = 3`, current session có `isCurrent = true`
**Expected Result (FAIL):** `NullPointerException` hoặc `data.size() != 3`
**Current Status:** 🔴 Not written

---

### SES-TC-002 — size > 50 → SES-001

**Severity:** `HIGH`
**Feature Under Test:** `SessionServiceImpl.getActiveSessionsPaged()` — size enforcement
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-003` — max size = 50; > 50 → SES-001

**Test Steps (JUnit 5):**
```java
@Test
void getActiveSessionsPaged_sizeExceeds50_throwsValidationException() {
    // Act & Assert
    ValidationException ex = assertThrows(ValidationException.class,
        () -> sessionService.getActiveSessionsPaged(USER_ID, 0, 51));

    assertEquals("SES-001", ex.getCode());
    verify(sessionRepository, never()).findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(
        any(), any(Pageable.class));
}

@Test
void getActiveSessionsPaged_sizeExactly50_succeeds() {
    // Boundary: size = 50 must NOT throw
    when(sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(
        eq(USER_ID), any(Pageable.class))).thenReturn(Page.empty());

    assertDoesNotThrow(() -> sessionService.getActiveSessionsPaged(USER_ID, 0, 50));
}
```

**Expected Result (PASS):** size=51 → `ValidationException("SES-001")`; size=50 → no exception
**Expected Result (FAIL):** size=51 được chấp nhận và query DB
**Current Status:** 🔴 Not written

---

### SES-TC-003 — Revoke session thành công (non-current)

**Severity:** `CRITICAL`
**Feature Under Test:** `SessionServiceImpl.revokeSession(UUID, UUID, String)`
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-PRIVACY` + `BR-SES-TOKEN` + `BR-SES-AUDIT`

**Preconditions:**
- `OTHER_SES_ID` thuộc `USER_ID`, đang active, không phải current
- `extractCurrentSessionId()` trả về `CURRENT_SES_ID` (khác `OTHER_SES_ID`)
- `sessionRepository.revokeSession()` trả về `1` (success)

**Test Steps (JUnit 5):**
```java
@Test
void revokeSession_validNonCurrentSession_revokesBothSessionAndToken() {
    // Arrange
    UserSession session = buildActiveSession(OTHER_SES_ID, USER_ID);
    when(sessionRepository.findById(OTHER_SES_ID)).thenReturn(Optional.of(session));
    when(sessionRepository.revokeSession(eq(OTHER_SES_ID), eq(USER_ID), any()))
        .thenReturn(1); // 1 row updated

    // Act
    assertDoesNotThrow(() -> sessionService.revokeSession(OTHER_SES_ID, USER_ID, "127.0.0.1"));

    // Assert — session revoked
    verify(sessionRepository, times(1)).revokeSession(eq(OTHER_SES_ID), eq(USER_ID), any());

    // Assert — token blacklisted in same call
    verify(tokenBlacklistRepository, times(1)).save(argThat(
        bl -> bl.getReason().equals("session_revoke")));

    // Assert — SecurityEvent TOKEN_REVOKED
    verify(securityEventService, times(1)).record(
        eq(SecurityEventType.TOKEN_REVOKED), eq(USER_ID), any(), any());

    // Assert — Audit log
    verify(auditService, times(1)).log(
        eq(AuditAction.SESSION_REVOKED), eq(USER_ID), eq("UserSession"),
        eq(OTHER_SES_ID.toString()), any());
}
```

**Expected Result (PASS):** 4 verify calls all pass; no exception
**Expected Result (FAIL):** TokenBlacklist not saved → session revoked but token still valid
**Current Status:** 🔴 Not written

---

### SES-TC-004 — Revoke session của người khác → SES-004

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `SessionServiceImpl.revokeSession()` — ownership enforcement
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-PRIVACY` — own-resource enforcement

**Test Steps (JUnit 5):**
```java
@Test
void revokeSession_sessionBelongsToOtherUser_throwsAuthorizationException() {
    // Arrange — session belongs to OTHER_USER_ID, but USER_ID is requesting
    UserSession otherUserSession = buildActiveSession(OTHER_SES_ID, OTHER_USER_ID);
    when(sessionRepository.findById(OTHER_SES_ID)).thenReturn(Optional.of(otherUserSession));

    // Act & Assert
    assertThrows(AuthorizationException.class,
        () -> sessionService.revokeSession(OTHER_SES_ID, USER_ID, "127.0.0.1"));

    // Verify: no DB modification, no blacklist, no audit
    verify(sessionRepository, never()).revokeSession(any(), any(), any());
    verify(tokenBlacklistRepository, never()).save(any());
    verify(securityEventService, never()).record(any(), any(), any(), any());
}
```

**Expected Result (PASS = hệ thống an toàn):** `AuthorizationException` thrown; no DB changes
**Expected Result (FAIL = lỗ hổng tồn tại):** Session của user khác bị revoke
**Current Status:** 🔴 Not written

---

### SES-TC-005 — Revoke current session → SES-005

**Severity:** `HIGH`
**Feature Under Test:** `SessionServiceImpl.revokeSession()` — current session detection
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-004` — current session phải dùng Logout

**Test Steps (JUnit 5):**
```java
@Test
void revokeSession_currentSession_throwsIllegalArgumentWithSES005Message() {
    // Arrange — CURRENT_SES_ID matches the session in the JWT sid claim
    UserSession currentSession = buildActiveSession(CURRENT_SES_ID, USER_ID);
    when(sessionRepository.findById(CURRENT_SES_ID)).thenReturn(Optional.of(currentSession));
    // extractCurrentSessionId() returns CURRENT_SES_ID (mocked via SecurityContext)

    // Act & Assert
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> sessionService.revokeSession(CURRENT_SES_ID, USER_ID, "127.0.0.1"));

    assertTrue(ex.getMessage().contains("Logout"));

    // Verify: no revoke happened
    verify(sessionRepository, never()).revokeSession(any(), any(), any());
}

@Test
void revokeCurrentSession_viaApi_returns400WithSES005() throws Exception {
    // Controller-level test: maps IllegalArgumentException("Logout") → SES-005
    doThrow(new IllegalArgumentException("Please use Logout to sign out from this device"))
        .when(sessionService).revokeSession(eq(CURRENT_SES_ID), eq(USER_ID), any());

    mockMvc.perform(delete("/api/v1/sessions/" + CURRENT_SES_ID)
            .with(mockJwt().jwt(j -> j.subject(USER_ID.toString())
                .claim("roles", List.of("ROLE_MOTHER")))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("SES-005"));
}
```

**Expected Result (PASS):** Exception thrown with Logout message; API returns 400 SES-005
**Expected Result (FAIL):** Current session gets revoked → user immediately loses access
**Current Status:** 🔴 Not written

---

### SES-TC-006 — Revoke đã-revoked session → SES-002

**Severity:** `MEDIUM`
**Feature Under Test:** `SessionServiceImpl.revokeSession()` — idempotency check
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-SES` — session đã revoked không được revoke lại

**Test Steps (JUnit 5):**
```java
@Test
void revokeSession_alreadyRevoked_throwsIllegalArgumentWithSES002() {
    // Arrange — session is already revoked
    UserSession revokedSession = buildActiveSession(REVOKED_SES_ID, USER_ID);
    revokedSession.setRevokedAt(Instant.now().minusSeconds(60));
    // isRevoked() returns true when revokedAt != null
    when(sessionRepository.findById(REVOKED_SES_ID)).thenReturn(Optional.of(revokedSession));

    // Act & Assert
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> sessionService.revokeSession(REVOKED_SES_ID, USER_ID, "127.0.0.1"));

    assertTrue(ex.getMessage().contains("already revoked"));
    verify(sessionRepository, never()).revokeSession(any(), any(), any());
}
```

**Expected Result (PASS):** Exception; no second revoke attempt
**Expected Result (FAIL):** DB gets a duplicate revoke UPDATE (wasted round-trip, potential audit duplication)
**Current Status:** 🔴 Not written

---

### SES-TC-007 — Revoke-all-except-current → tất cả session khác bị revoke

**Severity:** `HIGH`
**Feature Under Test:** `SessionServiceImpl.revokeAllExceptCurrent(UUID, UUID, String)`
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC-16 AC-003` — revoke all other sessions in one operation

**Preconditions:**
- User `USER_ID` có 4 sessions: `ses-001` (current), `ses-002`, `ses-003`, `ses-004`
- `sessionRepository.revokeAllExceptSession()` trả về `3`

**Test Steps (JUnit 5):**
```java
@Test
void revokeAllExceptCurrent_multipleActiveSessions_revokesBulk() {
    // Arrange
    when(sessionRepository.revokeAllExceptSession(
        eq(USER_ID), eq(CURRENT_SES_ID), any(Instant.class))).thenReturn(3);

    // Act
    int revokedCount = sessionService.revokeAllExceptCurrent(USER_ID, CURRENT_SES_ID, "127.0.0.1");

    // Assert — bulk revoke called exactly once (not N individual calls)
    assertEquals(3, revokedCount);
    verify(sessionRepository, times(1)).revokeAllExceptSession(
        eq(USER_ID), eq(CURRENT_SES_ID), any());

    // Assert — SecurityEvent emitted (at minimum once for the bulk operation)
    verify(securityEventService, atLeastOnce()).record(
        eq(SecurityEventType.TOKEN_REVOKED), eq(USER_ID), any(), any());

    // Assert — individual session.revokeSession() NOT called (must use bulk)
    verify(sessionRepository, never()).revokeSession(any(), any(), any());
}

@Test
void revokeAllExceptCurrent_noOtherSessions_returns0() {
    when(sessionRepository.revokeAllExceptSession(
        eq(USER_ID), eq(CURRENT_SES_ID), any())).thenReturn(0);

    int revokedCount = sessionService.revokeAllExceptCurrent(USER_ID, CURRENT_SES_ID, "127.0.0.1");
    assertEquals(0, revokedCount);
}
```

**Expected Result (PASS):** `revokedCount = 3`; bulk UPDATE called once; no individual revokeSession calls
**Expected Result (FAIL):** Iterates N times calling `revokeSession()` per session → violates C7
**Current Status:** 🔴 Not written

---

### SES-TC-008 — TOKEN_REVOKED SecurityEvent được emit

**Severity:** `HIGH`
**Feature Under Test:** `SessionServiceImpl` → `SecurityEventService.record(TOKEN_REVOKED, ...)`
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-SES-AUDIT` — SecurityEventType.TOKEN_REVOKED PHẢI được emit khi revoke

**Test Steps (JUnit 5):**
```java
@Test
void revokeSession_success_emitsTokenRevokedSecurityEvent() {
    // Arrange
    UserSession session = buildActiveSession(OTHER_SES_ID, USER_ID);
    when(sessionRepository.findById(OTHER_SES_ID)).thenReturn(Optional.of(session));
    when(sessionRepository.revokeSession(any(), any(), any())).thenReturn(1);

    // Act
    sessionService.revokeSession(OTHER_SES_ID, USER_ID, "10.0.0.1");

    // Assert — TOKEN_REVOKED security event
    ArgumentCaptor<SecurityEventType> eventTypeCaptor =
        ArgumentCaptor.forClass(SecurityEventType.class);
    verify(securityEventService).record(eventTypeCaptor.capture(), eq(USER_ID), any(), any());
    assertEquals(SecurityEventType.TOKEN_REVOKED, eventTypeCaptor.getValue());
}

@Test
void revokeSession_repositoryFails_securityEventNotEmitted() {
    // If DB update fails, security event MUST NOT be emitted
    UserSession session = buildActiveSession(OTHER_SES_ID, USER_ID);
    when(sessionRepository.findById(OTHER_SES_ID)).thenReturn(Optional.of(session));
    when(sessionRepository.revokeSession(any(), any(), any()))
        .thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class,
        () -> sessionService.revokeSession(OTHER_SES_ID, USER_ID, "127.0.0.1"));

    verify(securityEventService, never()).record(any(), any(), any(), any());
}
```

**Expected Result (PASS):** `TOKEN_REVOKED` event captured; no event on failure
**Expected Result (FAIL):** `TOKEN_REVOKED` not emitted → token revocation undetected by security monitoring
**Current Status:** 🔴 Not written

---

### SES-TC-009 — Session không tồn tại → SES-003

**Severity:** `MEDIUM`
**Feature Under Test:** `SessionServiceImpl.revokeSession()` — session lookup
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009` (implicit)
**Oracle Source:** `SES-003` error code in §10

**Test Steps (JUnit 5):**
```java
@Test
void revokeSession_sessionNotFound_throwsResourceNotFoundException() {
    UUID nonExistentId = UUID.randomUUID();
    when(sessionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> sessionService.revokeSession(nonExistentId, USER_ID, "127.0.0.1"));

    verify(sessionRepository, never()).revokeSession(any(), any(), any());
}
```

**Expected Result (PASS):** `ResourceNotFoundException` with SES-003
**Expected Result (FAIL):** `NullPointerException` or 500 error
**Current Status:** 🔴 Not written

---

### SES-TC-010 — isCurrent field đúng

**Severity:** `MEDIUM`
**Feature Under Test:** `SessionServiceImpl.mapToSessionInfo()` — isCurrent via JWT sid
**Test File:** `src/test/java/com/carebridge/backend/identity/service/SessionServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-004` — current session detect via sid claim

**Test Steps (JUnit 5):**
```java
@Test
void getActiveSessionsPaged_currentSessionFlagged_isCurrent() {
    // Arrange — mock SecurityContext to return CURRENT_SES_ID from JwtAuthenticationToken
    JwtAuthenticationToken mockAuth = mock(JwtAuthenticationToken.class);
    when(mockAuth.getSessionId()).thenReturn(CURRENT_SES_ID);
    when(mockAuth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(mockAuth);

    UserSession currentSession = buildActiveSession(CURRENT_SES_ID, USER_ID);
    UserSession otherSession = buildActiveSession(OTHER_SES_ID, USER_ID);

    Page<UserSession> page = new PageImpl<>(List.of(currentSession, otherSession));
    when(sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(
        eq(USER_ID), any())).thenReturn(page);

    // Act
    PaginatedResponse<SessionInfo> response = sessionService.getActiveSessionsPaged(USER_ID, 0, 20);

    // Assert
    SessionInfo current = response.getData().stream()
        .filter(s -> s.getSessionId().equals(CURRENT_SES_ID))
        .findFirst().orElseThrow();
    SessionInfo other = response.getData().stream()
        .filter(s -> s.getSessionId().equals(OTHER_SES_ID))
        .findFirst().orElseThrow();

    assertTrue(current.isCurrent());
    assertFalse(other.isCurrent());
}
```

**Expected Result (PASS):** Chỉ `CURRENT_SES_ID` có `isCurrent = true`
**Expected Result (FAIL):** `isCurrent = false` cho tất cả, hoặc tất cả đều true
**Current Status:** 🔴 Not written

---

### SES-TC-INT-001 — Integration: DELETE /sessions/{id} → DB revoked + blacklist

**Severity:** `HIGH`
**Feature Under Test:** `Full stack: SessionController → SessionServiceImpl → UserSessionRepository + TokenBlacklistRepository`
**Test File:** `src/test/java/com/carebridge/backend/identity/SessionIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainers running, Flyway V1–V5 applied
- User `USER_ID` tồn tại; session `ses-002` (active, non-current) trong DB
- JWT mock cho `USER_ID` với `sid = ses-001` (current session)

**Test Steps (Spring Boot Integration):**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SessionIntegrationTest {

    @Test
    void deleteSession_nonCurrent_revokesBothSessionAndBlacklist() throws Exception {
        // Arrange — seed sessions
        String ses001 = insertSession(USER_ID, "ses-001", "active", "hash-001");
        String ses002 = insertSession(USER_ID, "ses-002", "active", "hash-002");

        // Act — revoke ses-002
        mockMvc.perform(delete("/api/v1/sessions/" + ses002)
                .with(mockJwt().jwt(j -> j.subject(USER_ID.toString())
                    .claim("roles", List.of("ROLE_MOTHER"))
                    .claim("sid", ses001)))) // current session = ses-001
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        // Assert DB — ses-002 revoked
        Map<String, Object> revokedSession = jdbcTemplate.queryForMap(
            "SELECT status, revoked_at FROM user_sessions WHERE session_id = ?::uuid", ses002);
        assertEquals("REVOKED", revokedSession.get("status"));
        assertNotNull(revokedSession.get("revoked_at"));

        // Assert DB — ses-001 still active (current session not affected)
        String currentStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM user_sessions WHERE session_id = ?::uuid", String.class, ses001);
        assertEquals("active", currentStatus);

        // Assert TokenBlacklist entry exists
        Integer blacklistCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM token_blacklist WHERE reason = 'session_revoke'",
            Integer.class);
        assertTrue(blacklistCount >= 1);
    }

    @Test
    void deleteCurrentSession_returns400WithSES005() throws Exception {
        String ses001 = insertSession(USER_ID, "ses-001", "active", "hash-001");

        mockMvc.perform(delete("/api/v1/sessions/" + ses001)
                .with(mockJwt().jwt(j -> j.subject(USER_ID.toString())
                    .claim("sid", ses001)))) // trying to revoke current
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("SES-005"));

        // Session must still be active
        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM user_sessions WHERE session_id = ?::uuid", String.class, ses001);
        assertEquals("active", status);
    }
}
```

**Expected Result (PASS):**
- `ses-002` status = "REVOKED"; `revoked_at` is not null
- `ses-001` status = "active" (unaffected)
- `token_blacklist` has 1 new entry with reason = "session_revoke"
- DELETE current session → 400 + SES-005

**Expected Result (FAIL):**
- `ses-002` still active → revoke did not persist
- `ses-001` also revoked → current session incorrectly affected

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID             | Test File                                                              | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note                  |
| ----------------- | ---------------------------------------------------------------------- | ---------------- | ----------------- | --------------------------------- |
| `SES-TC-001`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | Extract `mapToSessionInfo()`      |
| `SES-TC-002`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-003`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | Extract `blacklistToken()`        |
| `SES-TC-004`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-005`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-006`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-007`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-008`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-009`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-010`      | `identity/service/SessionServiceImplTest.java`                         | `[ ]`            | `—`               | —                                 |
| `SES-TC-INT-001`  | `identity/SessionIntegrationTest.java`                                 | `[ ]`            | `—`               | —                                 |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase (thêm vào SessionServiceImpl tạm thời):**

```java
// Red Phase — TEMPORARY — revert sau khi gate pass
// Comment out existing implementations, replace với:
@Override
public PaginatedResponse<SessionInfo> getActiveSessionsPaged(UUID userId, int page, int size) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

@Override
public int revokeAllExceptCurrent(UUID userId, UUID currentSessionId, String ipAddress) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
// Lưu ý: revokeSession() đã có implementation — test mới cho SES-002, SES-008 etc.
// sẽ FAIL vì chưa có TOKEN_REVOKED emit
```

**Red Gate Verification:**

| TC ID            | Stub Result                             | Expected   | Actual           | Root Cause (nếu PASS bất thường)           |
| ---------------- | --------------------------------------- | ---------- | ---------------- | ------------------------------------------ |
| `SES-TC-001`     | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☐ FAIL ☐ PASS  | ☐ Tautology ☐ Shared state                |
| `SES-TC-002`     | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☐ FAIL ☐ PASS  | —                                          |
| `SES-TC-007`     | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☐ FAIL ☐ PASS  | —                                          |
| `SES-TC-008`     | `TOKEN_REVOKED not called`              | 🔴 FAIL   | ☐ FAIL ☐ PASS  | ☐ Existing code emits event already?       |
| `SES-TC-INT-001` | `throw UnsupportedOperationException`   | 🔴 FAIL   | ☐ FAIL ☐ PASS  | —                                          |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SES-IMP-001` đã được review và approve bởi Tech Lead
- [ ] Logic Issues (Section 2) đã được confirm — đặc biệt L3 (TOKEN_REVOKED missing) và L4 (endpoint mới)
- [ ] Migration `V5__session_enhancements.sql` đã được approve (nếu columns chưa tồn tại)
- [ ] Test fixtures FX-001 đến FX-007 đã được chuẩn bị

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test -pl 05_Development/CareBridgeAPI` — tất cả unit tests xanh (không skip)
- [ ] Integration test `SES-TC-INT-001` xanh với Testcontainers PostgreSQL
- [ ] Test coverage ≥ 80% lines cho: `SessionController`, `SessionServiceImpl` (phần thêm mới)
- [ ] `GET /sessions?size=51` → 400 + `SES-005`
- [ ] `DELETE /sessions/{currentId}` → 400 + `SES-005`
- [ ] `DELETE /sessions` → revoke tất cả non-current sessions
- [ ] `token_blacklist` có entry sau mỗi revoke thành công
- [ ] `SecurityEventType.TOKEN_REVOKED` được emit và lưu trong security_events

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả 11 tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — compile không lỗi:
  ```bash
  ./mvnw compile -pl 05_Development/CareBridgeAPI 2>&1 | grep "error:"
  # Expected: no errors
  ```
- [ ] **Props Isolation** — dùng `buildActiveSession()` factory; không share mutable state
- [ ] **Oracle Source** — mọi `assertEquals` có comment chỉ nguồn BR/ADR/UC

### Suspension Criteria

- Migration `V5__session_enhancements.sql` bị block bởi DPO (nếu columns mới cần DPO review)
- `JwtAuthenticationToken.getSessionId()` trả về null trong test environment — cần fix JwtConfig trước
- CI pipeline broken bởi thay đổi không liên quan

---

## 7. Rollback Plan

```bash
# Revert code changes (session service và controller additions)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java

# Revert migration V5 (nếu chỉ ADD COLUMN — safe nếu columns đều nullable)
# ALTER TABLE user_sessions DROP COLUMN IF EXISTS browser;
# ALTER TABLE user_sessions DROP COLUMN IF EXISTS location;
# ALTER TABLE user_sessions DROP COLUMN IF EXISTS last_activity_at;
# (Chỉ thực hiện nếu không có data trong columns này)

# Gap UC-16 vẫn PARTIAL — GET/DELETE /{id} cũ vẫn hoạt động; chỉ pagination và revoke-all mới bị revert
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                                | Check | Gate chặn |
| --------- | ------------------------ | -------------------------------------------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation  | TC không reference `BR-PRIVACY` hoặc `ADR-004` cho ownership/current session tests     | ☐    | G-0       |
| AP-AI-002 | Green-from-Birth          | `SES-TC-008` PASS ngay vì existing code đã emit TOKEN_REVOKED (verify thực tế)         | ☐    | G-2 ★    |
| AP-AI-003 | Implicit Decision         | Test assume `user_sessions` có column `browser` mà không verify migration V5           | ☐    | G-1       |
| AP-AI-004 | Layer Violation           | Service test assert HTTP 400 status code (là controller concern)                       | ☐    | G-4       |
| AP-AI-005 | Hallucinated Contract     | Test import `revokeAllExceptCurrent()` method chưa tồn tại trong interface             | ☐    | G-3       |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected  | TC ID        | Mô tả                                                              | Fix action                                     | Fixed? |
| ------------ | ------------ | ------------------------------------------------------------------ | ---------------------------------------------- | ------ |
| `AP-AI-002`  | `SES-TC-008` | Cần verify: existing `revokeSession()` đã emit `TOKEN_REVOKED`? Nếu rồi → test sẽ PASS ngay → cần rewrite với stricter assertion | Chạy Red Gate; nếu PASS → tăng assertion strictness | ☐ |
