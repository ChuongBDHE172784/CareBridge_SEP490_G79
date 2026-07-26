# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# TriageSymptomSynonymExpansion — Vietnamese Folk-Term Synonym Dictionary Expansion

| Field | Value |
|-------|-------|
| **Document ID** | `CB-TRIAGE-IMP-005` |
| **Version** | `1.0` |
| **Date** | `2026-07-26` |
| **Status** | `Approved — Implemented 2026-07-27` |
| **Document Owner** | `AI Agent` |
| **Author** | `AI Agent` |
| **Reviewed by** | `[x] Project owner approval 2026-07-26 ("Approved hết") — NOT an independent clinical reviewer; follow-up clinical review remains advisable` |
| **DPO Sign-off** | `[ ] Not required — no new PII field, no schema change (dictionary constants only). Symptom text handling itself is unchanged.` |
| **Approved by** | `[x] Project owner, 2026-07-26 ("Approved hết")` |
| **Last Review** | `2026-07-26` |
| **Based on EDS** | `v2.0` |

> ⚠️ **HEALTHCARE SAFETY NOTICE:** Every synonym→canonical mapping in this document that is marked
> `Proposed — pending clinical review` is an AI-proposed medical mapping. It MUST be reviewed and
> confirmed by a human with clinical competence before this document may be set to `Approved`.
> The AI author does not assert any medical claim; severity decisions are never invented here (BR-SAFETY).

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Never delete old information. Every change goes in this table.

| Date | Person | Change |
|------|--------|--------|
| 2026-07-26 | AI Agent | Initial creation — TDS for synonym dictionary expansion (roadmap Part III item 5) |
| 2026-07-27 | AI Agent — Amelia (Dev Agent) | Phase 3: Implementation — 9/9 tests PASS (Java SymptomNormalizerTest 7/7; Python test_symptom_synonym_expansion 58/58 incl. parametrized sub-cases). Red Gate evidence: `red-gate-evidence.log`. Documented deviation to §5.3 column 3: `đ` (U+0111) has no NFD canonical decomposition, so the engines strip "lừ đừ"→`lu đu` and "đi ngoài"→`đi ngoai`; both đ-variants stored ALONGSIDE the approved plain-d forms `lu du`/`di ngoai` in both dictionaries (same folk terms S5/S11, same canonicals — required for diacritic input matching, TSSE-TC-02/06). Clinical-review gate satisfied at project-owner level (2026-07-26), not by an independent clinician. S1 remains DEFERRED. |

---

## TABLE OF CONTENTS

