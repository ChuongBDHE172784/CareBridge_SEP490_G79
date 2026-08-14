"""Fail-closed numeric extraction for V2 free-text measurements."""

from __future__ import annotations

import json
import pathlib
import re
import uuid

import pytest

import app.triage.api as api
from app.rules.registry import get_registry
from app.triage.deterministic_measurements import extract_reported_measurements


VAGUE_CORPUS = json.loads(
    (pathlib.Path(__file__).parent / "data" / "triage_v2_vague_corpus_v1.json").read_text(
        encoding="utf-8"
    )
)


def _case(case_id: str) -> dict[str, object]:
    return next(case for case in VAGUE_CORPUS if case["id"] == case_id)


@pytest.mark.parametrize(
    ("word", "months"),
    [
        ("một", 1), ("hai", 2), ("ba", 3), ("bốn", 4), ("năm", 5),
        ("sáu", 6), ("bảy", 7), ("tám", 8), ("chín", 9),
        ("mười", 10), ("mười một", 11), ("mười hai", 12),
    ],
)
def test_baby_age_supports_vietnamese_number_words_one_through_twelve(word, months):
    result = extract_reported_measurements(f"Bé {word} tháng tuổi", target_entity="BABY")

    assert result.baby_age_months == months
    assert result.measurements["babyAgeMonths"] == {
        "value": months,
        "unit": "MONTHS",
        "temporalStatus": "CURRENT",
        "provenance": "USER_REPORTED_TEXT",
    }


@pytest.mark.parametrize(
    ("message", "months", "temperature"),
    [
        ("Bé 2 tháng đo được 38,2 độ", 2, 38.2),
        ("Con mười tháng sốt 39.3 độ C", 10, 39.3),
        ("Bé hai tháng, thân nhiệt đo 38 độ", 2, 38.0),
        ("Be hai thang do duoc 38,2 do", 2, 38.2),
        ("Bé hai tháng sốt 38°C", 2, 38.0),
        ("Bé hai tháng sốt 38.2 C", 2, 38.2),
        ("Bé mới được hai tháng, đo được 38,2 độ", 2, 38.2),
    ],
)
def test_digits_words_and_comma_or_dot_celsius_are_extracted(message, months, temperature):
    result = extract_reported_measurements(message, target_entity="BABY")

    assert result.baby_age_months == months
    assert result.measurements["temperatureC"]["value"] == temperature
    assert result.measurements["temperatureC"]["unit"] == "C"
    assert result.measurements["temperatureC"]["provenance"] == "USER_REPORTED_TEXT"


@pytest.mark.parametrize(
    "message",
    [
        "Bé được 2 tuổi rồi",
        "Em uống 2 viên thuốc",
        "Nhà em có 3 đứa con",
        "Bé sinh lúc 38 tuần",
        "Em đo huyết áp 120/80",
        "Bé sốt 3,8 độ",
        "Bé sốt 380 độ",
        "Nếu bé hai tháng sốt 38 độ thì sao?",
    ],
)
def test_unrelated_units_and_implausible_temperatures_are_rejected(message):
    result = extract_reported_measurements(message, target_entity="BABY")

    assert result.baby_age_months is None
    assert result.measurements == {}


@pytest.mark.parametrize(
    ("message", "code"),
    [
        ("Bé sốt 100 độ C", "temperatureC"),
        ("Bé 24 tháng tuổi", "babyAgeMonths"),
    ],
)
def test_explicit_out_of_range_values_are_reported_for_correction(message, code):
    result = extract_reported_measurements(message, target_entity="BABY")

    assert code in result.invalid_codes
    assert code not in result.measurements


def test_newborn_zero_months_is_valid():
    result = extract_reported_measurements("Bé 0 tháng tuổi", target_entity="BABY")

    assert result.baby_age_months == 0
    assert result.measurements["babyAgeMonths"]["value"] == 0


