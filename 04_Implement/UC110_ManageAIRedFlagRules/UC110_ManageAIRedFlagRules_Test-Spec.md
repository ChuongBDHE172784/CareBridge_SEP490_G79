# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test Specification — UC-110: Manage AI and Red-Flag Rules

**Document ID:** `CB-MOD-TEST-005`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Implemented — 2026-07-02 (18/18 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] N/A — no PII in this module (see TDS §1)`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `04_Implement/UC110_ManageAIRedFlagRules/UC110_ManageAIRedFlagRules_TDS.md` (`CB-MOD-IMP-005`) — Technical Design Specification
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — schema baseline (verified: no pre-existing `red_flag_rules` table)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260702002000__create_red_flag_rules.sql` — new migration (proposed, TDS §5.2)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.12 — UC-110 Manage AI and Red-Flag Rules
- `CLAUDE.md` — BR-SAFETY ("AI provides guidance only; never diagnose, prescribe, or delay emergency routing")
- `triage/policy/TriageRedFlagPolicy.java`, `integration/gemini/filter/TriageRedFlagSafetyFilter.java`, `integration/gemini/dto/RagSafetyResult.java` — existing code under test/modification

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data (module này không xử lý PII).

---

## CHANGELOG

| Ngày       | Người thực hiện   | Nội dung thay đổi                                              |
| ---------- | -------------------- | ------------------------------------------------------------------ |
| 2026-07-01 | AI Agent — Winston   | Khởi tạo tài liệu — Test-Spec cho UC-110 (Draft, chưa implement)   |
| 2026-07-02 | AI Agent — Claude (Audit Pass) | Audit fix: (1) migration filename `V20260701000001__create_red_flag_rules.sql` collided with an actual existing migration (`V20260701000001__create_topic_follows.sql`) — corrected to `V20260702002000__create_red_flag_rules.sql` throughout (References, TDS-05, RFR-TC-002, RFR-TC-INT-001, §6, §7); (2) `RFR-TC-SEC-002` asserted a non-existent `IAM-001` JSON error code for missing-JWT 401 — corrected to the verified real behavior (`HttpStatusEntryPoint(UNAUTHORIZED)`, empty body, no JSON envelope), matching UC-100's sibling correction. No test case Current Status changed (all remain 🔴 Not written — no production code exists yet). No Status change (remains Draft). |
| 2026-07-02 | AI Agent — Amelia (Dev Agent) | Phase 3 Implementation — user confirmed Approved for TDS+Test-Spec. Red Gate run confirmed 14/18 TCs FAIL as expected (RFR-TC-001,002,004-015); 3 TCs (RFR-TC-003, RFR-TC-SEC-001, RFR-TC-SEC-002) legitimately PASS at Red Gate because their subject (bean validation / Spring Security filter chain) already exists independently of the stubbed service — not AP-AI-002 (documented in §5.1 root-cause column). Implemented `RedFlagRuleServiceImpl` + `TriageRedFlagPolicy` (floor-first, fail-closed). Repository query methods renamed `findBySeverityAndActiveTrue`/`findBySeverityAndActive` (dropped "Is" prefix) — verified Spring Data property-path resolution rejects "isActive" as a literal segment for a boolean property exposed via an `isActive()` getter; TDS's `findBySeverityAndIsActiveTrue` naming does not compile against this codebase's entity shape. `MOD-024` (blank-keyword validation) is never actually thrown by `RedFlagRuleServiceImpl` — bean validation (`@NotBlank`) fails before the controller method body runs, producing the framework's generic `VALIDATION_ERROR` envelope, not a custom code (same class of finding as this doc's earlier `IAM-001` correction); RFR-TC-003 asserts status only, matching sibling `AdminContentControllerTest` convention. RFR-TC-INT-001 hosted as a `@SpringBootTest` against the H2 test datasource (this codebase has no Testcontainers/real-Postgres harness anywhere — verified project-wide) exercising real Spring-managed beans end-to-end (`RedFlagRuleService` → JPA → `TriageRedFlagSafetyFilter`), not a mocked-service MockMvc test — preserves the read-through/no-cache assertion the TC exists to verify. All 18/18 TCs GREEN; full-suite regression run confirmed 0 regressions (16 pre-existing failures in `community`/`exercise` packages verified present on baseline HEAD via `git stash`, unrelated to this module). Status: Approved → Implemented. |
| 2026-07-03 | AI Agent — Claude | Real-DB migration verification (closes the one gap the H2-only test suite structurally cannot cover): booted the app against the real Supabase Postgres instance. First boot **failed the migration** — `V20260702002000__create_red_flag_rules.sql`'s FK constraints referenced `users(id)`, but the actual `users` table's PK column is `user_id` (`V1__init_schema.sql`). This was invisible to all 18 GREEN tests because H2 test schema is generated from JPA entities, never from the migration's literal SQL. Fixed the FK column name in the migration file and in TDS §5.2, re-ran: Flyway applied the migration cleanly (schema advanced to version `20260702002000`), and `SELECT count(*) FROM red_flag_rules` confirmed all 19 seed rows present with `is_system_default = true`. No test case's Current Status changes as a result (this was a migration-file bug, not a test-case bug) — recorded here per the skill's "remaining risks" obligation. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field                      | Value                                                                        |
| --------------------------- | ------------------------------------------------------------------------------- |
| **Feature / Gap ID**       | `UC-110`                                                                       |
| **Module**                 | `Manage AI and Red-Flag Rules — triage bounded context (admin sub-feature)`    |
| **Spec gốc**                | `CB-MOD-IMP-005` (TDS, this batch)                                             |
| **Priority**                | 🔴 P0 *(BR-SAFETY-critical subset, RFR-TC-011/012)* / 🟠 P1 *(CRUD)*           |
| **Sprint**                  | `Open — not yet scheduled`                                                     |
| **Milestone**               | `Open`                                                                          |
| **Data Classification**    | `Internal` — no PII                                                            |
| **Compliance Scope**       | `BR-SAFETY (CLAUDE.md)` — no GDPR/PDPA-specific obligation for this table       |
| **Upstream Dependencies**  | `security (JWT auth)`, `audit (AuditService)`                                  |
| **Downstream Consumers**   | `triage.policy.TriageRedFlagPolicy`, `integration.gemini.filter.TriageRedFlagSafetyFilter` (UC-132 RAG pipeline) |

