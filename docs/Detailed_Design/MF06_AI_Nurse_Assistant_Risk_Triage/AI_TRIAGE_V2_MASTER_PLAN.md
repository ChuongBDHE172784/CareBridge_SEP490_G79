# AI Triage V2 — Master Plan

Stable reference for every Claude/Codex work package. Update only on a new architecture
decision (record it in `AI_TRIAGE_V2_DECISIONS.md`). Per-task state lives in
`AI_TRIAGE_V2_PROGRESS.md`; the current atomic task lives in `AI_TRIAGE_V2_CODEX_TASK.md`.

---

## 1. Product positioning (non-negotiable)

CareBridge is an **ACADEMIC_COMMUNITY_PROJECT**, a **THIRD_PARTY_INFORMATION_SUPPORT_TOOL**
providing **INFORMATIONAL_RISK_ORIENTATION_ONLY** from a source-backed, rule-based engine.

It is **not** a hospital, clinic, medical facility, diagnostic service, prescribing service,
an official WHO triage tool, WHO-certified, doctor-approved, or clinically validated.
**No clinician participated in building or approving these rules.**

Canonical metadata, to be used verbatim:

```
projectType             = ACADEMIC_COMMUNITY_PROJECT
intendedUse             = INFORMATIONAL_RISK_ORIENTATION
internalReviewStatus    = DEV_REVIEWED
clinicalValidationStatus= NOT_CLINICALLY_VALIDATED
externalClinicalSignOff = NONE
```

Forbidden anywhere in code, artifacts, docs or UI: `DOCTOR_APPROVED`, `CLINICALLY_APPROVED`,
`SIGNED_CLINICAL_APPROVAL`, `WHO_APPROVED`, `WHO_CERTIFIED`, `PO_CONFIRMED_DOCTOR_REVIEW`,
"an toàn tuyệt đối", "không có ca nguy hiểm nào lọt lưới", "không chịu bất kỳ trách nhiệm nào".

WHO must be described only as: *"CareBridge tham khảo và ánh xạ các dấu hiệu cảnh báo từ tài
liệu y tế công khai của WHO, Bộ Y tế và các nguồn y tế uy tín để xây dựng bộ quy tắc định
hướng rủi ro riêng của CareBridge."* RED/YELLOW/GREEN are CareBridge categories, never WHO output.

---

## 2. Safety invariants (22) — any violation fails its gate

1. LLM never decides the outcome colour.
2. LLM never decides a URL.
3. RAG never changes the outcome.
4. A system error never becomes GREEN.
5. Missing data never becomes ABSENT.
6. A historical signal never becomes a current signal.
7. A caller boolean is never a source of truth.
8. The Global Safety Gate re-runs after every answer.
9. RED outranks scope and intent.
10. Target UNKNOWN → no symptom-specific question.
11. Target CONFLICTED → never silently resolved.
12. The Question Planner never asks a wrong-entity question.
13. OUT_OF_SCOPE requires positive evidence.
14. GREEN requires its own release gate.
15. A PENDING source is never shown as a verified citation.
16. A registry failure never kills unrelated modules.
17. The Java fallback depends on no registry, Python, Gemini or RAG.
18. No diagnosis.
19. No medication advice.
20. No WHO endorsement claim.
21. The audit trace never loses a suppressed rule.
22. V2 never depends on the legacy `GeminiTriageClient` outcome interface.

---

## 3. Domain model

**TargetEntity**: `MOTHER | BABY | UNKNOWN | CONFLICTED` — never `null`.

**CareStage**: `PRECONCEPTION | POSSIBLE_PREGNANCY | PREGNANCY | POSTPARTUM_MOTHER |
INFANT_0_12M | TODDLER_12_24M | UNKNOWN | CONFLICTED`.
Legacy `POSTPARTUM` → `POSTPARTUM_MOTHER` only when `targetEntity=MOTHER`; never auto-mapped to BABY.
Valid pairs: MOTHER → the four maternal stages; BABY → the two paediatric stages. Anything
else is `CONFLICTED` and needs clarification.

**IntentType**: `SYMPTOM_TRIAGE | GENERAL_HEALTH_INFORMATION | SOURCE_LOOKUP |
FOLLOW_UP_ANSWER | EMERGENCY_HELP | OUT_OF_SCOPE_REQUEST | UNKNOWN | CONFLICTED`.

**ContextResolutionStatus**: `RESOLVED | NEEDS_TARGET_ENTITY | NEEDS_STAGE | NEEDS_INTENT |
CONFLICTED | INSUFFICIENT_CONTEXT`.

**Target resolution precedence**: explicit clarification answer → explicit subject in the
latest message → explicit selected profile → confirmed conversation target → strong
stage-specific context → extractor inference above threshold → UNKNOWN.
A profile context never silently overrides explicit current input.
One triage session has exactly one primary target entity.

**Entity-agnostic danger signals** (severe breathing difficulty, seizure, altered
consciousness, cyanosis, current self-harm intent, cannot ensure immediate safety) are
handled by the Global Safety Gate **before** any clarification. Entity-specific thresholds
are never borrowed across entities.

---

## 4. Question catalogue contract

