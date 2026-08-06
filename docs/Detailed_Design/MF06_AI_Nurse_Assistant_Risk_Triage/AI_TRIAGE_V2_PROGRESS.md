# AI Triage V2 — Progress Ledger

Read this first when resuming. Do not re-derive state from scratch.

**Updated:** 2026-08-06
**Branch:** `ChuongBD`
**Starting commit:** `00ed1c986a7090e628eb6bef2dc633e8031f6217` (unchanged; 0 local commits)
**Worktree:** dirty — V2 artifacts remain untracked alongside Flutter side-effect files from
an earlier baseline run. Nothing committed, nothing pushed.

---

## Current phase

**PHASE 4 — GEMINI STRUCTURED EXTRACTION** · status **PASS (LOCAL)** (2026-08-06)
Phase 0 through Phase 4 are closed locally. V2 extraction is optional, schema-constrained, and
non-authoritative; the Java boundary is still default-disabled and GREEN remains disabled.

**Execution baseline (2026-08-06):** branch `ChuongBD`; HEAD
`00ed1c986a7090e628eb6bef2dc633e8031f6217`; dirty worktree intentionally preserved;
`git diff --check` exit 0; full Python **602 passed, 2 warnings**. No baseline file was reset,
cleaned, committed or pushed.

| Task | Objective | Status |
|---|---|---|
| **P2-T1** | Typed state + initialization/invariant tests | **DONE** |
| **P2-T2** | Deterministic input validator | **DONE** |
| **P2-T3** | Global Safety Gate | **DONE** |
| **P2-T4** | Target entity resolver node | **DONE** |
| **P2-T5** | Intent resolver node | **DONE** |
| **P2-T6** | Stage/context resolver node | **DONE** |
| **P2-T7** | Signal normalizer + conflict merger | **DONE** |
| **P2-T8** | Entity/stage validator | **DONE** |
| **P2-T9** | Dataset + scope nodes | **DONE** |
| **P2-T10** | Question planner, interrupt/resume, idempotency | **DONE** |
| **P2-T11** | Clinical rule engine + GREEN gate | **DONE** |
| **P2-T12** | Renderer, audit and complete graph wiring | **DONE** |

### P2-T1 — closed 2026-08-05

`app.triage_v2.TriageV2State` defines all 42 minimum Phase 2 fields without importing or
wiring the legacy graph. `create_initial_state` preserves caller identity/message input and
creates independent containers with explicit fail-safe defaults: target/stage/intent remain
UNKNOWN, datasets remain INCOMPLETE, scope remains UNKNOWN, and no outcome, action, ruleset,
response or reading link is synthesized. `stateVersion` is caller-provided so P2-T1 does not
pre-empt the stale-version protocol that belongs to P2-T10. Raw messages are canonicalized to
JSON values, and rule-engine dataclass traces are converted to JSON-shaped audit records so a
suppressed rule remains persistence-safe instead of being dropped.

Verified: focused P2-T1 **11/11 passed**; full Python **602 passed** (591 baseline + 11 new);
`sync_triage_rule_registry.py --check` exit 0; `git diff --check` exit 0. No Java, Docker,
Flyway, Supabase, E2E, shadow-mode or clinical-validation test was run in this task.

### P2-T2 — closed 2026-08-06

`app.triage_v2.input_validator` now enforces the graph-entry technical boundary without
calling any outcome-producing component. It validates the complete typed-state key contract
dynamically, bounded ID/text syntax, exact canonical enums, strict non-boolean integers,
finite JSON numbers, collection/depth/node limits, cycles and caller-supplied Phase 2 links.
Exact enum strings restored from JSON are normalized into existing enum members; no other
state update is returned. Controlled errors contain only stable code and top-level field name,
and the validator does not log or mutate caller input.

