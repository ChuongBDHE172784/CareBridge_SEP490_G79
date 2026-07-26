# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC61 Persist Safety Outcomes and Return to Origin

**Document ID:** `CB-OV01-TDD-061`

**Version:** `1.1`

**Date:** `2026-07-22`

**Status:** `Approved`

**Standard:** ISO/IEC/IEEE 29119-3:2021 structure adapted for CareBridge

**Author:** `Codex — specification author`

**Reviewed by:** `[x] Story 6.7 independent specification verifier`

**DPO Sign-off:** `[ ] Required before production release; not a development gate`

**Approved by:** `[x] User — implementation approval granted 2026-07-22`

**Classification:** `Internal — Sensitive test design; synthetic data only`

**References:**

- `UC61 - Persist Safety Outcomes and Return to Origin_TDS.md` — Approved technical contract.
- `_bmad-output/implementation-artifacts/6-7-persist-safety-outcomes-and-return-to-origin.md` — Story 6.7 implementation source.
- `02_Requirements/SRS/3_Functional_Specification.md` — UC-61 View Risk Triage Result.
- `_bmad-output/planning-artifacts/{prd,epics,architecture,ux}.md` — FR51/Story 6.7 oracles.
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` and approved forward migrations — persistence oracle.

> TDD order remains the audit model: runnable test → intended RED → minimum implementation → GREEN → refactor while green. Section 4 now maps the approved cases to the actual Story 6.7 tests and current evidence; it does not retroactively claim missing standalone RED logs or incomplete manual evidence.

---

## CHANGELOG

| Date | Author | Change |
|---|---|---|
| 2026-07-22 | Codex — specification author | Initial Draft for Story 6.7; all cases Not written. |
| 2026-07-22 | Codex — implementation agent | Approved after independent specification verification and user implementation approval; cases remain Not written until executed through TDD. |
| 2026-07-22 | Codex — implementation agent | Synchronized actual Story 6.7 test files, GREEN/refactor evidence, entry/exit gates, and partial Android status after implementation. |

---

## TABLE OF CONTENTS

1. Module Information
2. Logic Issues Resolved
3. Test Design Specification
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry / Exit Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

---

## 1. Module Information

| Field | Value |
|---|---|
| **Feature / Gap ID** | `OV01-GAP-07 / Story 6.7 / FR51 / UC61 remediation` |
| **Module** | Triage + Emergency sources, Journey projection/timeline, Flutter Mobile |
| **Source Spec** | `CB-OV01-IMP-061` Approved v1.1 |
| **Priority** | P1; P0 manual scenario OV01-MAN-028 |
| **Sprint** | Epic 6 Wave 2; date Open |
| **Milestone** | OV-01 safety integrity release gate |
| **Data Classification** | Sensitive-PII references; synthetic fixtures only |
| **Compliance Scope** | BR-RBAC, BR-PRIVACY, BR-SAFETY, lifecycle consent, immutable audit; legal citation Open |
| **Upstream Dependencies** | Stories 6.1–6.6 and approved UC61 TDS |
| **Downstream Consumers** | Story 6.10 traceability/E2E release gate |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|---|---|
| **AI Assisted?** | Yes |
| **Constraint Source** | Story 6.7 AC1–AC6; TDS ADR-061-01..04 and §§8–10,16–17 |
| **Constraints Injected** | Separate projection, sync transaction, one-shot exclusion, owner-bound typed continuation, unified timeline, no new pediatric cutoff, Story 6.6 preservation |
| **Model** | GPT-5 Codex |
| **Trust Level** | T3 — approved specification, automated/manual GREEN, and independent code review; coverage and production Privacy/DPO remain release gates |

---

## 2. Logic Issues Resolved

| ID | Requirement / Design Gap | Current Evidence | Test Oracle Resolution |
|---|---|---|---|
| L1 | UC61 legacy SRS only says view risk result | Story 6.7/FR51 requires projection + return | retain UC61 traceability and test remediation ACs explicitly |
| L2 | `mother_journey_transitions` cannot hold repeated safety events at one journey version | immutable/version-unique Story 6.1 schema | separate `lifecycle_safety_outcomes`, unique intake source |
| L3 | “triage and emergency outcomes” could create two RED rows | MAN-028 expects one projection per outcome; Story 6.6 permits emergency reuse | one row per terminal intake; RED has optional emergency source ID |
| L4 | one-shot `RunIntakeRequest` has no idempotency key | current code always creates a new session | one-shot/direct is explicitly non-projecting; conversation flow owns Story 6.7 |
| L5 | completion also has AFTER_COMMIT structured side work | current Story 6.6 handler behavior | new projection listener is ordinary/synchronous; existing AFTER_COMMIT handler remains unchanged |
| L6 | two independent paged APIs cannot form a global timeline | current `/history` only | new database-paginated discriminated `/timeline`; `/history` unchanged |
| L7 | arbitrary client return data would enable unsafe navigation | current origin is in-memory only | server issues token and returns only two enum descriptors; Flutter maps fixed routes |
| L8 | Story artifacts do not approve a new toddler cutoff | Story 6.6 production classifier exists | tests preserve existing classifier and add no new age expectation |
| L9 | Data dictionary names `triage_assessments`, implementation uses `intake_sessions` | approved Flyway/current code | Flyway history/current entity is implementation oracle; discrepancy is documented, not silently renamed |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```text
UC61 Story 6.7 test layers
├── Backend unit/service/event/controller
├── PostgreSQL migration, constraints, transaction, concurrency, pagination
├── Flutter model/service/storage/router/widget
├── Cross-module Story 6.6 regression
└── Android manual OV01-MAN-023/028/031 evidence

