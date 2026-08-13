# AI Triage V2 — Decision Log

Append-only. Each entry records what was decided, why, and what it touches.
Decider is either **DEV TEAM** (product/engineering choice) or **CLAUDE REVIEW**
(orchestration/architecture call made during implementation).

---

## D-001 — One canonical rule registry, two generated runtime copies
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** Java and Python must evaluate identical rules, but their Docker build contexts are
separate directories, so a shared runtime path is unreachable.
**Decision** Single canonical file under `05_Development/Contracts/triage/`; both runtimes get a
generated copy plus a `.sha256` sidecar; each loader verifies the digest before parsing;
`sync_triage_rule_registry.py --check` fails CI on drift.
**Alternatives rejected** Shared volume (breaks containerisation); rule service over HTTP (adds a
runtime dependency to the thing that must survive outages); duplicating rules by hand (the
original defect).
**Impact** Java, Python, CI. No DB.

## D-002 — Tri-state Kleene logic with a five-value Presence
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** Boolean evaluation let `not(missing)` be true, so an unanswered question could "prove"
a symptom absent.
**Decision** `TRUE/FALSE/UNKNOWN` Kleene logic; `Presence = PRESENT | ABSENT | UNKNOWN |
CONFLICTED | UNAWARE_OR_UNMEASURABLE`; the last three never read as a denial; `NOT_EXISTS` means
"no value captured", not "symptom absent".
**Alternatives rejected** Boolean with a separate missing-set (error-prone at every call site).
**Impact** Java, Python, parity vectors, contract.

## D-003 — GREEN locked behind a release gate
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** The rule set covers only a subset of danger signs, so "no rule matched" is not
evidence of low risk.
**Decision** `releaseGates.greenEnabled=false`; a would-be GREEN becomes
`NEEDS_MORE_INFO / ROUTE_TO_HEALTHCARE_WORKER`; 8 conservative release blockers block GREEN
without asserting any clinical outcome.
**Alternatives rejected** Enabling GREEN because the default rule is present in the matrix —
rejected as unsafe given incomplete coverage.
**Impact** Both engines, renderer, contract. Reported openly as a limitation.

## D-004 — Safety behaviours kept outside the clinical rule set
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** Cyanosis and self-harm escalation exist in V1 but are absent from the internal rule
matrix. Dropping them would regress safety; labelling them approved would be false.
**Decision** Separate `safetyPolicies[]` with their own status, provenance, owner team and review
deadline. No code path promotes a policy into a clinical rule.
**Alternatives rejected** Deleting them (safety regression); marking them APPROVED (false claim).
**Impact** Registry schema, both loaders, fallback.

## D-005 — Decision order corrected: OUT_OF_SCOPE after YELLOW
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** An earlier draft evaluated OUT_OF_SCOPE before YELLOW, so a genuinely reproductive
case could be dismissed as out of scope.
**Decision** safety policies + global RED → stage RED → pending RED → YELLOW → NEEDS_MORE_INFO →
OUT_OF_SCOPE → GREEN (gated) → controlled fallback.
**Impact** Both engines, parity vectors.

## D-006 — Fail-closed registry loading, isolated from application liveness
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** `TriageRuleRegistry` was a `@Component` whose constructor threw, so a bad artifact
failed Spring context creation and took unrelated modules down.
**Decision** Registry and evaluator are no longer Spring beans; `TriageV2ReadinessService`
constructs them, converts failure into a readiness state, and returns `Optional.empty()` for the
evaluator so nothing runs on a partial rule set. Technical, public-release and clinical-validation
status are three independent axes.
**Alternatives rejected** Keeping the throwing bean (blast radius); loading best-effort (a missing
emergency rule would silently become "no rule matched").
**Impact** Java only.

## D-007 — Independent Global Safety Fallback with a hard-coded signal set
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** With the registry invalid there was no path left to recognise a seizure.
`TriageRedFlagPreScreenPolicy` could not serve because it reads `RedFlagRuleRepository`.
**Decision** A dependency-free screen over 8 hard-coded global danger signals; only RED or
NEEDS_MORE_INFO reachable; `screen()` takes signals only, so no caller flag can suppress it;
`current=false` signals are ignored.
**Known risk** The hard-coded set can drift from the registry — PHASE0-FALLBACK-001 adds a test
that fails on divergence.
**Impact** Java only.

