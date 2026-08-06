"""Phase 2.5 safety and parity tests for the canonical V2 rule registry.

The vectors in ``tests/data/triage_rule_parity_vectors_v2.json`` are shared byte-for-byte
with the Java evaluator. Expected results come from the approved Rule Matrix plus the
Phase 2.5 safety policies; never edit an expectation just to turn a test green.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

import pytest

from app.questions.planner import plan_questions
from app.rendering.response_renderer import (
    BANNED_PHRASES,
    TemplateNotFoundError,
    render,
)
from app.rules.condition import (
    ConditionSchemaError,
    Presence,
    Tri,
    evaluate_condition,
    validate_condition,
)
from app.rules.evaluator import evaluate
from app.rules.registry import (
    REGISTRY_PATH,
    RegistryIntegrityError,
    load_registry,
)

VECTORS_PATH = Path(__file__).parent / "data" / "triage_rule_parity_vectors_v2.json"


@pytest.fixture(scope="module")
def registry():
    return load_registry()


@pytest.fixture(scope="module")
def vectors() -> list[dict]:
    return json.loads(VECTORS_PATH.read_text(encoding="utf-8"))["vectors"]


def _run(registry, payload):
    return evaluate(
        stage=payload["stage"],
        signals=payload["signals"],
        context=payload["context"],
        question_round=payload["questionRound"],
        minimum_dataset_complete=payload["minimumDatasetComplete"],
        reproductive_relevance_hint=payload["reproductiveRelevance"],
        registry=registry,
    )


# --------------------------------------------------------------------------- registry


def test_runtime_registry_matches_canonical_digest():
    sidecar = REGISTRY_PATH.with_suffix(REGISTRY_PATH.suffix + ".sha256")
    expected = sidecar.read_text(encoding="utf-8").strip()
    assert hashlib.sha256(REGISTRY_PATH.read_bytes()).hexdigest() == expected


def test_registry_reports_matrix_version_and_green_gate(registry):
    assert registry.rule_matrix_version == "0.1.0"
    assert registry.green_enabled is False, "GREEN must ship locked by default"


def test_registry_matches_approved_matrix_row_count(registry):
    """10 maternal rows from the v0.1.0 matrix, plus 7 paediatric rules ported from V1.

    The paediatric rules carry matchesSnapshot=false because that snapshot predates paediatric
    coverage; they are counted separately so a maternal row going missing still fails loudly.
    """

    maternal = [rule for rule in registry.rules if not rule.rule_id.startswith("PED_")]
    paediatric = [rule for rule in registry.rules if rule.rule_id.startswith("PED_")]

    assert len(maternal) == 10, "the Matrix has exactly 10 approved maternal rule rows"
    assert len(paediatric) == 7, "seven paediatric rules ported from V1"


def test_safety_policies_are_outside_the_clinical_rule_set(registry):
    policy_ids = {policy.policy_id for policy in registry.safety_policies}
    assert policy_ids == {"SAFETY_CYANOSIS_HOLDOVER_001", "SAFETY_SELF_HARM_001"}
    for policy in registry.safety_policies:
        assert policy.status != "APPROVED", (
            f"{policy.policy_id} must never carry clinical APPROVED status"
        )
        assert policy.review_due_at, f"{policy.policy_id} needs a review deadline"


def test_cyanosis_is_not_inside_global_red_001(registry):
    """The Matrix lists three signals for GLOBAL_RED_001; cyanosis is not one of them."""

    rule = registry.by_id("GLOBAL_RED_001")
    leaves = json.dumps(rule.condition)
    assert "CYANOSIS" not in leaves


def test_sys_info_001_keeps_the_matrix_stop_flag(registry):
    rule = registry.by_id("SYS_INFO_001")
    assert rule.stop_on_match is False, "Matrix 'Dừng hỏi' = FALSE"
    assert rule.action_code == "ASK_CLARIFYING_QUESTIONS"


def test_registry_fails_closed_on_invalid_approved_rule(tmp_path):
    document = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
    document["rules"][0]["condition"] = {"expression": "anything"}
    path = tmp_path / "triage_rules_v2.json"
    path.write_text(json.dumps(document), encoding="utf-8")
    (tmp_path / "triage_rules_v2.json.sha256").write_text(
        hashlib.sha256(path.read_bytes()).hexdigest(), encoding="utf-8"
    )
    with pytest.raises(RegistryIntegrityError):
        load_registry(path, manifest_path=tmp_path / "absent.json")


def test_registry_fails_closed_on_duplicate_rule_id(tmp_path):
    document = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
    document["rules"].append(dict(document["rules"][0]))
    path = tmp_path / "triage_rules_v2.json"
    path.write_text(json.dumps(document), encoding="utf-8")
    (tmp_path / "triage_rules_v2.json.sha256").write_text(
        hashlib.sha256(path.read_bytes()).hexdigest(), encoding="utf-8"
    )
    with pytest.raises(RegistryIntegrityError):
        load_registry(path, manifest_path=tmp_path / "absent.json")


def test_registry_fails_closed_when_a_critical_rule_is_missing(tmp_path):
    document = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
    document["rules"] = [r for r in document["rules"] if r["ruleId"] != "PREG_RED_001"]
    path = tmp_path / "triage_rules_v2.json"
    path.write_text(json.dumps(document), encoding="utf-8")
    (tmp_path / "triage_rules_v2.json.sha256").write_text(
        hashlib.sha256(path.read_bytes()).hexdigest(), encoding="utf-8"
    )
    manifest = tmp_path / "required_rule_manifest.json"
    manifest.write_text(json.dumps({"criticalRuleIds": ["PREG_RED_001"]}), encoding="utf-8")
    with pytest.raises(RegistryIntegrityError, match="critical rules missing"):
        load_registry(path, manifest_path=manifest)


def test_registry_rejects_a_tampered_copy(tmp_path):
    tampered = tmp_path / "triage_rules_v2.json"
    tampered.write_text(REGISTRY_PATH.read_text(encoding="utf-8") + "\n", encoding="utf-8")
    (tmp_path / "triage_rules_v2.json.sha256").write_text(
        REGISTRY_PATH.with_suffix(REGISTRY_PATH.suffix + ".sha256").read_text(encoding="utf-8"),
        encoding="utf-8",
    )
    with pytest.raises(RegistryIntegrityError):
        load_registry(tampered)


def test_reordering_the_registry_does_not_change_a_decision(registry, tmp_path):
    document = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
    document["rules"].reverse()
    path = tmp_path / "triage_rules_v2.json"
    path.write_text(json.dumps(document), encoding="utf-8")
    (tmp_path / "triage_rules_v2.json.sha256").write_text(
        hashlib.sha256(path.read_bytes()).hexdigest(), encoding="utf-8"
    )
    reordered = load_registry(path, manifest_path=tmp_path / "absent.json")

    payload = {
        "stage": "POSTPARTUM",
        "signals": {"SEIZURE": "PRESENT", "HEAVY_POSTPARTUM_BLEEDING": "PRESENT"},
        "context": {"postpartum_day": 3, "bleeding_amount": "HEAVY"},
        "questionRound": 1,
        "reproductiveRelevance": True,
        "minimumDatasetComplete": True,
    }
    assert _run(reordered, payload).decisive_rule_ids == _run(registry, payload).decisive_rule_ids


# ------------------------------------------------------------------------- tri-state


def test_kleene_truth_tables():
    assert (Tri.TRUE & Tri.UNKNOWN) is Tri.UNKNOWN
    assert (Tri.FALSE & Tri.UNKNOWN) is Tri.FALSE
    assert (Tri.TRUE | Tri.UNKNOWN) is Tri.TRUE
    assert (Tri.FALSE | Tri.UNKNOWN) is Tri.UNKNOWN
    assert (~Tri.UNKNOWN) is Tri.UNKNOWN
    assert (~Tri.TRUE) is Tri.FALSE
    assert (~Tri.FALSE) is Tri.TRUE


def test_not_of_missing_signal_is_never_true():
    """An unanswered question must not 'prove' a symptom is absent."""

    node = {"not": {"signal": "SEIZURE", "operator": "EQ", "value": True}}
    assert evaluate_condition(node, {}, {}) is Tri.UNKNOWN


def test_neq_against_unknown_is_unknown():
    node = {"signal": "SEIZURE", "operator": "NEQ", "value": True}
    assert evaluate_condition(node, {}, {}) is Tri.UNKNOWN


def test_not_exists_reports_absence_of_data_not_absence_of_symptom():
    node = {"signal": "SEIZURE", "operator": "NOT_EXISTS", "value": True}
    assert evaluate_condition(node, {}, {}) is Tri.TRUE
    assert evaluate_condition(node, {"SEIZURE": "ABSENT"}, {}) is Tri.FALSE


def test_explicit_absent_is_a_real_negative():
    node = {"signal": "SEIZURE", "operator": "EQ", "value": True}
    assert evaluate_condition(node, {"SEIZURE": "ABSENT"}, {}) is Tri.FALSE
    assert evaluate_condition(node, {"SEIZURE": "PRESENT"}, {}) is Tri.TRUE


def test_conflicted_and_unaware_behave_as_unknown():
    node = {"signal": "VAGINAL_BLEEDING", "operator": "EQ", "value": True}
    assert evaluate_condition(node, {"VAGINAL_BLEEDING": "CONFLICTED"}, {}) is Tri.UNKNOWN
    assert (
        evaluate_condition(node, {"VAGINAL_BLEEDING": "UNAWARE_OR_UNMEASURABLE"}, {})
        is Tri.UNKNOWN
    )


def test_boolean_and_number_spaces_stay_disjoint():
    node = {"context": "gestational_week", "operator": "EQ", "value": True}
    assert evaluate_condition(node, {}, {"gestational_week": 1}) is Tri.FALSE


@pytest.mark.parametrize(
    "operator,week,expected",
    [("GTE", 20, Tri.TRUE), ("GTE", 19, Tri.FALSE), ("LT", 19, Tri.TRUE), ("GT", 20, Tri.FALSE)],
)
def test_numeric_boundaries(operator, week, expected):
    node = {"context": "gestational_week", "operator": operator, "value": 20}
    assert evaluate_condition(node, {}, {"gestational_week": week}) is expected


def test_condition_schema_is_closed():
    for bad in (
        {"expression": "signals['SEIZURE'] == True"},
        {"signal": "SEIZURE", "operator": "MATCHES", "value": ".*"},
        {"any": []},
    ):
        with pytest.raises(ConditionSchemaError):
            validate_condition(bad)


# ---------------------------------------------------------------------------- vectors


def test_every_shared_vector_is_asserted(vectors):
    assert len(vectors) == 41


_VECTORS = json.loads(VECTORS_PATH.read_text(encoding="utf-8"))["vectors"]


@pytest.mark.parametrize("index", range(len(_VECTORS)))
def test_parity_vector(registry, vectors, index):
    vector = vectors[index]
    expected = vector["expected"]
    evaluation = _run(registry, vector["input"])
    label = f"{vector['id']}: {vector['description']}"

    assert evaluation.outcome == expected["outcome"], label
    assert list(evaluation.decisive_rule_ids) == expected["decisiveRuleIds"], label
    assert evaluation.stop_conversation == expected["stopConversation"], label
    assert evaluation.action_code == expected["actionCode"], label

    for reason in expected.get("reasonCodes", []):
        assert reason in evaluation.reason_codes, label
    for blocker in expected.get("greenBlockedBy", []):
        assert blocker in evaluation.green_blocked_by, label
    for rule_id in expected.get("pendingRedRuleIds", []):
        assert rule_id in evaluation.pending_red_rule_ids, label
    for signal in expected.get("unresolvedSignals", []):
        assert signal in evaluation.unresolved_signals, label
    for rule_id in expected.get("suppressedRuleIds", []):
        assert rule_id in evaluation.suppressed_rule_ids, label


# ------------------------------------------------------------------------ green lock


def test_green_can_never_be_reached_while_the_gate_is_locked(registry):
    for round_number in range(0, 5):
        for complete in (True, False):
            evaluation = evaluate(
                stage="PREGNANCY", signals={}, context={},
                question_round=round_number, minimum_dataset_complete=complete,
                registry=registry,
            )
            assert evaluation.outcome != "GREEN", (round_number, complete)


def test_audit_trace_records_suppressed_rules(registry):
    evaluation = evaluate(
        stage="PREGNANCY",
        signals={"HEAVY_VAGINAL_BLEEDING": "PRESENT", "VAGINAL_BLEEDING": "PRESENT"},
        context={"gestational_week": 20, "bleeding_amount": "HEAVY"},
        question_round=1, minimum_dataset_complete=True, registry=registry,
    )
    assert evaluation.decisive_rule_ids == ("PREG_RED_001",)
    assert "PREG_YELLOW_001" in evaluation.suppressed_rule_ids
    roles = {trace.rule_id: trace.role for trace in evaluation.all_matched_rules}
    assert roles["PREG_YELLOW_001"] == "SUPPRESSED_BY_HIGHER_SEVERITY"


# -------------------------------------------------------------------- planner pivot


def test_planner_pivots_away_from_an_unmeasurable_question():
    """No blood-pressure cuff: ask about perceivable symptoms instead of re-asking."""

    planned = plan_questions(
        candidate_question_ids=["Q_BP_IF_KNOWN", "Q_PAIN_SEVERITY"],
        signals={"blood_pressure": "UNAWARE_OR_UNMEASURABLE"},
    )
    assert "Q_BP_IF_KNOWN" not in planned.question_ids
    assert "Q_BP_IF_KNOWN" in planned.pivoted_from
    assert "Q_VISUAL_CHANGE" in planned.question_ids
    assert "Q_EPIGASTRIC_PAIN" in planned.question_ids


def test_planner_keeps_a_measurable_question_when_it_is_answerable():
    planned = plan_questions(candidate_question_ids=["Q_BP_IF_KNOWN"], signals={})
    assert planned.question_ids == ("Q_BP_IF_KNOWN",)
    assert planned.pivoted_from == ()


def test_planner_puts_unresolved_red_questions_first_and_caps_at_three():
    planned = plan_questions(
        candidate_question_ids=["Q_PAIN_SEVERITY", "Q_DIZZINESS", "Q_CLOTS", "Q_BLEEDING_AMOUNT"],
        signals={},
        priority_question_ids=["Q_VISUAL_CHANGE"],
    )
    assert planned.question_ids[0] == "Q_VISUAL_CHANGE"
    assert len(planned.question_ids) == 3


def test_planner_never_repeats_an_answered_question():
    planned = plan_questions(
        candidate_question_ids=["Q_BLEEDING_AMOUNT", "Q_DIZZINESS"],
        signals={},
        answered_question_ids=["Q_BLEEDING_AMOUNT"],
    )
    assert "Q_BLEEDING_AMOUNT" not in planned.question_ids


def test_every_catalog_option_carries_a_stable_code():
    from app.questions.catalog import CATALOG

    for item in CATALOG.values():
        for option in item.options:
            assert option.option_code.isupper()
            assert option.display_text


# ----------------------------------------------------------------------- renderer


def test_green_lock_template_never_reads_as_a_negative_finding():
    """A locked gate is 'we could not decide', never 'nothing was found'."""

    rendered = render(
        "NEEDS_MORE_INFO", "ROUTE_TO_HEALTHCARE_WORKER", ("GREEN_RELEASE_GATE_DISABLED",)
    )
    assert rendered.headline == "Chưa đủ thông tin để phân tầng nguy cơ an toàn"
    assert "không có nghĩa rằng triệu chứng là an toàn" in rendered.message


def test_unresolved_high_risk_template_says_a_signal_needs_review():
    rendered = render(
        "NEEDS_MORE_INFO", "ROUTE_TO_HEALTHCARE_WORKER", ("UNRESOLVED_RED_SIGNAL",)
    )
    assert rendered.headline == "Có dấu hiệu cần được làm rõ hoặc đánh giá sớm"
    assert "không thể phân tầng an toàn" in rendered.message


def test_every_template_is_flagged_as_not_clinically_signed():
    """Matrix columns 14 and 16 are untranscribed, so no wording is signed off yet."""

    rendered = render("RED", "IMMEDIATE_EMERGENCY_ASSESSMENT")
    assert rendered.clinical_sign_off == "PROVISIONAL_NOT_CLINICALLY_SIGNED"


@pytest.mark.parametrize("phrase", BANNED_PHRASES)
def test_no_template_contains_a_forbidden_reassurance(phrase):
    from app.rendering.response_renderer import _TEMPLATES

    for headline, message in _TEMPLATES.values():
        assert phrase not in f"{headline} {message}".lower()


def test_renderer_refuses_to_invent_text_for_an_unknown_verdict():
    with pytest.raises(TemplateNotFoundError):
        render("GREEN", "SOME_UNAPPROVED_ACTION")


def test_red_templates_lead_with_action_not_reassurance():
    rendered = render("RED", "IMMEDIATE_EMERGENCY_ASSESSMENT")
    assert "ngay" in rendered.message
    assert rendered.limitations


# ------------------------------------------------------- full-result parity fingerprint


def _canonical_rows(registry, vectors) -> list[dict]:
    """Canonicalise EVERY result field, not just the four mandatory ones.

    The per-vector assertions above check optional lists by containment, which cannot catch
    an extra entry appearing on one runtime only. Hashing the full canonical result can.
    """

    rows = []
    for vector in vectors:
        payload = vector["input"]
        evaluation = _run(registry, payload)
        rows.append({
            "id": vector["id"],
            "outcome": evaluation.outcome,
            "decisiveRuleIds": list(evaluation.decisive_rule_ids),
            "stopConversation": evaluation.stop_conversation,
            "actionCode": evaluation.action_code,
            "reasonCodes": sorted(evaluation.reason_codes),
            "questionIds": sorted(evaluation.question_ids),
            "requiredFields": sorted(evaluation.required_fields),
            "greenBlockedBy": sorted(evaluation.green_blocked_by),
            "pendingRedRuleIds": sorted(evaluation.pending_red_rule_ids),
            "unresolvedSignals": sorted(evaluation.unresolved_signals),
            "suppressedRuleIds": sorted(evaluation.suppressed_rule_ids),
        })
    return rows


def test_full_result_parity_fingerprint(registry, vectors):
    """Java computes the same digest over the same canonical form; drift fails both sides."""

    expected = json.loads(
        (Path(__file__).parents[2] / "Contracts" / "triage" / "parity_result_fingerprint.json")
        .read_text(encoding="utf-8")
    )
    assert expected["vectorCount"] == len(vectors)

    blob = json.dumps(
        _canonical_rows(registry, vectors),
        sort_keys=True, ensure_ascii=False, separators=(",", ":"),
    )
    digest = hashlib.sha256(blob.encode("utf-8")).hexdigest()
    assert digest == expected["fingerprint"], (
        "Python engine output changed. If this is intentional, regenerate the fingerprint "
        "deliberately and confirm Java produces the identical digest."
    )
