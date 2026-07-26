# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# TriageSymptomSynonymExpansion — Vietnamese Folk-Term Synonym Dictionary Expansion

**Document ID:** `CB-TRIAGE-IMP-005-TEST`
**Version:** `1.0`
**Date:** `2026-07-26`
**Status:** `Approved — Implemented 2026-07-27`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[x] Project owner approval 2026-07-26 ("Approved hết") — NOT an independent clinical reviewer`
**DPO Sign-off:** `[ ] Not required — data-only dictionary change, no PII handling change`
**Approved by:** `[x] Project owner, 2026-07-26 ("Approved hết")`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/TriageSymptomSynonymExpansion/TriageSymptomSynonymExpansion_TDS.md` (CB-TRIAGE-IMP-005) — **§5.3 Synonym Additions Table is THE oracle for all expected values below**
- `04_Implement/AITriageCompletion/AITriage_Assessment_Roadmap.md` Part III §5 — requirement source
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/engine/SymptomNormalizer.java` — current Java dictionary (baseline)
- `05_Development/CareBridgeAITriageService/app/symptom_normalizer.py` — current Python dictionary (baseline)
- `08_References/Template/PHASE-4_Test-Spec.md`

> **TDD convention:** tests are written BEFORE the dictionary edit. Mandatory order:
> write tests → run → confirm FAIL 🔴 → edit dictionaries → PASS 🟢 → refactor 🔵.
> SYNTHETIC test data only — no real parent narratives.

---

## CHANGELOG

| Date | Person | Change |
|------|--------|--------|
| 2026-07-26 | AI Agent | Initial creation — TDD spec for synonym dictionary expansion |
| 2026-07-27 | AI Agent — Amelia (Dev Agent) | Phase 3+4: Red Gate executed (Java 4/7 FAIL, Python 36/58 FAIL — exactly the Red-Gate-eligible set; evidence in `red-gate-evidence.log`), dictionaries edited, all 9 TCs GREEN on both sides. Documented deviation: `đ` (U+0111) has no NFD decomposition, so đ-variants `lu đu` (S5) and `đi ngoai` (S11) were added alongside the §5.3 plain-d forms `lu du`/`di ngoai` — same folk terms, same canonicals; required for TSSE-TC-02 diacritic inputs. Entry-criteria clinical-review gate satisfied via project-owner approval dated 2026-07-26 ("Approved hết"), NOT an independent clinician. |

---

## TABLE OF CONTENTS

1. [Module Info](#1-module-info)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `CB-TRIAGE-IMP-005` (roadmap Part III item 5) |
| **Module** | `Symptom Normalizer dictionaries — triage` |
| **Spec gốc** | `CB-TRIAGE-IMP-005` (TriageSymptomSynonymExpansion_TDS.md) |
| **Priority** | 🟡 P2 (roadmap: "việc nhỏ, làm dần") — but healthcare-safety review gated |
| **Sprint** | `TBD` |
| **Milestone** | `TBD` |
| **Data Classification** | `Internal` (dictionary constants; SYNTHETIC test inputs) |
| **Compliance Scope** | `BR-SAFETY, BR-TRIAGE-PARITY` |
| **Upstream Dependencies** | None (pure constants) |
| **Downstream Consumers** | `TriageService` / Python intake flow (consume canonical codes — unchanged) |

**Test commands (verified against the repo):**

```bash
# Java — new dedicated test class (to be created; no SymptomNormalizerTest exists today)
cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=SymptomNormalizerTest
# Java — regression + parity precedent suites
cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=TriageServiceTest,PediatricRedParityTest

# Python — per 05_Development/CareBridgeAITriageService/README.md ("Test" section)
cd 05_Development/CareBridgeAITriageService && python -m pytest -q
# Python — targeted new module
cd 05_Development/CareBridgeAITriageService && python -m pytest tests/test_symptom_synonym_expansion.py -q
```

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-TRIAGE-IMP-005 §17`, `ADR-TSSE-001` |
| **Constraints Injected** | C1 (dictionary-only), C2 (Java↔Python parity + shared vectors), C3 (no S1 / no invented severity), C4 (accent-stripped storage), C5 (data-change Red Gate), C6 (6-file blast radius) |
| **Model** | `Claude (Fable 5)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` — **medical mappings additionally require human clinical review (TDS §5.3)** |

---

## 2. Logic Issues Resolved

> Discovered by reading both current dictionaries before writing tests. Tests encode the **corrected**
> understanding below, not naive assumptions.