Verified after adversarial review fixes: focused P2-T2 + state **55/55 passed**; full Python **646 passed, 2 warnings**;
`sync_triage_rule_registry.py --check` exit 0; `git diff --check` exit 0. One initial test
assertion was corrected because the empty string is a substring of every string; the expected
`BLANK_TEXT` behavior was not changed. Java/Docker/Flyway/Flutter/E2E remain **NOT RUN** for
this Python-only atomic checkpoint.

### P2-T3 through P2-T12 — closed 2026-08-06 · PHASE 2 GATE PASS

The isolated V2 graph now has deterministic input/global/stage safety gates, canonical
target/intent/stage resolution, temporal signal normalization, conflict preservation,
entity/stage question hard filters, zero-trust dataset/scope calculations, the single existing
canonical clinical evaluator, a defense-in-depth GREEN deny gate, fixed rendering and audit.
No V1 graph, Gemini, RAG, Java, database, Supabase, or runtime Internet dependency is imported.

Question turns use a real LangGraph `interrupt()` with an in-memory Phase 2 checkpointer and
`sessionId` thread identity. Resume accepts only new request/message/latest text, expected
version, and signal/measurement deltas; it cannot overwrite session, persisted version,
idempotency ledgers, outcome, audit, or other checkpoint state. Every resume re-runs input
validation and latest-turn Global Safety before idempotency, context, rules, or planning.

Safety details locked by review: historical danger is not current; caller booleans are not
trusted presence; latest explicit danger beats an older negative observation; old GREEN/OOS is
cleared at the next safety entry; RED is monotonic; unknown/incomplete never becomes GREEN/OOS;
known wrist/exercise OOS requires a complete global screen, `possiblePregnancy=NO`, and no
reproductive evidence. Baby stages are coverage-limited rather than mapped to maternal rules.

Final gate evidence: focused graph/resume **31 passed** plus focused node suites; full Python
**754 passed, 2 warnings**; Java parity/readiness/fallback/context suites **150 passed**;
`sync_triage_rule_registry.py --check` exit 0; `git diff --check` exit 0. Docker/Flyway,
Flutter, cross-service E2E, production access, source verification, and GREEN enablement were
not part of Phase 2 and remain **NOT RUN / DISABLED**.

### Phase 3 — closed 2026-08-06 · LOCAL GATE PASS

The pre-edit audit found that canonical `triage_sessions` already provides JSONB state storage,
an owner/request unique key, owner/session lookup, and a pessimistic write query. Phase 3 therefore
adds no table and no migration. The default-disabled `/api/internal/v2/triage/sessions` boundary
reuses existing Spring auth and consent, while Java owns creation/replay, row locking, expected
state version, structured persistence, retention metadata, ruleset hash verification, timeout,
fallback, and public response shaping.

Python exposes one schema-hidden `/internal/triage/v2/turn` endpoint that is unavailable without
the configured shared secret. It rejects hash mismatch before graph execution and reconstructs
each turn from Java-persisted state using a fresh graph instance, so Phase 2's in-memory checkpointer
is not a recovery dependency. Java removes raw message/latest health text before JSONB persistence,
accepts no GREEN result, and never returns Python/RAG/Gemini-generated URLs.

Failure behavior is closed: registry or hash mismatch and Python timeout/unavailability invoke the
dependency-free Java global screen; explicit current danger is RED, while all other failures are
controlled NEEDS_MORE_INFO/unavailable. No failure can become GREEN or OUT_OF_SCOPE.

Acceptance review found one privacy gap before closure: caller-supplied free text could be nested
inside otherwise structured signal/measurement maps. Java and Python now share strict code, shape,
enum, boolean, finite-number and unit allowlists; Java also revalidates Python-returned state.

Gate evidence: Python API **8 passed**; full Python **762 passed, 2 warnings**; Java Phase 3 plus
parity/readiness/fallback/context **156 passed**; Java compile exit 0; registry sync exit 0;
`git diff --check` exit 0. Docker/Flyway bootstrap/upgrade and cross-service database E2E are
**NOT RUN** because Docker is unavailable. No production database/Supabase access occurred.
Acceptance re-review: **ACCEPT**, no directly introduced release blocker.

