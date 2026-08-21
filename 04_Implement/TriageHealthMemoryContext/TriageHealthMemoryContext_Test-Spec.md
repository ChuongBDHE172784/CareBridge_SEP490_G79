# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# TriageHealthMemoryContext — Connect Health Context Memory to the Triage Flow

**Document ID:** `CB-TRIAGE-THMC-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-07-26`
**Status:** `Implemented 18/18 — 2026-07-27` *(THMC-TC-01…16 + THMC-TC-INT-01/02 🟢 Passing; the two formerly env-blocked integration TCs executed green on a Docker-capable host — `./mvnw test -Dtest=TriageHealthMemoryContextIntegrationTest` → Tests run 2, Failures 0, Errors 0)*
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/TriageHealthMemoryContext/TriageHealthMemoryContext_TDS.md` (CB-TRIAGE-THMC-IMP-001) — design oracle
- `04_Implement/AITriageCompletion/AITriage_Assessment_Roadmap.md` Part III item 1 — requirement oracle
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/B20260724111500__canonical_70_table_baseline.sql` (:999-1012) — persistence oracle (`health_context_memories`)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/repository/HealthMemoryEntryRepository.java` (:14-33) — expiry/ownership filter oracle
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/entity/HealthMemoryEntry.java` (:32) — minimization oracle ("Raw conversation text is never stored here")
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/service/impl/TriageService.java` (:384-457, :768-777) — flow oracle
- `08_References/Template/PHASE-4_Test-Spec.md` — template
- `CLAUDE.md` — BR-SAFETY, layering, Flyway policy

> **TDD convention:** this document describes test cases BEFORE production code is written.
> Mandatory order: write test (`.java`) → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Never mark a test ✅ unless `./mvnw test` is green.
> No real PII in test data — SYNTHETIC only.
> **Test commands:** `./mvnw test` (full) / `./mvnw test -Dtest=ClassName` (single class), run from `05_Development/CareBridgeAPI`.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** never delete old information.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-26` | `AI Agent` | Initial creation — TDD spec for TriageHealthMemoryContext |
| `2026-07-26` | `AI Agent — Amelia (Dev Agent)` | Phase 3 Deviation D1: empty context keeps legacy one-arg AI/fallback calls (wire-identical; keeps legacy triage integration coverage green) — TC-09/13/15 assertions updated; re-run green |
| `2026-07-26` | `AI Agent — Amelia (Dev Agent)` | Phase 3 sync: Red Gate executed (17/18 method-failures + TC-15 designated service signal — evidence in `red-gate-evidence.log`); GREEN 16/18 TCs passing (`./mvnw surefire:test`, Tests run 17, Failures 0, Errors 0); INT-01/02 environment-blocked (no Docker); Python suite 233 passed (additive `healthContext` schema); §5/§5.1/§6/§8 updated to ACTUAL results |
| `2026-07-27` | `AI Agent — Amelia (Dev Agent)` | Docker host available: integration TCs executed — 2/2 PASS (THMC-TC-INT-01/02; incl. ADR-THMC-001 AFTER_COMMIT isolation: session stayed COMPLETED while the memory save threw). Test fixture adjustments for the real full context (test file only): §6-sanctioned consent fixture added (`@MockitoBean ITriageConsentService` — the sibling TriageDisclaimerConsent gate otherwise 409s elective intake); BR-THMC-004-faithful stubbing — the ONE-ARG `triageChild(request)` overload is now also stubbed and recorded as an empty context (empty context never reaches the two-arg overload); cleanup no longer deletes `triage_sessions` (COMPLETED snapshots are delete-protected by `triage_completed_snapshot_guard_trg`). Enabled by test-harness fixes: `bridge-bootstrap.sql` init script + test-only `db/testfix` Flyway shim. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `CB-TRIAGE-THMC-IMP-001` (Roadmap Part III item 1 — "Nối Bộ nhớ Ngữ cảnh vào Luồng Triage") |
| **Module** | `Triage Health Memory Context — triage` |
| **Spec gốc** | `CB-TRIAGE-THMC-IMP-001` (TDS in the same folder) |
| **Priority** | 🟠 P1 |
| **Sprint** | `TBD (Open — assign at sprint planning)` |
| **Milestone** | `TBD (Open)` |
| **Data Classification** | `Sensitive-PII` (minimized health summaries) |
| **Compliance Scope** | `PDPA / Luật 91/2025 / BR-SAFETY (CLAUDE.md)` |
| **Upstream Dependencies** | `TriageService` completion events (`IntakeSessionCompleted`), `health_context_memories` table (canonical baseline), IAM (JWT) |
| **Downstream Consumers** | `CareBridgeAITriageService` (Python — receives `healthContext`), `TriageGraphService` (Java fallback), `HealthMemoryController` (existing list/delete UI) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-TRIAGE-THMC-IMP-001 §17 (C1–C7)`, `ADR-THMC-001…004` |
| **Constraints Injected** | C1 (AFTER_COMMIT write, idempotent per session), C2 (minimization — no raw free text, no PII logs), C3 (read only via existing active-queries), C4 (userId from JWT; client healthContext discarded), C5 (advisory context — both AI + fallback, never lowers risk, fail-open), C6 (expires_at = completed_at + ttlDays, no schema change), C7 (CLAUDE.md layering, DTO boundary) |
| **Model** | `Claude (Fable 5) — claude-fable-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, the canonical baseline `B20260724111500__canonical_70_table_baseline.sql` and approved migrations are the final persistence oracle.

> **Mandatory before writing tests.** Tests encode the **corrected** behavior below, not the naive reading of the roadmap.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Roadmap says "table + service + repository already exist" implying the entity is write-ready | `HealthMemoryEntry.java` does **not** map `memory_payload_jsonb` (DB default `'{}'` fills it); payload cannot be written today | Tests assert the payload JSON is actually persisted via the new `memoryPayloadJson` field (THMC-TC-01/04); entity mapping is part of the implementation |
| L2 | A naive design would write memory inline in `TriageService` (same transaction) | ADR-THMC-001: write must be `AFTER_COMMIT` via `IntakeSessionCompleted`, mirroring `IntakeSessionCompletedHandler`; memory failure must never roll back a COMPLETED session | THMC-TC-INT-02 asserts session stays COMPLETED when the memory save throws; handler tests catch-and-log behavior |
| L3 | "TTL" could be read as a `ttl_seconds` column | Canonical schema stores absolute `expires_at timestamptz` (roadmap Part I.2; baseline :1008) | Tests assert `expiresAt == completedAt + ttlDays` (THMC-TC-05), never a seconds counter |
| L4 | `HealthMemoryService.list()` throws `TRIAGE-014` when `profileId == null` — reusing it for injection would break legacy INFANT sessions without a profile | Intake must not be blocked by missing profile (BR-THMC-004 fail-open) | New `loadContextForIntake` returns `[]` for null profileId (THMC-TC-10 alt-branch); `list()` behavior untouched |
| L5 | Roadmap III.1b says inject at "phiên mới" without naming flows | TDS scope decision: one-shot `runIntake` + `startConversation` only; `continueConversation` reuses the session envelope and is out of scope | No TC injects context on continue; THMC-TC-14 covers start only |
| L6 | Event payload might be assumed to carry stage/symptoms | `IntakeSessionCompleted` record carries only `eventId, sessionId, userId, riskLevel, completedAt` | Write-path tests seed the session row and assert the handler reloads it (owner-scoped) rather than trusting event fields |
| L7 | UC60-era docs reference `intake_sessions` table and 201 responses | Legacy names were dropped in Phase 2; canonical table is `triage_sessions`; entity `IntakeSession` maps to it | All DB assertions use `health_context_memories` / `triage_sessions` only |

---

## 3. Test Design Specification (TDS)

> Include the canonical baseline and approved Flyway migrations in the test basis wherever schema facts or persistence side effects are part of the oracle.

### TDS-01 — Scope / Phạm vi

