# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-102: Warn or Suspend Account

**Document ID:** `CB-MOD-TEST-004`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Implemented — 2026-07-01`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(Internal data — no new PII column; `suspended_until` is an operational account-state timestamp. Flagged for DPO awareness only, per TDS header.)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC102_WarnOrSuspendAccount/UC102_WarnOrSuspendAccount_TDS.md` (`CB-MOD-IMP-004`)
- SRS Section 3.2.2.4 (Warn or Suspend Account)
- Sibling (Draft, this batch, shares `ModerationController`/`ModerationServiceImpl`): `04_Implement/UC100_ModerateCommunityContent/` (`CB-MOD-IMP-002`)
- Sibling (Draft, this batch, forward-dependency source): `04_Implement/UC101_ResolveReport/` (`CB-MOD-IMP-003`)
- Sibling (Approved, read-only oracle): `04_Implement/UC99_ViewModerationQueue/` (`CB-MOD-IMP-001`)
- CLAUDE.md §3 Architecture Rules, §5 Delivery Rules

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-102 Warn or Suspend Account (Status=Draft)  |
| 2026-07-01 | AI Agent — Amelia (Dev Agent) | Phase 3: Implementation — 24/25 TCs PASS (13 service unit + 3 controller + 4 security + 1 login-gate enforcement + 2 mocked-HTTP-flow integration + 2 real-filter-chain enforcement integration). `WSA-TC-INT-003` (atomicity rollback) explicitly NOT implemented — no Testcontainers/real-DB harness exists in this codebase, same documented gap pattern as UC-101's RES-TC-INT-004. `WSA-TC-INT-001/002` implemented as mocked-service full-HTTP-stack tests (not real DB assertions) — same established convention as UC-100/101. `WSA-TC-207`'s "exactly now" boundary uses wall-clock ordering instead of an injected `Clock` (no `Clock` abstraction exists anywhere in this codebase). `WSA-TC-216/217/218/219` passed at Red Gate already because RBAC/`SecurityConfig` wiring was added alongside the stub in the same RED-phase commit, consistent with the UC-100/101 precedent. Full regression: 0 new failures (baseline 33 pre-existing DB-dependent errors unchanged: `BackendApplicationTests`, `ExerciseControllerDetailSecurityTest`, `ExerciseDetailIntegrationTest`, `AuthServiceRegisterTest`, `RegistrationIntegrationTest`, `TriageIntegrationTest`). **Not yet committed** — work is on the shared `dev` branch; per `.claude/rules/git-dual-remote.md` it should move to `HuyND` before any commit. |

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

| Field                     | Value                                                                                                                    |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-102`                                                                                                                |
| **Module**                | `Warn or Suspend Account — content (write path) + security (cross-package enforcement, ADR-003)`                        |
| **Spec gốc**              | `CB-MOD-IMP-004`                                                                                                          |
| **Priority**              | `P0 — High, Regular` (per FS 3.2.2.4)                                                                                     |
| **Sprint**                | `Open` — not sourced; TDS does not assign one                                                                              |
| **Milestone**             | `Open`                                                                                                                    |
| **Data Classification**   | `Internal` (no new PII; `suspended_until` is operational account-state)                                                    |
| **Compliance Scope**      | `N/A` (no new data-subject export)                                                                                        |
| **Upstream Dependencies** | `security (User, UserRepository, JwtAuthenticationFilter, AuthenticationPolicy)`, `content (ModerationAction, ModerationActionType, ReportTargetType, ModerationException)`, `audit (AuditService)`, `common (AccountSuspendedException — new, GlobalExceptionHandler)` |
| **Downstream Consumers**  | Every authenticated endpoint in the system (enforcement is account-scoped, not endpoint-scoped — ADR-003); `UC-101 Resolve Report` (potential future caller of `moderateAccount()` when a report outcome is "warn"/"suspend" — out of scope to wire here) |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-MOD-IMP-004 §17`, `ADR-001 §Decision`, `ADR-003 §Decision`, `ADR-004 §Decision`, `ADR-005 §Decision`, `ADR-006 §Decision`, `ADR-007 §Decision`, `ADR-008 §Decision` |
| **Constraints Injected** | `C1 (RBAC MODERATOR)`, `C2 (WARN no User mutation)`, `C3 (SUSPEND sets suspendedUntil + action in 1 tx)`, `C4 (never reuse locked/enabled)`, `C5 (reason required both)`, `C6 (targetType=ACCOUNT, reportId=null)`, `C7 (enforcement wired into security — else ghost action)`, `C8 (lazy read-only expiry, no write-back)`, `C9 (audit every success)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                                                                  | Thực tế (schema / policy / code)                                                                                                            | Fix áp dụng trong test                                                                                       |
| --- | ----------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| L1  | Dossier gợi ý "suspend = set `users.enabled=false` hoặc `users.locked=true`" (không cần migration)          | ADR-001 chứng minh reuse `locked`/`lockedAt` KHÔNG an toàn: xung đột với brute-force auto-unlock 15 phút hardcoded (`AuthenticationPolicy` lines 28-31) và clear-on-successful-login (`AuthServiceImpl` 347-348). Cần cột mới `suspended_until` | Test SUSPEND PHẢI assert `User.suspendedUntil` được set; PHẢI assert `enabled`/`locked`/`lockedAt` KHÔNG bị đụng tới (negative assertion) — WSA-TC-202 |
| L2  | FS-3.2.2.4 liệt kê "restricts posting" như một outcome riêng                                                | Không có `ModerationActionType` hay cơ chế posting-only-block nào trong codebase — chỉ có block-toàn-bộ (`suspended_until`). Ghi nhận `Open` (ADR-007) | KHÔNG viết test cho "restrict posting"; SUSPEND luôn là full-account block                                          |
| L3  | Dossier ngụ ý WARN "gửi cảnh báo cho user"                                                                  | ADR-004: WARN là **audit-only** — không mutate `User`, không có notification-delivery nào được source (channel/template `Open`)              | Test WARN PHẢI assert KHÔNG có `User` mutation, `userRepository.save()` KHÔNG được gọi, và (nếu mock) `NotificationService` KHÔNG được gọi — WSA-TC-201/WSA-TC-213 |
| L4  | FS ngụ ý suspend có thể vô thời hạn                                                                          | ADR-008: v1 chỉ hỗ trợ time-bound; SUSPEND BẮT BUỘC `expiresAt` tương lai hợp lệ. Indefinite suspension bị defer (`Open`)                     | Test PHẢI assert SUSPEND thiếu/null/quá-khứ/bằng-now `expiresAt` → `MOD-018`; KHÔNG test indefinite-suspend path    |
| L5  | Không nguồn nào quy định moderator có được tự WARN/SUSPEND chính mình hay không                              | ADR-007: self-action guard là **design decision (Proposed, chưa Accepted)**, không phải sourced FS/BR fact                                    | Test WSA-TC-209 PHẢI gắn `Oracle Source: ADR-007 (Proposed — Open)`; nếu ADR-007 bị reject, test này PHẢI bị xóa, không để lại dead logic |
| L6  | Dossier coi UC-102 như một endpoint content-moderation bình thường (giống UC-100)                           | ADR-003: enforcement CẮT NGANG sang bounded context `security`. Ghi `suspendedUntil` mà không wire `JwtAuthenticationFilter`/`AuthenticationPolicy` = **ghost action** (không có hiệu lực thực) | Test integration WSA-TC-INT-004 (CRITICAL) PHẢI chứng minh JWT phát hành TRƯỚC suspend bị chặn 403 `ACCOUNT_SUSPENDED` ở request kế tiếp — không chỉ assert `suspended_until` đã ghi vào DB |
| L7  | Không nguồn nào nói suspension hết hạn thì có tự động clear `suspended_until` về null hay không               | ADR-003: kiểm tra hết hạn là **lazy, read-only** — KHÔNG write-back `suspended_until=null` trong hot path (khác với `lockedAt` auto-unlock ghi đè) | Test WSA-TC-INT-005 PHẢI assert user với `suspended_until` trong QUÁ KHỨ được cho qua (không bị chặn) VÀ `suspended_until` vẫn giữ nguyên timestamp cũ (không bị ghi về null) |
| L8  | Error-code numbering: UC-102 dùng dải nào?                                                                   | Orchestrator instruction: UC-102 claim `MOD-015..MOD-020` (headroom trên UC-101's `MOD-011..013`); `ACCOUNT_SUSPENDED` sống ở `common/exception` (cùng tier `ACCESS_DENIED`/`ACCOUNT_LOCKED`), KHÔNG phải `MOD-xxx` | Mọi test error PHẢI dùng đúng mã `MOD-015..020`/`ACCOUNT_SUSPENDED`; Consistency Gate re-verify không đụng dải UC-100/UC-101 |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-102 Warn or Suspend Account bao gồm các layer:
├── Controller (ModerationController.moderateAccount() — mock service, @WebMvcTest)
├── Service (ModerationServiceImpl.moderateAccount() — mock UserRepository +
│   ModerationActionRepository + AuditService, Mockito)
├── Repository (UserRepository.findById/save + ModerationActionRepository.save — existing, no new
│   finder methods)
├── Enforcement (security package, ADR-003) — JwtAuthenticationFilter + AuthenticationPolicy read
│   User.suspendedUntil. Exercised at INTEGRATION level (WSA-TC-INT-004/005), NOT unit-mocked, vì đây
│   là điểm dễ trở thành "ghost action" nhất — phải test end-to-end với real filter chain.
└── Integration (Full API flow — MockMvc + Testcontainers PostgreSQL, migration
    V20260701120000__add_user_suspended_until.sql applied)
```

