# Merging the two triage engines into one

**Status** Draft for decision · **Written** 2026-08-08 · **Author** Claude (survey + measurement)

Goal: one engine, and the `V2` suffix gone from files, classes and endpoints. Versions belong
in git history and in artifact `rulesetVersion`, not in identifiers.

This document records what is actually there, measured, so the decision is made on facts.
It does not authorise any of the steps below.

---

## 1. Why the sprawl exists

Most of the machinery people find heavy is **the cost of running two engines side by side**,
not the cost of triage:

| Machinery | Exists only because there are two engines |
|---|---|
| `context_parity_vectors_v1.json` (×3 copies) + parity tests in both languages | yes |
| `TriageV2ShadowService`, `TriageV2ShadowMetrics` | yes |
| `isTriageV2CutoverCandidate` / `shouldHandOffToTriageV2` | yes |
| `TriageV2Readiness`, `TriageGreenBlocker`, green-release assessment | yes |
| `CareStage.mapLegacy`, 4 Python stage translations | yes |
| Two mobile screens (`symptom_intake_screen`, `triage_v2_screen`) | yes |

Collapsing to one engine deletes this category. **Merging is the anti-sprawl move**, not more
of it — which is the opposite of how it looks from the file count.

## 2. Measured surface

- **63 files** carry `triage_v2` / `TriageV2` / `_v2` in the filename.
- **585 occurrences** of the identifiers in `.py` / `.java` / `.dart`.
- Endpoint `/internal/triage/v2/turn`; config `carebridge.triage.v2.*`; env
  `CAREBRIDGE_TRIAGE_V2_INTERNAL_ENABLED`; error code `TRIAGE_V2_INTERNAL_ONLY`.
- **No `triage_v2_*` database table.** Entities are already neutrally named
  (`triage_sessions`, `red_flag_rules`, …). The rename needs no data migration.

## 3. Which engine is the base

**V2 absorbs V1.** Not negotiable on technical grounds:

- V1 decides RED by substring-matching free text. That is what produced both
  "ra máu rất nhiều" → not RED and "Tôi có giặt quần áo cho bé" → RED (fixed 2026-08-08, see
  `test_vietnamese_intake_corpus.py`). The fragility is in the approach, not the tuning.
- V2 consumes structured signals with explicit presence/negation/temporality, so that class of
  defect is absent by construction.
- V2 is no longer maternal-only: `PED_RED_001..006` + `PED_YELLOW_001` give it 13 paediatric
  rules (7 INFANT_0_12M, 6 TODDLER_12_24M). Any doc or comment saying otherwise is stale.

So the work is "V2 grows the missing pieces, V1 is deleted, the suffix drops" — not a blend.

## 4. The one real vocabulary conflict

Two stage vocabularies are live:

| Layer | Postpartum | Infant | Toddler |
|---|---|---|---|
| Context layer — Python `CareStage` **and** Java `CareStage`, parity vectors | `POSTPARTUM_MOTHER` | `INFANT_0_12M` | `TODDLER_12_24M` |
| Rule registry — `triage_rules_v2.json`, `V2_STAGES` in both languages | `POSTPARTUM` | `INFANT_0_12M` | `TODDLER_12_24M` |
| V1 / API / mobile | `POSTPARTUM` | `INFANT` | `TODDLER` |

The registry vocabulary is **internally inconsistent**: babies got the entity-explicit
treatment, the mother did not. That single mismatch is why these translations exist —
`clinical_rule_engine.py:22`, `coverage_resolver.py:118`, `dataset_scope_nodes.py:163`,
`stage_safety_gate.py:18`, and Java `CareStage.mapLegacy`.

> **Do not "simplify" `POSTPARTUM_MOTHER` to `POSTPARTUM`.** The suffix is a safety property,
> stated in `CareStage`'s javadoc: *"split by entity so a maternal threshold can never be
> applied to an infant"*, because a postpartum session is equally likely to be about the
> newborn. Collapsing the name removes the distinction that keeps maternal thresholds off a
> baby. The correct direction is the registry adopting `POSTPARTUM_MOTHER`.

**Step V-1 — DONE 2026-08-08.** The registry now stores `POSTPARTUM_MOTHER`, so the rule layer
and the context layer share one vocabulary and the translations are gone.