| # | Original assumption (wrong/missing) | Reality (codebase) | Fix applied in tests |
|---|-------------------------------------|---------------------|----------------------|
| L1 | "Both sides have the same 17 canonical codes" | Java `KEYWORDS` has 17 codes; Python `ONTOLOGY` has 19 (`high_fever`, `worsening_condition` extra). | Parity assertions (TSSE-TC-08) cover only the codes targeted by new synonyms + the 17 shared codes; the 2 Python-only codes are excluded and recorded as Open (TDS §11.4). |
| L2 | "Matching engines are equivalent" | Java = substring `contains` on accent-stripped text (e.g. `" ho "` needs spaces; `oi` matches inside `noi`); Python = word-boundary regex + negation guard `_without_negated_candidate`. | New synonyms were screened for substring collisions (TDS §5.3 rejected `noi me day`). Tests assert *presence* of the expected code, and only assert absence where behavior is engine-safe on both sides. |
| L3 | "Roadmap term ⇒ Red-Gate-eligible test" | `sot sinh sich` (S3) ALREADY matches `fever` on both sides via the standalone `sot` token. A "new" test for it passes from birth. | TSSE-TC-09 is explicitly classified as a **regression guard, excluded from Red Gate** (documented no-op). Only S2, S4–S13 terms are Red-Gate cases. |
| L4 | "There is an existing SymptomNormalizerTest to extend" | No dedicated Java unit test exists; the normalizer is only exercised inside `TriageServiceTest` (constructor use, line ~1318). | Test-Spec creates NEW `SymptomNormalizerTest.java`; regression on existing behavior is asserted both there (TSSE-TC-04/05) and via the untouched `TriageServiceTest`. |
| L5 | "Numeric/structured rules might need updating" | Numeric rules (≥37.5→`fever`, ≥39.0→`high_fever`, seizure flag, dehydration signs) live in code, not the dictionary. | TSSE-TC-05 pins them as pure regression; the feature must not alter them. |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
SymptomNormalizer dictionaries (data-only)
├── Java unit  : SymptomNormalizer.normalize(RunIntakeRequest) — pure component, no mocks needed
├── Python unit: normalize_symptoms / normalize_symptom_details_deterministic — pure functions
└── Parity     : shared JSON vectors executed by BOTH sides (pediatric_red_parity pattern)
Out of scope: controller/API layer, DB, Gemini fallback, risk-rule engines (unchanged).
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `CB-TRIAGE-IMP-005 §5.3` | Expected canonical code per folk term (THE oracle — no AI assumption) |
| `AITriage_Assessment_Roadmap.md` Part III §5 | S1–S3 term list |
| `BR-TRIAGE-PARITY` (TDS §1 invariant) | Java↔Python parity cases |
| `BR-SAFETY` | No new canonical / severity assertions anywhere in this suite |
| Current `SymptomNormalizer.java` / `symptom_normalizer.py` | Regression baseline (17 codes, numeric rules, negation guard) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Each new synonym (accent-stripped input) → expected canonical code (Java) | `SymptomNormalizer.normalize()` | TSSE-TC-01 |
| TC-COND-002 | Diacritic input forms are stripped and matched (Java) | `stripAccents` path | TSSE-TC-02 |
| TC-COND-003 | Mixed-case input matched (Java) | lower-casing path | TSSE-TC-03 |
| TC-COND-004 | No regression on 17 existing canonical codes (Java) | `KEYWORDS` baseline | TSSE-TC-04 |
| TC-COND-005 | Numeric/structured rules unchanged (Java) | temperature/seizure/dehydration branches | TSSE-TC-05 |
| TC-COND-006 | Each new synonym → canonical, 3 case forms (Python) | `normalize_symptoms` | TSSE-TC-06 |
| TC-COND-007 | Python regression + negation guard on new terms | `ONTOLOGY`, `_without_negated_candidate` | TSSE-TC-07 |
| TC-COND-008 | Java↔Python parity on shared vectors | both normalizers + vector files | TSSE-TC-08 |
| TC-COND-009 | S3 `sot sinh sich` already→`fever` both sides (documented no-op) | `sot` keyword | TSSE-TC-09 |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | one input phrase per synonym row (S2, S4–S13) | Each dictionary row is its own partition |
| Syntax/Format Variation | accent-stripped vs diacritics vs mixed case | Exercises the two normalization pre-steps |
| Regression (golden set) | 17 existing canonical codes + numeric rules | Data change must be additive-only |
| Back-to-back (parity) testing | identical vectors on Java and Python | Enforces BR-TRIAGE-PARITY |
| Error Guessing | negated phrase (`khong tro sua`) on Python | Negation guard must apply to new terms |

