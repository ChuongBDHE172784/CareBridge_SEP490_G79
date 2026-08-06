"""P2-T9 zero-trust dataset and positive-evidence scope tests."""

from __future__ import annotations

from app.context import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.rules.evaluator import DatasetStatus, ScopeStatus
from app.rules.registry import load_dataset_requirements
from app.triage_v2.dataset_scope_nodes import dataset_calculator, scope_calculator
from app.triage_v2.state import create_initial_state


def _state(message: str = "Tôi thấy không ổn"):
    state = create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message=message,
    )
    state.update(
        targetEntity=TargetEntity.MOTHER,
        intent=IntentType.SYMPTOM_TRIAGE,
        contextResolutionStatus=ContextResolutionStatus.RESOLVED,
    )
    return state


def _complete_global_screen(state):
    for code in load_dataset_requirements()["globalSafety"]["requiredSignalCodes"]:
        state["signals"][code] = {"presence": "ABSENT"}


def test_all_global_unknown_is_incomplete_and_caller_state_is_ignored():
    state = _state()
    state["safetyScreenStatus"] = DatasetStatus.COMPLETE

    updates = dataset_calculator(state)

    assert updates["safetyScreenStatus"] is DatasetStatus.INCOMPLETE
    assert updates["greenEligibilityDatasetStatus"] is DatasetStatus.INCOMPLETE


def test_caller_boolean_signals_cannot_manufacture_a_complete_safety_screen():
    state = _state()
    for code in load_dataset_requirements()["globalSafety"]["requiredSignalCodes"]:
        state["signals"][code] = False

    assert dataset_calculator(state)["safetyScreenStatus"] is DatasetStatus.INCOMPLETE


def test_positive_wrist_oos_stops_after_complete_safety_screen():
    state = _state("Tôi bị đau cổ tay sau khi tập thể thao")
    state["possiblePregnancy"] = "NO"
    state["stage"] = CareStage.PRECONCEPTION
    _complete_global_screen(state)
    state.update(dataset_calculator(state))

    updates = scope_calculator(state)

    assert updates["scopeStatus"] is ScopeStatus.CONFIRMED_OUT_OF_SCOPE
    assert updates["triageOutcome"] == "OUT_OF_SCOPE"
    assert updates["stopConversation"] is True


def test_wrist_complaint_waits_when_global_safety_is_incomplete():
    state = _state("Tôi bị đau cổ tay sau khi tập thể thao")
    state["possiblePregnancy"] = "NO"

    assert scope_calculator(state)["scopeStatus"] is ScopeStatus.POSSIBLY_IN_SCOPE


def test_unresolved_possible_pregnancy_blocks_out_of_scope():
    state = _state("Tôi bị đau cổ tay sau khi tập thể thao")
    _complete_global_screen(state)
    state.update(dataset_calculator(state))

    assert scope_calculator(state)["scopeStatus"] is ScopeStatus.IN_SCOPE


def test_unknown_complaint_never_becomes_out_of_scope():
    state = _state("Tôi thấy có gì đó không ổn")
    state["possiblePregnancy"] = "NO"
    _complete_global_screen(state)
    state.update(dataset_calculator(state))

    updates = scope_calculator(state)

    assert updates["scopeStatus"] is ScopeStatus.UNKNOWN
    assert "triageOutcome" not in updates


def test_red_is_never_overridden_by_scope():
    state = _state("Tôi bị đau cổ tay sau khi tập thể thao")
    state.update(triageOutcome="RED", stopConversation=False)

    updates = scope_calculator(state)

    assert updates["stopConversation"] is True
    assert "triageOutcome" not in updates