## D-008 — Gemini fail-open closed by throwing, not by defaulting
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** The adapter returned GREEN on outage **and** as the default before parsing; both dev
stubs returned GREEN. An outage produced the most reassuring answer available.
**Decision** `AiOutcomeUnavailableException` on outage and on an unparseable response; both stubs
throw. `RiskLevel` has no "cannot determine" value, so failing loudly is the only safe signal;
callers must map it to NEEDS_MORE_INFO, never GREEN.
**Impact** Java only. No production caller existed, so blast radius was nil.

## D-009 — Three-manifest governance split
**Date** 2026-08-05 · **Decider** DEV TEAM (positioning) + CLAUDE REVIEW (structure)
**Problem** `approval_manifest.json` used `PO_CONFIRMED_DOCTOR_REVIEW` and "approved by
obstetric/pediatric clinical advisors", implying clinical sign-off that never happened.
**Decision** Delete it. Split into `artifact_integrity_manifest.json` (checksums only),
`source_verification_manifest.json` (provenance), `internal_rule_review_manifest.json`
(engineering review). None may be presented as clinical approval.
**Impact** Artifacts, sync tool, reports.

## D-010 — Rule source verification is derived, never hand-set
**Date** 2026-08-05 · **Decider** CLAUDE REVIEW
**Problem** The registry claimed `SOURCE_VERIFIED` on every rule while the manifest said PENDING.
**Decision** `sync_triage_rule_registry.py` computes each rule's `sourceVerificationStatus` from
the manifest; `--check` fails if the registry disagrees. All rules are currently `PENDING`, and
`BYT_1139_2026` is `UNRESOLVED_SOURCE` with a null URL — deliberately not guessed.
**Impact** Artifacts, sync tool. Downgrades an earlier team assertion, on purpose.

## D-011 — Legacy `status=APPROVED` must be migrated, not annotated
**Date** 2026-08-05 · **Decider** DEV TEAM
**Problem** A previous session kept `status=APPROVED` as a compatibility field with an
explanatory note. The note does not stop a reader — or another tool — from reading "APPROVED" as
clinical approval.
**Decision** Migrate the canonical schema to `releaseStatus` (`DRAFT|ACTIVE|DISABLED|RETIRED`)
plus explicit `internalReviewStatus`, `sourceVerificationStatus`, `clinicalValidationStatus`.
Loaders accept legacy `status=APPROVED` from older artifacts, map it to `releaseStatus=ACTIVE`,
and emit a deprecation warning; newly generated runtime copies must not contain `APPROVED`.
**Alternatives rejected** Keeping the annotated legacy field (the current defect); a silent
rename (would erase the deprecation signal).
**Impact** Canonical registry, both loaders, sync tool, parity vectors. Ruleset version bump.
**Implemented by** PHASE0-GOV-001.

## D-012 — Codex delegation re-enabled for implementation tasks
**Date** 2026-08-05 · **Decider** DEV TEAM
**Problem** An earlier instruction had disabled Codex consultation entirely.
**Decision** Codex is re-enabled as an implementation worker under the atomic-task contract in
the master plan. Claude retains architecture authority, diff review, test verification and all
PASS/FAIL decisions, and does not accept a Codex PASS claim without evidence.
**Impact** Process only.