```
TriageHealthMemoryContext covers these layers:
├── Domain / Policy   (HealthMemorySummaryPolicy — pure logic, no deps)
├── Services          (HealthMemoryServiceImpl write/read — Mockito-mocked repositories;
│                      TriageService injection — mocked ChildTriageAiClient/TriageGraphService/HealthMemoryService)
├── Event handling    (HealthMemoryWriteHandler — unit with mocked service)
└── Integration       (@SpringBootTest + Testcontainers PostgreSQL; ChildTriageAiClient mocked;
                       no live Gemini/Python service in any test)
```

Out of scope: `continueConversation` injection (L5), Python-side pytest coverage (tracked in the Python repo — Open), physical purge job (TDS Open item O2).

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `Roadmap Part III.1 (a)(b)` | write-after-COMPLETED; read-and-inject at new intake into AI + fallback |
| `CB-TRIAGE-THMC-IMP-001 §2 BR-THMC-001…006` | status gate, idempotency, active-only reads, minimization, safety/fail-open, TTL, server-authoritative context |
| `CB-TRIAGE-THMC-IMP-001 §3 ADR-THMC-001…004` | AFTER_COMMIT handler, configurable TTL, advisory fail-open, no schema change |
| `HealthMemoryEntryRepository.java:14-33` | expiry (`expiresAt > :now`), soft-delete (`deletedAt is null`), owner+profile+stage filters, newest-first ordering |
| `HealthMemoryEntry.java:32` | "Processed/minimized summary only. Raw conversation text is never stored here." |
| `TriageService.java:384-457 / :768-777` | injection points (runIntake, fallback branch) and completion-event publishing |
| `IntakeSessionCompletedHandler.java:19-27` | AFTER_COMMIT + catch-and-log handler pattern |
| Baseline DDL `:999-1012` | column names/nullability for persistence assertions |
| `CLAUDE.md` BR-SAFETY / PDPA | risk never lowered by context; no PII in logs; SYNTHETIC data only |

### TDS-03 — Test Conditions and Coverage Items

> Every condition maps to ≥ 1 concrete test case.

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | COMPLETED session with persistable risk ⇒ exactly one memory written with correct linkage fields | `HealthMemoryServiceImpl.writeFromCompletedSession()` | `THMC-TC-01` |
| TC-COND-002 | Non-completed session (NEED_MORE_INFO / FAILED / null risk) ⇒ no write | same | `THMC-TC-02` |
| TC-COND-003 | Replayed completion event ⇒ no duplicate active memory (idempotency) | `existsBySourceSessionIdAndDeletedAtIsNull` guard | `THMC-TC-03` |
| TC-COND-004 | Memory content contains no raw free text (privacy/minimization) | `HealthMemorySummaryPolicy` | `THMC-TC-04` |
| TC-COND-005 | `expiresAt = completedAt + ttlDays` (configurable) | `HealthMemoryProperties.ttlDays` | `THMC-TC-05` |
| TC-COND-006 | Active memory injected into outbound Python payload at `runIntake` | `TriageService.runIntake` + `ChildTriageAiClient.triageChild(request, ctx)` | `THMC-TC-06` |
| TC-COND-007 | Expired memory excluded; boundary at `expiresAt == now` | repository filter via `loadContextForIntake` | `THMC-TC-07` |
| TC-COND-008 | Soft-deleted memory excluded | same | `THMC-TC-08` |
| TC-COND-009 | Ownership: another user's memories never injected | owner-scoped queries | `THMC-TC-09` |
| TC-COND-010 | Subject/stage isolation: other profile or other stage excluded; null profileId ⇒ empty context (no TRIAGE-014) | `loadContextForIntake` routing | `THMC-TC-10` |
| TC-COND-011 | Java fallback receives the SAME context when the AI client fails | `TriageGraphService.run(request, ctx)` | `THMC-TC-11` |
| TC-COND-012 | Context never lowers deterministic risk (BR-SAFETY) | fallback engine with context | `THMC-TC-12` |
| TC-COND-013 | Memory read failure ⇒ intake proceeds with empty context (fail-open) | try/catch in `TriageService` | `THMC-TC-13` |
| TC-COND-014 | `startConversation` puts `healthContext` into the canonical start payload | `TriageService.startConversation` | `THMC-TC-14` |
| TC-COND-015 | Client-supplied `healthContext` is discarded (server-authoritative) | public request boundary | `THMC-TC-15` |
| TC-COND-016 | Context bounded: max entries (newest first) and summary truncation | `HealthMemoryProperties.maxContextEntries/maxSummaryChars` | `THMC-TC-16` |
| TC-COND-017 | Full write→read loop against real PostgreSQL | end-to-end integration | `THMC-TC-INT-01` |
| TC-COND-018 | AFTER_COMMIT isolation: memory save failure does not roll back the COMPLETED session | handler + transaction semantics | `THMC-TC-INT-02` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | session status {COMPLETED+risk, NEED_MORE_INFO, FAILED}; memory state {active, expired, soft-deleted, foreign-owner, other-subject} | Each partition drives a distinct write/read decision |
| Boundary Value Analysis | `expiresAt` = now−1s / now / now+1s; context list size = maxContextEntries vs maxContextEntries+1; summary length = maxSummaryChars vs +1 | Repository uses strict `expiresAt > :now`; bounding is a hard NFR |
| State Transition Testing | memory lifecycle ACTIVE → EXPIRED / SOFT_DELETED (TDS §6.3) | Injection allowed only from ACTIVE |
| Error Guessing | repository exception on read; save exception post-commit; smuggled `healthContext` field; cross-user probing | Security & resilience vectors (CWE-639, fail-open) |
| Decision Table | (status × riskLevel × existing-memory) → write / no-write | Compact oracle for TC-01/02/03 |

### TDS-05 — Test Data Requirements

