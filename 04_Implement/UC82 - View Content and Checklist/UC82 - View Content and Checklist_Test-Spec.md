# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# Test Specification — UC-82 View Content and Checklist / Story 6.9

**Document ID:** `CB-CONTENT-TEST-009`
**Version:** `1.1`
**Date:** `2026-07-23`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 structure, adapted to CareBridge
**Author:** Codex — Function specification author
**Reviewed by:** Independent final-binary evidence-sync review PASS (`0 High / 0 Medium / 0 Low`); prior amendment snapshot also PASS
**DPO Sign-off:** Not requested by this document; separate approval is required if processing purpose changes
**Approved by:** User explicit pre-approval, effective after independent final-binary evidence-sync review PASS
**Classification:** Internal — contains design/test metadata only; fixtures must be synthetic

**References:**

- `UC82 - View Content and Checklist_TDS.md` (`CB-CONTENT-IMP-009`)
- `_bmad-output/implementation-artifacts/6-9-enforce-reviewed-checklist-and-stage-aware-content-boundaries.md` (canonical SHA-256 recorded in TDS §1.2)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.1.59 (authoritative 242-UC scope)
- `_bmad-output/planning-artifacts/prd.md` FR53
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` and applied Flyway migrations
- `06_Testing/TestCases/mobile/OV-01-Mother-Lifecycle-Orchestration-Manual-Test-Guide.md`

> TDD order is RED test → confirmed meaningful failure → implementation → GREEN → refactor. No case may be marked Passing without the named command/evidence. Use SYNTHETIC fixtures only.

---

## CHANGELOG

| Date | Author | Change |
|---|---|---|
| 2026-07-23 | Codex — Function specification author | Initial Story 6.9 Test-Spec using the mandatory Phase-4 skeleton; added risk, automated, concurrency, RAG, Mobile, Web, security, and manual verification coverage. |
| 2026-07-23 | Codex — Function specification author | Approved after independent review reached `0 High / 0 Medium / 0 Low`; synchronized 29 conditions and all 27 registered test aliases. |
| 2026-07-23 | Codex — development evidence sync | Recorded executable RED/GREEN, PostgreSQL, Mobile/Web, final-JAR manual evidence, and the remaining partial/full-repository gates without promoting the Story. |
| 2026-07-23 | Codex — minor course correction | v1.1 amendment under review: add a fail-closed dirty-baseline exception for Story 6.9 review readiness while preserving failed full-gate truth. |
| 2026-07-23 | Codex — fail-closed remediation | Rejected the first baseline after seven overlaps; synchronized focused remediation, exact 55-row Maven parity, overlap ownership, Flutter warning fingerprints, and refreshed runtime hashes while retaining the independent-review blockers. |
| 2026-07-23 | Independent amendment review | v1.1 approved at `0 High / 0 Medium / 0 Low`; the baseline rule may authorize the subsequent independent implementation review only. |
| 2026-07-23 | Independent final-binary verifier | Re-ran OV01-MAN-009/029 on exact JAR `4983...3308` and APK `0141...EA73`; accepted API/DB `48/48`, UI `5/5`, and zero-mismatch 32-row manifest evidence. |
| 2026-07-23 | Review-fix implementer/verifier | Added RED/GREEN oracles for deterministic pagination, latest-request-wins, real DOM/hooks, advice precedence, and nullable metadata; accepted final JAR `122BAE2F...8F557` with `58/58` API/DB, `5/5` UI, and 40-row zero-mismatch manifest. |
| 2026-07-23 | Round-2 patch implementer/verifier | Closed `R69-009..020`; accepted the round-2 checkpoint. This row and prior binary/hash rows are historical only. |
| 2026-07-24 | Final round-3 implementer/verifier | Closed `R69-021..027`; verified exact product/runtime fingerprints. |
| 2026-07-24 | Final tooling verifier | Closed `R69-028..047`; tooling `35/35`, parser `12/12`, strong scans `0`, pair/recovery locks and harness cleanup passed. Round4 template `351BE425...F1CA8` returned all three acceptance booleans true, `36/36`, 13 critical files; historical manifests `32/32`, `40/40`, `41/41`. |
| 2026-07-24 | Final fixed-scope verifier | Closed `R69-048..089`; tooling `76/76`, parser `7/7`, accepted-log scan `27/27`, independent fixed-scope review `15/15 PASS — APPROVE`, and Round4 `7470FD15...E419` authenticated immutable R3 and exact B18D/7FC binaries. |
| 2026-07-23 | Codex — final evidence sync | Reopened v1.1 to `In Review` while the independent reviewer validates the exact-final-binary manual artifacts and synchronized hashes. |
| 2026-07-23 | Independent final evidence review | v1.1 final-binary evidence sync approved at `0 High / 0 Medium / 0 Low`; the baseline rule may authorize the subsequent independent implementation review only. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|---|---|
| **Feature / Gap ID** | `Story 6.9 / OV01-GAP-09 / FR53 / UC-82` |
| **Module** | Content + Checklist + canonical Mother lifecycle + RAG; Mobile/Web adapters |
| **Source Spec** | `CB-CONTENT-IMP-009` |
| **Priority** | High; risk-based scenarios use P0–P3 independently of execution timing |
| **Sprint** | Epic 6; exact sprint dates remain managed by sprint-status artifact |
| **Estimate** | 8 story points for implementation; test automation estimate in TDS-06 |
| **Data Classification** | PII at ownership/lifecycle policy boundary; evidence must be synthetic and sanitized |
| **Compliance Scope** | Repository-approved BR-RBAC, BR-PRIVACY, minimum necessary; no unsupported legal claim |
| **Upstream Dependencies** | UC-22, UC-50, UC-108, Spring Security, PostgreSQL/Flyway |
| **Downstream Consumers** | Mother Mobile lifecycle guidance, UC-224/225 generic browsing, Gemini RAG, Web checklist admin |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|---|---|
| **AI Assisted?** | Yes |
| **Constraint Source** | TDS §§3, 8–10, 16–17; Story 6.9 AC1–AC7 |
| **Constraints Injected** | canonical server stage; SQL APPROVED predicate; neutral errors; context-first lock; safety-first RAG; generic/admin compatibility; V1 immutable; UI accessibility |
| **Model** | Codex agent session |
| **Trust Level** | `T3 v1.1 specification Approved; independent implementation review 15/15 APPROVE; Story 6.9 done` |

### 1.2 Testability baseline

- Controllability: repositories/services can be mocked; PostgreSQL fixtures can seed all statuses/stages; fake Gemini and barrier latches control failure/concurrency.
- Observability: API code/body, repository arguments, row/audit cardinality, fake Gemini call count, Mobile rendered semantics, and Web request path/status can be asserted.
- Reliability: each test creates fresh UUID fixtures, cleans its transaction/schema rows, fixes clock where timestamps matter, and must run without order dependency.
- Closure note: the graph remains incomplete for new Java/Flutter/Vitest mappings, so named behavior suites are authoritative. The final backend trace records `38/38 COVERED`, `0 PARTIAL`, `0 CONTRACT-ONLY`.

---

## 2. Logic Issues Resolved

| # | Prior/actual discrepancy | Approved resolution encoded by tests |
|---|---|---|
| L1 | Legacy/current `getChecklists()` uses `findAll/findByStage`, exposing every status | public/lifecycle repository predicates require `ChecklistTemplateStatus.APPROVED` |
| L2 | Template entity reuses `ContentStatus`; OV01 fixture requires REJECTED but UC-108 content rejection returns DRAFT | separate five-value `ChecklistTemplateStatus`; keep ContentStatus/UC-108 unchanged |
| L3 | Import loads an item UUID directly and never validates parent review status/stage | join/resolve approved parent and context before any save/audit; neutral `CHECKLIST-007` |
| L4 | Null/explicit stage has ambiguous personalized semantics | explicit no-stage lifecycle routes use canonical state; generic browse may intentionally span/select stages |
| L5 | Mobile parses nonexistent `data.content`, emits `PREPARATION`, offers an editable “for you” stage and hard-coded week | parse top-level paginated `data`; lifecycle mode renders envelope PRE_PREG/POST stage and locks selector/copy |
| L6 | Mother RAG accepts `request.userStage` and generic approved retrieval | safety first; non-RED Mother uses canonical stage and ignores client stage; other roles unchanged |
| L7 | Web admin uses public checklists and infers status from `items.length` | dedicated paginated admin contract returns real status/itemCount and no item body |
| L8 | V1 has varchar status and stage-only index; adding an index without evidence would alter migration behavior | V1 byte-identical; no enum migration; optional one forward index only after PostgreSQL plan evidence |
| L9 | Current import error says CHECKLIST-004 and reveals item UUID, while UC-50 reserved CHECKLIST-007 | freeze non-enumerating `404 CHECKLIST-007` for template/context unavailability |
| L10 | Template instructions mention Redis and destructive Flyway rollback; current slice has no Redis dependency, while mandatory workflow names `npm run test:run` but package lacks the alias | no Redis; add `package.json` alias `"test:run":"vitest run"`; production rollback is prior build/forward repair, never history deletion |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```text
UC-82 / Story 6.9 coverage
├── Backend unit: stage resolver, services, validation, RAG policy
├── Backend MVC/security: route shape, 401/403, neutral errors
├── PostgreSQL integration: enum mapping, predicates, atomicity, locks, transition races
├── Mobile model/widget: pagination, lifecycle mode, PRE/PREG/POST, retry/account generation, accessibility
├── Web Vitest/component/service: admin endpoint, true status/itemCount, paging/error state
├── Regression: UC-50/82/108/224/225, journey and RAG suites
└── Manual: OV01-MAN-009 and OV01-MAN-029 with sanitized evidence
```

Not in scope: content/template management state transitions, diagnostic accuracy, production load certification, production deployment, BABY_CARE inference from Mother stage, or the experimental 121-UC catalogue. Exclusion mitigation is regression of existing UC-108/generic browse/admin flows and explicit owned-baby import coverage.

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|---|---|
| `SRS §3.3.1.59 / UC-82` | approved article/FAQ/checklist by stage; authenticated access; empty/error behavior |
| `FR53` | approved publication boundary |
| Story 6.9 AC1–AC7 | status/stage/direct-ID/import/RAG/UI/evidence oracles |
| TDS ADR-001..008 | lifecycle/generic split, enum, neutral errors, locks, UI, RAG, admin, migration |
| UC-50 BR-CHECKLIST-001/004 | server copies template text/order and preserves personal snapshot |
| UC-108 | ContentStatus and PENDING_REVIEW→APPROVED/DRAFT remain unchanged |
| V1 + V3 + applied migrations | table/column/FK/index/persistence facts |
| SecurityConfig/current code | `/api/v1/**` authenticated baseline and current authorized RAG roles |
| OV01 manual guide | PRE “not yet” and APPROVED-only deep-link/import manual oracle |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test condition | Coverage item | Test cases |
|---|---|---|---|
| `COND-01` | generic checklist with stage returns only APPROVED same-stage parents/items | repo/service/API | `UC82-69-TC-001`, `UC82-69-TC-002` |
| `COND-02` | generic checklist without stage returns APPROVED across stages, not personalized | service/API | `UC82-69-TC-003` |
| `COND-03` | lifecycle checklist has envelope canonical stage and approved same-stage rows; absent lifecycle fails closed | resolver/service/API | `UC82-69-TC-004`, `UC82-69-TC-005`, `UC82-69-TC-016` |
| `COND-04` | known non-approved/wrong-stage content ID returns neutral CNT-003; absent lifecycle is CNT-013 before ID lookup; no template-detail route exists | lifecycle detail/routing | `UC82-69-TC-006`, `UC82-69-TC-016`, `UC82-69-TC-017` |
| `COND-05` | PRE/PREG/POST resolve exactly; absent lifecycle fails; transition affects next read | resolver/PostgreSQL | `UC82-69-TC-007`, `UC82-69-TC-016`, `UC82-69-INT-004` |
| `COND-06` | generic UC-224/225 cross-stage browse remains APPROVED and visibly non-personalized | API/Mobile | `UC82-69-TC-008`, `UC82-69-MOB-005` |
| `COND-07` | import validates 1/50 bounds, nulls, empty, >50, both contexts | DTO/service | `UC82-69-TC-009` |
| `COND-08` | every status/missing/wrong-stage item denies with CHECKLIST-007 and no write/audit | import policy | `UC82-69-TC-010`, `UC82-69-INT-001` |
| `COND-09` | duplicates deduplicate in first-occurrence order and copy DB text/order | import service | `UC82-69-TC-011` |
| `COND-10` | neither/journey-only/baby-only context matrix is exhaustive and owner-scoped; missing canonical fails before item lookup | import policy/API | `UC82-69-TC-012`, `UC82-69-TC-013`, `UC82-69-TC-023` |
| `COND-11` | foreign/noncanonical journey and foreign/inactive baby deny neutrally | security/import | `UC82-69-SEC-003` |
| `COND-12` | lifecycle/import/admin/RAG authorization and 401 behavior | method/security | `UC82-69-SEC-001`, `UC82-69-SEC-002` |
| `COND-13` | mixed/persistence-failure batch rolls back rows and success audits | PostgreSQL | `UC82-69-INT-001`, `UC82-69-INT-002` |
| `COND-14` | fixed locks serialize journey transition and baby archive races | PostgreSQL concurrency | `UC82-69-INT-003` |
| `COND-15` | RAG RED safety returns before lifecycle/retrieval/Gemini | RAG service | `UC82-69-RAG-001` |
| `COND-16` | non-RED Mother ignores userStage and retrieves APPROVED canonical stage | RAG service/repo | `UC82-69-RAG-002` |
| `COND-17` | missing Mother lifecycle yields CNT-013 before external call | RAG service | `UC82-69-RAG-003` |
| `COND-18` | non-Mother authorized roles retain generic userStage behavior | RAG service/security | `UC82-69-RAG-004` |
| `COND-19` | error bodies/logs/Gemini prompt contain no forbidden data | cross-cutting security | `UC82-69-SEC-004`, `UC82-69-RAG-005` |
| `COND-20` | admin checklist pagination/status/itemCount works without item bodies; Web uses latest-request-wins and explicit null fallbacks | API/Web | `UC82-69-TC-014`, `UC82-69-WEB-001` |
| `COND-21` | Mobile parses top-level paginated data and lifecycle envelopes | model/service | `UC82-69-MOB-001` |
| `COND-22` | lifecycle UI locks and labels server stage; truthful loading/empty/error/retry | widget | `UC82-69-MOB-002` |
| `COND-23` | dashboard PRE/PREG/POST entry and PRE “not yet” preserve same lifecycle/no baby | widget/manual | `UC82-69-MOB-003`, `UC82-69-MAN-009` |
| `COND-24` | late responses/account switch/restart never render stale or all-stage data; accessibility floor | widget/manual | `UC82-69-MOB-004`, `UC82-69-MAN-029` |
| `COND-25` | status preflight/V1 checksum/query plan and optional forward migration | PostgreSQL/Flyway | `UC82-69-INT-005` |
| `COND-26` | already imported snapshot survives later source rejection/archive | checklist regression | `UC82-69-TC-015` |
| `COND-27` | lifecycle content list enforces APPROVED canonical stage for ARTICLE/FAQ/CHECKLIST, filters/paginates, and returns an empty envelope | service/MVC/PostgreSQL | `UC82-69-TC-018`, `UC82-69-TC-019`, `UC82-69-INT-004` |
| `COND-28` | approved same-stage lifecycle detail returns the envelope/existing redacted DTO and Mobile uses the lifecycle detail route | service/MVC/Mobile | `UC82-69-TC-020`, `UC82-69-MOB-006` |
| `COND-29` | lifecycle/admin/generic/RAG query fields, UUIDs, enums, page/size/maxChunks and keyword sanitization retain exact boundary errors | MVC/regression | `UC82-69-TC-021`, `UC82-69-TC-022`, `UC82-69-RAG-006` |

Endpoint/error coverage is explicit rather than inferred from service tests:

| Contract | Covered by | Required oracle |
|---|---|---|
| `GET /api/v1/content/lifecycle` | `UC82-69-TC-016`, `UC82-69-TC-018`, `UC82-69-TC-019`, `UC82-69-TC-021`, `UC82-69-SEC-001`, `UC82-69-SEC-002`, `UC82-69-INT-004`, `UC82-69-MOB-001`, `UC82-69-MOB-002` | MOTHER-only, no client stage, envelope stage + paginated approved canonical-stage payload, filters/empty page, `CNT-001` for invalid page/size and `CNT-013` when context is absent |
| `GET /api/v1/content/lifecycle/checklists` | `UC82-69-TC-004`, `UC82-69-TC-005`, `UC82-69-SEC-001`, `UC82-69-SEC-002`, `UC82-69-MOB-002` | MOTHER-only, envelope stage, approved same-stage parents/items |
| `GET /api/v1/content/lifecycle/{id}` | `UC82-69-TC-006`, `UC82-69-TC-016`, `UC82-69-TC-020`, `UC82-69-SEC-001`, `UC82-69-SEC-004`, `UC82-69-MOB-006` | approved same-stage envelope/redacted detail, neutral `CNT-003`, and canonical context before ID lookup |
| `GET /api/v1/content` | `UC82-69-TC-008` plus existing UC-82 regressions | existing authenticated APPROVED generic page remains compatible |
| `GET /api/v1/content/search` | `UC82-69-TC-008`, `UC82-69-MOB-005` plus UC-224 regressions | deliberate explicit-stage APPROVED browse remains separate |
| `GET /api/v1/content/checklists` | `UC82-69-TC-001`, `UC82-69-TC-002`, `UC82-69-TC-003` | APPROVED-only with optional generic stage |
| `GET /api/v1/content/{id}` | `UC82-69-TC-008` plus UC-225 regressions | existing APPROVED generic detail and author redaction |
| `POST /api/v1/user-checklist-items/import` | `UC82-69-TC-009`, `UC82-69-TC-010`, `UC82-69-TC-011`, `UC82-69-TC-012`, `UC82-69-TC-013`, `UC82-69-TC-023`, `UC82-69-SEC-001`, `UC82-69-SEC-002`, `UC82-69-SEC-003`, `UC82-69-SEC-004`, `UC82-69-INT-001`, `UC82-69-INT-002`, `UC82-69-INT-003` | `CHECKLIST-001/007`, `CNT-013` when neither context ID and no canonical lifecycle, ownership, atomic rows/audits |
| `POST /api/v1/rag/answer` | `UC82-69-RAG-001`, `UC82-69-RAG-002`, `UC82-69-RAG-003`, `UC82-69-RAG-004`, `UC82-69-RAG-005`, `UC82-69-RAG-006`, `UC82-69-SEC-001`, `UC82-69-SEC-002` | safety first, canonical Mother retrieval, maxChunks boundary, existing non-Mother roles |
| `GET /api/v1/admin/content/checklists` | `UC82-69-TC-014`, `UC82-69-TC-022`, `UC82-69-SEC-001`, `UC82-69-SEC-002`, `UC82-69-WEB-001` | CONTENT_ADMIN/SYSTEM_ADMIN, exact enum/page boundaries, real status/itemCount, page 1–50, no item bodies |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique | Applied To | Rationale |
|---|---|---|
| Equivalence partitioning | five template statuses, four stages, role sets, context matrix | one representative per policy partition plus all denied statuses |
| Boundary value analysis | list size 0/1/50/51, page size 0/1/50/51, maxChunks | explicit validation edges |
| Decision tables | role × route, journey/baby context combinations, Mother vs non-Mother RAG | prevents missing policy branch |
| State transition | PRE→PREG→POST, template availability, UC-108 regression | proves current stage and preserved content state machine |
| Pairwise plus targeted exhaustive | status × stage × direct/list/import | exhaustive for security-relevant status and selected pairwise for benign filters |
| Race/barrier testing | import vs lifecycle transition/baby archive | deterministic concurrency oracle, not timing sleeps |
| Fault injection | save/audit/Gemini/network failure | rollback and truthful UI/error behavior |
| Attack/error guessing | direct UUID, foreign context, client stage tampering, injected query strings | enumeration/account-isolation protection |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Synthetic value/logic | Purpose |
|---|---|---|---|
| `FX-69-MPRE/MPREG/MPOST` | users + journeys | distinct Mothers with one ACTIVE canonical journey each | stage resolution |
| `FX-69-NOJOURNEY` | user | Mother with no canonical lifecycle | CNT-013 |
| `FX-69-OTHER` | user | second Mother and foreign context | isolation |
| `FX-69-BABY-ACTIVE/ARCHIVED` | baby rows | valid baby has `owner=caller,status=ACTIVE,isActive=true`; test owner mismatch, `ACTIVE/false`, and `ARCHIVED/true` independently | BABY_CARE matrix |
| `FX-69-TPL-{status}-{stage}` | checklist templates | all five statuses × PRE/PREG/POST/BABY_CARE | exhaustive status/stage |
| `FX-69-ITEM-*` | checklist items | deterministic UUID/text/order under each template | direct-ID and import |
| `FX-69-CONT-{status}-{stage}` | content | DRAFT/PENDING/APPROVED/ARCHIVED across stages | list/detail/RAG |
| `FX-69-RAG-RED` | fake safety result | RED guidance | safety-first |
| `FX-69-GEMINI` | fake client | call captor + success/unavailable behavior | external-boundary assertions |
| `FX-69-CLOCK` | fixed clock | one UTC instant where supported | audit/order determinism |

Factories return fresh instances per test. PostgreSQL integration tests use isolated transactions/schema cleanup; no shared mutable fixture, production export, real content body, email, phone, token, or health note.

### TDS-06 — Risk Assessment and NFR Plan

Probability/impact use 1–3; score ≥6 requires mitigation, score 9 blocks the gate while open.

| Risk | Category | Failure risk | P | I | Score | Mitigation / owner / timing | Residual evidence |
|---|---|---|---:|---:|---:|---|---|
| `R-001` | SEC | known UUID exposes non-approved/wrong-stage data | 3 | 3 | 9 | repository predicate + neutral direct-ID tests / Backend / before GREEN | API/body/log assertions |
| `R-002` | DATA | mixed import partially writes or audits | 3 | 3 | 9 | full prevalidation + transaction/fault tests / Backend+QA / before GREEN | row/audit zero delta |
| `R-003` | DATA | lifecycle transition races import | 2 | 3 | 6 | fixed pessimistic lock/barrier tests / Backend+DB / before review | both legal serial outcomes |
| `R-004` | SEC | RAG sends client-selected/wrong-stage content externally | 2 | 3 | 6 | audience context, safety-first captor tests / Backend+QA / before review | retriever args/Gemini count |
| `R-005` | BUS | Mobile shows stale/all-stage guidance | 3 | 2 | 6 | typed mode/generation guard/retry tests / Mobile / before review | widget + manual evidence |
| `R-006` | BUS | public tightening removes admin visibility | 2 | 2 | 4 | admin API/Web regression / Web+Backend / before review | route/status/itemCount proof |
| `R-007` | PERF | unnecessary/misfit index causes lock/write cost | 2 | 2 | 4 | EXPLAIN/cardinality gate / DB owner / pre-migration | plan + checksum artifact |
| `R-008` | TECH | generic UC-224/225 compatibility breaks | 2 | 2 | 4 | contract regressions / Backend+Mobile / PR suite | response compatibility |
| `R-009` | OPS | missing numeric RAG/availability SLO leads unsupported PASS claim | 2 | 2 | 4 | mark Open; collect baseline only / Product+Ops / before release assessment | NFR remains CONCERNS until approved |

NFR evidence plan: security through MVC/PostgreSQL/leak scans; performance through existing p99 baseline comparison and query plans; reliability through rollback/retry/concurrency tests; maintainability through full test/lint/build/analyze and graph review. This spec plans evidence and does not assign final PASS/CONCERNS/FAIL before implementation.

Automation effort range: P0 `~12–20h`, P1 `~24–40h`, P2 `~8–16h`, P3/exploratory `~2–4h`, total `~46–80h` over `~1–2 weeks`, including PostgreSQL fixtures and cross-platform setup. These are ranges, not commitments.

---

## 4. Test Case Specification

> TC priority expresses risk, not execution timing. Every row is a complete test contract: oracle, precondition, AAA steps, response/persistence/audit/external result, failure signature, intended file, and initial status.

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

Create `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/support/Story69TestFactory.java`. Its methods create new entity/DTO instances and new UUIDs on every call. Mockito is reset by a fresh test instance; PostgreSQL rows are created inside each test and deleted/rolled back. Flutter injects a new fake service per widget; Vitest creates new mocks in `beforeEach`. No mutable object is stored in a static field.

Exact test-file registry (every case below resolves to one or more of these paths):

| Registry key | Repository-relative test file |
|---|---|
| `B-CONTENT-UNIT` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentServiceImplTest.java` |
| `B-CONTENT-PG` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/integration/ContentIntegrationTest.java` |
| `B-LIFECYCLE-POLICY` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/policy/LifecycleContentStageResolverTest.java` |
| `B-LIFECYCLE-UNIT` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/LifecycleContentServiceTest.java` |
| `B-LIFECYCLE-MVC` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/LifecycleContentControllerTest.java` |
| `B-CONTENT-MVC` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentControllerTest.java` |
| `B-ADMIN-MVC` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/AdminChecklistControllerTest.java` |
| `B-CHECKLIST-UNIT` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportBoundaryTest.java` |
| `B-CHECKLIST-MVC` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportControllerTest.java` |
| `B-CHECKLIST-PG` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportPostgresIntegrationTest.java` |
| `B-CHECKLIST-RACE` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportConcurrencyPostgresTest.java` |
| `B-LIFECYCLE-PG` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/integration/LifecycleContentPostgresIntegrationTest.java` |
| `B-MIGRATION-PG` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/integration/ChecklistTemplateMigrationTest.java` |
| `B-SECURITY` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/security/Story69ContentSecurityTest.java` |
| `B-RAG` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagStageBoundaryTest.java` |
| `B-RAG-POLICY` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagPolicyServiceTest.java` |
| `B-RAG-IMPL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagImplementationContractTest.java` |
| `B-RAG-MVC` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java` |
| `B-RAG-SERVICE-REG` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagServiceTest.java` |
| `B-RAG-STARTUP` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagNoGeminiStartupTest.java` |
| `B-AUDIT-POLICY` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/policy/AuditEligibilityPolicyTest.java` |
| `M-SERVICE` | `05_Development/CareBridgeMobileApp/test/features/community/content_service_story_6_9_test.dart` |
| `M-LIST` | `05_Development/CareBridgeMobileApp/test/features/community/view_content_lifecycle_screen_test.dart` |
| `M-DETAIL` | `05_Development/CareBridgeMobileApp/test/features/community/verified_content_detail_lifecycle_test.dart` |
| `M-SEARCH` | `05_Development/CareBridgeMobileApp/test/features/community/verified_content_search_screen_test.dart` |
| `M-ENTRY` | `05_Development/CareBridgeMobileApp/test/features/journey/story_6_9_lifecycle_content_entry_test.dart` |
| `W-ADMIN` | `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistListPage.test.tsx` |

### BACKEND UNIT AND API TEST CASES

| TC ID | Pri/Severity | Oracle / Condition | Feature & intended file | Preconditions | Arrange / Act / Assert | Expected response and side effects | Failure signature | Status |
|---|---|---|---|---|---|---|---|---|
| `UC82-69-TC-001` | P1/HIGH | FR53, AC1 / COND-01 | `B-CONTENT-UNIT` | five same-stage statuses and two approved templates | A: mock APPROVED template query + one batch item query; A: call generic with PREG; A: capture repository/status/IDs and DTO parents | only APPROVED PREG templates/items; exactly one template query and one batch-item query, zero per-template item calls; stable item ordering; no status field in consumer DTO; no write/audit | findAll/findByStage/per-template item query called or denied status returned | PASS — backend trace |
| `UC82-69-TC-002` | P1/HIGH | AC1 / COND-01 | `B-CONTENT-PG` | PostgreSQL statuses/stages and multiple templates/items | seed; GET `/content/checklists?stage=PREGNANCY`; inspect body and SQL/query-count capture | 200; every parent APPROVED+PREG; every item belongs to returned parent; two bounded repository queries regardless of template count; order null-last then UUID | any other status/stage/item or N+1 query growth | PASS — backend trace |
| `UC82-69-TC-003` | P1/HIGH | ADR-001, AC1 / COND-02 | `B-CONTENT-UNIT`, `B-CONTENT-MVC` | APPROVED across stages + denied rows | GET generic checklists without stage | approved rows across stages, not envelope/personalized; denied rows absent | canonical-only result or denied row leak | PASS — backend trace |
| `UC82-69-TC-004` | P1/HIGH | AC1/4 / COND-03 | `B-LIFECYCLE-UNIT` | canonical PREG | resolver returns PREG; call lifecycle checklists | envelope.stage=PREG; payload approved PREG only | request stage used or envelope absent | PASS — backend trace |
| `UC82-69-TC-005` | P1/HIGH | ADR-001 / COND-03 | `B-LIFECYCLE-MVC` | MOTHER principal | GET lifecycle checklist, attempt stage tampering | route declares no stage; response remains canonical | client stage changes result | PASS — backend trace |
| `UC82-69-TC-006` | P0/CRITICAL | AC2, CNT-003 / COND-04 | `B-LIFECYCLE-UNIT`, `B-LIFECYCLE-MVC` | known DRAFT/PENDING/ARCHIVED/wrong-stage/missing IDs | invoke detail for each partition | `ErrorResponse{success=false,status=404,error=CNT-003,message='Content not found or not available',path,details=null,timestamp}`; forbidden fields absent; zero side effect | 200, distinct denial message, ID/status/body leak | PASS — backend trace |
| `UC82-69-TC-007` | P1/HIGH | AC4 / COND-05 | `B-LIFECYCLE-POLICY` | PRE/PREG/POST canonical and no journey | resolve/read/resolveForUpdate each | exact ContentStage mapping; locked result has only journeyId+stage; no journey→CNT-013 | latest/cache/request mapping or undefined context | PASS — backend trace |
| `UC82-69-TC-008` | P1/HIGH | AC4/5 / COND-06 | `B-CONTENT-MVC`, existing UC-224 tests in that file | approved other-stage content | generic search explicit other stage | 200 approved other-stage; ordinary paginated response, not lifecycle label | 403/canonical narrowing/non-approved leak | PASS — backend trace |
| `UC82-69-TC-009` | P1/HIGH | AC3 / COND-07 | `B-CHECKLIST-UNIT`, `B-CHECKLIST-MVC` | fresh request/body per boundary | test missing field, null, empty, 1, 50, 51, null element, both IDs, malformed JSON, wrong JSON type | valid 1/50 proceed; every invalid HTTP partition returns standard `ErrorResponse{status=400,error=CHECKLIST-001}` from scoped advice/service before repository/save/audit; response omits parser/raw body detail | VALIDATION_ERROR, lookup/write on invalid, raw parser detail, or wrong code | PASS — backend trace |
| `UC82-69-TC-010` | P0/CRITICAL | AC2/3 / COND-08 | `B-CHECKLIST-UNIT` | each template status + wrong stage/missing | import each direct item UUID | only approved compatible proceeds; others `ErrorResponse{status=404,error=CHECKLIST-007,message='Template item not found or unavailable'}`, zero save/audit, identical neutral body shape | status-specific leak or partial write | PASS — backend trace |
| `UC82-69-TC-011` | P1/HIGH | AC3, UC-50 BR-004 / COND-09 | `B-CHECKLIST-UNIT`, `B-AUDIT-POLICY` | IDs `[B,A,B]`, DB text/order; audit eligibility baseline | import; capture saves/output; assert `shouldAudit(CHECKLIST_ITEM_ADDED)` | 2 rows, response B then A, text/order from DB, policy eligible, and one transaction-participating success audit per row | 3 rows/client text/order mismatch, ineligible action, or missing audit | PASS — backend trace |
| `UC82-69-TC-012` | P1/HIGH | ADR-004 / COND-10 | `B-CHECKLIST-UNIT` | neither ID; journey-only canonical/noncanonical; no canonical lifecycle | import matrix | neither locks canonical; no canonical→CNT-013 before item lookup; journey-only must equal locked canonical | trusts request journey/unlocked read or queries items without context | PASS — backend trace |
| `UC82-69-TC-013` | P1/HIGH | AC3, ADR-004 / COND-10 | `B-CHECKLIST-UNIT` | owner/status/isActive baby partitions | baby-only import | only `owner=caller,status=ACTIVE,isActive=true` baby + APPROVED BABY_CARE succeeds | Mother stage inferred or foreign/archived/inactive baby accepted | PASS — backend trace |
| `UC82-69-TC-014` | P1/HIGH | ADR-007 / COND-20 | `B-ADMIN-MVC` | admin principals and mixed statuses | GET paginated admin checklist filters | paginated top-level data; exact fields/status/itemCount; no items/body | public endpoint used or fabricated status | PASS — backend trace |
| `UC82-69-TC-015` | P2/MEDIUM | Scope decision 13 / COND-26 | `B-CHECKLIST-UNIT` | prior imported row; source changes APPROVED→ARCHIVED | list personal row and try new import | prior row remains; new import denied; no delete/update of snapshot | snapshot revoked or new import succeeds | PASS — backend trace |
| `UC82-69-TC-016` | P1/HIGH | AC1/2/4, CNT-013 / COND-03/04/05 | `B-LIFECYCLE-UNIT`, `B-LIFECYCLE-MVC` | MOTHER without canonical lifecycle | call lifecycle list, checklists, and detail with known UUID | every route returns identical `ErrorResponse{status=409,error=CNT-013,message='Lifecycle content context unavailable'}` before content/checklist repository access | empty/all-stage payload, CNT-003 before context, or repository call | PASS — backend trace |
| `UC82-69-TC-017` | P2/MEDIUM | AC2, ADR-001 / COND-04 | `B-CONTENT-MVC` | authenticated caller and known template UUID | request `GET /api/v1/content/checklists/{templateId}` | no template-detail handler is mapped; response is the framework's non-enumerating not-found behavior and contains no template data | 200/template metadata or newly introduced endpoint | PASS — backend trace/final JAR |
| `UC82-69-TC-018` | P1/HIGH | AC1/4, FR53 / COND-27 | `B-LIFECYCLE-UNIT` | canonical PREG; ARTICLE/FAQ/CHECKLIST in every status/stage/topic | call lifecycle list for each type/topic and page | repository receives canonical PREG+APPROVED; payload contains only matching rows; page metadata/order preserved | checklist-only coverage, wrong status/stage/type/topic, or unbounded page | PASS — backend trace |
| `UC82-69-TC-019` | P1/HIGH | AC1/4 / COND-27 | `B-LIFECYCLE-MVC`, `B-LIFECYCLE-PG` | PRE/PREG/POST Mothers and an empty canonical stage fixture | GET lifecycle list with type/topic/page/size, including empty result | 200 envelope always includes canonical stage; payload is existing PaginatedResponse, empty `data=[]` with correct zero totals; no all-stage fallback | missing envelope/stage or wrong-stage row | PASS — backend trace |
| `UC82-69-TC-020` | P1/HIGH | AC2/4/5, BR-PRIVACY / COND-28 | `B-LIFECYCLE-UNIT`, `B-LIFECYCLE-MVC` | approved same-stage detail with internal author/review sentinels | GET lifecycle detail | 200 envelope stage matches payload; all existing ContentDetailResponse fields including public `status=APPROVED` are preserved; author ID and review-only metadata/reason are absent | denial-only implementation, generic lookup, missing APPROVED status, or metadata leak | PASS — backend trace |
| `UC82-69-TC-021` | P1/HIGH | CNT-001, MOD-001, existing search rules / COND-29 | `B-LIFECYCLE-MVC`, `B-CONTENT-MVC` | MOTHER and synthetic topic/content | exercise valid ARTICLE/FAQ/CHECKLIST; malformed enum/UUID; page -1; size 0/1/50/51; unknown valid topic; `%`, `_`, quote keyword regressions | page/size invalid→CNT-001; malformed type/topic UUID→MOD-001; unknown valid topic→200 empty envelope; boundaries 1/50 succeed; generic search escapes metacharacters and remains APPROVED-only; zero writes | 500, injection broadening, size 51 accepted, or side effect | PASS — backend trace |
| `UC82-69-TC-022` | P1/HIGH | ADR-007, CNT-001, MOD-001 / COND-20/29 | `B-ADMIN-MVC` | CONTENT_ADMIN and mixed templates | stage/status filters; malformed enum; page -1; size 0/1/50/51 | valid page returns exact fields; page/size invalid→CNT-001; malformed enum→MOD-001; no item bodies | invalid accepted, wrong paging, status/body fabrication | PASS — backend trace |
| `UC82-69-TC-023` | P1/HIGH | CNT-013, AC3 / COND-10 | `B-CHECKLIST-UNIT`, `B-LIFECYCLE-MVC` | MOTHER has no canonical; import body has neither context ID | POST import | 409 CNT-013 before template lookup; zero save/audit; exact ErrorResponse path/message | CHECKLIST-007 after lookup, empty success, write/audit | PASS — backend trace |

### SECURITY TEST CASES

| TC ID | Pri/Severity | Oracle / Condition | Feature & intended file | Preconditions | Arrange / Act / Assert | Expected response and side effects | Failure signature | Status |
|---|---|---|---|---|---|---|---|---|
| `UC82-69-SEC-001` | P1/HIGH | BR-RBAC / COND-12 | `B-SECURITY` | no JWT then each role | call lifecycle/import/admin endpoints | no JWT=401; wrong authenticated role=403; no data/write | 200 or enumerating body | PASS — backend trace |
| `UC82-69-SEC-002` | P1/HIGH | AC5 / COND-12 | `B-SECURITY` | MOTHER, CONTENT_ADMIN, SYSTEM_ADMIN | exercise allowed route matrix | lifecycle/import only MOTHER; admin list only content/system admin | role broadening/narrowing | PASS — backend trace |
| `UC82-69-SEC-003` | P0/CRITICAL | BR-PRIVACY, AC2/5 / COND-11 | `B-SECURITY`, `B-CHECKLIST-PG` | Mother B knows A journey/baby/item UUID | submit foreign assertions | 404 CHECKLIST-007; zero row/audit; body has none of known metadata | 403/404 distinction leaks metadata or write | PASS — backend trace |
| `UC82-69-SEC-004` | P1/HIGH | TDS ADR-003 / COND-19 | `B-SECURITY`, `B-RAG` | unique sentinel body/text/token/email | trigger each denial | response/log lacks all sentinels and raw request; allowed fields only correlation/caller/operation/code | any sentinel found | PASS — backend trace |

### RAG TEST CASES

| TC ID | Pri/Severity | Oracle / Condition | Feature & intended file | Preconditions | Arrange / Act / Assert | Expected response and side effects | Failure signature | Status |
|---|---|---|---|---|---|---|---|---|
| `UC82-69-RAG-001` | P1/HIGH | safety contract, ADR-006 / COND-15 | `B-RAG-POLICY` | Mother, RED fake | safety RED; generate answer | emergency guidance; zero resolver/downstream generator/retriever/Gemini calls | lifecycle or any generator called first | PASS — backend trace |
| `UC82-69-RAG-002` | P0/CRITICAL | AC4 / COND-16 | `B-RAG-POLICY`, `B-RAG` | parameterized canonical PRE/PREG/POST; each request supplies a different userStage | non-RED generate; capture policy context/retriever/prompt | policy maps canonical stage to matching UserStage for all three; Gemini receives server `RagExecutionContext`; four-argument retriever receives canonical stage; only APPROVED same-stage sources; client value ignored; BABY_CARE/default conversion is unreachable/rejected | client stage reaches retriever/prompt or incomplete switch | PASS — backend trace |
| `UC82-69-RAG-003` | P1/HIGH | CNT-013 / COND-17 | `B-RAG-POLICY`, `B-RAG-IMPL`, `B-RAG-MVC`, `B-RAG-STARTUP` | Mother without canonical under normal non-test, test, and minimal fallback-only contract contexts | non-RED generate through policy and HTTP adapter | 409 CNT-013; zero downstream generator/retriever/Gemini in every context contract; startup regression compiles with policy injection | fallback/mock/Gemini invocation before policy, startup failure, or wrong HTTP error | PASS — backend trace |
| `UC82-69-RAG-004` | P1/HIGH | AC5 / COND-18 | `B-RAG-POLICY`, `B-RAG-IMPL`, `B-RAG-MVC`, `B-RAG-SERVICE-REG`, `B-RAG-STARTUP` | FAMILY/EXPERT/MODERATOR/CONTENT_ADMIN/SYSTEM_ADMIN; normal non-test, test, and minimal fallback-only contract contexts | request existing userStage and exercise HTTP roles | policy skips lifecycle; effective request.userStage reaches downstream contract; Gemini preserves three-argument generic retriever; fallback/mock stay deterministic; existing service/startup regressions compile and pass; PARTNER 403 | forced Mother lifecycle lookup, context bypass, startup/signature regression, or role broadening | PASS — backend trace |
| `UC82-69-RAG-005` | P1/HIGH | BR-PRIVACY / COND-19 | `B-RAG` | lifecycle entity with sentinel private fields and fake Gemini captor | generate Mother answer | prompt contains curated content/query/canonical `UserStage` only; no lifecycle object/health fields | raw lifecycle serialization | PASS — backend trace |
| `UC82-69-RAG-006` | P2/MEDIUM | existing RAG-001/RAG-002 validation / COND-29 | `B-RAG-MVC` | authorized principals | maxContextChunks null, -1, 0, 1, 10, 11; query null/blank/2/3/500/501 chars | null/-1/0 preserve current non-positive default 5, 1/10 pass exact limit, 11→RAG-002 400; invalid query→RAG-001 400; all invalid paths precede safety/resolver/downstream generator/retrieval/Gemini | external call on invalid or changed error | PASS — backend trace |

### INTEGRATION TEST CASES

| TC ID | Pri/Severity | Oracle / Condition | Feature & intended file | Preconditions | Arrange / Act / Assert | Expected response and side effects | Failure signature | Status |
|---|---|---|---|---|---|---|---|---|
| `UC82-69-INT-001` | P1/HIGH | AC3 / COND-08/13 | `B-CHECKLIST-PG` | approved+denied mixed batch | call import; count rows/audits before/after | CHECKLIST-007; both deltas zero | partial row/audit | PASS — backend trace |
| `UC82-69-INT-002` | P1/HIGH | ADR-004 / COND-13 | `B-CHECKLIST-PG` | two valid items; injected second save/audit failure | execute transaction | exception; rows/audits roll back to baseline | first row/audit committed | PASS — backend trace |
| `UC82-69-INT-003` | P1/HIGH | ADR-004 / COND-14 | `B-CHECKLIST-RACE` | barriers and two DB transactions | race import vs journey transition, then baby `status/isActive` invalidation | no deadlock; one legal serialization; imported rows match locked context or denial with zero delta | mixed stages, timeout/deadlock, foreign link | PASS — backend trace |
| `UC82-69-INT-004` | P1/HIGH | AC4 / COND-05/27 | `B-LIFECYCLE-PG` | canonical PRE then legal transition to PREG; approved/denied rows in both stages | lifecycle list before/after transition | first envelope PRE with only approved PRE; next PREG with only approved PREG; same lifecycle ID; no second active journey | stale PRE/second journey/wrong status | PASS — backend trace |
| `UC82-69-INT-005` | P2/MEDIUM | ADR-008 / COND-25 | `B-MIGRATION-PG`; query-plan evidence `_bmad-output/test-artifacts/story-6-9/postgresql/checklist-index-preflight.txt` | real PostgreSQL/Flyway | status distinct/cardinality/EXPLAIN; V1 hash; optional fresh+upgrade | allowed status set; V1 unchanged; latest baseline migration recorded; no migration by default, or exactly `V20260723140000__optimize_approved_checklist_stage_lookup.sql` with only `idx_checklist_templates_approved_stage(stage) WHERE status='APPROVED'` proven | unknown status/V1 drift/unproven or alternate index | PASS — backend trace |

### MOBILE TEST CASES

| TC ID | Pri/Severity | Oracle / Condition | Feature & intended file | Preconditions | Arrange / Act / Assert | Expected response and side effects | Failure signature | Status |
|---|---|---|---|---|---|---|---|---|
| `UC82-69-MOB-001` | P1/HIGH | API schema / COND-21 | `M-SERVICE` | fake top-level generic page plus lifecycle list/detail envelope JSON | call generic/lifecycle list/detail services | exact list/page/envelope stage parsed; lifecycle detail calls `/api/v1/content/lifecycle/{id}`; `PRE_PREGNANCY` valid | reads `data.content`, generic detail in lifecycle mode, or PREPARATION | PASS — UI green |
| `UC82-69-MOB-002` | P1/HIGH | ADR-005 / COND-22 | `M-LIST` | fake PRE/PREG/POST success/empty/error | pump mode, retry | server stage text, no lifecycle stage selector/week copy, truthful loading/empty/error/retry | stale/all-stage fallback or silent empty on error | PASS — UI green/manual |
| `UC82-69-MOB-003` | P1/HIGH | AC6 / COND-23 | `M-ENTRY` | canonical PRE/PREG/POST | tap content entry; PRE “not yet” | lifecycle mode opens; “not yet” returns same PRE state; zero mutation/baby request | creates pregnancy/baby or generic mode | PASS — UI green/MAN-009 |
| `UC82-69-MOB-004` | P1/HIGH | AC6 / COND-24 | `M-LIST`, `M-DETAIL` | delayed response, account generation switch/restart | complete old response after switch; restore | old result ignored; correct account/stage reload; TalkBack labels, 48x48 targets, 200% text no critical overflow | cross-account/stale render/a11y failure | PASS — UI green/manual cold-start |
| `UC82-69-MOB-005` | P2/MEDIUM | AC4/6 / COND-06 | `M-LIST`, `M-SEARCH` | generic explicit-stage search | open deliberate browse | separate non-personalized label and editable explicit search filters | shown as current-stage guidance | PASS — UI green/manual |
| `UC82-69-MOB-006` | P1/HIGH | AC2/4, ADR-005 / COND-28 | `M-DETAIL` | lifecycle list card; fake same-stage success then wrong-stage/CNT-003 after transition; generic search card | tap each card and complete delayed responses | lifecycle card uses typed lifecycle detail/envelope and shows server stage; transition denial shows truthful retry without stale body; generic card still uses `/api/v1/content/{id}`; PRE label is `PRE_PREGNANCY` | lifecycle card calls generic route or renders stale/wrong-stage detail | PASS — UI green |

### WEB TEST CASES

| TC ID | Pri/Severity | Oracle / Condition | Feature & intended file | Preconditions | Arrange / Act / Assert | Expected response and side effects | Failure signature | Status |
|---|---|---|---|---|---|---|---|---|
| `UC82-69-WEB-001` | P1/HIGH | ADR-007 / COND-20 | `W-ADMIN` | jsdom with mocked API transport only; DRAFT/REJECTED/APPROVED, null stage/updatedAt, deferred out-of-order requests | render real component/hooks; exercise success, stage/status filters, page, latest error/retry, older-success-first, older-rejection-last, and null row | request `/api/v1/admin/content/checklists`; true text+status+itemCount; stale completion cannot mutate rows/error/loading; null row renders `Không xác định` / `Chưa cập nhật`; warm pill/focus labels | mocked React hooks, public request, status based on itemCount, stale mutation, or missing null fallback | PASS — review-fix UI green 7/7 |

### MANUAL VERIFICATION CASES

| TC ID | Pri | Oracle / Condition | Fixture and procedure | Expected evidence | Failure signature | Status |
|---|---|---|---|---|---|---|
| `UC82-69-MAN-009` | P1 | OV01-MAN-009 / COND-23 | synthetic PRE Mother; open dashboard→guidance; choose “not yet”; close/reopen; inspect journey/baby counts | sanitized screenshots/semantics + API/DB counts prove same lifecycle, PRE approved rows only, zero PREG/baby creation | changed lifecycle/new journey/baby/stale data | PASS — final round-3 closed-set manifest `FDD915B4C98F1054384F4DA1382A1120EF94EA9696E6D1C832AAD7EA3955E055` |
| `UC82-69-MAN-029` | P0 | OV01-MAN-029 / COND-24 | PRE/PREG/POST with APPROVED/DRAFT/PENDING/REJECTED/ARCHIVED; lifecycle list/detail/deep-link/import/RAG; generic browse/admin regression | sanitized UI/API/PostgreSQL matrix: only approved correct-stage personalized rows; neutral denials; deterministic pages; nullable metadata; zero denied writes/audits/Gemini calls; non-diagnostic disclaimer | any denied status/stage exposure or metadata leak | PASS — auth `3/3`, API/DB `55/55`, UI `5/5`, rejected ContentItem `0`, checklist denials `3/3`; Round4 `Semantics/BinaryBinding/ClosedSet=true`, `36/36`, 13 critical files |

Manual evidence folder target: `_bmad-output/test-artifacts/story-6-9-manual/`. Store a run summary, environment/build identifiers, fixture manifest with synthetic IDs/status/stage only, screenshots/semantics, sanitized API results, and count-only PostgreSQL evidence. Never store tokens or raw content/checklist/health text.

Accepted manual artifact contract (the executor consolidated the planned text matrices into sanitized Markdown while retaining linked PNG/XML evidence):

| File | Required content |
|---|---|
| `runtime-build-manifest.md` | Git baseline, accepted/rejected backend hashes, APK hash, OS/device/API/PostgreSQL/Flyway labels, incidents and privacy policy |
| `fixture-story69.sql` | local synthetic-only fixture source; no production/shared data |
| `final-man009-*.png/.xml` | PRE dashboard, “not yet” snackbar, cold home and restored same dashboard |
| `man029-pre-*.png/.xml`, `man029-preg-*.png/.xml`, `man029-post-*.png/.xml` | locked PRE/PREG/POST lifecycle guidance and semantics |
| `api-matrix.md` | sanitized lifecycle/direct-ID/status/wrong-stage/import/404/405 result matrix; no Authorization value or body text |
| `db-evidence.md` | count/status/stage-only import, audit, journey and baby invariants |
| `manual-run-summary.md` | PASS/FAIL, final build hashes, exact evidence links, independent executor, incidents/retests |

The accepted final differential is `_bmad-output/test-artifacts/story-6-9-manual/final-round2-b18df006-7fc34f65-r3/`. Round4 template SHA-256 `7470FD151E7C2DB91B02497B3F291309855B7B4C44283D2F1EA9BCBE6504E419` verifies `Semantics=true`, `BinaryBinding=true`, `ClosedSet=true`, `AuthenticatedByteSnapshots=true`, `36/36` rows and 13 critical files for exact B18D/7FC binaries. Historical manifests independently verify final4983 `32/32`, final122 `40/40`, and round2 `41/41`; current is `36/36`. Accepted-log scan `27/27` and runtime shutdown PASS.

Read-only database evidence maps to: canonical journey count/type/status/version; baby count/status/is_active; template counts grouped by stage/status; user checklist/audit counts before/after; and content counts grouped by stage/status. Queries select IDs/counts/status/stage only and never `item_text`, content `body`, notes, email, phone, or token fields. MAN-009 executes: capture PRE baseline → open lifecycle guidance → choose “not yet” → force-stop/relaunch → recapture UI/counts. MAN-029 manually executes PRE/PREG/POST lifecycle list/checklist/detail and status/wrong-stage/import denials; the final `38/38 COVERED` backend trace and Web/UI suites supply the complementary RAG, generic cross-stage, admin, role, paging, and concurrency evidence.

---

## 5. Red-Green-Refactor Tracker

| Group | Test files | RED confirmed | GREEN evidence | REFACTOR note |
|---|---|---|---|---|
| Backend lifecycle/checklist/audit | `B-CONTENT-UNIT`, `B-CONTENT-PG`, `B-LIFECYCLE-POLICY`, `B-LIFECYCLE-UNIT`, `B-LIFECYCLE-MVC`, `B-CONTENT-MVC`, `B-ADMIN-MVC`, `B-CHECKLIST-UNIT`, `B-CHECKLIST-MVC`, `B-CHECKLIST-PG`, `B-CHECKLIST-RACE`, `B-LIFECYCLE-PG`, `B-MIGRATION-PG`, `B-SECURITY`, `B-AUDIT-POLICY` | `[x]` | `[x]` | `38/38 COVERED`; final PostgreSQL `15/15`; preserve controller→policy/service→repository boundaries and transactional audit eligibility |
| RAG | `B-RAG`, `B-RAG-POLICY`, `B-RAG-IMPL`, `B-RAG-MVC`, `B-RAG-SERVICE-REG`, `B-RAG-STARTUP` | `[x]` | `[x]` | full focused RAG `72/72`; one safety/lifecycle policy gate before every active generator/context |
| Mobile | `M-SERVICE`, `M-LIST`, `M-DETAIL`, `M-SEARCH`, `M-ENTRY` | `[x]` | `[x]` | generic-search async races `10/10`, full Flutter `388/388`; reuse design tokens and keep behavior outside styling |
| Web | `W-ADMIN` | `[x]` | `[x]` | review-fix RED `3` intended failures of `7`; final Vitest `19/19`; real DOM/hooks, latest-request-wins, dataset-shrink page reconciliation, nullable metadata fallbacks, admin DTO, no item bodies |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

RED proof must come from the pre-change implementation or a narrow throw/no-op stub; production source must not be destructively replaced solely to manufacture RED. Expected initial failures include: denied template statuses currently returned, direct import accepted, missing lifecycle route, Mother RAG trusting user stage, Mobile reading `data.content`, and Web calling the public endpoint. A test that passes against empty data/no-op/throw without asserting the required oracle is AP-AI-002 and must be rewritten.

Supported focused commands after test files exist:

```powershell
# Backend
.\mvnw.cmd -Dtest=LifecycleContentStageResolverTest,ChecklistImportBoundaryTest,RagStageBoundaryTest test

# Mobile
flutter test test/features/community/content_service_story_6_9_test.dart test/features/community/view_content_lifecycle_screen_test.dart

# Web — Story 6.9 adds the workflow-required alias "test:run": "vitest run"
npm run test:run -- src/features/contentManagement/pages/ChecklistListPage.test.tsx
```

Store RED logs under `_bmad-output/test-artifacts/story-6-9/red-gate/`; record exact command, exit code, failing TC IDs, and failure reason. Do not record a commit hash because commit/push is outside the authorized task.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] Pre-implementation v1.0, the prior v1.1 snapshot, and the final-binary evidence sync passed independent review at `0 High / 0 Medium / 0 Low`; v1.1 is `Approved`.
- [x] Approved Story baseline SHA, V1 SHA-256 `A1B20BB1B4ED6037E853C627D8A21E4369B4CBB96B412BF068AA0E4FAFE5D021`, baseline HEAD/worktree, current tests, and supported commands are recorded and rechecked before/after implementation.
- [x] PostgreSQL and synthetic fixtures for every status/stage/context are available.
- [x] RED tests are written and meaningful failures captured before behavior implementation.
- [x] No unresolved architecture/product decision changes Story 6.9 scope.

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] Every one of the 29 TC conditions has implemented evidence; all `47/47` registered cases PASS; backend trace is `38/38 COVERED`, `0 PARTIAL`, `0 CONTRACT-ONLY`.
- [x] No open score-9 risk, no unresolved High/Medium independent implementation-review finding, and no skipped Story 6.9 test; final fixed-scope verdict is `15/15 PASS — APPROVE`.
- [x] Backend evidence capture is complete: Story trace `38/38 COVERED`, behavior/RAG `118/118`, affected regressions `58/58`, review-fix unit/component `21/21`, real PostgreSQL `19/19`, final lifecycle/request-scoped SQL `11/11` with planner payload `0`. Exact round-3 full test and test-inclusive package remain `FAILED — accepted baseline`, never PASS, at `423` XML / `2688 tests / `7 failures / 48 errors / 1 skipped`, 55 unique IDs, exact `55/55`, shared set SHA-256 `34297985A24EC1B9D5A755DE33E5A67C2109AE1DA67F0CF5004DE24A280B3A34`; comparators report `0/0/0`.
- [x] PostgreSQL predicate/rollback/concurrency/migration evidence passes; V1 hash remains unchanged.
- [x] Mobile evidence capture is complete: generic-search async races `10/10`, full tests `388/388` with log SHA-256 `917D67E1FC627BD4183C72672F397F934CF06F611FC600E434ECC88E3F81EC37`; scoped analyze/format is clean. Full analyze remains `FAILED — accepted baseline`, not PASS, on exactly two rows; raw/set/atomic-manifest SHA-256 `2FC4D82B8649F1E9E3B9CEF76E4FA6FA341F680BD660E0F54260140470C48BAE` / `04F45EE4F3547D539790E6CED5379E94AB82088D28FA529E56B81171FF11E677` / `EF90BF3C7FB152C3CCEF0222E0F37EFD90C74E5A1DBBF60BAE7692D0859414D0`. APK build exit `0` has 66 classified rows (41 dependency, 3 Zego, 2 KGP, 20 Java plugin/toolchain, zero Story paths), raw/set/atomic-manifest SHA-256 `6AFFE60DB0C001F1A609ABE355B0D63062358FC46BCD986C43ADBAAB854394F3` / `3825D9C48B931CE0F2E8AAF8974D65D57E4EDBD2FF7095B316E2A19A4C53635B` / `B2D3C3FD3C28FC3187ABA11F7C98995D14C64C43C02BD55E13D040D36454D224`; APK `226838368` bytes / `7FC34F65EAD4566D83D5BEDD4C6D186249A238727F08C4513E9896B56B34A4D9`. A second clean build has row delta `0` and identical APK hash.
- [x] Web `19/19`, lint/build exit `0`; evidence tooling `76/76`, parser `7/7`, accepted-log scan `27/27` zero; atomic pair/recovery/identity locks and harness cleanup pass.
- [x] OV01-MAN-009/029 reran on exact B18D JAR / 7FC APK. Round4 template SHA-256 `7470FD151E7C2DB91B02497B3F291309855B7B4C44283D2F1EA9BCBE6504E419` returns `Semantics/BinaryBinding/ClosedSet/AuthenticatedByteSnapshots=true`, `36/36`, 13 critical files; historical manifests verify `32/32`, `40/40`, `41/41`; accepted leak/runtime shutdown PASS.
- [x] Graph refreshed; change detection, affected flows, impact radius, and tests-for evidence reviewed with named-suite supplementation.
- [x] API fields, states, errors, roles, side effects, UI states, and manual guide are synchronized with TDS/story/sprint/gap artifacts at final `done` truth.
- [x] No real/production email, phone, token, health data, content body, or raw checklist text appears in accepted logs/evidence; synthetic test-only addresses such as `noreply@carebridge.test` may appear in framework configuration diagnostics.

Coverage target: all 29 conditions bidirectionally mapped; new/changed policy branches 100% condition coverage. A generic project-wide line percentage is not used to conceal a missing security branch.

#### Evidence-based baseline-exception rule for review transition

This rule implements TDS ADR-009 and `_bmad-output/planning-artifacts/sprint-change-proposal-2026-07-23-story-6-9-baseline-exception.md`.

- All `47/47` registered cases must be PASS; backend trace must remain `38/38 COVERED`, `0 PARTIAL`, `0 CONTRACT-ONLY`.
- Behavior/RAG `118/118`, real PostgreSQL `15/15`, affected regressions `58/58`, Mobile/Web gates, and OV01-MAN-009/029 must pass.
- From `05_Development/CareBridgeAPI` on Windows 11, Oracle JDK `21.0.10`, Maven Wrapper `3.9.16`, both exact commands must run with test-only properties `-Dcarebridge.zego.app-id=1` and `-Dcarebridge.zego.server-secret=synthetic-test-secret`: canonical `test` and test-inclusive `clean package`. The no-Zego run is diagnostic only.
- Full-test evidence must remain exit `1`, `423 / 2688 / 7 / 48 / 1`, 55 IDs; only `full-maven-test.round3.current.raw.log` SHA-256 `BF65A11B6FDF43BC998E5393BEFC20D623ED555AC426B0928CC1BBC04F01305A` and normalized SHA-256 `F19D538C80E56D150869D3A3CBEF4F3FB61BBE6246520A1BCC1FA4CD65BE0114` are accepted; set SHA-256 `34297985A24EC1B9D5A755DE33E5A67C2109AE1DA67F0CF5004DE24A280B3A34`.
- Test-inclusive package must remain failed with the same aggregate and `55/55`; only round-3 raw SHA-256 `B9A94256BB6A44E5E878FA6E8FFB078D7A7502D54A3D8D7251091C0DD8002934` and normalized SHA-256 `3D2BE2736E2D63307AADF1345082CE3A249B03995FE95DEE03BC89332F148D75` are accepted. Neither Maven command is PASS.
- `_bmad-output/test-artifacts/story-6-9/green-gate/story-overlap-remediation.md` must continue to disposition every overlapping failure. Its delta must remain removed `7`, added `0`, changed-retained `0`; any reappearance/unexplained overlap blocks.
- Flutter analyzer must remain the exact two-row atomic pair above. APK build must remain exit `0`, APK `226838368` bytes / SHA-256 `7FC34F65EAD4566D83D5BEDD4C6D186249A238727F08C4513E9896B56B34A4D9`, and exactly the 66-row inventory; second-build row delta must remain `0`. Any drift blocks.
- Baseline HEAD `9caae2aa8d619c21bc8b1677ced239de607a618f`, V1 SHA-256 `A1B20BB1B4ED6037E853C627D8A21E4369B4CBB96B412BF068AA0E4FAFE5D021`, runtime-only log SHA-256 `5F2D592ADF3E5F4276F35D878C8D6FB3ADA3109B928B21CFBF6B934D4FDD3558`, JAR `B18DF0066E81EA896EA000E37F4661CD6DA9566DEE3B0749D8D4CBA5C06C5A4F`, and APK `7FC34F65EAD4566D83D5BEDD4C6D186249A238727F08C4513E9896B56B34A4D9` must match. The test-skipped JAR is runtime/compile evidence, never a test-inclusive PASS.
- Any new failure/error/warning/notice, aggregate/ID/fingerprint/set drift, missing evidence/hash, or Story 6.9-induced delta blocks review transition.
- Independent implementation review returned `15/15 PASS — APPROVE` with no unresolved High/Medium finding. The baseline exception still does not authorize deployment or satisfy Story 6.10.

### Suspension Criteria (Điều kiện tạm dừng)

- A source conflict would materially change product scope, role access, legal processing purpose, or production deployment authority.
- PostgreSQL contains an unknown checklist status outside the allowed five-value set.
- A same-scope fix repeatedly fails due to an external environment outage; preserve evidence and resume when available.
- A RED test unexpectedly passes against the insecure/no-op implementation until its oracle is repaired.

### Execution Strategy

Run all focused and ordinary functional suites in the implementation/review gate. Defer only expensive measured performance/manual device work to the designated verification window; concurrency tests remain part of the focused backend gate. Sequence: RED focused → GREEN focused → affected regressions → full Backend/Mobile/Web commands with exact baseline comparison → manual cases → graph/independent review. Full command failures are recorded as failed-with-accepted-baseline only under the rule above; priority is not execution timing.

---

## 7. Rollback Plan

Test artifacts are additive. If a test encodes the wrong oracle, revert only that test/spec edit through a reviewed patch; never weaken production security to make it green. Application rollback follows TDS §12. V1 and Flyway history are never edited or deleted. If the optional index migration fails its evidence gate, omit it; if already applied in an authorized non-production environment, use an authorized forward repair/drop-index migration. Keep OV01-GAP-09 open until all evidence and review gates pass.

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-pattern | Check in this Test-Spec | Gate |
|---|---|---|---|
| AP-AI-001 | Unconstrained generation | every TC cites AC/FR/BR/ADR and a condition | G-0 |
| AP-AI-002 | Green-from-birth | RED logs prove old/no-op behavior cannot pass | G-2 |
| AP-AI-003 | Implicit decision | no expected field/error/role/status outside TDS | G-1 |
| AP-AI-004 | Layer violation | service/repository tests own business logic; controller tests own HTTP/security | G-4 |
| AP-AI-005 | Hallucinated contract | planned new symbols are listed in TDS §11.3; existing commands verified in module config | G-3 |

**Review result:** v1.0, the prior v1.1 snapshot, and the exact-final-binary evidence sync passed at `0 High / 0 Medium / 0 Low`; v1.1 is `Approved`. Independent implementation review remains a separate blocking Story gate.

| AP detected | TC ID | Description | Fix action | Fixed? |
|---|---|---|---|---|
| None in authoring pass | — | Deterministic checks completed: 17/17 TDS sections, 8/8 Test-Spec sections, 29/29 bidirectional conditions, and 27/27 registry/tracker aliases | rerun after final metadata sync and record hashes in handoff | Yes |