## D-013 — Phase 2 uses checkpointed safety-first turns and one clinical evaluator
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** A question turn must suspend and resume without letting the resume payload overwrite
trusted checkpoint state, and pending-rule questions cannot be planned before the canonical
engine identifies them.
**Decision** Compile the isolated V2 LangGraph with a Phase 2 in-memory checkpointer keyed by
`sessionId`; use `interrupt()` at the question boundary; allow only new request/message identity,
latest text, expected version and structured signal/measurement deltas on resume; re-enter input
validation and latest-turn Global Safety before any other node. The existing canonical evaluator
runs before final catalog filtering/planning because it supplies unresolved-rule candidates.
Every terminal path then crosses a separate GREEN-deny gate, fixed renderer and audit node.
**Alternatives rejected** Resume directly into the planner (skips new danger); shallow-merge the
resume dictionary (checkpoint injection); duplicate rule engine (parity drift); plan before rule
evaluation (pending RED questions unavailable).
**Impact** Python Phase 2 only. Java remains persistence/idempotency authority in Phase 3;
in-memory checkpointing is not a production persistence decision.

## D-014 — Reuse triage_sessions JSONB; Java owns recoverable V2 turns
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** Phase 2 used an in-memory LangGraph checkpointer, while Phase 3 needs durable,
idempotent turns without creating a second session model or trusting Python with auth/state version.
**Decision** Reuse `triage_sessions.result_jsonb`, the existing owner/request unique key and row
lock. Java persists the sanitized full graph state and reconstructs each Python turn from it; Python
uses a fresh graph per transport request. Java increments the accepted version, validates session
identity and ruleset hash, and strips raw health text before persistence. No Flyway migration is
needed. The internal Java flag defaults false and Python requires the existing shared secret.
**Alternatives rejected** New V2 tables (parallel source of truth); relying on process-local
checkpoints (not recoverable); persisting raw conversations (unnecessary sensitive data); partial
execution on hash mismatch (parity risk).
**Impact** Phase 3 Java/Python boundary. Public routing and GREEN remain disabled.

## D-015 — Gemini V2 is grounded extraction only and runs behind pre-safety
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** Free-form model output or model latency must not decide/delay an already known safety
outcome, and exact-span grounding alone does not prove a model-assigned clinical signal code.
**Decision** Use an extra-forbid extraction schema with no outcome/action/stop/URL/diagnosis/treatment.
Run deterministic Global Safety before any Gemini call. Post-validation requires canonical code
membership plus a canonical display-phrase match inside exact copied spans; negation and historical
classification require independent lexical markers. Existing deterministic resolvers must agree with
target/intent candidates. Stage/measurement candidates remain audit-only for now.
**Alternatives rejected** Letting Gemini write graph context/signals directly (model authority);
calling extraction before safety (RED latency); accepting arbitrary grounded spans (semantic
hallucination); using the legacy Java outcome interface.
**Impact** Python Phase 4 only. Gemini remains optional; failures use deterministic fallback/NMI.

## D-016 — V2 evidence is local, hash-pinned, and empty until genuinely verified
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** The repository has 13 legacy source documents but none declares `SOURCE_VERIFIED`;
the legacy retriever also accepts `APPROVED`/`PENDING` and exposes runtime official-search paths.
**Decision** V2 uses a separate local-only BM25 retriever after outcome. A document is eligible only
when it declares `SOURCE_VERIFIED`, its body matches its SHA-256 hash, and its HTTPS deep-link,
section, organization, target/stage/language/rules and domain pass deterministic filters. Java
revalidates citations against the database-backed deep-link allowlist. RED skips retrieval entirely.
The current verified corpus is therefore empty and citations remain empty.
**Alternatives rejected** Treating legacy `APPROVED` as verified (misleading governance); runtime
web search (forbidden and nondeterministic); pgvector for 13 documents/zero verified examples (no
measured benefit); inventing source metadata to unblock demos.
**Impact** Phase 5 Python/Java V2 path. Source coverage remains an external blocker; outcome logic
and public GREEN remain unchanged.

## D-017 — Flutter V2 is typed, fail-closed, and internal-gated
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** Reusing the V1 mobile result model would admit legacy GREEN/citation semantics and would
not carry the V2 state-version/idempotency contract. Directly replacing the V1 route would also violate
the required shadow rollout.
**Decision** Add a separate typed V2 model/service/screen. Parsing supports only RED, YELLOW,
NEEDS_MORE_INFO and OUT_OF_SCOPE, revalidates citations, retains mutation IDs across retry, refreshes
409 stale state, and creates a second session rather than mixing mother/baby. The internal route is
compile-time gated with default false; V1 remains the normal route.
**Alternatives rejected** Patch V1 DTOs in place (semantic coupling); display any server citation
(trust-boundary bypass); enable V2 route by default (premature rollout); treat transport failure as a
result (could imply safety).
**Impact** Flutter Phase 6 only. No production/public enablement.