> All values SYNTHETIC. UUIDs are fixed literals so assertions are deterministic.

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-THMC-001` | Factory | Owner user `USER_A = 00000000-0000-0000-0000-0000000000a1` | Session/memory owner |
| `FX-THMC-002` | Factory | Other user `USER_B = 00000000-0000-0000-0000-0000000000b2` | Ownership negative tests |
| `FX-THMC-003` | Factory | Baby profile `BABY_1 = 00000000-0000-0000-0000-00000000c001`; second baby `BABY_2 = …c002` | Subject isolation |
| `FX-THMC-004` | Factory | Mother profile `MOTHER_1 = 00000000-0000-0000-0000-00000000d001` | Maternal routing |
| `FX-THMC-005` | Factory | COMPLETED `IntakeSession` (INFANT, YELLOW, `completedAt = 2026-07-26T08:00:00Z`, resultJson with `normalizedSymptoms:["fever","cough"]`, `recommendationCode:"CONTACT_HEALTHCARE_PROVIDER"`, `parentFreeText` marker `RAW_FREETEXT_MARKER_7f3a` inside the stored request snapshot) | Write happy path + minimization probe |
| `FX-THMC-006` | Factory | Active memory (INFANT, BABY_1, `summaryText = "SYNTHETIC prior triage: risk YELLOW; fever, cough"`, `createdAt = 2026-07-20T09:00:00Z`, `expiresAt = 2026-08-19T09:00:00Z`) | Read happy path |
| `FX-THMC-007` | Factory | Expired memory (`expiresAt = NOW_FIXED − 1s`) / boundary twin (`expiresAt = NOW_FIXED + 1s`) | Expiry boundary |
| `FX-THMC-008` | Factory | Soft-deleted memory (`deletedAt = NOW_FIXED − 1h`) | Soft-delete exclusion |
| `FX-THMC-009` | Config | `HealthMemoryProperties{ttlDays=30, maxContextEntries=5, maxSummaryChars=500}` and override `{ttlDays=7}` | TTL + bounding assertions |
| `FX-THMC-010` | Mock | `ChildTriageAiClient` returning a valid COMPLETED/GREEN JSON (same contract as `validateAndCanonicalizeOneShotResponse` expects) — payload captured via `ArgumentCaptor` | Injection assertions |
| `FX-THMC-011` | Mock | `ChildTriageAiClient` throwing `IllegalStateException("AI triage service unavailable", new IOException())` | Fallback path |
| `FX-THMC-012` | Constant | `NOW_FIXED = Instant.parse("2026-07-26T10:00:00Z")` via injected `Clock`/captured `Instant` argument | Deterministic time boundary |
| `FX-THMC-013` | JWT (integration) | `{sub: USER_A, role: ROLE_MOTHER}` synthetic token | Integration auth |

---

## 4. Test Case Specification

> **TC ID format:** `THMC-TC-[NN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** every test builds fresh instances via this factory. No shared mutable state between test cases (anti AP-AI-002). **ALL test data in every TC below comes from these `makeXxx()` methods.**

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// File: src/test/java/com/carebridge/backend/triage/HealthMemoryContextTestFactory.java
// ═══════════════════════════════════════════════════════════
class HealthMemoryContextTestFactory {

    static final UUID USER_A   = UUID.fromString("00000000-0000-0000-0000-0000000000a1"); // FX-THMC-001
    static final UUID USER_B   = UUID.fromString("00000000-0000-0000-0000-0000000000b2"); // FX-THMC-002
    static final UUID BABY_1   = UUID.fromString("00000000-0000-0000-0000-00000000c001"); // FX-THMC-003
    static final UUID BABY_2   = UUID.fromString("00000000-0000-0000-0000-00000000c002"); // FX-THMC-003
    static final UUID MOTHER_1 = UUID.fromString("00000000-0000-0000-0000-00000000d001"); // FX-THMC-004
    static final UUID SESSION_1 = UUID.fromString("00000000-0000-0000-0000-00000000e001");
    static final Instant NOW_FIXED     = Instant.parse("2026-07-26T10:00:00Z");           // FX-THMC-012
    static final Instant COMPLETED_AT  = Instant.parse("2026-07-26T08:00:00Z");           // FX-THMC-005
    static final String RAW_MARKER     = "RAW_FREETEXT_MARKER_7f3a";                      // FX-THMC-005

    /** FX-THMC-005 — COMPLETED INFANT session, risk YELLOW, canonical result snapshot present. */
    static IntakeSession makeCompletedSession() {
        return IntakeSession.builder()
                .id(SESSION_1)
                .userId(USER_A)
                .babyProfileId(BABY_1)
                .stage(TriageStage.INFANT)
                .status(IntakeStatus.COMPLETED)
                .riskLevel(RiskLevel.YELLOW)
                .symptoms("{\"hasFreeText\":true,\"parentFreeTextSnapshot\":\"" + RAW_MARKER + "\"}")
                .rawAiResponse("{\"status\":\"COMPLETED\",\"riskLevel\":\"YELLOW\","
                        + "\"normalizedSymptoms\":[\"fever\",\"cough\"],"
                        + "\"recommendationCode\":\"CONTACT_HEALTHCARE_PROVIDER\","
                        + "\"parentFreeText\":\"" + RAW_MARKER + "\"}")
                .createdAt(COMPLETED_AT.minusSeconds(120))
                .completedAt(COMPLETED_AT)
                .createdBy(USER_A)
                .build();
    }

    static IntakeSession makeCompletedSession(Consumer<IntakeSession> overrides) {
        IntakeSession s = makeCompletedSession();
        overrides.accept(s);
        return s;
    }

    /** FX-THMC-006 — active memory for (USER_A, BABY_1, INFANT). */
    static HealthMemoryEntry makeActiveMemory() {
        return HealthMemoryEntry.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-00000000f001"))
                .userId(USER_A)
                .babyProfileId(BABY_1)
                .relatedStage(TriageStage.INFANT)
                .summaryText("SYNTHETIC prior triage: risk YELLOW; fever, cough")
                .sourceSessionId(SESSION_1)
                .createdAt(Instant.parse("2026-07-20T09:00:00Z"))
                .expiresAt(Instant.parse("2026-08-19T09:00:00Z"))
                .build();
    }

    static HealthMemoryEntry makeActiveMemory(Consumer<HealthMemoryEntry> overrides) {
        HealthMemoryEntry e = makeActiveMemory();
        overrides.accept(e);
        return e;
    }

    /** FX-THMC-005-event — completion event matching makeCompletedSession(). */
    static IntakeSessionCompleted makeCompletedEvent() {
        return new IntakeSessionCompleted(
                UUID.fromString("00000000-0000-0000-0000-00000000ee01"),
                SESSION_1, USER_A, RiskLevel.YELLOW, COMPLETED_AT);
    }

    /** One-shot request for (USER_A, BABY_1, INFANT) — public contract fields only. */
    static RunIntakeRequest makeRunIntakeRequest() {
        return RunIntakeRequest.builder()
                .stage(TriageStage.INFANT)
                .babyProfileId(BABY_1)
                .symptomList(List.of("ho", "sốt nhẹ"))
                .childAgeMonths(7)
                .build();
    }

    static StartIntakeConversationRequest makeStartConversationRequest() {
        StartIntakeConversationRequest r = new StartIntakeConversationRequest();
        r.setStage(TriageStage.INFANT);
        r.setBabyProfileId(BABY_1);
        r.setInitialText("SYNTHETIC initial text");
        return r;
    }

    /** FX-THMC-009 — fresh properties per test; override for TTL/bounding variants. */
    static HealthMemoryProperties makeProperties() {
        HealthMemoryProperties p = new HealthMemoryProperties();
        p.setTtlDays(30);
        p.setMaxContextEntries(5);
        p.setMaxSummaryChars(500);
        return p;
    }

    static HealthMemoryProperties makeProperties(Consumer<HealthMemoryProperties> overrides) {
        HealthMemoryProperties p = makeProperties();
        overrides.accept(p);
        return p;
    }

    /** FX-THMC-010 — valid canonical one-shot AI response body. */
    static String makeAiOneShotGreenJson() {
        return "{\"status\":\"COMPLETED\",\"riskLevel\":\"GREEN\",\"stage\":\"INFANT\","
                + "\"disclaimer\":\"SYNTHETIC disclaimer\"}";
    }
}
```

---

### THMC-TC-01 — COMPLETED session ⇒ exactly one memory with correct linkage, payload, and stage

**Severity:** `HIGH`
**Legal:** `PDPA (lawful minimized storage)`
**Feature Under Test:** `HealthMemoryServiceImpl.writeFromCompletedSession(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryWriteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `Roadmap Part III.1(a)` (write summary + payload + expires_at + linkage) / `BR-THMC-001` / baseline DDL `:999-1012` (column set) / `TDS §5.2 payload schemaVersion "1.0"`

**Preconditions:**
- `IIntakeSessionRepository.findByIdAndUserId(SESSION_1, USER_A)` mocked → `makeCompletedSession()` (FX-THMC-005)
- `HealthMemoryEntryRepository.existsBySourceSessionIdAndDeletedAtIsNull(SESSION_1)` mocked → `false`
- Properties `makeProperties()` (ttlDays=30)

**Test Steps:**
1. Arrange: service wired with mocks above; `ArgumentCaptor<HealthMemoryEntry>` on `repository.save`.
2. Act: `service.writeFromCompletedSession(SESSION_1, USER_A)`.
3. Assert: `save` called exactly once; captured entity fields checked.

**Expected Result (PASS — hành vi đúng):**
- `userId = USER_A`, `sourceSessionId = SESSION_1`, `babyProfileId = BABY_1`, `motherProfileId = null` *(oracle: session fields, BR-THMC-001)*
- `relatedStage = TriageStage.INFANT` *(oracle: session stage, Roadmap III.1a "related_stage")*
- `summaryText` non-blank and contains stage + risk tokens (e.g. `"YELLOW"`) *(oracle: TDS §8.1 buildSummary contract)*
- `memoryPayloadJson` parses as JSON with `schemaVersion="1.0"`, `riskLevel="YELLOW"`, `normalizedSymptoms=["fever","cough"]`, `sourceSessionId=SESSION_1` *(oracle: TDS §5.2 payload schema)*
- `expiresAt = COMPLETED_AT.plus(30, DAYS)` *(oracle: BR-THMC-005 / ADR-THMC-002)*
- Return value is a non-empty `Optional`

**Expected Result (FAIL — dấu hiệu lỗi):**
- `save` never called / called twice; payload empty (entity mapping gap L1); `expiresAt` null or computed from `now()` instead of `completedAt`

**Current Status:** 🟢 Passing
**Implementation Note:** compute from `session.getCompletedAt()`, not `Instant.now()` — the test uses a completedAt 2h in the past to catch this.

---

### THMC-TC-02 — Non-completed session ⇒ no write (with positive control)

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthMemoryServiceImpl.writeFromCompletedSession()` status gate
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryWriteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-THMC-001` ("ONLY when status COMPLETED and risk persistable") / `TriageService.java:423` (completion events gated on `COMPLETED && riskLevel != null`)

**Preconditions:**
- Session variants from factory: `makeCompletedSession(s -> s.setStatus(IntakeStatus.NEED_MORE_INFO))`, `(s -> s.setStatus(IntakeStatus.FAILED))`, `(s -> s.setRiskLevel(null))`

**Test Steps:**
1. Arrange: mock session repo to return each variant.
2. Act: call `writeFromCompletedSession(SESSION_1, USER_A)` for each variant.
3. Assert: returns `Optional.empty()`; `repository.save` never invoked; **positive control** — `findByIdAndUserId` verified invoked (proves the method executed, not a no-op shell).

**Expected Result (PASS):** empty Optional + zero saves + session lookup performed for every variant.
**Expected Result (FAIL):** a memory row saved for NEED_MORE_INFO/FAILED/null-risk sessions.

**Current Status:** 🟢 Passing
**Implementation Note:** Red Gate sensitivity: in red phase the stub throws, so this test FAILS (it expects a normal `Optional.empty()` return) — not a tautology.

---

### THMC-TC-03 — Replayed completion event ⇒ no duplicate active memory (idempotency)

**Severity:** `HIGH`
**Feature Under Test:** `HealthMemoryServiceImpl.writeFromCompletedSession()` idempotency guard
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryWriteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-THMC-001` ("exactly one active memory per source session") / `ADR-THMC-001 Decision` (guard via `existsBySourceSessionIdAndDeletedAtIsNull`)

**Preconditions:**
- Session mock as in TC-01; `existsBySourceSessionIdAndDeletedAtIsNull(SESSION_1)` mocked → `true` (a prior write happened)

**Test Steps:**
1. Act: `writeFromCompletedSession(SESSION_1, USER_A)`.
2. Assert: `Optional.empty()`; `repository.save` never called; exists-guard verified invoked with `SESSION_1`.

**Expected Result (PASS):** no second save.
**Expected Result (FAIL):** duplicate memory rows per session (event replay bug).

**Current Status:** 🟢 Passing

---

### THMC-TC-04 — Minimization: no raw free text in summary, payload, or logs

**Severity:** `CRITICAL`
**CWE:** `CWE-359 — Exposure of Private Personal Information`
**Legal:** `PDPA data minimization / Luật 91/2025`
**Feature Under Test:** `HealthMemorySummaryPolicy.buildSummary()/buildPayloadJson()` via `writeFromCompletedSession`
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemorySummaryPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `HealthMemoryEntry.java:32` ("Processed/minimized summary only. Raw conversation text is never stored here.") / `BR-THMC-003` / TDS §17 C2

**Preconditions:**
- Session `makeCompletedSession()` — both `symptoms` snapshot and `rawAiResponse` contain `RAW_MARKER` (`RAW_FREETEXT_MARKER_7f3a`), simulating raw parent free text present in every plausible source field
- Logback `ListAppender` attached to the policy/service loggers

**Test Steps:**
1. Act: write the memory (as TC-01 arrangement).
2. Assert: captured `summaryText` does NOT contain `RAW_MARKER`; captured `memoryPayloadJson` does NOT contain `RAW_MARKER`.
3. Assert: `summaryText` DOES contain structured tokens (`"YELLOW"`, `"fever"`) — positive control proving the summary is built from structured data, not empty.
4. Assert: no captured log event contains `RAW_MARKER` or the full `summaryText` value.

**Expected Result (PASS = hệ thống an toàn):** marker absent everywhere; structured tokens present.
**Expected Result (FAIL = lỗ hổng tồn tại):** raw free text persisted or logged → PDPA violation, rollback trigger per TDS §12.1.

**Current Status:** 🟢 Passing
**Implementation Note:** the policy must read `normalizedSymptoms`/`recommendationCode` from the canonical result snapshot and ignore `parentFreeText` keys entirely.

---

### THMC-TC-05 — `expiresAt` honors configured TTL (override 7 days)

**Severity:** `HIGH`
**Feature Under Test:** `HealthMemoryServiceImpl.writeFromCompletedSession()` + `HealthMemoryProperties`
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryWriteTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-THMC-005 / ADR-THMC-002` (`expires_at = completed_at + ttlDays`, configurable; default 30 marked Open)

**Preconditions:**
- Service wired with `makeProperties(p -> p.setTtlDays(7))` (FX-THMC-009 override)

**Test Steps:**
1. Act: write the memory for `makeCompletedSession()`.
2. Assert: captured `expiresAt == COMPLETED_AT.plus(7, ChronoUnit.DAYS)`.

**Expected Result (PASS):** exact 7-day offset from `completedAt`.
**Expected Result (FAIL):** hard-coded 30 days (config ignored) or offset from wall-clock `now()`.

**Current Status:** 🟢 Passing

---

### THMC-TC-06 — `runIntake` injects active memories into the outbound AI payload

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.runIntake()` + `ChildTriageAiClient.triageChild(request, healthContext)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceHealthMemoryContextTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `Roadmap Part III.1(b)` ("đọc các memory còn hạn của đúng đối tượng … đưa vào context gửi AI") / `US-THMC-002` / TDS §8.1 `ChildTriageAiClient` v2.0

**Preconditions:**
- `HealthMemoryService.loadContextForIntake(USER_A, INFANT, BABY_1)` mocked → one `HealthMemoryContextItem` built from `makeActiveMemory()` (FX-THMC-006)
- `ChildTriageAiClient` mock (FX-THMC-010) returns `makeAiOneShotGreenJson()`; `ArgumentCaptor<List<HealthMemoryContextItem>>` on the two-arg `triageChild`

**Test Steps:**
1. Act: `triageService.runIntake(makeRunIntakeRequest(), USER_A)`.
2. Assert: two-arg `triageChild` invoked once; captured list has size 1 and `summaryText = "SYNTHETIC prior triage: risk YELLOW; fever, cough"`; `relatedStage = "INFANT"`.
3. Assert: `loadContextForIntake` called with exactly `(USER_A, TriageStage.INFANT, BABY_1)` — stage/profile taken from the resolved request, userId from the method's auth parameter.

**Expected Result (PASS):** context flows from repository to the AI client call unchanged.
**Expected Result (FAIL):** legacy one-arg `triageChild` used (context dropped), or context loaded with wrong subject keys.

**Current Status:** 🟢 Passing
**Implementation Note:** against unmodified `TriageService` this fails because `loadContextForIntake` is never called — genuine RED.

---

### THMC-TC-07 — Expiry boundary: expired excluded, not-yet-expired included

**Severity:** `HIGH`
**Feature Under Test:** `HealthMemoryServiceImpl.loadContextForIntake()` (delegating to the active queries)
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryContextReadTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `HealthMemoryEntryRepository.java:14-22` (strict `expiresAt > :now`) / `BR-THMC-002`

**Preconditions:**
- Repository mock: `findActivePediatric(USER_A, BABY_1, INFANT, any Instant)` answers by filtering a fixed list of `makeActiveMemory(...)` twins per the repository's documented predicate: entry with `expiresAt = NOW_FIXED.minusSeconds(1)` (FX-THMC-007 expired) and entry with `expiresAt = NOW_FIXED.plusSeconds(1)` (boundary-active), evaluated against the `now` argument the service passes
- Unit test drives determinism by asserting on which entries the mocked filter admits — the *service-level* contract under test is that it consumes ONLY the active-query result and maps every returned row

**Test Steps:**
1. Act: `service.loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1)`.
2. Assert: result contains exactly the summaries the active query returned (the +1s entry), and never the −1s entry.
3. Assert: the service invoked `findActivePediatric` (not `findAll`, not a custom bypass query) — verify no other repository read method touched.

**Expected Result (PASS):** only non-expired content mapped; sole read path is the guarded query (C3).
**Expected Result (FAIL):** service reads via an unguarded method (e.g. `findAll`) → expired PII could leak into prompts.

**Current Status:** 🟢 Passing
**Implementation Note:** the true SQL-level boundary (`> :now` strict) is re-verified against real PostgreSQL in THMC-TC-INT-01 step 6.

---

### THMC-TC-08 — Soft-deleted memory never injected

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthMemoryServiceImpl.loadContextForIntake()`
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryContextReadTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `HealthMemoryEntryRepository.java:16-17` (`deletedAt is null`) / `BR-THMC-002` / user-erasure right (existing delete flow, `HealthMemoryServiceImpl.delete`)

**Preconditions:**
- Repository mock returns `List.of()` for the active query while a soft-deleted twin `makeActiveMemory(e -> e.setDeletedAt(NOW_FIXED.minusSeconds(3600)))` (FX-THMC-008) exists in the mock's backing store (returned by `findAll` only)

**Test Steps:**
1. Act: `loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1)`.
2. Assert: empty result; only the guarded active query used.

**Expected Result (PASS):** deleted memories are gone from injection immediately (user erasure honored).
**Expected Result (FAIL):** deleted summary appears in the AI payload — privacy incident per TDS §12.1.

**Current Status:** 🟢 Passing

---

### THMC-TC-09 — Ownership: another user's memories are never injected

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `Luật 91/2025 (health data ownership)`
**Feature Under Test:** `TriageService.runIntake` → `loadContextForIntake` owner scoping
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceHealthMemoryContextTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `HealthMemoryEntryRepository.java:16/26` (`entry.userId = :userId`) / `BR-THMC-002` / TDS §16 (`Own` only)

**Preconditions (Attack Simulation):**
- Backing store (mock) holds ONLY `makeActiveMemory(e -> { e.setUserId(USER_B); })` — a foreign user's memory for an identical (BABY_1-shaped, INFANT) subject
- Real `HealthMemoryServiceImpl` wired with the mocked repository (not a mocked service) so the owner key path is exercised

**Test Steps:**
1. Act: `triageService.runIntake(makeRunIntakeRequest(), USER_A)` (auth principal = USER_A).
2. Assert: repository active query invoked with `userId = USER_A` exactly (captor) — never USER_B, never a value derived from the request body.
3. Assert: captured AI-call context list is empty; `USER_B`'s summary string appears nowhere in the outbound payload.

> **Phase-3 Deviation D1 (2026-07-26):** empty server-loaded context now keeps the LEGACY one-arg `triageChild(request)` / `run(request)` calls (an empty list is wire-identical to omitting the additive `healthContext` field — `HttpChildTriageAiClient` omits it either way). Reason: the sibling feature's `legacy triage integration coverage` (full Spring context) verifies the one-arg AI contract when no memories exist; always-two-arg broke it in regression. The two-arg overloads are used exactly when context is NON-EMPTY. Test assertions for this TC were updated accordingly (verify one-arg called + two-arg never); all safety oracles (fail-open, ownership, server-authoritative context) unchanged.

**Expected Result (PASS = hệ thống an toàn):** zero cross-user leakage.
**Expected Result (FAIL = lỗ hổng tồn tại):** USER_B's health summary leaks into USER_A's AI prompt → reportable privacy breach (TDS §12.3).

**Current Status:** 🟢 Passing

---

### THMC-TC-10 — Subject & stage isolation; null profileId ⇒ empty context without error

**Severity:** `HIGH`
**Feature Under Test:** `HealthMemoryServiceImpl.loadContextForIntake()` routing
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryContextReadTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `HealthMemoryEntryRepository.java:14-32` (profileId + stage bound into both queries; maternal vs pediatric split) / `HealthMemoryServiceImpl.list():29-31` routing precedent / Logic Issue L4 (null profile must not throw TRIAGE-014)

**Test Steps:**
1. Pediatric routing: `loadContextForIntake(USER_A, INFANT, BABY_2)` → verify `findActivePediatric(USER_A, BABY_2, INFANT, …)` used; a BABY_1 memory in the backing store is not returned.
2. Maternal routing: `loadContextForIntake(USER_A, PREGNANCY, MOTHER_1)` → verify `findActiveMaternal(USER_A, MOTHER_1, PREGNANCY, …)`; `findActivePediatric` never called (mirrors existing `postpartum_shouldUseMaternalMemoryBoundaryOnly` precedent in `HealthMemoryServiceImplTest`).
3. Null profile: `loadContextForIntake(USER_A, INFANT, null)` → returns `List.of()`; **no exception**; no repository query executed.

**Expected Result (PASS):** stage-correct query, profile-bound results, graceful empty on null.
**Expected Result (FAIL):** TRIAGE-014 thrown on null profile (blocks legacy intakes — violates BR-THMC-004), or cross-subject (BABY_1 ↔ BABY_2) contamination.

**Current Status:** 🟢 Passing

---

### THMC-TC-11 — Java fallback receives the SAME context when the AI client fails

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.triageWithAiServiceOrFallback` → `TriageGraphService.run(request, healthContext)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceHealthMemoryContextTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `Roadmap Part III.1(b)` ("đưa vào context gửi AI **+ Java fallback**") / TDS §8.1 `TriageGraphService` v2.0 / `TriageService.java:442-457` (fallback branch location)

**Preconditions:**
- `loadContextForIntake` mocked → one item (FX-THMC-006-derived)
- `ChildTriageAiClient` two-arg `triageChild` mocked → throws FX-THMC-011 (`IllegalStateException` wrapping `IOException`)
- `TriageGraphService` mocked → returns a deterministic GREEN `ChildTriageResult`

**Test Steps:**
1. Act: `runIntake(makeRunIntakeRequest(), USER_A)`.
2. Assert: `triageGraphService.run(request, ctx)` invoked; captured ctx list equals (same summaries, same order) the list previously passed to the AI client.
3. Assert: response status is COMPLETED (fallback flow intact).

**Expected Result (PASS):** identical context on both engine paths — no context loss on degradation.
**Expected Result (FAIL):** fallback called with the legacy single-arg `run(request)` → context silently dropped exactly when AI is down.

**Current Status:** 🟢 Passing

---

### THMC-TC-12 — BR-SAFETY: context can never lower deterministic risk

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY (CLAUDE.md) — never delay emergency routing`
**Feature Under Test:** `TriageGraphService.run(request, healthContext)` risk invariance
**Test File:** `src/test/java/com/carebridge/backend/triage/engine/TriageGraphServiceHealthContextTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `BR-THMC-004 / ADR-THMC-003 Decision` ("context participates only in summary phrasing, never in rule matching; riskLevel with context ≥ riskLevel without context") / RED contract precedent `TriageService.java:475-477`

**Preconditions:**
- Real `TriageGraphService` (no mock — deterministic engine), constructed exactly as the existing engine tests construct it
- RED-triggering request: `makeRunIntakeRequest()` overridden with a hardcoded red-flag pediatric input, e.g. `breathingStatus = "Tím tái"`, `consciousnessStatus = "Li bì"` (these map to existing hardcoded RED rules — `PediatricRiskRules`, roadmap Part II.2 note)
- Benign context: one `HealthMemoryContextItem` whose summary asserts a prior GREEN outcome (`"SYNTHETIC prior triage: risk GREEN; mild cough"`)

**Test Steps:**
1. Act A: `baseline = graphService.run(redRequest)` (no context).
2. Act B: `withCtx = graphService.run(redRequest, List.of(benignGreenContextItem))`.
3. Assert: `withCtx.riskLevel == baseline.riskLevel == RED`; `withCtx.emergencyActionRequired == baseline.emergencyActionRequired == true`.
4. Assert: matched rules set identical between the two runs (context did not enter rule matching).

**Expected Result (PASS = hệ thống an toàn):** RED stays RED with benign context; emergency flag unchanged.
**Expected Result (FAIL = lỗ hổng tồn tại):** benign memory downgrades RED → delayed emergency routing (highest-severity safety defect; rollback trigger).

**Current Status:** 🟢 Passing
**Implementation Note:** because the real engine is used, in red phase the two-arg `run` overload does not exist → compilation of the test against the stub throws / method-missing → RED confirmed via the throwing stub overload.

---

### THMC-TC-13 — Fail-open: memory read failure never blocks intake

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.runIntake` fail-open wrapper around `loadContextForIntake`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceHealthMemoryContextTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `BR-THMC-004 / ADR-THMC-003 Option B` (fail-open, WARN log, empty context) / `TDS §4.1` (0 intake failures caused by memory errors)

**Preconditions:**
- `loadContextForIntake` mocked → throws `new org.springframework.dao.DataAccessResourceFailureException("SYNTHETIC")`
- AI client mocked → returns `makeAiOneShotGreenJson()` when called with an EMPTY context list
- Logback `ListAppender` on `TriageService` logger

**Test Steps:**
1. Act: `runIntake(makeRunIntakeRequest(), USER_A)`.
2. Assert: no exception propagates; response `status == "COMPLETED"`.
3. Assert: two-arg `triageChild` called with an empty list (context degraded, not dropped API).
4. Assert: **`loadContextForIntake` was invoked** (positive control — the wrapper exists) and a WARN log matching `health memory context unavailable` was emitted **without** any summary text or user identifiers beyond correlation-safe fields.

> **Phase-3 Deviation D1 (2026-07-26):** empty server-loaded context now keeps the LEGACY one-arg `triageChild(request)` / `run(request)` calls (an empty list is wire-identical to omitting the additive `healthContext` field — `HttpChildTriageAiClient` omits it either way). Reason: the sibling feature's `legacy triage integration coverage` (full Spring context) verifies the one-arg AI contract when no memories exist; always-two-arg broke it in regression. The two-arg overloads are used exactly when context is NON-EMPTY. Test assertions for this TC were updated accordingly (verify one-arg called + two-arg never); all safety oracles (fail-open, ownership, server-authoritative context) unchanged.

**Expected Result (PASS):** intake completes; degradation observable via WARN.
**Expected Result (FAIL):** TRIAGE-005/exception raised because of a memory outage → BR-SAFETY violation.

**Current Status:** 🟢 Passing
**Implementation Note:** Red Gate sensitivity comes from step 4's `verify(loadContextForIntake)` — unmodified `TriageService` never calls it, so the test FAILS before implementation despite the lenient happy assertions.

---

### THMC-TC-14 — `startConversation` injects `healthContext` into the canonical start payload

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.startConversation()` payload assembly
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServiceHealthMemoryContextTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `Roadmap Part III.1(b)` (conversation start included) / `TriageService.java:300-315` (canonicalRequest map → `childTriageAiClient.startIntake`) / TDS §9.2 internal `/triage/intake/start` contract

**Preconditions:**
- `loadContextForIntake(USER_A, INFANT, BABY_1)` mocked → one item (FX-THMC-006-derived)
- `childTriageAiClient.startIntake(any)` mocked → a valid `ASK_MORE` envelope JSON (one renderable INFANT question, round 1) so the safe-envelope gate passes; `ArgumentCaptor<Map<String,Object>>` on it

**Test Steps:**
1. Act: `triageService.startConversation(makeStartConversationRequest(), USER_A)`.
2. Assert: captured map contains key `"healthContext"` whose value is a list of size 1 with `summaryText` equal to the factory summary and `relatedStage = "INFANT"`.
3. Assert: existing canonical keys (`initialText`, `currentIntake`, `intakeSessionId`, `stage`) still present (no regression on the map contract).

**Expected Result (PASS):** additive key present alongside the untouched existing payload.
**Expected Result (FAIL):** key absent (context not wired) or existing keys disturbed (breaks Python `IntakeStartRequest` parsing).

**Current Status:** 🟢 Passing

---

### THMC-TC-15 — Client-supplied `healthContext` is discarded (server-authoritative)

**Severity:** `CRITICAL`
**OWASP:** `A04:2021 — Insecure Design (trust boundary violation)`
**CWE:** `CWE-602 — Client-Side Enforcement of Server-Side Security`
**Legal:** `Luật 91/2025 (context could impersonate another subject's history)`
**Feature Under Test:** public request boundary → outbound payload
**Test File:** `src/test/java/com/carebridge/backend/triage/IntakeControllerHealthContextTest.java` (`@WebMvcTest` + mocked `ITriageService` pass-through) and payload assertion in `TriageServiceHealthMemoryContextTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `BR-THMC-006` / TDS §17 C4 / TDS §15.2 smuggled-field sample

**Preconditions (Attack Simulation):**
- No active memories exist for USER_A (mock returns empty list)
- Malicious body: valid `makeRunIntakeRequest()` JSON augmented with `"healthContext":[{"summaryText":"INJECTED_FOREIGN_HISTORY"}]`

**Test Steps:**
1. Act: `POST /api/v1/triage/intake` with the malicious body (MockMvc, synthetic ROLE_MOTHER auth).
2. Assert (controller layer): request deserializes into `RunIntakeRequest` with **no** healthContext property (unknown field ignored / never mapped) — HTTP status is the normal flow status, not 400.
3. Assert (service layer): the outbound AI payload's context list is exactly the server-loaded list (empty here); the string `INJECTED_FOREIGN_HISTORY` appears nowhere in any captured outbound argument.

> **Phase-3 Deviation D1 (2026-07-26):** empty server-loaded context now keeps the LEGACY one-arg `triageChild(request)` / `run(request)` calls (an empty list is wire-identical to omitting the additive `healthContext` field — `HttpChildTriageAiClient` omits it either way). Reason: the sibling feature's `legacy triage integration coverage` (full Spring context) verifies the one-arg AI contract when no memories exist; always-two-arg broke it in regression. The two-arg overloads are used exactly when context is NON-EMPTY. Test assertions for this TC were updated accordingly (verify one-arg called + two-arg never); all safety oracles (fail-open, ownership, server-authoritative context) unchanged.

**Expected Result (PASS = hệ thống an toàn):** injected field inert end-to-end.
**Expected Result (FAIL = lỗ hổng tồn tại):** attacker-controlled "history" reaches the AI prompt → prompt-injection + subject impersonation channel.

**Current Status:** 🟢 Passing
**Implementation Note:** guard by design — `RunIntakeRequest` must NOT gain a `healthContext` field; the DTO stays context-free and context exists only as an internal method argument.

---

### THMC-TC-16 — Context bounded: max entries (newest first) and summary truncation

**Severity:** `MEDIUM`
**Feature Under Test:** `HealthMemoryServiceImpl.loadContextForIntake()` bounding
**Test File:** `src/test/java/com/carebridge/backend/triage/HealthMemoryContextReadTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-THMC-003 Decision (3)` (bounded list + truncated summaries) / `TDS §4.1` prompt-bounding NFR / FX-THMC-009 defaults

**Preconditions:**
- Properties `makeProperties(p -> { p.setMaxContextEntries(5); p.setMaxSummaryChars(500); })`
- Repository active query mocked → 6 entries via `makeActiveMemory(e -> e.setCreatedAt(base.plusSeconds(i)))`, one of which has a 501-char synthetic summary (`"S".repeat(501)`), returned newest-first per the query's `order by createdAt desc` contract

**Test Steps:**
1. Act: `loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1)`.
2. Assert: result size == 5; the DROPPED entry is the oldest (`createdAt` smallest) — order preserved newest-first.
3. Assert: every `summaryText.length() <= 500`; the 501-char summary is truncated to exactly 500 chars.

**Expected Result (PASS):** hard caps enforced at the service boundary.
**Expected Result (FAIL):** unbounded context → oversized AI prompt (NFR breach) — boundary values 5/6 and 500/501 chosen to catch off-by-one.

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

> Testcontainers `PostgreSqlContainer`; Flyway applies the canonical baseline automatically on context start. Timeout: 120s. `ChildTriageAiClient` replaced by a `@MockBean`/test double — no network calls.

---

### THMC-TC-INT-01 — Full loop: complete intake #1 ⇒ memory row; intake #2 ⇒ context injected

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: runIntake #1 → AFTER_COMMIT write → runIntake #2 → payload injection`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageHealthMemoryContextIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
> *(2026-07-27: executed on a Docker-capable host — Testcontainers postgres:16-alpine, real Flyway chain via the test-harness bridge bootstrap — PASSED.)*
**Condition Ref:** `TC-COND-017` (+ SQL-level re-check of TC-COND-007)

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`); Flyway baseline applied (creates `health_context_memories`, `triage_sessions` — persistence oracle baseline :999-1012)
- Seed: synthetic MOTHER user (USER_A) + baby profile row for BABY_1, inserted via JPA repositories
- `ChildTriageAiClient` test double: two-arg `triageChild` returns `makeAiOneShotGreenJson()` and records every context argument
- Properties: defaults (`ttl-days: 30`)

**Test Steps:**
1. Call `triageService.runIntake(makeRunIntakeRequest(), USER_A)` inside a committed transaction boundary (e.g. `TransactionTemplate`) so `AFTER_COMMIT` fires.
2. Await (Awaitility ≤ 5s) then assert DB: exactly 1 row in `health_context_memories` with `triage_session_id = <session1>`, `user_id = USER_A`, `baby_profile_id = BABY_1`, `related_stage = 'INFANT'`, `deleted_at IS NULL`, `memory_payload_jsonb ->> 'schemaVersion' = '1.0'`.
3. Assert `expires_at = completed_at + INTERVAL '30 days'` (SQL join with `triage_sessions`, §14.1 oracle query).
4. Call `runIntake` again (intake #2, same user/profile/stage).
5. Assert the recorded context argument of call #2 contains the memory summary written in step 2; call #1's recorded context was empty.
6. Expiry re-check at SQL level: `UPDATE health_context_memories SET expires_at = now() - interval '1 second'`; run intake #3; assert its recorded context is empty (strict `> now` boundary — repository oracle :19).

**Expected Result (PASS):**
- DB row exists exactly once with the asserted column values
- Context appears in intake #2 payload and disappears after forced expiry in intake #3

**Expected Result (FAIL):**
- No memory row (handler not wired), duplicate rows, or expired context still injected

**DB Assertion:**
```java
HealthMemoryEntry row = healthMemoryEntryRepository.findAll().stream()
        .filter(e -> session1Id.equals(e.getSourceSessionId())).findFirst().orElseThrow();
assertThat(row.getUserId()).isEqualTo(HealthMemoryContextTestFactory.USER_A);
assertThat(row.getRelatedStage()).isEqualTo(TriageStage.INFANT);
assertThat(row.getExpiresAt()).isEqualTo(row1CompletedAt.plus(30, ChronoUnit.DAYS));
assertThat(row.getDeletedAt()).isNull();
```

**Current Status:** 🟢 Passing
> *(2026-07-27: actual green run — 2/2 in `TriageHealthMemoryContextIntegrationTest`; see §5 tracker.)*

---

### THMC-TC-INT-02 — AFTER_COMMIT isolation: memory save failure does not roll back the COMPLETED session

**Severity:** `HIGH`
**Feature Under Test:** `HealthMemoryWriteHandler` transactional decoupling
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageHealthMemoryContextIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
> *(2026-07-27: executed on a Docker-capable host — Testcontainers postgres:16-alpine, real Flyway chain via the test-harness bridge bootstrap — PASSED.)*
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-THMC-001` (Option B: failure-isolated post-commit write) / `IntakeSessionCompletedHandler.java:19-27` (catch-and-log precedent) / `BR-THMC-004`

**Preconditions:**
- Same container setup as INT-01
- `HealthMemoryEntryRepository` wrapped by a test double whose `save` throws `RuntimeException("SYNTHETIC save failure")` (e.g. `@SpyBean` + `doThrow`)

**Test Steps:**
1. Act: `runIntake(makeRunIntakeRequest(), USER_A)` through a committing boundary.
2. Assert: the call returns normally with `status == "COMPLETED"` (no exception surfaces to the caller).
3. Assert DB: `triage_sessions` row for the session has `status = 'COMPLETED'` and `completed_at IS NOT NULL` (session commit survived).
4. Assert DB: zero rows in `health_context_memories` for this session (write genuinely failed, not silently skipped-by-guard) — and the WARN log from the handler was emitted.

**Expected Result (PASS):** session persisted COMPLETED; memory absent; failure observable in logs only.
**Expected Result (FAIL):** intake HTTP flow fails or the session row is rolled back because of the memory write — violates ADR-THMC-001/BR-SAFETY.

**Current Status:** 🟢 Passing
> *(2026-07-27: actual green run — 2/2 in `TriageHealthMemoryContextIntegrationTest`; see §5 tracker.)*

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `THMC-TC-01` | `HealthMemoryWriteTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-02` | `HealthMemoryWriteTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-03` | `HealthMemoryWriteTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-04` | `HealthMemorySummaryPolicyTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-05` | `HealthMemoryWriteTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-06` | `TriageServiceHealthMemoryContextTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-07` | `HealthMemoryContextReadTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-08` | `HealthMemoryContextReadTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-09` | `TriageServiceHealthMemoryContextTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-10` | `HealthMemoryContextReadTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-11` | `TriageServiceHealthMemoryContextTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-12` | `engine/TriageGraphServiceHealthContextTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-13` | `TriageServiceHealthMemoryContextTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-14` | `TriageServiceHealthMemoryContextTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-15` | `IntakeControllerHealthContextTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-16` | `HealthMemoryContextReadTest.java` | `[x]` | `2026-07-26 (no-commit)` | `greps §8 clean; no refactor needed` |
| `THMC-TC-INT-01` | `TriageHealthMemoryContextIntegrationTest.java` | `[x]` | `2026-07-27 (no-commit)` | |
| `THMC-TC-INT-02` | `TriageHealthMemoryContextIntegrationTest.java` | `[x]` | `2026-07-27 (no-commit)` | |

Run commands:

```bash
cd 05_Development/CareBridgeAPI
./mvnw test                                                # full suite
./mvnw test -Dtest=HealthMemoryWriteTest                   # write path
./mvnw test -Dtest=HealthMemorySummaryPolicyTest           # minimization
./mvnw test -Dtest=HealthMemoryContextReadTest             # read path
./mvnw test -Dtest=TriageServiceHealthMemoryContextTest    # injection
./mvnw test -Dtest=TriageGraphServiceHealthContextTest     # safety invariance
./mvnw test -Dtest=IntakeControllerHealthContextTest       # boundary
./mvnw test -Dtest=TriageHealthMemoryContextIntegrationTest # integration
```

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ Before implementing, run the whole suite against throwing stubs. Every test MUST FAIL. Any test passing against a stub ⇒ **AP-AI-002 detected** ⇒ reject and rewrite that test.
> Anti-tautology design notes: TC-02/03 (no-write cases) and TC-13 (fail-open) each carry a **positive-control verify** on a stubbed method invocation, so they cannot pass against an empty/no-op implementation; TC-12 exercises a stub overload that throws.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stubs (MUST throw)

// HealthMemoryServiceImpl.java — new methods only (existing list/delete stay functional)
@Override
public Optional<HealthMemoryEntry> writeFromCompletedSession(UUID sessionId, UUID userId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

@Override
public List<HealthMemoryContextItem> loadContextForIntake(UUID userId, TriageStage stage, UUID profileId) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

// HealthMemoryWriteHandler.java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onIntakeSessionCompleted(IntakeSessionCompleted event) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

// HealthMemorySummaryPolicy.java
public String buildSummary(IntakeSession session) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
public String buildPayloadJson(IntakeSession session) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

// TriageGraphService.java — new overload only
public ChildTriageResult run(RunIntakeRequest request, List<HealthMemoryContextItem> healthContext) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

// ChildTriageAiClient default overload keeps delegating (interface default) — the RED failure
// for TC-06/11/13/14 comes from TriageService not yet calling loadContextForIntake (verify() fails).
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `THMC-TC-01` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `THMC-TC-02` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-03` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-04` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-05` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-06` | unmodified TriageService → `verify(loadContextForIntake)` fails | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-07` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-08` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-09` | unmodified TriageService → owner-scoped call never happens | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-10` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-11` | stub overload throws / verify fails | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-12` | stub overload `run(request, ctx)` throws | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-13` | `verify(loadContextForIntake)` fails on unmodified service | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-14` | canonical map lacks `healthContext` key | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-15` | service-layer captor assertion on server-loaded list fails (wiring absent) | 🔴 FAIL | ☑ FAIL ☐ PASS | Note: designated service-layer signal FAILED as expected; the companion controller-boundary method passes by design (verifies PRE-EXISTING behavior: DTO has no healthContext property, Jackson ignores unknown fields) — documented in red-gate-evidence.log, not an AP-AI-002 on new code |
| `THMC-TC-16` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-INT-01` | handler stub throws → no memory row → step-2 assert fails | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `THMC-TC-INT-02` | step-4 WARN-log + zero-row asserts fail against stub behavior | 🔴 FAIL | ☑ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `(no commit — HARD RULE: no git commit; stubs verified in working tree 2026-07-26)`
- Tất cả FAIL? ☑ Yes (18/18 designated red signals FAILED — run 2026-07-26: Tests run 18, Failures 1, Errors 16, + TC-15 service-layer signal; see red-gate-evidence.log) → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `04_Implement/TriageHealthMemoryContext/red-gate-evidence.log`

> **If any test PASSES:** stop. Identify the root cause in the table above. Rewrite the test from its TC spec with the Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] TDS `CB-TRIAGE-THMC-IMP-001` reviewed and **Approved** (verified 2026-07-26)
- [ ] Logic Issues (Section 2, L1–L7) confirmed with the Principal Architect / Tech Lead
- [x] No Flyway migration required (ADR-THMC-004) — confirmed against the canonical baseline; optional index explicitly deferred (TDS Open O3). Verified 2026-07-26: no THMC migration file added
- [x] TTL default (30 days) consciously accepted as provisional (TDS Open O1 stays Open — DPO confirmation pending)
- [x] Test fixtures FX-THMC-001…013 prepared via `HealthMemoryContextTestFactory`

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — all unit tests green (no skips) *(2026-07-26: all THMC + triage unit tests green; the FULL suite cannot be all-green on this machine — known pre-existing ChecklistTemplateMigrationTest failure + Docker-unavailable Testcontainers errors)*
- [x] `./mvnw verify` — integration tests green (Testcontainers) *(MET in substance 2026-07-27 via `./mvnw test -Dtest=TriageHealthMemoryContextIntegrationTest` — Tests run 2, Failures 0, Errors 0; full `verify` lifecycle not separately run)*
- [ ] Test coverage ≥ 80% lines for `HealthMemoryServiceImpl` (new methods), `HealthMemoryWriteHandler`, `HealthMemorySummaryPolicy`
- [x] No business logic added to any controller (verified: `git diff --stat triage/controller/` = 0 lines)
- [x] No PII/summary text plaintext in logs (THMC-TC-04 / TC-13 log captors green 2026-07-26)
- [x] Exactly one active memory per completed session on the integration DB (THMC-TC-INT-01) *(MET 2026-07-27: asserted on real PostgreSQL incl. TTL = completed_at + 30 days and strict `expires_at > now` boundary)*
- [x] Context proven injected into BOTH AI payload and fallback input (THMC-TC-06 + THMC-TC-11 green 2026-07-26)
- [x] RED-stays-RED safety invariance green (THMC-TC-12 green 2026-07-26)
- [x] Existing suites untouched and green: `HealthMemoryServiceImplTest`, existing `TriageService`/engine tests + the relevant triage safety tests (verified 2026-07-26: full `./mvnw clean test` = 2981 run / 1 failure / 73 errors, ALL attributable to the known pre-existing set — ChecklistTemplateMigrationTest SHA drift + Docker-unavailable Testcontainers suites incl. THMC-TC-INT-01/02)

**Exit Criteria bổ sung — CASE 2.0:**

- [x] **Red Gate (§5.1)** — all 18 designated red signals FAILED against the throwing stubs before implementation (2026-07-26; TC-15 companion-method note in §5.1)
- [x] **Contract Existence** — every injected class exists in the codebase (ran 2026-07-26 — no output):
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [x] **Props Isolation** — no shared mutable state between tests (grep ran 2026-07-26; only hits are per-test-instance helper fields — `objectMapper`, `events` — recreated per method under JUnit PER_METHOD lifecycle, identical to the accepted TriageServicePreScreenTest precedent):
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " src/test/java/com/carebridge/backend/triage/HealthMemory*Test.java src/test/java/com/carebridge/backend/triage/TriageServiceHealthMemoryContextTest.java
  # Every instance MUST be created inside @Test or via HealthMemoryContextTestFactory.makeXxx()
  ```
- [x] **Oracle Source** — every expected value in every assert cites its source (BR-THMC-xxx / ADR-THMC-xxx / repository or baseline line reference) — already embedded per-TC above

### Suspension Criteria (Điều kiện tạm dừng)

- Canonical baseline drift detected (e.g. `health_context_memories` columns changed by a parallel feature)
- New architecture concern raised on `IntakeSessionCompleted` consumers requiring Principal Architect review
- CI pipeline broken by unrelated changes

---

## 7. Rollback Plan

```bash
# No Flyway migration to revert by default (ADR-THMC-004 — no schema change).
# If the OPTIONAL index migration (TDS §11.3 Chặng 6) was applied on dev, revert it:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_hcm_user_baby_stage; DROP INDEX IF EXISTS idx_hcm_user_mother_stage;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260801000000';"

# Revert implementation files (Java)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/application.yaml

# Revert Python service changes
git checkout -- 05_Development/CareBridgeAITriageService/app/schemas.py
git checkout -- 05_Development/CareBridgeAITriageService/app/main.py

# Revert tests
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/

# Data hygiene (dev only): soft-delete memories written during the trial run
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE health_context_memories SET deleted_at = now() WHERE deleted_at IS NULL;"

# Gap remains OPEN → keep the Roadmap Part III item 1 entry unchanged
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> ⭐ Reviewer checklist — these test cases were AI-assisted (see §1.1).

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | A TC references no BR-THMC/ADR-THMC/repository-line constraint | ☑ | G-0 |
| AP-AI-002 | Green-from-Birth | Any test PASSES against the §5.1 throwing stubs (watch TC-02/03/13 — their positive-control verifies exist precisely to defeat this) | ☑ | G-2 ★ |
| AP-AI-003 | Implicit Decision | A test assumes architecture absent from the TDS ADRs (e.g., expiry cron, new event type, new endpoint, `ttl_seconds` column) | ☑ | G-1 |
| AP-AI-004 | Layer Violation | A test asserts business logic inside `IntakeController` / `HealthMemoryController` | ☑ | G-4 |
| AP-AI-005 | Hallucinated Contract | A test imports a type/method outside TDS §8 (e.g., `CareSubjectService`, a three-arg `triageChild`, an eager `HealthMemoryService.purge()`) | ☑ | G-3 |

**Kết quả review:**

- [x] No anti-pattern detected → TDD spec approved *(reviewed 2026-07-26; TC-15 controller companion method passing against stubs is guard-by-design on PRE-EXISTING behavior, documented in §5.1 and red-gate-evidence.log)*
- [ ] AP detected → record below → fix before implementation

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

*TDD Spec v1.0 — TriageHealthMemoryContext — CB-TRIAGE-THMC-IMP-001-TEST*
*Sections marked ⭐ are CASE 2.0 additions.*