### Phase 4 — closed 2026-08-06 · LOCAL GATE PASS

Gemini now has one V2-only extraction method whose extra-forbid schema contains target/intent/stage,
symptom evidence, measurements, temporal expressions, negations, conflict candidates, confidence,
unknowns, language and parser warnings, but cannot express outcome, action, stop, URL, diagnosis or
treatment. Only bounded PII-sanitized current text is sent; no profile, history, consent, state or
source corpus is transmitted or logged.

Post-validation is deterministic and conservative: exact character spans, canonical registry code
membership, confidence, canonical display-phrase grounding, and explicit negation/history tokens are
required. Target and intent candidates are retained only when existing deterministic resolvers agree;
stage and measurement candidates remain non-clinical/audit-only until dedicated validators exist.
Global Safety runs before Gemini, so an already explicit RED never waits for the model; accepted
signals re-enter the graph's normal Global Safety Gate. All extraction failures simply use the
deterministic route and can never become GREEN.

Gate evidence: focused extraction/API **17 passed**; full Python **771 passed, 2 warnings**; Java
session/parity/fallback **72 passed**. Live Gemini credential test **NOT RUN**; no credential was used.
Acceptance subagent became unavailable due its external usage limit, so Phase 4 received local
code/test invariant review only; this is recorded rather than presented as external acceptance.

---

## Phase 1 task history

| Task | Objective | Status |
|---|---|---|
| **P1-T1** | Shared `TargetEntity` / `CareStage` / `IntentType` / `ContextResolutionStatus` / `ResolutionSource` contracts | **DONE** |
| **P1-T2** | Deterministic `TargetEntityResolver` (Java + Python) | **DONE** |
| **P1-T3** | Deterministic `IntentResolver` | **DONE** |
| **P1-T4** | `StageResolver` + entity–stage validator | **DONE** |
| **P1-T5** | Question catalogue metadata expansion | **DONE** |
| **P1-T6** | Hard `QuestionCatalogFilter` | **DONE** |
| **P1-T7** | Target clarification questions | **DONE** |
| **P1-T8** | Deterministic OOS complaint taxonomy (minimal) | **DONE** |
| **P1-T9** | Shared context/intent/entity parity vectors | **DONE** |

### P1-T1 — closed 2026-08-05

Canonical `context_contract_v1.json` now defines all five enums, the entity→stage map, the
legacy-stage mapping and the resolution precedence. Both runtimes declare the enums in code
and assert them against the contract, so a value added on one side alone fails a test rather
than drifting — the failure mode that broke rule parity earlier.

Two decisions worth flagging:

- **Legacy `POSTPARTUM` is not auto-mapped.** It named the maternal stage, but a postpartum
  session may equally be about the newborn. It maps only when the target is MOTHER; for BABY
  or an unresolved target it returns nothing and the caller must resolve the target first.
- **Only `SYMPTOM_TRIAGE` and `FOLLOW_UP_ANSWER` may produce a colour.** Asking what a danger
  sign *is* (`GENERAL_HEALTH_INFORMATION`) or where a source came from (`SOURCE_LOOKUP`) must
  never be answered as an assessment of the user.

Verified: Java `ContextContractParityTest` **10/10**, Python `test_context_contract_parity.py`
**13/13**, `sync --check` exit 0, `ParityResultFingerprintTest` and `GeminiOutcomeBoundaryTest`
still green (no regression from the new package).

---

## Phase 0 atomic task breakdown