Changed: `triage_rules_v2.json`, `triage_rule_parity_vectors_v2.json`,
`dataset_requirements_v1.json` (`byStage` key), and the stage enums in
`triage_rule_condition.schema.json` / `triage_response_v2.schema.json` — propagated to all
copies and re-digested with `DevTools/sync_triage_rule_registry.py` (`--check` passes).
Code: `V2_STAGES` in both languages; 5 comparisons in `TriageRuleEvaluator`; the two Python
stage maps became membership sets (`_RULE_STAGES`, `_MATERNAL_STAGES`) since only the
restriction was left; the translations in `coverage_resolver` and `dataset_scope_nodes` deleted.

Three files keep a bare `POSTPARTUM` **on purpose**, and a future sweep must not "finish the
job" on them:

| File | Why it stays |
|---|---|
| `context_contract_v1.json` | it is the `legacyStageMapping` **key** |
| `context_parity_vectors_v1.json` | `legacyStage` is the test **input**; two vectors assert legacy POSTPARTUM + BABY must NOT auto-map |
| `matrix_snapshot_v0.1.0.json` | transcription of the approved source matrix — *"Never edit to make code pass"* |

Java `CareStage.mapLegacy` also keeps its `POSTPARTUM` arm: it maps the **legacy wire**
vocabulary that V1, the API and the mobile app still send. It retires with V1 in step 5, not
here. (An earlier draft of this plan wrongly listed it for deletion in V-1.)

Two stale artifacts surfaced while doing this and were corrected: the stage enums in both
JSON schemas listed only the four maternal stages, so they did not admit the 13 paediatric
rules the loader has been accepting since the paediatric port. Neither schema is enforced
anywhere — `triage_response_v2.schema.json` has no reader at all — which is why the drift went
unseen. They are documentation; treat them as such or wire them into validation.

## 5. What V2 still lacks before it can take production traffic

| Gap | Notes |
|---|---|
| A chat experience | `triage_v2_screen` is an internal harness. `symptom_intake_screen` is the real chat and talks to V1. |
| ~~Deterministic path without Gemini~~ | **Answered 2026-08-08 — it works.** See below. |
| Deterministic self-harm detection | **The one real hole found.** See below. |
| Free-text entry parity | V1 accepts a sentence and asks follow-ups; V2's entry contract is signals + answered question ids. |
| Clinical sign-off | Unchanged and unsolved — see §6. |

### Measured: V2 with Gemini unavailable, turn 1, no signals, journey supplied

Five danger sentences were put through `execute_turn` with `GEMINI_ENABLED=false` and an empty
`signals` map — the exact shape of a turn where extraction produced nothing.

| Message (stage) | Outcome |
|---|---|
| "Tôi ra máu rất nhiều sau sinh" (POSTPARTUM_MOTHER) | `NEEDS_MORE_INFO`, asks `Q_GLOBAL_DANGER` + `Q_SAFETY_SELF_HARM`, conversation continues |
| "Tôi bị co giật" (PREGNANCY) | `NEEDS_MORE_INFO`, asks `Q_GLOBAL_DANGER` |
| "Tôi khó thở dữ dội" (PREGNANCY) | `NEEDS_MORE_INFO`, asks `Q_GLOBAL_DANGER` |
| "Bé nhà em li bì khó đánh thức" (INFANT_0_12M) | `NEEDS_MORE_INFO`, asks `Q_GLOBAL_DANGER`, `Q_BABY_TEMPERATURE`, `Q_BABY_FEEDING` |
| **"Tôi không muốn sống nữa"** (POSTPARTUM_MOTHER) | **only `Q_CLARIFY_INTENT`** |

No reassuring outcome in any case — the fail-closed property holds, and V2 degrades to asking
the explicit danger screen rather than guessing. That removes the largest unknown from the
cutover.

Note the journey context matters: with no `journeyContext` the stage is `UNKNOWN`, no rule
targets it, and every one of these stops with `RULESET_COVERAGE_LIMITATION` /
`ROUTE_TO_HEALTHCARE_WORKER`. Safe, but useless. Java must always supply the journey.

### The hole: self-harm has no deterministic path

The last row is the exception, and it is the worst place to have one. V1 flags
"Tôi không muốn sống nữa" as RED self-harm deterministically, from the phrase list. V2 reaches
its self-harm path only through extraction, so with Gemini down a person disclosing suicidal
ideation is asked to clarify what they meant — a clinical step backwards, occurring exactly
when the system is already degraded.

**Step V-2 — DONE 2026-08-08, but see D-027 before relying on it.** It infers global signals
from free text with an unreviewed phrase lexicon, which D-025 and D-026 decided to block until
a source/rule-reviewed contract exists. Nothing here establishes that free-text end-to-end
danger detection works — D-025 exists precisely to stop that claim being made — and public
rollout stays blocked either way. The ruling is pending.

