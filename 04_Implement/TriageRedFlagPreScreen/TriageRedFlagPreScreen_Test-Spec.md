# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test Specification — TriageRedFlagPreScreen: Consume `red_flag_rules` on the Intake Path

**Document ID:** `CB-TRIAGE-TEST-003`
**Version:** `1.0`
**Date:** `2026-07-26`
**Status:** `Implemented — 2026-07-26`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(symptom free text is processed in memory; no new storage — see TDS §1)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/TriageRedFlagPreScreen/TriageRedFlagPreScreen_TDS.md` (`CB-TRIAGE-IMP-003`) — Technical Design Specification (primary oracle)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/B20260724111500__canonical_70_table_baseline.sql` (lines 1437–1450) — canonical persistence oracle for `red_flag_rules` (severity CHECK GREEN/YELLOW/RED; action CHECK BLOCK/WARN/ESCALATE)
- `04_Implement/AITriageCompletion/AITriage_Assessment_Roadmap.md` Part III item 2 — requirement oracle
- `04_Implement/UC110_ManageAIRedFlagRules/UC110_ManageAIRedFlagRules_TDS.md` (`CB-MOD-IMP-005`) — rule-table semantics, terminology
- `CLAUDE.md` — BR-SAFETY ("AI provides guidance only; never diagnose, prescribe, or delay emergency routing")
- Code under test/modification: `triage/service/impl/TriageService.java`, `triage/policy/TriageRedFlagPreScreenPolicy.java` (NEW), `triage/repository/RedFlagRuleRepository.java`, `triage/engine/SymptomNormalizer.java` (normalization oracle, read-only), `triage/policy/TriageRedFlagPolicy.java` (must remain untouched)

> **TDD convention:** This document describes test cases BEFORE any production code is written.
> Mandatory order: write test (`.java`) → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Never mark a test ✅ unless `./mvnw test` is green.
> No real PII in test data — SYNTHETIC data only.
>
> **Test commands:** `./mvnw test` (full backend suite) or narrowest scope
> `./mvnw test -Dtest=TriageRedFlagPreScreenPolicyTest`,
> `./mvnw test -Dtest=TriageServicePreScreenTest`,
> `./mvnw test -Dtest=TriageRedFlagPreScreenIntegrationTest`
> (run from `05_Development/CareBridgeAPI`).

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** never delete old information.

| Date       | Performed by | Change description                                                        |
| ---------- | ------------ | -------------------------------------------------------------------------- |
| 2026-07-26 | AI Agent     | Initial creation — TDD spec for TriageRedFlagPreScreen (Draft, pre-implementation) |
| 2026-07-26 | AI Agent — Amelia (Dev Agent) | Phase 3: Implementation — Red Gate 20/21 FAIL (`TRFP-TC-SEC-001` = documented pre-existing-subject pass); GREEN 21/21 PASS (`./mvnw test -Dtest='TriageRedFlagPreScreenPolicyTest,TriageServicePreScreenTest,TriageRedFlagPreScreenSecurityTest,TriageRedFlagPreScreenIntegrationTest'` → Tests run: 21, Failures: 0, Errors: 0). O1 resolved POSITIVE: `IntakeStartRequest`/`IntakeContinueRequest` in `app/schemas.py` have no `extra="forbid"` (pydantic default ignores extras) → `preScreenFlags` injected into conversation canonical requests; TRFP-TC-017 asserts the key IS present. Deviations: `PreScreenResult` static factory named `degradedNoMatch()` (Java forbids a static `degraded()` beside the record accessor `degraded()`); `normalize()` additionally maps 'đ'→'d' (NFD does not decompose U+0111 — precedent `TriageService.normalizeAnswerToken`, required for BR-SAFETY-TRFP-004); policy records the degraded metric with fixed flow label `"screen"` (screen() has no flow context); INT-001 mocks `IEmergencyService` + `ILifecycleSafetyOutcomeProjector` and seeds via `saveAndFlush` (H2 merged-entity schema artifact for `triage_sessions`, documented in the test) |

---

## TABLE OF CONTENTS

