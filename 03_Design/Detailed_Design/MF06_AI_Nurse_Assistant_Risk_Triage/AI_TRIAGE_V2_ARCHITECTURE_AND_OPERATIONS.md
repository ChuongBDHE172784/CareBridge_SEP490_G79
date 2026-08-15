# AI Reproductive Health Triage Assistant V2 — Architecture and Operations

Updated: 2026-08-06 · Status: FUNCTIONALLY IMPLEMENTED LOCALLY, PUBLIC RELEASE BLOCKED

## 1. Governance and intended use

CareBridge is an `ACADEMIC_COMMUNITY_PROJECT` and a third-party informational risk-orientation tool.
It is not a hospital, clinic, diagnostic/prescription service, WHO product or clinically validated
medical device. Metadata remains `DEV_REVIEWED`, `NOT_CLINICALLY_VALIDATED`, external sign-off `NONE`.

CareBridge tham khảo và ánh xạ các dấu hiệu cảnh báo từ tài liệu y tế công khai của WHO, Bộ Y tế và
các nguồn y tế uy tín để xây dựng bộ quy tắc định hướng rủi ro riêng của CareBridge.

## 2. Architecture and authority boundaries

```mermaid
flowchart LR
  F["Flutter V2 internal UI"] --> J["Java auth/consent/session API"]
  J --> P["Python deterministic LangGraph"]
  P --> X["Gemini fact extraction only"]
  P --> R["Local verified BM25 evidence"]
  J --> D["triage_sessions JSONB"]
  J --> B["Independent Java safety fallback"]
```

- Java owns auth, consent, owner scoping, idempotency, state version, persistence, retention metadata,
  timeout/fallback, feature flags and ruleset-hash handshake.
- Python owns deterministic context/entity/intent/stage, signals, fixed question planning, canonical
  clinical rule evaluation, GREEN denial, rendering and post-outcome evidence retrieval.
- Gemini can return facts only; schemas cannot carry outcome/action/stop/URL/diagnosis/treatment.
- RAG runs after outcome and cannot mutate disposition.

## 3. State machine

Every new/resumed turn enters input validation and Global Safety. RED precedes target/intent/scope.
Then target→intent→stage→normalization/conflict→entity/stage validation→stage safety→dataset/scope→
canonical rule evaluator→question planner or terminal renderer→audit. Resume never jumps to planning.
Duplicate/stale messages are controlled and do not advance rounds. Maximum: 3 questions/round, 3 rounds.

## 4. Context, intent and entity

Targets: MOTHER, BABY, UNKNOWN, CONFLICTED. A state has one primary subject. “Mẹ và bé” requires a
choice and may open a second session. Explicit clarification has highest precedence. MOTHER and BABY
stages are validated; invalid cross-entity pairs are conflicts, never silently corrected. Unknown
complaints are not automatically out of scope; OOS requires positive taxonomy evidence.

## 5. Rules and safety

Rules load from canonical, hash-checked contracts and share parity vectors across Python/Java. Missing
is UNKNOWN, never ABSENT. Historical is never current. Independent Java fallback recognizes only the
small global danger set from trusted structured signals and otherwise returns NMI. No runtime failure
becomes GREEN/OOS. The fallback does not infer a global signal from arbitrary free text.

GREEN runtime status is `DISABLED`; eligibility is `BLOCKED_BY_SOURCE_COVERAGE`. The graph, Java
readiness/flags and Flutter parser are independent denial layers.

## 6. Question catalog

The canonical catalog carries question ID, target/stage/intent applicability, fields/signals resolved,
answer/options/measurement, priority, escalation signals and clarification/precondition flags. Hard
filters prevent mother questions for a baby and vice versa; `UNAWARE_OR_UNMEASURABLE` pivots rather
than repeating a measurement.

An unresolved target or intent permits clarification **and** the entity-agnostic global danger screen
(`Q_GLOBAL_DANGER`, `isGlobalDangerScreen`), asked in the same turn. The screen also survives a
coverage refusal, so a complaint the ruleset cannot stratify still gets screened for emergency signs
before the turn ends. Both properties are enforced in `app/questions/catalog_filter.py` and
`app/triage_v2/question_resume.py`, with Java parity in `rules/QuestionCatalogFilter.java`.

A question is eligible only when it resolves a field or signal the engine is actually missing. Rule
`requiredFields` and question `resolvesFields` must therefore share one vocabulary: a rule requiring
`visual_change` while the question declares only the signal `VISUAL_DISTURBANCE` makes that question
permanently ineligible. `tests/` holds the invariant that closes this class of defect.

## 7. API and persistence

Internal Java endpoints:

