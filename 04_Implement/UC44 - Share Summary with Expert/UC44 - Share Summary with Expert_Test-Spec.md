# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-44 Share Summary with Expert — Story 6.8

| Field | Value |
|---|---|
| **Document ID** | `CB-CONSULT-IMP-044-S68-TEST` |
| **Version** | `1.0` |
| **Date** | `2026-07-23` |
| **Status** | `Approved` |
| **Function** | `UC-44 Share Summary with Expert` |
| **Story / Gap** | `Story 6.8 / FR52 / OV01-GAP-08` |
| **Platforms** | `Spring Boot Backend + PostgreSQL + Flutter Mobile` |
| **TDS** | `CB-CONSULT-IMP-044-S68` |
| **Owner** | `CareBridge Test Architecture / Consultation Domain` |
| **Author** | `Codex — Implementation Agent` |
| **Reviewed by** | `Independent Specification Verifier — PASS, no High/Medium findings` |
| **Implementation review** | `bmad-code-review — APPROVE, no unresolved High/Medium findings` |
| **Approved by** | `Product authority — explicit conditional pre-approval satisfied 2026-07-23` |
| **DPO sign-off** | `[ ] Pending — production release gate, not represented as complete` |
| **Template** | `PHASE-4_Test-Spec.md / TDD Template v2.0` |

---

## CHANGELOG

| Date | Author | Change |
|---|---|---|
| 2026-07-23 | Codex — Implementation Agent | Created the companion Test-Spec using `.agents/workflows/create-specs.md` and the mandatory Phase 4 skeleton. |
| 2026-07-23 | Product authority | Confirmed the fully documented 242-UC corpus is authoritative and the 121-UC scope is experimental only. |
| 2026-07-23 | Independent Specification Verifier / Product authority | Resolved 2 High and 5 Medium findings; final PASS with no High/Medium blockers; conditional approval applied. |
| 2026-07-23 | Backend/PostgreSQL implementation review | Replaced invalid V1-only parity oracle with full-history schema verification plus V1 forward-ownership sentinel pairing; independent re-review pending. |
| 2026-07-23 | Independent Specification Verifier / Product authority | Schema correction PASS with no High/Medium blockers; conditional approval restored. |
| 2026-07-23 | Backend/PostgreSQL runtime review | Proved comment-only V1 sentinel changes an applied Flyway checksum; restored V1 and moved ownership metadata to an external manifest pending independent re-review. |
| 2026-07-23 | Independent Specification Verifier / Product authority | Applied-checksum correction and pinned canonical V1 digest PASS; no High/Medium blockers; conditional approval restored. |
| 2026-07-23 | Codex — Implementation Agent / Independent Code Reviewer | Synchronized final evidence: Expert accepted-queue detail navigation fixed and independently re-reviewed `APPROVE`; Backend 210 unique green tests; consultation focused 11/11 and full Mobile 364/364 green; final APK hash; OV01-MAN-024 PASS with sanitized count-only PostgreSQL evidence. |

---

## MỤC LỤC

1. Thông tin Module
2. Logic Issues Resolved
3. Test Design Specification (TDS)
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry / Exit Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

---

## 1. Thông tin Module

| Field | Value |
|---|---|
| **Module / Feature** | `Consented YELLOW Triage Expert Handoff` |
| **Authoritative UC** | `UC-44 Share Summary with Expert` from the 242-UC corpus |
| **Primary actor** | `MOTHER`; assigned `EXPERT` is a participant reader |
| **Trigger** | Mother selects expert support from a completed owned YELLOW result |
| **Outcome** | Exactly one lightweight consultation request, one action-specific consent receipt, one immutable minimum context share, and approved-source snapshots |
| **Data classification** | Sensitive internally; minimum pseudonymous clinical context in the participant response |
| **Primary TDS** | `04_Implement/UC44 - Share Summary with Expert/UC44 - Share Summary with Expert_TDS.md` |
| **Source authority** | User clarification → approved 242-UC requirements/ACs → TDS ADRs → current code/schema evidence |
| **Out of scope** | Booking/payment/refund, video/voice, expert verification administration, full health summary, Story 6.9 content policy |
| **Current phase** | Implementation, automated verification, Android OV01-MAN-024, artifact synchronization, and independent review complete |

The experimental 121-UC documents are explicitly excluded as implementation and acceptance oracles. In particular, their reuse of `UC-44` for vaccination cannot rename, redirect, or alter this Function. Legacy artifacts under `04_Implement/UC44_ShareSummaryWithExpert/`, `ExpertConsultationRequests/`, and `MotherExpertDiscoveryInbox/` are supporting evidence only.

### 1.1 AI Generation Context (CASE 2.0)

| Item | Value |
|---|---|
| **Generation mode** | AI-assisted, human-authorized plan and decisions |
| **Constraint source** | Story 6.8 AC1–AC7; TDS ADR-S68-001…004; BR-PRIVACY; BR-RBAC; current contracts |
| **Required workflow** | `.agents/workflows/create-specs.md` |
| **Supporting skills** | `bmad-create-story`, `bmad-ux`, `ui-skill-system`; final implementation/review use `bmad-dev-story`, `bmad-code-review` |
| **Oracle rule** | Every expected value cites an AC, ADR, BR, API/error contract, or existing regression contract |
| **Red rule** | A new-behavior test must fail against the missing/no-op implementation for its intended reason |
| **Isolation rule** | Each test owns fixtures, clock, UUIDs, mocks, account generation, and database transaction/container state |
| **Privacy rule** | Synthetic identifiers/content only; never record real tokens, symptoms, notes, raw AI output, or identifiable screenshots/logs |

---

## 2. Logic Issues Resolved

