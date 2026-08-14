"""P2-T7 signal normalization and conflict-merger tests."""

from __future__ import annotations

from copy import deepcopy

import pytest

from app.triage.signal_normalizer import signal_normalizer
from app.triage.state import create_initial_state


def _state():
    return create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message="structured observations",
    )


@pytest.mark.parametrize(
    ("raw", "expected"),
    [
        ({"presence": "PRESENT"}, "PRESENT"),
        ({"presence": "UNKNOWN", "explicitNegation": True}, "ABSENT"),
        (True, "UNKNOWN"),
        (None, "UNKNOWN"),
        ({"presence": "UNAWARE_OR_UNMEASURABLE"}, "UNAWARE_OR_UNMEASURABLE"),
    ],
)
def test_presence_is_normalized_without_boolean_or_missing_coercion(raw, expected):
    state = _state()
    state["signals"]["SEIZURE"] = raw

    assert signal_normalizer(state)["signals"]["SEIZURE"]["presence"] == expected


def test_historical_health_memory_does_not_become_a_current_signal():
    state = _state()
    state["signals"]["SEIZURE"] = {"presence": "PRESENT", "temporalStatus": "HISTORICAL"}

    normalized = signal_normalizer(state)["signals"]["SEIZURE"]

    assert normalized["presence"] == "UNKNOWN"
    assert normalized["historicalPresence"] == "PRESENT"


def test_contradictory_current_answers_merge_to_conflicted():
    state = _state()
    state["signals"]["VAGINAL_BLEEDING"] = [
        {"presence": "ABSENT", "temporalStatus": "CURRENT"},
        {"presence": "PRESENT", "temporalStatus": "CURRENT"},
    ]

    updates = signal_normalizer(state)

    assert updates["signals"]["VAGINAL_BLEEDING"]["presence"] == "CONFLICTED"
    assert updates["dataConflicts"] == ["SIGNAL_CONFLICTED:VAGINAL_BLEEDING"]


def test_duplicate_equivalent_answer_is_idempotent_not_conflicted():
    state = _state()
    state["signals"]["SEIZURE"] = ["ABSENT", "ABSENT"]

    updates = signal_normalizer(state)

    assert updates["signals"]["SEIZURE"]["presence"] == "ABSENT"
    assert updates["dataConflicts"] == []


def test_present_plus_explicit_negation_in_one_observation_is_conflicted():
    state = _state()
    state["signals"]["SEIZURE"] = {"presence": "PRESENT", "explicitNegation": True}

    assert signal_normalizer(state)["signals"]["SEIZURE"]["presence"] == "CONFLICTED"


def test_measurements_are_shape_normalized_without_threshold_inference():
    state = _state()
    state["measurements"] = {
        "temperatureC": 38.5,
        "bloodPressure": {"systolic": 140, "diastolic": 90, "unit": "mmHg"},
        "unknownDevice": "UNAWARE_OR_UNMEASURABLE",
    }
    before = deepcopy(state)

    updates = signal_normalizer(state)

    assert updates["measurements"]["temperatureC"] == {"value": 38.5}
    assert updates["measurements"]["bloodPressure"]["systolic"] == 140
    assert updates["measurements"]["unknownDevice"] == {"status": "UNAWARE_OR_UNMEASURABLE"}
    assert updates["signals"] == {}
    assert state == before


def test_stopped_red_bypasses_normalization():
    state = _state()
    state.update(triageOutcome="RED", requiredAction="IMMEDIATE", stopConversation=True)

    assert signal_normalizer(state) == {}


def test_remembered_context_never_asserts_a_current_present_signal():
    """A profile note or health-memory entry is background, not a report of what is happening
    now. Letting it read as current is how a stale record becomes a live danger signal."""

    state = _state()
    state["signals"] = {
        "SEIZURE": {"presence": "PRESENT", "provenance": "HEALTH_MEMORY_CONTEXT"},
    }

    updates = signal_normalizer(state)

    assert updates["signals"]["SEIZURE"]["presence"] == "UNKNOWN"
    assert updates["signals"]["SEIZURE"]["historicalPresence"] == "PRESENT"


def test_profile_context_is_demoted_to_history_not_to_absent():
    """Demoting to ABSENT would be inventing a negative finding nobody stated."""

    state = _state()
    state["signals"] = {
        "CYANOSIS": {"presence": "PRESENT", "provenance": "PROFILE_CONTEXT"},
    }

    updates = signal_normalizer(state)

    assert updates["signals"]["CYANOSIS"]["presence"] != "ABSENT"
    assert updates["signals"]["CYANOSIS"]["historicalPresence"] == "PRESENT"


def test_a_question_answer_of_the_same_signal_is_current():
    """The demotion must be specific to remembered context, not applied to real answers."""

    state = _state()
    state["signals"] = {
        "SEIZURE": {"presence": "PRESENT", "provenance": "QUESTION_ANSWER"},
    }

    updates = signal_normalizer(state)

    assert updates["signals"]["SEIZURE"]["presence"] == "PRESENT"
    assert updates["signals"]["SEIZURE"]["temporalStatus"] == "CURRENT"


def test_user_confirmation_overrides_remembered_context():
    """Once the user reports it this session, the remembered entry stops suppressing it."""

    state = _state()
    state["signals"] = {
        "SEIZURE": [
            {"presence": "PRESENT", "provenance": "HEALTH_MEMORY_CONTEXT"},
            {"presence": "PRESENT", "provenance": "USER_REPORTED"},
        ],
    }

    updates = signal_normalizer(state)

    assert updates["signals"]["SEIZURE"]["presence"] == "PRESENT"