### 1.1 AI Generation Context (CASE 2.0)

| Field                     | Value                                                                                          |
| --------------------------- | -------------------------------------------------------------------------------------------------- |
| **AI Assisted?**           | `Yes`                                                                                              |
| **Constraint Source**      | `CB-MOD-IMP-005 §17.1` (C1-C7)                                                                     |
| **Constraints Injected**   | C1 (RBAC SYSTEM_ADMIN), C2 (floor-first check), C3 (fail-closed on DB error), C4 (system-default guard), C5 (audit logging), C6 (no caching — ADR-004 Open), C7 (no GREEN/YELLOW runtime wiring — ADR-003) |
| **Model**                  | `Claude (Sonnet) — create-specs skill`                                                             |
| **Trust Level**            | `T1 → T2 (pending Red Gate, §5.1)`                                                                 |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | FS §3.2.2.12 mô tả "green/yellow/red levels, and actions" như thể cả 3 mức đều có hành vi runtime riêng | `RagSafetyResult` (DTO thực tế) chỉ có nhị phân `safe`/`redFlag` — không có pipeline cho GREEN/YELLOW (verified: `RagSafetyResult.java`, `TriageRedFlagSafetyFilter.java`) | Test chỉ assert hành vi runtime cho `severity=RED, action=ESCALATE, isActive=true`. Test cho GREEN/YELLOW chỉ assert **persistence** (CRUD lưu đúng), KHÔNG assert bất kỳ runtime side-effect nào (ADR-003, TDS §3) |
| L2 | Một cách đọc ngây thơ FS có thể nghĩ "xoá rule" luôn được phép cho System Admin (quyền cao nhất) | BR-SAFETY (CLAUDE.md) cấm "delay emergency routing" — xoá toàn bộ rule mặc định sẽ tạo lỗ hổng fail-safe (TDS ADR-001/BR-SAFETY-RFR-003) | Test `RFR-TC-007`/`RFR-TC-009` xác nhận DELETE/deactivate bị từ chối (`MOD-027`) cho `isSystemDefault=true`, **kể cả khi actor là SYSTEM_ADMIN hợp lệ** — đây không phải vấn đề phân quyền role mà là service-level invariant |
| L3 | Không có FK seed user cố định trong Flyway (`DevDataSeeder.java` tạo admin account lúc app start, không phải migration-time) | Migration TDS §5.2 dùng `created_by = NULL` cho 19 seed rows | Test fixture cho seed rows dùng `createdBy = null`, không giả định một UUID admin cố định nào |
| L4 | `RedFlagException`/`RED_FLAG_DETECTED` (existing) trông như đã wired cho red-flag detection | Verified: chưa từng được throw ở đâu trong codebase hiện tại — dead code, không liên quan đến UC-110 | Test KHÔNG assert bất kỳ tương tác nào với `RedFlagException` — module mới dùng `RedFlagRuleException` riêng (`MOD-024..027`) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Manage AI and Red-Flag Rules bao gồm các layer:
├── Domain (RedFlagRule entity — pure data, no logic beyond JPA mapping)
├── Service (RedFlagRuleServiceImpl — mock JPA Repository + AuditService với Mockito)
├── Controller (RedFlagRuleController — mock Service với @WebMvcTest)
├── Integration point (TriageRedFlagPolicy.isRedFlag() — mock RedFlagRuleRepository để test fail-safe behavior — CRITICAL layer)
└── Integration (Testcontainers PostgreSQL — full RAG pipeline TriageRedFlagSafetyFilter.check())
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-110 (§3.2.2.12)` | CRUD hành vi cơ bản, role System Admin |
| `CLAUDE.md BR-SAFETY` | Fail-safe floor không được vô hiệu hoá (RFR-TC-011/012) |
| `CB-MOD-IMP-005 §3 ADR-001` | Floor-first check, fail-closed on DB error |
| `CB-MOD-IMP-005 §3 ADR-003` | Severity/action runtime scope boundary (chỉ RED+ESCALATE) |
| `CB-MOD-IMP-005 §3 BR-SAFETY-RFR-003` | System-default rule không thể DELETE/deactivate |
| `V20260702002000__create_red_flag_rules.sql` (TDS §5.2) | Unique constraint trên `keyword`, CHECK constraint severity/action enum |
| `CB-MOD-IMP-005 §10` | Error code mapping (`MOD-024..027`) |
| `CB-MOD-IMP-005 §16` | Authorization matrix — chỉ SYSTEM_ADMIN |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Tạo rule hợp lệ → 201, persisted, audit logged | `RedFlagRuleServiceImpl.createRule()` | `RFR-TC-001` |
| TC-COND-002 | Tạo rule trùng keyword (case-insensitive) → 409 | `RedFlagRuleServiceImpl.createRule()` | `RFR-TC-002` |
| TC-COND-003 | Tạo rule thiếu/sai field → 400 | `RedFlagRuleController` bean validation | `RFR-TC-003` |
| TC-COND-004 | List rule có filter severity/isActive → 200, đúng page | `RedFlagRuleServiceImpl.listRules()` | `RFR-TC-004` |
| TC-COND-005 | Update rule (non-default) hợp lệ → 200, persisted | `RedFlagRuleServiceImpl.updateRule()` | `RFR-TC-005` |
| TC-COND-006 | Update rule không tồn tại → 404 | `RedFlagRuleServiceImpl.updateRule()` | `RFR-TC-006` |
| TC-COND-007 | Update isActive=false trên rule isSystemDefault=true → 409 | `RedFlagRuleServiceImpl.updateRule()` | `RFR-TC-007` |
| TC-COND-008 | Delete rule (non-default) hợp lệ → 204, removed | `RedFlagRuleServiceImpl.deleteRule()` | `RFR-TC-008` |
| TC-COND-009 | Delete rule isSystemDefault=true → 409, không xoá | `RedFlagRuleServiceImpl.deleteRule()` | `RFR-TC-009` |
| TC-COND-010 | Delete rule không tồn tại → 404 | `RedFlagRuleServiceImpl.deleteRule()` | `RFR-TC-010` |
| TC-COND-011 | `isRedFlag()` match floor keyword khi DB rỗng | `TriageRedFlagPolicy.isRedFlag()` | `RFR-TC-011` **CRITICAL** |
| TC-COND-012 | `isRedFlag()` match floor keyword khi DB throw exception | `TriageRedFlagPolicy.isRedFlag()` | `RFR-TC-012` **CRITICAL** |
| TC-COND-013 | `isRedFlag()` match DB-only admin-added RED+active keyword | `TriageRedFlagPolicy.isRedFlag()` | `RFR-TC-013` |
| TC-COND-014 | `isRedFlag()` KHÔNG match rule severity=GREEN/YELLOW | `TriageRedFlagPolicy.isRedFlag()` | `RFR-TC-014` |
| TC-COND-015 | `isRedFlag()` KHÔNG match rule đã `isActive=false` | `TriageRedFlagPolicy.isRedFlag()` | `RFR-TC-015` |
| TC-COND-016 | End-to-end: `TriageRedFlagSafetyFilter.check()` trả `redFlag=true` cho DB-added keyword | `TriageRedFlagSafetyFilter` + Testcontainers | `RFR-TC-INT-001` |
| TC-COND-017 | Non-SYSTEM_ADMIN bị từ chối mọi endpoint | `@PreAuthorize` | `RFR-TC-SEC-001` |
| TC-COND-018 | Không có JWT → 401 | Spring Security filter | `RFR-TC-SEC-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `severity` (GREEN/YELLOW/RED), `action` (BLOCK/WARN/ESCALATE) | 3×3 combinations reduced to representative classes: RED+ESCALATE (runtime-active) vs others (persistence-only) |
| Boundary Value Analysis | `keyword` length (blank, max-length VARCHAR(255), unicode Vietnamese diacritics) | Validation boundary per `chk`/`VARCHAR(255)` constraint |
| State Transition Testing | `isActive` ACTIVE↔INACTIVE, `isSystemDefault` immutability | TDS §6.4 state machine — verify forbidden transitions rejected |
| Error Guessing | DB exception injection (simulate `RedFlagRuleRepository` throwing) | Direct test of BR-SAFETY fail-closed behavior — the single highest-risk path in this module |
| Decision Table | `isSystemDefault` × {DELETE, PATCH isActive=false, PATCH other field} | Confirms guard only blocks the 2 unsafe transitions, not legitimate metadata edits |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `RedFlagRule{keyword:"chảy máu nhiều", severity:RED, action:ESCALATE, isActive:true, isSystemDefault:true}` | Mirrors real seed row — system-default guard tests |
| `FX-002` | DB seed | `RedFlagRule{keyword:"đau đầu nhẹ", severity:GREEN, action:WARN, isActive:true, isSystemDefault:false}` | Out-of-scope severity test (ADR-003 boundary) |
| `FX-003` | DB seed | `RedFlagRule{keyword:"từ khoá thử nghiệm khẩn cấp", severity:RED, action:ESCALATE, isActive:true, isSystemDefault:false}` | Admin-added RED keyword — verifies additive DB integration |
| `FX-004` | DB seed | `RedFlagRule{keyword:"đã tắt", severity:RED, action:ESCALATE, isActive:false, isSystemDefault:false}` | Deactivated rule — must NOT trigger `isRedFlag()` |
| `FX-005` | JWT | `{ sub: '<uuid>', role: 'SYSTEM_ADMIN' }` | Auth context, happy path |
| `FX-006` | JWT | `{ sub: '<uuid>', role: 'MODERATOR' }` | Auth context, RBAC rejection test |
| `FX-007` | Mock | `RedFlagRuleRepository.findBySeverityAndIsActiveTrue(RED)` throws `DataAccessResourceFailureException` | Simulates DB outage for fail-closed test |
| `FX-008` | Synthetic query string | `"tôi bị khó thở dữ dội"` | Contains floor keyword `"khó thở"` — used in RFR-TC-011/012/016 |