## D-018 — Evaluation drives shared-contract fixes; telemetry accepts closed labels only
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** A static test suite did not measure required safety rates or Vietnamese colloquial coverage;
free-form telemetry labels could leak health text/IDs and create unbounded cardinality.
**Decision** Add a deterministic, machine-readable V2 evaluation corpus/report and bounded metrics in
both runtimes. Metric APIs accept only fixed outcomes/failure enums and numeric aggregates. The corpus
found `tui` unresolved; the fix was made in the canonical target indicator and synced to both runtimes.
Citation precision remains null while the verified corpus is empty.
**Alternatives rejected** Manual spreadsheet scores (not reproducible); logging request/exception text
(privacy risk); Python-only colloquial fix (parity drift); reporting empty-set citation precision as 100%.
**Impact** Phase 7 evaluation, canonical context contract runtime copies, Python/Java observability.

## D-019 — Retention expiry is not deletion authority
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending policy review)
**Problem** V2 writes `retentionUntil`, but completed triage snapshots are protected by an immutable DB
trigger and the account-deletion feature only schedules requests; no permanent triage purge processor is
present. Treating metadata as enforced deletion would be a false privacy claim.
**Decision** Record this as a public-rollout blocker. Do not bypass immutable audit controls or add a
privileged purge/migration without repository-wide retention/legal policy, database integration tests and
explicit approval.
**Alternatives rejected** Claiming deletion is implemented; deleting completed snapshots through normal
application credentials; weakening the immutable trigger inside the V2 module.
**Impact** Phase 7 audit and Phase 9 release gate. Local functional work continues independently.

## D-020 — Shadow comparison is asynchronous, ephemeral and categorical
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** Reusing the durable V2 session service for shadow traffic would create duplicate patient
records and side effects; synchronously comparing V2 could delay or break the V1 user path.
**Decision** Run shadow through the internal Python workflow transport only, using a fresh ephemeral
session and no persistence collaborator. Dispatch after V1 has built its result, isolate all failures,
and retain only MATCH/MISMATCH/ERROR counters. The flag defaults false.
**Alternatives rejected** Durable V2 shadow sessions (duplicate records); raw diff storage (privacy);
synchronous fail-closed coupling to V1 (availability regression); default-on rollout.
**Impact** Phase 8 Java V1 integration. V1 remains user-facing; no public V2/GREEN enablement.

## D-021 — GREEN eligibility is report-only and separate from runtime enablement
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending release authority review)
**Problem** A single `BLOCKED` flag hides which gate failed, while deriving runtime enablement from a
passing assessment could accidentally expose GREEN after a future data/test change.
**Decision** Assess explicit GREEN eligibility statuses and blockers, but keep runtime status separate.
The assessor has no mutation/config write path and reports `automaticEnablement=false`. The Python graph
deny, Java default-off flags and Flutter GREEN rejection remain independent controls. Current runtime is
DISABLED; eligibility is BLOCKED_BY_SOURCE_COVERAGE.
**Alternatives rejected** One generic BLOCKED status (not actionable); auto-enabling on green CI (unsafe);
removing the graph deny once an assessor exists (single point of failure).
**Impact** Phase 9 release reporting/readiness only. No public enablement.

## D-022 — Deprecate by boundary and evidence, not deletion
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending release authority review)
**Problem** Removing the V1 graph, Gemini outcome adapter, or legacy source paths now would eliminate the
only public rollback path before database E2E, source coverage, privacy enforcement, shadow observation,
and clinical approval exist.
**Decision** Mark legacy components explicitly, inventory every caller, and prohibit new V2 dependencies
through an executable boundary test. Retain V1 as the public route and remove legacy code only after the
documented exit gates and a separate approved migration decision.
**Alternatives rejected** Immediate deletion (unsafe rollback loss); leaving boundaries undocumented
(new coupling risk); silently routing users to V2 (unapproved release).
**Impact** Phase 10 documentation and dependency hygiene. Runtime behavior and public exposure do not
change.