| ID | Conflict / discrepancy | Evidence | Approved resolution | Test impact |
|---|---|---|---|---|
| `LI-01` | Experimental 121-UC names `UC-44` as vaccination while the full SRS names it Share Summary with Expert | User clarification; `3_Functional_Specification.md §3.3.1.21`; Function allocation | The 242-UC corpus and `UC-44 Share Summary with Expert` are authoritative; 121-UC is ignored as an oracle | Test basis and artifact identity use only authoritative UC44 |
| `LI-02` | Legacy UC44 assumes paid booking/full summary, while the reused generic request requires topic/description and optional windows | Legacy TDS/Test-Spec; current `CreateConsultationRequestRequest`; Story 6.8/FR52 | Compose the lightweight request with a dedicated context share and freeze server-neutral `topic="YELLOW triage expert support"`, `description="Consented minimum YELLOW triage context is available in the protected context view."`, and null preferred windows | Replay compares these exact constants; forbidden-field tests prevent clinical leakage/scope creep |
| `LI-03` | Existing YELLOW routed screen has doctor/clinic placeholders; the normal flow renders a separate inline result | Mobile source evidence / S68-AC1 | Both terminal YELLOW surfaces use one real typed handoff; GREEN/RED remain unchanged | Widget tests cover both surfaces and regressions |
| `LI-04` | Directory queries require approved/active profile but do not independently prove enabled/unlocked/non-suspended account | Current repository/service evidence / S68-AC2 | Directory and submit policy require profile verification/trust plus current account eligibility; submit rechecks under lock | Repository, controller, race, and widget fixtures include disabled/locked/suspended cases |
| `LI-05` | Generic consultation free text cannot prove the exact shared snapshot or consent | Current schema / ADR-S68-001/002 | Separate append-only context/citation tables and action-specific `ConsentGrant` in one transaction | Cardinality, FK, rollback, and participant-read tests |
| `LI-06` | Triage citation validation may retain `PENDING_REVIEW` or fabricated legacy metadata | Current triage implementation evidence / ADR-S68-002 | Resolve and lock authoritative `EvidenceSource`; share only current `APPROVED` metadata | Status matrix and source-approval race tests |
| `LI-07` | Client-authored summary/citations could be forged, stale, or excessive | Security/privacy analysis / ADR-S68-002 | Create DTO accepts only client key, expert ID, true consent, and exact policy version; server derives context | Unknown-field, overposting, and database structural tests |
| `LI-08` | Separate generic consent and request calls could commit partially | Current service boundaries / ADR-S68-003 | Outer transaction; stable lock order intake → expert → evidence; all writes and audit/event participate | Failure-injection and concurrency tests |
| `LI-09` | Existing Mobile async guards are uneven across directory/form/detail and account switches | Mobile evidence / ADR-S68-004 | Account ID + generation guards every preview/directory/selection/create/detail response | Delayed-response and A→B account-switch widget tests |
| `LI-10` | Template rollback examples use destructive SQL/git checkout, prohibited for production and unsafe in a dirty tree | Repository rules / TDS §12 | Only disposable-test cleanup and forward-fix/additive rollback; never delete production evidence or revert user work | Rollback section and evidence rules updated |
| `LI-11` | No approved numeric SLA or retention duration exists | TDS §4 | Do not invent numbers; verify bounded/indexed query shapes and exact functional recovery; retention remains DPO release gate | No arbitrary timing oracle; collect diagnostic latency only |
| `LI-12` | `recommendedAction` is visible to Mother but not required for the expert | Approved Story/TDS scope | Exclude it from persistence and participant DTO | Structural and serialization negative assertions |
| `LI-13` | Consent prose could be misread as a non-existent operation field | Current `ConsentGrant`, `ConsentDataType`, `ConsentPurpose` | Use exact fields `dataType=EXPERT_SHARED_DATA`, `purpose=SHARE`; no `operation`; freeze recipient/scope/policy/evidence-key mapping | Entity/integration tests assert exact grant values |
| `LI-14` | “Completed YELLOW” could include a session with no canonical lifecycle linkage although schema requires journey/origin | Epic linkage requirement; ADR-S68-003; TDS schema | Admission requires owned + completed + YELLOW + canonical journey/origin linkage; otherwise safe unavailable/retry state and zero writes | Backend and both Mobile surfaces cover missing-linkage state |
| `LI-15` | A pre-lock replay check alone leaves a concurrent loser vulnerable to a context unique violation | Existing consultation conflict-safe insert; ADR-S68-003 | Recheck after intake lock and obligatorily reload after generic create; replay winner aggregate or map changed/colliding intent to HANDOFF-009 | Concurrency test requires 201+200, never raw integrity failure |
| `LI-16` | Separate intake-owner and journey-owner FKs do not prove copied origin/stage/risk belong to that intake | Schema review | Add composite intake snapshot FK including journey/origin/stage/risk/status plus completed/YELLOW checks | Mismatch cases fail at PostgreSQL boundary |
| `LI-17` | Triage citation strings have no authoritative UUID/title/version mapping | Current citation map and `EvidenceSource` registry | Resolve HTTPS host to exact/longest label-boundary registry domain; lock/revalidate approved+reviewed+stage; snapshot only registry ID/organization/baseUrl/reviewedAt | Deterministic collision/status/race cases |
| `LI-18` | Create result lacked a complete Mobile/API model | API review | Freeze 201/200 envelope with request ID/status, replayed, original sharedAt, and exact context | Controller/serialization/replay tests share one oracle |
| `LI-19` | 500-character sanitized summary had no 501 behavior | Privacy boundary review | NFC, whitespace/control normalization, blank rejection, Unicode-code-point first-499+ellipsis truncation | 0/1/500/501 and surrogate tests |
| `LI-20` | Directly copying Story 6.8 DDL into executable V1 would reference tables created only by later Flyway migrations; copying prerequisites would then collide with applied migrations | Actual migration order and successful 98-migration RED harness startup | Keep all executable DDL in V20260723090000; external ownership-manifest handling is finalized by LI-21 | Prevents fresh-database ordering failure |
| `LI-21` | Even a comment-only V1 sentinel changes the checksum of an applied Flyway migration and can fail validation on existing databases; a two-phase test using the same mutated source would miss this | PostgreSQL/Flyway runtime evidence and independent review | Restore V1 byte-for-byte; pin its normalized-LF UTF-8 SHA-256 `EF0D1B28017BF32681924DED4AAF92D75427B5E5B8377B4A14F685A72CD62054` in the external manifest; assert it before pre-Story-to-current validation/migration; verify manifest pairing and unique executable ownership | Preserves existing-database startup and detects any V1 whitespace/comment/content drift |

No unresolved logic issue currently changes the test oracle. Independent specification review remains required before approval.

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

In scope:

- Backend policy/service/controller tests for preview, create/replay, and participant context read.
- PostgreSQL full-history migration, pre-Story-to-current checksum compatibility, external-manifest/forward-owner pairing, unchanged-V1/no-duplicate-DDL, constraint, trigger, isolation, rollback, and concurrency tests.
- Expert directory current-eligibility and submit-time revalidation tests.
- Flutter models/service/body serialization, typed routing, both YELLOW surfaces, consent, account generation, participant detail, accessibility, and safe recovery tests.
- Regression tests for generic consultations, GREEN/RED safety, Story 6.7 origins/continuation/timeline, consent, and expert discovery.
- OV01-MAN-024 Android execution with sanitized API/database cardinality evidence.

Out of scope:

- Production deployment, production database mutation, real expert/mother data, payment/booking/video-call tests, verification-admin workflows, and Story 6.9.
- A numeric performance/availability SLA or retention test until an approved source defines it.
- Firebase; the feature uses existing backend consultation notification behavior and introduces no Firebase contract.

Layer applicability:

| Layer | Applicable | Main objective |
|---|---:|---|
| Java unit | Yes | Policy, intent comparison, minimization, authorization, errors |
| MockMvc/controller | Yes | Auth/role/DTO/HTTP/neutral IDOR contracts |
| JPA/PostgreSQL integration | Yes | Migration, linkage, concurrency, atomicity, triggers |
| Existing consultation regression | Yes | Preserve lifecycle/idempotency/after-commit behavior |
| Flutter model/service | Yes | Exact request/response serialization and forbidden fields |
| Flutter widget/navigation | Yes | UX, consent, account isolation, Back/continuation, accessibility |
| Android manual | Yes | Real device/emulator end-to-end OV01-MAN-024 |
| Web | No | Story 6.8 exposes no Web UI |

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Basis ID | Source | Oracle supplied |
|---|---|---|
| `B-UC44` | `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.21` | Function/actor/consented share identity |
| `B-ALLOC` | `04_Implement/implement_artifacts/function-spec-task-allocation.md` | Authoritative 242-UC allocation |
| `B-FR52` | Epic 6 / FR52 / OV01-GAP-08 | Real verified-expert routing with approved minimum context |
| `B-AC` | Story 6.8 AC1–AC7 | Full behavioral acceptance |
| `B-ADR1` | TDS ADR-S68-001 | Aggregate composition and compatibility |
| `B-ADR2` | TDS ADR-S68-002 | Fixed policy `YELLOW_EXPERT_CONTEXT_V1`, allowlist/exclusions |
| `B-ADR3` | TDS ADR-S68-003 | Atomic transaction, locks, exact-once, rollback |
| `B-ADR4` | TDS ADR-S68-004 | Typed route, account generation, continuation independence |
| `B-API` | TDS §§8–10, 16 | DTOs, endpoints, statuses/errors, authorization |
| `B-SCHEMA` | TDS §5.2, full Flyway history, unchanged applied V1, and external Story 6.8 schema-ownership manifest | Tables, keys, checks, triggers, fresh-history validity, checksum compatibility, manifest/forward pairing, no duplicate executable DDL |
| `B-UX` | TDS ADR-S68-004; `ui-skill-system`; design system | Warm Claymorphism, 48px targets, semantics, 200% scale |
| `B-REG` | Current consultation/triage/directory/continuation test contracts | Brownfield compatibility |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Risk | Condition / coverage item | Oracle | Primary cases |
|---|---|---|---|---|
| `S68-TC-COND-001` | Critical | Owned completed lifecycle-bound YELLOW preview returns exact allowlist and canonical summary | AC1/3/4; ADR2 | TC-001, 002, 014 |
| `S68-TC-COND-002` | Critical | Foreign, missing, incomplete, failed, GREEN, RED, or unbound intake fails with zero writes | AC1/5; API | TC-003, 004 |
| `S68-TC-COND-003` | Critical | Only approved/trusted/enabled/unlocked/unsuspended experts appear and can submit | AC2; ADR3 | TC-005, 006 |
| `S68-TC-COND-004` | Critical | Consent starts unchecked; refusal/cancel/back/dismiss/false creates zero side effects | AC3 | TC-007, 008 |
| `S68-TC-COND-005` | Critical | Create body and stored/API snapshot exclude every forbidden field | AC3/4; ADR2 | TC-009, SEC-001 |
| `S68-TC-COND-006` | Critical | Only authoritative currently approved citations are shared | AC4; ADR2 | TC-010, INT-005 |
| `S68-TC-COND-007` | Critical | Successful create atomically links request/consent/context/citations/intake/journey/origin/expert and uses exact neutral generic request/consent fields | AC4; ADR1/2/3 | TC-009, INT-001, INT-002 |
| `S68-TC-COND-008` | Critical | Same key/same intent is exact-once under sequential/concurrent retry | AC5; ADR3 | TC-011, INT-003 |
| `S68-TC-COND-009` | High | Same key/changed intake, expert, or policy conflicts without new rows | AC5; HANDOFF-009 | TC-012 |
| `S68-TC-COND-010` | Critical | Persistence failure rolls back request/consent/context/citations/audit and prevents notification | AC5; ADR3 | INT-004 |
| `S68-TC-COND-011` | Critical | Expert eligibility loss race and source approval loss race fail closed | AC2/4/5 | INT-005, INT-006 |
| `S68-TC-COND-012` | Critical | Mother and assigned Expert read same allowlist; outsider gets neutral 404; revoked/expired consent fails closed | AC4/5; auth matrix | TC-013, SEC-002 |
| `S68-TC-COND-013` | Critical | A→B switch and late responses cannot preview/post/render/navigate A data | AC5; ADR4 | MOB-005, MOB-006 |
| `S68-TC-COND-014` | High | Inline and routed YELLOW show real handoff; no YELLOW placeholders | AC1 | MOB-001, MOB-002 |
| `S68-TC-COND-015` | High | Back/refuse/retry/success preserves YELLOW result and Story 6.7 continuation | AC6; ADR4 | MOB-003, MOB-004 |
| `S68-TC-COND-016` | High | Loading/error/offline/empty/eligibility-loss copy is truthful and retry-safe | AC6 | MOB-007 |
| `S68-TC-COND-017` | High | Accessibility, non-color state, focus, semantics/live region, 200% scale | AC6; UX | MOB-008 |
| `S68-TC-COND-018` | High | Schema constraints/indexes/triggers, full fresh-history validity, pre-Story checksum compatibility, unchanged V1, external manifest/forward pairing, and no duplicate executable DDL | ADR3; TDS §5.2 | INT-007, INT-008 |
| `S68-TC-COND-019` | High | GREEN/RED, generic consultation, directory, consent, timeline regressions remain green | AC1/6/7 | REG-001…004 |
| `S68-TC-COND-020` | Critical | OV01-MAN-024 proves refusal 0/0/0, approved retry 1/1/1, allowlist, races, account safety | AC7 | MAN-024 |

Coverage mapping:

| Story AC | Conditions |
|---|---|
| AC1 | 001, 002, 014, 019 |
| AC2 | 003, 011 |
| AC3 | 001, 004, 005 |
| AC4 | 005, 006, 007, 012 |
| AC5 | 002, 008, 009, 010, 011, 012, 013, 018 |
| AC6 | 014, 015, 016, 017, 019 |
| AC7 | 001–020 |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique | Application | Rationale |
|---|---|---|
| Equivalence partitioning | risk state, intake ownership/status, expert eligibility, citation status, viewer role | Covers valid and invalid policy classes without combinatorial waste |
| Boundary analysis | 0/1/500/501 summary length; empty/one/multiple/duplicate citations; text scale 1.0/2.0 | Validates fixed limits and UI resilience |
| Decision tables | expert verification × trust × enabled × locked × suspension; consent × policy × ownership | Makes fail-closed combinations explicit |
| State transition | YELLOW → discovery → consent → submit → shared; refusal/error/back transitions | Ensures no premature `SHARED` or continuation acknowledgement |
| Pairwise | device size/text scale/network state/account switch timing | Efficient Mobile interaction coverage |
| Property/structural assertions | request keys and serialized/persisted field names | Proves forbidden context cannot be overposted/copied |
| Concurrency scheduling | barriers/latches around expert and evidence locks; parallel same key | Reproduces TOCTOU and exact-once races deterministically |
| Fault injection | context/citation/audit persistence and notification boundary | Proves whole-aggregate rollback and after-commit behavior |
| Authorization/abuse testing | guest/wrong role/foreign owner/outsider/expired consent/unknown fields | Protects IDOR, mass assignment, and privacy boundaries |
| Regression selection | graph impact plus named direct suites | Graph test mapping is incomplete, so named suites supplement it |

### TDS-05 — Test Data Requirements

All values are synthetic and deterministic.

| Fixture | Required variants |
|---|---|
| Mother accounts | `motherA`, `motherB`; authenticated generation counter; no real PII |
| Experts | approved+active+enabled; pending; rejected; restricted; suspended; inactive; disabled; locked; eligibility revoked mid-flight |
| Intake sessions | Mother A/B ownership; STARTED/COMPLETED/FAILED; GREEN/YELLOW/RED; complete/missing journey/origin; five Story 6.7 origins |
| Triage result | sanitized summaries at 0/1/500/501 chars; raw payload seeded with unique forbidden markers |
| Evidence sources | APPROVED, PENDING_REVIEW, DEPRECATED, missing, malformed HTTP, duplicate URL, approval revoked under lock |
| Consultation | new key; existing same-intent key; existing key with different intake/expert/policy; participant/outsider views |
| Consent | active exact grant; missing; revoked; expired; wrong recipient/scope/policy/evidence key |
| Mobile transport | controllable completers for preview/directory/profile/create/detail; offline, timeout, late success/error, reordered generations |