def test_turn_rejects_implausible_temperature_instead_of_silently_ignoring(
    monkeypatch,
):
    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    request = api.TriageTurnRequest.model_validate(
        {
            "sessionId": str(uuid.uuid4()),
            "stateVersion": 0,
            "expectedStateVersion": 0,
            "requestId": "request_" + uuid.uuid4().hex[:16],
            "messageId": "message_" + uuid.uuid4().hex[:16],
            "latestUserMessage": "Bé sốt 100 độ C",
            "selectedTarget": "BABY",
            "journeyContext": {"stage": "INFANT_0_12M", "babyAgeMonths": 2},
            "signals": {},
            "measurements": {},
            "expectedRulesetHash": get_registry().ruleset_sha256,
        }
    )

    with pytest.raises(api.HTTPException) as caught:
        api.execute_turn(request)

    assert caught.value.status_code == 422
    assert caught.value.detail == {
        "code": "IMPLAUSIBLE_MEASUREMENT",
        "fields": ["temperatureC"],
    }


@pytest.mark.parametrize(
    "message",
    [
        "Bé hai tháng trước đo được 38,2 độ, giờ đã hết sốt",
        "Nhiệt độ phòng 38 độ, bé có vẻ sốt",
        "Mẹ sốt 38 độ, bé hai tháng vẫn bình thường",
        "Bé hai tháng sốt 38 độ F",
        "Bé hai tháng sốt 38 F",
    ],
)
def test_non_current_non_patient_or_non_celsius_readings_do_not_emit_temperature(message):
    result = extract_reported_measurements(message, target_entity="BABY")

    assert "temperatureC" not in result.measurements


@pytest.mark.parametrize(
    "message",
    [
        "Bé 2 tháng hay bé 3 tháng, em chưa nhớ chính xác",
        "Con hai tháng nhưng hồ sơ lại ghi con ba tháng",
    ],
)
def test_conflicting_baby_ages_fail_closed(message):
    result = extract_reported_measurements(message, target_entity="BABY")

    assert result.baby_age_months is None
    assert "babyAgeMonths" not in result.measurements


def test_conflicting_temperatures_fail_closed():
    result = extract_reported_measurements(
        "Bé sốt, đo 38,2 độ rồi đo lại 39 độ", target_entity="BABY"
    )

    assert "temperatureC" not in result.measurements


def test_temperature_requires_a_local_clinical_anchor():
    result = extract_reported_measurements(
        "Phòng hôm nay khoảng 38 độ, bé vẫn chơi bình thường", target_entity="BABY"
    )

    assert "temperatureC" not in result.measurements


def test_temperature_anchor_cannot_cross_a_clause_boundary():
    result = extract_reported_measurements(
        "Phòng đang 38 độ, bé có vẻ sốt", target_entity="BABY"
    )

    assert "temperatureC" not in result.measurements


def test_accent_folded_con_does_not_turn_pregnancy_timing_into_baby_age():
    result = extract_reported_measurements(
        "Còn 2 tháng nữa mới đến ngày sinh", target_entity="MOTHER"
    )

    assert result.baby_age_months is None
    assert result.measurements == {}


def test_clear_months_old_phrase_can_resolve_age_before_target_resolution():
    result = extract_reported_measurements("Hai tháng tuổi", target_entity="UNKNOWN")

    assert result.baby_age_months == 2


@pytest.mark.parametrize("case_id", ["pediatric_018", "pediatric_019"])
def test_corpus_numeric_fever_cases_reach_red_without_gemini(case_id, monkeypatch):
    case = _case(case_id)
    profile = case["profile"]
    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    request = api.TriageTurnRequest.model_validate(
        {
            "sessionId": str(uuid.uuid4()),
            "stateVersion": 0,
            "expectedStateVersion": 0,
            "requestId": "request_" + uuid.uuid4().hex[:16],
            "messageId": "message_" + uuid.uuid4().hex[:16],
            "latestUserMessage": case["message"],
            "selectedTarget": profile["selectedTarget"],
            "journeyContext": profile["journeyContext"],
            "signals": {},
            "measurements": {},
            "expectedRulesetHash": get_registry().ruleset_sha256,
        }
    )

    state = api.execute_turn(request).state

    assert state["triageOutcome"] == "RED"
    assert state["measurements"]["temperatureC"]["provenance"] == "USER_REPORTED_TEXT"
    assert state["measurements"]["babyAgeMonths"]["provenance"] == "USER_REPORTED_TEXT"
    assert state["babyAgeMonths"] in {2, 10}