| Task ID | Objective | Status |
|---|---|---|
| **PHASE0-GOV-001** | Migrate canonical registry `status=APPROVED` → `releaseStatus`, backward-compatible loaders, tests | **DONE — verified by Claude** |
| PHASE0-GOV-002 | Governance doc cleanup: remove "APPROVED & SYNCHRONIZED" and dangling `approval_manifest.json` references | **DONE** |
| PHASE0-TEST-001 | `DatasetCalculatorTest` (Java) + Python equivalent | **DONE** |
| PHASE0-TEST-002 | `ScopeCalculatorTest` (Java) + Python equivalent | **DONE** |
| PHASE0-TEST-003 | `PendingRiskEvaluatorTest` (Java) + Python equivalent | **DONE** |
| PHASE0-TEST-004 | `ExclusionAuditTest` (Java) + Python equivalent | **DONE** |
| PHASE0-TEST-005 | `SourceVerificationDerivationTest` (sync-script derivation) | **DONE** |
| PHASE0-ARCH-001 | Gemini V2 dependency architecture test (package/reference based, not source-scan); mark `GeminiTriageClient` LEGACY_V1_ONLY | **DONE** |
| PHASE0-PARITY-001 | Parity exactness: optional expectation lists asserted for equality, not just containment | **DONE** |
| PHASE0-FALLBACK-001 | Drift protection: fail a test when the registry's `globalRed` set diverges from the fallback's hard-coded set | **DONE** |

---

## Already completed (verified in earlier sessions)

- Canonical rule registry v2.1.0 reconciled against the 26-column × 10-rule internal matrix.
- Tri-state Kleene logic + 5-value `Presence` (incl. `UNAWARE_OR_UNMEASURABLE`), both runtimes.
- Fail-closed registry loading; critical-rule manifest.
- GREEN release gate locked (`greenEnabled=false`); 8 conservative release blockers.
- Safety policies split out of the clinical rule set (cyanosis holdover, self-harm).
- Decision order corrected (OUT_OF_SCOPE after YELLOW/NEEDS_MORE_INFO).
- Audit trace: decisive / all-matched / suppressed.
- Gemini fail-open closed: adapter + both dev stubs throw `AiOutcomeUnavailableException`.
- `IndependentGlobalSafetyFallback` — zero dependencies, RED or NEEDS_MORE_INFO only.
- `TriageV2ReadinessService` — registry failure no longer kills the Spring context.
- Three-manifest governance split; `approval_manifest.json` deleted.
- Source verification derived from the manifest (all rules currently `PENDING`).
- Parity vector reconciliation: 41 records, 41/41 both runtimes.

---

## Open limitations (not Phase 0 blockers)

1. Source verification is `PENDING` for all five source IDs; `BYT_1139_2026` is
   `UNRESOLVED_SOURCE` — no retrievable official document, URL deliberately left null.
   Four rules cite it. This does not block Phase 1 but blocks any verified-citation claim.
2. Docker unavailable: integration and Flyway tests are **NOT RUN**, not passing.
3. Renderer wording stays `PROVISIONAL_NOT_CLINICALLY_SIGNED` — Matrix columns
   "Lý do hiển thị" and "Mẫu hành động" were never transcribed.
4. `publicReleaseStatus` is hard-coded BLOCKED; nothing here makes V2 user-facing.

---

## PHASE0-GOV-001 — closed 2026-08-05

Ruleset **2.1.0 → 2.2.0**. All 10 rules now carry `releaseStatus: ACTIVE`; the `status` key is
gone from the canonical registry and from both generated runtime copies. Clinical fields were
fingerprinted before/after and are byte-identical. Safety policies, green blockers and
`sourceVerificationStatus` were untouched.

Both loaders accept a legacy artifact (`status: "APPROVED"`), map it to `releaseStatus=ACTIVE`
and log a deprecation warning that states explicitly it is a release flag, not validation.
Fail-closed preserved: an invalid releasable rule still raises, an unrecognised status is
refused, and a rule carrying neither field is refused rather than assumed releasable.

Verified: Java **85/85** (parity 54, legacy-compat 7, readiness 7, fallback 12, fail-open 5);
Python **424**; `sync --check` exit 0; full Java triage+ai **332 run — 1 FAIL + 4 ERROR,
identical to baseline**; `git diff --check` clean.

**Codex delegation note:** the MCP call returns `Request timed out`, but Codex *does* complete
the work. The canonical JSON edit was made by Codex and verified by reading the repo. Treat
Codex output as unavailable and always verify by inspecting files and running tests.