### TDS-05 — Test Data Requirements (all SYNTHETIC)

| Fixture ID | Type | Value / Logic | Purpose |
|-----------|------|---------------|---------|
| `FX-SYN-01` | constant table in test code | The 11 implemented rows of TDS §5.3 (S2, S4–S13): `(folkTermDiacritic, accentStripped, canonicalCode)` | Parameterized mapping cases |
| `FX-SYN-02` | JSON file | `symptom_synonym_parity_vectors.json` — `[{"parentFreeText": "...", "expectedCodes": ["..."]}]`, byte-identical at `05_Development/CareBridgeAPI/src/test/resources/triage/` and `05_Development/CareBridgeAITriageService/tests/data/` | Parity oracle (TSSE-TC-08) |
| `FX-SYN-03` | golden list | One representative existing keyword per current canonical code (e.g. `sot`→fever, `rut lom`→chest_indrawing, …) taken verbatim from the current dictionaries | Regression (TSSE-TC-04/07) |
| `FX-SYN-04` | request factory | Minimal `RunIntakeRequest` / `ChildTriageRequest` with only `parentFreeText` (or `symptomList`) set | Isolation — no unrelated structured fields |

---

## 4. Test Case Specification

> **TC ID format:** `TSSE-TC-[NN]` · **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — MANDATORY)

```java
// SymptomNormalizerTest.java — CASE 2.0 Props Isolation
// Fresh instances per test; no shared mutable state (anti AP-AI-002).
class SymptomSynonymTestFactory {

    static SymptomNormalizer makeNormalizer() {
        return new SymptomNormalizer(); // stateless component, fresh per test anyway
    }

    // FX-SYN-04 — minimal request, ONLY free text set
    static RunIntakeRequest makeFreeTextRequest(String parentFreeText) {
        RunIntakeRequest request = new RunIntakeRequest();
        request.setParentFreeText(parentFreeText);
        return request;
    }

    // FX-SYN-01 — oracle rows copied verbatim from TDS CB-TRIAGE-IMP-005 §5.3 (S2, S4–S13)
    record SynonymRow(String diacritic, String stripped, String canonical) {}
    static List<SynonymRow> synonymRows() {
        return List.of(
            new SynonymRow("trớ sữa", "tro sua", "vomiting"),
            new SynonymRow("hâm hấp", "ham hap", "fever"),
            new SynonymRow("lừ đừ", "lu du", "lethargy"),
            new SynonymRow("sụt sịt", "sut sit", "runny_nose"),
            new SynonymRow("khò khè", "kho khe", "difficulty_breathing"),
            new SynonymRow("thở rít", "tho rit", "difficulty_breathing"),
            new SynonymRow("biếng ăn", "bieng an", "poor_feeding"),
            new SynonymRow("ọc sữa", "oc sua", "vomiting"),
            new SynonymRow("đi ngoài", "di ngoai", "diarrhea"),
            new SynonymRow("ỉa chảy", "ia chay", "diarrhea"),
            new SynonymRow("rôm sảy", "rom say", "rash"));
    }
}
```

```python
# tests/test_symptom_synonym_expansion.py — Props Isolation (Python mirror)
# Fresh request per test via factory; SYNONYM_ROWS mirrors TDS §5.3 verbatim (same 11 rows).
def make_free_text_request(parent_free_text: str) -> ChildTriageRequest:
    return ChildTriageRequest(..., parentFreeText=parent_free_text)  # minimal valid intake, FX-SYN-04
```

---

### TSSE-TC-01 — Java: each new synonym (accent-stripped input) maps to its canonical code

**Severity:** `HIGH`
**Legal:** `BR-SAFETY (mappings pre-approved via TDS §5.3 clinical review)`
**Feature Under Test:** `SymptomNormalizer.normalize()`
**Test File:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/SymptomNormalizerTest.java`
**TDD Phase:** 🟢 GREEN — Red Gate FAIL confirmed 2026-07-27, then dictionary edit → PASS
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-TRIAGE-IMP-005 §5.3` rows S2, S4–S13 ← no AI assumption

**Preconditions:** FX-SYN-01, FX-SYN-04.