## D-023 — Java owns consent version; selected target and journey context are explicit contracts
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** Flutter sent a made-up `V2` disclaimer version, while Java's canonical policy may differ;
Java also accepted selected target/journey context but silently omitted them from the Python payload.
**Decision** Mobile sends an empty consent context and Java checks active canonical consent. Java and
Python carry a closed selected-target/journey schema; explicit current message remains higher precedence
than profile selection, and unknown health-text fields are rejected before persistence.
**Alternatives rejected** Duplicating the policy version in Flutter (drift); trusting arbitrary journey
maps (PII/schema bypass); ignoring the fields (broken entity/stage UX).
**Impact** Phase 3/6 internal contract only. Feature flags and public routing do not change.

## D-024 — Evidence rejection is non-clinical and cannot downgrade an outcome
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending whole-diff review)
**Problem** Java citation validation previously escaped into the workflow catch and replaced a valid
YELLOW result with fallback NMI, violating the post-outcome RAG boundary. Vietnam priority and the
max-one-WHO requirement were also not encoded.
**Decision** Isolate citation failure to an empty list plus bounded reject metric, retain outcome/action,
prioritize verified `.vn` documents after relevance filtering, and cap WHO at one in Python and Java.
**Alternatives rejected** Trusting Python citations (boundary bypass); downgrading the clinical result
(RAG authority leak); ranking nationality before verification/relevance (unsafe source selection).
**Impact** Phase 5 evidence selection and Java response validation. No production source is promoted.

## D-025 — Completion evidence distinguishes structured-signal proof from free-text E2E
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending rule/source-owner review)
**Problem** Global RED evaluation and Java fallback tests inject trusted structured signals; describing
them as proof of free-text-only danger detection would overstate the current end-to-end capability.
**Decision** Mark every evaluation input mode, trace all required categories/metrics, and explicitly
block free-text→global-signal inference without a source/rule-reviewed deterministic contract. Keep
unmeasurable metrics null and keep Phase 8 overall PARTIAL.
**Alternatives rejected** Inventing a clinical phrase lexicon (unreviewed safety logic); relying on Gemini
before the Global Safety Gate (ordering violation); calling component evidence full E2E.
**Impact** Evaluation, documentation and rollout posture only. Structured-signal safety behavior remains.

## D-026 — Client clinical state is untrusted; server provenance is mandatory
**Date** 2026-08-06 · **Decider** CODEX IMPLEMENTATION (pending security/rule-owner review)
**Problem** The mobile-facing DTO exposed `signals`, `measurements`, and `journeyContext`. Shape
validation alone did not prove provenance, so an authenticated caller could turn UNKNOWN into ABSENT or
provide stage facts that influence OOS routing. This supersedes D-023's decision to forward journey context.
**Decision** Java rejects non-empty caller-authored clinical state, validates selected baby ownership,
and only forwards an ownership-checked selected target. Trusted structured fields may be reintroduced only
through a server-side canonical option/profile mapper with executable provenance tests. Python's shared-key
transport remains an internal Java-to-Python boundary, not a public client contract.
**Alternatives rejected** Trusting canonical-looking values (no provenance); silently dropping the values
(ambiguous contract); inventing a text lexicon without rule/source review (unsafe clinical logic).
**Impact** Existing component fallback RED tests do not establish free-text public E2E safety. Public
rollout remains blocked on the reviewed server-derived text/answer-to-signal contract.

## D-027 — The V2 danger floor conflicts with D-025/D-026 and needs a ruling
**Date** 2026-08-08 · **Decider** PROJECT OWNER (ChuongBD), ruled 2026-08-08. Claude implemented, then
found the conflict and disclosed it.
**RULING — option (a), narrowly.** The floor stays. This supersedes D-025/D-026 **only** for the five
global signals it emits, **only** inside V2, and **only** while V2 is default-off. It grants nothing else:
- Public rollout stays blocked. D-026's condition — a reviewed server-derived text/answer-to-signal
  contract — is unchanged and unmet.