## PHASE 0 closed — 2026-08-05

All nine remaining atomic tasks completed and verified. What changed beyond GOV-001:

- **GOV-002** — `AI_TRIAGE_V2_RULE_RECONCILIATION.md` no longer says "APPROVED & SYNCHRONIZED"
  and no longer points at the deleted `approval_manifest.json`; stale 2.1.0 digests are marked
  stale rather than silently wrong.
- **ARCH-001** — `GeminiOutcomeBoundaryTest` checks the dependency graph by import and type
  reference (the real control); `GeminiTriageClient` is now `@Deprecated` and documented
  `LEGACY_V1_ONLY`.
- **PARITY-001** — containment assertions could not see an extra entry on one runtime. Both
  engines now canonicalise **every** result field over all 41 vectors and hash it;
  `parity_result_fingerprint.json` holds the shared digest. Java and Python independently
  produce `a506f06774cc1d94992c3fb6841b0829b27992050241f613843da757b0d87639` — full-result
  parity is now proven, not assumed.
- **FALLBACK-001** — a test fails if the registry's `globalRed` set drifts from the fallback's
  hard-coded list, which is the compensating control for that deliberate duplication.
- **TEST-001..004** — `ZeroTrustCalculatorTest` (19 tests) exercises dataset, scope, pending
  risk and exclusion audit directly, including caller-claim rejection, missing≠absent,
  conflicted, unaware, historical-not-current, and suppressed-rule audit retention.
- **TEST-005** — `test_source_verification_derivation.py` (8 tests) pins the derivation table,
  the null URL for the unlocatable Vietnamese citation, and the WHO audience limitations.

### Gate evidence

| Command | Result |
|---|---|
| `sync_triage_rule_registry.py --check` | PASS exit 0 (caught a stale integrity manifest first — regenerated) |
| `pytest -q` (Python service) | **PASS 433** |
| Java V2 rules package (9 classes) | **PASS 110/110** |
| Java triage+ai suite | **360 run — 1 FAIL + 4 ERROR, identical to baseline** |
| `git diff --check` | clean |

### P1-T2 / P1-T3 — closed 2026-08-05

`TargetEntityResolver` and `IntentResolver` exist in both runtimes, reading the shared
`target_entity_indicators_v1.json` and `intent_indicators_v1.json`. Both return the resolved
value **and** the `ResolutionSource` that produced it, so an audit can show whether the user
stated it or the engine inferred it.

**Two Vietnamese accent-collision bugs found and fixed** — the same class of defect twice:

1. Clause splitting on accent-folded text cut `"giúp con tôi"` in half, because the
   conjunction **"còn"** folds to **"con"** (child). Clauses are now split on the *accented*
   text and folded only afterwards, once the boundaries are fixed.
2. `"dấu hiệu cảnh báo là gì?"` was classified as a symptom report, because **"dấu"** (sign)
   folds to **"đau"** (pain). Short ambiguous markers are now matched **with accents**, whole
   word only; longer multi-word phrases still match folded so unaccented typing still works.

Behavioural decisions worth carrying forward:

- A possessed child outranks the pronoun that introduced it — *"tôi hỏi giúp con tôi, bé bị
  sốt"* is BABY.
- A message naming both is **CONFLICTED**, never a silent pick. One session, one target.
- `POSTPARTUM_MOTHER` alone does **not** resolve the target: postpartum is exactly when a
  newborn question is most likely.
- A third party's symptom ("bạn tôi bị đau bụng") is UNKNOWN, not the user's triage.
- A symptom report **wins** over a general question in the same message — an untriaged real
  symptom is the worse error.

Verified: Python `test_target_entity_resolver.py` **32**, `test_intent_resolver.py` **23**;
Java `TargetEntityResolverTest` **30**, `IntentResolverTest` **22**. Full Python **501 passed**;
Java triage+ai **419 run — 1 FAIL + 4 ERROR, identical to baseline**; `sync --check` exit 0.