**Test Steps:**
1. Arrange: for each `SynonymRow`, build `makeFreeTextRequest(row.stripped())`.
2. Act: `List<String> codes = makeNormalizer().normalize(request)`.
3. Assert: `codes` contains `row.canonical()`.

**Expected Result (PASS):** every implemented §5.3 row maps to its canonical code.
**Expected Result (FAIL):** returned list missing the canonical code (current dictionary — verified state, see §5.1).

**Current Status:** 🟢 Passing

---

### TSSE-TC-02 — Java: diacritic input forms are matched (accent-strip path)

**Severity:** `HIGH`
**Feature Under Test:** `SymptomNormalizer.normalize()` + private accent stripping (`\p{M}+`)
**Test File:** `SymptomNormalizerTest.java` (same as TC-01)
**TDD Phase:** 🟢 GREEN — Red Gate FAIL confirmed 2026-07-27, then dictionary edit → PASS
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-TRIAGE-IMP-005 §5.3` (diacritic column)

**Test Steps:** same as TC-01 but input = `row.diacritic()` (e.g. `"trớ sữa"`, `"khò khè"`).
**Expected Result (PASS):** same canonical codes as TC-01 (engine strips accents before lookup).
**Expected Result (FAIL):** code missing → dictionary entry absent or stored with diacritics (violates TDS C4).

**Current Status:** 🟢 Passing

---

### TSSE-TC-03 — Java: mixed-case input is matched (lower-casing path)

**Severity:** `MEDIUM`
**Feature Under Test:** `SymptomNormalizer.normalize()`
**Test File:** `SymptomNormalizerTest.java`
**TDD Phase:** 🟢 GREEN — Red Gate FAIL confirmed 2026-07-27, then dictionary edit → PASS
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-TRIAGE-IMP-005 §5.3`

**Test Steps:** input = mixed-case diacritic forms (e.g. `"Trớ SỮA"`, `"KHÒ khè"`); assert canonical present.
**Expected Result (PASS):** matched (engine lower-cases via `Locale.ROOT` before stripping).
**Expected Result (FAIL):** case-sensitive entry added — reject.

**Current Status:** 🟢 Passing

---

### TSSE-TC-04 — Java: NO regression on the 17 existing canonical symptoms

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY — a lost mapping can suppress a danger-sign escalation`
**Feature Under Test:** `SymptomNormalizer.normalize()` — full existing `KEYWORDS` baseline
**Test File:** `SymptomNormalizerTest.java`
**TDD Phase:** 🟢 GREEN — regression guard: PASSED at red phase as pre-declared (§5.1), still passing after dictionary edit (2026-07-27)
**Condition Ref:** `TC-COND-004`
**Oracle Source:** current `SymptomNormalizer.java` `KEYWORDS` map (FX-SYN-03 golden list copied verbatim: `sot`→fever, `" ho "`→cough via input `"be ho nhieu"`, `so mui`→runny_nose, `kho tho`→difficulty_breathing, `rut lom`→chest_indrawing, `tim tai`→cyanosis, `co giat`→seizure, `li bi`→lethargy, `kho danh thuc`→difficult_to_wake, `khong uong`→unable_to_drink, `bo bu`→poor_feeding, `non`→vomiting, `non lien tuc`→persistent_vomiting, `tieu chay`→diarrhea, `phat ban`→rash, `moi kho`→mild_dehydration, `mat nuoc nang`→severe_dehydration)

**Test Steps:** parameterized over the 17 golden pairs; assert each still normalizes to its code after the dictionary edit.
**Expected Result (PASS):** all 17 unchanged. **FAIL:** any existing mapping lost/altered → reject the change.

**Current Status:** 🟢 Passing

---

### TSSE-TC-05 — Java: numeric/structured rules unchanged

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY`
**Feature Under Test:** temperature (≥37.5→`fever`, ≥39.0→`high_fever`), `seizure` flag, `dehydrationSigns` branches of `SymptomNormalizer.normalize()`
**Test File:** `SymptomNormalizerTest.java`
**TDD Phase:** 🟢 GREEN — regression guard: PASSED at red phase as pre-declared (§5.1), still passing after dictionary edit (2026-07-27)
**Condition Ref:** `TC-COND-005`
**Oracle Source:** current `SymptomNormalizer.java` lines 48–63 (numeric thresholds) — cited as-is