Every question carries: `questionId`, `targetEntities`, `applicableStages`,
`applicableIntents`, `resolvesFields`, `answerType`, `optionCodes`, `measurement`,
`priority`, `escalationSignals`, `requiresResolvedTarget`, `requiresResolvedStage`,
`isTargetClarification`, `isStageClarification`, `isIntentClarification`.

Eligibility = target matches **and** stage matches **and** intent matches **and** it resolves
at least one `missingRequiredField` **and** preconditions hold **and** it is unanswered
**and** limits are not exceeded. Max 3 questions per round, max 3 rounds.

Business logic keys off `optionCode`/`questionId` only — never display text.
A measurement answered `UNAWARE_OR_UNMEASURABLE` is never re-asked; the planner pivots to
perceivable symptoms.

`targetEntity=UNKNOWN` → only `Q_CLARIFY_TARGET_ENTITY`
(`CLARIFY_TARGET_MOTHER` / `CLARIFY_TARGET_BABY` / `CLARIFY_TARGET_BOTH`).
`CLARIFY_TARGET_BOTH` → ask which to assess first; a second session may follow.
`targetEntity=CONFLICTED` → conflict-resolution question only; never GREEN, never OUT_OF_SCOPE.

---

## 5. OUT_OF_SCOPE

Requires positive evidence, all of: global gate scanned the current input and found no
explicit danger signal; a deterministic taxonomy classified the complaint as outside
obstetric/paediatric scope; no maternal/paediatric reproductive signal; no unresolved
possible pregnancy; no target/context conflict; no pending RED/YELLOW.
An unrecognised complaint is `NEEDS_MORE_INFO`, never OUT_OF_SCOPE.

---

## 6. Phase plan and gates

| Phase | Scope | Gate summary |
|---|---|---|
| **0** | Pre-LangGraph gate closure: `releaseStatus` migration, legacy loader compat, targeted tests (dataset/scope/pending/exclusion/source-derivation), Gemini V2 dependency architecture test, readiness paths, fallback drift protection, parity exactness, governance doc cleanup | No `APPROVED` in canonical; 41/41 vectors both runtimes; exact parity fields; Python full PASS; no new Java regression; GREEN disabled; release blocked; source verification honest; legacy Gemini isolated |
| **1** | Contract: TargetEntity / IntentType / CareStage / ContextResolutionStatus, resolvers, question metadata + hard filter, target clarification, OOS taxonomy, context parity vectors | Shared contract; Java/Python parity; no wrong-entity question; UNKNOWN/CONFLICTED → clarification only; OOS stops correctly; global safety still wins |
| **2** | Deterministic LangGraph 3A (17 nodes, typed state, repeated safety gate, interrupt/resume, idempotency, stale-version rejection) | Node/routing/resume/repeated-RED tests; Python full PASS; no parity regression |
| **3** | Java API + contract + fallback + persistence; Flyway only if audit proves need | Contract, integration, timeout/fallback, idempotency, stale version, hash mismatch, migration tests |
| **4** | Gemini structured extraction (no outcome fields), deterministic post-validator, injection defence | Schema tests, no-outcome architecture test, accuracy thresholds, fail-closed |
| **5** | Source verification + filtered RAG citations, allowlist, no runtime browsing | Retrieval precision, allowlist, no pending-source exposure, no outcome change |
| **6** | Flutter flow, entity selection, five outcomes, citations | Widget/DTO/error-state/entity-filter tests |
| **7** | Evaluation, security, privacy, observability | 20 evaluation categories, adversarial suite, redaction |
| **8** | Internal shadow mode + E2E + chaos | E2E PASS, no RED downgrade, no failure-to-GREEN, rollback tested |
| **9** | GREEN release gate (20 conditions) | GREEN stays DISABLED unless every condition holds |
| **10** | Cutover, V1 retirement, documentation | Flagged rollout, rollback, docs without forbidden claims |

A phase starts only after the previous gate is PASS. On FAIL: stop, record, do not advance.

---

## 7. Definition of done (functionally complete)

The 30 conditions in the source master prompt apply in full. Highlights: no failure-to-GREEN
path, no unknown-to-ABSENT path, no wrong-entity question in the test corpus, no unverified
citation path, Java/Python parity PASS, E2E and shadow mode PASS, rollback tested,
`NOT_CLINICALLY_VALIDATED` retained, no WHO endorsement, Supabase changes only via approved
Flyway, no push unless requested.

**PUBLIC RELEASE READY is a separate, stricter gate than FUNCTIONALLY COMPLETE.**

---

## 8. Codex delegation protocol

Claude orchestrates, decides architecture, reviews every diff, runs tests, and owns
PASS/FAIL. Codex implements one atomic task: 1 objective, ~1–5 production files plus their
direct tests. Codex never pushes, resets, widens scope, edits files outside allowed paths,
starts the next task, or draws safety/production-readiness conclusions.

Codex receives only: the path to `AI_TRIAGE_V2_CODEX_TASK.md`, the files to read, the task
ID, acceptance criteria, test commands, allowed and forbidden paths. Never the chat history.

Codex output format: summary (≤10 lines), files changed, unified diff (never whole files),
test summary (command, exit code, counts, ≤50 lines of root cause), blockers, assumptions.

After every task Claude runs `git diff --check`, reads the diff itself, checks for files
outside allowed paths, checks whether expected test values were edited to force a pass,
checks for fail-open logic, verifies tests independently, then updates progress and decisions.