- No new phrase may be added to the lexicon under this ruling. Adding one is a fresh decision.
- D-025's block on free-text→global-signal inference stays in force everywhere else, including V1's
  public path and any future cutover.
- If V2 is ever cut over to real users, this ruling expires and the reviewed contract is required first.
**Problem** `app/triage_v2/deterministic_signals.py` (commit 6f9d30e5) infers global danger signals from
free text using a phrase lexicon. D-025 decided to "explicitly block free-text→global-signal inference
without a source/rule-reviewed deterministic contract" and rejected "inventing a clinical phrase lexicon";
D-026 rejected "inventing a text lexicon without rule/source review". The floor was built without checking
this log — a process failure, disclosed here rather than left for someone to find.
**What it actually does** Emits SELF_HARM_IDEATION, SEIZURE, SEVERE_BREATHING_DIFFICULTY,
ALTERED_CONSCIOUSNESS and CYANOSIS as signals only; `global_safety_gate` remains the sole decider. A
question answer outranks it, so an explicit ABSENT is never overwritten. It runs in V2, which is
default-off, so nothing public changed.
**Why it was built** Measured 2026-08-08: with Gemini unavailable, "Tôi không muốn sống nữa" produced
only `Q_CLARIFY_INTENT` — a suicidal disclosure answered with a categorisation question, exactly when the
system was already degraded. The other four danger sentences degraded safely to the danger screen.
**Mitigating, but not sufficient** The lexicon is not invented: it is V1's existing production phrase set,
reached by every V1 user today, now corpus-tested in both directions (`test_vietnamese_intake_corpus.py`).
"Already shipping" is nonetheless not "reviewed", which is what D-025 requires.
**Options** (a) Supersede D-025/D-026 for this narrow scope, on the record, and keep the floor —
public rollout stays blocked regardless; (b) revert 6f9d30e5 and leave the self-harm gap open until a
reviewed text→signal contract exists; (c) keep it but gate it behind an explicit off-by-default flag.
**Impact until ruled** No public behaviour. AI_TRIAGE_MERGE_PLAN.md §5 must not be read as evidence that
free-text E2E danger detection is established — D-025 is precisely about not making that claim.

## D-028 — V2 is frozen; V1 ships
**Date** 2026-08-08 · **Decider** PROJECT OWNER (ChuongBD), ruled 2026-08-08. Recommended by Claude.
**ACCEPTED.** V2 is frozen at commit 6f9d30e5. In practice: no new V2 features, no cutover work, flags
stay off, and the suites stay green so it does not rot. Effort goes to V1 and to evidence — the corpus —
not to architecture.
**Problem** V2 is ~6.2k lines of code and ~8.7k of tests that have never served a request, while V1
serves every user. Most of the machinery that makes this area feel heavy — parity vectors, shadow
service, cutover gate, readiness blockers, two mobile screens — exists only because two engines run side
by side. Left undecided, V2 keeps growing and the coexistence cost keeps compounding.
**Decision** Freeze V2 at commit 6f9d30e5: no new features, flags stay off, but it stays building and
green so it does not rot. Ship V1, whose real defect was the phrase matching (fixed in da3556b6), not the
architecture. Do not delete V2 — it is designed, tested and shadow-validated work, and it is the fix for
the fragility V1 has by construction.
**Alternatives rejected** Cutting over one stage now — V2 has no chat experience (`triage_v2_screen` is
an internal harness), so this is weeks of work for one person and lands two engines in production;
half-finished at a deadline is the worst of the three outcomes. Deleting V2 — destroys the better
architecture and the evidence of it.
**Unfreezes when** someone decides to cut over AND V2 has a chat entry AND D-027 is ruled on.
**Impact** No code change. It is a commitment about where effort goes.