**Test Steps:** requests with `temperatureC=37.5 / 38.9 / 39.0`, `seizure=true`, `dehydrationSigns=["mat nuoc nang"]`; assert `fever`, `fever`(no high_fever), `fever`+`high_fever`, `seizure`, `severe_dehydration` respectively.
**Expected Result (PASS):** thresholds intact. **FAIL:** feature accidentally touched engine logic (violates C1).

**Current Status:** 🟢 Passing

---

### TSSE-TC-06 — Python: each new synonym maps to its canonical code (3 case forms)

**Severity:** `HIGH`
**Feature Under Test:** `normalize_symptoms()` / `normalize_symptom_details_deterministic()` (`app/symptom_normalizer.py`)
**Test File:** `05_Development/CareBridgeAITriageService/tests/test_symptom_synonym_expansion.py`
**TDD Phase:** 🟢 GREEN — Red Gate FAIL confirmed 2026-07-27, then dictionary edit → PASS
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-TRIAGE-IMP-005 §5.3` rows S2, S4–S13 (same table = same oracle as Java)

**Test Steps:**
1. Parameterize over `SYNONYM_ROWS` × input form ∈ {accent-stripped, diacritic, mixed case}.
2. `codes = normalize_symptoms(make_free_text_request(form))` (no Gemini client — deterministic path only).
3. Assert `row.canonical in codes`; additionally assert the `NormalizedSymptom.normalizationMethod == "KEYWORD"` for a representative row.

**Expected Result (PASS):** all forms map; deterministic method used.
**Expected Result (FAIL):** code missing → `ONTOLOGY` not updated (current state) or diacritic form stored.

**Current Status:** 🟢 Passing

---

### TSSE-TC-07 — Python: regression on existing ONTOLOGY codes + negation guard applies to new terms

**Severity:** `CRITICAL`
**Legal:** `BR-SAFETY — negation guard prevents false-positive escalation on "không trớ sữa"`
**Feature Under Test:** `ONTOLOGY` baseline + `_without_negated_candidate()`
**Test File:** `tests/test_symptom_synonym_expansion.py`
**TDD Phase:** 🟢 GREEN — negation sub-case FAILED at red phase (Red-Gate-eligible), baseline sub-cases PASSED at red phase as pre-declared (§5.1); all passing after dictionary edit (2026-07-27)
**Condition Ref:** `TC-COND-007`
**Oracle Source:** current `symptom_normalizer.py` `ONTOLOGY` (19 codes) + negation regex; TDS §5.3 for the new-term inputs

**Test Steps:**
1. Regression: for each of the 19 current codes, one representative existing keyword still maps (golden list verbatim from current `ONTOLOGY`).
2. Negation: `"khong tro sua"` → `vomiting` NOT in result; `"be tro sua"` → `vomiting` in result.

**Expected Result (PASS):** baseline intact; negated new term suppressed.
**Expected Result (FAIL):** lost baseline mapping, or negated phrase still matches.

**Current Status:** 🟢 Passing

---

### TSSE-TC-08 — Parity: Java and Python produce identical canonical codes for shared vectors

**Severity:** `CRITICAL`
**Legal:** `BR-TRIAGE-PARITY (TDS §1 invariant); BR-SAFETY (fallback engine must not disagree with primary)`
**Feature Under Test:** both normalizers, driven by `symptom_synonym_parity_vectors.json` (FX-SYN-02)
**Test File:** Java side in `SymptomNormalizerTest.java` (loads `/triage/symptom_synonym_parity_vectors.json`); Python side in `tests/test_symptom_synonym_expansion.py` (loads `tests/data/symptom_synonym_parity_vectors.json`) — pattern copied from `PediatricRedParityTest.java` / `test_pediatric_red_parity.py`
**TDD Phase:** 🟢 GREEN — Red Gate FAIL confirmed 2026-07-27, then dictionary edit → PASS
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-TRIAGE-IMP-005 §5.3` (vectors generated 1:1 from implemented rows; `expectedCodes` restricted to the shared 17-code set per Logic Issue L1)

**Test Steps:**
1. Vector file contains one entry per implemented synonym: `{"parentFreeText": "<diacritic form>", "expectedCodes": ["<canonical>"]}`.
2. Java test: for each vector, `normalize()` result contains all `expectedCodes`.
3. Python test: identical assertion via `normalize_symptoms()`.
4. Both tests also assert the two vector files' content hashes conceptually match — practically: keep files byte-identical (checked in Exit Criteria).