Control rules:

- UUIDs use fixed reserved values or per-test factories; never share mutable fixture objects between tests.
- Java clock is fixed/injected for expiry and `sharedAt`; PostgreSQL timestamps are asserted by ordering/range, not wall-clock equality.
- Each integration test uses rollback where compatible or a fresh Testcontainers schema; concurrency tests clean only their synthetic rows in the disposable database.
- Each Flutter test creates fresh fake services, auth state, router, and request generation in `setUp`; complete pending futures before teardown.
- Logs/screenshots/XML use `[REDACTED]` tokens and synthetic summaries. Forbidden marker assertions search outputs but never print marker payload values.

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

Backend tests use a factory per test:

```java
@Test
void example() {
    UUID ownerId = TestIds.newId();
    HandoffFixture fixture = HandoffFixture.validYellow(ownerId);
    // Arrange, Act, Assert remain local to this test.
}
```

Flutter tests create services inside each test:

```dart
testWidgets('example', (tester) async {
  final auth = FakeAuthState(accountId: motherA);
  final api = FakeTriageExpertHandoffService();
  await pumpSubject(tester, auth: auth, api: api);
});
```

No mutable DTO/entity/widget fake is declared globally or reused across cases.

### Functional and Policy Test Cases

| TC ID | Preconditions / Arrange | Act | Expected response / UI | Persistence, audit, event oracle | Intended test / Initial status |
|---|---|---|---|---|---|
| `S68-TC-001` | Mother A owns COMPLETED YELLOW with journey/origin, valid summary and approved source | GET preview | 200; policy V1; YELLOW, canonical stage, <=500 summary, approved metadata, exact shared/excluded labels | Read-only; 0 new rows/audits/events | `TriageExpertHandoffServiceTest` / Implemented; Backend 49-test gate green |
| `S68-TC-002` | Valid result also contains raw symptoms, notes, raw AI, claims, red flags, IDs, token/route markers | GET preview | None of the forbidden fields/markers serialize | 0 writes; logs contain no forbidden marker | `TriageExpertHandoffControllerTest` / Implemented; Backend 49-test gate green |
| `S68-TC-003` | Partition fixtures: missing, foreign-owner, GREEN, RED, STARTED, FAILED, missing lifecycle linkage | GET preview and POST create | Missing/foreign → neutral 404 HANDOFF-002; invalid state → 409 HANDOFF-003 | Each variant 0 request/consent/context/citation/success-audit/event | `TriageExpertHandoffControllerTest`, `TriageExpertHandoffServiceTest` / Implemented; green |
| `S68-TC-004` | Valid YELLOW but canonical server summary is null/blank after sanitation, with or without approved sources | Preview/create | 422 HANDOFF-008; citations cannot substitute for required summary; no claim of share | 0 writes/notification | `TriageExpertHandoffPolicyTest` / Implemented; green |
| `S68-TC-005` | Expert decision-table fixtures | Directory query | Only approved+active+enabled+unlocked+unsuspended appears/selectable | Read-only | Backend handoff suites + Flutter directory tests / Implemented; Backend 49/49 and canonical Mobile 18/18 green |
| `S68-TC-006` | Expert was selectable, then loses verification/trust/account eligibility before submit | POST create | 409 HANDOFF-004 with safe copy | 0 request/consent/context/citation/success audit/notification | `TriageExpertHandoffServiceTest`, `TriageExpertHandoffConcurrencyRollbackPostgresTest` / Implemented; green |
| `S68-TC-007` | Handoff screen loaded with valid preview/expert | Open consent | Checkbox false; submit disabled; exact allowlist/exclusions readable | No create call and no write | `triage_expert_handoff_mobile_test.dart` / Implemented; Mobile focused gate green |
| `S68-TC-008` | Consent sheet open | Refuse, cancel, Back, barrier dismiss; separately POST false/missing consent | UI returns/remains on safe YELLOW; API 400 HANDOFF-001 | Mobile makes 0 POST for UI exits; server variants 0 writes/audit/event | Mobile + controller tests / Implemented; automated green; Android refusal cardinality proof PASS |
| `S68-TC-009` | Valid consent; intercepted HTTP body and captured generic request adapter input | Submit | Mobile body contains exactly clientRequestId, expertProfileId, consentAccepted, consentPolicyVersion; path holds intake ID. Adapter supplies exact fixed topic/description and null windows | No client-authored context accepted; committed rows server-derived; consent uses exact dataType/purpose/recipient/scope/policy/evidence key | Mobile model/service + Backend controller/service/PostgreSQL tests / Implemented; green |
| `S68-TC-010` | Result URLs exercise exact domain, nested domain, ambiguous suffix, approved/pending/deprecated/missing-review/stage-mismatch, malformed, duplicate and unregistered sources | Preview/create/read | Exact domain else longest label-boundary match; only unique locked current APPROVED+reviewed+stage-applicable registry ID/organization/baseUrl/reviewedAt in first-occurrence order | Citation rows match registry set only; no client title/version/deep path/query/excerpts/evidence blobs | `TriageCitationResolverTest` + PostgreSQL/service tests / Implemented; green, including 5/5 PSL confirmation |
| `S68-TC-011` | Existing aggregate for owner/key/same intake/expert/policy | Replay sequentially | 200 with `replayed=true`, same request/status/original sharedAt/immutable context | Counts remain exactly 1 request/1 consent/1 context/one citation set; no second notification | Service/PostgreSQL concurrency tests / Implemented; green |
| `S68-TC-012` | Existing owner/key but changed intake, expert, or policy | Replay each changed intent | 409 HANDOFF-009 | No new rows/audit/event/notification | Service/PostgreSQL concurrency tests / Implemented; green |
| `S68-TC-013` | Committed aggregate; viewers are owner Mother, assigned Expert, another Mother/Expert; consent active/revoked/expired | GET participant context | Participants active → same allowlist; outsider → 404 HANDOFF-006; expert invalid consent/trust → 403 HANDOFF-007 | Read-only; response omits all internal linkage | Backend controller/service + `triage_expert_handoff_participant_test.dart` / Implemented; green |
| `S68-TC-014` | Server summaries: null, whitespace/control only, 1/500/501 Unicode code points, decomposed Unicode, repeated whitespace, surrogate-pair boundary | Preview/create | NFC; controls/formats removed; whitespace collapsed; blank → HANDOFF-008; 1/500 unchanged after canonicalization; >500 → first 499 code points + U+2026; valid Unicode and plain-text render | Only canonical <=500 summary may persist; invalid variant 0 writes | Policy/PostgreSQL/Flutter tests / Implemented; green |

Failure signatures:

- A test fails if a forbidden JSON key/column/marker appears, not only when its value differs.
- A zero-side-effect case fails on any count above zero or any after-commit notification capture.
- A replay case fails if identities differ, if HTTP status is not 200, or if any cardinality/audit/notification increments.
- Authorization cases fail if outsider and missing-resource responses are distinguishable by status/code/body shape.

### SECURITY TEST CASES

#### S68-TC-SEC-001 — Reject overposting and prove minimum storage