### P1-T4 / T5 / T6 / T7 — closed 2026-08-05

`StageResolver` (both runtimes) resolves the stage **and** validates it against the entity. A
cross-entity pair is `CONFLICTED`, never silently corrected: `BABY` + `PREGNANCY` means the
engine is about to reason about a pregnancy belonging to someone who is not the subject.
Legacy `POSTPARTUM` with a BABY target is likewise conflicted rather than guessed. Both a
gestational week and a postpartum day present at once is a conflict, not a preference.

`resolveContextStatus` folds target + stage + intent into one status, reporting a **conflict
before a gap** — a contradiction cannot be fixed by asking one more question.

The question catalogue now carries `targetEntities`, `applicableStages`, `applicableIntents`,
`priority`, `escalationSignals`, `requiresResolvedTarget/Stage` and the three clarification
flags. Four questions were added: `Q_CLARIFY_TARGET_ENTITY`, `Q_CLARIFY_TARGET_FIRST`,
`Q_CLARIFY_INTENT`, `Q_BABY_AGE_MONTHS`.

`catalog_filter.py` is the gate that makes invariant 12 enforced rather than conventional.
While the context is unresolved it collapses to clarification questions only — **a caller
passing symptom question ids cannot bypass it**. One bug found and fixed during testing:
clarification questions leaked into an already-RESOLVED context, which would have asked
"whose symptom is this?" after the subject was settled.

Python **561 passed**; Java `StageResolverTest` **29**.

### P1-T6b / T8 / T9 — closed 2026-08-05 · PHASE 1 GATE PASS

The question catalogue became a canonical artifact (`question_catalog_v1.json`) rather than
two hand-written copies — two runtimes with two catalogues would eventually ask different
questions of the same patient. Python now loads it too. `QuestionCatalogFilter` ported to
Java. `oos_complaint_taxonomy_v1.json` added with `ComplaintTaxonomy` in both runtimes, and
14 shared context vectors run identically on both sides.

**A third Vietnamese accent-collision bug, different mechanism.** Phrase matching used raw
substring containment, so **"tã"** (nappy) → folded `"ta"` → matched *inside* `"tay"` (hand)
and `"tập"` (exercise). "Tôi bị đau cổ tay sau khi tập thể thao" therefore scored as a **baby**
message. All six matchers (three per runtime) now anchor phrases on word boundaries.
Verified the fix did not over-correct: "Bé bị hăm tã" still resolves to BABY.

Two vector failures were resolved differently, deliberately:

- `CV_PREGNANCY_STAGE_WITH_BABY_TARGET` — a real gap: classic infant symptom words ("bỏ bú",
  "quấy khóc", "ọc sữa", "vàng da", "li bì") were missing from the intent markers. Indicators
  fixed.
- `CV_GENERAL_INFORMATION_QUESTION` — my **expectation** was over-specified. "thai kỳ" does
  genuinely indicate the mother even inside a general question, and recording that is harmless
  because intent already forbids a colour. The vector was corrected, not the behaviour, and
  the description now says why.

**OUT_OF_SCOPE requires positive evidence.** The taxonomy lists only categories we are
confident sit outside obstetric/paediatric scope; an unrecognised complaint is NEEDS_MORE_INFO.
Each category carries `excludedWhenAlso`, so a swollen painful leg after birth is not
"musculoskeletal" and a fall with bleeding is not a "sports injury".

Gate evidence: Python **591 passed**; Java context suites **66 + 15 + 16 + 14**; Java
triage+ai **493 run — 1 FAIL + 4 ERROR, identical to baseline**; `sync --check` exit 0.

## Phase 5 — source verification and post-outcome RAG — closed 2026-08-06

V2 now has a separate local-only post-outcome evidence path. Eligibility requires exact
`SOURCE_VERIFIED` status, an allowlisted HTTPS deep-link, section, organization, target/stage/language
and rule compatibility, plus an exact SHA-256 match over the local body. Deterministic BM25 ranks only
that eligible set. Python never runtime-browses or runs an agent loop; Java independently revalidates
every returned citation against the database-backed deep-link allowlist before persistence/response.