V2 has a deterministic danger floor, and this was the first real merge: the phrase matcher
moved out of `risk_rules` into `app/danger_phrases.py`, which both engines now import. One implementation, so they cannot drift on the question that matters
most. `risk_rules.py` went from 446 to 242 lines and its behaviour is unchanged.

`app/triage_v2/deterministic_signals.py` turns a message into global danger signals and
`execute_turn` merges them *under* the caller's signals before the safety pre-check, so the
gate sees them whether or not Gemini is reachable. The gate still decides; the floor only
supplies evidence. V2's ban on importing `app.risk_rules` is intact — the shared module
depends on neither engine.

Same five sentences, Gemini still unavailable:

| Message | Before V-2 | After V-2 |
|---|---|---|
| "Tôi không muốn sống nữa" | only `Q_CLARIFY_INTENT` | **RED · IMMEDIATE_SAFETY_SUPPORT** |
| "Tôi bị co giật" | asks `Q_GLOBAL_DANGER` | **RED · IMMEDIATE_EMERGENCY_ASSESSMENT** |
| "Tôi khó thở dữ dội" | asks `Q_GLOBAL_DANGER` | **RED · IMMEDIATE_EMERGENCY_ASSESSMENT** |
| "Bé nhà em li bì khó đánh thức" | asks `Q_GLOBAL_DANGER` | **RED · IMMEDIATE_EMERGENCY_ASSESSMENT** |
| "Tôi ra máu rất nhiều sau sinh" | asks `Q_GLOBAL_DANGER` | unchanged — bleeding is stage-scoped, deliberately out of the floor's scope |

Free text can now raise RED in V2, so the false-positive side is pinned as hard as the
true-positive side: all 17 ordinary sentences in the corpus produce no danger signal and no
RED (`tests/test_triage_v2_deterministic_signals.py`, 48 cases). A question answer also
outranks a phrase match — an explicit ABSENT from `Q_GLOBAL_DANGER` is not overwritten by the
floor, only silence is filled.

## 6. The blocker that is not engineering

`internal_rule_review_manifest.json` states, about the rule set both engines share:

- `clinicalValidationStatus: NOT_CLINICALLY_VALIDATED`
- `externalClinicalSignOff: NONE`
- *"No clinician participated in this project"*
- *"Thresholds for HEAVY bleeding, LARGE_CLOTS, SEVERE_HEADACHE and SEVERE_BREATHING_DIFFICULTY
  are internal working definitions, not validated clinical criteria."*

No amount of merging changes this. It bounds what the feature may honestly claim
(`INFORMATIONAL_RISK_ORIENTATION`), and it is why `PREG_RED_002` (pre-eclampsia) and
`LARGE_CLOTS` remain deliberately inactive in V1 — pinned by
`test_unsigned_pregnancy_specific_signs_remain_inactive`. Activating them is a governance
decision, not a code change.

## 7. Suggested order

1. **V-1** — unify the stage vocabulary (§4). Prerequisite for everything else.
2. **Decide the cutover unit.** One stage, in production, end to end. `PREGNANCY` is the
   natural first: most rules, and the mobile gate already treats maternal as the candidate set.
3. **Give V2 the chat entry** — point `symptom_intake_screen` at V2 for that one stage, keeping
   V1 for the rest. This is where parity vectors earn their keep; keep them until step 5.
4. **Widen stage by stage**, deleting the V1 branch for each stage as it lands.
5. **Delete V1 and the coexistence machinery** (§1 table). Only now is the parity apparatus
   dead weight rather than the safety net.
6. **Drop the `V2` suffix** — 63 files, 585 identifiers, endpoint, config, env, error code.
   Pure rename, no data migration (§2). Last, because until step 5 the suffix is the only thing
   distinguishing two live engines.

Doing step 6 first produces two things named `triage` in one codebase, and step 5 would rename
them again. That is the one ordering mistake worth calling out explicitly.

## 8. Corrections this survey produced

- `triage_v2_cutover_test.dart` claimed *"V2 declares no paediatric rule at all"*. False since
  the paediatric port; comment corrected 2026-08-08.
- An earlier reading of this plan's author treated `POSTPARTUM_MOTHER` as redundant naming and
  proposed collapsing it. That would have removed a safety property — recorded here so the
  same mistake is not repeated.