1. [Module Information](#1-module-information)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Module Information

| Field                      | Value                                                                          |
| -------------------------- | ------------------------------------------------------------------------------- |
| **Feature / Gap ID**       | `TriageRedFlagPreScreen` (roadmap Part III item 2 — no dedicated UC number)     |
| **Module**                 | `Triage Red-Flag Pre-Screen — triage bounded context`                          |
| **Spec gốc**               | `CB-TRIAGE-IMP-003` (TDS, this folder)                                          |
| **Priority**               | 🔴 P0 *(BR-SAFETY-critical: TRFP-TC-001/009/011/012/013/016)* / 🟠 P1 (rest)   |
| **Sprint**                 | `Open — not yet scheduled`                                                      |
| **Milestone**              | `Open`                                                                          |
| **Data Classification**    | `Sensitive-PII` (symptom free text in memory; no new persistence)               |
| **Compliance Scope**       | `BR-SAFETY (CLAUDE.md)`; PDPA scope inherited from UC-60                        |
| **Upstream Dependencies**  | `red_flag_rules` + `RedFlagRuleRepository` (UC-110), `TriageService` intake flows (UC-60) |
| **Downstream Consumers**   | Emergency escalation chain (`EmergencyEscalationTriggered` → `EmergencyEscalationHandler` → `safety_events`/`safety_event_actions`) |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                       |
| ------------------------ | -------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                         |
| **Constraint Source**    | `CB-TRIAGE-IMP-003 §17.1` (C1–C9)                                                             |
| **Constraints Injected** | C1 (pre-screen before every AI call; no AI on short-circuit), C2 (existing completion path only), C3 (degrade, never throw), C4 (hardcoded floors untouched), C5 (RED+ESCALATE/BLOCK short-circuit; WARN/YELLOW annotate; GREEN ignored), C6 (NFD + `\p{M}+` normalization, substring contains), C7 (read-through, no cache), C8 (canonical RED contract / envelope key allow-list), C9 (no new endpoints/DTOs/migrations) |
| **Model**                | `Claude (Fable) — spec-authoring agent`                                                       |
| **Trust Level**          | `T1 → T2 (pending Red Gate, §5.1)`                                                            |

---

## 2. Logic Issues Resolved

> Persistence disputes are resolved against `B20260724111500__canonical_70_table_baseline.sql` and approved migrations; ERD/older docs are supporting evidence only.
> Tests encode the **corrected** behavior below, not the original prose.

| # | Original spec (wrong / incomplete) | Reality (schema / policy / code) | Fix applied in tests |
|---|------------------------------------|----------------------------------|----------------------|
| L1 | Roadmap Part III.2 says pre-screen goes "qua `TriageRedFlagPolicy`" | `TriageRedFlagPolicy` (verified `:31-52`) is diacritic-**sensitive** (`toLowerCase().contains()` only), ignores the `action` column (`findBySeverityAndActiveTrue(RED)` at `:45`), returns a bare boolean, and is live for 3 other consumers (RAG filter/policy, community) | Tests target the NEW `TriageRedFlagPreScreenPolicy` (TDS ADR-001). A regression guard asserts `TriageRedFlagPolicy.java` source is untouched (Exit Criteria §6) — no TC mocks or modifies it |
| L2 | DDL gives `action IN (BLOCK, WARN, ESCALATE)` no runtime meaning; naive reading of "BLOCK" = reject the request | BR-SAFETY forbids delaying/suppressing emergency routing — rejecting a symptom intake because it contains an emergency keyword is the unsafe inversion | Tests encode TDS ADR-002: `RED+BLOCK` short-circuits to RED exactly like `RED+ESCALATE` (`TRFP-TC-007`); no test anywhere expects a rejected/blocked intake |
| L3 | UC-110 ADR-003 declared GREEN/YELLOW/BLOCK/WARN globally inert at runtime | This feature is the first intake-path consumer; TDS ADR-002 partially supersedes UC-110 ADR-003 **for the intake path only** (WARN/YELLOW → annotate) | `TRFP-TC-005/006` assert ANNOTATE_ONLY (not NO_MATCH, not short-circuit); `TRFP-TC-008` asserts GREEN stays fully inert |
| L4 | Template suggests Testcontainers for integration tests | No Testcontainers harness exists anywhere in this codebase (verified finding recorded in UC-110 Test-Spec changelog 2026-07-02) | `TRFP-TC-INT-001` is `@SpringBootTest` + H2 test datasource, per project convention |
| L5 | A naive repository method name would be `findByIsActiveTrue()` | Entity boolean property is `active` (getter `isActive()`); Spring Data rejects "IsActive" as a path segment — verified UC-110 implementation finding; existing method is `findBySeverityAndActiveTrue` (`RedFlagRuleRepository.java:20`) | Tests reference the new method as `findByActiveTrue()` (TDS §8.2) |
| L6 | One might assert the pre-screen RED result equals the Java-fallback RED result byte-for-byte | The only *enforced* RED contract is `hasCanonicalRedContract` (`TriageService.java:1128-1132`): `emergencyActionRequired=true`, `recommendationCode="SEEK_EMERGENCY_CARE"`, non-empty `matchedRules`; plus `applyCanonicalSnapshot` sets `emergency=true` for RED (`:743`) | Tests assert exactly those contract fields + session state, not incidental formatting |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
TriageRedFlagPreScreen covers these layers:
├── Policy / Domain rule (TriageRedFlagPreScreenPolicy — mock RedFlagRuleRepository with Mockito)
├── Service workflow (TriageService — mock ChildTriageAiClient, real/mocked policy, mock event capture
│   via recording ApplicationEventPublisher — same technique as existing TriageServiceTest)
├── Security (existing @PreAuthorize on IntakeController — @WebMvcTest / MockMvc, unchanged config)
└── Integration (@SpringBootTest + H2: repository → policy → service → event listener chain)
Out of test scope: Python service (unchanged), TriageRedFlagPolicy behavior (unchanged, guarded by
source-diff check in Exit Criteria), UC-110 admin CRUD (already covered by RFR-TC-*).
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `AITriage_Assessment_Roadmap.md` Part III item 2 | Pre-screen before Python; RED match → immediate emergency short-circuit independent of AI; hardcoded rules kept |
| `CLAUDE.md BR-SAFETY` | Pre-screen must never delay/fail intake (degradation tests), never block |
| `CB-TRIAGE-IMP-003 §3 ADR-001` | New policy component; short-circuit via existing completion path (event parity) |
| `CB-TRIAGE-IMP-003 §3 ADR-002` | Action/severity classification table (short-circuit set = RED × {ESCALATE, BLOCK} × active) |
| `CB-TRIAGE-IMP-003 §3 ADR-003` | Degrade-to-no-op on lookup failure; never throw |
| `CB-TRIAGE-IMP-003 §3 ADR-004` | Read-through freshness (no cache staleness) |
| `CB-TRIAGE-IMP-003 §3 ADR-005` | Diacritic/case-insensitive substring matching, both directions |
| `B20260724111500__canonical_70_table_baseline.sql:1437-1450` | Valid severity/action value domain for fixtures; `is_active` column semantics |
| `TriageService.java:768-777, :743, :1128-1132, :67-80` | Event publication, emergency flag, canonical RED contract, envelope key allow-list (expected-value oracles) |
| `IntakeController.java:36-89` | MOTHER-only RBAC on all intake endpoints (unchanged) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|--------------|----------------|---------------|-----------|
| TC-COND-001 | Active RED+ESCALATE rule match → ESCALATE_RED (happy path) | `TriageRedFlagPreScreenPolicy.screen()` | `TRFP-TC-001` **CRITICAL** |
| TC-COND-002 | Diacritics: accented keyword matches unaccented input | `screen()` normalization | `TRFP-TC-002` |
| TC-COND-003 | Case-insensitivity + reverse diacritics (accented/uppercase input, unaccented keyword) | `screen()` normalization | `TRFP-TC-003` |
| TC-COND-004 | Inactive rule never matches | `screen()` + `findByActiveTrue()` | `TRFP-TC-004` |
| TC-COND-005 | YELLOW severity match → ANNOTATE_ONLY, never short-circuit | `screen()` classification | `TRFP-TC-005` |
| TC-COND-006 | RED+WARN match → ANNOTATE_ONLY (WARN vs ESCALATE action split) | `screen()` classification | `TRFP-TC-006` |
| TC-COND-007 | RED+BLOCK match → ESCALATE_RED (BLOCK ≡ ESCALATE on intake; never rejection) | `screen()` classification | `TRFP-TC-007` |
| TC-COND-008 | GREEN severity match → NO_MATCH (inert) | `screen()` classification | `TRFP-TC-008` |
| TC-COND-009 | Repository throws → NO_MATCH + degraded=true, no exception propagates | `screen()` degradation | `TRFP-TC-009` **CRITICAL** |
| TC-COND-010 | Null/blank/whitespace-only input → NO_MATCH without repository call | `screen()` boundary | `TRFP-TC-010` |
| TC-COND-011 | One-shot short-circuit: AI never called; session COMPLETED/RED/emergency=true; canonical RED contract | `TriageService.runIntake` | `TRFP-TC-011` **CRITICAL** |
| TC-COND-012 | One-shot short-circuit publishes exactly one `EmergencyEscalationTriggered` + one `IntakeSessionCompleted` | `TriageService.runIntake` → `publishCompletionEvents` | `TRFP-TC-012` **CRITICAL** |
| TC-COND-013 | Conversation-start short-circuit: TRIAGE_COMPLETE envelope with RED triageResult; envelope survives `ensureSafeEnvelope` invariants; events fired | `TriageService.startConversation` | `TRFP-TC-013` **CRITICAL** |
| TC-COND-014 | Conversation-continue short-circuit on newly matching answers | `TriageService.continueConversation` | `TRFP-TC-014` |
| TC-COND-015 | No match → AI called with unchanged request; response passthrough identical to pre-feature behavior | `TriageService` all flows (repr.: one-shot) | `TRFP-TC-015` |
| TC-COND-016 | Degraded pre-screen → AI still called; if AI also fails, hardcoded Java fallback still runs (defense-in-depth chain intact) | `TriageService.runIntake` | `TRFP-TC-016` **CRITICAL** |
| TC-COND-017 | ANNOTATE_ONLY → AI called; no short-circuit; no RED forced; annotation recorded (metric; `preScreenFlags` only if Open item O1 resolves positive) | `TriageService.startConversation` | `TRFP-TC-017` |
| TC-COND-018 | Idempotent replay (same `clientRequestId`) after a short-circuited conversation start returns stored envelope, publishes NO duplicate escalation events | `TriageService.startConversation` idempotency + concurrency arbitration | `TRFP-TC-018` |
| TC-COND-019 | Freshness / cache-staleness: rule set change between two `screen()` calls is visible on the second call (repository queried per invocation — no memoization) | `TriageRedFlagPreScreenPolicy` (ADR-004) | `TRFP-TC-019` |
| TC-COND-020 | Non-MOTHER roles rejected on intake endpoints (RBAC unchanged by this feature) | `IntakeController` `@PreAuthorize` | `TRFP-TC-SEC-001` |
| TC-COND-021 | End-to-end: DB-seeded admin RED rule → one-shot intake with accentless matching text → RED/COMPLETED/emergency session + escalation listener effect, AI stub never hit | Full Spring context (H2) | `TRFP-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|-------------------------|-----------|-----------|
| Equivalence Partitioning | `severity` × `action` × `is_active` (3×3×2 domain from DDL CHECK constraints) | Reduced to the ADR-002 classification classes: {RED×(ESCALATE,BLOCK)×active}, {RED×WARN, YELLOW×any}×active, {GREEN×any}, {any×any×inactive} |
| Boundary Value Analysis | Input text: null / "" / blank; keyword at word boundaries inside longer text (substring semantics) | ADR-005 substring `contains`; blank input must not hit the DB |
| Decision Table Testing | ADR-002 outcome table (6 rows) | Each row maps to ≥ 1 TC (001, 004, 005, 006, 007, 008) |
| Error Guessing | Repository exception injection (`DataAccessResourceFailureException`); AI client exception after degraded pre-screen | Directly probes the two BR-SAFETY failure paths (TC-009, TC-016) |
| State Transition Testing | Session PROCESSING → COMPLETED(RED) via short-circuit; replay of already-completed conversation | Confirms reuse of the existing state machine, no new states (TDS §6.4) |
| Syntax/Localization Testing | Vietnamese diacritics both directions, uppercase | ADR-005 (`\p{M}+` strip, Locale.ROOT lowercase) |

### TDS-05 — Test Data Requirements

> All fixtures SYNTHETIC. Keyword choices deliberately avoid every hardcoded list (`TriageRedFlagPolicy.FLOOR_KEYWORDS`, `SymptomNormalizer.KEYWORDS`, `PediatricRiskRules` phrases, `app/risk_rules.py` constants) so that a pre-screen match can never be confused with a hardcoded-engine match. Verified non-membership: "ngã đập đầu", "sặc sữa liên tục", "sốt kéo dài", "phát ban toàn thân"*, "uống nhầm hóa chất", "hắt hơi" do not appear in those lists (*"phát ban" alone appears in `SymptomNormalizer` as `rash` synonym — FX-004 is used only in policy-level unit tests where the normalizer is not involved).

| Fixture ID | Type | Value / Logic | Purpose |
|-----------|------|---------------|---------|
| `FX-001` | Rule (mock/seed) | `RedFlagRule{keyword:"ngã đập đầu", severity:RED, action:ESCALATE, active:true, systemDefault:false}` | Short-circuit happy path |
| `FX-002` | Rule | `RedFlagRule{keyword:"sặc sữa liên tục", severity:RED, action:ESCALATE, active:false}` | Inactive-rule condition |
| `FX-003` | Rule | `RedFlagRule{keyword:"sốt kéo dài", severity:YELLOW, action:WARN, active:true}` | YELLOW annotation condition |
| `FX-004` | Rule | `RedFlagRule{keyword:"phát ban toàn thân", severity:RED, action:WARN, active:true}` | RED+WARN action split |
| `FX-005` | Rule | `RedFlagRule{keyword:"uống nhầm hóa chất", severity:RED, action:BLOCK, active:true}` | RED+BLOCK ≡ escalate |
| `FX-006` | Rule | `RedFlagRule{keyword:"hắt hơi", severity:GREEN, action:WARN, active:true}` | GREEN inertness |
| `FX-007` | Mock behavior | `redFlagRuleRepository.findByActiveTrue()` throws `new DataAccessResourceFailureException("db down")` | Degradation (ADR-003) |
| `FX-008` | Input text | `"bé bị nga dap dau xuống sàn"` (keyword accent-stripped inside otherwise accented sentence) | Diacritics direction 1 (rule accented, text not) |
| `FX-009` | Input text | `"BÉ BỊ NGÃ ĐẬP ĐẦU"` (uppercase, fully accented) | Case-insensitivity + direction 2 |
| `FX-010` | Input text | `"bé hơi quấy khóc nhẹ"` | Neutral text — no rule (and no hardcoded phrase) matches |
| `FX-011` | Intake request | `RunIntakeRequest{stage:INFANT, babyProfileId:<fixed UUID>, symptoms:FX-008, temperatureC:null, seizure:null}` | One-shot service tests (no numeric-rule interference) |
| `FX-012` | Conversation request | `StartIntakeConversationRequest{initialText:FX-008, clientRequestId:"trfp-cr-001", stage:INFANT, babyProfileId:<fixed UUID>}` | Conversation start + idempotent replay |
| `FX-013` | AI stub | `childTriageAiClient` mock — `triageChild`/`startIntake`/`continueIntake` return a minimal valid GREEN/ASK_MORE payload; interactions recorded | AI-never-called and passthrough assertions |
| `FX-014` | JWT context | `role=MOTHER` principal (userId fixed UUID) / `role=EXPERT` principal | SEC test |
| `FX-015` | Event capture | Recording `ApplicationEventPublisher` (list-appending stub — same technique as existing `TriageServiceTest`) | Event parity assertions |

---

## 4. Test Case Specification

> **TC ID format:** `TRFP-TC-[NNN]` (Triage Red-Flag Pre-screen)
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — MANDATORY)

> ⭐ Every test builds fresh instances through `makeXxx()` factories. No shared mutable state between tests (anti AP-AI-002). All test data below derives from these factories only.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// File: src/test/java/com/carebridge/backend/triage/TriagePreScreenTestFactory.java
// Each @Test calls makeXxx() — never reuses instances across tests.
// ═══════════════════════════════════════════════════════════
class TriagePreScreenTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID BABY_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    // FX-001 — RED + ESCALATE + active
    static RedFlagRule makeRedEscalateRule() {
        RedFlagRule rule = new RedFlagRule();
        rule.setId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        rule.setKeyword("ngã đập đầu");
        rule.setSeverity(RedFlagSeverity.RED);
        rule.setAction(RedFlagAction.ESCALATE);
        rule.setActive(true);
        rule.setSystemDefault(false);
        return rule;
    }

    // Generic override-based factory — FX-002..FX-006 are expressed as overrides
    static RedFlagRule makeRule(Consumer<RedFlagRule> overrides) {
        RedFlagRule rule = makeRedEscalateRule();
        overrides.accept(rule);
        return rule;
    }
    // e.g. FX-005: makeRule(r -> { r.setKeyword("uống nhầm hóa chất"); r.setAction(RedFlagAction.BLOCK); })

    // FX-011 — one-shot request whose only riskable content is the pre-screen keyword
    static RunIntakeRequest makeOneShotRequest() {
        RunIntakeRequest request = new RunIntakeRequest();
        request.setStage(TriageStage.INFANT);
        request.setBabyProfileId(BABY_PROFILE_ID);
        request.setSymptoms("bé bị nga dap dau xuống sàn");   // FX-008
        return request;
    }

    // Overload to override specific fields — never share mutated instances across tests
    static RunIntakeRequest makeOneShotRequest(Consumer<RunIntakeRequest> overrides) {
        RunIntakeRequest request = makeOneShotRequest();
        overrides.accept(request);
        return request;
    }

    static RunIntakeRequest makeNeutralOneShotRequest() {
        return makeOneShotRequest(r -> r.setSymptoms("bé hơi quấy khóc nhẹ")); // FX-010
    }

    // FX-012
    static StartIntakeConversationRequest makeStartRequest() {
        StartIntakeConversationRequest request = new StartIntakeConversationRequest();
        request.setInitialText("bé bị nga dap dau xuống sàn"); // FX-008
        request.setClientRequestId("trfp-cr-001");
        request.setStage(TriageStage.INFANT);
        request.setBabyProfileId(BABY_PROFILE_ID);
        return request;
    }

    // FX-013 — minimal valid non-RED AI payloads (recorded mock)
    static String makeAiGreenOneShotJson() {
        return """
               {"status":"COMPLETED","riskLevel":"GREEN","riskColor":"#22C55E",
                "emergencyActionRequired":false,"recommendationCode":"MONITOR_AT_HOME",
                "matchedRules":[],"redFlags":[],"disclaimer":"synthetic"}""";
    }

    static String makeAiAskMoreEnvelopeJson() {
        return """
               {"status":"ASK_MORE","mergedIntake":{},"round":1,
                "questions":[{"questionKey":"duration","text":"Bao lâu rồi?",
                              "answerType":"TEXT","options":[]}]}""";
    }

    // FX-015 — event recorder (fresh per test)
    static List<Object> makeEventSink() { return new ArrayList<>(); }
}
```

---

### TRFP-TC-001 — screen(): active RED+ESCALATE rule match returns ESCALATE_RED

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY (CLAUDE.md)`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-002` (classification table row 1: RED+ESCALATE+active → ESCALATE_RED); DDL value domain `B20260724111500...sql:1448-1449`

**Preconditions:**
- `RedFlagRuleRepository.findByActiveTrue()` mocked → `List.of(makeRedEscalateRule())` (FX-001)

**Test Steps:**
1. Arrange: policy with mocked repository; `String text = "bé bị ngã đập đầu xuống sàn"` (accented, exact keyword present).
2. Act: `PreScreenResult result = policy.screen(text)`.
3. Assert: `result.outcome() == PreScreenOutcome.ESCALATE_RED`; `result.matchedKeywords()` contains `"ngã đập đầu"`; `result.matchedRuleIds()` contains FX-001 id; `result.degraded() == false`; repository queried exactly once.

**Expected Result (PASS):** outcome `ESCALATE_RED` with matched keyword/ruleId populated, degraded=false.
**Expected Result (FAIL — bug signal):** NO_MATCH (rule table ignored — the exact production gap this feature fixes) or exception.

**Current Status:** 🟢 Passing
**Implementation Note:** classification per ADR-002 must run on the normalized forms; the returned `matchedKeywords` carry the original keyword text.

---

### TRFP-TC-002 — screen(): accented keyword matches unaccented input (diacritics direction 1)

**Severity:** `HIGH`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-005` (normalize both keyword and text — NFD + `\p{M}+` strip per `SymptomNormalizer.java:83-86`); `BR-SAFETY-TRFP-004`

**Preconditions:** repository mocked → `List.of(makeRedEscalateRule())` (keyword `"ngã đập đầu"`, accented).

**Test Steps:**
1. Arrange: input FX-008 `"bé bị nga dap dau xuống sàn"` (keyword typed WITHOUT diacritics).
2. Act: `policy.screen(text)`.
3. Assert: outcome `ESCALATE_RED`.

**Expected Result (PASS):** match despite missing diacritics in input.
**Expected Result (FAIL):** NO_MATCH — normalization applied to only one side (the `TriageRedFlagPolicy` bug class this feature must not repeat).

**Current Status:** 🟢 Passing

---

### TRFP-TC-003 — screen(): uppercase + fully accented input matches (case-insensitivity, direction 2)

**Severity:** `MEDIUM`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-005` (lowercase `Locale.ROOT` before NFD); `BR-SAFETY-TRFP-004`

**Preconditions:** repository mocked → rule via `makeRule(r -> r.setKeyword("nga dap dau"))` (keyword stored WITHOUT diacritics — reverse direction).

**Test Steps:**
1. Arrange: input FX-009 `"BÉ BỊ NGÃ ĐẬP ĐẦU"` (uppercase, accented).
2. Act: `policy.screen(text)`.
3. Assert: outcome `ESCALATE_RED`.

**Expected Result (PASS):** match — both sides normalized to the same canonical form.
**Expected Result (FAIL):** NO_MATCH (case- or direction-sensitive matching).

**Current Status:** 🟢 Passing

---

### TRFP-TC-004 — screen(): inactive rule never matches

**Severity:** `HIGH`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)` + repository contract
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-002` (row: `is_active=false` → ignored); baseline DDL `is_active boolean DEFAULT true NOT NULL` (`:1442`)