**Oracle Source:** S68-AC3/4, ADR-S68-002, TDS §§5.2, 9.2.

**Preconditions:** Valid owned YELLOW and expert; request JSON additionally contains `riskSummary`, `symptoms`, `rawAiResponse`, `ownerId`, `journeyId`, `origin`, `citations`, `recommendedAction`, and `continuationToken` synthetic fields.

**Arrange / Act / Assert:**

1. POST the overposted document and assert 400 `HANDOFF-001`, or—if global Jackson policy intentionally ignores unknown properties—assert via a structural test that those fields are absent from the DTO and cannot affect derived persistence.
2. Submit the valid four-field body.
3. Inspect response, context/citation schema, entity mappings, rows, audit capture, and sanitized logs.
4. Assert zero forbidden keys/columns/markers; assert summary comes from the locked server result.

**Side effects:** Invalid request creates zero rows. Valid request creates exactly the approved aggregate only.

**Failure signature:** Any client-supplied value reaches storage/API/log, or an unknown property changes the result.

**Implemented tests:** `TriageExpertHandoffControllerTest`, `TriageExpertHandoffPostgresIntegrationTest`. **Status:** Green in the 49-test Backend context gate.

#### S68-TC-SEC-002 — Authentication, role, ownership, participant and neutral-IDOR matrix

**Oracle Source:** BR-RBAC, S68-AC4/5, TDS §16.

**Preconditions:** Owned/foreign intakes, committed participant request, outsider request IDs, active and revoked consent.

**Act:** Execute all three endpoints as guest, Mother owner, other Mother, assigned Expert, other Expert, and Admin without an explicit Story override.

**Expected:** JWT required; only owner Mother previews/creates; only owner Mother and currently eligible assigned Expert read; all outsiders receive neutral not-found; role alone never bypasses ownership.

**Side effects:** Unauthorized calls create no rows/audits/events.

**Failure signature:** IDOR, role-only access, differentiated resource enumeration, or partial write.

**Implemented tests:** `TriageExpertHandoffControllerTest`, `TriageExpertHandoffServiceTest`. **Status:** Green in the 49-test Backend context gate.

#### S68-TC-SEC-003 — Injection, malformed identifier, URL and log hygiene

**Oracle Source:** TDS ADR-S68-002, §§9–10, 14.2.

**Act:** Send malformed UUIDs, oversized strings, HTML/control characters, unsupported policy values, HTTP/javascript citation URLs in source fixtures, and synthetic secret markers.

**Expected:** Validation-safe 400/404/422 responses without stack traces; unsafe citation omitted; UI renders text, not markup; local logs and evidence contain no body/token/raw marker.

**Side effects:** Zero writes for invalid requests.

**Implemented tests:** controller validation, `TriageExpertHandoffPolicyTest`, `TriageCitationResolverTest`, and Flutter rendering/model tests. **Status:** Green; targeted analyzer reports zero Story 6.8 issues.

### INTEGRATION TEST CASES

#### S68-TC-INT-001 — Atomic happy path and participant parity

**Oracle Source:** S68-AC4, ADR-S68-001/003, TDS §§5.2, 6.1.

Arrange a full synthetic lifecycle-bound YELLOW, approved expert/source, and new key. POST once, read as Mother and Expert, then query PostgreSQL. Expect 201 with `replayed=false` and the exact create envelope; same immutable allowlist for both participants; internal linkage matches the same intake snapshot's owner/journey/origin/stage/risk/completed state and expert; generic request has exact fixed neutral topic/description and null windows; consent has `dataType=EXPERT_SHARED_DATA`, `purpose=SHARE`, exact recipient/scope/policy/evidence key; counts `1/1/1`; citation count equals approved unique registry sources; one existing request-created audit/event and one after-commit notification; no forbidden column/key/marker.

**Implemented test:** `TriageExpertHandoffPostgresIntegrationTest`. **Status:** Green in the 49-test Backend context gate.

#### S68-TC-INT-002 — Constraint and linkage enforcement

**Oracle Source:** ADR-S68-003, TDS §5.2.

In a disposable PostgreSQL transaction, attempt mismatched owner/request/expert/key/intake/journey/origin/consent links, non-YELLOW risk, unsupported stage/origin/policy, duplicate request/context/consent/intake-expert, duplicate/unsafe citation, and UPDATE/DELETE of append-only rows. Each invalid operation must fail at the database boundary; valid fixtures commit.

**Implemented tests:** `TriageExpertHandoffPostgresIntegrationTest`, `TriageExpertHandoffSchemaRedPostgresTest`. **Status:** Green.

#### S68-TC-INT-003 — Concurrent same-key exact-once

**Oracle Source:** S68-AC5, ADR-S68-003.

Start two transactions for the same owner/key/intent behind a barrier. Release concurrently. Expect exactly one 201 `replayed=false` and one 200 `replayed=true`, no raw unique violation or deadlock, same request/status/original sharedAt/immutable context, counts `1/1/1`, one citation set, one success audit/event/notification. Repeat with different intakes but the same owner/key: one intent wins and the other returns HANDOFF-009 after the mandatory post-create reload. Seed a generic consultation with the same owner/key but no context: handoff returns HANDOFF-009 and never attaches context to it.

**Implemented test:** `TriageExpertHandoffConcurrencyRollbackPostgresTest`. **Status:** Green.

#### S68-TC-INT-004 — Failure-injection rollback and notification boundary

**Oracle Source:** S68-AC5, ADR-S68-003.

Inject failure separately at consent, context, citation and success-audit persistence before commit. Assert the request and all other synchronous rows roll back, no after-commit subscriber fires, and retry with the same key can later succeed exactly once. Do not simulate by throwing after commit.

**Implemented test:** `TriageExpertHandoffConcurrencyRollbackPostgresTest`. **Status:** Green.

#### S68-TC-INT-005 — Evidence approval race

**Oracle Source:** S68-AC4/5, ADR-S68-002/003.

Coordinate handoff and approval-status/review timestamp mutation using stable UUID lock order. Exercise exact-host versus longest label-boundary suffix selection and equal-length ambiguity fail-closed. If revocation owns the lock first, the source is excluded/fails safe with no unapproved snapshot. If handoff owns the lock first, commit contains only registry UUID/organization/baseUrl/reviewedAt from the then-authoritative row and later mutation cannot rewrite it. No client title/version/deep link is copied; no deadlock or partial rows.

**Implemented tests:** `TriageExpertHandoffConcurrencyRollbackPostgresTest`, `TriageCitationResolverTest`. **Status:** Green, including final 5/5 PSL confirmation.

#### S68-TC-INT-006 — Expert eligibility race

**Oracle Source:** S68-AC2/5, existing expert-lock semantics, ADR-S68-003.

Coordinate handoff and trust/account eligibility mutation. Mutation-first produces HANDOFF-004 and `0/0/0`; handoff-first commits consistently before mutation. Acceptance still revalidates expert eligibility through its existing lifecycle. No stale directory result bypasses submit policy.

**Implemented tests:** `TriageExpertHandoffConcurrencyRollbackPostgresTest`, `TriageExpertHandoffServiceTest`. **Status:** Green.

#### S68-TC-INT-007 — Full-history, applied-checksum compatibility and external ownership pairing

**Oracle Source:** create-specs schema-sync rule as safely corrected by TDS §5.2 and the actual Flyway dependency order.