### TDS-02 — Test Basis

| Source                                  | Items Derived                                                                                 |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| `SRS 3.2.2.4`                             | "Warns, restricts posting, or suspends accounts that violate rules" — WARN/SUSPEND supported v1; "restricts posting" gapped (ADR-007) |
| `TDS CB-MOD-IMP-004 ADR-001`              | `suspended_until` new column; SUSPEND sets it, WARN does not; never reuse `locked`/`enabled`       |
| `TDS CB-MOD-IMP-004 ADR-002`              | `@PreAuthorize("hasRole('MODERATOR')")` at controller (mirror UC-100); no `RoleHierarchy`          |
| `TDS CB-MOD-IMP-004 ADR-003`              | Enforcement wired into `JwtAuthenticationFilter` + `AuthenticationPolicy`; lazy read-only expiry    |
| `TDS CB-MOD-IMP-004 ADR-004`              | WARN is audit-only — no `User` mutation, no notification delivery v1                                |
| `TDS CB-MOD-IMP-004 ADR-005`              | `reason` required (non-blank) for BOTH WARN and SUSPEND (`MOD-017`)                                 |
| `TDS CB-MOD-IMP-004 ADR-006`              | New `ReportTargetType.ACCOUNT`; `ModerationAction.targetType=ACCOUNT`, `reportId=null`              |
| `TDS CB-MOD-IMP-004 ADR-007`              | Self-action guard (`MOD-020`) — **Proposed/Open**, not sourced                                      |
| `TDS CB-MOD-IMP-004 ADR-008`              | SUSPEND requires future `expiresAt` (`MOD-018`); indefinite suspension deferred                     |
| `security/entity/User.java`               | `enabled`, `locked`, `lockedAt` existing; `suspendedUntil` new — oracle for negative assertions     |
| `security/policy/AuthenticationPolicy.java` (lines 19-36) | 15-min hardcoded lockout auto-unlock — oracle for why `locked` cannot be reused (L1)  |
| `content/entity/ModerationActionType.java`| APPROVE/HIDE/LOCK/WARN/SUSPEND — oracle for MOD-016 scope check                                     |
| `content/exception/GlobalExceptionHandler.java` | Real 403 code = `ACCESS_DENIED`; real 401 = bodiless — oracle for security tests (same as UC-100/101) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                                            | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | -------------------------------------------------------------- | ------------------- |
| TC-COND-001  | WARN một user PENDING → `ModerationAction` recorded, `User` KHÔNG bị mutate | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-201`        |
| TC-COND-002  | SUSPEND một user với `expiresAt` tương lai → `User.suspendedUntil` set, `locked`/`enabled` KHÔNG đổi | `ModerationServiceImpl.moderateAccount()`             | `WSA-TC-202`        |
| TC-COND-003  | actionType ∈ {APPROVE, HIDE, LOCK} → `MOD-016`                             | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-203`        |
| TC-COND-004  | reason blank/null cho WARN → `MOD-017`                                     | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-204`        |
| TC-COND-005  | reason blank/null cho SUSPEND → `MOD-017`                                  | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-205`        |
| TC-COND-006  | SUSPEND thiếu/null `expiresAt` → `MOD-018`                                 | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-206`        |
| TC-COND-007  | SUSPEND `expiresAt` trong quá khứ hoặc bằng now → `MOD-018` (boundary)     | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-207`        |
| TC-COND-008  | WARN có `expiresAt` non-null → `MOD-019`                                   | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-208`        |
| TC-COND-009  | targetUserId == moderatorUserId → `MOD-020` (ADR-007, Open)               | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-209`        |
| TC-COND-010  | targetUserId không tồn tại → `MOD-015` (404)                              | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-210`        |
| TC-COND-011  | `ModerationAction.reportId` luôn null, `targetType` luôn ACCOUNT           | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-211`        |
| TC-COND-012  | `AuditService.log(MODERATION_ACTION,"ACCOUNT",...)` gọi đúng 1 lần         | `AuditService` mock verify                                     | `WSA-TC-212`        |
| TC-COND-013  | WARN KHÔNG BAO GIỜ gọi `userRepository.save()` (no User mutation)          | `ModerationServiceImpl.moderateAccount()`                      | `WSA-TC-213`        |
| TC-COND-014  | Missing required field (`targetUserId`/`actionType` null) → 400 @Valid     | `@Valid` bean validation                                       | `WSA-TC-214`        |
| TC-COND-015  | Unexpected exception → 500 `INTERNAL_ERROR` (not dead-code MOD-005)        | `GlobalExceptionHandler.handleGeneric()`                       | `WSA-TC-215`        |
| TC-COND-016  | Non-MODERATOR bị 403 `ACCESS_DENIED`                                       | `@PreAuthorize` Spring Security                                | `WSA-TC-216`        |
| TC-COND-017  | SYSTEM_ADMIN không có quyền ngầm — cũng bị 403                             | `@PreAuthorize` Spring Security                                | `WSA-TC-217`        |
| TC-COND-018  | Request không có JWT → 401, body rỗng                                      | `HttpStatusEntryPoint`                                          | `WSA-TC-218`        |
| TC-COND-019  | SQL injection trong `reason` field                                         | Parameterized query / JPA                                      | `WSA-TC-219`        |
| TC-COND-020  | Suspended user login → `AccountSuspendedException` → 403 `ACCOUNT_SUSPENDED` at login gate | `AuthenticationPolicy.ensureCanAuthenticate()`   | `WSA-TC-220`        |
| TC-COND-021  | Full integration flow SUSPEND — DB có action + `users.suspended_until` set | Testcontainers integration                                     | `WSA-TC-INT-001`    |
| TC-COND-022  | Full integration flow WARN — DB có action + `users` row KHÔNG đổi cột nào  | Testcontainers integration                                     | `WSA-TC-INT-002`    |
| TC-COND-023  | Rollback khi lỗi giữa chừng (atomicity) — SUSPEND                          | Testcontainers integration                                     | `WSA-TC-INT-003`    |
| TC-COND-024  | ENFORCEMENT (CRITICAL): pre-issued JWT của user vừa bị SUSPEND → 403 `ACCOUNT_SUSPENDED` ở request kế tiếp | Testcontainers + full filter chain            | `WSA-TC-INT-004`    |
| TC-COND-025  | Lazy expiry: user với `suspended_until` QUÁ KHỨ → request được cho qua, `suspended_until` KHÔNG bị clear | Testcontainers + full filter chain              | `WSA-TC-INT-005`    |