**Expected Result (PASS):** every vector satisfied on BOTH sides.
**Expected Result (FAIL):** a synonym added on one side only — the parity invariant is broken.

**Current Status:** 🟢 Passing

---

### TSSE-TC-09 — S3 "sốt sình sịch" already normalizes to fever on both sides (documented no-op / regression guard)

**Severity:** `LOW`
**Feature Under Test:** `sot` keyword matching (Java substring / Python word boundary)
**Test File:** `SymptomNormalizerTest.java` + `tests/test_symptom_synonym_expansion.py`
**TDD Phase:** 🟢 GREEN — passed from birth as pre-declared (⚠️ deliberately EXCLUDED from Red Gate; documents Logic Issue L3 so a future reader does not mistake S3 for a new mapping); still passing after dictionary edit (2026-07-27)
**Condition Ref:** `TC-COND-009`
**Oracle Source:** Logic Issue L3 (verified against current dictionaries) + roadmap Part III §5

**Test Steps:** input `"sốt sình sịch"` / `"sot sinh sich"` → assert `fever` in result on both sides.
**Expected Result (PASS):** `fever` present (true today and after the change).
**Expected Result (FAIL):** would indicate a regression in the pre-existing `sot` keyword.

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

**Not applicable.** No new attack surface: no endpoint, parsing, or privilege change. Prompt-injection
guards (`INSTRUCTION_PATTERNS`) and PII masking (`_mask`) are untouched and remain covered by the
existing suites (`test_triage.py`, `test_gemini_integration.py`).

### INTEGRATION TEST CASES

**Not applicable (unit-level data change).** End-to-end intake behavior is already covered by the
untouched `TriageIntegrationTest.java` and `tests/test_intake_flow.py`; both MUST still pass (Exit Criteria).

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `TSSE-TC-01` | `SymptomNormalizerTest.java` | `[x]` FAIL confirmed 2026-07-27 | Passed (working tree 2026-07-27, no commit yet — NO-COMMIT rule) | none — data-only |
| `TSSE-TC-02` | `SymptomNormalizerTest.java` | `[x]` FAIL confirmed 2026-07-27 | Passed (working tree 2026-07-27) | đ-variants `lu đu`/`đi ngoai` added for S5/S11 (see CHANGELOG deviation note) |
| `TSSE-TC-03` | `SymptomNormalizerTest.java` | `[x]` FAIL confirmed 2026-07-27 | Passed (working tree 2026-07-27) | |
| `TSSE-TC-04` | `SymptomNormalizerTest.java` | `[x]` *(regression guard — PASSED at red phase as pre-declared; see §5.1 note)* | Passed (working tree 2026-07-27) | |
| `TSSE-TC-05` | `SymptomNormalizerTest.java` | `[x]` *(regression guard — PASSED at red phase as pre-declared)* | Passed (working tree 2026-07-27) | |
| `TSSE-TC-06` | `tests/test_symptom_synonym_expansion.py` | `[x]` FAIL confirmed 2026-07-27 (34 sub-cases) | Passed (working tree 2026-07-27) | |
| `TSSE-TC-07` | `tests/test_symptom_synonym_expansion.py` | `[x]` *(negation sub-case FAILED as required; 19 baseline sub-cases PASSED as pre-declared)* | Passed (working tree 2026-07-27) | |
| `TSSE-TC-08` | both parity tests | `[x]` FAIL confirmed 2026-07-27 on BOTH sides | Passed (working tree 2026-07-27; vector copies byte-identical, `diff` empty) | |
| `TSSE-TC-09` | both files | `[x]` *(documented no-op — PASSED from birth as pre-declared; excluded from Red Gate)* | Passed (working tree 2026-07-27) | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> **Adaptation for a pure DATA addition:** there is no service stub to make throw. The "red" state is
> defined as: *the new-synonym tests run against the CURRENT, UNMODIFIED dictionaries and FAIL because
> the lookup returns no canonical code.* The current dictionaries therefore ARE the stub.
>
> **Baseline verification already performed while authoring this spec (by reading the current
> dictionaries):** none of the 11 implemented accent-stripped forms (`tro sua`, `ham hap`, `lu du`,
> `sut sit`, `kho khe`, `tho rit`, `bieng an`, `oc sua`, `di ngoai`, `ia chay`, `rom say`) appears in
> `SymptomNormalizer.KEYWORDS` (17 entries) or `symptom_normalizer.ONTOLOGY` (19 entries), and none
> collides as a substring with an existing Java keyword — so TSSE-TC-01/02/03/06/07(negation)/08
> genuinely FAIL today. Conversely `sot sinh sich` DOES already match `sot` on both sides, which is
> why TSSE-TC-09 (and the pure-regression TCs 04/05 and TC-07 baseline sub-cases) are **excluded**
> from the Red Gate FAIL requirement and marked as regression guards instead. This must be
> re-confirmed by actually RUNNING the suites at red phase.

