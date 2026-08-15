"""Run the deterministic, non-clinical Triage V2 regression evaluation corpus."""

from __future__ import annotations

import argparse
from collections import Counter
from collections.abc import Callable, Iterable
from copy import deepcopy
from hashlib import sha256
import json
import re
import sys
from pathlib import Path
from time import perf_counter
from uuid import NAMESPACE_URL, uuid5

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.context import CareStage
from app.config import GeminiSettings
from app.gemini_client import GeminiClient
from app.questions.catalog import CATALOG
from app.rules.registry import get_registry, load_dataset_requirements
from app.triage.api import (
    TriageTurnRequest,
    _merge_observations,
    _merge_reported_measurements,
    _turn_state,
)
from app.triage.deterministic_measurements import extract_reported_measurements
from app.triage.deterministic_signals import detect_danger_signals, merge_as_floor
from app.triage.extraction import extract_and_validate
from app.triage.graph import build_triage_graph, graph_config
from app.triage.global_safety_gate import global_safety_gate
from app.triage.state import MAXIMUM_QUESTION_ROUNDS, create_initial_state


DEFAULT_CASES = SERVICE_ROOT / "tests" / "data" / "triage_v2_evaluation_cases.json"
DEFAULT_VAGUE_CASES = SERVICE_ROOT / "tests" / "data" / "triage_v2_vague_corpus_v1.json"
REPO_ROOT = SERVICE_ROOT.parents[1]

VAGUE_GROUP_QUOTAS = {
    "VAGUE": 55,
    "MATERNAL": 32,
    "PEDIATRIC": 32,
    "MULTI_SYMPTOM_CONFLICT": 21,
    "GEMINI_FAILURE": 20,
}
VAGUE_CROSS_TAG_MINIMUMS = {
    "NEGATION": 10,
    "NO_DEVICE_OR_UNAWARE": 10,
    "MULTI_TURN": 14,
    "HISTORY_CURRENT": 8,
}
GEMINI_FAULT_MODES = {"OFF", "TIMEOUT", "429", "5XX"}
KNOWN_DEFECT_CODES = {"F-P1-1", "F-P1-2", "F-P1-4", "F-P2-4", "F-COV-7", "F-PROD-1"}
KNOWN_DEFECT_DIMENSIONS = {
    "F-P1-1": {"firstQuestion", "focusedQuestion", "pendingRule", "finiteTermination"},
    "F-P1-2": {"repeatedQuestion", "finiteTermination", "disposition"},
    "F-P1-4": {"firstQuestion", "focusedQuestion", "pendingRule", "disposition"},
    "F-P2-4": {"focusedQuestion", "repeatedQuestion", "finiteTermination", "disposition"},
    "F-COV-7": {"firstQuestion", "focusedQuestion", "disposition", "reasonCodes", "pendingRule"},
    "F-PROD-1": {"focusedQuestion", "pendingRule", "disposition", "reasonCodes"},
}
_CASE_FIELDS = {
    "id", "group", "synthetic", "tags", "profile", "message", "turns", "geminiMode",
    "expectedTarget", "expectedStage", "expectedSafetyQuestions",
    "acceptableFocusedQuestions", "acceptableFirstQuestions", "forbiddenQuestions",
    "maxTurns", "allowedDispositions", "expectedReasonCodes", "expectedPendingRule",
    "rationale", "clinicalReviewStatus", "knownDefect",
}
#: Only for conversations whose subject or stage legitimately changes mid-session. One entry per
#: authored turn, oracle-stated. Absent on the vast majority of cases, where the single
#: expectedTarget/expectedStage covers the whole conversation.
_OPTIONAL_CASE_FIELDS = {"expectedTargetByTurn", "expectedStageByTurn"}
_TURN_FIELDS = {"message", "signals", "measurements", "answeredQuestionIds"}
_DISPOSITIONS = {"RED", "YELLOW", "GREEN", "NEEDS_MORE_INFO", "OUT_OF_SCOPE"}