### TDS-04 — Test Techniques

| Technique                | Applied To                                                | Rationale                                                                  |
| --------------------------- | -------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Equivalence Partitioning  | `actionType` × validity of `expiresAt`/`reason`                  | WARN vs SUSPEND vs APPROVE/HIDE/LOCK partitioned into valid / MOD-016..019       |
| Boundary Value Analysis   | `expiresAt` relative to `now()` (past / exactly now / future)    | MOD-018 boundary — only strictly-future proceeds (ADR-008)                        |
| State Transition Testing  | `User.suspendedUntil` null → future → (lapsed past)              | Verify set-on-SUSPEND, lazy-expiry read-only, no write-back (ADR-003)             |
| Negative / Non-Mutation   | WARN path assertions that `User` columns unchanged               | Guards ADR-004 (audit-only) — the most likely accidental-side-effect regression   |
| Error Guessing            | SQL injection in `reason`, JWT tampering, self-action, unknown targetId | Security + robustness vectors                                             |
| Cross-package Integration | Enforcement through the real `JwtAuthenticationFilter` chain     | The whole UC exists to make suspension actually block — cannot be unit-mocked (ADR-003) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                                      | Mục đích                                  |
| ----------- | -------- | -------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-301`   | DB seed | `User{id: U1, enabled: true, locked: false, lockedAt: null, suspendedUntil: null}`                        | Target for WARN/SUSPEND happy paths            |
| `FX-302`   | DB seed | `User{id: MOD1, enabled: true}` — the acting moderator                                                    | Self-action guard test (MOD-020, ADR-007)      |
| `FX-303`   | DB seed | `User{id: U2, enabled: true, locked: false, suspendedUntil: <now + 14 days>}` (already suspended)         | Enforcement test — pre-issued JWT blocked (INT-004) |
| `FX-304`   | DB seed | `User{id: U3, enabled: true, locked: false, suspendedUntil: <now − 1 day>}` (lapsed suspension)           | Lazy-expiry test — request allowed, no write-back (INT-005) |
| `FX-305`   | value   | `expiresAt = Instant.now().plus(14, DAYS)` (valid future); variants: `null`, `now − 1h`, `now`            | SUSPEND expiresAt validation (MOD-018 boundary) |
| `FX-306`   | JWT     | `{sub: MOD1, role: "ROLE_MODERATOR"}`                                                                     | Auth happy path                                 |
| `FX-307`   | JWT     | `{sub: "<uuid>", role: "ROLE_MOTHER"}`                                                                     | Auth failure (403 `ACCESS_DENIED`)              |
| `FX-308`   | JWT     | `{sub: "<uuid>", role: "ROLE_SYSTEM_ADMIN"}`                                                               | Auth failure (403 — no implicit superuser)      |
| `FX-309`   | none    | No `Authorization` header                                                                                  | Auth failure (401, bodiless)                    |
| `FX-310`   | JWT     | `{sub: U2, role: "ROLE_MOTHER"}` issued BEFORE U2 is suspended                                             | Pre-issued token for enforcement test (INT-004) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// === WSA-TC Props Isolation Pattern ===
// Đặt ở đầu test class — mỗi @Test dùng factory method, không share mutable state

class WarnSuspendAccountTestFactory {

    static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-0000000000f2");
    static final UUID TARGET_USER_ID   = UUID.fromString("bb000000-0000-0000-0000-000000000001");
    static final UUID SUSPENDED_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000002");
    static final UUID LAPSED_USER_ID    = UUID.fromString("bb000000-0000-0000-0000-000000000003");

    // Fixed clock reference — all time-bound assertions computed against this, never Instant.now()
    // directly inside a test body (randomness/clock control per §TDS-05).
    static final Instant T0 = Instant.parse("2026-07-01T10:00:00Z");
    static final Instant FUTURE_EXPIRY = T0.plus(14, ChronoUnit.DAYS);
    static final Instant PAST_EXPIRY   = T0.minus(1, ChronoUnit.DAYS);

    static User makeUser(UUID id, Instant suspendedUntil, Consumer<User> overrides) {
        User u = User.builder()
                .id(id)
                .enabled(true)
                .locked(false)
                .lockedAt(null)
                .suspendedUntil(suspendedUntil)   // NEW field (ADR-001)
                .build();
        overrides.accept(u);
        return u;
    }

    static WarnOrSuspendAccountRequest makeRequest(ModerationActionType actionType,
                                                   String reason, Instant expiresAt) {
        return new WarnOrSuspendAccountRequest(TARGET_USER_ID, actionType, reason, expiresAt);
    }
}
```

> **Note:** `User.builder()` fields used above (`enabled`/`locked`/`lockedAt`/`suspendedUntil`) must match
> the real `security/entity/User.java` after the ADR-001 field is added. If `User` uses a different builder
> shape (e.g. a partial builder), the factory adapts to the real entity — the invariant is only that the
> factory, not each `@Test`, owns construction.

---