---

## 4. Test Case Specification

> **TC ID format:** `RFR-TC-[NNN]` (Red Flag Rule)
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// RedFlagRuleTestFactory.java
class RedFlagRuleTestFactory {

    // Baseline system-default rule — đồng bộ với FX-001 (§3 TDS-05)
    static RedFlagRule makeSystemDefaultRule() {
        RedFlagRule rule = new RedFlagRule();
        rule.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        rule.setKeyword("chảy máu nhiều");
        rule.setSeverity(RedFlagSeverity.RED);
        rule.setAction(RedFlagAction.ESCALATE);
        rule.setActive(true);
        rule.setSystemDefault(true);
        rule.setCreatedBy(null);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }

    // Baseline admin-created (non-default) rule
    static RedFlagRule makeAdminRule() {
        RedFlagRule rule = new RedFlagRule();
        rule.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        rule.setKeyword("từ khoá thử nghiệm khẩn cấp");
        rule.setSeverity(RedFlagSeverity.RED);
        rule.setAction(RedFlagAction.ESCALATE);
        rule.setActive(true);
        rule.setSystemDefault(false);
        rule.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }

    // Overload để override specific fields — KHÔNG share mutable instance giữa test
    static RedFlagRule makeAdminRule(Consumer<RedFlagRule> overrides) {
        RedFlagRule rule = makeAdminRule();
        overrides.accept(rule);
        return rule;
    }

    static CreateRedFlagRuleRequest makeCreateRequest() {
        return new CreateRedFlagRuleRequest("ra máu nhiều khi mang thai", RedFlagSeverity.RED, RedFlagAction.ESCALATE);
    }

    static CreateRedFlagRuleRequest makeCreateRequest(Consumer<CreateRedFlagRuleRequest> ignored) {
        // records are immutable — build directly per test as needed; helper kept for symmetry
        return makeCreateRequest();
    }

    static UpdateRedFlagRuleRequest makeDeactivateRequest() {
        return new UpdateRedFlagRuleRequest(null, null, null, false);
    }