Not in scope: Web, AI risk threshold changes, YELLOW expert handoff, provider notification exactly-once, legacy backfill.
```

### TDS-02 — Test Basis

| Source | Derived Oracles |
|---|---|
| UC-61 SRS | authenticated Mother, GREEN/YELLOW/RED result, safe next action, errors/retry |
| FR51 / Story 6.7 AC1–6 | exact projection, minimum data, lifecycle context, restart-safe return |
| TDS ADR-061-01 | separate projection; one row/intake; RED optional emergency source |
| TDS ADR-061-02 | synchronous rollback/retry; preserve existing AFTER_COMMIT side work |
| TDS ADR-061-03 | owner-bound token, approved seven-day default TTL, neutral errors, strict dashboard/action pair, fixed routes |
| TDS ADR-061-04 | unified global timeline pagination |
| Story 6.6 | RED ordering, emergency reuse, GET-only mobile handoff, five origins, 115 fallback |
| V1 + approved migrations | PostgreSQL names/types/FKs/idempotency boundaries |
| UI Skill System / UX | Warm Clay, 48px, 16px, semantics, non-color-only state, inline retry |

### TDS-03 — Test Conditions and Coverage

| Condition ID | Test Condition | Coverage Item | Cases |
|---|---|---|---|
| `S67-C01` | valid Mother origin binds canonical journey/stage/token | conversation start service/controller | 001 |
| `S67-C02` | valid baby origin binds owned linked baby without new age rule | origin policy | 002 |
| `S67-C03` | non-owner, inactive, unlinked, consent invalid fail neutrally | policy/controller | 003,004 |
| `S67-C04` | replay same intent is stable; changed intent conflicts; one-shot excluded | idempotency/legacy | 005,006 |
| `S67-C05` | GREEN/YELLOW/RED produce one minimum projection | projector | 007,008 |
| `S67-C06` | duplicate/concurrent/retry exact-once | PostgreSQL transaction | 009 |
| `S67-C07` | append-only/FK/check/index and no payload leakage | migration/entity | 010,011 |
| `S67-C08` | RED references Story 6.6 association without extra AI/emergency/outbox | event ordering | 012 |
| `S67-C09` | resolve/ack state, TTL, neutral errors | continuation service/controller | 013,014 |
| `S67-C10` | restart return GREEN/YELLOW/RED fixed origin | Flutter coordinator/router | 015,016 |
| `S67-C11` | account switch, logout, late response isolation | secure store/auth | 017 |
| `S67-C12` | RBAC/IDOR/consent/log redaction | security/privacy | 003,014,018 |
| `S67-C13` | unified timeline global order/pagination | timeline query/API/mobile | 019 |
| `S67-C14` | accessible truthful UI state | widget/manual | 020 |
| `S67-C15` | Story 6.6 regression | backend/mobile suites | 021 |
| `S67-C16` | OV-01 device acceptance | Android/API/DB | 022 |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|---|---|---|
| Equivalence partitioning | risk/origin/token status/roles | cover valid and invalid contracts |
| Boundary analysis | page size 0/1/100/101; token expiry; idempotency key length | validate exact contract edges |
| Decision table | stage × origin × journey/baby/consent state | prevent mixed maternal/baby behavior |
| State transition | ACTIVE→ACKNOWLEDGED/EXPIRED | validate monotonic token state |
| Concurrency | duplicate completion/start replay | prove database exact-once, not mock behavior |
| Fault injection | projection repository/network/late response | prove no false success and recovery |
| Security abuse | foreign IDs/token, arbitrary URL fields, payload logging | prevent IDOR/open redirect/data leak |

### TDS-05 — Test Data Requirements

| Fixture | Type | Synthetic Value / Logic | Purpose |
|---|---|---|---|
| `S67-FX-01` | users | Mother A, Mother B UUIDs + JWTs | owner isolation |
| `S67-FX-02` | journeys | A PRE/PREG/POST active canonical; B active journey | maternal mapping/IDOR |
| `S67-FX-03` | babies | A INFANT/TODDLER accepted by current classifier, A unlinked, B foreign | baby origin/link checks |
| `S67-FX-04` | consent | valid, missing, expired, revoked | fail-closed decisions |
| `S67-FX-05` | intakes | terminal GREEN/YELLOW/RED and nonterminal conversation | projection/state |
| `S67-FX-06` | emergency | RED association; multiple intakes reuse one ACTIVE session | source reference/cardinality |
| `S67-FX-07` | tokens | active, expired, acknowledged, malformed, foreign | resolve/ack partitions |
| `S67-FX-08` | clock/random | fixed UTC Clock + deterministic token generator fake | TTL and repeatability |
| `S67-FX-09` | network | offline, 5xx, delayed response, process rebuild | Mobile recovery |

Fixtures use no real health text, token, user, phone, location, or credentials. Each test creates fresh data and cleans its transaction/container/storage.

---

## 4. Test Case Specification

**ID format:** `S67-TC-NNN`. Statuses below distinguish automated GREEN, qualified/partial evidence, and manual work still in progress.

Implemented Story 6.7 test-file map:

- Backend contract (`10`): `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/Story67LifecycleContractRedTest.java`
- Backend continuation (`4`): `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageContinuationServiceTest.java`
- Backend PostgreSQL (`6`): `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/Story67SafetyOutcomePostgresRedTest.java`
- Mobile origin contract/surfaces: `05_Development/CareBridgeMobileApp/test/features/aiTriage/story_6_7_lifecycle_origin_contract_test.dart`
- Mobile account-scoped storage: `05_Development/CareBridgeMobileApp/test/features/aiTriage/story_6_7_continuation_storage_test.dart`
- Mobile restart/arrival/timeline/acknowledgement: `05_Development/CareBridgeMobileApp/test/features/aiTriage/story_6_7_continuation_restore_test.dart`

The reported focused Mobile `85/85` also includes affected symptom/result/emergency/router/home tests; Backend `117/117` is the affected regression set in addition to the 20 direct files above.

### 4.1 Backend Contract and Policy Cases

| ID / Severity | Feature / Actual Test File(s) | Oracle | Preconditions | Arrange / Act / Assert | Expected Persistence / Side Effects | Failure Signature | Status |
|---|---|---|---|---|---|---|---|
| `S67-TC-001` HIGH | bound Mother start — `triage/Story67LifecycleContractRedTest.java`; affected triage/controller regressions | AC1; TDS §5.3 | FX01/02/04 valid | Arrange active POST journey; Act start with `MOTHER_JOURNEY`; Assert 200, persisted journey/stage/origin, stable token/expiry | one intake; zero projection until terminal; no token log | missing/mismatched server context or client route trusted | PASS — Backend `137/137` final evidence |
| `S67-TC-002` HIGH | bound baby start — `triage/Story67LifecycleContractRedTest.java`; `story_6_7_lifecycle_origin_contract_test.dart` | AC1; Decision 6 | owned linked baby accepted by current classifier | start INFANT/TODDLER with BABY_PROFILE; assert related journey and exact baby ref; repeat for both existing production classifications | one intake; no new age rule or link mutation | wrong baby/journey or Story 6.6 classifier changed | PASS — focused Mobile `85/85` + Backend contract |
| `S67-TC-003` CRITICAL | owner/link/consent denial — `Story67LifecycleContractRedTest.java`, `TriageContinuationServiceTest.java` | BR-RBAC/PRIVACY; AC1/5 | foreign/inactive/unlinked/revoked fixtures | submit each invalid partition; assert neutral 404/409 and no existence disclosure | no intake/token/projection/audit success | resource data or distinguishable foreign response leaks | PASS — Story 6.7 Backend `20/20` |
| `S67-TC-004` HIGH | maternal stage mapping — `Story67LifecycleContractRedTest.java`; `story_6_7_lifecycle_origin_contract_test.dart` | TDS §5.3 | PRE/PREG/POST journeys | table-drive stage↔journey combinations; assert only exact matches pass | no side effect for mismatch | default/loose mapping accepts incompatible stage | PASS — Backend contract + focused Mobile |
| `S67-TC-005` HIGH | start replay / changed intent — `Story67LifecycleContractRedTest.java`; affected `TriageServiceTest` regressions | AC1; ADR-061-03 | same owner/key | call same intent twice then changed journey/origin; assert same intake/token then `TRIAGE-016` | one intake/token; no duplicate audit | token rotates or changed intent reuses key | PASS — Backend final `137/137` |
| `S67-TC-006` MEDIUM | one-shot/direct exclusion — `Story67LifecycleContractRedTest.java`; `story_6_7_lifecycle_origin_contract_test.dart` | TDS scope; L4 | legacy Run/direct input | run legacy path and attempt lifecycle fields at boundary; assert legacy response and no durable descriptor | no continuation/projection claim | one-shot silently projects or breaks legacy | PASS — Backend contract + focused Mobile |

### 4.2 Projection and PostgreSQL Cases

| ID / Severity | Feature / Actual Test File(s) | Oracle | Preconditions | Arrange / Act / Assert | Expected Persistence / Side Effects | Failure Signature | Status |
|---|---|---|---|---|---|---|---|
| `S67-TC-007` CRITICAL | projection minimum data — `triage/Story67LifecycleContractRedTest.java`, `journey/Story67SafetyOutcomePostgresRedTest.java` | AC2; ADR-061-01 | terminal GREEN/YELLOW/RED | project each risk; assert allowlisted fields and absence of raw content | one row/intake; creation audit once | raw symptom/prose/token copied or row missing | PASS — Story 6.7 Backend `20/20` |
| `S67-TC-008` CRITICAL | transactional listener — `Story67LifecycleContractRedTest.java`, `Story67SafetyOutcomePostgresRedTest.java` | AC2/5; ADR-061-02 | conversation terminal | publish completion; inject projection failure; assert terminal transaction fails; retry succeeds | first attempt commits none; retry commits intake/result/projection once | false success or lost projection | PASS — rollback/retry PostgreSQL test |
| `S67-TC-009` CRITICAL | concurrent exact-once — `journey/Story67SafetyOutcomePostgresRedTest.java` | AC2/5; R-OV01-06 | PostgreSQL 16, same intake | race duplicate handlers/callbacks; assert count=1 and audit=1 | one projection; no aborted transaction workaround | duplicates, swallowed unique error, multiple audit | PASS — PostgreSQL `6/6` |
| `S67-TC-010` HIGH | migration constraints/append-only — `journey/Story67SafetyOutcomePostgresRedTest.java` | TDS §5.2 | empty PostgreSQL + complete Flyway chain | inspect composite journey-owner/intake-owner/emergency-owner FKs, checks, indexes and trigger; try cross-owner insert, invalid enums, update/delete/source delete | cross-owner/invalid/mutation operations rejected; valid row preserved; V1 checksum unchanged | missing owner FK, baseline drift, or cascade erases evidence | PASS — fresh Testcontainers/Flyway rerun |
| `S67-TC-011` HIGH | minimum-data/log scan — `Story67LifecycleContractRedTest.java`, `Story67SafetyOutcomePostgresRedTest.java`; sanitized manual summary | AC2/5; BR-PRIVACY | synthetic sentinel strings | complete intake and inspect row/DTO/audit/log capture | sentinels absent outside source triage | payload/token/owner UUID appears in projection or telemetry | PASS — automated allowlist + sanitized evidence |
| `S67-TC-012` CRITICAL | RED association and Story 6.6 preservation — `Story67SafetyOutcomePostgresRedTest.java`; affected emergency regressions | AC3; Story 6.6 | RED intake + existing/reused emergency | complete RED; assert projection has intake+emergency IDs; verify AI/emergency/outbox call counts | one association, one projection, no extra open/notification | second AI/POST/emergency/outbox or missing source ID | PASS — Backend final `137/137` |

### 4.3 Continuation and Security Cases

| ID / Severity | Feature / Actual Test File(s) | Oracle | Preconditions | Arrange / Act / Assert | Expected Persistence / Side Effects | Failure Signature | Status |
|---|---|---|---|---|---|---|---|
| `S67-TC-013` HIGH | resolve/ack state — `triage/TriageContinuationServiceTest.java`; `story_6_7_continuation_restore_test.dart` | AC4/5; ADR-061-03 | active owner token + fixed Clock | resolve repeatedly; render marker; acknowledge twice; advance beyond TTL | ack timestamp set once; replay 200; expired resolve neutral 404 | consume-before-render, non-idempotent ack, TTL ignored | PASS — Backend continuation `4/4` + focused Mobile |
| `S67-TC-014` CRITICAL | token IDOR/open-redirect/logging — `Story67LifecycleContractRedTest.java`, `TriageContinuationServiceTest.java`, `story_6_7_continuation_restore_test.dart` | BR-RBAC/PRIVACY; AC5 | owner/foreign/malformed tokens | call endpoints as guest/foreign/owner; send unknown URL field; inspect logs | 401 or uniform 404/409; no state mutation/body logging | foreign descriptor, arbitrary URL, token in access log | PASS — malformed string, neutral errors, unknown-field rejection, strict action pairing |

### 4.4 Unified Timeline Cases

| ID / Severity | Feature / Actual Test File(s) | Oracle | Preconditions | Arrange / Act / Assert | Expected Persistence / Side Effects | Failure Signature | Status |
|---|---|---|---|---|---|---|---|
| `S67-TC-019` HIGH | global timeline — `Story67SafetyOutcomePostgresRedTest.java`; `story_6_7_continuation_restore_test.dart` | AC2/4; ADR-061-04 | interleaved transitions/outcomes with tie timestamps | request page sizes 1/20/100 and adjacent pages; assert discriminator and stable sort tuple | read-only; no duplicate/skip across pages; `/history` unchanged | client-merged ordering, unstable ties, IDOR, regression | PASS — PostgreSQL global paging + Flutter all-page consumption |

### 4.5 Flutter and Device Cases

| ID / Severity | Feature / Actual Test File(s) / Evidence | Oracle | Preconditions | Arrange / Act / Assert | Expected Persistence / Side Effects | Failure Signature | Status |
|---|---|---|---|---|---|---|---|
| `S67-TC-015` HIGH | five-origin request/return — `story_6_7_lifecycle_origin_contract_test.dart`, `story_6_7_continuation_restore_test.dart` | AC1/4; UX | PRE/PREG/POST + current INFANT/TODDLER origins | pump each production screen; start/finish GREEN/YELLOW; assert exact journey/baby body and fixed return | token saved then acknowledged/cleared; no emergency for GREEN/YELLOW | mixed origin, wrong ID, missing stage lock | PASS — focused Mobile `85/85` |
| `S67-TC-016` CRITICAL | restart-safe RED — `story_6_7_continuation_restore_test.dart` + affected result/emergency/router tests | AC3/4; Story 6.6 | terminal RED + active emergency + stored token | destroy/rebuild app/auth; resolve; resume existing emergency; return/ack | GET active only; no MANUAL POST; one projection/outbox | second POST/AI, origin lost, 115 removed | PASS automated + Android MAN-028; one intake/outcome/emergency/outbox before and after restart/return |
| `S67-TC-017` CRITICAL | logout/account switch/late response — `story_6_7_continuation_storage_test.dart`, `story_6_7_continuation_restore_test.dart` | AC5; R-OV01-11 | Mother A token, delayed A response, login B | switch/logout while request pending; complete late future; assert B sees no A token/data | A key removed; queued write blocked; no cross-account navigation | flash/resurrection/cross-account route | PASS — generation guard + account-aware fingerprint tests |
| `S67-TC-018` HIGH | Mobile network retry — focused affected tests + `_bmad-output/test-artifacts/story-6-7-manual/manual-run-summary.md` | AC5; MAN-031 | offline/5xx/delay then recovery | start/complete/resolve under failures; assert inline retry/no false success; recover | same key/token; one projection after reconnect | lost input, duplicate write, late overwrite | PASS — Android MAN-031 Story 6.7 slice + exact-one DB evidence |
| `S67-TC-020` HIGH | accessible confirmation/timeline destination — `story_6_7_continuation_restore_test.dart` + focused UI regressions | UX/UI Skill; AC4 | result/timeline/error states | widget semantics, 200% text, grayscale/non-color cues, control size; inspect changed golden if any | no data side effect | clipped CTA, unlabeled status, color-only risk, target <48px | PASS automated scope — destination-owned accessible confirmation and render-before-ack |
| `S67-TC-021` CRITICAL | Story 6.6 regression suite — affected Backend `117/117`; focused Mobile `85/85` | AC3/6 | existing 6.6 fixtures | run RED ordering, association, active uniqueness, outbox, five origins, postpartum, 115/no-second-POST | cardinalities unchanged except one new projection per new bound intake | any prior invariant fails | PASS — no affected safety regression |
| `S67-TC-022` CRITICAL | Android evidence — `_bmad-output/test-artifacts/story-6-7-manual/{manual-run-summary,db-evidence}.md` | AC6; manual guide | sanitized synthetic devices/accounts | execute five GREEN origins, GREEN/YELLOW/RED restart/duplicate, offline retry; query DB | exact one projection/outcome, correct origin, no leaked token, sanitized evidence | unexecuted/unsanitized or cardinality mismatch | PASS — MAN-023, MAN-028 and Story 6.7 MAN-031 completed with final Android/PostgreSQL evidence |

### 4.6 Props Isolation Boilerplate

Each JUnit/Flutter test uses a fresh factory/fixture and fixed `Clock`/token generator. No mutable entity, fake client, secure-storage state, or authenticated singleton is shared across tests. `setUp` resets `FlutterSecureStorage` and auth; Testcontainers tests use unique UUID namespaces and transaction/cleanup isolation.

---

## 5. Red-Green-Refactor Tracker

| Case Group | Actual Test Files / Evidence | RED Confirmed | GREEN Evidence | Refactor Note |
|---|---|---|---|---|
| 001–006 | `Story67LifecycleContractRedTest.java`; affected triage/controller tests; `story_6_7_lifecycle_origin_contract_test.dart` | Yes — intended contract failures were observed during authoring; no standalone RED log was committed | Backend contract `10/10`; included in Backend final `137/137`; focused Mobile `85/85` | Locked stage added to binding; replay uses `Objects.equals`; unknown start fields fail closed |
| 007–014,019 | `Story67LifecycleContractRedTest.java`, `TriageContinuationServiceTest.java`, `Story67SafetyOutcomePostgresRedTest.java`, `story_6_7_continuation_restore_test.dart` | Yes — missing/incorrect projection, malformed-token, rollback, and paging behavior failed before fixes; no standalone RED log was committed | Story 6.7 Backend `20/20`; PostgreSQL `6/6`; focused Mobile `85/85` | Token parses in service for `TRIAGE-014`; infrastructure errors propagate; timeline proves interleaved global pagination |
| 015–021 | three `story_6_7_*_test.dart` files plus affected symptom/result/emergency/router/home/auth suites | Yes — late-account write, source-owned ack, one-page timeline, action mismatch, same-route delivery, phantom token, transient retry, and token-log checks failed before refactor; no standalone RED log was committed | focused Mobile `85/85` baseline + `70/70` final post-review; full Flutter `342/342`; affected Backend `117/117` baseline + `80/80` post-review | generation guard/fingerprint, destination-owned arrival, render-before-ack, all-page loading, strict descriptor validation, keyed same-route rebuild, account-bound emergency responses, and retry-in-place |
| 022 | `_bmad-output/test-artifacts/story-6-7-manual/` | N/A — manual acceptance | MAN-023, MAN-028 and MAN-031 PASS | Final Android rerun caught and corrected same-route Mother acknowledgement; evidence proves all five origins plus RED restart/return exact-once |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

Do not introduce a shared empty stub that breaks existing Story 6.6 code. Add the new contracts/tests against the current implementation; every new assertion must fail for the missing lifecycle/projection/continuation behavior while existing regressions remain green.

```powershell
# Backend focused verification (the same contracts that were RED before implementation)
Set-Location 05_Development/CareBridgeAPI
.\mvnw.cmd -Dtest=Story67LifecycleContractRedTest,TriageContinuationServiceTest,Story67SafetyOutcomePostgresRedTest test