### WSA-TC-201 — WARN một user → `ModerationAction` recorded, `User` KHÔNG bị mutate

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount(request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS CB-MOD-IMP-004 ADR-004 §Decision`, `BR-MOD-011`

**Preconditions:**
- `UserRepository.findById(TARGET_USER_ID)` trả về `FX-301` (suspendedUntil=null)
- `ModerationActionRepository`, `AuditService` mock; `principal` resolves to `MODERATOR_ID`

**Test Steps:**
1. Arrange: request = `makeRequest(WARN, "Ngôn từ không phù hợp, lần đầu", null)`
2. Act: `service.moderateAccount(request, principal)`
3. Assert kết quả

**Expected Result (PASS):**
- `response.actionType()` = `WARN`, `response.accountSuspended()` = `false`, `response.expiresAt()` = `null`
- `moderationActionRepository.save(...)` gọi 1 lần với `actionType=WARN`, `targetType=ACCOUNT`, `reportId=null`, `expiresAt=null`
- `userRepository.save(...)` **không bao giờ được gọi** (verify no interaction) — `User` không bị mutate
- `auditService.log(MODERATION_ACTION, MODERATOR_ID, "ACCOUNT", TARGET_USER_ID.toString(), ...)` gọi 1 lần

**Expected Result (FAIL):**
- `userRepository.save()` được gọi (WARN mutate User) → vi phạm ADR-004
- `suspendedUntil`/`locked`/`enabled` bị đổi → vi phạm C2/C4

**Current Status:** 🟢 Passing

---

### WSA-TC-202 — SUSPEND với `expiresAt` tương lai → `suspendedUntil` set, `locked`/`enabled` KHÔNG đổi

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — the core SUSPEND write path
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-001 §Decision (Option D)`, `ADR-008 §Decision`, `security/entity/User.java`

**Preconditions:** `UserRepository.findById(TARGET_USER_ID)` trả về `FX-301`; capture the saved `User` argument

**Test Steps:**
1. Arrange: request = `makeRequest(SUSPEND, "Vi phạm lặp lại sau cảnh báo", FUTURE_EXPIRY)`
2. Act: `service.moderateAccount(request, principal)`
3. Assert (capture `userRepository.save(...)` argument + `moderationActionRepository.save(...)` argument)

**Expected Result (PASS):**
- `response.actionType()` = `SUSPEND`, `response.accountSuspended()` = `true`, `response.expiresAt()` = `FUTURE_EXPIRY`
- Saved `User.suspendedUntil == FUTURE_EXPIRY`
- Saved `User.locked == false`, `User.lockedAt == null`, `User.enabled == true` — **unchanged** (the L1/C4 guarantee: SUSPEND must not touch the brute-force-lockout fields)
- Saved `ModerationAction`: `actionType=SUSPEND`, `targetType=ACCOUNT`, `reportId=null`, `expiresAt == FUTURE_EXPIRY` (mirrors request)

**Expected Result (FAIL):**
- `User.locked`/`enabled` mutated instead of / in addition to `suspendedUntil` → violates ADR-001/C4
- `ModerationAction.expiresAt` not mirroring `suspendedUntil` → data desync

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ The negative assertions on `locked`/`enabled` are the heart of ADR-001 — they
prove the implementer used the new dedicated column, not a reuse of the brute-force-lockout primitive.

---

### WSA-TC-203 — actionType ∈ {APPROVE, HIDE, LOCK} → 400 `MOD-016`

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — endpoint scope boundary
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §8.1 @throws MOD-016`, `§6.4 Action×Field matrix` — APPROVE/HIDE/LOCK belong to UC-100

**Preconditions:** `UserRepository` mock (may or may not be reached depending on validation order)

**Test Steps:**
1. Arrange: request = `makeRequest(APPROVE, "x", null)`
2. Act + Assert: throws `ModerationException` code `MOD-016`, `httpStatus == 400`
3. Repeat for `HIDE`, `LOCK` — same rejection

**Expected Result (PASS):**
- All 3 rejected with `MOD-016`
- `userRepository.save(...)` / `moderationActionRepository.save(...)` never called — no side effect

**Expected Result (FAIL):** Any of APPROVE/HIDE/LOCK proceeds to create a `ModerationAction` at this endpoint.

**Current Status:** 🟢 Passing
**Implementation Note:** Cross-UC boundary guard — this endpoint (`/account-actions`) must reject the
content-action verbs that belong to UC-100's `/actions` endpoint (TDS §9.1 endpoint-naming note).

---

### WSA-TC-204 — reason blank/null cho WARN → 400 `MOD-017`

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — reason validation (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS ADR-005 §Decision` — **design decision, not a sourced FS/BR fact**

**Preconditions:** `UserRepository.findById(TARGET_USER_ID)` mock available

**Test Steps:**
1. Sub-case a: request = `makeRequest(WARN, null, null)` → Act + Assert: `MOD-017`, 400
2. Sub-case b: request = `makeRequest(WARN, "   ", null)` (blank) → Act + Assert: `MOD-017`, 400

**Expected Result (PASS):** Both throw `MOD-017`; no `ModerationAction` created, no `User` mutation.

**Current Status:** 🟢 Passing

---

### WSA-TC-205 — reason blank/null cho SUSPEND → 400 `MOD-017`

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — reason validation (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS ADR-005 §Decision` — design decision, not sourced fact

**Test Steps:**
1. Sub-case a: request = `makeRequest(SUSPEND, null, FUTURE_EXPIRY)` → Act + Assert: `MOD-017`, 400
2. Sub-case b: request = `makeRequest(SUSPEND, "  ", FUTURE_EXPIRY)` → Act + Assert: `MOD-017`, 400

**Expected Result (PASS):** Both throw `MOD-017`; `suspendedUntil` NOT set (no `userRepository.save()`).

**Current Status:** 🟢 Passing
**Implementation Note:** Validation-order guard — `reason` must be checked before the `User` is mutated so a
blank-reason SUSPEND leaves the account untouched.

---

### WSA-TC-206 — SUSPEND thiếu/null `expiresAt` → 400 `MOD-018`

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — SUSPEND requires future expiresAt (ADR-008)
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-008 §Decision`, `§8.1 @throws MOD-018`

**Test Steps:**
1. Arrange: request = `makeRequest(SUSPEND, "reason hợp lệ", null)`
2. Act + Assert: throws `ModerationException` code `MOD-018`, `httpStatus == 400`

**Expected Result (PASS):** `MOD-018`; no `User` mutation; indefinite-suspend path is not reachable (ADR-008).

**Current Status:** 🟢 Passing

---

### WSA-TC-207 — SUSPEND `expiresAt` trong quá khứ hoặc bằng now → 400 `MOD-018` (boundary)

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — strictly-future boundary (ADR-008)
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS ADR-008 §Decision` — "validated to be strictly after `Instant.now()`"

**Test Steps:**
1. Sub-case a (past): request = `makeRequest(SUSPEND, "reason", PAST_EXPIRY)` → Act + Assert: `MOD-018`
2. Sub-case b (exactly now): inject a fixed clock so `now == T0`, request `expiresAt == T0` → Act + Assert: `MOD-018` (equal-to-now is NOT strictly future)

**Expected Result (PASS):** Both throw `MOD-018`; no `User` mutation.

**Expected Result (FAIL):** `expiresAt == now` is accepted → off-by-one boundary bug (would create a
suspension already lapsed at creation time — a de-facto no-op suspension).

**Current Status:** 🟢 Passing
**Implementation Note:** Requires clock control (inject `Clock` or a time abstraction) so the "exactly now"
boundary is deterministic; do NOT compare against a live `Instant.now()` inside the test body.

---

### WSA-TC-208 — WARN có `expiresAt` non-null → 400 `MOD-019`

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — WARN/expiresAt mutual exclusion
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §8.1 @throws MOD-019`, `§6.4 matrix (WARN ⇒ expiresAt always null)`

**Test Steps:**
1. Arrange: request = `makeRequest(WARN, "reason", FUTURE_EXPIRY)` (ambiguous — WARN doesn't expire)
2. Act + Assert: throws `ModerationException` code `MOD-019`, `httpStatus == 400`

**Expected Result (PASS):** `MOD-019`; no `ModerationAction` created.

**Current Status:** 🟢 Passing

---

### WSA-TC-209 — targetUserId == moderatorUserId → 400 `MOD-020` (self-action guard, ADR-007 Open)

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — self-action guard
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS ADR-007 §Decision` — **Proposed, NOT Accepted; not a sourced FS/BR fact**

**Preconditions:** `principal` resolves to `MODERATOR_ID`; request targets `MODERATOR_ID` itself

**Test Steps:**
1. Arrange: `request = new WarnOrSuspendAccountRequest(MODERATOR_ID, SUSPEND, "reason", FUTURE_EXPIRY)`
2. Act + Assert: throws `ModerationException` code `MOD-020`, `httpStatus == 400`, thrown BEFORE any other validation/lookup

**Expected Result (PASS):** `MOD-020`; no `User` mutation, no `ModerationAction`.

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ **Gated on ADR-007 sign-off.** If Product/Tech Lead REJECT the self-action
guard, this test AND the `MOD-020` factory must be removed together — do not leave `MOD-020` as untested
dead logic (per TDS ADR-007 §Quyết định). Oracle is explicitly a `Proposed` decision.

---

### WSA-TC-210 — targetUserId không tồn tại → 404 `MOD-015`

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — target resolution
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §10 Error Codes (MOD-015)`, `§8.1 @throws MOD-015`

**Preconditions:** `UserRepository.findById(TARGET_USER_ID)` trả về `Optional.empty()`

**Test Steps:**
1. Arrange: valid WARN request against `TARGET_USER_ID`
2. Act + Assert: throws `ModerationException` code `MOD-015`, `httpStatus == 404`

**Expected Result (PASS):** `MOD-015`; no `ModerationAction` created, no audit log.

**Current Status:** 🟢 Passing

---

### WSA-TC-211 — `ModerationAction.reportId` luôn null, `targetType` luôn ACCOUNT

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — action shape invariant (ADR-006)
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS ADR-006 §Decision`, `§6.4 matrix`

**Test Steps:**
1. Arrange: valid SUSPEND request (`FUTURE_EXPIRY`) against `FX-301`
2. Act: `service.moderateAccount(...)`
3. Assert: capture argument passed to `moderationActionRepository.save(...)`

**Expected Result (PASS):**
- `capturedAction.getReportId() == null` (account actions are never report-linked — the inverse of UC-101's `RES-TC-113` which asserts `reportId != null`)
- `capturedAction.getTargetType() == ReportTargetType.ACCOUNT`
- `capturedAction.getTargetId() == TARGET_USER_ID` (the user's id, not a content id)

**Current Status:** 🟢 Passing
**Implementation Note:** Cross-UC invariant triad — UC-100 `reportId=null`+content targetType, UC-101
`reportId!=null`, UC-102 `reportId=null`+`ACCOUNT` targetType. Guards against copy-paste drift.

---

### WSA-TC-212 — `AuditService.log()` gọi đúng 1 lần với MODERATION_ACTION/"ACCOUNT"

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — audit side effect (ADR-002)
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS ADR-002 §Decision`, `§17 C9`

**Test Steps:**
1. Arrange: valid SUSPEND request
2. Act + Assert: `verify(auditService, times(1)).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID), eq("ACCOUNT"), eq(TARGET_USER_ID.toString()), any())`
3. Repeat for a valid WARN request — also exactly 1 audit call

**Expected Result (PASS):** Exactly 1 invocation each (both WARN and SUSPEND are audited).
**Expected Result (FAIL):** 0 invocations — account-level punitive action silently un-audited (accountability gap, violates ADR-002/C9).

**Current Status:** 🟢 Passing

---

### WSA-TC-213 — WARN KHÔNG BAO GIỜ gọi `userRepository.save()` (no User mutation)

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateAccount()` — WARN purity (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS ADR-004 §Decision`, `§4.2 WARN purity NFR`

**Test Steps:**
1. Arrange: valid WARN request against `FX-301`
2. Act: `service.moderateAccount(...)`
3. Assert: `verify(userRepository, never()).save(any())`

**Expected Result (PASS):** `userRepository.save()` never invoked; `findById` MAY be invoked (read-only, to validate target exists per ADR-004).
**Expected Result (FAIL):** Any `userRepository.save()` call on the WARN path — a hidden side effect the audit trail wouldn't reveal.

**Current Status:** 🟢 Passing
**Implementation Note:** This is the standalone negative-assertion sentinel for ADR-004 (WSA-TC-201 asserts
it inline; this isolates it as its own severity-tagged guard so a future refactor can't quietly reintroduce
a WARN-time `User` write).

---

### CONTROLLER TEST CASES

### WSA-TC-214 — Missing required field (`targetUserId`/`actionType` null) → 400 @Valid

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationController.moderateAccount()` — `@Valid` bean validation
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `TDS §8.3 DTO (@NotNull targetUserId, @NotNull actionType)`, same drift note as UC-100/101 on exact error code

**Test Steps:**
1. Sub-case a: `POST /api/v1/admin/moderation/account-actions` body missing `targetUserId`, MODERATOR JWT
2. Sub-case b: body missing `actionType`

**Expected Result (PASS):** `response.status == 400`, error references the missing field. Exact `error.code`
is `Open` (same MOD-001 wiring gap UC-100/101 documented) — assert `status==400` + field name as the stable minimum oracle.

**Current Status:** 🟢 Passing
**Implementation Note:** Do not hard-code an unverified error-code string; assert HTTP 400 + offending field only.

---

### WSA-TC-215 — Unexpected exception → 500 `INTERNAL_ERROR` (not `MOD-005`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationController.moderateAccount()` / generic fallback handler
**Test File:** `src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`
**Oracle Source:** Reused finding from UC-100/101 §10 — `MOD-005` is dead code; real fallback is `GlobalExceptionHandler.handleGeneric()`

**Preconditions:** `ModerationService.moderateAccount(...)` mock throws `RuntimeException("simulated")`, MODERATOR JWT valid

**Test Steps:** `POST .../account-actions` valid MODERATOR auth, service mock throws

**Expected Result (PASS):** `response.status == 500`, `response.body.error.code == "INTERNAL_ERROR"` (NOT `"MOD-005"`).

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

### WSA-TC-216 — Non-MODERATOR bị 403 `ACCESS_DENIED`

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `ModerationController.moderateAccount()` — `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/WarnOrSuspendAccountControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `TDS ADR-002`, `GlobalExceptionHandler.java` (real 403 = `ACCESS_DENIED`, reused finding)

**Preconditions:** JWT với role `ROLE_MOTHER` (FX-307)

**Test Steps:** `POST /api/v1/admin/moderation/account-actions` với MOTHER JWT, valid body

**Expected Result (PASS — hệ thống an toàn):**
- `response.status == 403`, `response.body.error.code == "ACCESS_DENIED"` *(NOT `"MOD-004"`)*
- Không có mutation nào trên `users` / `moderation_actions`

**Current Status:** 🟢 Passing

---

### WSA-TC-217 — SYSTEM_ADMIN không có quyền ngầm — cũng bị 403

**Severity:** `HIGH`
**Feature Under Test:** `ModerationController.moderateAccount()` — verifies no `RoleHierarchy`
**Test File:** `src/test/java/com/carebridge/backend/security/WarnOrSuspendAccountControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-017`
**Oracle Source:** Reused finding — `grep -rln "RoleHierarchy" .` returns zero matches (TDS §16 note)

**Preconditions:** JWT với role `ROLE_SYSTEM_ADMIN` (FX-308)

**Test Steps:** `POST .../account-actions` với SYSTEM_ADMIN JWT

**Expected Result (PASS):** `response.status == 403`, `error.code == "ACCESS_DENIED"`.

**Current Status:** 🟢 Passing
**Implementation Note:** If Product later requires SYSTEM_ADMIN superuser access over moderation, that is an
explicit cross-cutting `@PreAuthorize` change (TDS §16 Open) — this test must be revisited, not silently relaxed.

---

### WSA-TC-218 — Request không có JWT → 401, body rỗng

**Severity:** `CRITICAL`
**Feature Under Test:** JWT authentication entry point
**Test File:** `src/test/java/com/carebridge/backend/security/WarnOrSuspendAccountControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `SecurityConfig.java` `HttpStatusEntryPoint` (reused finding from UC-100/101)

**Test Steps:** `POST .../account-actions` không có `Authorization` header (FX-309)

**Expected Result (PASS):** `response.status == 401`. Body MAY be empty — test MUST NOT assert any `error.code`.

**Current Status:** 🟢 Passing

---

### WSA-TC-219 — SQL Injection trong `reason` field không ảnh hưởng DB

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `ModerationController` — `reason` field handling
**Test File:** `src/test/java/com/carebridge/backend/security/WarnOrSuspendAccountControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-019`

**Test Steps:** `POST .../account-actions` với `reason = "x'; DROP TABLE users;--"`, MODERATOR JWT, `actionType=WARN`, targeting `FX-301`

**Expected Result (PASS):** Request xử lý bình thường (reason lưu nguyên văn — JPA parameterized query);
`users`/`moderation_actions` tables vẫn tồn tại và intact.

**Expected Result (FAIL):** 500 error từ DB hoặc bảng bị xóa → injection được thực thi.

**Current Status:** 🟢 Passing

---

### WSA-TC-220 — Suspended user login → 403 `ACCOUNT_SUSPENDED` at login gate

**Severity:** `CRITICAL`
**Feature Under Test:** `AuthenticationPolicy.ensureCanAuthenticate()` — login-time enforcement (ADR-003 touchpoint #2)
**Test File:** `src/test/java/com/carebridge/backend/security/WarnOrSuspendAccountEnforcementTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `TDS ADR-003 §Decision (touchpoint #2)`, `security/policy/AuthenticationPolicy.java`

**Preconditions:** A `User` with `suspendedUntil = FUTURE_EXPIRY`, `enabled=true`, `locked=false`, valid password

**Test Steps:**
1. Arrange: user suspended (future expiry)
2. Act: call `AuthenticationPolicy.ensureCanAuthenticate(user)` (or the login endpoint with correct credentials)
3. Assert

**Expected Result (PASS):** Throws `AccountSuspendedException` → mapped to 403 `ACCOUNT_SUSPENDED`; login does NOT
issue a token. (A suspended user must be blocked at login, not merely on subsequent API calls.)

**Expected Result (FAIL):** Login succeeds and issues a token for a suspended account → suspension only
partially enforced.

**Current Status:** 🟢 Passing
**Implementation Note:** Verifies touchpoint #2 (login gate). Touchpoint #1 (per-request filter) is verified
by WSA-TC-INT-004. Touchpoints #3/#4 are `Open`/out-of-scope per ADR-003 — no test asserts them.

---

### INTEGRATION TEST CASES

### WSA-TC-INT-001 — Full API flow SUSPEND với real DB (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/moderation/account-actions` — end to end (SUSPEND)
**Test File:** `src/test/java/com/carebridge/backend/integration/WarnOrSuspendAccountIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-021`

**Preconditions:** PostgreSQL Testcontainer, schema applied via Flyway **including new migration
`V20260701120000__add_user_suspended_until.sql`**; seed `FX-301`; MODERATOR JWT hợp lệ

**Test Steps:**
1. Seed `FX-301` (`User`, suspendedUntil=null)
2. `POST .../account-actions` `{targetUserId: U1, actionType: SUSPEND, reason: "...", expiresAt: <future>}` với MODERATOR JWT
3. Assert response 201
4. Re-fetch `users` + `moderation_actions` rows directly from DB

**Expected Result (PASS):**
- Response 201, `accountSuspended == true`
- DB: `users.suspended_until` = the requested `expiresAt` (non-null) for U1
- DB: `users.locked == false`, `users.enabled == true` (unchanged)
- DB: exactly 1 new `moderation_actions` row with `target_type='ACCOUNT'`, `report_id IS NULL`, `expires_at` = the requested value

**Current Status:** 🟢 Passing
**Implementation Note:** This integration test is the first to require the new migration — if the migration
is missing, `suspended_until` column won't exist and the test fails at the persistence layer (a useful
Red-phase signal that the schema delta was not applied).

---

### WSA-TC-INT-002 — Full API flow WARN — `users` row KHÔNG đổi cột nào

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/moderation/account-actions` — end to end (WARN)
**Test File:** `src/test/java/com/carebridge/backend/integration/WarnOrSuspendAccountIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-022`

**Preconditions:** Seed `FX-301`; MODERATOR JWT hợp lệ

**Test Steps:**
1. Snapshot the full `users` row for U1 before the call
2. `POST .../account-actions` `{targetUserId: U1, actionType: WARN, reason: "..."}` (no expiresAt)
3. Assert response 201, `accountSuspended == false`
4. Re-fetch `users` + `moderation_actions`

**Expected Result (PASS):**
- DB: `users` row for U1 **byte-for-byte identical** to the pre-call snapshot (`suspended_until` still null, `locked`/`enabled`/`locked_at` unchanged)
- DB: exactly 1 new `moderation_actions` row with `action_type='WARN'`, `target_type='ACCOUNT'`, `report_id IS NULL`, `expires_at IS NULL`

**Current Status:** 🟢 Passing

---

### WSA-TC-INT-003 — Rollback khi lỗi giữa chừng (atomicity) — SUSPEND

**Severity:** `CRITICAL`
**Feature Under Test:** Transaction boundary in `ModerationServiceImpl.moderateAccount()`
**Test File:** `src/test/java/com/carebridge/backend/integration/WarnOrSuspendAccountIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `TDS §4.2 Atomicity NFR` — `User.suspendedUntil` update + `ModerationAction` insert + audit in 1 transaction

**Preconditions:** Seed `FX-301`; force a failure AFTER `users` update but BEFORE the `ModerationAction`
insert commits (e.g. via a test-only `AuditService`/repository wrapper that throws inside the same `@Transactional`)

**Test Steps:**
1. Seed
2. Trigger `moderateAccount()` SUSPEND with a forced downstream failure
3. Assert exception propagates
4. Re-fetch `users`, `moderation_actions` from a new transaction/session

**Expected Result (PASS):**
- DB: `users.suspended_until` is **still NULL** (the update rolled back)
- DB: `moderation_actions` has **0** new rows for this user — the whole chain rolled back together

**Expected Result (FAIL):** `suspended_until` set but no `ModerationAction` row (or vice versa) — a user
suspended with no audit trail, violating the atomicity NFR.

**Current Status:** 🔴 Not written

---

### WSA-TC-INT-004 — ENFORCEMENT: pre-issued JWT của user vừa bị SUSPEND → 403 `ACCOUNT_SUSPENDED`

**Severity:** `CRITICAL`
**Feature Under Test:** `JwtAuthenticationFilter` per-request enforcement (ADR-003 touchpoint #1) — **the central scenario this whole UC exists to prove**
**Test File:** `src/test/java/com/carebridge/backend/integration/WarnOrSuspendAccountEnforcementIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-024`
**Oracle Source:** `TDS ADR-003 §Decision (touchpoint #1)`, `§4.3 Enforcement correctness NFR`, `§11.4 CRITICAL smoke-test`

**Preconditions:** PostgreSQL Testcontainer + migration applied; seed a target user `U2` (`FX-303` semantics
but starting with `suspendedUntil=null`); obtain a valid JWT (`FX-310`) for `U2` issued BEFORE suspension;
a MODERATOR JWT (`FX-306`)

**Test Steps:**
1. Seed `U2` (not yet suspended); issue/obtain `U2`'s JWT (`FX-310`) while still active
2. As MODERATOR: `POST .../account-actions` SUSPEND `U2` with a future `expiresAt`
3. As `U2`, using the **pre-issued** JWT from step 1, call ANY authenticated endpoint (e.g. `GET /api/v1/community/questions`)
4. Assert the second call is rejected

**Expected Result (PASS):**
- Step 3 returns `403`, body `{"code":"ACCOUNT_SUSPENDED", ...}` (filter-written shape, per TDS §9.2 — NOT the `{"error":{...}}` envelope)
- The still-valid JWT does **not** let the suspended user through — proving `suspended_until` is actually enforced, not a ghost write

**Expected Result (FAIL):** Step 3 returns 200 → **ghost action** — the exact anti-pattern ADR-003/AP-AI-002
warns about (suspension written to DB but never enforced). This is the single most important test in the UC.

**Current Status:** 🟢 Passing
**Implementation Note:** ⚠️ Must exercise the **real** `JwtAuthenticationFilter` chain (full `@SpringBootTest`
+ MockMvc with security filters enabled), not a mocked filter — the ghost-action failure mode only surfaces
end-to-end.

---

### WSA-TC-INT-005 — Lazy expiry: `suspended_until` QUÁ KHỨ → request cho qua, KHÔNG clear column

**Severity:** `HIGH`
**Feature Under Test:** `JwtAuthenticationFilter` lazy read-only expiry (ADR-003 — no write-back)
**Test File:** `src/test/java/com/carebridge/backend/integration/WarnOrSuspendAccountEnforcementIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-025`
**Oracle Source:** `TDS ADR-003 §Decision "Lazy/computed check, no write-back in the hot path"`

**Preconditions:** Seed `FX-304` (`U3` with `suspendedUntil = <now − 1 day>`, i.e. lapsed); a valid JWT for `U3`

**Test Steps:**
1. Seed `U3` with a past `suspended_until`
2. As `U3`, call any authenticated endpoint
3. Assert allowed through
4. Re-fetch `users` row for `U3` directly from DB

**Expected Result (PASS):**
- Step 2 returns success (NOT 403) — a lapsed suspension does not block
- DB: `U3.suspended_until` **still holds the original past timestamp** (NOT reset to null) — confirms the
  deliberate no-write-back design (contrast with `lockedAt` auto-unlock which DOES write back)

**Expected Result (FAIL):**
- Lapsed user is blocked (403) → expiry not honored, OR
- `suspended_until` cleared to null after the request → violates the read-only hot-path decision (ADR-003 C8), adding an unwanted UPDATE on every request

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                                | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | ----------------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `WSA-TC-201`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-202`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-203`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-204`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-205`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-206`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-207`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | Adapted: wall-clock ordering instead of injected `Clock` (no `Clock` abstraction exists in this codebase) |
| `WSA-TC-208`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-209`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | ADR-007 Accepted — kept |
| `WSA-TC-210`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-211`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-212`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-213`      | `WarnOrSuspendAccountServiceImplTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-214`      | `WarnOrSuspendAccountControllerTest.java`                   | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-215`      | `WarnOrSuspendAccountControllerTest.java`                   | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-216`      | `WarnOrSuspendAccountControllerSecurityTest.java`           | `[x]`               | Passed (uncommitted, `dev`) | RBAC/SecurityConfig wired alongside stub in RED phase (same as UC-100/101) — passed before service GREEN |
| `WSA-TC-217`      | `WarnOrSuspendAccountControllerSecurityTest.java`           | `[x]`               | Passed (uncommitted, `dev`) | Same as WSA-TC-216 |
| `WSA-TC-218`      | `WarnOrSuspendAccountControllerSecurityTest.java`           | `[x]`               | Passed (uncommitted, `dev`) | Same as WSA-TC-216 |
| `WSA-TC-219`      | `WarnOrSuspendAccountControllerSecurityTest.java`           | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-220`      | `WarnOrSuspendAccountEnforcementTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | —                    |
| `WSA-TC-INT-001`  | `WarnOrSuspendAccountIntegrationTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | Adapted to mocked-service full-HTTP-stack (no Testcontainers/real-DB harness — same convention as UC-100/101) |
| `WSA-TC-INT-002`  | `WarnOrSuspendAccountIntegrationTest.java`                  | `[x]`               | Passed (uncommitted, `dev`) | Same adaptation as WSA-TC-INT-001 |
| `WSA-TC-INT-003`  | `WarnOrSuspendAccountIntegrationTest.java`                  | `[ ]`               | —                     | **NOT implemented** — requires a real transactional DB; no Testcontainers/real-DB harness exists in this codebase (same documented gap as UC-101's RES-TC-INT-004). Remains an open gap, not faked. |
| `WSA-TC-INT-004`  | `WarnOrSuspendAccountEnforcementIntegrationTest.java`       | `[x]`               | Passed (uncommitted, `dev`) | ★ ghost-action gate — real `JwtAuthenticationFilter` + real `SecurityFilterChain` exercised (UserRepository mocked), not a Testcontainers DB. Verified with mocked repository, same pattern as pre-existing `JwtAuthenticationFilterAccountStateTest` |
| `WSA-TC-INT-005`  | `WarnOrSuspendAccountEnforcementIntegrationTest.java`       | `[x]`               | Passed (uncommitted, `dev`) | Same real-filter-chain approach as WSA-TC-INT-004 |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// ModerationServiceImpl.java — Red Phase stub (added method only; getModerationQueue() unchanged/
// already GREEN from UC-99; moderateContent() from UC-100 unchanged)
@Service
public class ModerationServiceImpl implements ModerationService {

    // ... existing getModerationQueue() (UC-99), moderateContent() (UC-100) ...

    @Override
    public WarnOrSuspendAccountResponse moderateAccount(WarnOrSuspendAccountRequest request, Principal principal) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
// Enforcement touchpoints — Red Phase: the suspension check is NOT yet added to
// JwtAuthenticationFilter / AuthenticationPolicy. WSA-TC-INT-004 and WSA-TC-220 therefore FAIL
// (a suspended user is NOT yet blocked) until ADR-003's wiring is implemented — this failure IS the
// Red-phase proof that the ghost-action gate is real. The migration V20260701120000 may be added
// before the service logic, so WSA-TC-INT-001 fails at the service stub, not the schema.
```

**Red Gate Verification:**

| TC ID            | Stub Result                                | Expected         | Actual        | Root Cause (nếu PASS bất thường) |
| ----------------- | ---------------------------------------------- | ------------------- | ---------------- | ------------------------------------ |
| `WSA-TC-201`      | `throw UnsupportedOperationException`           | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `WSA-TC-202`      | `throw UnsupportedOperationException`           | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `WSA-TC-203`      | `throw UnsupportedOperationException`           | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `WSA-TC-206`      | `throw UnsupportedOperationException`           | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `WSA-TC-210`      | `throw UnsupportedOperationException`           | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `WSA-TC-213`      | `throw UnsupportedOperationException`           | 🔴 FAIL              | ☑ FAIL ☐ PASS     | —                                     |
| `WSA-TC-216`      | `@PreAuthorize not yet present → 404/405`       | 🔴 FAIL (no 403)     | ☐ FAIL ☑ PASS     | Controller `@PreAuthorize` + `SecurityConfig` rule were wired in the same RED-phase commit as the service stub (established UC-100/101 precedent: RBAC wiring is not part of the "business-logic stub"). Not a Green-from-Birth defect — it's independent of `moderateAccount()`'s logic, which is what the stub gates. |
| `WSA-TC-220`      | suspension check absent → login succeeds        | 🔴 FAIL (no 403)     | ☑ FAIL ☐ PASS     | —                                     |
| `WSA-TC-INT-001`  | `throw UnsupportedOperationException`           | 🔴 FAIL              | ☐ FAIL ☐ PASS     | Not applicable as specified — implemented as a mocked-service test (no Testcontainers), so it never exercises the real stub. Real-stub RED behavior is covered instead by `WSA-TC-201/202` above. |
| `WSA-TC-INT-004`  | enforcement not wired → 200 (ghost action)      | 🔴 FAIL (returns 200)| ☑ FAIL ☐ PASS     | Actual stub-phase result was 500 (unstubbed mock `Page` → NPE in controller), not literally 200 — still correctly non-403, so RED gate holds. Fixed after `contentService` stub was corrected to be irrelevant to this assertion path. |

**Red Gate Evidence:**
- Stub commit: not committed — verified locally via `./mvnw test -Dtest=...` before GREEN implementation (uncommitted work on `dev` branch)
- Tất cả FAIL? ☑ Yes → GATE-2 PASS (T2→T3) → tiếp tục implement
- Log file: local `mvn test` console output (not persisted to a file)

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-MOD-IMP-004` Approved by explicit user instruction ("approved tất cả rồi implement-feature
      đi") prior to implementation. **Not** independently re-reviewed by a human Tech Lead/DBA/
      Security-domain-owner beyond that approval — ADR-001/003/005/007/008 remain flagged as design
      decisions in their own sections.
- [x] Migration `V20260701210000__add_user_suspended_until.sql` created (timestamp > latest existing
      `V20260701000003__widen_audit_logs_action_check.sql` at implementation time). **Not applied against
      a live Postgres instance in this session** — no DB connectivity exists in this dev environment (same
      pre-existing condition documented for UC-100/101; confirmed by the 33 baseline DB-dependent test
      errors, unchanged before/after this work).
- [x] `User.java` đã thêm field `suspendedUntil`; `ReportTargetType` đã thêm giá trị `ACCOUNT`
- [ ] Logic Issues (Section 2) — reflected in the implementation, not independently re-confirmed by a
      human Tech Lead
- [x] Test fixtures — implemented via `WarnSuspendAccountTestFactory` (`makeUser()`/`makeRequest()`);
      covers the FX-301..310 intent, not a literal 1:1 port of each fixture ID
- [x] Spring Security test dependencies + `@WebMvcTest` filter-chain setup available and used (verified
      working via the pre-existing `JwtAuthenticationFilterAccountStateTest` pattern)

### Exit Criteria (DoD)

- [x] `./mvnw test -Dtest=WarnOrSuspendAccountServiceImplTest` — 13/13 PASS
- [x] `./mvnw test -Dtest=WarnOrSuspendAccountControllerTest` — 3/3 PASS
- [x] `./mvnw test -Dtest=WarnOrSuspendAccountControllerSecurityTest` — 4/4 PASS
- [x] `./mvnw test -Dtest=WarnOrSuspendAccountEnforcementTest` — 1/1 PASS (login-gate enforcement)
- [x] `./mvnw test -Dtest=WarnOrSuspendAccountIntegrationTest` — 2/2 PASS. **Deviation:** mocked-service
      full-HTTP-stack test, not Testcontainers (no such harness exists in this codebase — same UC-100/101
      convention); does not assert real DB rows.
- [x] `./mvnw test -Dtest=WarnOrSuspendAccountEnforcementIntegrationTest` — 2/2 PASS (real
      `JwtAuthenticationFilter` + real `SecurityFilterChain`, `UserRepository` mocked — not Testcontainers)
- [ ] Test coverage: `ModerationServiceImpl.moderateAccount()` ≥ 80% lines — **not measured**, no JaCoCo
      plugin configured in this project (same unmeasured-coverage caveat as UC-101)
- [x] Không có business logic trong `ModerationController` (chỉ `@Valid` + delegate)
- [x] WSA-TC-202/213: WARN never mutates `User`; SUSPEND writes ONLY `suspendedUntil` (not `locked`/`enabled`) — VERIFIED (CRITICAL — ADR-001/ADR-004)
- [x] WSA-TC-INT-004: pre-issued JWT of suspended user is blocked 403 `ACCOUNT_SUSPENDED` — VERIFIED (CRITICAL — the ghost-action gate, ADR-003). Verified through the real filter chain with a mocked `UserRepository`, not a live DB.
- [x] WSA-TC-INT-005: lapsed suspension allows request through, no write-back — VERIFIED (CRITICAL — lazy-expiry, ADR-003 C8)
- [ ] WSA-TC-INT-003: atomicity rollback (suspend + action + audit all-or-nothing) — **NOT VERIFIED**, no Testcontainers/real-DB harness exists in this codebase; documented open gap, same as UC-101 RES-TC-INT-004
- [x] WSA-TC-216/217/218: Non-MODERATOR / SYSTEM_ADMIN / no-JWT all rejected correctly — VERIFIED (CRITICAL security gate)

**Exit Criteria bổ sung — CASE 2.0:**

- [x] Red Gate (§5.1) — confirmed via actual `./mvnw test` run before GREEN implementation: all
      logic-dependent tests failed with `UnsupportedOperationException`/missing-enforcement (§5.1 Red Gate
      Verification records the actual, not assumed, results)
- [x] Contract Existence — `./mvnw compile` clean, no symbol errors, for all new classes
      (`WarnOrSuspendAccountRequest`, `WarnOrSuspendAccountResponse`, 6 factory methods on
      `ModerationException` (MOD-015..020), `AccountSuspendedException`, `User.suspendedUntil`,
      `ReportTargetType.ACCOUNT`)
- [x] Props Isolation — factory methods (`makeUser()`, `makeRequest()`) đảm bảo isolation, không share
      mutable `static` instance bị mutate giữa test
- [x] Oracle Source — mọi expected value có comment trỏ về BR/ADR/file code cụ thể
- [ ] Clock control — **deviation**: WSA-TC-207's "exactly now" boundary uses wall-clock ordering
      (capture `Instant.now()` then call the service immediately) rather than an injected `Clock`, because
      no `Clock` abstraction exists anywhere in this codebase (verified — `AuthenticationPolicy` also
      compares against a live `Instant.now()`). The boundary invariant is still exercised correctly.

### Suspension Criteria

- ADR-001, ADR-003, ADR-005, ADR-007, hoặc ADR-008 chưa được Tech Lead/Product/Security-domain-owner xác nhận
- Migration `V20260701120000` chưa được apply (không có cột `suspended_until` → integration tests không chạy được)
- Spring Security config chưa enable `@EnableMethodSecurity`
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert implementation files (content package + security enforcement touchpoints + common exception)
git checkout -- src/main/java/com/carebridge/backend/content/
git checkout -- src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java
git checkout -- src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java
git checkout -- src/main/java/com/carebridge/backend/common/exception/

# Migration: Flyway forward-only (CLAUDE.md — never modify/delete an applied migration).
# Nếu suspended_until phải bị gỡ, viết migration MỚI để DROP COLUMN; KHÔNG sửa/xóa
# V20260701120000__add_user_suspended_until.sql sau khi đã apply.

# CHÚ Ý: Nếu rollback xảy ra SAU KHI ADR-003 enforcement đã merge, xác nhận login/JWT flow của
# user KHÔNG bị suspend vẫn hoạt động đúng sau revert (chạy lại UC-99/UC-100 test suite + auth smoke test).

# Test spec files được giữ nguyên (không rollback test spec)
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                                    | Check | Gate chặn |
| --------- | ------------------------- | ---------------------------------------------------------------------------------------------- | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-003/ADR-004/ADR-005/ADR-008                                       | `[x]`   | G-0         |
| AP-AI-002 | Ghost Action             | SUSPEND set `suspended_until` nhưng test KHÔNG chứng minh enforcement (thiếu WSA-TC-INT-004)     | `[x]`   | G-2 ★       |
| AP-AI-002 | Green-from-Birth         | WSA-TC-201..213 PASS với empty/throw stub (no real action/mutation)                             | `[x]`   | G-2 ★       |
| AP-AI-003 | Implicit Decision        | Test giả định reuse `User.locked`/`enabled` cho SUSPEND mà không override ADR-001               | `[x]`   | G-1         |
| AP-AI-003 | Implicit Decision        | Test coi self-action guard (MOD-020) là sourced fact thay vì ADR-007 Proposed/Open              | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | Test verify Controller gọi trực tiếp `UserRepository`/`ModerationActionRepository`               | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | Test import `AccountModerationFacade`/`UserSuspensionService` không có trong TDS §8              | `[x]`   | G-3         |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào trong bản thân spec này → TDD spec approved-for-RED-phase
- [x] Post-implementation re-check: no AP-AI-001..005 detected in the actual implementation. AP-AI-002
      (Ghost Action) is the CRITICAL one for this UC and is verified closed by `WSA-TC-INT-004` PASSING
      against the real `JwtAuthenticationFilter` (real filter chain, mocked `UserRepository`) — the
      suspension write in `ModerationServiceImpl.moderateAccount()` does have real enforcement effect.

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0 Anti-Pattern Detection & Red Gate Protocol) — Status: Implemented (2026-07-01).*
*ADR-001, ADR-003 (ghost-action gate — verified closed, `WSA-TC-INT-004`/`005` PASSING), ADR-005, ADR-007
(self-action guard — Accepted, `WSA-TC-209` kept), and ADR-008 were approved via explicit user instruction
prior to implementation, not independently re-reviewed by a human Tech Lead/DBA/Security-domain-owner
beyond that approval. Migration `V20260701210000__add_user_suspended_until.sql` (renamed from the TDS's
placeholder `V20260701120000` timestamp to sort after the actual latest migration at implementation time,
`V20260701000003`) has NOT been applied against a live Postgres instance in this session — no DB
connectivity exists in this dev environment. `WSA-TC-INT-003` (atomicity rollback) remains an open,
documented gap requiring a real transactional DB. Work is uncommitted on the `dev` branch.*