Before starting Flyway, read V1 bytes, normalize CRLF/CR to LF, encode UTF-8 without BOM, and assert SHA-256 `EF0D1B28017BF32681924DED4AAF92D75427B5E5B8377B4A14F685A72CD62054` from the independently pinned manifest. Build a fresh PostgreSQL 16 database through the complete history including `V20260723090000`; assert all Story 6.8 objects and fixture behavior. In a second disposable database, migrate only through `V20260722210000`, then validate and migrate with the current sources; expect no checksum error and exactly the new forward migration applied. Source assertions require the external manifest to name both tables/version/reason/digest and the forward file to contain the sole executable `CREATE TABLE` definitions. Fail on any V1 digest drift, manifest drift, or duplicate executable Story 6.8 DDL.

**Implemented tests:** `TriageExpertHandoffPostgresIntegrationTest`, `TriageExpertHandoffSchemaRedPostgresTest`. **Status:** Green; full 99-migration history, pre-Story upgrade, unchanged-V1 digest, external-manifest pairing, and sole forward owner verified.

#### S68-TC-INT-008 — Query boundedness and deterministic ordering

**Oracle Source:** TDS §4.1/4.4.

Verify replay lookup, participant context/citations, approved-source lock query, and eligible directory use planned indexes, bounded/pageable results, and deterministic UUID/ordinal ordering. Record plans diagnostically; no invented millisecond threshold.

**Implemented tests:** Story 6.8 repository/PostgreSQL suites. **Status:** Green; named suites are authoritative because graph mapping cannot resolve new untracked handoff nodes.

### MOBILE TEST CASES

| TC ID | Scenario and assertions | Oracle | Intended test / Initial status |
|---|---|---|---|
| `S68-TC-MOB-001` | Inline terminal YELLOW shows one real verified-expert CTA, pushes typed handoff with only intake ID; no doctor/clinic placeholder; GREEN/RED CTA sets unchanged | AC1; ADR4 | `story_6_8_yellow_handoff_red_test.dart`, affected triage suites / Implemented; focused gate green |
| `S68-TC-MOB-002` | Routed YELLOW behaves equivalently; malformed/missing typed extra fails safely and URL contains no health/token/route context | AC1/5; ADR4 | `story_6_8_yellow_handoff_red_test.dart`, affected route suites / Implemented; focused gate green |
| `S68-TC-MOB-003` | Back/refuse/cancel/offline error returns to intact YELLOW; return-to-origin remains available and continuation is not acknowledged | AC3/6 | `triage_expert_handoff_mobile_test.dart` + affected continuation suites / Implemented; automated and Android manual PASS |
| `S68-TC-MOB-004` | Success/detail then Back preserves result; restart and return-to-origin follow Story 6.7 contract | AC6 | handoff/participant + affected Story 6.7 suites / Implemented; automated and Android restart/return PASS |
| `S68-TC-MOB-005` | Delay Mother A preview/directory/profile then switch to B; complete A futures | AC5; ADR4 | `triage_expert_handoff_mobile_test.dart` / Implemented; focused gate green |
| `S68-TC-MOB-006` | Switch before submit and during create/detail response | AC5; ADR4 | `triage_expert_handoff_mobile_test.dart`, `triage_expert_handoff_participant_test.dart` / Implemented; focused gate green |
| `S68-TC-MOB-007` | Loading, empty, offline, retry, eligibility loss, changed consent policy | AC6; API errors | `triage_expert_handoff_mobile_test.dart`, `triage_expert_handoff_model_service_test.dart` / Implemented; timeout gate 15/15 green |
| `S68-TC-MOB-008` | 430×932 and compact viewport at 100%/200% text; keyboard/focus/TalkBack semantics/live regions; risk/status has text/icon | AC6; UX/UI system | handoff widget semantics/layout tests / Implemented; focused gate green; Android TalkBack/manual view open |
| `S68-TC-MOB-009` | Intercept request serialization after consent and double tap | AC3/5; ADR2 | `triage_expert_handoff_model_service_test.dart`, handoff widget tests / Implemented; focused gate green |
| `S68-TC-MOB-010` | Mother and Expert detail render allowlisted context and approved metadata only; Expert accepted queue opens the detail route | AC4 | `triage_expert_handoff_participant_test.dart`, `consultation_request_mobile_test.dart` / Implemented; final focused consultation suite 11/11 and Android Expert view PASS |

Golden screenshots may cover the YELLOW CTA, eligible expert list and consent sheet at 430×932, but semantic/widget assertions are the acceptance oracle. Goldens must contain synthetic copy only.

### REGRESSION AND MANUAL CASES

| TC ID | Scope | Expected | Intended suite / Initial status |
|---|---|---|---|
| `S68-TC-REG-001` | Existing consultation create/replay/accept/reject/cancel/concurrency | Unchanged and green | Included in affected Backend 161/161 green |
| `S68-TC-REG-002` | Existing directory/profile/direct chat | Generic mode unchanged; current eligibility strengthened safely | Backend affected gate 161/161 and Mobile full 364/364 green |
| `S68-TC-REG-003` | Story 6.6 GREEN/RED and emergency flows | No YELLOW handoff for GREEN/RED; RED emergency unchanged | Mobile focused 47/47, full 364/364, and Android GREEN/RED smoke PASS |
| `S68-TC-REG-004` | Story 6.7 five origins, projection, restart, return, continuation | Handoff never consumes continuation; all remain green | Affected Backend 161/161, Mobile full 364/364, and Android restart/return PASS |
| `S68-TC-MAN-024` | Android OV01-MAN-024 | Refusal `0/0/0`; approved retry `1/1/1`; safe expert view; eligibility/account/back/restart/GREEN/RED checks pass | **PASS** — `_bmad-output/test-artifacts/story-6-8-manual/manual-run-summary.md` |

OV01-MAN-024 procedure:

1. Seed Mother A/B, one eligible expert, each ineligible expert class, one YELLOW with one approved and one pending source; use synthetic data.
2. Reach YELLOW from a real supported origin; verify safe guidance, real CTA, and absence of YELLOW placeholders.
3. Verify directory exposes only the currently eligible expert.
4. Inspect unchecked consent and exact allowlist/exclusions; refuse/cancel and prove database/API counts `0/0/0`.
5. Repeat, approve, double-tap/retry; prove exactly `1 request / 1 consent / 1 context` and one approved citation set.
6. Sign in as assigned Expert; verify only YELLOW, stage, sanitized summary and approved bibliographic metadata.
7. Revoke eligibility after selection; submit must fail closed with `0/0/0` for the new key.
8. Delay A responses, switch to B, and prove no A data/navigation appears.
9. Verify Back, restart and return-to-origin preserve/complete Story 6.7 correctly.
10. Smoke GREEN and RED; RED emergency remains unchanged and neither creates a handoff.
11. Capture sanitized screenshots/XML/log snippets and count-only SQL; inspect before saving evidence.

---

## 5. Red-Green-Refactor Tracker