# Mobile Story 6.7 focused verification
Set-Location ..\CareBridgeMobileApp
flutter test test/features/aiTriage/story_6_7_lifecycle_origin_contract_test.dart test/features/aiTriage/story_6_7_continuation_storage_test.dart test/features/aiTriage/story_6_7_continuation_restore_test.dart
```

**Red Gate evidence status:** no standalone `red-gate-evidence.log` was committed. The implementation session recorded intended RED failures and the test filenames retain the RED lineage, but this document does not invent a missing durable artifact. Current durable evidence is the implemented test source plus the GREEN counts above.

The sensitivity gate is accepted for the implemented automated cases because the assertions caught concrete defects during refactor (malformed UUID handling, over-broad exception mapping, account-switch late writes, premature acknowledgement, partial paging, and dashboard/action mismatch). Manual case 022 remains outside this automated gate.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] Story 6.7, UC61 TDS, and this Test-Spec are human-approved.
- [x] ADR-061-01..04, seven-day default TTL, UC61 naming trace, and checksum-stable forward-migration synchronization rule are approved.
- [x] Dirty-worktree baseline manifest exists before production edits.
- [x] PostgreSQL/Testcontainers, Android emulator, and Flutter tooling are available and have executed Story 6.7 evidence.
- [x] All recorded fixtures/evidence are synthetic and sanitized; actual target test paths/contracts are mapped in §4.
- [ ] Privacy/DPO production-release review remains required; it is not an implementation entry blocker.

### Exit Criteria

- [x] Automated cases 001–021 are mapped to implemented tests and passing; concrete RED defects and refactors are recorded in §5. A standalone RED log was not persisted and is not claimed.
- [x] Automated P0/critical pass rate is 100% in the recorded Story 6.7 and affected safety suites; no skipped relevant test is reported.
- [ ] Changed/in-scope coverage is at least the approved Epic 6 target (80%); no unsupported repository-wide percentage claim.
- [x] Backend final affected baseline passes `137/137` and post-review suite passes `80/80`; full Flutter passes `342/342`; focused Mobile passes `85/85` baseline plus `70/70` final post-review; standard Spring Boot package and debug APK build. Full Flutter analyze has two unrelated warnings and repository-wide Maven tests retain unrelated baseline failures, so no repository-wide clean claim is made.
- [x] PostgreSQL proves migration/FK/check/append-only/concurrency/rollback/global pagination in `Story67SafetyOutcomePostgresRedTest` `6/6` on fresh Testcontainers/Flyway.
- [x] Automated allowlists and sanitized Android/API/PostgreSQL evidence contain no continuation token, raw health payload, arbitrary route, or unsanitized owner identity.
- [x] OV01-MAN-023, MAN-028, and Story 6.7 MAN-031 evidence is complete.
- [x] Graph review and independent verifier have no unresolved High/Medium findings; final re-review `41/41` PASS and verdict `APPROVE`.

Manual qualification: MAN-023, MAN-028, and the Story 6.7 slice of MAN-031 are PASS with final Android/PostgreSQL evidence. Independent verification is complete; coverage and production Privacy/DPO review remain separate release gates.

### Suspension Criteria

- TDS/ADR approval missing; dirty baseline cannot be attributed; migration target conflicts; PostgreSQL unavailable; new pediatric/product rule required; any safety/ownership/data-leak regression; or three consecutive implementation failures. Missing Privacy/DPO approval suspends production release, not completed development verification.

---

## 7. Rollback Plan

- During RED, revert only newly added test scaffolds with reviewed patches; never reset the dirty workspace.
- After deployment, roll application code back to the prior build while retaining additive nullable columns and append-only projection evidence.
- Never delete Flyway history or drop production tables to “rollback.” Use a forward corrective migration after approval.
- Re-run Story 6.6 emergency and owner-isolation smoke tests after any rollback.
- Keep Story 6.7 open and preserve failure evidence if the gate cannot be met.

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP ID | Anti-Pattern | Detection in This Spec | Review Gate |
|---|---|---|---|
| AP-AI-001 | Unconstrained generation | case lacks AC/ADR/BR oracle | reject before RED |
| AP-AI-002 | Green from birth | new test passes current missing implementation | rewrite test |
| AP-AI-003 | Implicit decision | test invents age cutoff, SLA, role, TTL, or route | require approval/TDS update |
| AP-AI-004 | Layer violation | controller test expects business logic in controller | move oracle to service/policy |
| AP-AI-005 | Hallucinated contract | file/symbol/import not in approved TDS or current repo | contract review before coding |
| AP-AI-006 | Weak exact-once proof | Mockito call count substitutes for PostgreSQL concurrency | require Testcontainers evidence |
| AP-AI-007 | Sensitive test evidence | token/health/owner data appears in logs/screenshots | block and sanitize |

**Review result:** authoring/refactor and independent review found and corrected concrete AP-AI-002/003/005-adjacent defects. Final graph-backed independent re-review passed `41/41` and issued `APPROVE` with no unresolved High/Medium findings.

| Detected AP | Case | Fix Action | Fixed |
|---|---|---|---|
| AP-AI-002 risk | 017,019 | Added late-account persistence and all-page timeline tests that failed before fixes | `[x]` |
| AP-AI-003 risk | 014 | Require and validate approved dashboard/action pair; reject synthesized/unknown action | `[x]` |
| AP-AI-005 risk | 001,013,015–019 | Remapped all cases to actual Story 6.7 test files and implemented contracts | `[x]` |
| Final independent review | All | Complete graph-backed verifier pass and resolve every High/Medium finding | `[ ]` |

---

*This Approved Test-Spec preserves the PHASE-4 major structure and records current implementation evidence. Manual cases MAN-023, MAN-028, Story 6.7 MAN-031, and independent code review are complete; coverage and production privacy review remain open release gates.*