The audit found **13 local documents, 0 SOURCE_VERIFIED**. Citations therefore remain empty by design;
official URL/section/publication/content verification is an external source-owner blocker. No pgvector
was added because this corpus size and verified set provide no evidence of benefit. RED explicitly skips
the evidence registry/retriever, and retrieval cannot mutate outcome/action/stop.

Evidence: focused Python **73 passed**; full Python **778 passed**; Java
`TriageV2SessionServiceTest` **8 passed**, compile/selected parity-fallback command exit 0;
`sync_triage_rule_registry.py --check` exit 0; `git diff --check` exit 0.

## Phase 6 — Flutter V2 flow — closed 2026-08-06

Flutter now has an isolated typed V2 model/service/screen wired to the internal Java endpoint. It shows
target and stage, uses stable target option codes, supports free-text/option rounds, preserves mutation
IDs on retry, refreshes 409 stale versions, cancels with the authoritative version, and creates a second
session rather than mixing mother/baby. RED action renders before evidence. YELLOW, NMI, OOS and
controlled unavailable are explicit; public GREEN is rejected by the parser. Source cards require
`SOURCE_VERIFIED`, `LOCAL_BM25`, HTTPS/domain binding and SHA-256.

The route is internal compile-time gated and defaults false, so V1 remains user-facing. Evidence:
focused V2 **9 passed**; changed-file analyze **0 issues**; later completion audit non-golden aiTriage **145 passed**.
Whole-app analyze retains 34 unrelated baseline findings. The full aiTriage run has one existing golden
failure (`triage_stage_selector_mobile`, 31.32%); its failure images were dirty at baseline and untouched
by the V2 implementation. Registry sync and diff check exit 0.

## Phase 7 — evaluation, security, privacy and observability — closed 2026-08-06

A reproducible 14-case evaluation corpus now measures global/stage RED, pending RED, OOS, target,
conflict, temporal history, prompt injection and colloquial Vietnamese. It found one real gap (`tui` was
UNKNOWN); the shared canonical target indicator was fixed and synced to both runtimes. Final evaluation:
**14/14**, RED recall **4/4**, target accuracy **100%**, wrong entity/question **0**, OOS false positive
**0**, unsupported GREEN **0**. Citation precision is honestly not measurable with zero verified sources.

Python/Java telemetry now records only closed categories and numeric aggregates for outcomes, latency,
questions, fallback, hash mismatch, extraction/citation rejection, target conflict and stale version.
Focused Python **66**, full Python **780**, selected Java **54**, all passed; sync/diff checks exit 0.

Security/privacy audit passes auth, consent, internal secret, owner scoping, redaction, minimization,
exception/prompt secrecy and retention metadata. **Blocker:** `retentionUntil` is not an enforced purge;
completed snapshots are immutable and the repository's account-deletion flow has no permanent processor.
No unsafe trigger bypass or unapproved migration was introduced.

## Phase 8 — internal shadow mode and E2E/chaos — partial 2026-08-06

Java now has a default-off asynchronous V2 shadow runner integrated after V1 result construction. It
calls only the internal Python workflow with ephemeral state, has no persistence collaborator, and emits
only MATCH/MISMATCH/ERROR counts. V1 remains user-facing; disabled shadow makes no call, and mismatch or
Python failure cannot escape the shadow boundary.

Executed evidence: Python component/chaos **55 passed**, Java shadow/session/metrics selection exit 0,
Flutter V2 contracts **9 passed**. Mother, baby, ambiguity, OOS, global/pending RED, Python failure,
registry/hash, Gemini, corpus, citation, duplicate/stale/resume and shadow failure were exercised at the
component boundary.

**NOT RUN:** full Flutter→Java→Python→PostgreSQL→GET, DB timeout, Flyway bootstrap/upgrade and live auth
expiration. Docker/PostgreSQL and live credentials are unavailable. Phase 8 is therefore PARTIAL, not
full-stack E2E PASS.