| Cycle / TC group | Intended test path | RED run | GREEN run | Refactor run | Notes |
|---|---|---:|---:|---:|---|
| Backend policy/API | `src/test/java/.../consultation/context/` | `[x] 5 intended HTTP 404 assertion failures` | `[x] 49-test context gate green` | `[x]` | Controller remains mapping/validation-only; service owns policy/transaction |
| PostgreSQL migration/integration | backend integration test packages | `[x] 5 intended schema assertion failures` | `[x] included in 49/49` | `[x]` | Disposable PostgreSQL 16; full 99-migration history and upgrade path green |
| Expert eligibility | expert repository/service tests | `[x] layer-specific assertion failures captured` | `[x]` | `[x]` | Includes profile trust plus current user-account state and accept-time revalidation |
| Mobile handoff | `test/features/consultation/` | `[x] initial route/CTA acceptance RED` | `[x] focused 47/47; timeout 15/15` | `[x]` | Fresh fakes/auth generation and bounded timeouts |
| Both YELLOW surfaces | `test/features/aiTriage/` | `[x] 2 intended missing-CTA failures` | `[x]` | `[x]` | GREEN/RED contracts preserved in full 364/364 and Android smoke |
| Full regression/manual | named/full suites + OV01-MAN-024 | N/A | `[x] automated and manual green` | N/A | Backend 210 unique, Mobile 364 green, consultation navigation 11/11, OV01-MAN-024 PASS |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

Production code must not be changed until the initial new acceptance/structure tests compile and fail for the intended missing behavior. Existing behavior/regression tests are not required to fail. Do not add a production `@Service`, DTO, route, or throw-stub merely to manufacture RED.

Compileable test-only RED seams:

- Backend MockMvc tests use raw path/JSON and existing test infrastructure, without importing planned production classes; the absent endpoints fail with 404 instead of expected 200/201/validation statuses.
- PostgreSQL tests query metadata/DDL behavior by table and constraint name strings; the absent Story 6.8 tables/constraints fail the assertions.
- A test-only `Class.forName` contract-existence assertion may record missing planned classes, but the principal RED evidence is the executable HTTP/schema behavior above.
- Flutter tests exercise the existing inline/routed screens and search for the planned semantic key/CTA; the current missing control/route makes an assertion fail. No production route or fake service is introduced for RED.
- After this compileable acceptance RED is captured, production contracts may be added. Layer-specific unit tests are then added before their corresponding behavior and must fail at an assertion/test-double boundary, not on a hallucinated import or shared state.

Exact narrow commands will be updated with final test class/file names before execution; planned commands are:

```powershell
# Backend from 05_Development/CareBridgeAPI
.\mvnw.cmd -Dtest=TriageExpertHandoffServiceTest,TriageExpertHandoffControllerTest test
.\mvnw.cmd -Dtest=*TriageExpertHandoff*Postgres* test

# Mobile from 05_Development/CareBridgeMobileApp
flutter test test/features/consultation/triage_expert_handoff_mobile_test.dart
flutter test test/features/aiTriage/symptom_intake_screen_test.dart test/features/aiTriage/risk_triage_result_screen_test.dart
```

Red evidence requirements:

| Group | Expected RED signature | Actual | Evidence path |
|---|---|---|---|
| Backend contract/policy | Compile succeeds; MockMvc assertion is reached and the absent endpoint returns 404 instead of expected 200/201 (or another documented missing-behavior assertion mismatch) | `[x] 5/5 HTTP tests failed at reached status assertions; 0 errors/skips` | `_bmad-output/implementation-artifacts/evidence/story-6-8/red/backend.log` |
| PostgreSQL | Missing tables/constraints/triggers or expected concurrency/cardinality mismatch | `[x] 5/5 schema tests failed at reached catalog assertions; PostgreSQL/Flyway healthy` | `_bmad-output/implementation-artifacts/evidence/story-6-8/red/backend.log` |
| Flutter | Missing handoff CTA/route/consent/account guard | `[x] 2/2 tests failed at missing inline/routed CTA assertions; 0 compile/setup failures` | `_bmad-output/implementation-artifacts/evidence/story-6-8/red/mobile.log` |

Red Gate passes only when every initial new-behavior acceptance test compiles and fails for its intended production gap and no test passes due to tautology/shared state/mock-only assertions. The story/test records must capture the command, test count, and concise failure signature; evidence logs must be sanitized. Compile failure is not accepted as the Red Gate, and no production stub is permitted before it.

### 5.2 Current Green Evidence

| Gate | Current result | Evidence / limitation |
|---|---:|---|
| Backend context/schema/PostgreSQL | 8 suites / 49 tests green | Includes API/policy/service, PostgreSQL, race, rollback, fresh history, upgrade, checksum and forward-owner checks |
| Affected Backend regressions | 22 suites / 161 tests green | 30 unique suites / 210 unique Backend tests overall |
| Mobile focused/affected | 47/47 green | Story handoff and affected triage coverage |
| Expert queue/detail navigation | 11/11 green | Accepted queue exposes `Xem chi tiết` and navigates to allowlisted participant detail |
| Canonical `consultationEligible` patch | 18/18 green | Canonical field prioritized; legacy fallback retained; invalid values fail closed |
| Timeout/directory-timeout patch | 15/15 green | Both handoff HTTP and expert-directory waits are bounded; retry preserves the stable request key |
| Full Mobile suite | 364/364 green | Full regression rerun completed after Expert detail navigation fix |
| Static/format | targeted analyze: zero issues; final two-file format check: zero changes | Full analyzer retains two pre-existing unused-import warnings in clean unrelated `family_member_home_screen.dart` |
| APK | build succeeded | Size `254428175` bytes; SHA-256 `2B1942FF374F959422A6B9817AEF36FACCA2988F0D3866CCABF3CC8BEB4B7066` |
| Independent review | `APPROVE` | No unresolved High/Medium findings |
| Android OV01-MAN-024 | **PASS** | Sanitized XML plus count-only PostgreSQL evidence in `_bmad-output/test-artifacts/story-6-8-manual/` |

Authoritative evidence: `_bmad-output/implementation-artifacts/evidence/story-6-8/green/automated-verification.md` and `_bmad-output/implementation-artifacts/evidence/story-6-8/review/final-review.md`.

The graph was refreshed and explicit `detect_changes`, two-hop impact, affected-flow, and `tests_for` queries ran. The broad dirty worktree is scored high risk; new untracked Story nodes are missing from graph search, so affected flows are reported as zero and `tests_for` cannot resolve the new handoff nodes. The named direct suites above remain the authoritative executable coverage. The unfiltered Backend `mvnw clean package` compiled 1,334 production and 442 test sources but failed in 17 unrelated dirty-baseline classes; this is an open baseline failure, not a successful package gate.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] Function identity is confirmed as authoritative 242-UC `UC-44 Share Summary with Expert`.
- [x] Story 6.8 implementation plan and ordinary technical/UX/API/data decisions are explicitly pre-approved.
- [x] TDS `CB-CONSULT-IMP-044-S68` exists with API, schema, state, privacy and rollback contracts.
- [x] Logic discrepancies in §2 have approved resolutions.
- [x] Independent story review has no High/Medium findings.
- [x] Independent TDS/Test-Spec review has no High/Medium findings.
- [x] TDS and Test-Spec checksum correction was independently re-reviewed and statuses restored to `Approved` under the user's conditional pre-approval.
- [x] Initial compileable RED fixtures/tests were created without altering unrelated dirty baseline files.

