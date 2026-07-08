# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification - UC-04 Logout

| Field | Value |
|-------|-------|
| **Document ID** | `CB-AUTH-IMP-004` |
| **Version** | `1.1` |
| **Date** | `2026-06-27` |
| **Status** | `Approved` |
| **Document Owner** | `PhuongNT` |
| **Author** | `AI Agent` |
| **Reviewed by** | `[Tech Lead]` |
| **DPO Sign-off** | `[ ] Pending` |
| **Approved by** | `[Principal Architect]` |
| **Last Review** | `2026-06-27` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| Date | Author | Change |
|------|--------|--------|
| 2026-06-26 | AI Agent | Initial draft created |
| 2026-06-27 | AI Agent | Realigned contract to match current backend implementation in `AuthController` + `SessionServiceImpl` |

---

## TABLE OF CONTENTS

1. [Module Overview](#1-module-overview)
2. [Traceability Matrix](#2-traceability-matrix)
3. [Architecture Decision Records (ADR)](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling](#5-static-modeling)
6. [Dynamic Modeling](#6-dynamic-modeling)
7. [Domain Event Catalog](#7-domain-event-catalog)
8. [Interface Specification](#8-interface-specification)
9. [API Specification](#9-api-specification)
10. [Error Codes](#10-error-codes)
11. [Implementation Steps](#11-implementation-steps)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Detailed Test Scenarios](#13-detailed-test-scenarios)
14. [Verification Approach](#14-verification-approach)
15. [API Verification Samples](#15-api-verification-samples)
16. [Authorization Matrix](#16-authorization-matrix)
17. [AI Prompt Constraints (CASE 2.0)](#17-ai-prompt-constraints-case-20)

---

## 1. Module Overview

| Field | Value |
|-------|-------|
| **Module Name** | `Logout` |
| **Bounded Context** | `auth / identity` |
| **UC ID** | `UC-04` |
| **SRS Reference** | `3.1.1.4` |
| **Primary Actor** | `Authenticated User` |
| **Platform** | `Web App + Mobile App` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-SECURITY, PDPA` |
| **Upstream Dependencies** | `UC-03 Login`, `user_sessions`, JWT `sid` claim |
| **Downstream Consumers** | `AuditService`, `TokenBlacklistRepository`, `RefreshTokenRepository` |

**Current backend-aligned behavior**

- Endpoint: `POST /api/v1/auth/logout`
- Authentication: required via bearer access token
- Request body: optional; if `refreshToken` is omitted or blank, backend attempts logout by current JWT `sid`
- Primary behavior: revoke only the current session
- Response: `ApiResponse<Void>` with success message `"Logged out"`
- Idempotency: if session is already revoked or not found by token hash, backend treats logout as successful and returns `200`
- No `logoutAll` support exists in the current endpoint implementation

**Observed implementation note**

- The controller delegates to `SessionService.logout(refreshToken, userId, ipAddress)`, not to a dedicated `AuthService.logout(...)` contract.
- Current backend stores revoked refresh-token hashes in `token_blacklist` and also revokes matching rows in `refresh_tokens`.

---

## 2. Traceability Matrix

| Requirement ID | Type | Requirement | Code Component | Notes |
|----------------|------|-------------|----------------|-------|
| UC-04 | Use Case | Authenticated user can sign out from current device | `AuthController.logout()` | Implemented |
| BR-LOGOUT-001 | Business Rule | Successful logout must create audit trail | `AuditService.log(LOGOUT, ...)` | Implemented |
| BR-LOGOUT-002 | Business Rule | Logout revokes current session only | `SessionServiceImpl.logout()` | Implemented |
| BR-LOGOUT-003 | Business Rule | Revoked refresh token cannot be used again | `RefreshTokenRepository.revokeByTokenHashAndUserId(...)` | Implemented |
| BR-LOGOUT-004 | Business Rule | System may identify current session via JWT sid | `extractCurrentSessionId()` fallback path | Implemented |
| BR-LOGOUT-005 | Business Rule | Access token still expires naturally after logout | Stateless access JWT; refresh revoked | Implemented |

---

## 3. Architecture Decision Records (ADR)

### ADR-AUTH-009 - Current-session logout with optional refresh-token body

| Field | Value |
|-------|-------|
| **Status** | `Accepted (as implemented)` |
| **Date** | `2026-06-27` |
| **Decision** | Use one endpoint `POST /api/v1/auth/logout`; accept optional request body containing `refreshToken`; if absent, resolve current session by JWT `sid`. |

**Consequences**

- Frontend can call logout even when it only has the authenticated access token.
- Backend keeps the API ergonomic for mobile and web confirmation flows.
- The endpoint currently represents "logout current session" only.

### ADR-AUTH-010 - Refresh-token revocation with session blacklist support

| Field | Value |
|-------|-------|
| **Status** | `Accepted (as implemented)` |
| **Date** | `2026-06-27` |
| **Decision** | Revoke refresh/session artifacts on logout; access token remains valid until normal expiry. |

**Consequences**

- Logout immediately blocks refresh-token reuse.
- Access token may remain usable for the rest of its short TTL.
- Backend writes token-hash revocation metadata for revoked sessions.

### ADR-AUTH-011 - Idempotent logout for already-revoked or missing sessions

| Field | Value |
|-------|-------|
| **Status** | `Accepted (as implemented)` |
| **Date** | `2026-06-27` |
| **Decision** | Treat "session already revoked / token hash not found" as successful logout instead of surfacing an error. |

**Consequences**

- Safer UX for repeated logout taps and stale mobile clients.
- Frontend should not expect a distinct "already logged out" error state.

---

## 4. Non-Functional Requirements & SLA

| Category | Requirement | Target |
|----------|-------------|--------|
| Latency | Logout API p95 | `< 300ms` |
| Availability | Monthly uptime | `99.9%` |
| Auditability | Every successful logout logs an audit event | `100%` |
| Replay Prevention | Revoked refresh token cannot refresh | `100%` |
| UX | Repeated logout request remains safe | Idempotent |

---

## 5. Static Modeling

### 5.1 Main Components

```text
AuthController
  -> SessionService
      -> UserSessionRepository
      -> TokenBlacklistRepository
      -> RefreshTokenRepository
      -> AuditService
      -> JwtTokenProvider
```

### 5.2 Core Data Elements

```text
RefreshTokenRequest
  - refreshToken: String?

UserSession
  - sessionId: UUID
  - userId: UUID
  - refreshTokenHash: String?
  - revoked: boolean
  - revokedAt: Instant?
  - expiresAt: Instant?

TokenBlacklist
  - tokenHash: String
  - expiresAt: Instant
  - revokedAt: Instant
  - reason: "logout" | "logout_sid" | ...
```

---

## 6. Dynamic Modeling

### 6.1 Flow A - Logout with request body

```text
Client -> POST /api/v1/auth/logout
Authorization: Bearer <access token>
Body: { "refreshToken": "<raw refresh token>" }

AuthController
  -> resolve current userId from Principal
  -> SessionService.logout(refreshToken, userId, ipAddress)

SessionServiceImpl
  -> SHA-256 hash refresh token
  -> find active session by refreshTokenHash
  -> if not found: revoke matching refresh-token row if present, then return success
  -> verify session.userId == authenticated userId
  -> mark session revoked
  -> add token hash to blacklist
  -> revoke refresh-token row
  -> write audit log
  -> clear security context
```

### 6.2 Flow B - Logout without request body / empty body

```text
Client -> POST /api/v1/auth/logout
Authorization: Bearer <access token>
Body: {}

AuthController
  -> refreshToken resolves to null
  -> SessionService.logout(null, userId, ipAddress)

SessionServiceImpl
  -> extract current sessionId from JWT sid
  -> load current session by sessionId
  -> if active and owned by user: revoke session + blacklist token hash + revoke refresh row
  -> write audit log
  -> clear security context
  -> return success
```

### 6.3 Flow C - Repeated logout

```text
If token hash no longer maps to an active session:
  - backend does not throw
  - backend returns 200 success
```

---

## 7. Domain Event Catalog

The current implementation does **not** publish a dedicated domain event object for logout.
It writes an audit log entry instead.

| Artifact | Trigger | Producer |
|----------|---------|----------|
| `AuditAction.LOGOUT` | Successful session logout | `SessionServiceImpl` |

---

## 8. Interface Specification

### 8.1 Controller Interface

```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
        @RequestBody(required = false) RefreshTokenRequest request,
        Principal principal,
        HttpServletRequest httpRequest)
```

### 8.2 Service Interface

```java
void logout(String refreshToken, UUID userId, String ipAddress);
```

### 8.3 Effective Request DTO

```java
public class RefreshTokenRequest {
    private String refreshToken;
}
```

**Alignment note**

- `logoutAll` is not part of the current backend contract.
- Frontend must not send or depend on `revokedCount`.

---

## 9. API Specification

### 9.1 Endpoint Table

| Method | Path | Auth | Roles | Current Behavior |
|--------|------|------|-------|------------------|
| `POST` | `/api/v1/auth/logout` | Bearer JWT | Authenticated user | Logout current session |

### 9.2 Request

**Headers**

```http
Authorization: Bearer <access-token>
Content-Type: application/json
```

**Supported body forms**

With refresh token:

```json
{
  "refreshToken": "raw-refresh-token"
}
```

Without refresh token:

```json
{}
```

Or request body omitted entirely.

### 9.3 Success Response

```json
{
  "success": true,
  "data": null,
  "message": "Logged out"
}
```

### 9.4 Known Contract Differences vs old TDS

- No `logoutAll`
- No `revokedCount`
- Response payload is `null`
- Empty request body is valid

---

## 10. Error Codes

The current endpoint is intentionally narrow and most happy-path retries return success.

| Situation | Current Result |
|-----------|----------------|
| Missing/invalid access token | `401` from security layer |
| Session already revoked / token not found | `200` success |
| Session ownership mismatch on token-hash path | backend currently throws `IllegalArgumentException`; not expected in normal self-logout UI flow |

**Implementation note**

- The ownership-mismatch case is not modeled as a typed business exception in the current controller path.
- Frontend for CB-116 does not expose arbitrary session selection, so this path should not occur in standard logout confirmation flow.

---

## 11. Implementation Steps

1. Authenticate request via bearer JWT.
2. Resolve `userId` from `Principal`.
3. If `refreshToken` is blank/null:
   - extract current `sessionId` from JWT `sid`
   - revoke that session if it exists and belongs to the user
4. If `refreshToken` is present:
   - hash token
   - find active session by hash
   - if not found, treat as success
   - verify ownership
   - revoke session
5. Blacklist the stored refresh-token hash when available.
6. Revoke matching row in `refresh_tokens`.
7. Write `AuditAction.LOGOUT`.
8. Clear security context.
9. Return `ApiResponse.success(null, "Logged out")`.

---

## 12. Rollback & Incident Runbook

### 12.1 Rollback Triggers

| Condition | Trigger |
|-----------|---------|
| Logout no longer revokes current session | Immediate rollback |
| Refresh token remains usable after logout | Immediate rollback |
| Audit log missing for successful logouts | Investigate within same release window |

### 12.2 Rollback Actions

```bash
git revert <logout-change-commit>
```

Operational validation after rollback:

- verify current-session logout works again
- verify refresh fails for revoked token
- verify audit entries continue to appear

---

## 13. Detailed Test Scenarios

### 13.1 Happy Path

- Authenticated user calls logout with `{ "refreshToken": "<current token>" }`
- Expect `200`
- Expect session marked revoked
- Expect refresh-token row revoked

### 13.2 SID Fallback Path

- Authenticated user calls logout with empty body
- JWT includes valid `sid`
- Expect `200`
- Expect current session marked revoked

### 13.3 Idempotent Retry

- Logout same session twice
- Expect second call still returns `200`

### 13.4 Post-Logout Refresh Rejected

- Login
- Logout
- Attempt refresh with same refresh token
- Expect refresh denied

### 13.5 Access Token TTL Window

- Logout successfully
- Call protected API with still-unexpired access token
- Expect current stateless behavior until access token expires

---

## 14. Verification Approach

### 14.1 API Verification

- Call `POST /api/v1/auth/logout` with bearer token and empty `{}` body
- Confirm `200`
- Confirm response `data` is `null`

### 14.2 Database Verification

```sql
SELECT session_id, revoked, revoked_at
FROM user_sessions
WHERE user_id = :user_id
ORDER BY revoked_at DESC;
```

```sql
SELECT token_hash, revoked
FROM refresh_tokens
WHERE user_id = :user_id;
```

### 14.3 Audit Verification

Confirm `AuditAction.LOGOUT` exists for the session/user combination.

---

## 15. API Verification Samples

### 15.1 Empty-body logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 15.2 Explicit refresh-token logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

Expected result for both:

```json
{
  "success": true,
  "data": null,
  "message": "Logged out"
}
```

---

## 16. Authorization Matrix

| Endpoint | Guest | Authenticated User |
|----------|-------|--------------------|
| `POST /api/v1/auth/logout` | No | Yes, for current owned session |

---

## 17. AI Prompt Constraints (CASE 2.0)

1. Do not model `logoutAll` in frontend or downstream specs until backend actually supports it.
2. Treat empty JSON body `{}` as valid logout request.
3. Expect success response payload `data = null`; do not depend on `revokedCount`.
4. Use backend behavior as source of truth over older draft TDS text.
5. Repeated logout must remain safe and idempotent in UI logic.

---

## References

| Artifact | Path |
|----------|------|
| Backend controller | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java` |
| Backend service | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java` |
| Mobile screen | `05_Development/CareBridgeMobileApp/lib/features/auth/screens/logout_confirmation_screen.dart` |