    static final UUID SYSTEM_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
}
```

---

### RFR-TC-001 — createRule() persists a valid rule and logs audit

**Severity:** `HIGH`
**Feature Under Test:** `RedFlagRuleServiceImpl.createRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-MOD-IMP-005 §8.1` (Service contract), `§9.2` (201 response shape)

**Preconditions:**
- `RedFlagRuleRepository.existsByKeywordIgnoreCase(...)` mocked → `false`
- `RedFlagRuleRepository.save(...)` mocked → returns saved entity with generated `id`

**Test Steps:**
1. Arrange: `CreateRedFlagRuleRequest request = RedFlagRuleTestFactory.makeCreateRequest()`
2. Act: `service.createRule(request, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID)`
3. Assert: response fields match request; `isActive=true`, `isSystemDefault=false`; `repository.save()` called once; `auditService.log(RED_FLAG_RULE_CREATED, ...)` called once

**Expected Result (PASS):**
- Returns `RedFlagRuleResponse` with `isActive=true`, `isSystemDefault=false`
- `AuditService.log(AuditAction.RED_FLAG_RULE_CREATED, actorUserId, ruleId, details)` invoked exactly once

**Expected Result (FAIL):**
- Exception thrown, or `isSystemDefault=true` returned for an admin-created rule (would violate ADR-001 invariant), or audit not called

**Current Status:** 🟢 Passing
**Implementation Note:** Per TDS §11 Chặng 2 step 9 — implement BR-SAFETY-RFR-003 guard FIRST in the service even though this happy-path test doesn't exercise it directly.

---

### RFR-TC-002 — createRule() rejects duplicate keyword (case-insensitive)

**Severity:** `HIGH`
**Feature Under Test:** `RedFlagRuleServiceImpl.createRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-MOD-IMP-005 §10 MOD-025`, `V20260702002000__create_red_flag_rules.sql` `uq_red_flag_rules_keyword`

**Preconditions:**
- `RedFlagRuleRepository.existsByKeywordIgnoreCase("CHẢY MÁU NHIỀU")` mocked → `true`

**Test Steps:**
1. Arrange: request with `keyword = "CHẢY MÁU NHIỀU"` (different case from existing `"chảy máu nhiều"`)
2. Act: `service.createRule(request, actorId)`
3. Assert: throws `RedFlagRuleException` with `code = "MOD-025"`, `httpStatus = 409`

**Expected Result (PASS):**
- `RedFlagRuleException` thrown, `getCode()` returns `"MOD-025"`; `repository.save()` NEVER called

**Expected Result (FAIL):**
- No exception, or rule silently saved as duplicate, or wrong error code

**Current Status:** 🟢 Passing

---

### RFR-TC-003 — Controller rejects blank keyword with 400 (MOD-024)

**Severity:** `MEDIUM`
**Feature Under Test:** `RedFlagRuleController.create()` (bean validation)
**Test File:** `src/test/java/com/carebridge/backend/triage/controller/RedFlagRuleControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-MOD-IMP-005 §8.3 CreateRedFlagRuleRequest (@NotBlank)`, `§10 MOD-024`

**Preconditions:**
- `@WebMvcTest(RedFlagRuleController.class)`, `MockMvc` configured, `SYSTEM_ADMIN` JWT mock

**Test Steps:**
1. Arrange: JSON body `{"keyword": "", "severity": "RED", "action": "ESCALATE"}`
2. Act: `POST /api/v1/admin/red-flag-rules`
3. Assert: status `400`, body `error.code = "MOD-024"`, `service.createRule()` never invoked

**Expected Result (PASS):** `400` with `MOD-024`, no service call
**Expected Result (FAIL):** `201` or unrelated error code

**Current Status:** 🟢 Passing

---

### RFR-TC-004 — listRules() applies severity/isActive filter and pagination

**Severity:** `MEDIUM`
**Feature Under Test:** `RedFlagRuleServiceImpl.listRules()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-MOD-IMP-005 §8.3 RedFlagRuleFilter`, `§9.2 GET response`

**Preconditions:**
- `RedFlagRuleRepository.findBySeverityAndIsActive(RED, true, pageable)` mocked → `Page` containing `FX-001` and `FX-003`

**Test Steps:**
1. Arrange: `filter = new RedFlagRuleFilter(RED, true, 0, 20)`
2. Act: `service.listRules(filter)`
3. Assert: `totalElements = 2`, every item has `severity = RED` and `isActive = true`

**Expected Result (PASS):** Page content matches mocked repository result exactly
**Expected Result (FAIL):** Unfiltered results returned, or wrong repository method invoked

**Current Status:** 🟢 Passing

---

### RFR-TC-005 — updateRule() updates a non-default rule and logs audit

**Severity:** `HIGH`
**Feature Under Test:** `RedFlagRuleServiceImpl.updateRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-MOD-IMP-005 §8.1`

**Preconditions:**
- `RedFlagRuleRepository.findById(ruleId)` mocked → `Optional.of(RedFlagRuleTestFactory.makeAdminRule())`

**Test Steps:**
1. Arrange: `UpdateRedFlagRuleRequest request = new UpdateRedFlagRuleRequest("từ khoá đã sửa", null, null, null)`
2. Act: `service.updateRule(ruleId, request, actorId)`
3. Assert: returned `keyword = "từ khoá đã sửa"`; `repository.save()` called with `updatedBy = actorId`; `auditService.log(RED_FLAG_RULE_UPDATED, ...)` called once

**Expected Result (PASS):** keyword updated, audit logged
**Expected Result (FAIL):** No persistence change, or audit skipped

**Current Status:** 🟢 Passing

---

### RFR-TC-006 — updateRule() returns 404 (MOD-026) for unknown ruleId

**Severity:** `MEDIUM`
**Feature Under Test:** `RedFlagRuleServiceImpl.updateRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-MOD-IMP-005 §10 MOD-026`

**Preconditions:**
- `RedFlagRuleRepository.findById(unknownId)` mocked → `Optional.empty()`

**Test Steps:**
1. Act: `service.updateRule(unknownId, RedFlagRuleTestFactory.makeDeactivateRequest(), actorId)`
2. Assert: throws `RedFlagRuleException` `code = "MOD-026"`, `httpStatus = 404`

**Expected Result (PASS):** `MOD-026` thrown
**Expected Result (FAIL):** `NullPointerException` or silent no-op

**Current Status:** 🟢 Passing

---

### RFR-TC-007 — updateRule() rejects isActive=false on a system-default rule (MOD-027)

**Severity:** `CRITICAL`
**CWE:** `CWE-840 — Business Logic Errors`
**Legal:** `BR-SAFETY (CLAUDE.md), BR-SAFETY-RFR-003 (CB-MOD-IMP-005 ADR-001)`
**Feature Under Test:** `RedFlagRuleServiceImpl.updateRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-MOD-IMP-005 §3 ADR-001, §10 MOD-027`

**Preconditions:**
- `RedFlagRuleRepository.findById(ruleId)` mocked → `Optional.of(RedFlagRuleTestFactory.makeSystemDefaultRule())` (`isSystemDefault=true`, `isActive=true`)

**Test Steps:**
1. Arrange: `request = RedFlagRuleTestFactory.makeDeactivateRequest()` (`isActive=false`, other fields null)
2. Act (performed by a **valid SYSTEM_ADMIN actor** — this is not an authz test): `service.updateRule(ruleId, request, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID)`
3. Assert: throws `RedFlagRuleException` `code = "MOD-027"`, `httpStatus = 409`; `repository.save()` NEVER invoked

**Expected Result (PASS — hành vi đúng):**
- Exception thrown with `MOD-027`; entity in mock remains `isActive=true` (no save call)

**Expected Result (FAIL — dấu hiệu lỗi):**
- Rule is deactivated (save() invoked with `isActive=false`) — **this would be a direct BR-SAFETY violation** if this guard is ever skipped

**Current Status:** 🟢 Passing
**Implementation Note:** This is one of the highest-priority tests in the entire batch — it directly encodes the fail-safe invariant from TDS ADR-001/BR-SAFETY-RFR-003.

---

### RFR-TC-008 — deleteRule() removes a non-default rule and logs audit

**Severity:** `HIGH`
**Feature Under Test:** `RedFlagRuleServiceImpl.deleteRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-MOD-IMP-005 §8.1`

**Preconditions:**
- `RedFlagRuleRepository.findById(ruleId)` mocked → `Optional.of(RedFlagRuleTestFactory.makeAdminRule())` (`isSystemDefault=false`)

**Test Steps:**
1. Act: `service.deleteRule(ruleId, actorId)`
2. Assert: `repository.delete(rule)` called once; `auditService.log(RED_FLAG_RULE_DELETED, ...)` called once

**Expected Result (PASS):** delete + audit both invoked exactly once
**Expected Result (FAIL):** delete skipped or audit skipped

**Current Status:** 🟢 Passing

---

### RFR-TC-009 — deleteRule() rejects deletion of a system-default rule (MOD-027)

**Severity:** `CRITICAL`
**CWE:** `CWE-840 — Business Logic Errors`
**Legal:** `BR-SAFETY (CLAUDE.md), BR-SAFETY-RFR-003`
**Feature Under Test:** `RedFlagRuleServiceImpl.deleteRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-MOD-IMP-005 §3 ADR-001, §10 MOD-027`

**Preconditions:**
- `RedFlagRuleRepository.findById(ruleId)` mocked → `Optional.of(RedFlagRuleTestFactory.makeSystemDefaultRule())`

**Test Steps:**
1. Act: `service.deleteRule(ruleId, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID)`
2. Assert: throws `RedFlagRuleException` `code = "MOD-027"`; `repository.delete(...)` NEVER invoked

**Expected Result (PASS):** exception thrown, `delete()` never called
**Expected Result (FAIL):** rule deleted — direct loss of a floor-mirror row, BR-SAFETY violation risk

**Current Status:** 🟢 Passing

---

### RFR-TC-010 — deleteRule() returns 404 (MOD-026) for unknown ruleId

**Severity:** `MEDIUM`
**Feature Under Test:** `RedFlagRuleServiceImpl.deleteRule()`
**Test File:** `src/test/java/com/carebridge/backend/triage/service/RedFlagRuleServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-MOD-IMP-005 §10 MOD-026`

**Preconditions:**
- `RedFlagRuleRepository.findById(unknownId)` mocked → `Optional.empty()`

**Test Steps:**
1. Act: `service.deleteRule(unknownId, actorId)`
2. Assert: throws `RedFlagRuleException` `code = "MOD-026"`, `httpStatus = 404`

**Expected Result (PASS):** `MOD-026` thrown
**Expected Result (FAIL):** silent no-op or `NullPointerException`

**Current Status:** 🟢 Passing

---

### CRITICAL — BR-SAFETY FAIL-SAFE TEST CASES (TriageRedFlagPolicy)

> These tests are the direct, executable encoding of ADR-001 (CB-MOD-IMP-005 §3) and the project-wide
> BR-SAFETY rule in CLAUDE.md. They must pass before this feature can be merged (see §6 Exit Criteria).

---

### RFR-TC-011 — isRedFlag() still detects floor keyword when red_flag_rules table is empty

**Severity:** `CRITICAL`
**CWE:** `CWE-636 — Not Failing Securely ('Failing Open')`
**Legal:** `BR-SAFETY (CLAUDE.md)`
**Feature Under Test:** `TriageRedFlagPolicy.isRedFlag(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/policy/TriageRedFlagPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-MOD-IMP-005 §3 ADR-001 ("floor đánh giá trước và độc lập với DB")`, original `TriageRedFlagPolicy.RED_FLAG_KEYWORDS` list (pre-existing code, now `FLOOR_KEYWORDS`)

**Preconditions:**
- `RedFlagRuleRepository.findBySeverityAndIsActiveTrue(RED)` mocked → `List.of()` (empty — simulates a freshly-migrated/empty DB)

**Test Steps:**
1. Arrange: `policy = new TriageRedFlagPolicy(mockRepository)`; query = `"tôi bị khó thở dữ dội"` (FX-008, contains floor keyword `"khó thở"`)
2. Act: `boolean result = policy.isRedFlag(query)`
3. Assert: `result == true`

**Expected Result (PASS — hành vi đúng):**
- `true` — floor keyword detected regardless of empty DB result

**Expected Result (FAIL — dấu hiệu lỗi):**
- `false` — would mean the implementation refactored `isRedFlag()` to depend solely on the DB query, silently dropping the floor. **This is the single most safety-critical regression this module could introduce.**

**Current Status:** 🟢 Passing
**Implementation Note:** This test alone is sufficient to catch AP-AI-006 (Fail-Open Safety Bug, TDS §17.4) if an AI-generated implementation collapses the two-step check into a DB-only query.

---

### RFR-TC-012 — isRedFlag() still detects floor keyword when repository throws

**Severity:** `CRITICAL`
**CWE:** `CWE-636 — Not Failing Securely ('Failing Open')`
**Legal:** `BR-SAFETY (CLAUDE.md), BR-SAFETY-RFR-002`
**Feature Under Test:** `TriageRedFlagPolicy.isRedFlag(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/policy/TriageRedFlagPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-MOD-IMP-005 §3 ADR-001, §6.3 sequence diagram (DB error branch)`

**Preconditions:**
- `RedFlagRuleRepository.findBySeverityAndIsActiveTrue(RED)` mocked (FX-007) → throws `DataAccessResourceFailureException("simulated DB outage")`

**Test Steps:**
1. Arrange: `policy = new TriageRedFlagPolicy(mockRepository)`; query = `"tôi bị khó thở dữ dội"` (FX-008)
2. Act: `boolean result = policy.isRedFlag(query)`
3. Assert: `result == true`, AND no exception propagates out of `isRedFlag()` (call does not throw)

**Expected Result (PASS — hành vi đúng):**
- Returns `true`; the thrown `DataAccessResourceFailureException` is caught internally and never reaches the caller (`TriageRedFlagSafetyFilter`)

**Expected Result (FAIL — dấu hiệu lỗi):**
- `DataAccessResourceFailureException` propagates out (would crash the RAG/Triage request entirely on transient DB hiccups), OR `isRedFlag()` returns `false` despite the floor match (fail-open, BR-SAFETY violation)

**Current Status:** 🟢 Passing
**Implementation Note:** Floor check (step 1) MUST run before the DB call (step 2) so that this assertion is satisfiable even though the DB call always throws in this test.

---

### RFR-TC-013 — isRedFlag() detects a DB-only admin-added RED+active keyword (additive integration)

**Severity:** `HIGH`
**Feature Under Test:** `TriageRedFlagPolicy.isRedFlag(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/policy/TriageRedFlagPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-MOD-IMP-005 §3 ADR-001 ("DB-backed rule chỉ mở rộng thêm từ khoá")`

**Preconditions:**
- Query text contains `"từ khoá thử nghiệm khẩn cấp"` — NOT in `FLOOR_KEYWORDS` (verified: not among the 19 hardcoded phrases)
- `RedFlagRuleRepository.findBySeverityAndIsActiveTrue(RED)` mocked → `List.of(RedFlagRuleTestFactory.makeAdminRule())` (FX-003, `keyword="từ khoá thử nghiệm khẩn cấp"`, `isActive=true`)

**Test Steps:**
1. Act: `policy.isRedFlag("Tôi đang có từ khoá thử nghiệm khẩn cấp, phải làm sao?")`
2. Assert: returns `true`

**Expected Result (PASS):** `true` — confirms the DB-additive path actually works, not just the floor path
**Expected Result (FAIL):** `false` — DB integration not wired, or DB step never reached

**Current Status:** 🟢 Passing

---

### RFR-TC-014 — isRedFlag() does NOT trigger for a GREEN/YELLOW-severity rule (ADR-003 boundary)

**Severity:** `HIGH`
**Feature Under Test:** `TriageRedFlagPolicy.isRedFlag(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/policy/TriageRedFlagPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-MOD-IMP-005 §3 ADR-003 ("chỉ RED+ESCALATE+active mới ảnh hưởng isRedFlag()")`, Logic Issue L1 (§2)

**Preconditions:**
- Query contains `"đau đầu nhẹ"` (FX-002, `severity=GREEN`) — NOT in `FLOOR_KEYWORDS`
- `RedFlagRuleRepository.findBySeverityAndIsActiveTrue(RED)` mocked → `List.of()` (the GREEN rule is, by construction, never returned by this RED-only query)

**Test Steps:**
1. Act: `policy.isRedFlag("tôi bị đau đầu nhẹ hôm nay")`
2. Assert: returns `false`

**Expected Result (PASS):** `false` — confirms `isRedFlag()` queries only `severity=RED`, per the repository method signature `findBySeverityAndIsActiveTrue(RED)` (TDS §8.2) — GREEN/YELLOW rows are never even fetched
**Expected Result (FAIL):** `true` — would indicate an out-of-scope runtime behavior was implemented for GREEN, contradicting ADR-003 and risking an undocumented UX (Anti-Pattern AP-AI-005)

**Current Status:** 🟢 Passing

---

### RFR-TC-015 — isRedFlag() does NOT trigger for a deactivated (isActive=false) DB rule

**Severity:** `HIGH`
**Feature Under Test:** `TriageRedFlagPolicy.isRedFlag(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/policy/TriageRedFlagPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CB-MOD-IMP-005 §8.2 RedFlagRuleRepository.findBySeverityAndIsActiveTrue` (method name encodes the `isActive=true` filter at the query level)

**Preconditions:**
- Query contains `"đã tắt"` (FX-004, `severity=RED`, `isActive=false`) — NOT in `FLOOR_KEYWORDS`
- `RedFlagRuleRepository.findBySeverityAndIsActiveTrue(RED)` mocked → `List.of()` (deactivated rows are excluded by the repository query itself, per its name)

**Test Steps:**
1. Act: `policy.isRedFlag("tôi đã tắt thông báo này")`
2. Assert: returns `false`

**Expected Result (PASS):** `false`
**Expected Result (FAIL):** `true` — would mean a deactivated rule still affects runtime behavior, defeating the purpose of the `isActive` toggle

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### RFR-TC-SEC-001 — Non-SYSTEM_ADMIN role rejected on every endpoint (403)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC-001 (CB-MOD-IMP-005 §2)`
**Feature Under Test:** `RedFlagRuleController` (`@PreAuthorize("hasRole('SYSTEM_ADMIN')")`)
**Test File:** `src/test/java/com/carebridge/backend/triage/controller/RedFlagRuleControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:**
- JWT with `role=MODERATOR` (FX-006) — chosen because UC-99/100/101/102 grant `MODERATOR` access to sibling moderation endpoints, making this the most realistic "almost-but-not-quite" privilege-escalation attempt to test

**Test Steps (Attack Simulation):**
1. Obtain valid `MODERATOR` JWT
2. Call each of `POST`, `GET`, `PATCH`, `DELETE /api/v1/admin/red-flag-rules[...]` with that JWT
3. Inspect response status and body

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden` for all 4 endpoints, body `error.code = "ACCESS_DENIED"` (per TDS §10 — real path, not a custom `MOD-xxx` code), `RedFlagRuleService` never invoked

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Any endpoint returns `2xx` for a `MODERATOR` actor

**Current Status:** 🟢 Passing

---

### RFR-TC-SEC-002 — Missing JWT rejected with 401

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `RedFlagRuleController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/triage/controller/RedFlagRuleControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:**
- No `Authorization` header

**Test Steps (Attack Simulation):**
1. Call `GET /api/v1/admin/red-flag-rules` without `Authorization` header
2. Inspect response

**Expected Result (PASS = hệ thống an toàn):**
- `401 Unauthorized`, empty response body — no JSON error envelope (verified real path: `SecurityConfig.java` → `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`; `IAM-001` does NOT exist anywhere in production Java source — corrected per TDS §9.2/§10, same finding already documented in sibling UC-100 TDS)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- `200 OK` or any data returned

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### RFR-TC-INT-001 — Full RAG safety pipeline detects a DB-added RED keyword end-to-end

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: RedFlagRuleController.create() → DB → TriageRedFlagSafetyFilter.check()`
**Test File:** `src/test/java/com/carebridge/backend/triage/RedFlagRuleIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-016`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration `V20260702002000__create_red_flag_rules.sql` applied automatically on Spring context start (19 seed rows present)
- Seed: one `SYSTEM_ADMIN` JWT for the create call

**Test Steps:**
1. `POST /api/v1/admin/red-flag-rules` with `{keyword: "tự làm đau bản thân", severity: RED, action: ESCALATE}` using `SYSTEM_ADMIN` JWT → expect `201`
2. Call `TriageRedFlagSafetyFilter.check("tôi có suy nghĩ tự làm đau bản thân")` directly (Spring-managed bean, real `RedFlagRuleRepository` against the Testcontainers DB)
3. Assert: `RagSafetyResult.isRedFlag() == true` and `getEmergencyGuidance()` is non-blank

**Expected Result (PASS):**
- Newly created DB rule is visible to `TriageRedFlagPolicy` on the very next call (read-through, no cache — ADR-004) without any restart/cache-bust step

**Expected Result (FAIL):**
- `isRedFlag()` returns `false` despite the rule existing and being active — would indicate the read-through wiring is broken (e.g. stale connection pool, or a cache was added without an ADR, violating C6 TDS §17.1)

**DB Assertion:**
```java
RedFlagRule persisted = redFlagRuleRepository.findById(createdRuleId).orElseThrow();
assertThat(persisted.getSeverity()).isEqualTo(RedFlagSeverity.RED);
assertThat(persisted.isActive()).isTrue();
assertThat(persisted.isSystemDefault()).isFalse();
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `RFR-TC-001` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-002` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-003` | `RedFlagRuleControllerTest.java` | `[x]` (see §5.1 note — passed at Red Gate, bean validation) | 2026-07-02 (uncommitted) | Asserts status only, not `MOD-024` body code — see §5.1 |
| `RFR-TC-004` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-005` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-006` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-007` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | **CRITICAL — BR-SAFETY** |
| `RFR-TC-008` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-009` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | **CRITICAL — BR-SAFETY** |
| `RFR-TC-010` | `RedFlagRuleServiceImplTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-011` | `TriageRedFlagPolicyTest.java` | `[x]` | 2026-07-02 (uncommitted) | **CRITICAL — BR-SAFETY fail-safe** |
| `RFR-TC-012` | `TriageRedFlagPolicyTest.java` | `[x]` | 2026-07-02 (uncommitted) | **CRITICAL — BR-SAFETY fail-safe** |
| `RFR-TC-013` | `TriageRedFlagPolicyTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-014` | `TriageRedFlagPolicyTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-015` | `TriageRedFlagPolicyTest.java` | `[x]` | 2026-07-02 (uncommitted) | |
| `RFR-TC-SEC-001` | `RedFlagRuleControllerSecurityTest.java` | `[x]` (see §5.1 note — passed at Red Gate, `@PreAuthorize`) | 2026-07-02 (uncommitted) | |
| `RFR-TC-SEC-002` | `RedFlagRuleControllerSecurityTest.java` | `[x]` (Test-Spec pre-acknowledged — filter chain) | 2026-07-02 (uncommitted) | |
| `RFR-TC-INT-001` | `RedFlagRuleIntegrationTest.java` | `[x]` | 2026-07-02 (uncommitted) | Hosted as `@SpringBootTest`+H2 (real beans), not Testcontainers — none exist project-wide |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.
> Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class RedFlagRuleServiceImpl implements RedFlagRuleService {

    @Override
    public RedFlagRuleResponse createRule(CreateRedFlagRuleRequest request, UUID actorUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public RedFlagRulePageResponse listRules(RedFlagRuleFilter filter) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public RedFlagRuleResponse updateRule(UUID ruleId, UpdateRedFlagRuleRequest request, UUID actorUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void deleteRule(UUID ruleId, UUID actorUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// Red Phase stub for the modified existing class — MUST also throw until implemented,
// so RFR-TC-011/012/013/014/015 fail correctly during Red Phase
@Component
public class TriageRedFlagPolicy {

    public boolean isRedFlag(String query) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    public String getEmergencyGuidance() {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `RFR-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☑ PASS | Not AP-AI-002: `@Valid`/`@NotBlank` bean validation rejects the blank keyword during argument binding, before the (stubbed) service method is ever invoked — the subject under test (Spring's validation layer) was never stubbed and already worked correctly. Verified `service.createRule()` was not invoked. |
| `RFR-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `RFR-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☑ PASS | Not AP-AI-002: `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` rejects the MODERATOR JWT at the Spring Security method-security layer, before the (stubbed) service is ever reached — the subject under test (RBAC enforcement) was never stubbed and already worked correctly. Verified `redFlagRuleService` was never invoked for any of the 4 endpoints. |
| `RFR-TC-SEC-002` | N/A — auth filter runs before controller/service | 🔴 FAIL (still 401, but for a different reason — confirm test setup is meaningful, not vacuous) | ☐ FAIL ☑ PASS | Not AP-AI-002 — pre-acknowledged by this table's own "Stub Result" column: `HttpStatusEntryPoint` rejects the missing-JWT request before the DispatcherServlet routes to the controller, so the stub is never reached. Confirmed meaningful (not vacuous) — same real path independently verified for RFR-TC-003/SEC-001 above. |
| `RFR-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `uncommitted` *(RED gate captured 2026-07-02; work not yet committed to git)*
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** (T1→T2) → tiếp tục implement — 15/18 stubs FAIL as required; the 3 exceptions above are documented, verified-legitimate (not AP-AI-002), and consistent with this table's own pre-acknowledgment for RFR-TC-SEC-002.
- Log file: `mvn test -Dtest=RedFlagRuleServiceImplTest,RedFlagRuleControllerTest,TriageRedFlagPolicyTest,RedFlagRuleControllerSecurityTest` (2026-07-02, local run — 6 failures + 8 errors = 14 FAIL, 3 PASS as documented)

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] TDS `CB-MOD-IMP-005` đã được review và approve (Status đổi từ `Draft` sang `Approved`) — user xác nhận approve 2026-07-02
- [x] Logic Issues (Section 2) đã được confirm với Principal Architect — đặc biệt L1 (severity scope) và L2 (system-default guard) — không có deviation khi implement
- [x] Flyway migration `V20260702002000__create_red_flag_rules.sql` đã được approved (chưa cần chạy trên staging tại Entry — chạy ở Implementation Step 11.3 Chặng 1)
- [x] Test fixtures (Section 3 TDS-05, FX-001..008) đã được chuẩn bị (`RedFlagRuleTestFactory.java`)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (không có skip), bao gồm bắt buộc `RFR-TC-011` và `RFR-TC-012` — 18/18 PASS
- [x] `./mvnw verify` — `RFR-TC-INT-001` xanh — **deviation from spec, documented:** hosted as `@SpringBootTest` + H2 (real Spring-managed beans end-to-end), NOT Testcontainers — this codebase has no Testcontainers/real-Postgres harness anywhere (verified project-wide, same finding as `ModerateContentIntegrationTest`); adding one would violate CLAUDE.md's "no new dependencies without approval"
- [ ] Test coverage ≥ 80% lines cho `RedFlagRuleServiceImpl` và modified `TriageRedFlagPolicy` — **not measured**, no JaCoCo/coverage tool configured in this project's `pom.xml` — left unchecked rather than falsely claimed
- [x] Không có business logic trong `RedFlagRuleController` (chỉ validation + mapping)
- [x] Không có PII/secret xuất hiện plaintext trong logs (module không có PII nhưng vẫn kiểm tra theo policy chung)
- [x] **BR-SAFETY gate (đặc thù feature này):** `RFR-TC-011`, `RFR-TC-012`, `RFR-TC-007`, `RFR-TC-009` PASS — đây là điều kiện chặn merge cứng, không thể waive — tất cả 4 PASS, verified individually

**Exit Criteria bổ sung — CASE 2.0:**

- [x] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement — 15/18 FAIL as required; 3 legitimate exceptions documented in §5.1
- [x] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output — VERIFIED 2026-07-02, no output
  ```
- [x] **Props Isolation** — không có shared mutable state giữa tests: verified — `RedFlagRuleTestFactory` methods return fresh instances per call, no static mutable fields
- [x] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR) — đã ghi trong mỗi TC §4

### Suspension Criteria (Điều kiện tạm dừng)

- TDS `CB-MOD-IMP-005` chưa Approved
- Migration `V20260702002000` xung đột timestamp với một migration khác được merge song song (kiểm tra `ls db/migration | sort -V | tail -3` trước khi bắt đầu implement — đã từng xảy ra 1 lần với đề xuất `V20260701000001` ban đầu, xem CHANGELOG)
- Phát hiện lỗi kiến trúc mới liên quan đến ADR-002 (package placement) cần Principal Architect review

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS red_flag_rules CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260702002000';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/triage/
git checkout -- src/main/resources/db/migration/V20260702002000__create_red_flag_rules.sql
git checkout -- src/test/java/com/carebridge/backend/triage/

# Gap vẫn OPEN → UC-110 quay về trạng thái "hardcoded floor only", hành vi
# TriageRedFlagSafetyFilter/RagSafetyResult không thay đổi so với trước khi implement.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ checked — none found | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☑ checked — 3 exceptions found and verified legitimate, see §5.1 root-cause column (not AP-AI-002) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume caching architecture không có trong ADR-004 | ☑ checked — grep for `@Cacheable`/`CacheManager` in `triage` package: no output | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ checked — `RedFlagRuleController` contains only `@PreAuthorize`, `@Valid`, and delegation | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (e.g. wiring `RagSafetyResult` với GREEN/YELLOW field không có trong §8) | ☑ checked — no GREEN/YELLOW runtime wiring found in `RagSafetyResult`/`TriageRedFlagSafetyFilter` | G-3 |
| AP-AI-006 | Fail-Open Safety Bug | `RFR-TC-011`/`RFR-TC-012` PASS for the wrong reason (e.g. mock accidentally returns the floor match instead of testing the real floor-check code path) | ☑ checked — floor check runs unconditionally before any repository call (verified: repository stub is provably unreached for floor-matching queries, confirmed via Mockito strict-stubbing `UnnecessaryStubbingException` during development, then resolved with explicit `lenient()`) | G-2 ★★ — **đặc thù module này, mức ưu tiên cao nhất** |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào → Test-Spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected — full review completed 2026-07-02 after GREEN gate)_ | | | | |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Implemented — 2026-07-02 (18/18 PASS).*
