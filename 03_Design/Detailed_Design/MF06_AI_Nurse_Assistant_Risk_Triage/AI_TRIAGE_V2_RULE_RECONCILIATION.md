# AI Triage V2 Step 1 Rule Reconciliation & Zero-Trust Engine Report

> **Positioning.** CareBridge is an ACADEMIC_COMMUNITY_PROJECT providing INFORMATIONAL_RISK_ORIENTATION only. It is not a hospital, clinic, diagnostic service or WHO-certified tool. No clinician participated in building or approving these rules. `clinicalValidationStatus=NOT_CLINICALLY_VALIDATED`, `externalClinicalSignOff=NONE`, `internalReviewStatus=DEV_REVIEWED`. Rules were mapped from publicly available medical guidance; the publishers cited have not reviewed or endorsed CareBridge.


**Date**: 2026-08-05  
**Scope**: Step 1 — BƯỚC AN TOÀN NỀN TẢNG (Zero-Trust Dataset Calculator, Scope Status Calculator, Pending Risk Model, Exclusion Audit Trace, Parity Governance)  
**Status**: IMPLEMENTED & SYNCHRONISED across both runtimes (Python `CareBridgeAITriageService`, Java `CareBridgeAPI`). `releaseStatus=ACTIVE`, `internalReviewStatus=DEV_REVIEWED`, `sourceVerificationStatus=PENDING`, `clinicalValidationStatus=NOT_CLINICALLY_VALIDATED`, `externalClinicalSignOff=NONE`. "Synchronised" means the two engines agree — it is not an approval.  

---

## Executive Summary

As part of the independent technical and clinical safety audit for the CareBridge AI Reproductive Health Triage Assistant V2, **Step 1 (BƯỚC AN TOÀN NỀN TẢNG)** has been fully designed, implemented, and verified across both backend runtimes (Python FastAPI + Java Spring Boot).

This step replaces caller-asserted assumptions (`minimumDatasetComplete`, `reproductiveRelevance`) with deterministic **Zero-Trust engine calculators**, preventing false-negative clinical bypasses (e.g., Ruptured Ectopic Pregnancy, Severe Preeclampsia, Neonatal Sepsis, Pulmonary Embolism) from being trapped in `NEEDS_MORE_INFO` or misclassified as `OUT_OF_SCOPE` or `GREEN`.

---

## 1. Zero-Trust Dataset Calculator

The engine no longer trusts `minimumDatasetComplete` or `reproductiveRelevance` passed by external callers, frontend clients, or LLM extractors.

### 1.1 Dataset Status Types & Calculations
- **`SafetyScreenStatus`**:
  - `COMPLETE`: All 8 global safety signal leaf codes (`ALTERED_CONSCIOUSNESS`, `SEIZURE`, `SEVERE_BREATHING_DIFFICULTY`, `CYANOSIS`, `SELF_HARM_IDEATION`, `SELF_HARM_INTENT_OR_PLAN`, `HARM_TO_BABY_IDEATION`, `CANNOT_ENSURE_OWN_SAFETY`) are explicitly resolved to `PRESENT`, `ABSENT`, or `NOT_APPLICABLE`.
  - `INCOMPLETE`: Any global safety signal is `UNKNOWN` or unasked.
  - `CONFLICTED`: Contradictory signals detected across turns.
- **`ContextDatasetStatus`**:
  - `COMPLETE`: Required stage context fields (e.g. `gestational_week` for PREGNANCY, `postpartum_day` and `delivery_method` for POSTPARTUM, `possible_pregnancy` for POSSIBLE_PREGNANCY) are present and resolved.
  - `INCOMPLETE`: Required context field missing or `UNKNOWN`.
  - `CONFLICTED`: Stage/context conflict (e.g., `stage=PREGNANCY` with `postpartum_day=10`).
- **`GreenEligibilityDatasetStatus`**:
  - Requires `SafetyScreenStatus == COMPLETE` AND `ContextDatasetStatus == COMPLETE` AND zero active green safety blockers.

---

## 2. Scope Status Calculator

The engine computes `ScopeStatus` deterministically:
- `IN_SCOPE`: Explicit reproductive stage or reproductive signals/context present.
- `POSSIBLY_IN_SCOPE`: Ambiguous context (e.g., `possible_pregnancy == UNKNOWN`).
- `CONFIRMED_OUT_OF_SCOPE`: Allowed **ONLY IF**:
  1. `SafetyScreenStatus == COMPLETE` (All 8 global safety signals explicitly `ABSENT`).
  2. No global RED policy hits.
  3. No clinical RED, pending RED, or YELLOW matches.
  4. Zero positive reproductive context/evidence (`gestational_week`, `postpartum_day`, `possible_pregnancy == YES`).
  5. `context.possible_pregnancy != "UNKNOWN"`.