## D-029 — Official .gov.vn wording may be added to the lexicon; it adds spellings, not signs
**Date** 2026-08-08 · **Decider** PROJECT OWNER (ChuongBD), ruled 2026-08-08.
**Problem** D-027 froze the lexicon: "no new phrase may be added under this ruling". Probing the engine
with wording quoted verbatim from state health pages then showed it missing the easiest possible case —
a reader repeating the authorities' own words. 10 of 23 official phrasings were not recognised.
**Decision** Phrases quoted from a `.gov.vn` source may be added **when they are another spelling of a
danger sign the catalogue already carries**. Added: `tím môi` (the catalogue held only `môi tím` — a word
order away), `bú kém`, `xanh tái`, `ngừng thở`, `cánh mũi phập phồng`, `thở rên`, `hôn mê`, and
`giải thoát cho cả mẹ và con`. Each cites its source in a comment.
**Boundary** This does not permit a phrase that introduces a sign the rules do not already act on. Adding
one of those is a rule decision (see D-030), not a spelling decision, and quoting a source does not make
it reviewed: choosing that `cánh mũi phập phồng` means SEVERE_BREATHING_DIFFICULTY is a mapping judgement
the publisher never made.
**Impact** V1 and, through the shared `app/danger_phrases.py`, V2's floor. 20 of 23 official phrasings now
land correctly; the remaining three are D-030's scope.
**Sources** benhviennhitrunguong.gov.vn (paediatric danger signs); bachmai.gov.vn (postpartum depression).

## D-030 — PREG_RED_002 is ported to V1; three other gaps are refused
**Date** 2026-08-08 · **Decider** PROJECT OWNER (ChuongBD), ruled 2026-08-08.
**Problem** Four danger presentations published by state health services reached no V1 rule: severe
headache with visual disturbance, dizziness/faintness, shock signs after blood loss, and foul-smelling
lochia. The owner approved activating them.
**Decision** Only the first is activated. `PREG_RED_002` already exists in the registry as
`SEVERE_HEADACHE AND VISUAL_DISTURBANCE -> RED`, PREGNANCY only, so porting it moves a stated rule rather
than authoring one. V1 gains `RED_PREGNANCY_NEURO_DANGER` with the registry's conjunction and stage scope
intact. `test_unsigned_pregnancy_specific_signs_remain_inactive` is updated, not deleted: it still pins
the two signs that remain unruled.
**Refused, with reasons** Dizziness and foul-smelling lochia exist in the registry's signal vocabulary but
**no rule uses them**; shock signs have neither signal nor rule. Activating any of the three means picking
a threshold and an outcome that no artifact states — inventing clinical logic, which D-025 blocks and
which no `.gov.vn` citation substitutes for. They stay open pending a rule/source owner.
**Why the conjunction matters** Headache and "hoa mắt" are each ordinary in pregnancy. Firing on either
alone would turn a common complaint into an emergency; the registry's AND is what makes this safe to port,
and tests pin both halves, the stage scope and negation.
**Impact** V1 PREGNANCY only. First rule activation in V1 since the paediatric set — treat any further one
as needing the same standard: an existing registry rule to port, or a clinical decision.