**Preconditions:** repository mocked: `findByActiveTrue()` → `List.of()` *(the derived query itself excludes inactive rows — the mock encodes the repository contract; FX-002 exists in the fixture set but is filtered by the query)*.

**Test Steps:**
1. Arrange: input `"bé bị sặc sữa liên tục"` (matches FX-002's keyword, but FX-002 is inactive).
2. Act: `policy.screen(text)`.
3. Assert: outcome `NO_MATCH`; `matchedRuleIds()` empty; degraded=false.

**Expected Result (PASS):** NO_MATCH.
**Expected Result (FAIL):** ESCALATE_RED — policy loaded rules via `findAll()` or ignored the active filter.

**Current Status:** 🟢 Passing
**Implementation Note:** the companion integration test `TRFP-TC-INT-001` covers the real SQL filtering of `is_active=false` through H2; this unit TC pins the policy's choice of the active-only query method.

---

### TRFP-TC-005 — screen(): YELLOW match returns ANNOTATE_ONLY (never short-circuits)

**Severity:** `HIGH`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-002` (row: YELLOW any action → ANNOTATE_ONLY); `BR-SAFETY-TRFP-005`

**Preconditions:** repository mocked → `List.of(makeRule(r -> { r.setKeyword("sốt kéo dài"); r.setSeverity(RedFlagSeverity.YELLOW); r.setAction(RedFlagAction.WARN); }))` (FX-003).

**Test Steps:**
1. Arrange: input `"bé sốt kéo dài ba ngày"`.
2. Act: `policy.screen(text)`.
3. Assert: outcome `ANNOTATE_ONLY`; matchedKeywords contains `"sốt kéo dài"`.

**Expected Result (PASS):** ANNOTATE_ONLY.
**Expected Result (FAIL):** ESCALATE_RED (over-escalation — YELLOW must not trigger emergency) or NO_MATCH (annotation tier lost).

**Current Status:** 🟢 Passing

---

### TRFP-TC-006 — screen(): RED+WARN returns ANNOTATE_ONLY (action split WARN vs ESCALATE)

**Severity:** `HIGH`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-002` (row: RED+WARN → ANNOTATE_ONLY — recorded decision; deliberately different from RAG-side behavior, see TDS ADR-002 trade-off)

**Preconditions:** repository mocked → `List.of(makeRule(r -> { r.setKeyword("phát ban toàn thân"); r.setAction(RedFlagAction.WARN); }))` (FX-004, severity stays RED).

**Test Steps:**
1. Arrange: input `"bé bị phát ban toàn thân"`.
2. Act: `policy.screen(text)`.
3. Assert: outcome `ANNOTATE_ONLY` (NOT ESCALATE_RED).

**Expected Result (PASS):** ANNOTATE_ONLY.
**Expected Result (FAIL):** ESCALATE_RED — action column ignored (the `TriageRedFlagPolicy` semantics leaking into the pre-screen).

**Current Status:** 🟢 Passing

---

### TRFP-TC-007 — screen(): RED+BLOCK returns ESCALATE_RED (BLOCK ≡ escalate on intake; never rejection)

**Severity:** `HIGH`
**Legal:** `BR-SAFETY (CLAUDE.md)` — blocking an intake would delay emergency routing
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-002` (row: RED+BLOCK → ESCALATE_RED; Option A "literal block" explicitly rejected)

**Preconditions:** repository mocked → `List.of(makeRule(r -> { r.setKeyword("uống nhầm hóa chất"); r.setAction(RedFlagAction.BLOCK); }))` (FX-005).

**Test Steps:**
1. Arrange: input `"bé uống nhầm hóa chất tẩy rửa"`.
2. Act: `policy.screen(text)`.
3. Assert: outcome `ESCALATE_RED`; no exception of any kind.

**Expected Result (PASS):** ESCALATE_RED.
**Expected Result (FAIL):** any thrown exception or a "blocked/rejected" style outcome — BR-SAFETY violation.

**Current Status:** 🟢 Passing

---

### TRFP-TC-008 — screen(): GREEN match is inert (NO_MATCH)

**Severity:** `MEDIUM`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-002` (row: GREEN → ignored, consistent with UC-110 ADR-003)

**Preconditions:** repository mocked → `List.of(makeRule(r -> { r.setKeyword("hắt hơi"); r.setSeverity(RedFlagSeverity.GREEN); r.setAction(RedFlagAction.WARN); }))` (FX-006).

**Test Steps:**
1. Arrange: input `"bé hắt hơi vài lần"`.
2. Act: `policy.screen(text)`.
3. Assert: outcome `NO_MATCH`; matchedKeywords empty.

**Expected Result (PASS):** NO_MATCH.
**Expected Result (FAIL):** ANNOTATE_ONLY/ESCALATE_RED — GREEN gained unintended runtime effect.

**Current Status:** 🟢 Passing

---

### TRFP-TC-009 — screen(): repository throws → NO_MATCH + degraded=true, never propagates

**Severity:** `CRITICAL`
**CWE:** `CWE-703 — Improper Check or Handling of Exceptional Conditions`
**Legal:** `BR-SAFETY (CLAUDE.md)` / `BR-SAFETY-TRFP-002`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-TRIAGE-IMP-003 §3 ADR-003` (degrade to no-op; log + metric; never throw)

**Preconditions:** repository mocked with FX-007: `findByActiveTrue()` throws `DataAccessResourceFailureException("db down")`.

**Test Steps:**
1. Arrange: input FX-008 (would match if rules were readable).
2. Act: `PreScreenResult result = policy.screen(text)` — wrapped in `assertDoesNotThrow`.
3. Assert: `result.outcome() == NO_MATCH`; `result.degraded() == true`; degradation metric recorded once.

**Expected Result (PASS):** no exception; NO_MATCH/degraded=true; metric incremented.
**Expected Result (FAIL):** exception propagates (would surface as `TRIAGE-005` and fail the mother's intake — the single worst failure mode of this module).

**Current Status:** 🟢 Passing

---

### TRFP-TC-010 — screen(): null / blank input → NO_MATCH without any repository call (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-TRIAGE-IMP-003 §8.1` (screen contract) + boundary analysis TDS-04; precedent: `TriageRedFlagPolicy.java:32-34` (null/blank → false without DB access)

**Preconditions:** repository mock with verification enabled (no stubbing needed).

**Test Steps:**
1. Act: `policy.screen((String) null)`, `policy.screen("")`, `policy.screen("   ")` (three fresh calls).
2. Assert: each returns outcome `NO_MATCH`, degraded=false; `verifyNoInteractions(redFlagRuleRepository)`.

**Expected Result (PASS):** NO_MATCH ×3, zero repository interactions.
**Expected Result (FAIL):** NPE, or a wasted DB query on empty input.

**Current Status:** 🟢 Passing

---

### TRFP-TC-011 — runIntake(): RED short-circuit — AI never called; session COMPLETED/RED/emergency with canonical RED contract

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY-TRFP-001`
**Feature Under Test:** `TriageService.runIntake()` (pre-screen insertion, one-shot flow)
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** expected values — `hasCanonicalRedContract` fields (`TriageService.java:1128-1132`); `emergency=true` for RED (`applyCanonicalSnapshot`, `:743`); `recommendationCode` mapping (`TriageRecommendationCode.forRisk("RED") → "SEEK_EMERGENCY_CARE"`); `riskColor "#EF4444"` (`TriageGraphService.java:135`); short-circuit requirement `RM-III-2` + TDS ADR-001

**Preconditions:**
- `TriageService` built via its test constructor (existing pattern in `TriageServiceTest`) with: mocked repository (in-memory save), recorded AI client stub FX-013, real `TriageRedFlagPreScreenPolicy` over a mocked rule repository returning FX-001, event sink FX-015.

**Test Steps:**
1. Arrange: `RunIntakeRequest request = makeOneShotRequest()` (FX-011 — text matches FX-001 without diacritics).
2. Act: `IntakeSessionResponse response = triageService.runIntake(request, MOTHER_ID)`.
3. Assert: `childTriageAiClient.triageChild(...)` **never invoked** (`verifyNoInteractions` on the AI mock); saved session has `status=COMPLETED`, `riskLevel=RED`, `emergency=true`, `completedAt != null`; persisted `rawAiResponse`/result JSON contains `"emergencyActionRequired":true`, `"recommendationCode":"SEEK_EMERGENCY_CARE"`, non-empty `matchedRules` containing `"RED_FLAG_RULE_PRESCREEN"`, `redFlags` containing `"ngã đập đầu"`.

**Expected Result (PASS):** all assertions above; response mirrors COMPLETED/RED.
**Expected Result (FAIL):** AI client invoked (pre-screen after AI / not short-circuiting), or RED persisted without the canonical contract (would be rejected as unsafe elsewhere in the pipeline), or `emergency=false`.

**Expected persistence/side effects:** one `triage_sessions` row (via mocked repo `save`) in COMPLETED/RED/emergency state; events asserted separately in TRFP-TC-012.

**Current Status:** 🟢 Passing

---

### TRFP-TC-012 — runIntake(): short-circuit publishes exactly one EmergencyEscalationTriggered and one IntakeSessionCompleted

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY-TRFP-001`
**Feature Under Test:** `TriageService.runIntake()` → `publishCompletionEvents`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `publishCompletionEvents` (`TriageService.java:768-777`): RED → `EmergencyEscalationTriggered(eventId, sessionId, userId, "AUTO_TRIAGE", completedAt)` then `IntakeSessionCompleted(..., RED, completedAt)`; event records `ai/event/EmergencyEscalationTriggered.java`, `triage/event/IntakeSessionCompleted.java`

**Preconditions:** same harness as TRFP-TC-011 with event sink FX-015.

**Test Steps:**
1. Arrange: `makeOneShotRequest()`; fresh event sink.
2. Act: `triageService.runIntake(request, MOTHER_ID)`.
3. Assert: sink contains exactly one `EmergencyEscalationTriggered` with `triggerSource="AUTO_TRIAGE"`, `sessionId` = saved session id, `userId=MOTHER_ID`; exactly one `IntakeSessionCompleted` with `riskLevel=RED`; no other escalation-type events.

**Expected Result (PASS):** 1 + 1 events with the exact payload fields above.
**Expected Result (FAIL):** zero escalation events (short-circuit bypassed `publishCompletionEvents` — emergency chain broken) or duplicates (double publication).

**Expected downstream (documented, not asserted here):** `EmergencyEscalationHandler.onEmergencyEscalationTriggered` → `EmergencyService.openOrReuseFromTriage` → `safety_events`/`safety_event_actions` — covered end-to-end in `TRFP-TC-INT-001`.

**Current Status:** 🟢 Passing

---

### TRFP-TC-013 — startConversation(): RED short-circuit returns TRIAGE_COMPLETE envelope and completes the session

**Severity:** `CRITICAL`
**Feature Under Test:** `TriageService.startConversation()` (pre-screen insertion, conversation-start flow)
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** envelope key allow-list `CONVERSATION_RESPONSE_FIELDS`/`..._METADATA_FIELDS` (`TriageService.java:67-80`); completion semantics of `persistConversationEnvelope` (`:701-730` — `TRIAGE_COMPLETE` → COMPLETED + events at `:726-729`); canonical RED contract (`:1128-1132`); TDS §8.3 `buildPreScreenRedEnvelope`

**Preconditions:** harness as in TRFP-TC-011; conversation-capable session repository mocks (`findByUserIdAndClientRequestId` → empty, save-through), FX-012 request.

**Test Steps:**
1. Arrange: `StartIntakeConversationRequest request = makeStartRequest()` (initialText = FX-008).
2. Act: `IntakeConversationResponse response = triageService.startConversation(request, MOTHER_ID)`.
3. Assert: `childTriageAiClient.startIntake(...)` never invoked; response/envelope `status="TRIAGE_COMPLETE"`; `triageResult.riskLevel="RED"` with `emergencyActionRequired=true`, `recommendationCode="SEEK_EMERGENCY_CARE"`, non-empty `matchedRules`; saved session `status=COMPLETED`, `riskLevel=RED`, `emergency=true`; event sink contains `EmergencyEscalationTriggered` + `IntakeSessionCompleted`; envelope contains no keys outside the allow-list.

**Expected Result (PASS):** all above.
**Expected Result (FAIL):** AI invoked; or envelope with foreign keys (would be stripped/rejected by `sanitizeEnvelope`/`ensureSafeEnvelope`); or session left NEED_MORE_INFO.

**Current Status:** 🟢 Passing

---

### TRFP-TC-014 — continueConversation(): RED short-circuit when a new answer introduces a matching keyword

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.continueConversation()` (pre-screen insertion, conversation-continue flow)
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** same completion oracles as TRFP-TC-013; insertion point before `childTriageAiClient.continueIntake` (`TriageService.java:370`); input aggregation TDS §8.1 (newAnswers + mergedIntake string values)

**Preconditions:**
- Session mock: existing conversation session (`symptoms="CONVERSATION_INTAKE"`, status `NEED_MORE_INFO`, `rawAiResponse` = FX-013 ASK_MORE envelope with outstanding question key `parentFreeText`... *(question key must be in the stored envelope's `questions` so the answer passes the `:361-366` filter)*).
- Rule repository mock → FX-001.

**Test Steps:**
1. Arrange: `ContinueIntakeConversationRequest` with `newAnswers={"parentFreeText":"bé vừa bị nga dap dau"}`.
2. Act: `triageService.continueConversation(request, MOTHER_ID)`.
3. Assert: `continueIntake` never invoked; envelope `TRIAGE_COMPLETE`/RED (same contract assertions as TC-013); session COMPLETED/RED/emergency; both events published.

**Expected Result (PASS):** short-circuit on continue path.
**Expected Result (FAIL):** AI invoked, or existing validations (`TRIAGE-010` answer filtering) broken by the insertion (pre-screen must run AFTER them — ordering per TDS §8.1).

**Current Status:** 🟢 Passing

---

### TRFP-TC-015 — No rule match → AI called exactly as before (passthrough regression guard)

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.runIntake()` non-matching path
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `RM-III-2` scope ("pre-screen adds detection, changes nothing else"); existing one-shot flow contract (`TriageService.java:403-428` unchanged); TDS §9.1 "behavioral change only on match"

**Preconditions:** rule repository mock → `List.of(makeRedEscalateRule())`; AI stub FX-013 returns `makeAiGreenOneShotJson()`.

**Test Steps:**
1. Arrange: `makeNeutralOneShotRequest()` (FX-010 — matches no rule and no hardcoded phrase).
2. Act: `triageService.runIntake(request, MOTHER_ID)`.
3. Assert: `triageChild` invoked exactly once with the request; session `COMPLETED`/`GREEN`; **no** `EmergencyEscalationTriggered` in the sink; result JSON does not contain `RED_FLAG_RULE_PRESCREEN`.

**Expected Result (PASS):** behavior identical to pre-feature flow.
**Expected Result (FAIL):** AI skipped, spurious escalation, or mutated request reaching the AI.

**Current Status:** 🟢 Passing

---

### TRFP-TC-016 — Degraded pre-screen → AI called; AI failure → hardcoded Java fallback still runs (defense-in-depth chain intact)

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY (CLAUDE.md)` / `BR-SAFETY-TRFP-002/003`
**Feature Under Test:** `TriageService.runIntake()` with failing rule lookup AND failing AI
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-016`
**Oracle Source:** TDS ADR-003 (degrade = behave as no-match); existing fallback chain `triageWithAiServiceOrFallback` (`TriageService.java:442-457` — AI exception → `triageGraphService.run`); `RM-III-2` ("keep hardcoded rules as last-resort layer")

**Preconditions:**
- Rule repository mock throws (FX-007) — pre-screen degraded.
- AI stub throws `RuntimeException("python down")`.
- Real `TriageGraphService` (or its existing test double per `TriageServiceTest` conventions) so the hardcoded `PediatricRiskRules` path executes.

**Test Steps:**
1. Arrange: request `makeOneShotRequest(r -> r.setBreathingStatus("khó thở, rút lõm"))` — content that the HARDCODED rules classify RED (`RED_BREATHING_DISTRESS`, `PediatricRiskRules.apply`), independent of the dead pre-screen.
2. Act: `triageService.runIntake(request, MOTHER_ID)` — wrapped in `assertDoesNotThrow`.
3. Assert: request did NOT fail with `TRIAGE-005`; session COMPLETED with `riskLevel=RED` produced by the fallback (`matchedRules` contains `RED_BREATHING_DISTRESS`, NOT `RED_FLAG_RULE_PRESCREEN`); `EmergencyEscalationTriggered` published (fallback RED still escalates); degradation metric recorded.

**Expected Result (PASS):** intake survives double failure at the pre-feature protection level.
**Expected Result (FAIL):** pre-screen exception fails the request (503 `TRIAGE-005`) — the feature would have *reduced* safety; immediate reject (AP-AI-006).

**Current Status:** 🟢 Passing

---

### TRFP-TC-017 — ANNOTATE_ONLY (WARN/YELLOW) → AI still called, no short-circuit, annotation recorded

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.startConversation()` annotation path
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-017`
**Oracle Source:** TDS ADR-002 annotation mechanism (metric + log always; `preScreenFlags` request key only if Open item O1 resolves positive — this TC asserts the always-guaranteed part and conditionally the key)

**Preconditions:** rule repository mock → FX-003 (YELLOW `"sốt kéo dài"`); AI stub returns `makeAiAskMoreEnvelopeJson()`, captures its request argument.

**Test Steps:**
1. Arrange: `makeStartRequest()` with `initialText="bé sốt kéo dài ba ngày"`.
2. Act: `triageService.startConversation(request, MOTHER_ID)`.
3. Assert: `startIntake` invoked once; response `status="ASK_MORE"` (AI's answer passed through, no forced completion); saved session `NEED_MORE_INFO`, `riskLevel=null`; no escalation event; annotation metric recorded once with flow `conversation_start`; **[conditional on O1]** captured AI request map contains `preScreenFlags=["sốt kéo dài"]` — if O1 resolves negative, this assertion is replaced by "captured request contains NO `preScreenFlags` key" (spec must be updated at implementation time, not silently).
   **[O1 RESOLVED 2026-07-26 — POSITIVE]** Verified `CareBridgeAITriageService/app/schemas.py`: `IntakeStartRequest` (line 153) and `IntakeContinueRequest` (line 159) declare no `extra="forbid"` (pydantic default ignores unknown keys), so the additive `preScreenFlags` key is tolerated. The implemented test asserts the captured request DOES contain `preScreenFlags=["sốt kéo dài"]`.

**Expected Result (PASS):** flow continues to AI; annotation observable via metric.
**Expected Result (FAIL):** YELLOW short-circuits (over-escalation) or annotation silently dropped (no metric).

**Current Status:** 🟢 Passing

---

### TRFP-TC-018 — Idempotent replay after short-circuit: same clientRequestId returns stored envelope, no duplicate escalation (concurrency guard)

**Severity:** `HIGH`
**Feature Under Test:** `TriageService.startConversation()` idempotency arbitration + pre-screen interaction
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-018`
**Oracle Source:** existing replay contract (`TriageService.java:255-261` — existing session with `rawAiResponse` → stored envelope returned before any AI/pre-screen work); event-parity requirement BR-SAFETY-TRFP-001 ("exactly once")

**Preconditions:** harness as TRFP-TC-013; first call performed inside the test to create the short-circuited stored state (fresh instances via factories — no cross-test state).

**Test Steps:**
1. Arrange: `makeStartRequest()` (clientRequestId `"trfp-cr-001"`); execute first `startConversation` → short-circuit completes, events recorded; snapshot event count.
2. Act: call `startConversation` again with an equal request built by a **fresh** `makeStartRequest()` (simulates client retry / duplicate delivery — the concurrency-relevant replay path arbitrated by `findByUserIdAndClientRequestId`).
3. Assert: second response equals the stored TRIAGE_COMPLETE/RED envelope; AI still never invoked; event sink count unchanged (no second `EmergencyEscalationTriggered`, no second `IntakeSessionCompleted`); no second session row saved as new.

**Expected Result (PASS):** replay-safe; exactly-once escalation preserved.
**Expected Result (FAIL):** duplicate escalation events (would open duplicate `safety_events`) or a second pre-screen run creating a divergent session.

**Current Status:** 🟢 Passing
**Implementation Note:** true parallel-thread arbitration is exercised by the existing `IntakeSessionWriter` DB-arbitrated path and is out of unit scope; this TC pins the replay contract the arbitration converges to.

---

### TRFP-TC-019 — Freshness (cache-staleness guard): rule change is visible on the very next screen() call

**Severity:** `MEDIUM`
**Feature Under Test:** `TriageRedFlagPreScreenPolicy.screen(String)` — read-through property (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-019`
**Oracle Source:** TDS ADR-004 decision ("read-through, no cache — an admin-added emergency keyword protects the very next intake request"; §4.2 Freshness = 100%)

**Preconditions:** rule repository mock programmed with **consecutive** returns: first call → `List.of()` (rule not yet active), second call → `List.of(makeRedEscalateRule())` (admin activated it).

**Test Steps:**
1. Act 1: `policy.screen("bé bị nga dap dau")` → assert outcome `NO_MATCH`.
2. Act 2 (immediately after): `policy.screen("bé bị nga dap dau")` → assert outcome `ESCALATE_RED`.
3. Assert: `findByActiveTrue()` invoked exactly twice (once per screen() — no memoization, no TTL cache).

**Expected Result (PASS):** second call sees the new rule; repository hit per invocation.
**Expected Result (FAIL):** second call still NO_MATCH (a cache was introduced — violates ADR-004/C7) or repository called once.

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### TRFP-TC-SEC-001 — Intake endpoints remain MOTHER-only (RBAC unchanged by the pre-screen)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** existing RBAC requirement (CLAUDE.md — enforce existing RBAC for health workflows)
**Feature Under Test:** `IntakeController` + Spring Security (`@PreAuthorize("hasRole('MOTHER')")`, verified `IntakeController.java:36-89`)
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenSecurityTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `CB-TRIAGE-IMP-003 §16` Authorization Matrix (unchanged); UC-110 precedent for 403 shape (`ACCESS_DENIED` envelope)

**Preconditions:** MockMvc slice with security filters active; principal FX-014 `role=EXPERT`.

**Test Steps (Attack Simulation):**
1. Arrange: authenticated EXPERT principal; body containing a RED-matching keyword (FX-008) — probing whether the new safety path can be abused to trigger escalations cross-role.
2. Act: `POST /api/v1/triage/intake` and `POST` conversation start with that principal.
3. Assert: both return `403`; no session persisted; no event published; pre-screen never invoked (verify policy mock zero interactions).

**Expected Result (PASS = secure):** 403 on all intake endpoints for non-MOTHER; pre-screen unreachable pre-authorization.
**Expected Result (FAIL = vulnerability):** 2xx or a persisted session/escalation for a non-MOTHER caller.

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

> Hosted as `@SpringBootTest` + H2 test datasource — **project convention**; no Testcontainers harness exists anywhere in this codebase (verified finding, UC-110 Test-Spec changelog 2026-07-02). Timeout: 120s.

---

### TRFP-TC-INT-001 — End-to-end: seeded admin RED rule short-circuits a real one-shot intake through real beans

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: RedFlagRuleRepository (H2) → TriageRedFlagPreScreenPolicy → TriageService.runIntake → event listeners`
**Test File:** `src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `RM-III-2` (independence from AI); TDS §14.1 DB assertions; canonical contract oracles as TRFP-TC-011

**Preconditions:**
- Spring context with H2 test datasource (schema derived per existing test profile).
- AI client replaced by a `@MockBean`/test-profile stub that **throws if invoked** (proves independence — stricter than "not asserted").
- Seed via `RedFlagRuleRepository.save`: FX-001 (`"ngã đập đầu"`, RED/ESCALATE/active) and FX-002 (inactive).
- Authenticated MOTHER user context per existing integration-test conventions.

**Test Steps:**
1. Seed rules; build `makeOneShotRequest()` (accentless text FX-008).
2. Call `triageService.runIntake(request, MOTHER_ID)` (service-level, real beans).
3. Assert response COMPLETED/RED; assert DB state; repeat with `makeNeutralOneShotRequest()` against the inactive-rule keyword text `"bé bị sặc sữa liên tục"` → expect the AI-throwing stub to be reached (wrapped: the existing fallback then handles it) — proving inactive rules do NOT short-circuit through the real SQL filter.

**Expected Result (PASS):**
- Matching request: session row `status='COMPLETED'`, `risk_level='RED'`, `emergency_flag=true`; AI stub never invoked; escalation listener effect present (per available beans in the test profile — at minimum the published `EmergencyEscalationTriggered` is observed via a test listener; full `safety_events` row asserted if the emergency beans are active in this profile — align with existing `TriageIntegrationTest` conventions).
- Inactive-keyword request: no short-circuit (AI stub reached → fallback path), no `RED_FLAG_RULE_PRESCREEN` in result.

**Expected Result (FAIL):** short-circuit not triggered from a real DB row (wiring gap between repository and policy), or inactive rule triggering (SQL filter bug).

**DB Assertion:**
```java
IntakeSession session = intakeSessionRepository.findById(sessionId).orElseThrow();
assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
assertThat(session.isEmergency()).isTrue();
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|------------------|-------------------|------------------|
| `TRFP-TC-001` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-002` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-003` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-004` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-005` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-006` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-007` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-008` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-009` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-010` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-011` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-012` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-013` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-014` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-015` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-016` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-017` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-018` | `triage/TriageServicePreScreenTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-019` | `triage/TriageRedFlagPreScreenPolicyTest.java` | `[x]` | 2026-07-26 (no-commit) | |
| `TRFP-TC-SEC-001` | `triage/TriageRedFlagPreScreenSecurityTest.java` | `[ ]`* | 2026-07-26 (no-commit) | *PASSED at Red Gate — documented legitimate pre-existing-subject pass (§5.1 table note; exercises the unchanged Spring Security chain) |
| `TRFP-TC-INT-001` | `triage/TriageRedFlagPreScreenIntegrationTest.java` | `[x]` | 2026-07-26 (no-commit) | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ The MOST IMPORTANT gate. Before implementing, run the whole suite against throw-stubs.
> Every test MUST FAIL. Any test passing immediately → **AP-AI-002 detected** → reject and rewrite.

**Red Phase stubs (scaffolding compiles; ALL logic throws):**

```java
// Red Phase — implementation stubs (MUST throw)

// triage/policy/TriageRedFlagPreScreenPolicy.java
@Component
public class TriageRedFlagPreScreenPolicy {

    public PreScreenResult screen(String aggregatedText) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    public PreScreenResult screen(RunIntakeRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// triage/service/TriagePreScreenMetrics.java
@Component
public class TriagePreScreenMetrics {
    public void recordShortCircuit(String flow) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void recordAnnotation(String flow) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void recordDegraded(String flow) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// TriageService: constructor wiring + call sites added at the three insertion points,
// but the branch handlers delegate to the throwing stubs above — every service-level TC
// therefore fails (the stub throw inside runIntake's try block surfaces as TRIAGE-005;
// tests assert success/contract fields and fail as required).
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (if unexpectedly PASS) |
|-------|-------------|----------|--------|------------------------------------|
| `TRFP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `TRFP-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | *(asserts NO exception + degraded flag — stub throw fails it)* |
| `TRFP-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-013` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-015` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-016` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-017` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-019` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TRFP-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☑ PASS | ⚠ *Note: exercises the pre-existing Spring Security chain; if it passes at Red Gate, document as legitimate pre-existing-subject pass (UC-110 precedent RFR-TC-SEC-001/002) — its policy-zero-interaction assertion still requires the stubbed bean to be present. **Actual 2026-07-26: PASSED at Red Gate exactly per this documented exception** (403 + zero policy interactions against the throwing stub bean) |
| `TRFP-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `no-commit (working tree — HuyND branch, 2026-07-26)`
- All FAIL? [x] Yes → **GATE-2 PASS** (T2→T3) → proceed to implement
  *(actual run 2026-07-26 22:21 +07: `Tests run: 21, Failures: 3, Errors: 17` → 20/21 FAIL;
  sole pass = `TRFP-TC-SEC-001`, the exception documented in the table note above)*
- Log file: `04_Implement/TriageRedFlagPreScreen/red-gate-evidence.log` *(produced at Red Gate run)*

> **If any test PASSES:** stop. Determine the root cause from the table. Rewrite the test from its TC spec using the Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-TRIAGE-IMP-003` reviewed and approved (header `Status: Approved` verified 2026-07-26 before implementation)
- [x] Logic Issues (Section 2, L1–L6) confirmed with the reviewer/architect (part of the approved spec; L5 re-verified against `RedFlagRuleRepository.java` javadoc)
- [x] Open items O1 (Python `preScreenFlags` tolerance) and O2/O3 triaged — O1 RESOLVED POSITIVE 2026-07-26 (see TRFP-TC-017 note); O2/O3 remain `Open` with reviewer owners (non-blocking for v1 per TDS)
- [x] No Flyway migration required (verified: `red_flag_rules` exists in baseline `B20260724111500`; no migration added by this feature)
- [x] Test fixtures (TDS-05) reviewed: fixture keywords re-verified 2026-07-26 — grep over `triage/engine/*`, `TriageRedFlagPolicy.java`, `app/risk_rules.py`, `app/symptom_normalizer.py`: 0 hits for all six fixture keywords

### Exit Criteria (DoD)

- [x] `./mvnw clean test` run 2026-07-26: all 21 TRFP TCs green; overall `Tests run: 2953, Failures: 1, Errors: 72, Skipped: 100` — the 1 failure (`ChecklistTemplateMigrationTest.uc82_69_int_005_v1RemainsByteIdentical`, checksum drift from rebaselining commit `faef9640`; file untouched by this feature) and all 72 errors (Testcontainers `AbstractPostgresIntegrationTest` suites — Docker unavailable in this environment) are demonstrably PRE-EXISTING and unrelated to TriageRedFlagPreScreen
- [x] `./mvnw test -Dtest=TriageRedFlagPreScreenIntegrationTest` — integration green (H2), also green inside the full run
- [x] Pre-existing triage suites still green in the full run: `TriageServiceTest` (52), `TriageRedFlagPolicyTest` (5), `TriageGraphServiceTest` (26), `PediatricRedParityTest` (1), `RedFlagRuleServiceImplTest` (9), `RedFlagRuleControllerTest` (1), `RedFlagRuleIntegrationTest` (1), `IntakeControllerTest` (7) — zero regression
- [ ] Test coverage ≥ 80% lines for `TriageRedFlagPreScreenPolicy` — NOT MEASURED (no coverage tooling configured/run in this session; 11 unit TCs cover every branch of `screen`/`classify`/`normalize` by construction)
- [x] No business logic in controllers (no controller change at all — `git status` confirms no controller file touched)
- [x] `git status`/`git diff` confirm `TriageRedFlagPolicy.java`, `PediatricRiskRules.java`, `TriageGraphService.java`, `SymptomNormalizer.java`, and the entire `CareBridgeAITriageService/` tree are untouched by this feature (C4)
- [x] No PII (symptom free text) in any new log statement — reviewed: new log lines carry only flow/ruleCount/exception-class
- [ ] Feature-specific: an admin-added RED/ESCALATE rule demonstrably short-circuits a staging intake without the Python service running (§14.3 smoke) — NOT PERFORMED (no staging deployment in this session)

**Additional Exit Criteria — CASE 2.0:**

- [x] **Red Gate (§5.1)** — 20/21 TCs FAILED against throw-stubs before implementation; `TRFP-TC-SEC-001` passed per its documented pre-existing-subject exception (evidence: `red-gate-evidence.log`)
- [x] **Contract Existence** — `./mvnw compile` clean (exit 0, no errors), every injected class exists:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [x] **Props Isolation** — grep run 2026-07-26: single hit `TriageServicePreScreenTest.java: private final ObjectMapper objectMapper = new ObjectMapper();` — a per-test-instance immutable-config object (JUnit 5 creates a fresh test instance per method; same established pattern as `TriageServiceTest`); all fixture data comes from `TriagePreScreenTestFactory.makeXxx()`:
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java
  # Every instance must live inside @Test or come from TriagePreScreenTestFactory.makeXxx()
  ```
- [x] **Oracle Source** — every expected value in every assert traces to the cited BR/ADR/code-line oracle (spot-check performed while implementing: `#EF4444` ← `TriageGraphService.colorFor`, `SEEK_EMERGENCY_CARE` ← `TriageRecommendationCode.forRisk("RED")`, `AUTO_TRIAGE` ← `TriageService.publishCompletionEvents`, canonical RED fields ← `hasCanonicalRedContract`)

### Suspension Criteria

- TDS ADRs rejected or materially changed during review (re-baseline the affected TCs first)
- Open item O1 unresolved by Stage 4 (§11.3) — suspend only `TRFP-TC-017`'s conditional assertion, not the suite
- CI broken by unrelated changes

---

## 7. Rollback Plan

```bash
# Code-only feature — NO database rollback (no migration, no new tables; red_flag_rules
# belongs to UC-110/baseline and is NOT dropped under any circumstances).

# Revert implementation + test files (dev):
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/policy/PreScreenOutcome.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/policy/PreScreenResult.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/policy/TriageRedFlagPreScreenPolicy.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/service/TriagePreScreenMetrics.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/repository/RedFlagRuleRepository.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/service/impl/TriageService.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriagePreScreenTestFactory.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenPolicyTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageServicePreScreenTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenSecurityTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageRedFlagPreScreenIntegrationTest.java

# Deployed environments: kubectl rollout undo deployment/carebridge-api (see TDS §12.2 —
# the previous binary has no dependency on the new policy; old behavior fully restored).

# Gap remains OPEN → keep roadmap Part III item 2 unresolved in
# 04_Implement/AITriageCompletion/AITriage_Assessment_Roadmap.md (do not edit that doc's status).
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Signal in this TDD spec | Check | Blocking gate |
|-------|--------------|--------------------------|-------|---------------|
| AP-AI-001 | Unconstrained Generation | A TC without an ADR/BR/code-line Oracle Source reference | ☑ checked 2026-07-26 — every implemented TC carries its Oracle Source comment | G-0 |
| AP-AI-002 | Green-from-Birth | Any TC passing against the §5.1 throw-stubs (documented exception: `TRFP-TC-SEC-001`, pre-existing-subject) | ☑ checked 2026-07-26 — Red Gate run: 20/21 FAIL; sole pass is the documented exception | G-2 ★ |
| AP-AI-003 | Implicit Decision | A TC assuming a cache, a new endpoint, a new event type, or Python-side changes (all absent from TDS ADRs) | ☑ checked 2026-07-26 — no cache (TC-019 pins read-through), no new endpoints/events, Python untouched | G-1 |
| AP-AI-004 | Layer Violation | A TC asserting matching/classification logic inside `TriageService` or a controller | ☑ checked 2026-07-26 — matching/classification only in `TriageRedFlagPreScreenPolicy`; service tests assert workflow effects only | G-4 |
| AP-AI-005 | Hallucinated Contract | A TC importing `findByIsActiveTrue()`, a `PreScreenController`, new DTO fields on API responses, or envelope keys outside `TriageService.java:67-80` | ☑ checked 2026-07-26 — grep: `findByIsActiveTrue` absent from triage code (only unrelated masterdata `Specialty`); no new controller/DTO; envelope keys within allow-list (enforced by `toConversationResponse`) | G-3 |

**Review result:**

- [x] No anti-pattern detected → TDD spec approved *(checks actually run 2026-07-26 — grep evidence + Red Gate log)*
- [ ] AP detected → record below → fix before implementation

| AP detected | TC ID | Description | Fix action | Fixed? |
|-------------|-------|-------------|------------|--------|
| `AP-AI-___` | `TC-___` | | | ☐ |

---

*TDD Template v2.0 — CASE 2.0 Anti-Pattern Detection & Red Gate Protocol integrated.*