**"Stub" for Red Phase:** the unmodified `SymptomNormalizer.java` / `symptom_normalizer.py` at the
baseline commit — no code is written or altered for the red run.

**Red Gate Verification:**

| TC ID | Stub Result (current dictionary) | Expected | Actual | Root Cause (if unexpected PASS) |
|-------|----------------------------------|----------|--------|---------------------------------|
| `TSSE-TC-01` | lookup miss — canonical absent | 🔴 FAIL | ☑ FAIL ☐ PASS | — (failed as required) |
| `TSSE-TC-02` | lookup miss | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TSSE-TC-03` | lookup miss | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TSSE-TC-06` | lookup miss | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TSSE-TC-07` (negation + new-term sub-cases only) | lookup miss | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TSSE-TC-08` | vector file entries unmatched | 🔴 FAIL | ☑ FAIL ☐ PASS | |
| `TSSE-TC-04`, `TSSE-TC-05`, `TSSE-TC-09`, TC-07 baseline sub-cases | regression guards | 🟢 expected PASS at red phase (by design — NOT a Red Gate violation; documented in L3/L5) | ☑ PASS ☐ FAIL | Regression guards passed at red phase exactly as pre-declared — baseline golden lists confirmed correct |

**Red Gate Evidence:**

- Baseline commit hash: `8b33c700` (branch HuyND; working tree additionally carried uncommitted sibling triage features, but both dictionaries were verbatim baseline at red run)
- All Red-Gate-eligible TCs FAIL? ☑ [x] Yes → **GATE-2 PASS** (T2→T3) → proceeded to dictionary edit (Java: 4/7 FAIL = TC-01/02/03/08; Python: 36/58 FAIL = TC-06 all 34 sub-cases + TC-07 negation + TC-08)
- Log file: `04_Implement/TriageSymptomSynonymExpansion/red-gate-evidence.log` (written 2026-07-27 from actual runs)

> **If a Red-Gate-eligible test PASSES:** stop; the term already matches (substring collision like
> the rejected `noi me day` case) — take the term back to TDS §5.3 for re-review instead of implementing.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-TRIAGE-IMP-005` reviewed and `Approved`
- [x] **Clinical review of TDS §5.3** — satisfied via **project-owner approval dated 2026-07-26 ("Approved hết")**, NOT an independent clinician. All 11 rows S2, S4–S13 survived as written; S1 remains DEFERRED per ADR-TSSE-001. A follow-up review by a person with clinical competence remains advisable (recorded honestly per Truthful Sync).
- [x] Logic Issues (§2) acknowledged by reviewer (covered by the same project-owner approval)
- [x] No Flyway migration required (confirmed — data-only change)
- [x] Fixtures FX-SYN-01…04 prepared from the FINAL approved §5.3 table (11 rows, unchanged)

### Exit Criteria (DoD)

- [x] `cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=SymptomNormalizerTest` — green, no skips (Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 — actual run 2026-07-27)
- [x] `./mvnw test -Dtest=TriageServiceTest,PediatricRedParityTest,TriageIntegrationTest` — green (TriageServiceTest 52/52, PediatricRedParityTest 1/1 in targeted run; TriageIntegrationTest 1/1 green inside the full `clean test` run 2026-07-27)
- [x] `cd 05_Development/CareBridgeAITriageService && python -m pytest -q` — full suite green: **291 passed** (prior baseline 233 + 58 new TSSE tests; actual runs 2026-07-27)
- [x] Parity vector files byte-identical: `diff` of the two `symptom_synonym_parity_vectors.json` copies is empty (verified 2026-07-27)
- [x] Diff blast radius = exactly 6 files (2 dictionaries, 2 test files, 2 vector copies) — TDS C6 (verified via `git status`/`git diff`; only additional artifact is the spec-mandated `red-gate-evidence.log` + these spec syncs)
- [x] No risk-rule file touched (`PediatricRiskRules*.java`, `risk_rules.py`) — TDS C3 (verified in diff)

> **Full-repo regression note (actual, 2026-07-27):** `./mvnw clean test` = `Tests run: 3031, Failures: 1, Errors: 75, Skipped: 100`. The 1 failure (`ChecklistTemplateMigrationTest.uc82_69_int_005_v1RemainsByteIdentical` SHA drift) and all 75 errors (Docker/Testcontainers `AbstractPostgresIntegrationTest` family, no Docker daemon in this environment) are the KNOWN pre-existing ignore-set — zero regressions introduced by this feature.

**Exit Criteria bổ sung — CASE 2.0:**

- [x] **Red Gate (§5.1)** — all Red-Gate-eligible TCs confirmed FAIL against baseline dictionaries (actual runs 2026-07-27; evidence log)
- [x] **Contract Existence** — Java test compiled and ran (surefire); `python -m pytest --collect-only -q` clean (291 tests collected)
- [x] **Props Isolation** — all instances created inside tests / via factory (no shared mutable state)
- [x] **Oracle Source** — every expected canonical code in asserts traces to TDS §5.3 row IDs (S2, S4–S13)

### Suspension Criteria

- Clinical reviewer strikes or disputes any §5.3 row → suspend, update TDS §5.3 + FX-SYN-01/02, re-enter
- Any regression guard (TC-04/05/07-baseline/09) fails at red phase → baseline misunderstood, stop

---

## 7. Rollback Plan

No migration and no schema change — rollback is source-only:

```bash
# Post-merge: revert the single feature commit (dictionaries + tests + vectors together)
git revert <feature-commit-sha>

