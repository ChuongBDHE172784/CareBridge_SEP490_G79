"""Run the deterministic, non-clinical Triage V2 regression evaluation corpus."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from time import perf_counter


SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.context import CareStage
from app.questions.catalog import CATALOG
from app.rules.registry import load_dataset_requirements
from app.triage_v2.graph import build_triage_v2_graph, graph_config
from app.triage_v2.state import MAXIMUM_QUESTION_ROUNDS, create_initial_state


DEFAULT_CASES = SERVICE_ROOT / "tests" / "data" / "triage_v2_evaluation_cases.json"
REPO_ROOT = SERVICE_ROOT.parents[1]

REQUIRED_EVALUATION_COVERAGE = {
    "GLOBAL_RED": {
        "caseIds": ["global_red_breathing", "global_red_seizure"],
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_global_safety_gate.py::test_explicit_global_danger_is_red_before_target_resolution",
        ],
    },
    "STAGE_RED": {"caseIds": ["stage_red_pregnancy_bleeding", "stage_red_postpartum_chest"]},
    "PENDING_RED": {"caseIds": ["pending_red_round_three"]},
    "SCOPE_OOS": {"caseIds": ["positive_wrist_oos", "unknown_complaint"]},
    "TARGET_ENTITY": {
        "caseIds": ["mother_colloquial_diarrhea", "baby_colloquial_diarrhea", "ambiguous_fever"],
    },
    "INTENT": {"caseIds": ["general_information_no_color"]},
    "WRONG_ENTITY_QUESTION": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_entity_stage_validator.py::test_mother_only_question_is_removed_from_baby_session",
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_entity_stage_validator.py::test_baby_only_question_is_removed_from_mother_session",
        ],
    },
    "CONTEXT_CONFLICT": {
        "caseIds": ["mother_and_baby_fever"],
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_stage_context_resolver.py::test_invalid_entity_stage_is_a_conflict_not_a_silent_rewrite",
        ],
    },
    "EXTRACTION": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_extraction.py::test_grounded_canonical_signal_is_accepted_but_deterministic_engine_decides_outcome",
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_extraction.py::test_hallucinated_span_or_unknown_code_is_rejected",
        ],
    },
    "PROMPT_INJECTION": {
        "caseIds": ["prompt_injection"],
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_extraction.py::test_prompt_injection_is_plain_text_and_only_creates_a_warning",
        ],
    },
    "CITATION": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_evidence_retrieval.py::test_pending_changed_broken_or_legacy_approved_never_becomes_citation",
            "05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageV2SessionServiceTest.java::unverifiedCitationIsRejectedWithoutDowngradingOutcome",
        ],
    },
    "FALLBACK": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_graph.py::test_registry_unavailable_is_rendered_as_controlled_non_green",
            "05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageV2SessionServiceTest.java::pythonFailureWithoutDangerNeverBecomesGreenOrOutOfScope",
        ],
    },
    "TIMEOUT": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_extraction.py::test_timeout_invalid_json_empty_and_unsupported_enum_fail_closed",
        ],
    },
    "DUPLICATE_STALE": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_v2_graph.py::test_duplicate_and_stale_requests_do_not_advance_question_round",
            "05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/TriageV2SessionServiceTest.java::staleContinueIsRejectedBeforeWorkflowExecution",
        ],
    },
    "VIETNAMESE_COLLOQUIAL": {
        "caseIds": ["mother_colloquial_diarrhea", "baby_colloquial_diarrhea"],
    },
}

REQUIRED_METRIC_COVERAGE = {
    "redFalseNegativeCountRate": {"status": "MEASURED", "fields": ["redFalseNegativeCount", "redFalseNegativeRate"]},
    "wrongEntityRate": {"status": "MEASURED", "fields": ["wrongEntityCount", "wrongEntityRate"]},
    "wrongQuestionRate": {"status": "MEASURED", "fields": ["wrongQuestionCount", "wrongQuestionRate"]},
    "oosFalsePositive": {"status": "MEASURED", "fields": ["oosFalsePositiveCount", "oosFalsePositiveRate"]},
    "unsupportedGreen": {"status": "MEASURED", "fields": ["unsupportedGreenCount"]},
    "citationPrecision": {"status": "BLOCKED_BY_SOURCE_COVERAGE", "fields": ["citationPrecision"]},
    "brokenUnverifiedLink": {"status": "COMPONENT_TESTED", "fields": ["brokenOrUnverifiedLinkExposureCount"]},
    "schemaFailure": {"status": "COMPONENT_TESTED", "fields": ["schemaFailureCount"]},
    "latency": {"status": "MEASURED", "fields": ["averageLatencyMs"]},
    "questionCount": {"status": "MEASURED", "fields": ["averageQuestionCount"]},
    "maxRoundRoute": {"status": "MEASURED", "fields": ["maxRoundRouteCount"]},
    "fallbackCount": {"status": "COMPONENT_TESTED", "fields": ["fallbackCount"]},
    "parityMismatch": {"status": "COMPONENT_TESTED", "fields": ["parityMismatchCount"]},
}


def evaluate(cases_path: Path) -> dict[str, object]:
    cases = json.loads(cases_path.read_text(encoding="utf-8"))
    coverage = _build_coverage(cases)
    results: list[dict[str, object]] = []
    global_codes = load_dataset_requirements()["globalSafety"]["requiredSignalCodes"]
    for index, case in enumerate(cases):
        state = create_initial_state(
            session_id=f"evaluation-session-{index:04d}",
            state_version=1,
            request_id=f"evaluation-request-{index:04d}",
            message_id=f"evaluation-message-{index:04d}",
            latest_user_message=case["message"],
        )
        if case.get("completeGlobal"):
            for code in global_codes:
                state["signals"][code] = {"presence": "ABSENT"}
        state["signals"].update(case.get("signals", {}))
        for key in ("possiblePregnancy", "gestationalWeek", "postpartumDay", "questionRound"):
            if key in case:
                state[key] = case[key]
        if "stage" in case:
            state["stage"] = CareStage(case["stage"])
        started = perf_counter()
        result = build_triage_v2_graph().invoke(state, graph_config(state["sessionId"]))
        latency_ms = round((perf_counter() - started) * 1_000, 3)
        actual_outcome = _value(result.get("triageOutcome"))
        actual_target = _value(result.get("targetEntity"))
        question_ids = result.get("plannedQuestionIds", [])
        wrong_questions = sum(
            1 for question_id in question_ids
            if question_id not in CATALOG or (
                actual_target in {"MOTHER", "BABY"}
                and CATALOG[question_id].target_entities
                and actual_target not in {_value(item) for item in CATALOG[question_id].target_entities}
            )
        )
        specified_assertions = sum(
            key in case for key in ("expectedOutcome", "expectedTarget", "expectedStop", "forbidOutcome")
        )
        checks = {
            "hasExpectation": specified_assertions > 0,
            "outcome": case.get("expectedOutcome") in (None, actual_outcome),
            "target": case.get("expectedTarget") in (None, actual_target),
            "stop": case.get("expectedStop") in (None, result.get("stopConversation")),
            "forbiddenOutcome": case.get("forbidOutcome") != actual_outcome,
        }
        results.append({
            "id": case["id"], "category": case["category"], "passed": all(checks.values()),
            "inputMode": "STRUCTURED_SIGNALS" if case.get("signals") else "TEXT_ONLY",
            "checks": checks, "actualOutcome": actual_outcome, "actualTarget": actual_target,
            "stop": result.get("stopConversation"),
            "questionCount": len(question_ids), "wrongQuestionCount": wrong_questions,
            "questionRound": result.get("questionRound"),
            "completionReason": _value(result.get("completionReason")),
            "latencyMs": latency_ms,
        })

    def category_recall(category: str, wanted: str) -> dict[str, object]:
        group = [item for item in results if item["category"] == category]
        hits = sum(item["actualOutcome"] == wanted for item in group)
        return {"hits": hits, "total": len(group), "rate": hits / len(group) if group else None}

    passed = sum(bool(item["passed"]) for item in results)
    red_gold = [item for item in results if item["category"] in {"GLOBAL_RED", "STAGE_RED"}]
    red_hits = sum(item["actualOutcome"] == "RED" for item in red_gold)
    target_gold = [item for item in results if item["category"] in {"TARGET", "TARGET_CONFLICT"}]
    target_hits = sum(item["checks"]["target"] for item in target_gold)
    non_oos = [item for item in results if item["category"] != "OOS"]
    question_total = sum(item["questionCount"] for item in results)
    wrong_question_total = sum(item["wrongQuestionCount"] for item in results)
    oos_false_positives = sum(item["actualOutcome"] == "OUT_OF_SCOPE" for item in non_oos)
    metrics = {
        "redRecall": red_hits / len(red_gold) if red_gold else None,
        "redFalseNegativeCount": len(red_gold) - red_hits,
        "redFalseNegativeRate": (len(red_gold) - red_hits) / len(red_gold) if red_gold else None,
        "globalRedRecall": category_recall("GLOBAL_RED", "RED"),
        "stageRedRecall": category_recall("STAGE_RED", "RED"),
        "targetAccuracy": target_hits / len(target_gold) if target_gold else None,
        "wrongEntityCount": len(target_gold) - target_hits,
        "wrongEntityRate": (len(target_gold) - target_hits) / len(target_gold) if target_gold else None,
        "wrongQuestionCount": wrong_question_total,
        "wrongQuestionRate": wrong_question_total / question_total if question_total else 0,
        "oosFalsePositiveCount": oos_false_positives,
        "oosFalsePositiveRate": oos_false_positives / len(non_oos) if non_oos else None,
        "unsupportedGreenCount": sum(item["actualOutcome"] == "GREEN" for item in results),
        "averageLatencyMs": round(sum(item["latencyMs"] for item in results) / len(results), 3) if results else None,
        "averageQuestionCount": round(sum(item["questionCount"] for item in results) / len(results), 3) if results else None,
        "maxRoundRouteCount": sum(
            item["questionRound"] == MAXIMUM_QUESTION_ROUNDS
            and item["actualOutcome"] == "NEEDS_MORE_INFO"
            and item["stop"] is True
            for item in results
        ),
        "citationPrecision": None,
        "citationPrecisionReason": "No SOURCE_VERIFIED corpus documents; not measurable without fabrication",
        "brokenOrUnverifiedLinkExposureCount": None,
        "brokenOrUnverifiedLinkExposureReason": "Enforced by component trust-boundary tests; no verified evaluation corpus",
        "schemaFailureCount": None,
        "schemaFailureReason": "Measured by extraction/API component telemetry, not the deterministic graph corpus",
        "fallbackCount": None,
        "fallbackCountReason": "Measured by runtime component telemetry, not the deterministic graph corpus",
        "parityMismatchCount": None,
        "parityMismatchReason": "Measured by Java shadow metrics; shadow runtime remains disabled",
    }
    coverage["requiredMetrics"] = REQUIRED_METRIC_COVERAGE
    coverage["allRequiredMetricFieldsReported"] = all(
        all(field in metrics for field in item["fields"])
        for item in REQUIRED_METRIC_COVERAGE.values()
    )
    return {
        # The full canonical metadata block, verbatim. A partial block invites a reader to
        # assume the missing fields say something more favourable than they do.
        "governance": {
            "projectType": "ACADEMIC_COMMUNITY_PROJECT",
            "intendedUse": "INFORMATIONAL_RISK_ORIENTATION",
            "internalReviewStatus": "DEV_REVIEWED",
            "clinicalValidationStatus": "NOT_CLINICALLY_VALIDATED",
            "externalClinicalSignOff": "NONE",
        },
        "total": len(results), "passed": passed, "failed": len(results) - passed,
        "metrics": metrics,
        "coverage": coverage,
        "results": results,
    }


def _build_coverage(cases: list[dict[str, object]]) -> dict[str, object]:
    case_ids = {case.get("id") for case in cases}
    categories: dict[str, object] = {}
    for name, requirement in REQUIRED_EVALUATION_COVERAGE.items():
        wanted_cases = requirement.get("caseIds", [])
        wanted_tests = requirement.get("tests", [])
        missing_cases = [case_id for case_id in wanted_cases if case_id not in case_ids]
        missing_tests = [reference for reference in wanted_tests if not _test_evidence_exists(reference)]
        categories[name] = {
            "caseIds": wanted_cases,
            "testEvidence": wanted_tests,
            "covered": bool(wanted_cases or wanted_tests) and not missing_cases and not missing_tests,
            "missingCaseIds": missing_cases,
            "missingTestEvidence": missing_tests,
        }
    return {
        "requiredEvaluationCategories": categories,
        "allRequiredCategoriesCovered": all(item["covered"] for item in categories.values()),
    }


def _test_evidence_exists(reference: str) -> bool:
    relative_path, separator, symbol = reference.partition("::")
    if not separator or not symbol:
        return False
    path = REPO_ROOT / relative_path
    if not path.is_file():
        return False
    source = path.read_text(encoding="utf-8")
    return re.search(rf"\b(?:def|void)\s+{re.escape(symbol)}\b", source) is not None


def _value(value: object) -> object:
    return getattr(value, "value", value)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    report = evaluate(args.cases)
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    return 0 if (
        report["failed"] == 0
        and report["coverage"]["allRequiredCategoriesCovered"]
        and report["coverage"]["allRequiredMetricFieldsReported"]
    ) else 1


if __name__ == "__main__":
    raise SystemExit(main())
