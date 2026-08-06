# Codex Task Contract

**Task ID:** PHASE0-GOV-001
**Phase:** 0 — Final pre-LangGraph gate closure
**Objective:** Migrate the canonical rule registry from the legacy governance field
`status: "APPROVED"` to an explicit `releaseStatus`, keep loaders backward-compatible with
older artifacts, and add tests. **No clinical outcome may change.**

---

## Why (read this, it constrains the design)

CareBridge is an ACADEMIC_COMMUNITY_PROJECT. **No clinician reviewed or approved these rules.**
A field literally named `APPROVED` reads as clinical approval and is therefore a governance
defect, not a cosmetic one. See `AI_TRIAGE_V2_DECISIONS.md` → **D-011**.

---

## Read First

- `docs/Detailed_Design/MF06_AI_Nurse_Assistant_Risk_Triage/AI_TRIAGE_V2_MASTER_PLAN.md` (§1, §2)
- `docs/Detailed_Design/MF06_AI_Nurse_Assistant_Risk_Triage/AI_TRIAGE_V2_DECISIONS.md` (D-011, D-010)
- `05_Development/Contracts/triage/triage_rules_v2.json`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/rules/TriageRuleRegistry.java`
- `05_Development/CareBridgeAITriageService/app/rules/registry.py`
- `05_Development/DevTools/sync_triage_rule_registry.py`

## Allowed Paths (edit only these)

- `05_Development/Contracts/triage/triage_rules_v2.json`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/rules/TriageRuleRegistry.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/rules/TriageRule.java`
- `05_Development/CareBridgeAITriageService/app/rules/registry.py`
- `05_Development/DevTools/sync_triage_rule_registry.py`
- New test files only:
  - `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/rules/LegacyGovernanceCompatibilityTest.java`
  - `05_Development/CareBridgeAITriageService/tests/test_legacy_governance_compatibility.py`

## Forbidden Paths

- Anything under `CareBridgeMobileApp/` (Flutter)
- Anything under `db/migration/` — **no migration in this task**
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/service/`,
  `policy/`, `engine/`, `entity/` (V1 code)
- `05_Development/Contracts/triage/triage_rule_parity_vectors_v2.json` — **do not touch the vectors**
- Any `*.sha256` file — those are generated; run the sync tool instead
- Any production config, `.env`, or secret file
- Any file already dirty in the worktree that is unrelated to this task

---

## Required Behaviour (invariants)

1. Every rule in the canonical registry gains `releaseStatus` with value `ACTIVE`
   (`DRAFT|ACTIVE|DISABLED|RETIRED` is the allowed set).
2. The literal string `APPROVED` **must not appear as a rule status value** in the canonical
   registry after this change, nor in the generated runtime copies.
3. Each rule keeps/carries explicitly: `internalReviewStatus` (`NOT_REVIEWED|DEV_REVIEWED`),
   `sourceVerificationStatus` (`PENDING|SOURCE_VERIFIED|SOURCE_CHANGED|BROKEN|UNRESOLVED_SOURCE`),
   `clinicalValidationStatus` (`NOT_CLINICALLY_VALIDATED`).
   **Do not hand-edit `sourceVerificationStatus` values** — the sync tool derives them (D-010).
4. Both loaders (Java + Python) accept a legacy artifact carrying `status: "APPROVED"` and map
   it to `releaseStatus=ACTIVE`, emitting a deprecation warning/log. Legacy `DRAFT`/`RETIRED`
   remain skippable exactly as today.
5. Legacy `APPROVED` must **never** be interpreted as clinical validation: the mapping sets
   `releaseStatus` only, and `clinicalValidationStatus` stays `NOT_CLINICALLY_VALIDATED`.
6. Loaders remain **fail-closed**: an `ACTIVE` rule that fails validation still raises
   (`RegistryIntegrityException` / `RegistryIntegrityError`); no warn-and-continue.
7. Bump `rulesetVersion` `2.1.0` → `2.2.0` in the canonical registry.
8. **No clinical field changes**: `condition`, `outcome`, `priority`, `decisionOrder`,
   `stopOnMatch`, `reasonCode`, `actionCode`, `stages`, `requiredFields`, `questionIds`,
   `exclusionPredicates` must be byte-identical for all 10 rules.
9. Do not weaken any existing test. Do not edit expected values in existing tests.

## Acceptance Criteria

- `python -c "import json;d=json.load(open('05_Development/Contracts/triage/triage_rules_v2.json',encoding='utf-8'));assert all(r['releaseStatus']=='ACTIVE' for r in d['rules']);assert not any(r.get('status')=='APPROVED' for r in d['rules']);assert d['rulesetVersion']=='2.2.0';print('OK')"` prints `OK`.
- Java: loading a synthetic legacy artifact with `status:"APPROVED"` yields a registry with 10
  rules and logs a deprecation warning; loading one with an invalid ACTIVE rule still throws.
- Python: same two behaviours.
- `TriageRuleParityV2Test` still passes **54/54** with the vectors unchanged.
- `pytest tests/test_rule_registry_parity_v2.py -q` still passes **87**.
- `python 05_Development/DevTools/sync_triage_rule_registry.py --check` exits 0 after you run
  the non-`--check` sync once to regenerate copies.

## Tests to run (report exact counts)

```
python 05_Development/DevTools/sync_triage_rule_registry.py
python 05_Development/DevTools/sync_triage_rule_registry.py --check
cd 05_Development/CareBridgeAITriageService && python -m pytest -q
cd 05_Development/CareBridgeAPI && ./mvnw -B test -Dtest="TriageRuleParityV2Test,LegacyGovernanceCompatibilityTest" -DfailIfNoSpecifiedTests=false
```

## Output Contract

- Summary ≤10 lines.
- Files changed: added / modified / deleted.
- **Unified diff only** — never full file contents. If the diff exceeds ~350 lines, split by file.
- Test summary: command, exit code, passed/failed/errors/skipped, duration. **No full logs.**
  On failure: failing test name + ≤50 lines of root-cause stack.
- Blockers ≤10 lines. Assumptions: only those genuinely needing review.
- **Do not commit. Do not push. Do not start any other task.**