- `CONFLICTED`: Caller asserts `reproductiveRelevance = false` but reproductive context/evidence is present.

---

## 3. Pending Risk Model & Escalation

When clinical data is incomplete, unresolved risk conditions are mapped to pending statuses with strict precedence:

| Precedence | Pending Risk Status | Trigger Condition | Round Exhaustion Action (`questionRound >= 3`) |
|---|---|---|---|
| **P1** | `UNRESOLVED_RED_CONDITION` | Partial match on clinical RED rule (e.g. `SEVERE_HEADACHE` present, `VISUAL_DISTURBANCE` unknown) | `NEEDS_MORE_INFO` + `ROUTE_TO_HEALTHCARE_WORKER` + `stopConversation = true` |
| **P2** | `UNRESOLVED_GLOBAL_SAFETY_SCREEN` | Global safety screen incomplete | `NEEDS_MORE_INFO` + `ROUTE_TO_HEALTHCARE_WORKER` + `stopConversation = true` |
| **P3** | `UNRESOLVED_SAFETY_BLOCKER` | Active green safety blocker (e.g. severe headache unpaired) | `NEEDS_MORE_INFO` + `ASK_CLARIFYING_QUESTIONS` / `ROUTE_TO_HEALTHCARE_WORKER` |
| **P4** | `UNRESOLVED_CONTEXT` | Required stage context field missing | `NEEDS_MORE_INFO` + `ASK_CLARIFYING_QUESTIONS` |

---

## 4. Exclusion Audit Trace

When a rule condition evaluates to `TRUE` but is suppressed by `exclusionPredicates` (e.g. `bleeding_amount == UNKNOWN` or `CONFLICTED`), the rule evaluation trace logs:
- `role`: `SUPPRESSED_BY_EXCLUSION`
- `suppressionReason`: `DATA_AMBIGUITY` (for `UNKNOWN`) or `DATA_CONFLICT` (for `CONFLICTED`)
- `missingFields`: List of missing fields causing suppression (e.g., `["bleeding_amount"]`).

---

## 5. Parity Vector Governance & Test Verification

The canonical parity vector suite in `05_Development/Contracts/triage/triage_rule_parity_vectors_v2.json` holds **41 vector records, all active**, covering the Zero-Trust edge cases. Both runtimes execute all 41; see `PARITY_VECTOR_RECONCILIATION.md` for how each of the numbers below is counted:

- **Python** (`pytest tests/test_rule_registry_parity_v2.py`): 87 tests = 41 parity invocations + 46 others.
- **Java** (`mvnw test -Dtest="TriageRuleParityV2Test"`): 54 tests = 41 parity invocations + 13 others.
- **Known limitation:** the four mandatory fields are asserted exactly; the optional expectation lists (`reasonCodes`, `greenBlockedBy`, `pendingRedRuleIds`, `unresolvedSignals`, `suppressedRuleIds`) are asserted by containment only, so an extra entry on one side would not fail parity. Tracked as PHASE0-PARITY-001.

---

## 6. Shared Registry Integrity Checksums

All canonical artifacts in `05_Development/Contracts/triage/` are synced to both runtimes and guarded by SHA-256 checksums recorded in `artifact_integrity_manifest.json` (generated by `DevTools/sync_triage_rule_registry.py`). The former `approval_manifest.json` was **deleted**: its name and its `PO_CONFIRMED_DOCTOR_REVIEW` field implied a clinical sign-off that never happened. Provenance now lives in `source_verification_manifest.json` and engineering review in `internal_rule_review_manifest.json`. Integrity proves only that a file matches its checksum — never that its content is medically correct.

> Digests below were captured at ruleset 2.1.0 and are **stale after the 2.2.0 governance migration**. Read the current values from `artifact_integrity_manifest.json`; do not trust this list.
- `triage_rules_v2.json`: `7aaac224c8d88aaebbb2c1696c8d915f5d09525f064621d55c6995d158259365`
- `triage_rule_parity_vectors_v2.json`: `78b66f5df8dcce89cb796123a1a1f0bd6dc90d33e5c942ee4ed503aa32f86641`
- `dataset_requirements_v1.json`: `e39665bc7ecdd473fa58a6ae94819d9b4c09d5718a221f71dfb34255be5ea3ee`
- `draft_safety_disposition_matrix_v1.json`: `dfc1ee91ac797a7bbdf5cfcf6891ebfcf38b1d9bf5b3ef39df8d06d4e5dbeed4`