Staging migration approval is not an entry condition for local RED/implementation because production/staging deployment is outside this task. It remains a release gate.

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] Red Gate §5.1 evidence shows the initial HTTP/schema/Mobile acceptance tests failed for the intended missing behavior before production patches.
- [x] Focused Backend policy/controller/repository tests pass with no skip: 49/49.
- [x] PostgreSQL fresh-history and pre-Story upgrade/checksum tests, unchanged-V1 external-manifest/forward pairing, no-duplicate-DDL, integration/concurrency/rollback tests pass within the 49-test gate.
- [x] Affected Backend regressions pass: 161/161; 210 unique Backend tests are green overall.
- [x] Unfiltered `.\mvnw.cmd clean package` was executed and transparently assessed: it remains red in 17 unrelated dirty-baseline classes; Story 6.8 gates are green and package success is not claimed.
- [x] Focused Mobile tests pass, final consultation navigation suite is 11/11, and full `flutter test` is 364/364 green.
- [x] Targeted `dart analyze` reports zero issues for the eight Story 6.8 files.
- [x] Full `flutter analyze` was executed and transparently assessed: two pre-existing unused-import warnings remain in unrelated clean `family_member_home_screen.dart`; targeted Story analysis has zero issues.
- [x] Targeted `dart format --output=none --set-exit-if-changed` succeeds for 8/8 files.
- [x] `flutter build apk --debug` succeeds; final APK size is `254428175` bytes and SHA-256 is `2B1942FF374F959422A6B9817AEF36FACCA2988F0D3866CCABF3CC8BEB4B7066`.
- [x] Coverage evidence was reviewed: this Backend module has no configured numerical line-coverage reporter/threshold, so no percentage is invented; the approved substitute is the named 49-test policy/controller/service/PostgreSQL gate plus 161 affected regressions and independent `APPROVE`.
- [x] Controllers contain validation/mapping only; service owns policy/transaction logic.
- [x] API fields, errors, state transitions, authorization rules and side effects have named executable coverage in the 49-test Backend gate and 47-test focused Mobile gate.
- [x] Automated and Android PostgreSQL evidence prove exact-once approved retry `1/1/1` and refusal/offline/eligibility-loss `0/0/0`.
- [x] Automated structural/API/DB/log/evidence checks find zero forbidden fields/tokens/raw health payload.
- [x] OV01-MAN-024 passes on Android with sanitized evidence, including account switch, Back/restart and GREEN/RED smoke.
- [x] Graph was refreshed and detect/impact/affected-flow/`tests_for` queries were reviewed; missing new untracked nodes and zero affected-flow/direct-test mapping are explicitly recorded as tool limitations.
- [x] Independent `bmad-code-review` verdict is `APPROVE` with no unresolved High/Medium finding.
- [x] Story, File List, Completion Notes, Change Log, TDS/Test-Spec, manual evidence, sprint status, OV01 tracker and dirty-baseline manifest agree.

### Suspension Criteria (Điều kiện tạm dừng)

Pause the current patch, preserve evidence, diagnose, and re-plan when:

- a discovered requirement/architecture conflict materially changes the approved product scope;
- a migration cannot be proven safe on disposable PostgreSQL or requires destructive production action;
- any cross-account exposure, forbidden-field persistence/logging, non-atomic commit, or bypass of expert eligibility is observed;
- unrelated dirty user work overlaps a required edit and cannot be safely isolated;
- test infrastructure is broken independently of Story 6.8 after reasonable diagnosis/retry;
- legal/DPO approval is required for release (record as a release gate; do not fabricate approval).

---

## 7. Rollback Plan

This section supersedes the destructive example in the generic template for this dirty brownfield repository.

- During RED/local tests, remove only synthetic fixtures inside the disposable test database/container through test cleanup. Do not touch production or shared developer data.
- Flyway is forward-only. Never delete `flyway_schema_history`, edit an applied migration, or run `DROP ... CASCADE` outside an explicitly disposable test database.
- For an application incident, disable the handoff route/feature at rollout level if available while preserving YELLOW safety guidance and return-to-origin; roll back compatible application binaries without deleting additive evidence tables.
- Correct schema defects using a separately reviewed forward migration; keep applied V1 byte-identical and update only the external schema-ownership manifest.
- Revoke participant read through policy when privacy is at risk; preserve audit/evidence for incident review.
- Never use `git checkout`, `reset`, or broad formatting to remove Story work from the current dirty tree. Reconciliation uses the captured baseline checksums and explicit Story touched-file list.
- If rollback or forward-fix work occurs later, OV01-GAP-08 and Story 6.8 must be reopened until every affected exit gate is re-proven.

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Detection rule for Story 6.8 | Check | Gate |
|---|---|---|---:|---|
| `AP-AI-001` | Unconstrained Generation | A case lacks UC/FR/AC/ADR/API/schema oracle or invents an SLA/retention rule | `[x] Verified absent` | G-0 |
| `AP-AI-002` | Green-from-Birth | New behavior passes with absent/throw/no-op handoff implementation | `[x] Verified absent` | G-2 |
| `AP-AI-003` | Implicit Decision | Test assumes new field/status/dependency/route outside approved TDS | `[x] Verified absent` | G-1 |
| `AP-AI-004` | Layer Violation | Controller test expects business policy/transaction logic in controller | `[x] Verified absent` | G-4 |
| `AP-AI-005` | Hallucinated Contract | Imported class/path/command does not exist after the contract-creation step | `[x] Verified absent` | G-3 |
| `AP-S68-006` | Mock-only Atomicity | Test proves calls but not PostgreSQL rollback/cardinality/notification boundary | `[x] Verified absent` | G-4 |
| `AP-S68-007` | Value-only Privacy | Test asserts values but not forbidden keys/columns/log markers | `[x] Verified absent` | G-4 |
| `AP-S68-008` | Single-surface UI | Test covers routed result but not inline production YELLOW, or vice versa | `[x] Verified absent` | G-4 |
| `AP-S68-009` | Role-only Authorization | Test checks role but not ownership/participant/current consent/eligibility | `[x] Verified absent` | G-4 |
| `AP-S68-010` | Non-authoritative UC Oracle | Any expected result derives from the experimental 121-UC scope | `[x] Verified absent` | G-0 |

Review result:

- [x] No anti-pattern detected; Test-Spec approved after independent verification.
- [x] Final `bmad-code-review` re-review found no unresolved High/Medium implementation finding.

| AP detected | TC ID | Description | Fix action | Fixed? |
|---|---|---|---|---:|
| _None recorded_ | — | Independent review passed with no unresolved High/Medium finding | — | `[x]` |

---

## References

- `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.21` — authoritative UC-44.
- `04_Implement/implement_artifacts/function-spec-task-allocation.md` — authoritative 242-UC Function allocation.
- `_bmad-output/planning-artifacts/epics.md` and Story 6.8 artifact — FR52/AC contract.
- `04_Implement/UC44 - Share Summary with Expert/UC44 - Share Summary with Expert_TDS.md` — technical oracle.
- `08_References/Template/PHASE-4_Test-Spec.md` — mandatory skeleton.
- `04_Implement/UC44_ShareSummaryWithExpert/`, `ExpertConsultationRequests/`, `MotherExpertDiscoveryInbox/` — legacy/supporting evidence only.

---

*CB-CONSULT-IMP-044-S68-TEST v1.0 — Approved for local implementation, including the independently verified applied-checksum correction. No production deployment or DPO/legal approval is implied.*