REQUIRED_EVALUATION_COVERAGE = {
    "GLOBAL_RED": {
        "caseIds": ["global_red_breathing", "global_red_seizure"],
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_global_safety_gate.py::test_explicit_global_danger_is_red_before_target_resolution",
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
            "05_Development/CareBridgeAITriageService/tests/test_triage_entity_stage_validator.py::test_mother_only_question_is_removed_from_baby_session",
            "05_Development/CareBridgeAITriageService/tests/test_triage_entity_stage_validator.py::test_baby_only_question_is_removed_from_mother_session",
        ],
    },
    "CONTEXT_CONFLICT": {
        "caseIds": ["mother_and_baby_fever"],
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_stage_context_resolver.py::test_invalid_entity_stage_is_a_conflict_not_a_silent_rewrite",
        ],
    },
    "EXTRACTION": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_extraction.py::test_grounded_canonical_signal_is_accepted_but_deterministic_engine_decides_outcome",
            "05_Development/CareBridgeAITriageService/tests/test_triage_extraction.py::test_hallucinated_span_or_unknown_code_is_rejected",
        ],
    },
    "PROMPT_INJECTION": {
        "caseIds": ["prompt_injection"],
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_extraction.py::test_prompt_injection_is_plain_text_and_only_creates_a_warning",
        ],
    },
    "CITATION": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_evidence_retrieval.py::test_pending_changed_broken_or_legacy_approved_never_becomes_citation",
            "05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/CanonicalTriageSessionServiceTest.java::unverifiedCitationIsRejectedWithoutDowngradingOutcome",
        ],
    },
    "FALLBACK": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_graph.py::test_registry_unavailable_is_rendered_as_controlled_non_green",
            "05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/CanonicalTriageSessionServiceTest.java::pythonFailureWithoutDangerNeverBecomesGreenOrOutOfScope",
        ],
    },
    "TIMEOUT": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_extraction.py::test_timeout_invalid_json_empty_and_unsupported_enum_fail_closed",
        ],
    },
    "DUPLICATE_STALE": {
        "tests": [
            "05_Development/CareBridgeAITriageService/tests/test_triage_graph.py::test_duplicate_and_stale_requests_do_not_advance_question_round",
            "05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/CanonicalTriageSessionServiceTest.java::staleContinueIsRejectedBeforeWorkflowExecution",
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
        result = build_triage_graph().invoke(state, graph_config(state["sessionId"]))
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


class CorpusValidationError(ValueError):
    """One or more authoring invariants make a baseline corpus unsafe to execute."""

    def __init__(self, errors: list[str]):
        self.errors = errors
        super().__init__("invalid vague baseline corpus:\n- " + "\n- ".join(errors))


def _derive_submitted_option_codes(turn: object, *, label: str) -> list[str]:
    """Recover answer options solely from canonical question-answer provenance.

    The Phase 1 corpus predates the explicit ``submittedOptionCodes`` transport field, but its
    structured signals already preserve the canonical Java mapper's provenance. Each answered
    question must therefore resolve to exactly one ``sourceOptionCode`` among observations whose
    provenance is ``QUESTION_ANSWER`` and whose ``sourceQuestionId`` matches that question.
    Iterating the authored question IDs (rather than the signal mapping) preserves pair order.
    """

    if type(turn) is not dict:
        raise CorpusValidationError([f"{label} must be an object"])
    answered_ids = turn.get("answeredQuestionIds", [])
    signals = turn.get("signals", {})
    if type(answered_ids) is not list or any(type(item) is not str for item in answered_ids):
        raise CorpusValidationError([f"{label}.answeredQuestionIds must be a string array"])
    if type(signals) is not dict:
        raise CorpusValidationError([f"{label}.signals must be an object"])

    submitted: list[str] = []
    errors: list[str] = []
    for answer_index, question_id in enumerate(answered_ids):
        options: set[str] = set()
        for signal_value in signals.values():
            observations = signal_value if type(signal_value) is list else [signal_value]
            for observation in observations:
                if type(observation) is not dict:
                    continue
                if (
                    observation.get("provenance") == "QUESTION_ANSWER"
                    and observation.get("sourceQuestionId") == question_id
                ):
                    option_code = observation.get("sourceOptionCode")
                    if type(option_code) is str and option_code:
                        options.add(option_code)
        provenance_path = f"{label}.answeredQuestionIds[{answer_index}] ({question_id})"
        if not options:
            errors.append(
                f"{provenance_path} has no QUESTION_ANSWER sourceOptionCode "
                "in matching canonical signal provenance"
            )
        elif len(options) > 1:
            errors.append(
                f"{provenance_path} has conflicting QUESTION_ANSWER sourceOptionCodes: "
                f"{sorted(options)}"
            )
        else:
            option_code = next(iter(options))
            question = CATALOG.get(question_id)
            valid_options = {
                option.option_code for option in question.options
            } if question is not None else set()
            if option_code not in valid_options:
                errors.append(
                    f"{provenance_path} sourceOptionCode {option_code} does not belong to "
                    f"{question_id}"
                )
            else:
                submitted.append(option_code)
    if errors:
        raise CorpusValidationError(errors)
    return submitted


class LocalGeminiFaultFixture:
    """Exercise GeminiClient's real fail-closed transport handling with an injected local SDK."""

    def __init__(self, mode: str):
        if mode not in GEMINI_FAULT_MODES:
            raise ValueError(f"unsupported local Gemini mode: {mode}")
        self.mode = mode
        self.call_count = 0
        self.network_client_created = False
        self.observed_failure: str | None = "DISABLED" if mode == "OFF" else None
        settings = GeminiSettings(
            enabled=mode != "OFF",
            api_key="offline-evaluation-key" if mode != "OFF" else None,
            model="offline-evaluation-model",
            timeout_seconds=0.5,
            max_retries=0,
            temperature=0.0,
        )
        self._client = GeminiClient(settings, _LocalFaultSdk(self))

    def extract_triage(self, *, text: str, deadline: float | None = None) -> None:
        return self._client.extract_triage(text=text, deadline=deadline)


class _LocalProviderError(Exception):
    def __init__(self, code: int):
        super().__init__(f"offline provider failure {code}")
        self.code = code


class _LocalFaultModels:
    def __init__(self, fixture: LocalGeminiFaultFixture):
        self.fixture = fixture

    def generate_content(self, **_kwargs: object) -> None:
        self.fixture.call_count += 1
        if self.fixture.mode == "TIMEOUT":
            self.fixture.observed_failure = "TIMEOUT"
            raise TimeoutError("offline timeout fixture")
        if self.fixture.mode == "429":
            self.fixture.observed_failure = "HTTP_429"
            raise _LocalProviderError(429)
        if self.fixture.mode == "5XX":
            self.fixture.observed_failure = "HTTP_503"
            raise _LocalProviderError(503)
        raise AssertionError("OFF mode must not invoke the injected SDK")


class _LocalFaultSdk:
    def __init__(self, fixture: LocalGeminiFaultFixture):
        self.models = _LocalFaultModels(fixture)


def validate_vague_corpus(cases_path: Path) -> list[dict[str, object]]:
    """Load the pre-authored corpus and reject any schema, quota, or governance violation."""

    try:
        payload = json.loads(cases_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as failure:
        raise CorpusValidationError([f"cannot read JSON: {failure}"]) from failure
    if type(payload) is not list:
        raise CorpusValidationError(["root must be a JSON array"])
    validate_vague_cases(payload)
    return deepcopy(payload)


def validate_vague_cases(cases: list[object]) -> None:
    """Validate by unique case ID so duplicate rows can never satisfy a quota."""

    errors: list[str] = []
    seen: dict[str, dict[str, object]] = {}
    catalog_ids = set(CATALOG)
    registry = get_registry()
    signal_ids = set(registry.signal_display_text)
    rule_contract = json.loads(
        (REPO_ROOT / "05_Development" / "Contracts" / "triage" / "triage_rules_v2.json")
        .read_text(encoding="utf-8")
    )
    rule_ids = {item["ruleId"] for item in rule_contract["rules"]}
    reason_codes = {item["reasonCode"] for item in rule_contract["rules"]}
    stages = {item.value for item in CareStage}

    for index, raw_case in enumerate(cases):
        label = f"case[{index}]"
        if type(raw_case) is not dict:
            errors.append(f"{label} must be an object")
            continue
        case = raw_case
        case_id = case.get("id")
        if type(case_id) is not str or re.fullmatch(r"[a-z0-9_]{6,64}", case_id) is None:
            errors.append(f"{label}.id must match [a-z0-9_] and be 6-64 characters")
            case_id = f"index-{index}"
        label = str(case_id)
        if case_id in seen:
            errors.append(f"duplicate id: {case_id}")
        else:
            seen[str(case_id)] = case
        unknown_fields = set(case) - _CASE_FIELDS - _OPTIONAL_CASE_FIELDS
        missing_fields = _CASE_FIELDS - set(case)
        for field in _OPTIONAL_CASE_FIELDS & set(case):
            value = case[field]
            if type(value) is not list or not all(type(item) is str for item in value):
                errors.append(f"{label}.{field} must be a list of strings")
            elif len(value) != len(case.get("turns") or []) + 1:
                errors.append(f"{label}.{field} must have one entry per authored turn")
        if unknown_fields:
            errors.append(f"{label} unknown fields: {sorted(unknown_fields)}")
        if missing_fields:
            errors.append(f"{label} missing fields: {sorted(missing_fields)}")
        if case.get("group") not in VAGUE_GROUP_QUOTAS:
            errors.append(f"{label}.group is not a supported evaluation group")
        if case.get("synthetic") is not True:
            errors.append(f"{label}.synthetic must be true")
        if case.get("clinicalReviewStatus") != "PENDING":
            errors.append(f"{label}.clinicalReviewStatus must remain PENDING")
        known_defect = case.get("knownDefect")
        if known_defect is not None and known_defect not in KNOWN_DEFECT_CODES:
            errors.append(f"{label}.knownDefect is not approved")
        if type(case.get("message")) is not str or not str(case.get("message", "")).strip():
            errors.append(f"{label}.message must be non-empty")
        if type(case.get("rationale")) is not str or len(str(case.get("rationale", "")).strip()) < 40:
            errors.append(f"{label}.rationale must contain an authored Vietnamese explanation")
        if _contains_actual_field(case):
            errors.append(f"{label} must not contain actual output fields")

        tags = case.get("tags")
        if type(tags) is not list or any(type(tag) is not str for tag in tags):
            errors.append(f"{label}.tags must be a string array")
            tags = []
        elif len(tags) != len(set(tags)):
            errors.append(f"{label}.tags must not contain duplicates")
        if case.get("geminiMode") not in GEMINI_FAULT_MODES:
            errors.append(f"{label}.geminiMode must be OFF, TIMEOUT, 429, or 5XX")

        profile = case.get("profile")
        if type(profile) is not dict or set(profile) != {"selectedTarget", "journeyContext"}:
            errors.append(f"{label}.profile must contain only selectedTarget and journeyContext")
            profile = {}
        if profile.get("selectedTarget") not in {"MOTHER", "BABY", "UNKNOWN"}:
            errors.append(f"{label}.profile.selectedTarget is invalid")
        journey = profile.get("journeyContext")
        if type(journey) is not dict:
            errors.append(f"{label}.profile.journeyContext must be an object")
            journey = {}
        allowed_journey = {"stage", "possiblePregnancy", "gestationalWeek", "postpartumDay", "babyAgeMonths"}
        if set(journey) - allowed_journey:
            errors.append(f"{label}.profile.journeyContext has unsupported fields")
        if "stage" in journey and journey["stage"] not in stages:
            errors.append(f"{label}.profile.journeyContext.stage is invalid")
        if case.get("expectedTarget") not in {"MOTHER", "BABY", "UNKNOWN", "CONFLICTED"}:
            errors.append(f"{label}.expectedTarget is invalid")
        if case.get("expectedStage") not in stages:
            errors.append(f"{label}.expectedStage is invalid")

        for field in (
            "expectedSafetyQuestions", "acceptableFocusedQuestions", "acceptableFirstQuestions",
            "forbiddenQuestions",
        ):
            values = case.get(field)
            if type(values) is not list or any(value not in catalog_ids for value in values):
                errors.append(f"{label}.{field} must contain only canonical question IDs")
            elif len(values) != len(set(values)):
                errors.append(f"{label}.{field} must not contain duplicates")
        dispositions = case.get("allowedDispositions")
        if type(dispositions) is not list or not dispositions or not set(dispositions) <= _DISPOSITIONS:
            errors.append(f"{label}.allowedDispositions is invalid")
        expected_reasons = case.get("expectedReasonCodes")
        if type(expected_reasons) is not list or not set(expected_reasons) <= reason_codes:
            errors.append(f"{label}.expectedReasonCodes must come from canonical rules")
        pending_rule = case.get("expectedPendingRule")
        if pending_rule is not None and pending_rule not in rule_ids:
            errors.append(f"{label}.expectedPendingRule must be a canonical rule ID or null")

        max_turns = case.get("maxTurns")
        turns = case.get("turns")
        if type(max_turns) is not int or not 1 <= max_turns <= MAXIMUM_QUESTION_ROUNDS:
            errors.append(f"{label}.maxTurns must be 1-{MAXIMUM_QUESTION_ROUNDS}")
            max_turns = MAXIMUM_QUESTION_ROUNDS
        if type(turns) is not list:
            errors.append(f"{label}.turns must be an array")
            turns = []
        if 1 + len(turns) > max_turns:
            errors.append(f"{label} authors more turns than maxTurns")
        if "MULTI_TURN" in tags and 1 + len(turns) < 3:
            errors.append(f"{label} MULTI_TURN requires at least three authored turns")
        for turn_index, turn in enumerate(turns):
            turn_label = f"{label}.turns[{turn_index}]"
            if type(turn) is not dict:
                errors.append(f"{turn_label} must be an object")
                continue
            if set(turn) - _TURN_FIELDS or "message" not in turn:
                errors.append(f"{turn_label} has invalid fields")
            if type(turn.get("message")) is not str or not turn.get("message", "").strip():
                errors.append(f"{turn_label}.message must be non-empty")
            answered_ids = turn.get("answeredQuestionIds", [])
            answered_ids_valid = (
                type(answered_ids) is list
                and not any(type(item) is not str for item in answered_ids)
            )
            if not answered_ids_valid:
                errors.append(f"{turn_label}.answeredQuestionIds must be a string array")
            else:
                for question_id in answered_ids:
                    if question_id not in catalog_ids:
                        errors.append(f"{turn_label} has unknown answered question ID {question_id}")
            signals = turn.get("signals", {})
            signals_valid = type(signals) is dict and set(signals) <= signal_ids
            if not signals_valid:
                errors.append(f"{turn_label}.signals contains unknown canonical signals")
            if answered_ids_valid and signals_valid:
                try:
                    _derive_submitted_option_codes(turn, label=turn_label)
                except CorpusValidationError as failure:
                    errors.extend(failure.errors)
            if type(turn.get("measurements", {})) is not dict:
                errors.append(f"{turn_label}.measurements must be an object")

    unique_cases = list(seen.values())
    if len(unique_cases) != 160:
        errors.append(f"corpus must contain exactly 160 unique IDs; found {len(unique_cases)}")
    group_counts = Counter(str(case.get("group")) for case in unique_cases)
    for group, expected in VAGUE_GROUP_QUOTAS.items():
        if group_counts[group] != expected:
            errors.append(f"group {group} requires exactly {expected} unique cases; found {group_counts[group]}")
    tag_counts = Counter(tag for case in unique_cases for tag in case.get("tags", []))
    no_diacritic_or_typo = tag_counts["NO_DIACRITICS"] + tag_counts["TYPO"]
    if no_diacritic_or_typo < 15:
        errors.append(f"NO_DIACRITICS + TYPO requires at least 15 cases; found {no_diacritic_or_typo}")
    for tag, minimum in VAGUE_CROSS_TAG_MINIMUMS.items():
        if tag_counts[tag] < minimum:
            errors.append(f"tag {tag} requires at least {minimum} cases; found {tag_counts[tag]}")
    fault_modes = {
        str(case.get("geminiMode"))
        for case in unique_cases if case.get("group") == "GEMINI_FAILURE"
    }
    if fault_modes != GEMINI_FAULT_MODES:
        errors.append(f"GEMINI_FAILURE must cover every local fixture mode; found {sorted(fault_modes)}")
    represented_defects = {case.get("knownDefect") for case in unique_cases if case.get("knownDefect")}
    if represented_defects != KNOWN_DEFECT_CODES:
        errors.append(f"known defects must all be represented; found {sorted(represented_defects)}")
    if errors:
        raise CorpusValidationError(errors)


def evaluate_vague_baseline(cases_path: Path) -> dict[str, object]:
    """Execute an independently authored corpus without mutating its oracle."""

    source_bytes = cases_path.read_bytes()
    cases = validate_vague_corpus(cases_path)
    expectation_digest = _expectation_digest(cases)
    results = [_evaluate_vague_case(case, index) for index, case in enumerate(cases)]
    if cases_path.read_bytes() != source_bytes or _expectation_digest(cases) != expectation_digest:
        raise RuntimeError("corpus or expected oracle changed during baseline execution")

    findings = [_finding_from_result(item) for item in results if not item["passed"]]
    # A rate over the whole corpus is dominated by defects already known and scheduled. The
    # second slice — cases with no authored knownDefect — is the engine's own baseline.
    engine_only = [item for item in results if not item["knownDefect"]]
    new_discoveries = [
        {
            "caseId": finding["caseId"],
            "dimensions": [
                dimension for dimension, classification in finding["classificationByDimension"].items()
                if classification == "NEW_FINDING"
            ],
        }
        for finding in findings
        if "NEW_FINDING" in finding["classificationByDimension"].values()
    ]
    return {
        "schemaVersion": "phase-1-triage-v2-vague-baseline-1.0",
        "governance": {
            "projectType": "ACADEMIC_COMMUNITY_PROJECT",
            "intendedUse": "OFFLINE_SYNTHETIC_EVALUATION",
            "clinicalValidationStatus": "NOT_CLINICALLY_VALIDATED",
            "clinicalReviewStatus": "PENDING",
            "syntheticOnly": True,
            "networkAllowed": False,
            "expectedAuthoredBeforeActual": True,
        },
        "oracle": {
            "casesPath": str(cases_path),
            "sourceSha256": sha256(source_bytes).hexdigest(),
            "expectationSha256": expectation_digest,
            "caseCount": len(cases),
            "uniqueCaseCount": len({case["id"] for case in cases}),
        },
        "total": len(results),
        "passed": sum(bool(item["passed"]) for item in results),
        "failed": sum(not bool(item["passed"]) for item in results),
        "metrics": _vague_metrics(results),
        "metricsExcludingKnownDefects": _vague_metrics(engine_only) if engine_only else None,
        "rates": {
            "byGroup": _rates_by(results, lambda item: [item["group"]]),
            "byTag": _rates_by(results, lambda item: item["tags"] or ["UNTAGGED"]),
            "byGeminiMode": _rates_by(results, lambda item: [item["geminiMode"]]),
        },
        "findingSummary": {
            "knownDefectCaseCount": sum(finding["classification"] != "NEW_FINDING" for finding in findings),
            "newFindingCaseCount": len(new_discoveries),
            "classificationCounts": dict(sorted(Counter(
                classification
                for finding in findings
                for classification in finding["classificationByDimension"].values()
            ).items())),
        },
        "newDiscoveries": new_discoveries,
        "findings": findings,
        "results": results,
        "limitations": [
            "All messages and profiles are synthetic; no production, staging, log, database, or real-user data was used.",
            "Clinical review remains PENDING, so rates are an engineering baseline rather than clinical validation.",
            "Gemini modes are local deterministic failure fixtures and do not measure provider latency or live-model quality.",
            "The corpus measures the current canonical catalog and rules; it does not approve new questions or thresholds.",
            "Known-defect attribution is an authored engineering hypothesis; a matching failed dimension does not prove root-cause causality.",
            "The expectation digest proves no mutation during a run, not an independently timestamped pre-run oracle manifest.",
            "Some multi-turn follow-up messages reuse canonical safety-answer wording, so lexical diversity is lower than the case count.",
            "crossFamilyBatchRate uses a provisional question grouping declared in this script, not a reviewed complaint taxonomy; that contract is Phase 2 work.",
            "geminiFaultSafetyRetentionRate is not Gemini-on/off parity: the corpus holds no paired runs of one message under both modes.",
            "firstQuestionRelevanceRate scores strict first position; firstTurnQuestionRelevanceRate is the position-independent companion, because the approved design asks the global danger screen alongside a clarification question.",
        ],
    }


def _evaluate_vague_case(case: dict[str, object], index: int) -> dict[str, object]:
    fixture = LocalGeminiFaultFixture(str(case["geminiMode"]))
    session_id = str(uuid5(NAMESPACE_URL, f"carebridge-vague-baseline-{case['id']}"))
    profile = case["profile"]
    authored_turns = [{"message": case["message"]}, *case["turns"]]
    prior_state: dict[str, object] | None = None
    trajectory: list[dict[str, object]] = []
    started = perf_counter()
    for turn_index, turn in enumerate(authored_turns):
        turn_label = (
            f"{case['id']}.initial"
            if turn_index == 0
            else f"{case['id']}.turns[{turn_index - 1}]"
        )
        request = TriageTurnRequest(
            sessionId=session_id,
            stateVersion=int(prior_state.get("stateVersion", 1)) if prior_state else 1,
            expectedStateVersion=int(prior_state.get("stateVersion", 1)) if prior_state else 1,
            requestId=f"baseline-request-{index:04d}-{turn_index:02d}",
            messageId=f"baseline-message-{index:04d}-{turn_index:02d}",
            latestUserMessage=str(turn["message"]),
            activeProfileId=f"baseline-profile-{index:04d}",
            selectedTarget=profile["selectedTarget"],
            journeyContext=profile["journeyContext"],
            previousState=deepcopy(prior_state),
            signals=turn.get("signals", {}),
            measurements=turn.get("measurements", {}),
            answeredQuestionIds=turn.get("answeredQuestionIds", []),
            submittedOptionCodes=_derive_submitted_option_codes(turn, label=turn_label),
            expectedRulesetHash=get_registry().ruleset_sha256,
        )
        state = _turn_state(request)
        _merge_reported_measurements(
            state,
            extract_reported_measurements(
                request.latestUserMessage, target_entity=state.get("targetEntity")
            ),
        )
        state["signals"] = merge_as_floor(
            dict(state.get("signals", {})),
            detect_danger_signals(
                request.latestUserMessage,
                stage=state.get("stage"),
                stage_source="EXPLICIT_SELECTED_PROFILE",
            ),
        )
        state.update(global_safety_gate(state))
        if state.get("triageOutcome") != "RED" and fixture.mode != "OFF":
            extraction = extract_and_validate(request.latestUserMessage, fixture)
            if extraction is not None:
                state["signals"] = _merge_observations(state.get("signals"), extraction.signals)
        turn_started = perf_counter()
        result = dict(build_triage_graph().invoke(state, graph_config(session_id)))
        result.pop("__interrupt__", None)
        trajectory.append({
            "turn": turn_index + 1,
            "target": _value(result.get("targetEntity")),
            "stage": _value(result.get("stage")),
            "disposition": _value(result.get("triageOutcome")),
            "questions": list(result.get("plannedQuestionIds", [])),
            "answeredQuestionIds": list(result.get("answeredQuestionIds", [])),
            "reasonCodes": list(result.get("reasonCodes", [])),
            "pendingRule": _pending_rule_id(result.get("primaryPendingRiskStatus")),
            "stopConversation": bool(result.get("stopConversation")),
            "questionRound": result.get("questionRound"),
            "latencyMs": round((perf_counter() - turn_started) * 1_000, 3),
        })
        prior_state = result
        if result.get("stopConversation") is True:
            break

    final = trajectory[-1]
    questions_by_turn = [turn["questions"] for turn in trajectory]
    all_questions = [question for questions in questions_by_turn for question in questions]
    repeated = _repeated_questions(trajectory)
    wrong_entity, wrong_stage = _wrong_context_questions(
        trajectory,
        expected_target=str(case["expectedTarget"]),
        expected_stage=str(case["expectedStage"]),
        expected_target_by_turn=case.get("expectedTargetByTurn"),
        expected_stage_by_turn=case.get("expectedStageByTurn"),
    )
    acceptable_first = case["acceptableFirstQuestions"]
    actual_first = trajectory[0]["questions"][0] if trajectory[0]["questions"] else None
    focused = case["acceptableFocusedQuestions"]
    expected_safety = case["expectedSafetyQuestions"]
    terminal = bool(final["stopConversation"]) or final["disposition"] in {
        "RED", "YELLOW", "GREEN", "OUT_OF_SCOPE"
    }
    authored_reaches_limit = len(authored_turns) == case["maxTurns"]
    # A short authored trajectory cannot prove eventual termination. Report it as N/A,
    # not a pass; only terminal runs or runs observed through their budget are measurable.
    finite: bool | None = terminal if terminal or authored_reaches_limit else None
    immediate_red = final["disposition"] == "RED" and not all_questions
    expected = _oracle_projection(case)
    checks = {
        "target": final["target"] == case["expectedTarget"],
        "stage": final["stage"] == case["expectedStage"],
        # Immediate RED is supposed to stop without asking. Question expectations are N/A.
        "firstQuestion": None if immediate_red else (
            actual_first in acceptable_first if acceptable_first else actual_first is None
        ),
        "safetyQuestions": None if immediate_red else (
            set(expected_safety) <= set(trajectory[0]["questions"])
        ),
        "focusedQuestion": None if immediate_red else (
            not focused or bool(set(focused) & set(all_questions))
        ),
        "forbiddenQuestions": not bool(set(case["forbiddenQuestions"]) & set(all_questions)),
        "wrongEntityQuestion": not wrong_entity,
        "wrongStageQuestion": not wrong_stage,
        "repeatedQuestion": not repeated,
        "maxTurns": (
            len(trajectory) <= case["maxTurns"]
            and (final["questionRound"] is None or int(final["questionRound"]) <= case["maxTurns"])
        ),
        "finiteTermination": finite,
        "disposition": final["disposition"] in case["allowedDispositions"],
        # Expected reason codes are required evidence; additional deterministic/system codes
        # remain visible in actual and are not forbidden by an empty list.
        "reasonCodes": set(case["expectedReasonCodes"]) <= set(final["reasonCodes"]),
        "pendingRule": final["pendingRule"] == case["expectedPendingRule"],
        "noUnsupportedGreen": final["disposition"] != "GREEN" or "GREEN" in case["allowedDispositions"],
        "faultNoNetwork": fixture.network_client_created is False,
    }
    # Diagnostics are measured, reported, and deliberately NOT part of `checks`: adding a
    # dimension to the gate would silently move the pass/fail line of an already-published
    # baseline. They answer "what else is true about this run", not "did it pass".
    diagnostics = {
        # Position-independent variant of the firstQuestion check: was the expected opening
        # question asked anywhere in turn 1? The approved Phase 0.5 design pairs the global
        # danger screen with a clarification question, so position alone is a weak oracle.
        "firstTurnQuestion": None if immediate_red else (
            bool(set(acceptable_first) & set(trajectory[0]["questions"]))
            if acceptable_first else not trajectory[0]["questions"]
        ),
        "firstQuestionPosition": (
            trajectory[0]["questions"].index(actual_first)
            if actual_first in trajectory[0]["questions"] else None
        ),
        "expectedFirstAskedAtPosition": next(
            (index for index, question in enumerate(trajectory[0]["questions"])
             if question in acceptable_first),
            None,
        ),
    }
    return {
        "id": case["id"],
        "group": case["group"],
        "tags": case["tags"],
        "geminiMode": case["geminiMode"],
        "knownDefect": case["knownDefect"],
        "passed": all(value is not False for value in checks.values()),
        "checks": checks,
        "diagnostics": diagnostics,
        "expected": expected,
        "actual": {
            "turnQuestionStats": _turn_question_stats(trajectory),
            "finalTarget": final["target"],
            "finalStage": final["stage"],
            "finalDisposition": final["disposition"],
            "finalReasonCodes": final["reasonCodes"],
            "finalPendingRule": final["pendingRule"],
            "firstQuestion": actual_first,
            "allQuestions": all_questions,
            "repeatedQuestions": sorted(repeated),
            "wrongEntityQuestions": sorted(wrong_entity),
            "wrongStageQuestions": sorted(wrong_stage),
            "turnsExecuted": len(trajectory),
            "stopConversation": final["stopConversation"],
            "latencyMs": round((perf_counter() - started) * 1_000, 3),
            "geminiFixture": {
                "mode": fixture.mode,
                "callCount": fixture.call_count,
                "observedFailure": fixture.observed_failure,
                "networkClientCreated": fixture.network_client_created,
            },
            "trajectory": trajectory,
        },
    }


def _oracle_projection(case: dict[str, object]) -> dict[str, object]:
    return {
        key: deepcopy(value)
        for key, value in case.items()
        if key in {"message", "turns", "rationale", "clinicalReviewStatus", "knownDefect"}
        or key.startswith("expected") or key.startswith("acceptable") or key in {
            "forbiddenQuestions", "allowedDispositions", "maxTurns"
        }
    }


def _contains_actual_field(value: object) -> bool:
    if type(value) is dict:
        return any(
            str(key).lower().startswith("actual") or _contains_actual_field(item)
            for key, item in value.items()
        )
    if type(value) is list:
        return any(_contains_actual_field(item) for item in value)
    return False


def _expectation_digest(cases: list[dict[str, object]]) -> str:
    encoded = json.dumps(
        [{"id": case["id"], **_oracle_projection(case)} for case in cases],
        ensure_ascii=False, sort_keys=True, separators=(",", ":"),
    ).encode("utf-8")
    return sha256(encoded).hexdigest()


def _pending_rule_id(value: object) -> object:
    if type(value) is dict:
        return value.get("ruleId") or value.get("rule_id")
    return getattr(value, "rule_id", getattr(value, "ruleId", None))


def _repeated_questions(trajectory: list[dict[str, object]]) -> set[str]:
    seen: set[str] = set()
    repeated: set[str] = set()
    answered: set[str] = set()
    for turn in trajectory:
        questions = list(turn["questions"])
        answered.update(turn["answeredQuestionIds"])
        repeated.update(question for question, count in Counter(questions).items() if count > 1)
        current = set(questions)
        repeated.update((seen | answered) & current)
        seen.update(current)
    return repeated


def _turn_expectation(by_turn: object, index: int, fallback: str) -> str:
    """The oracle's value for this turn, or the single whole-session value."""

    if type(by_turn) is list and index < len(by_turn) and type(by_turn[index]) is str:
        return by_turn[index]
    return fallback


def _wrong_context_questions(
    trajectory: list[dict[str, object]], *, expected_target: str, expected_stage: str,
    expected_target_by_turn: object = None, expected_stage_by_turn: object = None,
) -> tuple[set[str], set[str]]:
    wrong_entity: set[str] = set()
    wrong_stage: set[str] = set()
    all_targets = {"MOTHER", "BABY"}
    all_stages = {item.value for item in CareStage}
    unresolved = {"UNKNOWN", "CONFLICTED"}
    # Scored against the oracle, deliberately — see
    # test_wrong_context_questions_are_scored_against_oracle_not_engine_prediction.
    # Judging a question against the context the engine held when it asked was tried and
    # reverted on 2026-08-11: it measures whether the engine was internally consistent, while
    # what matters is what the person on the other end was actually asked. If the subject is a
    # mother and the engine believed it was assessing her baby, "bé bú thế nào?" was still put
    # to a mother.
    #
    # `expected_target_by_turn` exists for the one case a single value cannot express: a
    # conversation whose subject legitimately changes. "Em thấy khó chịu, chắc là tình trạng
    # của mẹ" followed later by "Bé vẫn khó ở" is about the mother and then about the baby, and
    # a maternal question in turn two was right when it was asked. Still the oracle's answer,
    # not the engine's — the fixture states the change, the engine does not get to claim it.
    for index, turn in enumerate(trajectory):
        target = _turn_expectation(expected_target_by_turn, index, expected_target)
        stage = _turn_expectation(expected_stage_by_turn, index, expected_stage)
        for question_id in turn["questions"]:
            question = CATALOG.get(question_id)
            if question is None:
                wrong_entity.add(question_id)
                wrong_stage.add(question_id)
                continue
            targets = {_value(item) for item in question.target_entities}
            stages = {_value(item) for item in question.applicable_stages}
            if target in all_targets and targets and target not in targets:
                wrong_entity.add(question_id)
            elif target in unresolved and targets and targets != all_targets:
                wrong_entity.add(question_id)
            if stage not in unresolved and stages and stage not in stages:
                wrong_stage.add(question_id)
            elif stage in unresolved and stages and stages != all_stages:
                wrong_stage.add(question_id)
    return wrong_entity, wrong_stage


def _finding_from_result(result: dict[str, object]) -> dict[str, object]:
    failed = [
        dimension for dimension, passed in result["checks"].items() if passed is False
    ]
    known = result.get("knownDefect")
    classification = {
        dimension: known
        if known and dimension in KNOWN_DEFECT_DIMENSIONS.get(str(known), set())
        else "NEW_FINDING"
        for dimension in failed
    }
    return {
        "caseId": result["id"],
        "group": result["group"],
        "knownDefect": known,
        "classification": str(known) if classification and "NEW_FINDING" not in classification.values() else "NEW_FINDING",
        "failedDimensions": failed,
        "classificationByDimension": classification,
        "actualDisposition": result["actual"]["finalDisposition"],
        "actualFirstQuestion": result["actual"]["firstQuestion"],
        "actualQuestions": result["actual"]["allQuestions"],
    }


def _ratio(numerator: int, denominator: int) -> float:
    return round(numerator / denominator, 6) if denominator else 0.0


def _check_rate(results: list[dict[str, object]], dimension: str) -> float:
    evaluated = [item["checks"][dimension] for item in results if item["checks"][dimension] is not None]
    return _ratio(sum(value is True for value in evaluated), len(evaluated))


#: PROVISIONAL question grouping, used only to measure whether one turn mixes unrelated
#: clinical topics. It is NOT the complaint taxonomy — that contract does not exist yet and is
#: Phase 2 work. SAFETY and CONTEXT are deliberately excluded from the "family" count: the
#: global danger screen and target/stage clarification are designed to accompany anything, so
#: pairing them with a clinical question is not topic mixing.
_PROVISIONAL_QUESTION_FAMILIES: dict[str, str] = {
    "Q_GLOBAL_DANGER": "SAFETY",
    "Q_SAFETY_SELF_HARM": "SAFETY",
    "Q_CLARIFY_TARGET_ENTITY": "CONTEXT",
    "Q_CLARIFY_TARGET_FIRST": "CONTEXT",
    "Q_CLARIFY_INTENT": "CONTEXT",
    "Q_BABY_AGE_MONTHS": "CONTEXT",
    "Q_GESTATIONAL_WEEK": "CONTEXT",
    "Q_POSTPARTUM_DAY": "CONTEXT",
    "Q_PREGNANCY_TEST": "CONTEXT",
    "Q_BLEEDING_AMOUNT": "BLEEDING",
    "Q_CLOTS": "BLEEDING",
    "Q_HEADACHE_SEVERITY": "NEURO_HYPERTENSIVE",
    "Q_VISUAL_CHANGE": "NEURO_HYPERTENSIVE",
    "Q_BP_IF_KNOWN": "NEURO_HYPERTENSIVE",
    "Q_EPIGASTRIC_PAIN": "NEURO_HYPERTENSIVE",
    "Q_SWELLING": "NEURO_HYPERTENSIVE",
    "Q_DIZZINESS": "NEURO_HYPERTENSIVE",
    "Q_BABY_FEEDING": "PAEDIATRIC_INTAKE",
    "Q_BABY_HYDRATION": "PAEDIATRIC_INTAKE",
    "Q_BABY_TEMPERATURE": "PAEDIATRIC_INTAKE",
    "Q_PAIN_SEVERITY": "PAIN",
}
_ACCOMPANYING_FAMILIES = frozenset({"SAFETY", "CONTEXT"})


def _clinical_families(questions: Iterable[str]) -> set[str]:
    """Clinical topics represented in one batch, ignoring always-allowed companions."""

    return {
        family
        for question in questions
        if (family := _PROVISIONAL_QUESTION_FAMILIES.get(question, "UNMAPPED"))
        not in _ACCOMPANYING_FAMILIES
    }


def _turn_question_stats(trajectory: list[dict[str, object]]) -> dict[str, object]:
    """Per-turn batching diagnostics: how many questions, and do they mix topics?"""

    batched = [turn for turn in trajectory if len(turn["questions"]) >= 2]
    return {
        "turnCount": len(trajectory),
        "questionCount": sum(len(turn["questions"]) for turn in trajectory),
        "batchedTurnCount": len(batched),
        "crossFamilyTurnCount": sum(
            len(_clinical_families(turn["questions"])) >= 2 for turn in batched
        ),
        "crossFamilyTurns": [
            {"turn": turn["turn"], "questions": list(turn["questions"]),
             "families": sorted(_clinical_families(turn["questions"]))}
            for turn in batched
            if len(_clinical_families(turn["questions"])) >= 2
        ],
    }


def _vague_metrics(results: list[dict[str, object]]) -> dict[str, object]:
    """Every headline metric for one slice of the corpus.

    Computed twice by the caller — once over the whole corpus and once over cases with no
    authored ``knownDefect`` — because a rate dominated by defects already scheduled for repair
    says little about the engine's own behaviour.
    """

    checks = [
        passed for item in results for passed in item["checks"].values() if passed is not None
    ]
    red_only = [
        item for item in results if item["expected"]["allowedDispositions"] == ["RED"]
    ]
    fault_cases = [item for item in results if item["geminiMode"] != "OFF"]
    fault_safe = [
        item for item in fault_cases
        if item["checks"]["safetyQuestions"] is not False
        and item["checks"]["disposition"] is not False
    ]
    turn_stats = [item["actual"]["turnQuestionStats"] for item in results]
    batched_turns = sum(stat["batchedTurnCount"] for stat in turn_stats)
    return {
        "caseCount": len(results),
        "casePassRate": _ratio(sum(bool(item["passed"]) for item in results), len(results)),
        "checkPassRate": _ratio(sum(checks), len(checks)),
        "firstQuestionRelevanceRate": _check_rate(results, "firstQuestion"),
        # Position-independent companion to the metric above. The approved design asks the
        # global danger screen ALONGSIDE a clarification question, so a strict first-position
        # rule scores that deliberate behaviour as a miss.
        "firstTurnQuestionRelevanceRate": _ratio(
            sum(item["diagnostics"]["firstTurnQuestion"] is True for item in results),
            sum(item["diagnostics"]["firstTurnQuestion"] is not None for item in results),
        ),
        "safetyQuestionRate": _check_rate(results, "safetyQuestions"),
        "focusedQuestionRate": _check_rate(results, "focusedQuestion"),
        "targetAccuracyRate": _check_rate(results, "target"),
        "stageAccuracyRate": _check_rate(results, "stage"),
        "forbiddenQuestionRate": _ratio(
            sum(item["checks"]["forbiddenQuestions"] is False for item in results), len(results)
        ),
        "wrongEntityQuestionRate": _ratio(
            sum(item["checks"]["wrongEntityQuestion"] is False for item in results), len(results)
        ),
        "wrongStageQuestionRate": _ratio(
            sum(item["checks"]["wrongStageQuestion"] is False for item in results), len(results)
        ),
        "repeatedQuestionRate": _ratio(
            sum(item["checks"]["repeatedQuestion"] is False for item in results), len(results)
        ),
        "redRecallRate": _ratio(
            sum(item["actual"]["finalDisposition"] == "RED" for item in red_only), len(red_only)
        ),
        "redRecallEvaluatedCount": len(red_only),
        # Not true parity: the corpus has no paired ON/OFF runs of one message. This measures
        # whether a Gemini fault case still screened for danger and stayed inside its allowed
        # dispositions — necessary for safe degradation, not sufficient for a parity claim.
        "geminiFaultSafetyRetentionRate": _ratio(len(fault_safe), len(fault_cases)),
        "geminiFaultEvaluatedCount": len(fault_cases),
        "avgQuestionsPerTurn": _ratio(
            sum(stat["questionCount"] for stat in turn_stats),
            sum(stat["turnCount"] for stat in turn_stats),
        ),
        "crossFamilyBatchRate": _ratio(
            sum(stat["crossFamilyTurnCount"] for stat in turn_stats), batched_turns
        ),
        "crossFamilyEvaluatedTurnCount": batched_turns,
        "finiteTerminationRate": _check_rate(results, "finiteTermination"),
        "finiteTerminationEvaluatedCount": sum(
            item["checks"]["finiteTermination"] is not None for item in results
        ),
        "unsupportedGreenCount": sum(
            item["actual"]["finalDisposition"] == "GREEN"
            and "GREEN" not in item["expected"]["allowedDispositions"]
            for item in results
        ),
        "networkClientCreatedCount": sum(
            bool(item["actual"]["geminiFixture"]["networkClientCreated"]) for item in results
        ),
    }


def _rates_by(
    results: list[dict[str, object]], keys: Callable[[dict[str, object]], Iterable[str]]
) -> dict[str, dict[str, object]]:
    buckets: dict[str, list[dict[str, object]]] = {}
    for item in results:
        for key in keys(item):
            buckets.setdefault(str(key), []).append(item)
    return {
        key: {
            "total": len(items),
            "passed": sum(bool(item["passed"]) for item in items),
            "failed": sum(not bool(item["passed"]) for item in items),
            "passRate": _ratio(sum(bool(item["passed"]) for item in items), len(items)),
        }
        for key, items in sorted(buckets.items())
    }


def render_vague_markdown(report: dict[str, object]) -> str:
    """Render a reviewer-oriented report containing every failed case."""

    metrics = report["metrics"]
    engine = report.get("metricsExcludingKnownDefects") or {}

    def _cell(key: str, percent: bool = True) -> str:
        if key not in engine:
            return "n/a"
        value = engine[key]
        return f"{value:.2%}" if percent else str(value)

    lines = [
        "# AI Triage V2 vague-corpus baseline",
        "",
        "> Synthetic offline engineering evaluation. Clinical review status: **PENDING**.",
        "",
        "## Summary",
        "",
        f"- Cases: {report['total']} total; {report['passed']} passed; {report['failed']} failed.",
        f"- Cases with no authored knownDefect: {engine.get('caseCount', 0)}.",
        "",
        "The second column excludes cases whose failure was predicted against an already-known",
        "defect, so it is the closest available read on the engine's own behaviour.",
        "",
        "| Metric | All cases | Excluding known defects |",
        "|---|---:|---:|",
        f"| Case pass rate | {metrics['casePassRate']:.2%} | {_cell('casePassRate')} |",
        f"| Target accuracy | {metrics['targetAccuracyRate']:.2%} | {_cell('targetAccuracyRate')} |",
        f"| Stage accuracy | {metrics['stageAccuracyRate']:.2%} | {_cell('stageAccuracyRate')} |",
        f"| First-question relevance (strict position) | {metrics['firstQuestionRelevanceRate']:.2%} "
        f"| {_cell('firstQuestionRelevanceRate')} |",
        f"| First-turn question relevance (position-independent) | "
        f"{metrics['firstTurnQuestionRelevanceRate']:.2%} | {_cell('firstTurnQuestionRelevanceRate')} |",
        f"| Focused-question relevance | {metrics['focusedQuestionRate']:.2%} "
        f"| {_cell('focusedQuestionRate')} |",
        f"| Safety-question coverage | {metrics['safetyQuestionRate']:.2%} "
        f"| {_cell('safetyQuestionRate')} |",
        f"| Forbidden-question rate | {metrics['forbiddenQuestionRate']:.2%} "
        f"| {_cell('forbiddenQuestionRate')} |",
        f"| Wrong-entity question rate | {metrics['wrongEntityQuestionRate']:.2%} "
        f"| {_cell('wrongEntityQuestionRate')} |",
        f"| Wrong-stage question rate | {metrics['wrongStageQuestionRate']:.2%} "
        f"| {_cell('wrongStageQuestionRate')} |",
        f"| Repeated-question rate | {metrics['repeatedQuestionRate']:.2%} "
        f"| {_cell('repeatedQuestionRate')} |",
        f"| RED recall ({metrics['redRecallEvaluatedCount']} RED-only cases) "
        f"| {metrics['redRecallRate']:.2%} | {_cell('redRecallRate')} |",
        f"| Gemini-fault safety retention ({metrics['geminiFaultEvaluatedCount']} fault cases) "
        f"| {metrics['geminiFaultSafetyRetentionRate']:.2%} | {_cell('geminiFaultSafetyRetentionRate')} |",
        f"| Cross-family batch rate ({metrics['crossFamilyEvaluatedTurnCount']} batched turns) "
        f"| {metrics['crossFamilyBatchRate']:.2%} | {_cell('crossFamilyBatchRate')} |",
        f"| Finite termination ({metrics['finiteTerminationEvaluatedCount']} evaluable) "
        f"| {metrics['finiteTerminationRate']:.2%} | {_cell('finiteTerminationRate')} |",
        f"| Avg questions per turn | {metrics['avgQuestionsPerTurn']:.3f} "
        f"| {_cell('avgQuestionsPerTurn', percent=False)} |",
        f"| Unsupported GREEN | {metrics['unsupportedGreenCount']} "
        f"| {_cell('unsupportedGreenCount', percent=False)} |",
        f"| Network clients created | {metrics['networkClientCreatedCount']} "
        f"| {_cell('networkClientCreatedCount', percent=False)} |",
        "",
        "## Rates by group",
        "",
        "| Group | Total | Passed | Failed | Pass rate |",
        "|---|---:|---:|---:|---:|",
    ]
    for name, values in report["rates"]["byGroup"].items():
        lines.append(f"| {name} | {values['total']} | {values['passed']} | {values['failed']} | {values['passRate']:.2%} |")
    lines.extend(["", "## Rates by tag", "", "| Tag | Total | Passed | Failed | Pass rate |", "|---|---:|---:|---:|---:|"])
    for name, values in report["rates"]["byTag"].items():
        lines.append(f"| {name} | {values['total']} | {values['passed']} | {values['failed']} | {values['passRate']:.2%} |")
    lines.extend(["", "## Rates by Gemini fixture mode", "", "| Mode | Total | Passed | Failed | Pass rate |", "|---|---:|---:|---:|---:|"])
    for name, values in report["rates"]["byGeminiMode"].items():
        lines.append(f"| {name} | {values['total']} | {values['passed']} | {values['failed']} | {values['passRate']:.2%} |")
    lines.extend([
        "", "## Failed cases", "",
        "| Case | Group | Classification | Failed dimensions | Actual first question | Actual disposition |",
        "|---|---|---|---|---|---|",
    ])
    for finding in report["findings"]:
        dimensions = ", ".join(
            f"{name}={finding['classificationByDimension'][name]}"
            for name in finding["failedDimensions"]
        )
        lines.append(
            f"| {finding['caseId']} | {finding['group']} | {finding['classification']} | "
            f"{dimensions} | {finding['actualFirstQuestion'] or '—'} | "
            f"{finding['actualDisposition'] or '—'} |"
        )
    lines.extend(["", "## New discoveries", ""])
    if report["newDiscoveries"]:
        for discovery in report["newDiscoveries"]:
            lines.append(f"- `{discovery['caseId']}`: {', '.join(discovery['dimensions'])}.")
    else:
        lines.append("- None beyond the approved known-defect dimensions.")
    lines.extend(["", "## Limitations", ""])
    lines.extend(f"- {item}" for item in report["limitations"])
    lines.extend([
        "",
        "## Oracle integrity",
        "",
        f"- Corpus SHA-256: `{report['oracle']['sourceSha256']}`",
        f"- Expected/rationale SHA-256: `{report['oracle']['expectationSha256']}`",
        "- Actual values exist only in the JSON/Markdown baseline reports; the source corpus contains no actual fields.",
        "",
    ])
    return "\n".join(lines)


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
    parser.add_argument("--mode", choices=("legacy", "vague-baseline"), default="legacy")
    parser.add_argument("--cases", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--markdown-output", type=Path)
    args = parser.parse_args()
    if args.output and args.markdown_output:
        if args.output.resolve() == args.markdown_output.resolve():
            parser.error("--output and --markdown-output must resolve to different files")
    if args.mode == "vague-baseline" and args.output:
        candidate_markdown = args.markdown_output or args.output.with_suffix(".md")
        if args.output.resolve() == candidate_markdown.resolve():
            parser.error(
                "implicit Markdown output would overwrite JSON; use a non-.md --output "
                "or provide a distinct --markdown-output"
            )
    cases_path = args.cases or (
        DEFAULT_VAGUE_CASES if args.mode == "vague-baseline" else DEFAULT_CASES
    )
    try:
        report = (
            evaluate_vague_baseline(cases_path)
            if args.mode == "vague-baseline"
            else evaluate(cases_path)
        )
    except CorpusValidationError as failure:
        print(str(failure), file=sys.stderr)
        return 2
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    if args.mode == "vague-baseline" and (args.markdown_output or args.output):
        markdown_output = args.markdown_output or args.output.with_suffix(".md")
        markdown_output.parent.mkdir(parents=True, exist_ok=True)
        markdown_output.write_text(render_vague_markdown(report), encoding="utf-8")
    if args.mode == "vague-baseline":
        # Findings are the product of the baseline. Only invalid input/runtime failures are errors.
        return 0
    return 0 if (
        report["failed"] == 0
        and report["coverage"]["allRequiredCategoriesCovered"]
        and report["coverage"]["allRequiredMetricFieldsReported"]
    ) else 1


if __name__ == "__main__":
    raise SystemExit(main())