# Pre-merge (working tree):
git checkout -- \
  "05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/engine/SymptomNormalizer.java" \
  "05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/SymptomNormalizerTest.java" \
  "05_Development/CareBridgeAPI/src/test/resources/triage/symptom_synonym_parity_vectors.json" \
  "05_Development/CareBridgeAITriageService/app/symptom_normalizer.py" \
  "05_Development/CareBridgeAITriageService/tests/test_symptom_synonym_expansion.py" \
  "05_Development/CareBridgeAITriageService/tests/data/symptom_synonym_parity_vectors.json"

# Feature remains OPEN in the roadmap (Part III §5) until re-attempted
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Signal in this TDD spec | Check | Blocking Gate |
|-------|-------------|--------------------------|-------|-----------------|
| AP-AI-001 | Unconstrained Generation | A TC asserts a mapping not present in TDS §5.3 | ☑ checked 2026-07-27 — every asserted mapping traces to §5.3 rows S2, S4–S13 (đ-variants are encoding forms of S5/S11, not new mappings) | G-0 |
| AP-AI-002 | Green-from-Birth | A Red-Gate-eligible TC passes against baseline dictionaries (§5.1) — NOTE: TC-04/05/09 pass by design and are pre-declared exceptions | ☑ checked 2026-07-27 — all Red-Gate-eligible TCs FAILED at red phase (see §5.1 Actual column + evidence log) | G-2 ★ |
| AP-AI-003 | Implicit Decision | A TC assumes a new canonical code (e.g. `bulging_fontanelle`) or a severity outcome | ☑ checked 2026-07-27 — guard test `test_no_new_canonical_code_introduced` asserts `bulging_fontanelle` absent; no risk-rule file touched | G-1 |
| AP-AI-004 | Layer Violation | A TC drags controller/API/DB into this unit-level data change | ☑ checked 2026-07-27 — tests use only `SymptomNormalizer.normalize()` / `normalize_symptoms()` pure paths | G-4 |
| AP-AI-005 | Hallucinated Contract | A TC calls a non-existent API (e.g. public `SymptomNormalizer.stripAccents`) | ☑ checked 2026-07-27 — only public `normalize(RunIntakeRequest)` and module-level Python functions used; `./mvnw` compile + pytest collection clean | G-3 |
| AP-TSSE-001 | Invented Medicine | Any expected value whose §5.3 row was struck by the clinical reviewer | ☑ checked 2026-07-27 — no row struck (project-owner approval); S1 NOT implemented; only S2, S4–S13 mappings in the diff | Entry Criteria |

**Review result:**

- [x] No anti-pattern detected → TDD spec approved *(verified against actual diff and test runs, 2026-07-27; note: clinical sign-off level = project owner, see Entry Criteria)*
- [ ] AP detected → record below → fix before implementation

| AP detected | TC ID | Description | Fix action | Fixed? |
|------------|-------|-------------|------------|--------|
| | | | | ☐ |