## Phase 9 — GREEN release gate — closed 2026-08-06

GREEN now has explicit report-only statuses separate from runtime enablement. Current machine-readable
decision: runtime **DISABLED**, eligibility **BLOCKED_BY_SOURCE_COVERAGE**, automatic enablement false.
The evaluation sub-gate passes, but verified source/rule/dataset coverage, full E2E, deletion enforcement,
internal release decision, clinical validation and external sign-off remain blockers.

Defense in depth remains: Python graph denies candidate GREEN, Java reports exact blocked status and
keeps flags false, and Flutter rejects public GREEN. Focused Python GREEN/graph/rule **34 passed**; Java
readiness/session command exit 0; decision CLI exit 0.

## Resume point

**Next phase: Phase 10 — controlled V1 cleanup and final documentation.** Inventory every legacy caller,
mark (do not delete) forbidden/legacy outcome and open-web paths, document rollback/deprecation, produce
architecture/state/API/rules/sources/privacy/evaluation/operations/demo/limitations guides, run final
cross-runtime gates, and regenerate the final patch/result artifacts without commit or push.

Environment: Docker unavailable (integration/Flyway cannot run). Codex MCP always reports
`Request timed out` but still completes the work — verify by reading the repo.

Environment notes: Docker unavailable (integration/Flyway tests cannot run). Python venv at
`05_Development/CareBridgeAITriageService/.venv`. Maven wrapper at `05_Development/CareBridgeAPI/mvnw`.

## Phase 10 — controlled cleanup and documentation — closed locally 2026-08-06

Legacy V1 graph, outcome adapter, source-retrieval and runtime-search paths are now explicitly marked
legacy/deprecated and remain intact for rollback. The caller inventory records ownership and removal
conditions. V2 no longer imports the legacy source retriever; dependency-boundary tests prevent V2 from
reaching legacy graph/rules/open-web or agent primitives.

The final architecture/operations guide covers governance, contracts, state machine, entity/intent/stage,
rules, evidence, API, questions, fallbacks, privacy, evaluation, operations, rollback, demo and known
limitations. V1 remains public, while V2 UI, shadow and GREEN controls remain default-off.

Final local gates: Python **794 passed** (2 third-party warnings); Java compile plus selected
V2/parity/fallback command exit 0 with **153 tests**; Flutter changed-file analyze **0 issues** and
non-golden aiTriage **145 passed**; registry sync and diff check exit 0.

## Final resume point

All locally executable implementation work is complete. Resume with **official source verification and
manifest population**, then rerun Phase 5 eligibility and Phase 9 assessment. In parallel under explicit
owner authority: define retention/deletion policy and processor; provision Docker/PostgreSQL/live auth
for the Phase 8 E2E/Flyway/chaos matrix; obtain clinical and release sign-off; run the approved shadow
window. Do not change public V2 or GREEN flags before every gate is evidenced.

## Completion audit — closed locally 2026-08-06

Requirement-by-requirement review found and fixed multiple cross-layer gaps: Flutter no longer invents a
consent version; selected owned target now reaches Python while client-authored clinical context is rejected;
invalid citations no longer downgrade YELLOW; verified retrieval now prioritizes Vietnamese sources and caps WHO at one in Python
with a Java cap. Phase 7 now maps all 15 requested evaluation groups and all 13 metric groups, keeping
unmeasurable citation/fallback/parity values null with explicit component/runtime evidence.

The audit also narrowed an earlier claim: Java fallback and global-gate tests consume trusted structured
signals. Free-text-only danger detection with Gemini disabled or Python unavailable is not full-stack
proven and cannot be safely invented without a source/rule-reviewed phrase-to-signal contract. It joins
official source coverage, deletion enforcement, Docker DB/Flyway/live-auth E2E, clinical/release sign-off
and shadow observation as an explicit rollout blocker. Overall status remains PARTIAL; safe local scope
is complete.