## D-031 — D-029 extends to top-tier public hospitals; four visual phrases are refused
**Date** 2026-08-11 · **Decider** PROJECT OWNER (ChuongBD), ruled 2026-08-11.
**Problem** A source review was commissioned for the wording used by Vietnamese health authorities for
haemorrhage and pre-eclampsia danger signs. Sixteen candidate phrases came back. Ten of them cite
`tudu.com.vn` (Bệnh viện Từ Dũ), which is not a `.gov.vn` domain, so D-029 as written did not admit them.
Only two rows were true `.gov.vn`, and both were case-report news items rather than guidance.
**Decision, part "../../../docs/Detailed_Design/MF06_AI_Nurse_Assistant_Risk_Triage"1** D-029's source test is widened from "a `.gov.vn` source" to "a `.gov.vn` source, or a
top-tier public hospital's own health-education pages". Bệnh viện Từ Dũ qualifies as the leading public
obstetric hospital in the south. Everything else in D-029 is unchanged: a quoted phrase may only add
another spelling of a sign the catalogue already acts on, each phrase cites its source in a comment, and
quoting a source still does not make the mapping reviewed.
**Still excluded** Commercial and private-hospital content — Vinmec, Hello Bacsi, Medlatec, Long Châu —
along with news portals, forums and AI-generated pages. A case-report news article on a `.gov.vn` domain
describes one patient; it is not wording guidance and does not qualify either.
**Decision, part "../../../docs/Detailed_Design/MF06_AI_Nurse_Assistant_Risk_Triage"2** The four visual-disturbance phrases are refused: `ruồi bay`, `đèn nhấp nháy`,
`chói sáng`, `thấy các đốm sáng trước mắt`. Measured against the existing matcher, the first three fired on
six of six ordinary sentences — "nhà em nhiều ruồi bay quá", "bóng đèn nhấp nháy hỏng rồi, phòng bé tối
quá", "trời nắng chói sáng". `VISUAL_DISTURBANCE` is the second half of `PREG_RED_002`, so a pregnant user
mentioning a headache and a broken light would have been shown an eclampsia emergency. The fourth does not
even match the natural phrasing "nhìn thấy đốm sáng trước mắt" and adds no recall.
**Boundary** This refusal is about ambiguity, not about the clinical claim. Any of the four may return with
a context constraint — for instance, requiring "mắt" or "nhìn" in the same clause — but that is a matcher
design change with its own tests, not a lexicon addition under D-029.
**Refused as redundant** `băng huyết sau sinh`, `chảy máu ồ ạt` and `chảy máu ồ ạt trong ổ bụng` are already
matched by the existing `băng huyết` and `máu ồ ạt`. Adding them changes no behaviour.
**Admitted** `đau đầu kéo dài` and its mirror `nhức đầu kéo dài`, quoted from Từ Dũ's pre-eclampsia page.
Neither fires on "đau đầu nhẹ" or "đau đầu chút rồi hết", and neither can reach RED alone: `PREG_RED_002`
requires `VISUAL_DISTURBANCE` as well.
**Impact** `app/danger_phrases.py`, shared by V1 scoring and the V2 deterministic floor.

## D-032 — The V2 floor is wired to the three maternal phrase groups V1 already scores
**Date** 2026-08-11 · **Decider** PROJECT OWNER (ChuongBD), ruled 2026-08-11.
**Problem** `app/danger_phrases.py` declares eight phrase groups. `app/triage_v2/deterministic_signals.py`
maps five of them to signals. The three left unmapped — `MATERNAL_HEAVY_BLEEDING_PHRASES`,
`MATERNAL_SEVERE_HEADACHE_PHRASES`, `MATERNAL_VISUAL_DISTURBANCE_PHRASES` — are exactly the two maternal
RED rules. Measured on the 160-case vague corpus, V2 RED recall is 52.9%: with Gemini unavailable,
"Em đang bầu, máu ra ướt đẫm hai miếng băng" and "Em bầu bị đau đầu dữ dội kèm nhìn mờ" produce no signal
at all and reach NEEDS_MORE_INFO.
**Decision** The three groups are mapped into the V2 floor. This is a port, not an authoring: V1 already
scores RED from these exact groups at `app/risk_rules.py:228` and `:240-241`, and V1 is the engine
currently serving users. D-030 set the standard — "porting it moves a stated rule rather than authoring
one" — and this meets it on the same terms.
**Scope limit** Stage-scoped signals are emitted only when the stage is already resolved from trusted
context (`EXPLICIT_SELECTED_PROFILE` or `CONFIRMED_CONVERSATION_TARGET`). The original rationale for
leaving these groups out — that inferring them would mean guessing the stage — holds only while the stage
is unknown, and does not apply once a profile has stated it.
**Not included** No new sign, no new threshold, no new phrase beyond D-031's two. The seven signals with
no question that resolves them stay refused under D-030.
**Impact** `app/triage_v2/deterministic_signals.py`. Expected to close most of the RED recall gap; the
remainder is free-text numerics ("Bé hai tháng đo được 38,2 độ"), which is parsing, not a clinical claim.