1. [Module Overview](#1-module-overview)
2. [Traceability Matrix](#2-traceability-matrix)
3. [Architecture Decision Records (ADR)](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling](#5-static-modeling)
6. [Dynamic Modeling](#6-dynamic-modeling)
7. [Domain Event Catalog](#7-domain-event-catalog)
8. [Interface Specification](#8-interface-specification)
9. [API Specification](#9-api-specification)
10. [Error Codes](#10-error-codes)
11. [Implementation Plan (Step-by-Step)](#11-implementation-plan-step-by-step)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Detailed Test Scenarios](#13-detailed-test-scenarios)
14. [Verification Method](#14-verification-method)
15. [API Verification Samples](#15-api-verification-samples)
16. [Authorization Matrix](#16-authorization-matrix)
17. [AI Prompt Constraints (CASE 2.0)](#17-ai-prompt-constraints-case-20)

---

## 1. Module Overview

> Expand the Vietnamese folk/colloquial synonym dictionaries used by the deterministic symptom
> normalizers so that common parent phrasings ("trớ sữa", "khò khè", "biếng ăn", …) map to the
> existing canonical symptom codes. This is a **data-only** change to two constant dictionaries —
> one in Java, one in Python — which MUST stay in parity. No schema, API, or engine-logic change.

| Field | Value |
|-------|-------|
| **Module Name** | `Triage Symptom Synonym Expansion` |
| **Bounded Context** | `triage` |
| **Data Classification** | `Internal` *(the dictionary itself contains no PII; the surrounding intake flow remains Sensitive-PII and is untouched)* |
| **Compliance Scope** | `BR-SAFETY (AI guidance only — no diagnosis, no invented severity)` |
| **Upstream Dependencies** | `UC60 Run AI Symptom Intake (TriageService / Python intake flow)` |
| **Downstream Consumers** | `PediatricRiskRules / risk rule engines (consume canonical codes only — unchanged)` |

### Component Responsibilities (exact files)

| Component | Path | Responsibility in this feature |
|-----------|------|-------------------------------|
| Java normalizer | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/engine/SymptomNormalizer.java` | Holds `KEYWORDS` map (17 canonical codes, accent-stripped `contains` matching via `\p{M}+` strip; numeric rules ≥37.5°C→`fever`, ≥39.0°C→`high_fever`). New synonyms are added to `KEYWORDS` values only. |
| Python normalizer | `05_Development/CareBridgeAITriageService/app/symptom_normalizer.py` | Holds `ONTOLOGY` dict (19 codes incl. `high_fever`, `worsening_condition`; `strip_accents` + word-boundary regex + negation guard). New synonyms are added to `ONTOLOGY` value tuples only. |
| Java unit test (NEW) | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/SymptomNormalizerTest.java` | New dedicated test class (does not exist today; normalizer currently only exercised indirectly through `TriageServiceTest.java`). |
| Python unit test (NEW) | `05_Development/CareBridgeAITriageService/tests/test_symptom_synonym_expansion.py` | New test module (existing deterministic coverage lives in `tests/test_triage.py`). |
| Parity fixture (NEW) | `05_Development/CareBridgeAPI/src/test/resources/triage/symptom_synonym_parity_vectors.json` **and** `05_Development/CareBridgeAITriageService/tests/data/symptom_synonym_parity_vectors.json` | Shared identical JSON vectors, following the established `pediatric_red_parity_vectors.json` pattern (`PediatricRedParityTest.java` / `tests/test_pediatric_red_parity.py`). |

### ⚠️ Invariant — Java ↔ Python Parity (BR-TRIAGE-PARITY)

> **INVARIANT:** For every synonym added by this feature, the Java `KEYWORDS` map and the Python
> `ONTOLOGY` dict MUST produce the same canonical symptom code for the same input phrase.
> A synonym added to one side and not the other is a defect. Parity is enforced by the shared
> vector file above (TSSE-TC-08 in the Test-Spec). Known pre-existing divergences (see §2 note
> and Test-Spec §2 Logic Issues) are recorded as Open items, NOT silently "fixed" by this feature.

---

## 2. Traceability Matrix

| Requirement ID | Type (BR/ADR/US) | Requirement | Code Component | Compliance Target | Related ADR |
|----------------|------------------|-------------|----------------|-------------------|-------------|
| RM-III-5 | Roadmap item | Expand folk-term synonym dictionary in BOTH Java and Python normalizers, with tests (`04_Implement/AITriageCompletion/AITriage_Assessment_Roadmap.md` Part III §5) | `SymptomNormalizer.KEYWORDS`, `symptom_normalizer.ONTOLOGY` | BR-SAFETY | ADR-TSSE-001 |
| BR-TRIAGE-PARITY | Business Rule | Java and Python dictionaries stay in parity for all added synonyms | `symptom_synonym_parity_vectors.json` (both copies) | BR-SAFETY | ADR-TSSE-001 |
| BR-SAFETY | Business Rule | No AI-invented severity; new canonical codes require clinical review before any risk-rule wiring | Deferral of `bulging_fontanelle` (see §3, Open) | BR-SAFETY | ADR-TSSE-001 |
| BR-TSSE-NOREG | Business Rule | Zero regression on the 17 existing Java canonical codes and numeric rules | `SymptomNormalizerTest` regression cases | BR-SAFETY | — |

> **Note:** Pre-existing parity gaps discovered during analysis (Java `fever` includes `"nong"` but
> Python does not; Python has `high_fever`/`worsening_condition` keyword entries with no Java
> keyword counterpart; Java substring `contains` vs Python word-boundary matching) are **out of
> scope** and recorded as Open items in §11.4 — this feature must not widen them for new terms.

---

## 3. Architecture Decision Records (ADR)

### ADR-TSSE-001 — Dictionary-only expansion; defer any NEW canonical code pending clinical review

| Field | Value |
|-------|-------|
| **Status** | `Accepted — project-owner approval 2026-07-26 (no independent clinical reviewer; see §11.1)` |
| **Deciders** | `Project owner (Tech Lead-level approval); independent Clinical Reviewer still pending for the S1 follow-up` |
| **Date** | `2026-07-26` |
| **Supersedes** | `—` |

#### Context
The roadmap lists three candidate folk terms. Two map to existing canonical codes, but
"thóp phập phồng" (bulging fontanelle) has **no** canonical code among the current set and is a
recognized pediatric danger sign in clinical literature. Adding a new canonical code without a
risk-rule severity mapping would make the symptom normalize but never escalate — a silent
false-reassurance risk. Inventing a severity mapping ourselves violates BR-SAFETY.

#### Options Considered

| Option | Description | Pros | Cons |
|--------|-------------|------|------|
| A | Add synonyms to existing canonicals only; **defer** `bulging_fontanelle` until a clinician approves both the canonical code AND its severity mapping in `PediatricRiskRules.java` / `PediatricInfantRiskRules.java` and `app/risk_rules.py` | + No invented medical severity; + smallest scoped change; + no risk-rule blast radius | - Roadmap term "thóp phập phồng" not yet recognized |
| B | Add `bulging_fontanelle` code now, wire severity later | + Term recognized immediately | - Normalizes without escalating → false reassurance; violates BR-SAFETY |

#### Decision
**Option A.** This feature adds synonyms for EXISTING canonical codes only. `bulging_fontanelle`
is recorded as an **Open** follow-up requiring clinical review of both the code and its severity
(likely RED for infants per IMCI-style guidance — but that judgment is explicitly NOT made here).

#### Consequences
**Positive:** zero risk-rule impact; rollback is a plain `git revert`.
**Negative / Trade-offs:** "thóp phập phồng" remains unrecognized until the clinical-review follow-up; in the Python service such free text still falls through to the Gemini normalizer (bounded by `CANONICAL_SYMPTOM_CODES`), and in Java it is simply not matched.
**Compliance Impact:** upholds BR-SAFETY (no AI-invented diagnosis/severity).

---

## 4. Non-Functional Requirements & SLA

**Not applicable (with one note).** No API, latency, retention, or encryption characteristics change:
the addition of ~12 short strings to two in-memory constant dictionaries has negligible performance
impact (matching remains O(total keyword count) per request). Existing UC60 NFRs continue to apply unchanged.

---

## 5. Static Modeling

### 5.1. Class Diagram
**Not applicable.** No new classes, no signature changes. The only touched members are the private
static constants `SymptomNormalizer.KEYWORDS` (Java) and module-level `ONTOLOGY` (Python).

### 5.2. Data Structure (Flyway SQL Migration)
**Not applicable.** No database change of any kind. The dictionaries are compile-time constants; no
Flyway migration is created (schema_changes = none).

### 5.3. Synonym Additions Table (THE core artifact of this TDS — oracle for the Test-Spec)

> Matching is performed on the **accent-stripped, lower-cased** form (column 3).
> `Source = Roadmap` → term mandated by `AITriage_Assessment_Roadmap.md` Part III §5.
> `Source = Proposed` → term proposed by the AI author. **Every row whose mapping is marked
> "pending clinical review" requires human clinical confirmation before approval.**

| # | Folk term (Vietnamese) | Accent-stripped form | Canonical symptom | New or Existing canonical | Oracle / Source & review status |
|---|------------------------|----------------------|-------------------|---------------------------|--------------------------------|
| S1 | thóp phập phồng | `thop phap phong` | `bulging_fontanelle` | **NEW — DEFERRED** (see ADR-TSSE-001) | Roadmap. Severity mapping = **Open** — requires clinical review; NOT implemented in this feature. |
| S2 | trớ sữa | `tro sua` | `vomiting` | Existing | Roadmap (term); canonical mapping AI-proposed — **pending clinical review** (posseting vs. true vomiting distinction is a clinical call). |
| S3 | sốt sình sịch | `sot sinh sich` | `fever` | Existing | Roadmap. **Already matches today** in both engines via the `sot` keyword (Java substring, Python word boundary) → regression-guard only, NOT Red-Gate eligible. Whether it should additionally signal *persistent* low-grade fever is **Open** (clinical review; no `persistent_fever` canonical exists). |
| S4 | hâm hấp (sốt hâm hấp) | `ham hap` | `fever` | Existing | Proposed — pending clinical review. |
| S5 | lừ đừ | `lu du` | `lethargy` | Existing | Proposed — pending clinical review. |
| S6 | sụt sịt | `sut sit` | `runny_nose` | Existing | Proposed — pending clinical review. |
| S7 | khò khè | `kho khe` | `difficulty_breathing` | Existing | Proposed — pending clinical review (wheeze; Python already has English `wheeze`, Vietnamese form missing on both sides). |
| S8 | thở rít | `tho rit` | `difficulty_breathing` | Existing | Proposed — pending clinical review (stridor; mapping to `difficulty_breathing` escalates conservatively via `RED_BREATHING_DISTRESS`, never de-escalates). |
| S9 | biếng ăn | `bieng an` | `poor_feeding` | Existing | Proposed — pending clinical review. |
| S10 | ọc sữa | `oc sua` | `vomiting` | Existing | Proposed — pending clinical review (same clinical caveat as S2). |
| S11 | đi ngoài (lỏng) | `di ngoai` | `diarrhea` | Existing | Proposed — pending clinical review ("đi ngoài" alone is ambiguous folk usage; reviewer may require the longer form `di ngoai long`). |
| S12 | ỉa chảy | `ia chay` | `diarrhea` | Existing | Proposed — pending clinical review. |
| S13 | rôm sảy | `rom say` | `rash` | Existing | Proposed — pending clinical review (heat rash is typically benign; mapping to `rash` only escalates when combined with high fever per existing rules). |

**Implemented scope = S2, S4–S13 (11 synonyms, all to EXISTING canonicals). S1 deferred (Open). S3 documented as already-covered.**

> **Implementation note (2026-07-27):** `đ` (U+0111) has no NFD canonical decomposition, so the
> engines' own accent stripping yields `lu đu` for S5 ("lừ đừ") and `đi ngoai` for S11 ("đi ngoài").
> The dictionaries therefore store BOTH the table's plain-d forms (`lu du`, `di ngoai` — for
> accent-less typing) AND the đ-variants (`lu đu`, `đi ngoai` — for diacritic input). Same folk
> terms, same canonical codes; no new mapping introduced (AP-TSSE-001 not triggered).

Terms considered and **rejected** to avoid engine false positives (out of scope to fix the engine):
"nổi mề đay" (`noi me day`) — Java substring matching would also fire `vomiting` via the `oi` inside `noi` (pre-existing engine quirk, recorded as Open item §11.4).

---

## 6. Dynamic Modeling

**Not applicable.** No flow, sequence, or state change — the intake sequence documented in
`UC60_RunAISymptomIntake_TDS.md` §6 is unchanged; only the contents of a lookup table grow.

---

## 7. Domain Event Catalog

**Not applicable.** No events published or consumed by this change.

---

## 8. Interface Specification

**Not applicable (no contract change).** Method signatures are untouched:
- Java: `List<String> SymptomNormalizer.normalize(RunIntakeRequest request)`
- Python: `normalize_symptoms(intake) -> list[str]`, `normalize_symptom_details_deterministic(intake) -> list[NormalizedSymptom]`
The output value domain also does not grow (all new synonyms map to codes that already exist).

---

## 9. API Specification

**Not applicable.** No endpoint, request, or response schema changes. `POST /triage/intake` behavior
changes only in that more Vietnamese phrasings normalize to already-supported codes.

---

## 10. Error Codes

**Not applicable.** No new error conditions; an unmatched phrase behaves exactly as before.

---

## 11. Implementation Plan (Step-by-Step)

### 11.1. Prerequisites
- [x] This TDS and the companion Test-Spec are `Approved` (project owner, 2026-07-26)
- [x] **Clinical review** for every `pending clinical review` row in §5.3 — satisfied via project-owner approval 2026-07-26 ("Approved hết"); no rows struck; NOT an independent clinician (recorded truthfully — follow-up clinical review advisable)
- [x] ADR-TSSE-001 accepted (same project-owner approval)

### 11.2. Pre-Migration Checklist
**Not applicable — no migration.**

### 11.3. Implementation Steps

1. **Red phase (tests first):** create `SymptomNormalizerTest.java`, `tests/test_symptom_synonym_expansion.py`, and both copies of `symptom_synonym_parity_vectors.json` exactly per the Test-Spec. Run both suites and confirm the new-synonym cases FAIL against the current dictionaries (Red Gate).
2. **Java dictionary edit:** add the approved accent-stripped forms from §5.3 to the matching `KEYWORDS` entries in `SymptomNormalizer.java`. Note Java quirks: `cough` uses `" ho "` with surrounding spaces; keep new entries free of substring collisions with existing keywords.
3. **Python dictionary edit:** add the identical forms to the matching `ONTOLOGY` tuples in `app/symptom_normalizer.py`. Python's negation guard (`_without_negated_candidate`) applies automatically to the new terms.
4. **Verify:**
   ```bash
   cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=SymptomNormalizerTest,TriageServiceTest,PediatricRedParityTest
   cd 05_Development/CareBridgeAITriageService && python -m pytest -q
   ```
5. Truthful spec sync per implement-flow Phase 4.

### 11.4. Open Items (carried out of this feature)
- [ ] `bulging_fontanelle` — new canonical code + severity mapping (Java `PediatricRiskRules`/`PediatricInfantRiskRules`, Python `risk_rules.py`, Python `ONTOLOGY`/`CANONICAL_SYMPTOM_CODES`): **Open — clinical review required; severity NOT proposed here.**
- [ ] Pre-existing parity gaps: Java `fever` keyword `"nong"` absent in Python; Python `high_fever`/`worsening_condition` keywords absent in Java `KEYWORDS`.
- [ ] Java substring `contains` false positives (e.g. `oi` inside `noi`) vs Python word-boundary matching — engine-level fix, separate feature.
- [ ] S3 "sốt sình sịch" persistence nuance (possible future `persistent_fever` concept) — clinical review.

### 11.4a. Deployment Checklist
- [x] Both test suites green (commands in 11.3 step 4) — Java: SymptomNormalizerTest 7/7, TriageServiceTest 52/52, PediatricRedParityTest 1/1; Python full suite 291 passed. Full `./mvnw clean test`: 3031 run, 1 failure + 75 errors + 100 skipped = known pre-existing ignore-set only (ChecklistTemplateMigrationTest SHA drift + Docker/Testcontainers env errors), zero new. (Actual runs 2026-07-27.)
- [x] Parity vector files byte-identical in both locations (`diff` empty, 2026-07-27)
- [x] No production files touched other than the two dictionary constants (verified via `git diff` — dictionary entries + provenance comment only)

---

## 12. Rollback & Incident Runbook

### 12.1. Trigger Conditions

| Condition | Threshold | Decider |
|-----------|-----------|---------|
| Any new synonym causes an unexpected canonical match (false positive) reported in triage output | Any confirmed case | Tech Lead + Clinical Reviewer |
| Java↔Python parity test failure post-merge | Any | On-call Engineer |

### 12.2. Rollback Procedure

No migration, no infra — rollback is a plain source revert:

```bash
git revert <feature-commit-sha>   # single commit containing both dictionary edits + tests
# or, pre-merge:
git checkout -- \
  "05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/engine/SymptomNormalizer.java" \
  "05_Development/CareBridgeAITriageService/app/symptom_normalizer.py"
```

Redeploy backend + Python service. Verify with the two test commands in §11.3 step 4.

### 12.3. Notification Protocol
Standard `#incident` Slack notification. DPO notification not applicable (no PII processing change).

### 12.4. Post-Incident Review
Standard PIR template applies only if a clinical false positive/negative reached a user.

---

## 13. Detailed Test Scenarios

Fully specified in the companion Test-Spec
(`04_Implement/TriageSymptomSynonymExpansion/TriageSymptomSynonymExpansion_Test-Spec.md`,
test cases `TSSE-TC-01` … `TSSE-TC-09`). Summary: per-synonym mapping (accent-stripped, with
diacritics, mixed case) on both sides; 17-canonical regression; numeric-rule regression;
Java↔Python parity via shared vectors; documented no-op check for S3. Test data: SYNTHETIC only.

---

## 14. Verification Method

**Database/log inspection: Not applicable** (no persistence or logging change). Verification is
test-based only:

```bash
# Java — new dedicated class + indirect regression coverage + parity precedent suite
cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=SymptomNormalizerTest,TriageServiceTest,PediatricRedParityTest

# Python — full suite (fake Gemini clients; no live API), per service README
cd 05_Development/CareBridgeAITriageService && python -m pytest -q
```

---

## 15. API Verification Samples

**Not applicable.** No API contract change. (Optional smoke: existing UC60 curl samples in
`UC60_RunAISymptomIntake_TDS.md` §15 with `parentFreeText: "bé trớ sữa, khò khè"` should yield
`normalizedSymptoms` containing `vomiting` and `difficulty_breathing`.)

---

## 16. Authorization Matrix

**Not applicable.** No endpoint or role change; UC60's existing matrix governs the intake flow.

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source (ADR/BR) | Last Verified |
|---|-----------|-----------------|---------------|
| C1 | Modify ONLY the two dictionary constants: `SymptomNormalizer.KEYWORDS` (Java) and `ONTOLOGY` (Python). NO engine/matching-logic changes, NO signature changes, NO new canonical codes. | ADR-TSSE-001 | 2026-07-26 |
| C2 | Every synonym MUST be added to BOTH dictionaries with the identical accent-stripped form, and MUST appear in `symptom_synonym_parity_vectors.json` (both copies, byte-identical). | BR-TRIAGE-PARITY | 2026-07-26 |
| C3 | Implement ONLY rows S2, S4–S13 of TDS §5.3 that survived clinical review. Do NOT implement S1 (`bulging_fontanelle`) and do NOT invent any severity/risk-rule change. | BR-SAFETY / ADR-TSSE-001 | 2026-07-26 |
| C4 | Accent-stripped forms are the stored dictionary values (matching input is lower-cased + `\p{M}`/`Mn` stripped by the existing engines); never store diacritic forms in the dictionaries. | TDS §1 component table (existing engine behavior) | 2026-07-26 |
| C5 | Red Gate: new-synonym tests must be shown FAILING against the unmodified dictionaries before the dictionary edit (data-change red = failing lookup, not a throw stub). | Test-Spec §5.1 | 2026-07-26 |
| C6 | No schema change, no Flyway migration, no new files outside: the 2 dictionary files, 2 test files, 2 parity vector copies. | ADR-TSSE-001 | 2026-07-26 |

### 17.2 Constraint Injection Block (Copy-Paste into AI Prompt)

```
[CONSTRAINT BLOCK — Module: TriageSymptomSynonymExpansion]
Per TDS CB-TRIAGE-IMP-005 and ADR-TSSE-001:

1. Touch ONLY SymptomNormalizer.KEYWORDS (Java) and ONTOLOGY (app/symptom_normalizer.py).
   No engine logic, signatures, schema, or new canonical codes.
2. Add each approved synonym (TDS §5.3 rows S2, S4–S13) to BOTH dictionaries using the
   identical accent-stripped form, and to symptom_synonym_parity_vectors.json in BOTH test trees.
3. Do NOT implement S1 (bulging_fontanelle). Do NOT touch PediatricRiskRules / risk_rules.py.
   Never invent a medical severity (BR-SAFETY).
4. Store accent-stripped, lower-cased forms only.
5. Red Gate first: prove the new-synonym tests FAIL against current dictionaries before editing them.

[CONTEXT BLOCK]
- Bounded Context: triage
- Data Classification: Internal (dictionary constants)
- Compliance: BR-SAFETY, BR-TRIAGE-PARITY
- Oracle: TDS §5.3 Synonym Additions Table
- Tests: Test-Spec TSSE-TC-01..09

[TASK BLOCK]
Implement the dictionary expansion satisfying the constraints above; all TSSE test cases green;
regression suites (TriageServiceTest, PediatricRedParityTest, python -m pytest -q) stay green.
```

### 17.3 Constraint Quality Checklist

- [x] Every constraint traceable to a specific ADR or BR
- [x] No generic constraints
- [x] Every constraint has `Last Verified` ≤ 2 sprints (2026-07-26)
- [x] Constraint block has ≥ 3 specific constraints (6)
- [x] Constraint block references the binding contract (§5.3 oracle table; §8 confirms no contract change)
- [x] Authorization: §16 N/A is explicit — AI must not invent auth changes

### 17.4 Anti-Pattern Detection (for AI-Generated Code from this Block)

| AP-ID | Anti-Pattern | Signal | Action |
|-------|-------------|--------|--------|
| AP-AI-001 | Unconstrained Gen | Diff touches files outside the 6 allowed files (C6) | Reject — re-inject constraints |
| AP-AI-003 | Implicit Decision | Code adds a new canonical code or risk-rule entry (violates ADR-TSSE-001 / C3) | Reject — clinical review path required |
| AP-AI-005 | Hallucinated Contract | Tests reference a normalizer API that does not exist (e.g. a public `stripAccents`) | Reject — verify against §8 signatures |
| AP-TSSE-001 | Invented Medicine | Any synonym/mapping in the diff not present in approved §5.3 | Reject — healthcare safety violation |

---

## APPENDIX

### A. Glossary

| Term | Definition |
|------|------------|
| Canonical symptom code | Stable English snake_case code (e.g. `poor_feeding`) that crosses the rule-engine boundary |
| Folk term | Colloquial Vietnamese phrasing parents actually use (e.g. "trớ sữa") |
| Accent-stripped form | Lower-cased NFD form with combining marks removed — the form stored in both dictionaries |
| Red Gate (data change) | New-synonym test failing against the unmodified dictionary |

### B. References

| Document | Path |
|----------|------|
| Requirement oracle (Part III §5) | `04_Implement/AITriageCompletion/AITriage_Assessment_Roadmap.md` |
| UC60 intake TDS (flow context) | `04_Implement/UC60_RunAISymptomIntake/UC60_RunAISymptomIntake_TDS.md` |
| Parity test precedent | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/PediatricRedParityTest.java`, `05_Development/CareBridgeAITriageService/tests/test_pediatric_red_parity.py` |
| Python service test runner | `05_Development/CareBridgeAITriageService/README.md` ("Test" section: `python -m pytest -q`) |