def test_structured_measured_values_win_over_free_text(monkeypatch):
    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    request = api.TriageTurnRequest.model_validate(
        {
            "sessionId": str(uuid.uuid4()),
            "stateVersion": 0,
            "expectedStateVersion": 0,
            "requestId": "request_" + uuid.uuid4().hex[:16],
            "messageId": "message_" + uuid.uuid4().hex[:16],
            "latestUserMessage": "Bé hai tháng sốt 39,3 độ",
            "selectedTarget": "BABY",
            "journeyContext": {"stage": "INFANT_0_12M", "babyAgeMonths": 2},
            "signals": {},
            "measurements": {
                "temperatureC": {
                    "value": 37.2,
                    "unit": "C",
                    "temporalStatus": "CURRENT",
                    "provenance": "MEASURED",
                }
            },
            "expectedRulesetHash": get_registry().ruleset_sha256,
        }
    )

    state = api.execute_turn(request).state

    assert state["measurements"]["temperatureC"]["value"] == 37.2
    assert state["measurements"]["temperatureC"]["provenance"] == "MEASURED"
    assert state["triageOutcome"] != "RED"


@pytest.mark.parametrize(
    "prior",
    [
        {"status": "UNKNOWN", "provenance": "MEASURED"},
        {
            "value": 37.2,
            "unit": "C",
            "temporalStatus": "HISTORICAL",
            "provenance": "MEASURED",
        },
    ],
)
def test_empty_or_historical_measured_record_does_not_block_latest_current_text(prior):
    state = {"measurements": {"temperatureC": prior}}
    reported = extract_reported_measurements(
        "Bé hai tháng sốt 39,3 độ", target_entity="BABY"
    )

    api._merge_reported_measurements(state, reported)

    assert state["measurements"]["temperatureC"]["value"] == 39.3
    assert state["measurements"]["temperatureC"]["provenance"] == "USER_REPORTED_TEXT"


def test_conflicting_latest_temperature_clears_prior_text_report():
    state = {
        "measurements": {
            "temperatureC": {
                "value": 38.2,
                "unit": "C",
                "temporalStatus": "CURRENT",
                "provenance": "USER_REPORTED_TEXT",
            }
        }
    }
    reported = extract_reported_measurements(
        "Bé đo 36,5 độ rồi đo lại 37 độ", target_entity="BABY"
    )

    api._merge_reported_measurements(state, reported)

    assert "temperatureC" not in state["measurements"]


def test_integral_structured_baby_age_updates_the_direct_rule_context():
    state = {
        "measurements": {
            "babyAgeMonths": {
                "value": 2.0,
                "unit": "MONTHS",
                "temporalStatus": "CURRENT",
                "provenance": "MEASURED",
            }
        }
    }

    api._merge_reported_measurements(state, extract_reported_measurements("Không rõ"))

    assert state["babyAgeMonths"] == 2


def test_python_and_java_provenance_sets_are_exactly_in_lockstep():
    java_source = (
        pathlib.Path(__file__).resolve().parents[2]
        / "CareBridgeAPI"
        / "src/main/java/com/carebridge/backend/triage/service/impl/CanonicalTriageSessionService.java"
    ).read_text(encoding="utf-8")
    block = re.search(
        r"private static final Set<String> PROVENANCE = Set\.of\((.*?)\);",
        java_source,
        re.DOTALL,
    )

    assert block is not None
    java_provenance = set(re.findall(r'"([A-Z_]+)"', block.group(1)))
    assert java_provenance == api._PROVENANCE