- `POST /api/internal/v2/triage/sessions`
- `POST /api/internal/v2/triage/sessions/{id}/messages`
- `GET /api/internal/v2/triage/sessions/{id}`
- `DELETE /api/internal/v2/triage/sessions/{id}?expectedStateVersion=N`

Python transport: `POST /internal/triage/v2/turn` with shared secret. Java reuses `triage_sessions`
JSONB and existing owner/request constraints/row locking; no new migration was needed. Persisted raw
health text is replaced with `[REDACTED_HEALTH_TEXT]`.

## 8. Sources and RAG

Only local `SOURCE_VERIFIED` documents with allowlisted HTTPS deep-link, section/page, organization,
target/stage/language/rule mapping and exact SHA-256 body hash qualify. Ranking is deterministic BM25.
The present inventory is 13 documents, 0 verified, so citations are empty. pgvector was rejected for
lack of measured benefit. V2 never runtime-browses the Internet.

## 9. Fallback and failure modes

- Trusted server-derived explicit global danger: Java fallback RED/action-first. Client-authored
  signals, measurements and journey context are rejected at the Java authority boundary.
- Python/registry/hash unavailable without explicit danger: controlled NMI/FALLBACK_ONLY.
- Gemini invalid/timeout: deterministic path continues/NMI.
- A deterministic phrase-to-signal floor now exists and runs before Gemini: `app/danger_phrases.py`
  supplies the phrase groups, `app/triage_v2/deterministic_signals.py` maps them to signals, and
  `app/triage_v2/api.py` merges them under (never over) caller-supplied observations, ahead of the
  global safety gate. Free-text danger detection without Gemini is therefore partially claimed.
  It remains a rollout blocker for a different reason than before: the phrase lists have **not**
  been reviewed by a clinician, and coverage is incomplete — see the gap below.
- Coverage gap, measured 2026-08-10: of the eight phrase groups in `danger_phrases.py`, five are
  mapped to signals. `MATERNAL_HEAVY_BLEEDING_PHRASES`, `MATERNAL_SEVERE_HEADACHE_PHRASES` and
  `MATERNAL_VISUAL_DISTURBANCE_PHRASES` are not. Those three feed the two maternal RED rules
  (`PREG_RED_001`/`POST_RED_001` haemorrhage, `PREG_RED_002` pre-eclampsia), so with Gemini
  unavailable a user describing either in her own words reaches NEEDS_MORE_INFO, not RED. The
  original rationale — stage-scoped findings would require guessing the stage — does not hold when
  the stage is already resolved from a trusted profile. Resolving this needs clinical review of the
  phrase lists, not only code.
- Corpus missing/rejected: disposition remains, citations empty.
- Stale version: Flutter exposes a typed 409 failure. Automatic GET-refresh/retry is not yet implemented
  and remains required before a full multi-client E2E claim.
- Shadow mismatch/failure: categorical telemetry only; V1 unaffected.

## 10. Privacy and security

Role/Principal and owner-scoped queries protect endpoints; active consent precedes creation. Payloads,
IDs and collections are bounded. Structured fields accept canonical codes only. Logs/metrics exclude
health text, prompts, identifiers, secrets and exception details. Retention is clamped and recorded.

Known blocker: `retentionUntil` is not an enforced purge. Completed snapshots are immutable and the
repository has no permanent account-deletion processor for them. Public rollout requires a reviewed
retention/legal policy, privileged purge design, migration and DB tests.

## 11. Evaluation

Two corpora, with different jobs.

**Engine regression corpus** (`tests/data/triage_v2_evaluation_cases.json`, 14 cases) passes 14/14:
global/stage RED recall 4/4, target accuracy 100%, wrong entity/question 0, OOS false positive 0,
unsupported GREEN 0. It exercises structured signals and guards against regression.

**Vague-input corpus** (`tests/data/triage_v2_vague_corpus_v1.json`, 160 synthetic cases,
baseline 2026-08-10) measures free-text Vietnamese as users actually write it — missing diacritics,
typos, negation, "I don't know", multi-turn. Headline results, all cases / excluding cases whose
failure was predicted against a known defect:

| Metric | All | Excluding known defects |
|---|---:|---:|
| Case pass rate | 16.9% | 21.4% |
| Target accuracy | 71.9% | 74.8% |
| Stage accuracy | 63.8% | 62.1% |
| First-turn question relevance | 83.2% | 72.8% |
| RED recall (17 RED-only cases) | 52.9% | 60.0% |
| Safety-question coverage | 100% | 100% |
| Wrong-entity / wrong-stage / forbidden question | 0% | 0% |
| Repeated question | 1.9% | 0% |
| Unsupported GREEN | 0 | 0 |

Read together: the **hard guards hold** — the engine never asks a wrong-entity, wrong-stage or
forbidden question, and always screens for danger. What it gets wrong is **understanding who and
where the user is**, and **reaching RED from free text**. Both are upstream of question ranking.

This is engineering evidence, not clinical validation: every case carries
`clinicalReviewStatus: PENDING`, all data is synthetic, and Gemini modes are local deterministic
fixtures rather than live-provider measurements. Citation precision remains unmeasurable with zero
verified sources.

## 12. Operations and observability

Default-off flags:

- Java `CAREBRIDGE_TRIAGE_V2_INTERNAL_ENABLED=false`
- Java `CAREBRIDGE_TRIAGE_V2_SHADOW_ENABLED=false`
- Flutter `AI_TRIAGE_V2_INTERNAL_ENABLED=false`

Monitor technical readiness, fallback/hash/schema/extraction/citation/target/state-conflict counts,
outcomes, latency, questions/max-round and shadow match/mismatch/error. Labels are closed enums. Before
any flag change: run full suites, registry sync, diff check, disposable DB/Flyway E2E and review the
machine-readable GREEN decision. Never change flags as an automatic CI consequence.

## 13. Rollback and deprecation

V1 remains public and intact. Disable both Java V2 flags and Flutter internal flag to roll back. Do not
delete V1 until the caller inventory exit gates, shadow window, DB E2E and rollback drill pass. Legacy
model-outcome/open-web paths are marked V1-only and forbidden to new V2 callers.

## 14. Demo flow

1. Build an internal Flutter variant with V2 flag only in a disposable environment.
2. Select Mother/Baby or leave Unknown; enter a non-identifying test phrase.
3. Demonstrate target clarification and a second separate session for the other person.
4. Demonstrate explicit global RED action before any sources.
5. Demonstrate OOS wrist and an unknown complaint staying NMI.
6. Stop before claiming verified citations or GREEN: current source coverage is zero and GREEN disabled.

## 15. Known limitations

- No clinical validation or external clinical sign-off.
- **RED recall from free text is 52.9%** on the vague corpus. Three unmapped phrase groups (§9) and
  free-text numerics are the known contributors: "Bé hai tháng đo được 38,2 độ" does not populate
  `temperatureC`, and a spelled-out age does not populate `babyAgeMonths`, so the young-infant fever
  rule cannot fire from a sentence.
- **Target resolution is wrong in ~28% of vague inputs.** `score_message` splits on commas and
  conjunctions and scores each clause independently, so a speaker's own pronoun in a separate clause
  counts as a second patient: "Em lo quá, bé bỏ bú" and "Bé nóng người nhưng nhà em không có nhiệt kế"
  both resolve CONFLICTED, while "Em thấy bé bú kém" (one clause) resolves BABY correctly.
- **Stage resolution is wrong in ~36% of vague inputs**, including spelled-out ages
  ("Con mười tháng" resolving to TODDLER_12_24M) and gestational statements in free text.
- Intent is recomputed from the latest message only, with no session stickiness, while target has
  `confirmed_conversation_target`. A vague follow-up therefore drops the clinical thread and returns
  to `Q_CLARIFY_INTENT`.
- `bleeding_amount` is absent from `dataset_requirements_v1.json` context fields, so a postpartum
  bleeding complaint never reaches `Q_BLEEDING_AMOUNT`.
- Seven signals have no question that resolves them — `CHEST_INDRAWING`, `CHEST_PAIN`,
  `COUGH_OR_RUNNY_NOSE`, `DIARRHOEA`, `PERSISTENT_VOMITING`, `RASH`, `VOMITING` — so the rules
  reading them are reachable only through Gemini extraction.
- `current_status` is required by three maternal rules but no V2 component produces it.
- `Q_BABY_TEMPERATURE` is not flagged `measurement` and has no `pivotTo`, so "no thermometer"
  yields `FEVER: UNKNOWN` with no pivot to observable signs.
- `GREEN_DEFAULT_001` lists `stage-specific minimum dataset` — prose in a machine field list.
- Planning and review artifacts live under `_bmad-output/`, which `.gitignore` excludes, so no
  planning, review or baseline artifact is version-controlled alongside the code it describes.
- Verified source corpus: 0; citation coverage/precision unavailable.
- GREEN rule/dataset/source coverage incomplete and public GREEN disabled.
- Full DB-backed E2E/Flyway/DB chaos not run because Docker is unavailable.
- Retention/account deletion enforcement unresolved.
- V1 still contains legacy runtime search and model-outcome interfaces; V2 is isolated from them.
- Whole-app Flutter analyze retains unrelated baseline warnings and one dirty stage-selector golden diff.
